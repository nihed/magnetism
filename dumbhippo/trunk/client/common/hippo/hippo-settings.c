/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-settings.h"
#include "hippo-connection.h"
#include <string.h>


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
on_setting_changed(HippoConnection *connection,
                   const char      *key,
                   const char      *value,
                   void            *data)
{
    HippoSettings *settings = HIPPO_SETTINGS(data);

    g_debug("setting: %s=%s", key, value ? value : "(null)");
    
    if (value == NULL) {
        g_hash_table_remove(settings->entries, key);
    } else {
        g_hash_table_replace(settings->entries,
                             g_strdup(key),
                             g_strdup(value));
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

    g_hash_table_destroy(settings->entries);
    
    G_OBJECT_CLASS(hippo_settings_parent_class)->finalize(object);
}

HippoSettings*
hippo_settings_new(HippoConnection *connection)
{
    HippoSettings *settings;

    settings = g_object_new(HIPPO_TYPE_SETTINGS,
                            NULL);

    g_object_ref(connection);
    settings->connection = connection;

    settings->entries = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, g_free);

    g_signal_connect(G_OBJECT(settings), "setting-changed", G_CALLBACK(on_setting_changed), settings);
    
    hippo_connection_request_desktop_settings(settings->connection);
    
    return settings;
}
