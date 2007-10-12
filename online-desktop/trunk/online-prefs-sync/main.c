/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <glib.h>
#include <gconf/gconf-client.h>
#include "hippo-dbus-helper.h"

/* Sync tasks are in a hash by gconf key. This means we can
 * only have 1 pending sync in 1 direction per key at a time.
 * So if e.g. we get a change notify from gconf, then before
 * we sync, one from online, the online one wins. Similarly if
 * we get one from online, then one locally from gconf.
 * This is a little arbitrary, but in practice should not come
 * up. The main reason we have only 1 pending at a time is
 * simply to compress any multiple notifications and avoid
 * queueing up tons of them in pathological cases.
 */

typedef enum {
    PREFS_SYNC_FROM_GCONF_TO_ONLINE,
    PREFS_SYNC_FROM_ONLINE_TO_GCONF
} PrefsSyncTaskType;

typedef struct {
    PrefsSyncTaskType type;
    char *gconf_key;
    GConfValue *gconf_value;
} PrefsSyncTask;

typedef struct {
    HippoDBusProxy *proxy;
    gboolean ready;
    GHashTable *sync_tasks;
    guint sync_idle_id;
} PrefsManager;

static PrefsManager *global_manager = NULL;

/* the only point of this is to avoid the refcount from gconf_client_get_default() */
static GConfClient    *global_gconf_client;
static GConfClient*
get_gconf(void)
{
    if (global_gconf_client == NULL) {
        global_gconf_client = gconf_client_get_default();
    }
    return global_gconf_client;
}

typedef union
{
    dbus_int16_t  i16;   /**< as int16 */
    dbus_uint16_t u16;   /**< as int16 */
    dbus_int32_t  i32;   /**< as int32 */
    dbus_uint32_t u32;   /**< as int32 */
    dbus_int64_t  i64;   /**< as int64 */
    dbus_uint64_t u64;   /**< as int64 */
    double dbl;          /**< as double */
    unsigned char byt;   /**< as byte */
    const char *str;     /**< as char* */
    dbus_bool_t bl;
} VariantUnion;

/* can return NULL if conversion is not possible */
static GConfValue*
gconf_value_from_dbus(DBusMessageIter *iter)
{
    VariantUnion v_GENERIC;
    GConfValue *gconf_value;    

    gconf_value = NULL;
    switch (dbus_message_iter_get_arg_type(iter)) {
    case DBUS_TYPE_BYTE:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.byt);
        break;
    case DBUS_TYPE_INT16:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.i16);
        break;        
    case DBUS_TYPE_UINT16:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.u16);
        break;        
    case DBUS_TYPE_INT32:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.i32);
        break;        
    case DBUS_TYPE_UINT32:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.u32);
        break;        
    case DBUS_TYPE_INT64:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.i64);
        break;        
    case DBUS_TYPE_UINT64:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_INT);
        gconf_value_set_int(gconf_value, v_GENERIC.u64);
        break;
    case DBUS_TYPE_BOOLEAN:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_BOOL);
        gconf_value_set_bool(gconf_value, v_GENERIC.bl);
        break;        
    case DBUS_TYPE_DOUBLE:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_FLOAT);
        gconf_value_set_float(gconf_value, v_GENERIC.dbl);
        break;        
    case DBUS_TYPE_STRING:
        dbus_message_iter_get_basic(iter, &v_GENERIC);
        gconf_value = gconf_value_new(GCONF_VALUE_STRING);
        gconf_value_set_string(gconf_value, v_GENERIC.str);
        break;                
    case DBUS_TYPE_ARRAY:
        /* FIXME */
        break;        
    default:
        /* FIXME whine or something */
        break;
    }

    return gconf_value;
}

static GConfValue*
gconf_value_from_dbus_variant(DBusMessageIter *iter)
{
    DBusMessageIter variant_iter;
    
    if (dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_VARIANT)
        return NULL;

    dbus_message_iter_recurse(iter, &variant_iter);

    return gconf_value_from_dbus(&variant_iter);
}

