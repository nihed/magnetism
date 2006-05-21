#include <config.h>
#include <string.h>
#include "main.h"
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"
#include "hippo-chat-window.h"
#include "hippo-bubble.h"
#include "hippo-bubble-manager.h"
#include "hippo-dbus-server.h"

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
    GHashTable *chat_windows;
    HippoImageCache *photo_cache;
    HippoDBus *dbus;
    /* see join_chat() comment */
    const char *creating_chat_id;
};

void
hippo_app_quit(HippoApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

void
hippo_app_show_about(HippoApp *app)
{
    if (app->about_dialog == NULL) {
        app->about_dialog = g_object_new(GTK_TYPE_ABOUT_DIALOG,
            "name", "Mugshot",
            "version", VERSION,
            "copyright", "Copyright 2006 Red Hat, Inc. and others",
            "website", "http://mugshot.org",
            NULL);
        g_signal_connect(app->about_dialog, "destroy",
            G_CALLBACK(gtk_widget_destroyed), &app->about_dialog);
    }
    
    gtk_window_present(GTK_WINDOW(app->about_dialog));
}

/* use_login_browser uses the browser we've logged in to 
 * the site with; if it's FALSE, we would want to use 
 * the user's default browser instead, which will almost 
 * always be the same presumably.
 * 
 * The idea is that links that go to our site need to 
 * use our login browser, other links should use the
 * user's browser.
 * 
 * Right now only use_login_browser is implemented anyhow
 * though ;-)
 */
void
hippo_app_open_url(HippoApp   *app,
                   gboolean    use_login_browser,
                   const char *url)
{
    HippoBrowserKind browser;
    char *command;
    char *quoted;
    GError *error;
    
    g_debug("Opening url '%s'", url);
    
    browser = hippo_connection_get_auth_browser(app->connection);
    
    quoted = g_shell_quote(url);
    
    switch (browser) {
    case HIPPO_BROWSER_EPIPHANY:
        command = g_strdup_printf("epiphany %s", quoted);
        break;
    case HIPPO_BROWSER_FIREFOX:
    default:
        command = g_strdup_printf("firefox %s", quoted);    
        break;
    }
  
    error = NULL;
    if (!g_spawn_command_line_async(command, &error)) {
        GtkWidget *dialog;
        
        dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("Couldn't start your web browser!"));
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        
        gtk_widget_show(dialog);
        
        g_debug("Failed to launch browser: %s", error->message);
        g_error_free(error);
    }
    
    g_free(command);
    g_free(quoted);
}

static char*
make_absolute_url(HippoApp   *app,
                  const char *relative)
{
    char *server;
    char *url;
    g_return_val_if_fail(*relative == '/', NULL);
    server = hippo_platform_get_web_server(app->platform);
    url = g_strdup_printf("http://%s%s", server, relative);
    g_free(server);
    return url;
}

void
hippo_app_show_home(HippoApp *app)
{
    char *url;
    url = make_absolute_url(app, "/");
    hippo_app_open_url(app, TRUE, url);
    g_free(url);
}

void
hippo_app_visit_post(HippoApp   *app,
                     HippoPost  *post)
{
    char *url;
    char *relative;
    relative = g_strdup_printf("/visit?post=%s", hippo_post_get_guid(post));
    url = make_absolute_url(app, relative);
    hippo_app_open_url(app, TRUE, url);
    g_free(relative);
    g_free(url);
}

void
hippo_app_visit_post_id(HippoApp   *app,
                        const char *guid)
{
    HippoPost *post;
    
    post = hippo_data_cache_lookup_post(app->cache, guid);
    if (post == NULL) {
        g_warning("don't know about post '%s' can't open its page", guid);
        return;
    }
    hippo_app_visit_post(app, post);
}

static void
visit_entity(HippoApp       *app,
             const char     *id,
             HippoEntityType type)
{
    char *url;
    char *relative;
    if (type == HIPPO_ENTITY_PERSON)
        relative = g_strdup_printf("/person?who=%s", id);
    else if (type == HIPPO_ENTITY_GROUP)
        relative = g_strdup_printf("/person?who=%s", id);
    else {
        g_warning("Can't visit entity '%s' due to type %d", id, type);
        return;
    }        
    url = make_absolute_url(app, relative);
    hippo_app_open_url(app, TRUE, url);
    g_free(relative);
    g_free(url);
    
}             

void
hippo_app_visit_entity(HippoApp    *app,
                       HippoEntity *entity)
{
    visit_entity(app, hippo_entity_get_guid(entity),
                 hippo_entity_get_entity_type(entity));
}
                       
