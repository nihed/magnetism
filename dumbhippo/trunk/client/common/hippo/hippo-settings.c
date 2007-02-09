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

static void      hippo_settings_init                (HippoSettings       *settings);
static void      hippo_settings_class_init          (HippoSettingsClass  *klass);

static void      hippo_settings_dispose             (GObject            *object);
static void      hippo_settings_finalize            (GObject            *object);

struct _HippoSettings {
    GObject parent;
    HippoConnection *connection;

    GHashTable *entries;
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
cache_entry_free(CacheEntry *entry)
{
    g_free(entry->key);
    g_free(entry->value);
    g_free(entry);
}

static void
invalidate_cache(HippoSettings *settings,
                 const char    *key)
{
    g_hash_table_remove(settings->entries, key);
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

    g_signal_connect(G_OBJECT(connection), "setting-changed", G_CALLBACK(on_setting_changed), settings);
    
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
    /* don't return a stale value */
    invalidate_cache(settings, key);
}

const char*
hippo_settings_get(HippoSettings    *settings,
                   const char       *key)
{
    CacheEntry *entry;

    entry = g_hash_table_lookup(settings->entries, key);

    if (entry != NULL) {
        g_debug("getting: %s=%s", entry->key, entry->value ? entry->value : "(null)");
        return entry->value; /* may be NULL */
    } else {
        /* FIXME We need to synchronously get the value from the server
         */

        
        return NULL;
    }
}

