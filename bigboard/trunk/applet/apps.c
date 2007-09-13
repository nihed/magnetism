/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "apps.h"
#include "http.h"
#include <string.h>

#define APP_TYPE              (app_get_type ())
#define APP(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), APP_TYPE, App))
#define APP_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), APP_TYPE, AppClass))
#define APP_IS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), APP_TYPE))
#define APP_IS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), APP_TYPE))
#define APP_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), APP_TYPE, AppClass))

GType            app_get_type               (void) G_GNUC_CONST;

struct App
{
    GObject parent;
    DDMDataResource *resource;
    char *icon_url;
    GdkPixbuf *icon;
};

typedef struct {
    GObjectClass parent_klass;

} AppClass;

static void app_dispose  (GObject *object);
static void app_finalize (GObject *object);

enum {
    CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(App, app, G_TYPE_OBJECT);

static void
app_init(App *app)
{

}

static void
app_class_init(AppClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = app_dispose;
    object_class->finalize = app_finalize;

    signals[CHANGED] =
        g_signal_new ("changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
app_emit_changed(App *app)
{
    g_signal_emit(G_OBJECT(app), signals[CHANGED], 0);
}

static void
on_got_icon(GdkPixbuf  *new_icon,
            void       *data)
{
    App *app;
    GdkPixbuf *scaled;
    
    g_debug("Got reply to http GET for app icon");
    
    app = data;
    
    if (new_icon == NULL) {
        return;
    }

    /* FIXME hack, we need to scale to the panel's size and also
     * specify a matching size= in the url
     */
    scaled = gdk_pixbuf_scale_simple(new_icon, 22, 22, GDK_INTERP_BILINEAR);
    new_icon = scaled; /* new_icon not owned by us, no need to unref old one */
    
    if (app->icon)
        g_object_unref(app->icon);
    
    app->icon = new_icon;

    app_emit_changed(app);
}

static void
download_new_icon_url(App        *app,
                      const char *icon_url)
{
    DBusConnection *connection;
    
    g_free(app->icon_url);
    app->icon_url = g_strdup(icon_url);

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    if (connection == NULL)
        return;

    if (app->icon_url) {
        g_debug("Sending http request for app icon %s", app->icon_url);
        http_get_pixbuf(connection,
                        app->icon_url,
                        on_got_icon,
                        app);
    }
}

static void
update_icon_from_resource(App *app)
{
    const char *icon_url;
    char *sized_icon_url;
    
    icon_url = NULL;
    ddm_data_resource_get(app->resource,
                          "iconUrl", DDM_DATA_URL, &icon_url,
                          NULL);

    if (icon_url == NULL)
        sized_icon_url = NULL;
    else if (strchr(icon_url, '?') != NULL)
        sized_icon_url = g_strdup_printf("%s&%s", icon_url, "size=22");
    else
        sized_icon_url = g_strdup_printf("%s?%s", icon_url, "size=22");
    
    if ((icon_url == NULL && app->icon_url == NULL) ||
        (sized_icon_url && app->icon_url &&
         strcmp(sized_icon_url, app->icon_url) == 0)) {
        g_free(sized_icon_url);
        return;
    }
    
    download_new_icon_url(app, sized_icon_url);
    g_free(sized_icon_url);
}

static void
on_app_resource_changed(DDMDataResource *resource,
                        GSList          *changed_properties,
                        gpointer         user_data)
{
    App *app;
    
    app = user_data;
    
    update_icon_from_resource(app);

    app_emit_changed(app);
}
    
App*
app_new(DDMDataResource *resource)
{
    App *app;

    app = g_object_new(APP_TYPE, NULL);

    app->resource = resource;

    ddm_data_resource_connect(app->resource,
                              NULL, /* NULL = all properties */
                              on_app_resource_changed,
                              app);
    update_icon_from_resource(app);
    
    return app;
}

static void
app_dispose(GObject *object)
{    
    App *app;

    app = APP(object);
    
    if (app->icon)
        g_object_unref(app->icon);
    
    g_free(app->icon_url);
}

static void
app_finalize (GObject *object)
{
    App *app;

    app = APP(object);
    
    ddm_data_resource_disconnect(app->resource,
                                 on_app_resource_changed,
                                 app);
}

void
app_ref(App *app)
{
    g_object_ref(app);
}

void
app_unref(App *app)
{
    g_object_unref(app);
}

const char*
app_get_tooltip(App *app)
{
    const char *name;
    const char *tooltip;

    name = NULL;
    tooltip = NULL;
    ddm_data_resource_get(app->resource,
                          "name", DDM_DATA_STRING, &name,
                          "tooltip", DDM_DATA_STRING, &tooltip,
                          NULL);

    if (name == NULL)
        return "Unknown";
    else
        return name;
}

GdkPixbuf*
app_get_icon(App *app)
{
    return app->icon;
}

const char*
app_get_desktop_names (App *app)
{
    const char *names;

    names = NULL;
    ddm_data_resource_get(app->resource,
                          "desktopNames", DDM_DATA_STRING, &names,
                          NULL);
    
    return names;
}

int
app_get_usage_count (App *app)
{
    int usage;
    
    usage = 0;
    ddm_data_resource_get(app->resource,
                          "usageCount", DDM_DATA_INTEGER, &usage,
                          NULL);
    
    return usage;
}