void
hippo_app_visit_entity_id(HippoApp    *app,
                          const char  *guid)
{
    HippoEntity *entity;
    entity = hippo_data_cache_lookup_entity(app->cache, guid);
    if (entity == NULL) {
        g_warning("Don't know about entity '%s' can't go to their page", guid);
        return;
    }
    hippo_app_visit_entity(app, entity);
}

static void
on_chat_window_destroy(HippoChatWindow *window,
                       HippoApp        *app)
{
    HippoChatRoom *room;
    
    room = hippo_chat_window_get_room(window);
    g_hash_table_remove(app->chat_windows,
                        hippo_chat_room_get_id(room));
}

void
hippo_app_join_chat(HippoApp   *app,
                    const char *chat_id)
{
    HippoChatWindow *window;

    window = g_hash_table_lookup(app->chat_windows, chat_id);
    if (window == NULL) {
        HippoChatRoom *room;

        /* this can cause a bubble, which will open since post_is_active relies
         * on the chat window being inserted, something we're about to do... 
         * so we have a little hack here
         */
        g_assert(app->creating_chat_id == NULL); /* no recursion */
        app->creating_chat_id = chat_id;
        
        room = hippo_data_cache_ensure_chat_room(app->cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);
        window = hippo_chat_window_new(app->cache, room);
        g_hash_table_replace(app->chat_windows, g_strdup(chat_id), window);
        g_signal_connect(window, "destroy", G_CALLBACK(on_chat_window_destroy), app);
        
        app->creating_chat_id = NULL;
    }
    
    gtk_window_present(GTK_WINDOW(window));   
}

gboolean
hippo_app_post_is_active(HippoApp   *app,
                         const char *post_id)
{
    /* FIXME we should also detect having a post open in a browser */
    
    if (app->creating_chat_id && strcmp(post_id, app->creating_chat_id) == 0)
        return TRUE;
        
    return g_hash_table_lookup(app->chat_windows, post_id) != NULL;    
}

void
hippo_app_load_photo(HippoApp               *app,
                     HippoEntity            *entity,
                     HippoImageCacheLoadFunc func,
                     void                   *data)
{
    const char *url;
    
    url = hippo_entity_get_small_photo_url(entity);
    
    g_debug("Loading photo for entity '%s' url '%s'",
        hippo_entity_get_guid(entity),
        url ? url : "null");
    
    if (url == NULL) {
        /* not gonna succeed in loading this... */
        (* func)(NULL, data);
    } else {
        char *absolute = make_absolute_url(app, url);
        hippo_image_cache_load(app->photo_cache, absolute, func, data);
        g_free(absolute);
    }
}

void
hippo_app_put_window_by_icon(HippoApp  *app,
                             GtkWindow *window)
{
    GtkOrientation orientation;
    int x, y, width, height;
    GdkScreen *screen;
    GdkRectangle monitor;
    int monitor_num;
    int window_x, window_y;
    GtkRequisition req;
    gboolean is_west;
    gboolean is_north;

    orientation = hippo_gtk_status_icon_get_orientation(GTK_STATUS_ICON(app->icon));
    hippo_gtk_status_icon_get_screen_geometry(GTK_STATUS_ICON(app->icon), &screen,
        &x, &y, &width, &height);

    monitor_num = gdk_screen_get_monitor_at_point(screen, x, y);
    if (monitor_num < 0)
        monitor_num = 0;

    gdk_screen_get_monitor_geometry(screen, monitor_num, &monitor);

#define GAP 3

    is_west = ((x + width / 2) < (monitor.x + monitor.width / 2));
    is_north = ((y + height / 2) < (monitor.y + monitor.height / 2));

    /* this just assumes a borderless window for now, doesn't mess with gravity,
     * though it would be easily fixed if we ever cared
     */

    gtk_widget_size_request(GTK_WIDGET(window), &req);
    if (orientation == GTK_ORIENTATION_VERTICAL) {
        if (is_west) {
            window_x = x + width + GAP;
        } else {
            window_x = x - req.width - GAP;
        }
        if (is_north) {
            window_y = y;
        } else {
            window_y = y + height - req.height;
        }
    } else {
        if (is_west) {
            window_x = x;
        } else {
            window_x = x + width - req.width;
        }
        if (is_north) {
            window_y = y + height + GAP;
        } else {
            window_y = y - req.height - GAP;
        }
    }
    
    gtk_window_move(window, window_x, window_y);
}

static void
on_dbus_disconnected(HippoDBus *dbus,
                     HippoApp  *app)
{
    hippo_app_quit(app);
}

