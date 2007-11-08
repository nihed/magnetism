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
    SelfAppsChangedCallback callback;
    void *data;
} AppsChangedClosure;

typedef struct {
    DDMDataModel *ddm_model;
    DDMDataResource *self_resource;
    char *photo_url;
    GdkPixbuf *icon;
    GHashTable *apps_by_resource_id;
    GSList *icon_changed_closures;
    GSList *apps_changed_closures;

    guint apps_changed_idle;
} SelfData;


static void
on_got_photo(GdkPixbuf  *new_icon,
             void       *data)
{
    SelfData *sd;
    GSList *l;

    g_debug("Got reply to http GET for user photo");
    
    sd = data;
    
    if (new_icon == NULL) {
        return;
    }

    if (new_icon)
        g_object_ref(new_icon);
    
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
        http_get_pixbuf(connection,
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
    SelfData *sd = user_data;
    
    update_photo_from_self(sd);
}

static void
listify_foreach(void *key,
                void *value,
                void *data)
{
    GSList **list_p = data;

    app_ref(value);
    *list_p = g_slist_prepend(*list_p, value);
}
                
static int
apps_compare_usage_descending_func(const void *a,
                                   const void *b)
{
    int usage_a;
    int usage_b;

    usage_a = app_get_usage_count((void*)a);
    usage_b = app_get_usage_count((void*)b);

    if (usage_a < usage_b)
        return 1;
    else if (usage_a > usage_b)
        return -1;
    else
        return 0;
}

static gboolean
apps_changed_idle(void *data)
{
    SelfData *sd = data;
    GSList *apps;
    GSList *l;
    
    sd->apps_changed_idle = 0;

    apps = NULL;
    g_hash_table_foreach(sd->apps_by_resource_id,
                         listify_foreach, &apps);

    apps = g_slist_sort(apps, apps_compare_usage_descending_func);
    
    for (l = sd->apps_changed_closures; l != NULL; l = l->next) {
        AppsChangedClosure *acc = l->data;

        (* acc->callback) (apps, acc->data);
    }

    g_slist_foreach(apps, (GFunc) app_unref, NULL);
    g_slist_free(apps);

    /* remove idle */
    return FALSE;
}

static void
update_applications_from_self(SelfData *sd)
{
    if (sd->self_resource) {
        GSList *apps;
        GSList *l;
        GHashTable *new_hash;
        int new_created;
        int old_count;
        
        /* we don't own the list or its contents (ddm_data_resource_get does not copy it) */
        apps = NULL;
        ddm_data_resource_get(sd->self_resource,
                              "topApplications", DDM_DATA_RESOURCE | DDM_DATA_LIST,
                              &apps,
                              NULL);

        g_debug("Got %d top applications", g_slist_length(apps));
        
        new_hash = g_hash_table_new_full(g_str_hash,
                                         g_str_equal,
                                         (GFreeFunc) g_free,
                                         (GFreeFunc) app_unref);
        new_created = 0;
        if (sd->apps_by_resource_id)
            old_count = g_hash_table_size(sd->apps_by_resource_id);
        else
            old_count = 0;
        
        for (l = apps; l != NULL; l = l->next) {
            DDMDataResource *new_app_resource = l->data;
            App *old_app;
            App *new_app;

            if (sd->apps_by_resource_id != NULL)
                old_app = g_hash_table_lookup(sd->apps_by_resource_id,
                                              ddm_data_resource_get_resource_id(new_app_resource));
            else
                old_app = NULL;
            
            if (old_app == NULL) {
                new_app = app_new(new_app_resource);
                new_created += 1;
            } else {
                app_ref(old_app);
                new_app = old_app;
            }

            g_hash_table_replace(new_hash,
                                 g_strdup(ddm_data_resource_get_resource_id(new_app_resource)),
                                 new_app);
        }
        
        if (sd->apps_by_resource_id)
            g_hash_table_destroy(sd->apps_by_resource_id);
        sd->apps_by_resource_id = new_hash;

        g_debug("created %d new apps, had %d old apps, and now have %d total apps",
                new_created, old_count, g_hash_table_size(new_hash));
        
        if (new_created > 0 || (old_count != (int) g_hash_table_size(new_hash))) {
            if (sd->apps_changed_idle == 0) {
                g_debug("Adding apps changed idle handler");
                sd->apps_changed_idle = g_idle_add(apps_changed_idle, sd);
            }
        }
    }
}

static void
on_applications_changed(DDMDataResource *resource,
                        GSList          *changed_properties,
                        gpointer         user_data)
{
    SelfData *sd = user_data;
    
    update_applications_from_self(sd);
}

static void
on_query_response(DDMDataResource *self_resource,
                  gpointer         user_data)
{
    SelfData *sd;

    sd = user_data;

    if (self_resource)
        ddm_data_resource_ref(self_resource);
    
    if (sd->self_resource) {
        ddm_data_resource_disconnect(sd->self_resource,
                                     on_photo_changed,
                                     sd);
        ddm_data_resource_disconnect(sd->self_resource,
                                     on_applications_changed,
                                     sd);
        
        ddm_data_resource_unref(sd->self_resource);
    }

    sd->self_resource = self_resource;
    
    if (sd->self_resource == NULL) {
        g_debug("No self resource on global resource");
        /* If there was previous data; just leave it there. It's not 100% clear
         * to me that this is right ... the only time self should go from there
         * to not there is when the user clears there online.gnome.org login
         * entirely.
         */
        return;
    }
    
    ddm_data_resource_connect(sd->self_resource,
                              "photoUrl",
                              on_photo_changed,
                              sd);
    
    ddm_data_resource_connect(sd->self_resource,
                              "topApplications",
                              on_applications_changed,
                              sd);
    
    update_photo_from_self(sd);
    update_applications_from_self(sd);
}

static void
on_query_error(DDMDataError     error,
               const char      *message,
               gpointer         user_data)
{
    g_printerr("Query for photoUrl and topApplications failed: '%s'\n", message);
}

static void
on_ddm_ready(DDMDataModel *ddm_model,
             void         *data)
{
    SelfData *sd = data;
    DDMDataResource *self_resource = ddm_data_model_get_self_resource(ddm_model);
    
    if (self_resource) {
        DDMDataQuery *query;
        
        query = ddm_data_model_query_resource(ddm_model, self_resource,
                                              "photoUrl; topApplications +");
        
        /* query frees itself when either handler is called */
        
        ddm_data_query_set_single_handler(query,
                                          on_query_response, sd);    
        
        ddm_data_query_set_error_handler(query,
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
                         "ready",
                         G_CALLBACK(on_ddm_ready),
                         global_self_data);

        if (ddm_data_model_is_ready(global_self_data->ddm_model)) {
            on_ddm_ready(global_self_data->ddm_model, global_self_data);
        }
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

void
self_add_apps_changed_callback (SelfAppsChangedCallback  callback,
                                void                    *data)
{
    SelfData *sd;
    AppsChangedClosure *acc;

    sd = get_self_data();

    acc = g_new0(AppsChangedClosure, 1);
    acc->callback = callback;
    acc->data = data;

    sd->apps_changed_closures = g_slist_append(sd->apps_changed_closures, acc);
}

void
self_remove_apps_changed_callback(SelfAppsChangedCallback  callback,
                                  void                    *data)
{
    SelfData *sd;
    GSList *l;
    AppsChangedClosure *acc;
        
    sd = get_self_data();

    acc = NULL;
    for (l = sd->apps_changed_closures; l != NULL; l = l->next) {
        AppsChangedClosure *item = l->data;

        if (item->callback == callback && item->data) {
            acc = item;
            break;
        }
    }

    if (acc == NULL) {
        g_warning("Tried to remove nonexistent apps changed callback");
    }

    sd->apps_changed_closures = g_slist_remove(sd->apps_changed_closures, acc);
}