static dbus_bool_t
write_gconf_value_to_dbus(GConfValue      *value,
                          DBusMessageIter *iter)
{
    int dbus_type;
    VariantUnion v_GENERIC;

    dbus_type = DBUS_TYPE_INVALID;
    
    switch (value->type) {
    case GCONF_VALUE_STRING:
        dbus_type = DBUS_TYPE_STRING;
        v_GENERIC.str = gconf_value_get_string(value);
        break;
    case GCONF_VALUE_INT:
        dbus_type = DBUS_TYPE_INT32;
        v_GENERIC.i32 = gconf_value_get_int(value);
        break;
    case GCONF_VALUE_FLOAT:
        dbus_type = DBUS_TYPE_DOUBLE;
        v_GENERIC.dbl = gconf_value_get_float(value);
        break;
    case GCONF_VALUE_BOOL:
        dbus_type = DBUS_TYPE_BOOLEAN;
        v_GENERIC.bl = gconf_value_get_bool(value);
        break;
    case GCONF_VALUE_LIST:
        /* FIXME */
        break;
        
    case GCONF_VALUE_SCHEMA: /* FALL THRU */
    case GCONF_VALUE_PAIR:
    case GCONF_VALUE_INVALID:
        /* do nothing */
        break;
    }

    if (dbus_type == DBUS_TYPE_INVALID)
        return FALSE;

    return dbus_message_iter_append_basic(iter, dbus_type, &v_GENERIC);
}

static PrefsSyncTask*
sync_task_new(PrefsSyncTaskType type,
              const char       *gconf_key,
              GConfValue       *value)
{
    PrefsSyncTask *task;

    task = g_new0(PrefsSyncTask, 1);
    task->type = type;
    task->gconf_key = g_strdup(gconf_key);
    if (value)
        task->gconf_value = gconf_value_copy(value);

    return task;
}

static void
sync_task_free(PrefsSyncTask *task)
{

    if (task->gconf_value)
        gconf_value_free(task->gconf_value);
    g_free(task->gconf_key);
    g_free(task);
}

static dbus_bool_t
task_value_appender(DBusMessage *message,
                    void        *data)
{
    PrefsSyncTask *task = data;
    DBusMessageIter iter, variant_iter;
    char *dbus_key;

    dbus_key = g_strdup_printf("/gconf%s", task->gconf_key);

    g_debug("Setting %s online", dbus_key);
    
    dbus_message_iter_init_append(message, &iter);

    dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &dbus_key);

    g_free(dbus_key);
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_VARIANT, NULL, &variant_iter);
    if (!write_gconf_value_to_dbus(task->gconf_value, &variant_iter)) {
        sync_task_free(task);
        return FALSE;
    }

    dbus_message_iter_close_container(&iter, &variant_iter);

    sync_task_free(task);
    return TRUE;
}

static gboolean
sync_idle(void *data)
{
    PrefsManager *manager = data;
    GList *tasks;
    
    manager->sync_idle_id = 0;

    tasks = g_hash_table_get_values(manager->sync_tasks);
    g_hash_table_steal_all(manager->sync_tasks);
    g_assert(g_hash_table_size(manager->sync_tasks) == 0);

    while (tasks != NULL) {
        PrefsSyncTask *task = tasks->data;
        tasks = g_list_remove(tasks, tasks->data);
        
        switch (task->type) {
        case PREFS_SYNC_FROM_ONLINE_TO_GCONF:
            if (task->gconf_value) {
                g_debug("Setting %s in gconf", task->gconf_key);
                gconf_client_set(get_gconf(), task->gconf_key,
                                 task->gconf_value, NULL);
            } else {
                g_debug("Unsetting %s in gconf", task->gconf_key);
                gconf_client_unset(get_gconf(), task->gconf_key, NULL);
            }

            sync_task_free(task);            
            break;
            
        case PREFS_SYNC_FROM_GCONF_TO_ONLINE:
            if (manager->proxy == NULL && !manager->ready) {
                /* if we can't sync, then put the task back */
                g_hash_table_replace(manager->sync_tasks,
                                     task->gconf_key, task);
            } else {
                /* we could get fancy here and handle errors by putting the
                 * task back to retry later, but don't
                 */
                if (task->gconf_value) {
                    hippo_dbus_proxy_call_method_async_appender(manager->proxy,
                                                                "SetPreference",
                                                                NULL, NULL, NULL,
                                                                task_value_appender,
                                                                task);
                } else {
                    char *dbus_key = g_strdup_printf("/gconf%s", task->gconf_key);

                    g_debug("Unsetting %s online", dbus_key);
                    
                    hippo_dbus_proxy_call_method_async(manager->proxy,
                                                       "UnsetPreference",
                                                       NULL, NULL, NULL,
                                                       DBUS_TYPE_STRING,
                                                       &dbus_key,
                                                       DBUS_TYPE_INVALID);
                    g_free(dbus_key);
                    sync_task_free(task);
                }                                    
            }        
            break;
        }
    }
    
    return FALSE;
}

static void
check_need_sync_idle(PrefsManager *manager)
{
    gboolean need_idle;

    need_idle = g_hash_table_size(manager->sync_tasks) > 0 &&
        manager->ready &&
        manager->sync_idle_id == 0;
    
    if (need_idle) {
        manager->sync_idle_id =
            g_idle_add(sync_idle, manager);
    } else {
        if (manager->sync_idle_id != 0) {
            g_source_remove(manager->sync_idle_id);
            manager->sync_idle_id = 0;
        }
    }
}

