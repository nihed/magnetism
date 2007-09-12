/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>
#include "self.h"
#define DDM_I_KNOW_THIS_IS_UNSTABLE 1
#include <ddm/ddm.h>

#include "http.h"
#include "apps.h"
#include <string.h>

typedef struct {
    SelfIconChangedCallback callback;
    void *data;
} IconChangedClosure;

typedef struct {
    DDMDataModel *ddm_model;
    DDMDataQuery *self_query;
    DDMDataResource *self_resource;
    char *photo_url;
    GdkPixbuf *icon;
    GSList *icon_changed_closures;
    GHashTable *apps_by_resource_id;
} SelfData;


static GdkPixbuf*
pixbuf_parse(GString               *content,
             GError               **error_p)
{
    GdkPixbufLoader *loader;
    GdkPixbuf *pixbuf;

    loader = gdk_pixbuf_loader_new();

    if (!gdk_pixbuf_loader_write(loader, (guchar*) content->str, content->len, error_p))
        goto failed;
    
    if (!gdk_pixbuf_loader_close(loader, error_p))
        goto failed;

    pixbuf = gdk_pixbuf_loader_get_pixbuf(loader);
    if (pixbuf == NULL) {
        g_set_error(error_p,
                    GDK_PIXBUF_ERROR,
                    GDK_PIXBUF_ERROR_FAILED,
                    "Could not load pixbuf");
        goto failed;
    }

    g_object_ref(pixbuf);
    g_object_unref(loader);
    return pixbuf;

  failed:
    g_assert(error_p == NULL || *error_p != NULL);
    
    if (loader)
        g_object_unref(loader);

    return NULL;
}

static void
on_got_photo(const char *content_type,
             GString    *content_or_error,
             void       *data)
{
    SelfData *sd;
    GdkPixbuf *new_icon;
    GError *error;
    GSList *l;

    g_debug("Got reply to http GET for user photo");
    
    sd = data;
    
    if (content_type == NULL) {
        g_printerr("Failed to download user photo: %s\n",
                   content_or_error->str);
        return;
    }

    error = NULL;
    new_icon = pixbuf_parse(content_or_error, &error);
    if (new_icon == NULL) {
        g_printerr("Failed to parse user photo: %s\n",
                   error->message);
        g_error_free(error);
        return;
    }

    if (sd->icon)
        g_object_unref(sd->icon);

    sd->icon = new_icon;

    for (l = sd->icon_changed_closures; l != NULL; l = l->next) {
        IconChangedClosure *icc = l->data;

        (* icc->callback) (sd->icon, icc->data);
    }
}

static void
download_new_photo_url(SelfData   *sd,
                       const char *photo_url)
{
    DBusConnection *connection;
    
    g_free(sd->photo_url);
    sd->photo_url = g_strdup(photo_url);

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    if (connection == NULL)
        return;

    if (sd->photo_url) {
        g_debug("Sending http request for user photo %s", sd->photo_url);
        http_get(connection,
                 sd->photo_url,
                 on_got_photo,
                 sd);
    }
}

static void
update_photo_from_self(SelfData *sd)
{
    if (sd->self_resource) {
        const char *photo_url;

        photo_url = NULL;
        ddm_data_resource_get(sd->self_resource,
                              "photoUrl", DDM_DATA_URL, &photo_url,
                              NULL);
        
        if ((photo_url == NULL && sd->photo_url == NULL) ||
            (photo_url && sd->photo_url &&
             strcmp(photo_url, sd->photo_url) == 0))
            return;

        download_new_photo_url(sd, photo_url);
    }
}

static void
on_photo_changed(DDMDataResource *resource,
                 GSList          *changed_properties,
                 gpointer         user_data)
{
    SelfData *sd;

    sd = user_data;

    if (sd->self_resource == resource)
        update_photo_from_self(sd);
    else
        g_warning("Got photo changed notification for someone else?");
}

static void
update_applications_from_self(SelfData *sd)
{
    if (sd->self_resource) {
        GSList *apps;
        GSList *l;

        /* we don't own the list or its contents (ddm_data_resource_get does not copy it) */
        apps = NULL;
        ddm_data_resource_get(sd->self_resource,
                              "topApplications", DDM_DATA_RESOURCE | DDM_DATA_LIST,
                              &apps,
                              NULL);

        g_debug("Got %d top applications", g_slist_length(apps));
        
        if (sd->apps_by_resource_id == NULL) {
            sd->apps_by_resource_id = g_hash_table_new_full(g_str_hash,
                                                            g_str_equal,
                                                            (GFreeFunc) g_free,
                                                            (GFreeFunc) app_unref);
        }
        for (l = apps; l != NULL; l = l->next) {
            DDMDataResource *new_app_resource = l->data;
            App *old_app;

            old_app = g_hash_table_lookup(sd->apps_by_resource_id,
                                          ddm_data_resource_get_resource_id(new_app_resource));
            if (old_app == NULL) {
                App *new_app;
                new_app = app_new(new_app_resource);
                g_hash_table_replace(sd->apps_by_resource_id,
                                     g_strdup(ddm_data_resource_get_resource_id(new_app_resource)),
                                     new_app);
            }
        }
    }
}

