/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-settings.h"
#include "hippo-connection.h"
#include <string.h>

typedef struct CacheEntry CacheEntry;

struct CacheEntry {
    char *key;
    char *value;
};

typedef struct RequestEntry RequestEntry;

struct RequestEntry {
    char *key;
    GSList *waiting;
};

typedef struct WaitingForRequest WaitingForRequest;

struct WaitingForRequest {
    HippoSettingArrivedFunc func;
    void *data;
};

static void      hippo_settings_init                (HippoSettings       *settings);
static void      hippo_settings_class_init          (HippoSettingsClass  *klass);

static void      hippo_settings_dispose             (GObject            *object);
static void      hippo_settings_finalize            (GObject            *object);

struct _HippoSettings {
    GObject parent;
    HippoConnection *connection;

    GHashTable *entries;
    GHashTable *requests;
};

struct _HippoSettingsClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoSettings, hippo_settings, G_TYPE_OBJECT);

/*
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
*/


static void
hippo_settings_init(HippoSettings  *settings)
{

}

static void
hippo_settings_class_init(HippoSettingsClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_settings_dispose;
    object_class->finalize = hippo_settings_finalize;


}

static void
waiting_node_complete(WaitingForRequest *waiting,
                      const char        *key,
                      const char        *value)
{
    (* waiting->func) (key, value, waiting->data);
}

static void
cache_entry_free(CacheEntry *entry)
{
    g_free(entry->key);
    g_free(entry->value);
    g_free(entry);
}

static void
request_entry_notify(RequestEntry *request,
                     const char   *value)
{
    GSList *waiters;

    if (request->waiting == NULL)
        return;
    
    g_debug("async got: %s=%s", request->key, value ? value : "(null)");
    
    waiters = request->waiting;
    request->waiting = NULL;

    while (waiters != NULL) {
        WaitingForRequest *w = waiters->data;
        waiters = g_slist_remove(waiters, waiters->data);
        waiting_node_complete(w, request->key, value);
    }
}

static void
request_entry_free(RequestEntry *request)
{
    /* assume requests all returned "value is unset" if we're freed
     * and haven't notified yet
     */
    request_entry_notify(request, NULL);
    
    g_free(request->key);

    g_free(request);
}

static void
invalidate_cache(HippoSettings *settings,
                 const char    *key)
{
    g_hash_table_remove(settings->entries, key);
}

static void
mark_request_pending(HippoSettings *settings,
                     const char    *key,
                     HippoSettingArrivedFunc func,
                     void          *data)
{
    RequestEntry *request;

    /* can't be pending if we have it cached already */
    g_return_if_fail(g_hash_table_lookup(settings->entries, key) == NULL);
    
    request = g_hash_table_lookup(settings->requests, key);
    if (request == NULL) {
        request = g_new0(RequestEntry, 1);
        request->key = g_strdup(key);

        g_hash_table_replace(settings->requests, request->key, request);
        
        g_debug("request for '%s' now pending", key);
    }

    if (func != NULL) {
        WaitingForRequest *waiting;
        waiting = g_new0(WaitingForRequest, 1);
        waiting->func = func;
        waiting->data = data;
        request->waiting = g_slist_prepend(request->waiting, waiting);

        g_debug("new callback added to request for '%s'", key);
    }
}

static void
mark_request_filled(HippoSettings *settings,
                    const char    *key,
                    const char    *value)
{
    RequestEntry *request;

    /* has to be cached if it's now filled */
    g_return_if_fail(g_hash_table_lookup(settings->entries, key) != NULL);
    
    request = g_hash_table_lookup(settings->requests, key);
    if (request != NULL) {
        request_entry_notify(request, value);
        g_hash_table_remove(settings->requests, key); /* should destroy the RequestEntry */
    } else {
        g_debug("nobody was waiting to hear about '%s'", key);
    }

    g_assert(g_hash_table_lookup(settings->requests, key) == NULL);
}

