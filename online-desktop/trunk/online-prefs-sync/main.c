/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <glib.h>
#include <gconf/gconf-client.h>
#include "hippo-dbus-helper.h"
#include "whitelist.h"

/* The prefs sync is not "bidirectional"; the way it works is that
 * the server copy is always the master copy, and wins. However,
 * if this daemon sees you change anything locally, it copies that
 * to the server. If this daemon does not see a change, then
 * the server setting will overwrite next time you log in...
 */

/* To avoid infinite loops this code relies on both the
 * OnlinePrefsManager provider and GConfClient short-circuiting change
 * notifications when nothing really changed. This has some dangers,
 * e.g. if we could not round-trip a value properly without changing it,
 * then it might start toggling back and forth over and over... we'll
 * cross that bridge if we come to it.
 */

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

/* The "enabled" flag works as follows:
 *  - on each machine we have a gconf setting for whether to sync, allowing
 *    some machines to skip syncing
 *  - if we aren't enabled, we still do all the work except we never
 *    run the sync idle. This means if you ever toggle on the "enabled"
 *    flag we have a backlog of stuff to be synced that immediately goes through.
 *  - there is no server-side flag for this; the server always stores settings,
 *    it's just a question of whether a given machine pays attention to them
 */
#define ENABLED_GCONF_KEY "/apps/online-prefs-sync/enabled"

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
    GMainLoop *loop;
    guint enabled : 1;
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

static gboolean
gconf_key_to_dbus_key(const char *gconf_key,
                      char      **dbus_key)
{
    KeyScope scope;

    scope = whitelist_get_key_scope(gconf_key);

    *dbus_key = NULL;

    if (scope == KEY_SCOPE_NOT_SAVED_REMOTELY) {
        return FALSE;
    } else if (scope == KEY_SCOPE_SAVED_PER_USER) {
        *dbus_key = g_strdup_printf("/gconf%s", gconf_key);
        return TRUE;
    } else if (scope == KEY_SCOPE_SAVED_PER_MACHINE) {
        char *machine_id;
        machine_id = dbus_get_local_machine_id();
        *dbus_key = g_strdup_printf("/gconf-%s%s",
                                    machine_id, gconf_key);
        g_free(machine_id);
        return TRUE;
    } else {
        g_assert_not_reached();
        return FALSE;
    }
}

static gboolean
dbus_key_to_gconf_key(const char *dbus_key,
                      char      **gconf_key)
{
    *gconf_key = NULL;
    
    if (g_str_has_prefix(dbus_key, "/gconf/")) {
        *gconf_key = g_strdup(dbus_key + strlen("/gconf"));

        if (whitelist_get_key_scope(*gconf_key) != KEY_SCOPE_SAVED_PER_USER) {
            g_free(*gconf_key);
            *gconf_key = NULL;
            return FALSE;
        } else {
            return TRUE;
        }
    } else if (g_str_has_prefix(dbus_key, "/gconf-")) {
        const char *slash;
        const char *machine_id_start;
        char *key_machine_id;
        char *machine_id;
        gboolean machine_id_matches;
        
        slash = strchr(dbus_key+1, '/');
        if (slash == NULL)
            return FALSE;

        machine_id_start = dbus_key + strlen("/gconf-");
        key_machine_id = g_strndup(machine_id_start,
                                   slash - machine_id_start);
        machine_id = dbus_get_local_machine_id();
        machine_id_matches = FALSE;
        if (strcmp(key_machine_id, machine_id) == 0) {
            machine_id_matches = TRUE;
        }
        g_free(machine_id);
        g_free(key_machine_id);

        if (machine_id_matches) {
            *gconf_key = g_strdup(slash);
            return TRUE;
        } else {
            return FALSE;
        }
    } else {
        return FALSE;
    }
}