static void
on_applications_changed(DDMDataResource *resource,
                        GSList          *changed_properties,
                        gpointer         user_data)
{
    SelfData *sd;

    sd = user_data;
    
    if (sd->self_resource == resource)
        update_applications_from_self(sd);
    else
        g_warning("Got apps changed notification for someone else?");    
}

static void
on_query_response(GSList            *resources,
                  gpointer           user_data)
{
    SelfData *sd;

    sd = user_data;

    /* we ignore the returned resources and just fetch out
     * the one we wanted
     */
    if (sd->self_resource == NULL) {
        DDMDataResource *global_resource;

        global_resource = ddm_data_model_lookup_resource(sd->ddm_model,
                                                         "online-desktop:/o/global");
        if (global_resource == NULL) {
            g_printerr("No global resource in data model");
            return;
        }

        ddm_data_resource_get(global_resource,
                              "self", DDM_DATA_RESOURCE, &sd->self_resource,
                              NULL);
        
        if (sd->self_resource == NULL) {
            g_printerr("No self resource on global resource");
            return;
        }

        update_photo_from_self(sd);
        update_applications_from_self(sd);
        
        /* FIXME hack since child fetch does not work yet */
        ddm_data_model_query_resource(sd->ddm_model,
                                      ddm_data_resource_get_resource_id(sd->self_resource),
                                      "topApplications+");
        
        ddm_data_resource_connect(sd->self_resource,
                                  "photoUrl",
                                  on_photo_changed,
                                  sd);

        ddm_data_resource_connect(sd->self_resource,
                                  "topApplications",
                                  on_applications_changed,
                                  sd);
    }
}

static void
on_query_error(DDMDataError     error,
               const char      *message,
               gpointer         user_data)
{
    g_printerr("Failed to get query reply: '%s'\n", message);
}

static void
on_ddm_connected_changed(DDMDataModel *ddm_model,
                         gboolean      connected,
                         void         *data)
{
    SelfData *sd = data;

    if (connected && sd->self_query == NULL) {        
        sd->self_query =
            ddm_data_model_query_resource(ddm_model,
                                          /* the child fetch in [] does not work yet */
                                          "online-desktop:/o/global", "self [ photoUrl;topApplications+ ]");

        ddm_data_query_set_multi_handler(sd->self_query,
                                         on_query_response, sd);    
        
        ddm_data_query_set_error_handler(sd->self_query,
                                         on_query_error, sd);
    }
}

static SelfData *global_self_data = NULL;

static SelfData*
get_self_data(void)
{
    if (global_self_data == NULL) {
        global_self_data = g_new0(SelfData, 1);
        global_self_data->ddm_model = ddm_data_model_get_default();

        g_signal_connect(G_OBJECT(global_self_data->ddm_model),
                         "connected-changed",
                         G_CALLBACK(on_ddm_connected_changed),
                         global_self_data);
    }

    return global_self_data;
}

void
self_add_icon_changed_callback (SelfIconChangedCallback  callback,
                                void                    *data)
{
    SelfData *sd;
    IconChangedClosure *icc;

    sd = get_self_data();

    icc = g_new0(IconChangedClosure, 1);
    icc->callback = callback;
    icc->data = data;

    sd->icon_changed_closures = g_slist_append(sd->icon_changed_closures, icc);
}

void
self_remove_icon_changed_callback(SelfIconChangedCallback  callback,
                                  void                    *data)
{
    SelfData *sd;
    GSList *l;
    IconChangedClosure *icc;
        
    sd = get_self_data();

    icc = NULL;
    for (l = sd->icon_changed_closures; l != NULL; l = l->next) {
        IconChangedClosure *item = l->data;

        if (item->callback == callback && item->data) {
            icc = item;
            break;
        }
    }

    if (icc == NULL) {
        g_warning("Tried to remove nonexistent icon changed callback");
    }

    sd->icon_changed_closures = g_slist_remove(sd->icon_changed_closures, icc);
}