static void
update_cache(HippoSettings *settings,
             const char    *key,
             const char    *value)
{
    CacheEntry *entry;
    
    g_debug("caching: %s=%s", key, value ? value : "(null)");

    entry = g_hash_table_lookup(settings->entries, key);
    if (entry == NULL) {
        entry = g_new0(CacheEntry, 1);
        entry->key = g_strdup(key);
        entry->value = g_strdup(value);
        g_hash_table_replace(settings->entries, entry->key, entry);
    } else {
        g_free(entry->value);
        entry->value = g_strdup(value);
    }

    /* notify anyone that was waiting for this new value */
    mark_request_filled(settings, key, value);
}

static void
on_setting_changed(HippoConnection *connection,
                   const char      *key,
                   const char      *value,
                   void            *data)
{
    HippoSettings *settings = HIPPO_SETTINGS(data);

    update_cache(settings, key, value);
}

static void
hippo_settings_dispose(GObject *object)
{
    /* HippoSettings *settings = HIPPO_SETTINGS(object); */

    g_signal_handlers_disconnect_by_func(object, G_CALLBACK(on_setting_changed), object);
    
    G_OBJECT_CLASS(hippo_settings_parent_class)->dispose(object);
}

static void
hippo_settings_finalize(GObject *object)
{
    HippoSettings *settings = HIPPO_SETTINGS(object);

    g_hash_table_destroy(settings->requests);
    g_hash_table_destroy(settings->entries);
    
    G_OBJECT_CLASS(hippo_settings_parent_class)->finalize(object);
}

HippoSettings*
hippo_settings_new(HippoConnection *connection)
{
    HippoSettings *settings;

    settings = g_object_new(HIPPO_TYPE_SETTINGS,
                            NULL);

    /* Note that the connection refs the settings in the normal
     * hippo_settings_get_and_ref case, so we can't ref the
     * connection
     */
    settings->connection = connection;

    settings->entries = g_hash_table_new_full(g_str_hash, g_str_equal, NULL, (GFreeFunc) cache_entry_free);
    settings->requests = g_hash_table_new_full(g_str_hash, g_str_equal, NULL, (GFreeFunc) request_entry_free);

    g_signal_connect(G_OBJECT(connection), "setting-changed", G_CALLBACK(on_setting_changed), settings);

    /* FIXME this ends up happening at the wrong time (the first time someone
     * needs to use HippoSettings) instead of at application startup
     */
    hippo_connection_request_desktop_settings(settings->connection);
    
    return settings;
}

HippoSettings*
hippo_settings_get_and_ref(HippoConnection  *connection)
{
    HippoSettings *settings;

    settings = g_object_get_data(G_OBJECT(connection), "hippo-settings");

    if (settings == NULL) {
        settings = hippo_settings_new(connection);
        
        g_object_set_data_full(G_OBJECT(connection), "hippo-settings", settings,
                               (GDestroyNotify) g_object_unref);
    }

    g_object_ref(settings);
    
    return settings;
}

void
hippo_settings_set(HippoSettings    *settings,
                   const char       *key,
                   const char       *value)
{
    g_debug("setting: %s=%s", key, value ? value : "(null)");
    
    hippo_connection_send_desktop_setting(settings->connection, key, value);

    /* note that if the server side ever short-circuits notification if the
     * value is unchanged, we would need to also skip this stuff if the value
     * is unchanged, or add the new value to the reply to the "set" IQ
     */
    
    /* don't return a stale value */
    invalidate_cache(settings, key);
    /* mark that we expect notification of the change to come back */
    mark_request_pending(settings, key, NULL, NULL);
}

void
hippo_settings_get(HippoSettings           *settings,
                   const char              *key,
                   HippoSettingArrivedFunc  func,
                   void                    *data)
{
    CacheEntry *entry;

    entry = g_hash_table_lookup(settings->entries, key);

    if (entry != NULL) {
        g_debug("sync getting: %s=%s", entry->key, entry->value ? entry->value : "(null)");
        (* func) (entry->key, entry->value, data);
    } else {
        hippo_connection_request_desktop_setting(settings->connection, key);
        mark_request_pending(settings, key, func, data);
    }
}