static GConfValueType
get_gconf_type_for_dbus_type(int dbus_type)
{
    switch (dbus_type) {
    case DBUS_TYPE_BYTE:
    case DBUS_TYPE_INT16:
    case DBUS_TYPE_UINT16:
    case DBUS_TYPE_INT32:
    case DBUS_TYPE_UINT32:
    case DBUS_TYPE_INT64:
    case DBUS_TYPE_UINT64:
        return GCONF_VALUE_INT;

    case DBUS_TYPE_BOOLEAN:
        return GCONF_VALUE_BOOL;
        
    case DBUS_TYPE_DOUBLE:
        return GCONF_VALUE_FLOAT;
        
    case DBUS_TYPE_STRING:
        return GCONF_VALUE_STRING;
        
    case DBUS_TYPE_ARRAY:
        return GCONF_VALUE_LIST;
    }
    
    return GCONF_VALUE_INVALID;
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
    GConfValueType gconf_type;

    gconf_type = get_gconf_type_for_dbus_type(dbus_message_iter_get_arg_type(iter));
    if (gconf_type == GCONF_VALUE_INVALID)
        return NULL;
    
    gconf_value = gconf_value_new(gconf_type);

    if (gconf_type != GCONF_VALUE_LIST)
        dbus_message_iter_get_basic(iter, &v_GENERIC);
    
    switch (dbus_message_iter_get_arg_type(iter)) {
    case DBUS_TYPE_BYTE:
        gconf_value_set_int(gconf_value, v_GENERIC.byt);
        break;
    case DBUS_TYPE_INT16:
        gconf_value_set_int(gconf_value, v_GENERIC.i16);
        break;        
    case DBUS_TYPE_UINT16:
        gconf_value_set_int(gconf_value, v_GENERIC.u16);
        break;        
    case DBUS_TYPE_INT32:
        gconf_value_set_int(gconf_value, v_GENERIC.i32);
        break;        
    case DBUS_TYPE_UINT32:
        gconf_value_set_int(gconf_value, v_GENERIC.u32);
        break;        
    case DBUS_TYPE_INT64:
        gconf_value_set_int(gconf_value, v_GENERIC.i64);
        break;        
    case DBUS_TYPE_UINT64:
        gconf_value_set_int(gconf_value, v_GENERIC.u64);
        break;
    case DBUS_TYPE_BOOLEAN:
        gconf_value_set_bool(gconf_value, v_GENERIC.bl);
        break;        
    case DBUS_TYPE_DOUBLE:
        gconf_value_set_float(gconf_value, v_GENERIC.dbl);
        break;        
    case DBUS_TYPE_STRING:
        gconf_value_set_string(gconf_value, v_GENERIC.str);
        break;                

    case DBUS_TYPE_ARRAY:
        {
            GConfValueType list_type;

            list_type = get_gconf_type_for_dbus_type(dbus_message_iter_get_element_type(iter));
            if (list_type == GCONF_VALUE_INVALID ||
                list_type == GCONF_VALUE_LIST) {
                /* No good; array is of some kind of complex type */
                gconf_value_free(gconf_value);
                gconf_value = NULL;
            } else {
                GSList *list;
                DBusMessageIter array_iter;
                gboolean failed_conversion;
                
                list = NULL;
                failed_conversion = FALSE;
            
                dbus_message_iter_recurse(iter, &array_iter);
                while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
                    GConfValue *elem;

                    elem = gconf_value_from_dbus(&array_iter);
                    if (elem == NULL) {
                        failed_conversion = TRUE;
                        break;
                    }
                    list = g_slist_prepend(list, elem);
                    
                    dbus_message_iter_next(&array_iter);
                }
                list = g_slist_reverse(list); /* put it back in order */
                
                if (failed_conversion) {
                    /* should never happen */
                    g_warning("Failed to convert an element of dbus array to a GConfValue");
                    g_slist_foreach(list, (GFunc) gconf_value_free, NULL);
                    g_slist_free(list);
                    gconf_value_free(gconf_value);
                    gconf_value = NULL;
                } else {
                    gconf_value_set_list_type(gconf_value, list_type);
                    gconf_value_set_list_nocopy(gconf_value, list);
                }
            }
        }
        break;
    default:
        /* FIXME we could in theory handle variants, though a variant containing a variant is pretty f'd up */
        g_debug("Unable to convert complex dbus type to a gconf type");
        break;
    }

    return gconf_value;
}

