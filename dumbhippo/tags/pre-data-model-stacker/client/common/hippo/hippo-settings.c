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
    guint ready : 1;
    guint everything_loaded_since_connect : 1;
};

struct _HippoSettingsClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoSettings, hippo_settings, G_TYPE_OBJECT);

enum {
    READY_CHANGED,
    SETTING_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];


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

    signals[READY_CHANGED] =
        g_signal_new ("ready-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__BOOLEAN,
                      G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
    signals[SETTING_CHANGED] =
        g_signal_new ("setting-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__STRING,
                      G_TYPE_NONE, 1, G_TYPE_STRING);    
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
    gboolean unchanged;
    
    g_debug("caching: %s=%s", key, value ? value : "(null)");

    unchanged = FALSE;
    
    entry = g_hash_table_lookup(settings->entries, key);
    if (entry == NULL) {
        entry = g_new0(CacheEntry, 1);
        entry->key = g_strdup(key);
        entry->value = g_strdup(value);
        g_hash_table_replace(settings->entries, entry->key, entry);
    } else {

        unchanged = ((entry->value != NULL) == (value != NULL)) &&
            (value == NULL || strcmp(value, entry->value) == 0);

        if (!unchanged) {
            g_free(entry->value);
            entry->value = g_strdup(value);
        }
    }

    /* notify anyone that was waiting for this new value */
    mark_request_filled(settings, key, value);

    /* send out a signal if it changed - the short-circuit is good
     * since we convert this signal to a dbus signal also
     */
    if (!unchanged)
        g_signal_emit(settings, signals[SETTING_CHANGED], 0, key);
}

static void
on_settings_loaded(HippoConnection *connection,
                   void            *data)
{
    HippoSettings *settings = HIPPO_SETTINGS(data);

    settings->everything_loaded_since_connect = TRUE;
    
    /* We report ourselves as "ready" if we've ever successfully
     * loaded settings.  We remain "ready" even if the connection to
     * the server is disconnected right now.
     */
    if (!settings->ready) {
        settings->ready = TRUE;
        g_signal_emit(settings, signals[READY_CHANGED], 0, TRUE);
    }

    /* FIXME any pending requests at this point can be completed */
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
on_connected_changed(HippoConnection *connection,
                     gboolean         connected,
                     void            *data)
{
    HippoSettings *settings = HIPPO_SETTINGS(data);

    if (!connected) {
        settings->everything_loaded_since_connect = FALSE;
    }
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
    g_signal_connect(G_OBJECT(connection), "settings-loaded", G_CALLBACK(on_settings_loaded), settings);
    g_signal_connect(G_OBJECT(connection), "connected-changed", G_CALLBACK(on_connected_changed), settings);

    /* FIXME this ends up happening at the wrong time (the first time someone
     * needs to use HippoSettings) instead of at application startup.
     */
    /* FIXME we might drop all cache state on reconnecting to the server since
     * we may have missed change notifications
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
    CacheEntry *entry;
    gboolean unchanged;
    
    g_debug("setting: %s=%s", key, value ? value : "(null)");

    /* this short-circuit is intended to help avoid infinite loops and excess
     * server traffic when things go awry
     */
    entry = g_hash_table_lookup(settings->entries, key);
    if (entry != NULL) {
        unchanged = ((entry->value != NULL) == (value != NULL)) &&
            (value == NULL || strcmp(value, entry->value) == 0);
    } else {
        unchanged = (value == NULL);
    }
    
    if (unchanged) {
        g_debug("no change to setting, not sending to server");
        return;
    }
    
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
        if (settings->everything_loaded_since_connect) {
            g_debug("we should have any existing settings in cache, not asking for %s", key);
            (* func) (key, NULL, data);
        } else {
            /* FIXME This is just dangerous, really - if it ever happens on a large scale then
             * the server will be doomed. Probably we should only allow getting *all* settings
             * in bulk since that's the only sane thing.
             */
            
            hippo_connection_request_desktop_setting(settings->connection, key);
            mark_request_pending(settings, key, func, data);
        }
    }
}

gboolean
hippo_settings_get_ready(HippoSettings *settings)
{
    return settings->ready;
}

static void
get_values_foreach(gpointer       key,
                   gpointer       value,
                   gpointer       data)
{
    GList **values = data;
    *values = g_list_prepend(*values, value);
}

/* As of GLib-2.14, GLib has g_hash_table_get_values()
 */
static GList *
hash_table_get_values(GHashTable *hash)
{
    GList *values = NULL;
    
    g_hash_table_foreach(hash, get_values_foreach, &values);

    return values;
}

char**
hippo_settings_get_all_names(HippoSettings *settings)
{
    GList *entries;
    char **names;
    int names_len;
    int i;
    
    entries = hash_table_get_values(settings->entries);

    names_len = g_list_length(entries);

    names = g_new0(char*, names_len + 1);

    i = 0;
    while (entries != NULL) {
        CacheEntry *entry = entries->data;

        entries = g_list_remove(entries, entries->data);

        names[i] = g_strdup(entry->key);
        ++i;
    }

    return names;
}