static HippoApp*
hippo_app_new(HippoInstanceType  instance_type,
              HippoPlatform     *platform,
              HippoDBus         *dbus)
{
    HippoApp *app = g_new0(HippoApp, 1);

    app->platform = platform;
    g_object_ref(app->platform);

    app->loop = g_main_loop_new(NULL, FALSE);
    app->dbus = dbus;
    g_object_ref(app->dbus);

    g_signal_connect(G_OBJECT(app->dbus), "disconnected",
                     G_CALLBACK(on_dbus_disconnected), app);

    app->connection = hippo_connection_new(app->platform);
    g_object_unref(app->platform); /* let connection keep it alive */
    app->cache = hippo_data_cache_new(app->connection);
    g_object_unref(app->connection); /* let the data cache keep it alive */
    app->icon = hippo_status_icon_new(app->cache);
    
    app->photo_cache = hippo_image_cache_new();
    
    app->chat_windows = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);

    hippo_bubble_manager_manage(app->cache);
    
    return app;
}

static void
hippo_app_free(HippoApp *app)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                             G_CALLBACK(on_dbus_disconnected), app);

    hippo_bubble_manager_unmanage(app->cache);

    g_hash_table_destroy(app->chat_windows);
    app->chat_windows = NULL;

    if (app->about_dialog)
        gtk_object_destroy(GTK_OBJECT(app->about_dialog));
    g_object_unref(app->icon);
    g_object_unref(app->cache);
    g_object_unref(app->photo_cache);
    g_object_unref(app->dbus);
    g_main_loop_unref(app->loop);
    g_free(app);
}

static gboolean
show_debug_share_timeout(void *data)
{
    HippoApp *app = data;
    
    g_debug("Adding debug share data");
    
    hippo_data_cache_add_debug_data(app->cache);
    /* remove timeout */
    return FALSE;
}

/* 
 * Singleton HippoApp and main()
 */

static HippoApp *the_app;

HippoApp*
hippo_get_app(void)
{
    return the_app;
}

static void
print_debug_func(const char *message)
{
    g_printerr("%s\n", message);
}

int
main(int argc, char **argv)
{
    HippoOptions options;
    HippoDBus *dbus;
    HippoPlatform *platform;
    char *server;
    GError *error; 
     
    hippo_set_print_debug_func(print_debug_func);
     
    g_thread_init(NULL);
    
    /* not marked for translation, it's a trademark */
    g_set_application_name("Mugshot");
    
    gtk_init(&argc, &argv);

    if (!hippo_parse_options(&argc, &argv, &options))
        return 1;

    if (options.instance_type == HIPPO_INSTANCE_DEBUG) {
        gtk_icon_theme_append_search_path(gtk_icon_theme_get_default(),
                                          ABSOLUTE_TOP_SRCDIR "/icons");
    }
    
    platform = hippo_platform_impl_new(options.instance_type);
    server = hippo_platform_get_web_server(platform);

    error = NULL;
    dbus = hippo_dbus_try_to_acquire(server,
                                     (options.quit_existing || options.replace_existing),
                                     &error);
    if (dbus == NULL) {
        g_assert(error != NULL);
        g_printerr(_("Can't connect to session message bus: %s\n"), error->message);
        g_error_free(error);
        return 1;
    } else {
        g_assert(error == NULL);
    }
    
    if (options.quit_existing) {
        /* we kicked off the other guy, now just exit ourselves */
        return 1;
    }

    /* Now let HippoApp take over the logic, since we know we 
     * want to exist as a running app
     */
    the_app = hippo_app_new(options.instance_type, platform, dbus);

    /* get rid of all this, the app has taken over */
    g_object_unref(dbus);
    dbus = NULL;
    g_object_unref(platform);
    platform = NULL;
    g_free(server);
    server = NULL;

    if (hippo_connection_signin(the_app->connection))
        g_debug("Waiting for user to sign in");
    else
        g_debug("Found login cookie");

    gtk_status_icon_set_visible(GTK_STATUS_ICON(the_app->icon), TRUE);
    
    if (options.join_chat_id) {
        hippo_app_join_chat(the_app, options.join_chat_id);
    }

    if (options.initial_debug_share) {
        /* timeout removes itself */
        g_timeout_add(1000, show_debug_share_timeout, the_app);
    }
    
    hippo_options_free_fields(&options);

#if 0
    {
        GtkWidget *window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
        GtkWidget *bubble = hippo_bubble_new();
        gtk_container_set_border_width(GTK_CONTAINER(bubble), 10);
        gtk_container_add(GTK_CONTAINER(window), bubble);
        gtk_widget_show_all(window);
    }
#endif    
    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    g_debug("Dropping loudmouth connection");
    hippo_connection_signout(the_app->connection);

    g_debug("Releasing dbus");
    hippo_dbus_blocking_shutdown(the_app->dbus);

    hippo_app_free(the_app);

    return 0;
}