static int
get_dbus_type_for_gconf_type(GConfValueType gconf_type)
{
    switch (gconf_type) {
    case GCONF_VALUE_STRING:
        return DBUS_TYPE_STRING;

    case GCONF_VALUE_INT:
        return DBUS_TYPE_INT32;

    case GCONF_VALUE_FLOAT:
        return DBUS_TYPE_DOUBLE;

    case GCONF_VALUE_BOOL:
        return DBUS_TYPE_BOOLEAN;

    case GCONF_VALUE_LIST:
        return DBUS_TYPE_ARRAY;
        
    case GCONF_VALUE_SCHEMA: /* FALL THRU */
    case GCONF_VALUE_PAIR:
    case GCONF_VALUE_INVALID:
        return DBUS_TYPE_INVALID;
    }

    return DBUS_TYPE_INVALID;
}

static char*
get_dbus_signature_for_gconf_types(GConfValueType gconf_type,
                                   GConfValueType gconf_list_type)
{
    char dbus_signature[3] = { '\0', '\0', '\0' };

    dbus_signature[0] = get_dbus_type_for_gconf_type(gconf_type);
    if (dbus_signature[0] == DBUS_TYPE_ARRAY) {
        dbus_signature[1] = get_dbus_type_for_gconf_type(gconf_list_type);
    }

    if (*dbus_signature)
        return g_strdup(dbus_signature);
    else
        return NULL;
}

static char*
get_dbus_signature_for_gconf_value(GConfValue *value)
{
    if (value == NULL)
        return NULL;
    else if (value->type == GCONF_VALUE_LIST)
        return get_dbus_signature_for_gconf_types(value->type,
                                                  gconf_value_get_list_type(value));
    else
        return get_dbus_signature_for_gconf_types(value->type, GCONF_VALUE_INVALID);    
}

static char*
get_dbus_signature_for_gconf_key(const char *gconf_key)
{
    GConfEntry *entry;
    const char *schema_name;
    GConfSchema *schema;
    GConfValueType gconf_type;
    GConfValueType gconf_list_type;
    
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
    gconf_list_type = GCONF_VALUE_INVALID;
    if (gconf_type == GCONF_VALUE_LIST)
        gconf_list_type = gconf_schema_get_list_type(schema);
    
    gconf_schema_free(schema);

    return get_dbus_signature_for_gconf_types(gconf_type, gconf_list_type);
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
        {
            char array_element[2] = { '\0', '\0' };
            DBusMessageIter array_iter;
            GSList *l;
            
            dbus_type = DBUS_TYPE_ARRAY;
            array_element[0] = get_dbus_type_for_gconf_type(gconf_value_get_list_type(value));
            dbus_message_iter_open_container(iter, DBUS_TYPE_ARRAY, array_element, &array_iter);

            for (l = gconf_value_get_list(value); l != NULL; l = l->next) {
                GConfValue *elem = l->data;
                write_gconf_value_to_dbus(elem, &array_iter);
            }
            dbus_message_iter_close_container(iter, &array_iter);
        }
        break;
        
    case GCONF_VALUE_SCHEMA: /* FALL THRU */
    case GCONF_VALUE_PAIR:
    case GCONF_VALUE_INVALID:
        /* do nothing */
        break;
    }

    if (dbus_type == DBUS_TYPE_INVALID)
        return FALSE;

    if (dbus_type != DBUS_TYPE_ARRAY)
        return dbus_message_iter_append_basic(iter, dbus_type, &v_GENERIC);
    else
        return TRUE;
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
    char *signature;

    if (!gconf_key_to_dbus_key(task->gconf_key, &dbus_key)) {
        g_debug("gconf key '%s' is not saved online", task->gconf_key);
        sync_task_free(task);
        return FALSE;
    }

    signature = get_dbus_signature_for_gconf_value(task->gconf_value);
    if (signature == NULL) {
        g_debug("gconf key '%s' cannot be represented as a dbus type",
                task->gconf_key);
        sync_task_free(task);
        g_free(dbus_key);
        return FALSE;
    }
    
    g_debug("Setting %s online", dbus_key);
    
    dbus_message_iter_init_append(message, &iter);

    dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &dbus_key);

    g_free(dbus_key);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_VARIANT, signature, &variant_iter);
    g_free(signature);
    
    if (!write_gconf_value_to_dbus(task->gconf_value, &variant_iter)) {
        sync_task_free(task);
        return FALSE;
    }

    dbus_message_iter_close_container(&iter, &variant_iter);

    sync_task_free(task);
    return TRUE;
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