static void
manager_add_sync_task(PrefsManager     *manager,
                      PrefsSyncTaskType type,
                      const char       *gconf_key,
                      GConfValue       *value)
{
    PrefsSyncTask *task;

    task = sync_task_new(type, gconf_key, value);
    
    g_hash_table_replace(manager->sync_tasks,
                         task->gconf_key,
                         task);

    check_need_sync_idle(manager);
}

static void
manager_add_entry(PrefsManager *manager,
                  GConfEntry   *entry)
{
    manager_add_sync_task(manager, PREFS_SYNC_FROM_GCONF_TO_ONLINE,
                          entry->key, entry->value);
}

static void
manager_set_ready(PrefsManager *manager,
                  gboolean      is_ready)
{
    if (manager->ready == is_ready)
        return;
    
    manager->ready = is_ready;
    check_need_sync_idle(manager);
}

static void
on_gconf_notify(GConfClient* client,
                guint        cnxn_id,
                GConfEntry  *entry,
                gpointer     user_data)
{
    manager_add_entry(global_manager, entry);
}

static void
on_prefs_manager_ready_changed(DBusConnection *connection,
                               DBusMessage    *message,
                               void           *data)
{
    dbus_bool_t is_ready;

    is_ready = FALSE;
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_BOOLEAN, &is_ready,
                               DBUS_TYPE_INVALID))
        g_warning("wrong args to ReadyChanged signal");
    
    manager_set_ready(global_manager, is_ready);
}

static void
on_get_preference_reply(DBusMessage *reply,
                        void        *data)
{
    const char *key;
    GConfValue *gconf_value;
    char *gconf_key;
    
    key = data;

    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        DBusMessageIter iter;
        
        dbus_message_iter_init(reply, &iter);
        
        /* gconf value may be NULL */
        gconf_value = gconf_value_from_dbus_variant(&iter);
    } else if (dbus_message_is_error(reply, "org.freedesktop.Preferences.Error.NotFound")) {
        gconf_value = NULL;
    } else {
        /* If some other error happens, assume we just don't know what is going on, so do nothing */
        return;
    }

    gconf_key = g_strdup(key + strlen("/gconf"));
    
    manager_add_sync_task(global_manager, PREFS_SYNC_FROM_ONLINE_TO_GCONF,
                          gconf_key, gconf_value);

    g_free(gconf_key);

    if (gconf_value)
        gconf_value_free(gconf_value);
}

static char*
get_dbus_signature_for_gconf_type(GConfValueType gconf_type)
{
    char dbus_signature[2] = { '\0', '\0' };
    
    switch (gconf_type) {
    case GCONF_VALUE_STRING:
        dbus_signature[0] = DBUS_TYPE_STRING;
        break;
    case GCONF_VALUE_INT:
        dbus_signature[0] = DBUS_TYPE_INT32;
        break;
    case GCONF_VALUE_FLOAT:
        dbus_signature[0] = DBUS_TYPE_DOUBLE;
        break;
    case GCONF_VALUE_BOOL:
        dbus_signature[0] = DBUS_TYPE_BOOLEAN;
        break;

    case GCONF_VALUE_LIST:
        /* FIXME */
        break;
        
    case GCONF_VALUE_SCHEMA: /* FALL THRU */
    case GCONF_VALUE_PAIR:
    case GCONF_VALUE_INVALID:
        /* do nothing */
        break;
    }

    if (*dbus_signature)
        return g_strdup(dbus_signature);
    else
        return NULL;
}

static char*
get_dbus_signature_for_gconf_key(const char *gconf_key)
{
    GConfEntry *entry;
    const char *schema_name;
    GConfSchema *schema;
    GConfValueType gconf_type;
    
    entry = gconf_client_get_entry(get_gconf(), gconf_key,
                                   NULL, TRUE, NULL);
    if (entry == NULL)
        return NULL;

    schema_name = gconf_entry_get_schema_name(entry);
    if (schema_name == NULL) {
        gconf_entry_unref(entry);
        return NULL;
    }

    schema = gconf_client_get_schema(get_gconf(), schema_name, NULL);

    if (schema == NULL) {
        gconf_entry_unref(entry);
        return NULL;
    }

    gconf_entry_unref(entry);
    entry = NULL;

    gconf_type = gconf_schema_get_type(schema);

    gconf_schema_free(schema);

    return get_dbus_signature_for_gconf_type(gconf_type);
}