static gboolean
sync_idle(void *data)
{
    PrefsManager *manager = data;
    GList *tasks;
    
    manager->sync_idle_id = 0;

    tasks = hash_table_get_values(manager->sync_tasks);
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
                    char *dbus_key;

                    if (gconf_key_to_dbus_key(task->gconf_key, &dbus_key)) {
                        g_debug("Unsetting %s online", dbus_key);
                        
                        hippo_dbus_proxy_call_method_async(manager->proxy,
                                                           "UnsetPreference",
                                                           NULL, NULL, NULL,
                                                           DBUS_TYPE_STRING,
                                                           &dbus_key,
                                                           DBUS_TYPE_INVALID);
                        g_free(dbus_key);
                    } else {
                        g_debug("gconf key '%s' is not saved online", task->gconf_key);
                    }
                    
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

    need_idle = manager->enabled && manager->ready &&
        manager->sync_idle_id == 0 &&
        g_hash_table_size(manager->sync_tasks) > 0;
    
    if (need_idle) {
        /* timeout instead of idle is a little extra insurance vs. infinite loops */
        manager->sync_idle_id =
            g_timeout_add(2000, sync_idle, manager);
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

    if (type == PREFS_SYNC_FROM_GCONF_TO_ONLINE)
        g_debug("Will sync from gconf %s to online prefs storage", gconf_key);
    else if (type == PREFS_SYNC_FROM_ONLINE_TO_GCONF)
        g_debug("Will sync from online prefs storage to gconf %s", gconf_key);
    else
        g_debug("Unknown sync task???");
    
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

    if (dbus_key_to_gconf_key(key, &gconf_key)) {
        manager_add_sync_task(global_manager, PREFS_SYNC_FROM_ONLINE_TO_GCONF,
                              gconf_key, gconf_value);
        
        g_free(gconf_key);
    } else {
        g_debug("online key '%s' does not map to a gconf key on this machine",
                key);
    }
    
    if (gconf_value)
        gconf_value_free(gconf_value);
}

static void
request_new_value_of_pref(PrefsManager *manager,
                          const char   *dbus_key)
{
    if (manager->proxy) {
        char *gconf_key;

        if (dbus_key_to_gconf_key(dbus_key, &gconf_key)) {
            char *signature;

            signature = get_dbus_signature_for_gconf_key(gconf_key);

            if (signature != NULL) {
                g_debug("Requesting value of %s", dbus_key);
                
                hippo_dbus_proxy_call_method_async(manager->proxy,
                                                   "GetPreference",
                                                   on_get_preference_reply,
                                                   g_strdup(dbus_key), g_free,
                                                   DBUS_TYPE_STRING, &dbus_key,
                                                   DBUS_TYPE_SIGNATURE, &signature,
                                                   DBUS_TYPE_INVALID);
                g_free(signature);
            } else {
                g_debug("Not syncing gconf key '%s' since we can't figure out its expected type (no schema?)",
                        gconf_key);
            }
            
            g_free(gconf_key);
        } else {
            g_debug("online key '%s' does not map to a gconf key on this machine",
                    dbus_key);
        }
    }
}

static void
on_get_all_names_reply(DBusMessage *reply,
                       void        *data)
{
    PrefsManager *manager;

    manager = data;

    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        DBusMessageIter iter, array_iter;

        if (!dbus_message_has_signature(reply, "as")) {
            g_warning("Wrong signature in method reply to GetAllPreferenceNames");
            return;
        }
        
        dbus_message_iter_init(reply, &iter);
        dbus_message_iter_recurse(&iter, &array_iter);
        while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
            const char *key;

            key = NULL;
            dbus_message_iter_get_basic(&array_iter, &key);

            request_new_value_of_pref(manager, key);

            dbus_message_iter_next(&array_iter);
        }
    } else {
        /* Some error, these are unavoidable due to races; should work out fine since we'll retry
         * later after the prefs manager reappears
         */
        return;
    }
}

static void
manager_set_ready(PrefsManager *manager,
                  gboolean      is_ready)
{
    if (manager->ready == is_ready)
        return;
    
    manager->ready = is_ready;
    check_need_sync_idle(manager);

    if (manager->ready) {
        /* Get all the known pref names and sync them */
        hippo_dbus_proxy_call_method_async(manager->proxy,
                                           "GetAllPreferenceNames",
                                           on_get_all_names_reply,
                                           manager, NULL,
                                           DBUS_TYPE_INVALID);
    }
}

static void
on_gconf_notify(GConfClient* client,
                guint        cnxn_id,
                GConfEntry  *entry,
                gpointer     user_data)
{
    if (strcmp(entry->key, ENABLED_GCONF_KEY) == 0) {
        gboolean now_enabled;
        
        if (entry->value == NULL || entry->value->type != GCONF_VALUE_BOOL)
            now_enabled = FALSE;
        else
            now_enabled = gconf_value_get_bool(entry->value);

        g_debug("Got change notify for %s, was enabled %d now enabled %d",
                entry->key, global_manager->enabled, now_enabled);

        if (global_manager->enabled != now_enabled) { 
            global_manager->enabled = now_enabled;
            
            check_need_sync_idle(global_manager);
        }
    } else {
        g_debug("Got change notify for gconf key %s", entry->key);
        
        manager_add_entry(global_manager, entry);
    }
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

    g_debug("Got PreferenceChanged for online key %s", key);
    
    request_new_value_of_pref(global_manager, key);
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
on_own_bus_name (DBusConnection *connection,
                 void           *data)
{
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.OnlinePreferencesManager",
                                               &prefs_manager_tracker,
                                               prefs_manager_signals,
                                               NULL);
}

static void
on_do_not_own_bus_name(DBusConnection *connection,
                       void           *data)
{
    hippo_dbus_helper_unregister_service_tracker(connection,
                                                 "org.freedesktop.OnlinePreferencesManager",
                                                 &prefs_manager_tracker,
                                                 NULL);

    g_main_loop_quit(global_manager->loop);
}

static HippoDBusNameOwner single_instance_owner = {
    on_own_bus_name,
    on_do_not_own_bus_name
};

static void
on_dbus_connected (DBusConnection *connection,
                   void           *data)
{
    hippo_dbus_helper_register_name_owner(connection,
                                          "org.gnome.OnlinePrefsSync",
                                          HIPPO_DBUS_NAME_SINGLE_INSTANCE,
                                          &single_instance_owner,
                                          NULL);
}

static void
on_dbus_disconnected(DBusConnection *connection,
                     void           *data)
{    
    hippo_dbus_helper_unregister_name_owner(connection,
                                            "org.gnome.OnlinePrefsSync",
                                            &single_instance_owner,
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
    g_type_init();
    
    g_set_application_name("Online Prefs Sync");
   
    global_manager = g_new0(PrefsManager, 1);
    global_manager->sync_tasks = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                       NULL, (GFreeFunc) sync_task_free);

    global_manager->enabled = gconf_client_get_bool(get_gconf(),
                                                    ENABLED_GCONF_KEY,
                                                    NULL);
    g_debug("Online prefs sync enabled = %d", global_manager->enabled);
    
    gconf_client_add_dir(get_gconf(), "/",
                         GCONF_CLIENT_PRELOAD_NONE,
                         NULL);

    gconf_client_notify_add(get_gconf(), "/",
                            on_gconf_notify,
                            NULL, NULL, NULL);
    
    hippo_dbus_helper_register_connection_tracker(DBUS_BUS_SESSION,
                                                  &connection_tracker, NULL);
    
    global_manager->loop = g_main_loop_new(NULL, FALSE);

    g_main_loop_run(global_manager->loop);

    g_main_loop_unref(global_manager->loop);

    g_free(global_manager);
    global_manager = NULL;
    
    return 0;
}