static void
on_prefs_manager_pref_changed(DBusConnection *connection,
                              DBusMessage    *message,
                              void           *data)
{
    const char *key;

    key = NULL;
    if (!dbus_message_get_args(message, NULL, DBUS_TYPE_STRING, &key, DBUS_TYPE_INVALID)) {
        g_warning("Unexpected signature for PreferenceChanged signal");
        return;
    }

    if (!g_str_has_prefix(key, "/gconf"))
        return;
    
    if (global_manager->proxy) {
        char *signature;
        const char *gconf_key;
        
        gconf_key = key + strlen("/gconf");

        signature = get_dbus_signature_for_gconf_key(gconf_key);

        if (signature != NULL) {
            hippo_dbus_proxy_call_method_async(global_manager->proxy,
                                               "GetPreference",
                                               on_get_preference_reply,
                                               g_strdup(key), g_free,
                                               DBUS_TYPE_STRING, &key,
                                               DBUS_TYPE_SIGNATURE, &signature,
                                               DBUS_TYPE_INVALID);
            g_free(signature);
        }
    }
}

static void
on_get_ready_reply(DBusMessage *reply,
                   void        *data)
{
    dbus_bool_t is_ready;

    /* note that on error we assume ready=FALSE */
    is_ready = FALSE;
    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN &&
        !dbus_message_get_args(reply, NULL,
                               DBUS_TYPE_BOOLEAN, &is_ready,
                               DBUS_TYPE_INVALID))
        g_warning("wrong args to IsReady reply");
    
    manager_set_ready(global_manager, is_ready);    
}

static void
on_prefs_manager_available(DBusConnection *connection,
                           const char     *well_known_name,
                           const char     *unique_name,
                           void           *data)
{
    if (global_manager->proxy == NULL) {
        global_manager->proxy = hippo_dbus_proxy_new(connection,
                                                     unique_name,
                                                     "/org/freedesktop/online_preferences",
                                                     "org.freedesktop.Preferences");
        hippo_dbus_proxy_call_method_async(global_manager->proxy,
                                           "IsReady",
                                           on_get_ready_reply,
                                           NULL, NULL,
                                           DBUS_TYPE_INVALID);
    }
}

static void
on_prefs_manager_unavailable(DBusConnection *connection,
                             const char     *well_known_name,
                             const char     *unique_name,
                             void           *data)
{
    if (global_manager->proxy != NULL) {
        hippo_dbus_proxy_unref(global_manager->proxy);
        global_manager->proxy = NULL;
        global_manager->ready = FALSE;
        check_need_sync_idle(global_manager);
    }
}

static HippoDBusServiceTracker prefs_manager_tracker = {
    0,
    on_prefs_manager_available,
    on_prefs_manager_unavailable
};

static HippoDBusSignalTracker prefs_manager_signals[] = {
    { "org.freedesktop.Preferences",
      "ReadyChanged",
      on_prefs_manager_ready_changed },
    { "org.freedesktop.Preferences",
      "PreferenceChanged",
      on_prefs_manager_pref_changed },
    { NULL, NULL, NULL }
};

static void
on_dbus_connected (DBusConnection *connection,
                   void           *data)
{    
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.OnlinePreferencesManager",
                                               &prefs_manager_tracker,
                                               prefs_manager_signals,
                                               NULL);

}

static void
on_dbus_disconnected(DBusConnection *connection,
                     void           *data)
{
    hippo_dbus_helper_unregister_service_tracker(connection,
                                                 "org.freedesktop.OnlinePreferencesManager",
                                                 &prefs_manager_tracker,
                                                 NULL);
}

static HippoDBusConnectionTracker connection_tracker = {
    on_dbus_connected,
    on_dbus_disconnected
};


static void
print_debug_func(const char *message)
{
    g_printerr("%s\n", message);
}

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    
    g_type_init();
    
    g_set_application_name("Online Prefs Sync");

    /* FIXME try to own a bus name to keep ourselves single-instance */
    
    global_manager = g_new0(PrefsManager, 1);
    global_manager->sync_tasks = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                       NULL, (GFreeFunc) sync_task_free);
    
    gconf_client_add_dir(get_gconf(), "/",
                         GCONF_CLIENT_PRELOAD_NONE,
                         NULL);

    gconf_client_notify_add(get_gconf(), "/",
                            on_gconf_notify,
                            NULL, NULL, NULL);
    
    hippo_dbus_helper_register_connection_tracker(DBUS_BUS_SESSION,
                                                  &connection_tracker, NULL);
    
    loop = g_main_loop_new(NULL, FALSE);

    g_main_loop_run(loop);

    g_main_loop_unref(loop);

    g_free(global_manager);
    global_manager = NULL;
    
    return 0;
}
