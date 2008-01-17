/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-local.h"
#include "hippo-im.h"

#define MUGSHOT_INFO_NAME "org.mugshot.Mugshot"
#define STANDARD_INFO_NAME "org.freedesktop.od.Standard"

#define LOCAL_RESOURCE_BASE "online-desktop:/o/local-user"

typedef struct {
    char *session_id;
    char *user_resource_id;
    char *name;
    char *webdav_url;
} LocalBuddy;

typedef struct {
    const char *key;
    const char *value;
    gboolean required;
} DictStringEntry;

static GHashTable *local_buddies = NULL;
static char *local_export_unique_name = NULL;
static gboolean have_locally_exported = FALSE;

static gboolean
dict_extract_string_values(const DBusMessageIter *orig_dict_iter,
                           DictStringEntry       *entries)
{
    DBusMessageIter dict_iter;

    {
        int i;
        for (i = 0; entries[i].key != NULL; ++i) {
            entries[i].value = NULL;
        }
    }
    
    dict_iter = *orig_dict_iter;
    while (dbus_message_iter_get_arg_type(&dict_iter) == DBUS_TYPE_DICT_ENTRY) {
        DBusMessageIter entry_iter;
        DBusMessageIter variant_iter;
        const char *key;
        const char *value;
        
        dbus_message_iter_recurse(&dict_iter, &entry_iter);

        key = NULL;
        dbus_message_iter_get_basic(&entry_iter, &key);
        dbus_message_iter_next(&entry_iter);

        g_assert(dbus_message_iter_get_arg_type(&entry_iter) == DBUS_TYPE_VARIANT);
        dbus_message_iter_recurse(&entry_iter, &variant_iter);

        if (dbus_message_iter_get_arg_type(&variant_iter) == DBUS_TYPE_STRING) {
            dbus_message_iter_get_basic(&variant_iter, &value);
        } else {
            value = NULL;
        }
        
        if (value != NULL) {
            int i;
            for (i = 0; entries[i].key != NULL; ++i) {
                if (strcmp(key, entries[i].key) == 0) {
                    entries[i].value = value;
                    break;
                }
            }
        }
            
        dbus_message_iter_next(&dict_iter);
    }

    /* We failed if we didn't find all the keys we were expecting */
    {
        int i;
        for (i = 0; entries[i].key != NULL; ++i) {
            if (entries[i].value == NULL && entries[i].required) {
                g_warning("local: Missing property '%s'", entries[i].key);
                return FALSE;
            }
        }
    }
    
    return TRUE;
}

/* We build a resource_id URI around the session ID; to keep things
 * simple, we validate that the session ID is a valid D-DBUS session
 * ID up-front here.
 */
static gboolean
validate_dbus_session_id(const char *id)
{
    const char *p ;

    /* A D-BUS session ID should be exactly 32 hex digits */

    if (strlen(id) != 32)
        return FALSE;

    for (p = id; *p; p++) {
        if (!g_ascii_isxdigit(*p))
            return FALSE;
    }

    return TRUE;
}

static LocalBuddy *
get_local_buddy(const char *session_id)
{
    return g_hash_table_lookup(local_buddies, session_id);
}

static LocalBuddy *
ensure_local_buddy(const char *session_id)
{
    LocalBuddy *local_buddy = g_hash_table_lookup(local_buddies, session_id);
    if (!local_buddy) {
        local_buddy = g_new0(LocalBuddy, 1);
        local_buddy->session_id = g_strdup(session_id);
        g_hash_table_insert(local_buddies, local_buddy->session_id, local_buddy);
    }

    return local_buddy;
}

static void
maybe_remove_local_buddy(const char *session_id)
{
    LocalBuddy *local_buddy = get_local_buddy(session_id);
    if (!local_buddy)
        return;

    if (local_buddy->user_resource_id == NULL && local_buddy->webdav_url == NULL) {
        g_hash_table_remove(local_buddies, session_id);
        g_free(local_buddy->name);
        g_free(local_buddy);
    }
}

static void
update_mugshot_info(const char *session_id,
                    const char *user_resource_id,
                    const char *name)
{
    LocalBuddy *local_buddy = ensure_local_buddy(session_id);

    if (local_buddy->user_resource_id != NULL) {
        g_free(local_buddy->user_resource_id);
        local_buddy->user_resource_id = NULL;
        g_free(local_buddy->name);
        local_buddy->name = NULL;
    }

    local_buddy->user_resource_id = g_strdup(user_resource_id);
    local_buddy->name = g_strdup(name);
}

static void
update_standard_info(const char *session_id,
                     const char *webdav_url)
{
    LocalBuddy *local_buddy = ensure_local_buddy(session_id);

    if (local_buddy->webdav_url != NULL) {
        g_free(local_buddy->webdav_url);
        local_buddy->webdav_url = NULL;
    }

    local_buddy->webdav_url = g_strdup(webdav_url);
}

static char *
make_resource_id(const char *session_id)
{
    return g_strconcat(LOCAL_RESOURCE_BASE "/" , session_id, NULL);
}

static void
update_im_buddy(const char           *session_id)
{
    LocalBuddy *local_buddy = get_local_buddy(session_id);
    char *resource_id = make_resource_id(session_id);

    if (!local_buddy || local_buddy->user_resource_id == NULL) {
        hippo_im_remove_buddy(resource_id);
    } else {
        hippo_im_update_buddy(resource_id,
                              "mugshot-local", local_buddy->user_resource_id,
                              local_buddy->name,
                              TRUE, "Around", NULL, local_buddy->webdav_url);
    }
    
    g_free(resource_id);
}

static gboolean
read_session_info(DBusMessageIter *dict_iter,
                  char           **machine_id_p,
                  char           **session_id_p)
{
   DictStringEntry session_entries[3];
   
   session_entries[0].key = "machine";
   session_entries[1].key = "session";
   session_entries[2].key = NULL;
   
   if (!dict_extract_string_values(dict_iter, &session_entries[0]))
       return FALSE;

   if (!validate_dbus_session_id(session_entries[1].value)) {
       g_warning("Invalid D-BUS session ID");
       return FALSE;
   }

   if (machine_id_p)
       *machine_id_p = g_strdup(session_entries[0].value);
   if (session_id_p)
       *session_id_p = g_strdup(session_entries[1].value);
   
   return TRUE;
}

static gboolean
read_mugshot_info(DBusMessageIter *struct_iter,
                  char           **machine_id_p,
                  char           **session_id_p,
                  char           **user_resource_id_p,
                  char           **name_p)
{
    DictStringEntry info_entries[3];
    DBusMessageIter dict_iter;
 
    dbus_message_iter_recurse(struct_iter, &dict_iter);

    if (!read_session_info(&dict_iter, machine_id_p, session_id_p))
        return FALSE;

    dbus_message_iter_next(struct_iter);

    info_entries[0].key = "userResourceId";
    info_entries[0].required = TRUE;
    info_entries[1].key = "name";
    info_entries[1].required = FALSE; /* for back compat with older versions that don't provide this */
    info_entries[2].key = NULL;
    
    dbus_message_iter_recurse(struct_iter, &dict_iter);
    
    if (!dict_extract_string_values(&dict_iter, &info_entries[0])) {
        if (machine_id_p) {
            g_free(*machine_id_p);
            *machine_id_p = NULL;
        }
        if (session_id_p) {
            g_free(*session_id_p);
            *session_id_p = NULL;
        }
        return FALSE;
    }

    g_debug("local: Read local user info user resource ID '%s' and name '%s'\n",
            info_entries[0].value, info_entries[1].value ? info_entries[1].value : "null");
    
    if (user_resource_id_p)
        *user_resource_id_p = g_strdup(info_entries[0].value);

    if (name_p)
        *name_p = g_strdup(info_entries[1].value);
    
    return TRUE;
}

static char *
read_and_update_mugshot_info(DBusMessageIter *struct_iter)
{
    char *machine_id;
    char *session_id;
    char *user_resource_id;
    char *name;
    
    machine_id = NULL;
    session_id = NULL;
    user_resource_id = NULL;
    name = NULL;
    if (!read_mugshot_info(struct_iter, &machine_id, &session_id,
                           &user_resource_id, &name))
        return NULL;

    update_mugshot_info(session_id, user_resource_id, name);

    g_free(machine_id);
    g_free(user_resource_id);
    g_free(name);

    return session_id;
}

static gboolean
read_standard_info(DBusMessageIter *struct_iter,
                   char           **machine_id_p,
                   char           **session_id_p,
                   char           **webdav_url_p)
{
    DictStringEntry info_entries[2];
    DBusMessageIter dict_iter;
    const char *webdav_url = NULL;
 
    dbus_message_iter_recurse(struct_iter, &dict_iter);

    if (!read_session_info(&dict_iter, machine_id_p, session_id_p))
        return FALSE;

    dbus_message_iter_next(struct_iter);

    info_entries[0].key = "webdavUrl";
    info_entries[1].key = NULL;
    
    dbus_message_iter_recurse(struct_iter, &dict_iter);
    
    if (dict_extract_string_values(&dict_iter, &info_entries[0])) {
        webdav_url = info_entries[0].value;
    }
    
    if (webdav_url_p)
        *webdav_url_p = g_strdup(webdav_url);
    
    return TRUE;
}

static char *
read_and_update_standard_info(DBusMessageIter *struct_iter)
{
    char *machine_id;
    char *session_id;
    char *webdav_url;

    machine_id = NULL;
    session_id = NULL;
    webdav_url = NULL;
    if (!read_standard_info(struct_iter, &machine_id, &session_id,
                            &webdav_url))
        return NULL;
    
    update_standard_info(session_id, webdav_url);

    g_free(machine_id);
    g_free(webdav_url);

    return session_id;
}

static void
clean_mugshot_info_foreach(void *key,
                           void *value,
                           void *data)
{
    const char *session_id = key;
    GHashTable *seen_mugshot_ids = data;

    if (g_hash_table_lookup(seen_mugshot_ids, session_id) == NULL)
        update_mugshot_info(session_id, NULL, NULL);
}

static void
clean_standard_info_foreach(void *key,
                            void *value,
                            void *data)
{
    const char *session_id = key;
    GHashTable *seen_standard_ids = data;

    if (g_hash_table_lookup(seen_standard_ids, session_id) == NULL)
        update_standard_info(session_id, NULL);
}

static gboolean
update_im_buddies_foreach(void *key,
                          void *value,
                          void *data)
{
    const char *session_id = key;
    LocalBuddy *local_buddy = value;

    update_im_buddy(session_id);

    if (local_buddy->user_resource_id == NULL && local_buddy->webdav_url == NULL) {
        g_free(local_buddy->name);
        g_free(local_buddy);
        return TRUE;
    } else {
        return FALSE;
    }
}

static gboolean
get_info_from_all_sessions(HippoDBusProxy *proxy)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const char *info_name;
    GHashTable *seen_mugshot_ids;
    GHashTable *seen_standard_ids;

    seen_mugshot_ids = g_hash_table_new_full(g_str_hash, g_str_equal, (GDestroyNotify)g_free, NULL);
    
    info_name = MUGSHOT_INFO_NAME;
    dbus_error_init(&derror);
    reply = hippo_dbus_proxy_call_method_sync(proxy, "GetInfoFromAllSessions",
                                              &derror,
                                              DBUS_TYPE_STRING, &info_name,
                                              DBUS_TYPE_INVALID);

    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, "GetInfoFromAllSessions", &derror,
                                                               DBUS_TYPE_INVALID);

    if (retval) {
        if (!dbus_message_has_signature(reply, "a(a{sv}a{sv})")) {
            g_warning("Bad signature on GetInfoFromAllSessions reply");
            retval = FALSE;
        } else {
            DBusMessageIter iter, array_iter;
            
            dbus_message_iter_init(reply, &iter);
            dbus_message_iter_recurse(&iter, &array_iter);
            while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
                DBusMessageIter struct_iter;
                char *session_id;
                
                dbus_message_iter_recurse(&array_iter, &struct_iter);
                
                session_id = read_and_update_mugshot_info(&struct_iter);
                if (session_id)
                    g_hash_table_insert(seen_mugshot_ids, session_id, session_id);
                
                dbus_message_iter_next(&array_iter);
            }

        }
    }

    g_hash_table_foreach(local_buddies, clean_mugshot_info_foreach, seen_mugshot_ids);
    g_hash_table_destroy(seen_mugshot_ids);

    if (reply != NULL)
        dbus_message_unref(reply);
    
    /* Now get the "standard info" and merge it in */

    seen_standard_ids = g_hash_table_new_full(g_str_hash, g_str_equal, (GDestroyNotify)g_free, NULL);
    
    info_name = STANDARD_INFO_NAME;
    dbus_error_init(&derror);
    reply = hippo_dbus_proxy_call_method_sync(proxy, "GetInfoFromAllSessions",
                                              &derror,
                                              DBUS_TYPE_STRING, &info_name,
                                              DBUS_TYPE_INVALID);

    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, "GetInfoFromAllSessions", &derror,
                                                               DBUS_TYPE_INVALID);

    if (retval) {
        if (!dbus_message_has_signature(reply, "a(a{sv}a{sv})")) {
            g_warning("Bad signature on GetInfoFromAllSessions reply");
            retval = FALSE;
        } else {
            DBusMessageIter iter, array_iter;
            
            dbus_message_iter_init(reply, &iter);
            dbus_message_iter_recurse(&iter, &array_iter);
            while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
                DBusMessageIter struct_iter;
                char *session_id;
                
                dbus_message_iter_recurse(&array_iter, &struct_iter);
                
                session_id = read_and_update_standard_info(&struct_iter);
                if (session_id)
                    g_hash_table_insert(seen_standard_ids, session_id, session_id);
                
                dbus_message_iter_next(&array_iter);
            }

        }
    }

    if (reply != NULL)
        dbus_message_unref(reply);
    
    g_hash_table_foreach(local_buddies, clean_standard_info_foreach, seen_standard_ids);
    g_hash_table_destroy(seen_standard_ids);

    g_hash_table_foreach_remove(local_buddies, update_im_buddies_foreach, NULL);

    return retval;
}

static dbus_bool_t
append_string_pair(DBusMessageIter *dict_iter,
                   const char      *key,
                   const char      *value)
{
    DBusMessageIter entry_iter;
    DBusMessageIter variant_iter;
    
    if (!dbus_message_iter_open_container(dict_iter, DBUS_TYPE_DICT_ENTRY, NULL, &entry_iter))
        return FALSE;

    if (!dbus_message_iter_append_basic(&entry_iter, DBUS_TYPE_STRING, &key))
        return FALSE;

    if (!dbus_message_iter_open_container(&entry_iter, DBUS_TYPE_VARIANT, "s", &variant_iter))
        return FALSE;
    
    if (!dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_STRING, &value))
        return FALSE;

    if (!dbus_message_iter_close_container(&entry_iter, &variant_iter))
        return FALSE;
    
    if (!dbus_message_iter_close_container(dict_iter, &entry_iter))
        return FALSE;

    return TRUE;
}

static void
handle_info_changed(DBusConnection *connection,
                    DBusMessage    *message,
                    void           *data)
{
    DBusMessageIter iter, struct_iter;
    const char *name;
    char *session_id = NULL;
    
    if (!dbus_message_has_signature(message, "s(a{sv}a{sv})"))
        return;
    
    dbus_message_iter_init(message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    if (!(name &&
          (strcmp(name, MUGSHOT_INFO_NAME) == 0 ||
           strcmp(name, STANDARD_INFO_NAME) == 0)))
        return;
    
    dbus_message_iter_next(&iter);
    dbus_message_iter_recurse(&iter, &struct_iter);

    if (strcmp(name, MUGSHOT_INFO_NAME) == 0) {
        session_id = read_and_update_mugshot_info(&struct_iter);
    } else if (strcmp(name, STANDARD_INFO_NAME) == 0) {
        session_id = read_and_update_standard_info(&struct_iter);
    }

    if (session_id) {
        update_im_buddy(session_id);
        maybe_remove_local_buddy(session_id);
    }
}

static void
handle_info_removed(DBusConnection *connection,
                    DBusMessage    *message,
                    void           *data)
{
    DBusMessageIter iter, session_props_iter;
    const char *name;
    char *machine_id;
    char *session_id;
    
    if (!dbus_message_has_signature(message, "sa{sv}"))
        return;
    
    dbus_message_iter_init(message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    if (!(name &&
          (strcmp(name, MUGSHOT_INFO_NAME) == 0 ||
           strcmp(name, STANDARD_INFO_NAME) == 0)))
        return;
    
    dbus_message_iter_next(&iter);
    dbus_message_iter_recurse(&iter, &session_props_iter);
    
    machine_id = NULL;
    session_id = NULL;
    if (!read_session_info(&session_props_iter, &machine_id, &session_id))
        return;
    
    if (strcmp(name, MUGSHOT_INFO_NAME) == 0) {
        update_mugshot_info(session_id, NULL, NULL);
    } else if (strcmp(name, STANDARD_INFO_NAME) == 0) {
        update_standard_info(session_id, NULL);
    }

    update_im_buddy(session_id);
    maybe_remove_local_buddy(session_id);
        
    g_free(machine_id);
    g_free(session_id);
}

static const char*
get_self_id(void)
{
    HippoDataCache *cache;
    HippoConnection *hippo_connection;
    const char *self_resource_id;
    
    cache = hippo_engine_app_get_data_cache(hippo_get_engine_app());
    hippo_connection = hippo_data_cache_get_connection(cache);

    self_resource_id = hippo_connection_get_self_resource_id(hippo_connection);

    return self_resource_id;
}

static const char*
get_self_name(void)
{
    DDMDataModel *ddm_model;
    
    ddm_model = hippo_engine_app_get_data_model(hippo_get_engine_app());
    
    if (ddm_model) {
        DDMDataResource *resource;
        
        resource = ddm_data_model_get_self_resource(ddm_model);
        
        if (resource) {
            const char *name = NULL;
            ddm_data_resource_get(resource, "name", DDM_DATA_STRING, &name, NULL);
            return name;
        }
    }

    return NULL;
}

static dbus_bool_t
append_mugshot_info(DBusMessage *message,
                    void        *data)
{
    DBusMessageIter iter, prop_iter;
    const char *name;
    const char *s;
    
    dbus_message_iter_init_append(message, &iter);

    name = MUGSHOT_INFO_NAME;
    if (!dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &name))
        return FALSE;

    if (!dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &prop_iter))
        return FALSE;
    
    s = get_self_id();
    if (s != NULL) {
        const char *name;
        
        if (!append_string_pair(&prop_iter, "userResourceId", s))
            return FALSE;

        name = get_self_name();
        
        if (name) {
            if (!append_string_pair(&prop_iter, "name", name))
                return FALSE;
        }
    }
    
    if (!dbus_message_iter_close_container(&iter, &prop_iter))
        return FALSE;

    return TRUE;
}

static void
update_local_export_info(DBusConnection *connection)
{
    gboolean should_locally_export;

    should_locally_export = local_export_unique_name != NULL &&
        get_self_id() != NULL && get_self_name() != NULL;
    
    if (should_locally_export != have_locally_exported &&
        local_export_unique_name) {
        HippoDBusProxy *proxy;
        const char *name = MUGSHOT_INFO_NAME;
        
        proxy = hippo_dbus_proxy_new(connection, local_export_unique_name,
                                     "/org/freedesktop/od/LocalExport",
                                     "org.freedesktop.od.LocalExport");

        if (should_locally_export)
            hippo_dbus_proxy_call_method_async_appender(proxy, "AddInfoToOurSession",
                                                        NULL, NULL, NULL,
                                                        append_mugshot_info, NULL);
        else
            hippo_dbus_proxy_call_method_async(proxy, "RemoveInfoFromOurSession",
                                               NULL, NULL, NULL,
                                               DBUS_TYPE_STRING, &name,
                                               DBUS_TYPE_INVALID);
        
        hippo_dbus_proxy_unref(proxy);
    }

    have_locally_exported = should_locally_export;    
}

static void
on_self_properties_changed(DDMDataResource *resource,
                           GSList          *changed_properties,
                           gpointer         user_data)
{
    DBusConnection *connection;

    connection = user_data;

    update_local_export_info(connection);
}

static void
connection_has_auth_changed(HippoConnection *hippo_connection,
                            void            *data)
{

    DBusConnection *connection;

    connection = data;

    update_local_export_info(connection);
}

static void
handle_service_available(DBusConnection *connection,
                         const char     *well_known_name,
                         const char     *unique_name,
                         void           *data)
{
    HippoDBusProxy *proxy;
    
    if (local_export_unique_name != NULL) {
        g_warning("local_export_unique_name should be NULL");
        g_free(local_export_unique_name);
    }

    g_debug("local: %s (%s) appeared", well_known_name, unique_name);
    
    local_export_unique_name = g_strdup(unique_name);
    
    update_local_export_info(connection);
    
    proxy = hippo_dbus_proxy_new(connection, local_export_unique_name,
                                 "/org/freedesktop/od/LocalExport",
                                 "org.freedesktop.od.LocalExport");

    /* blocking call to get all the latest stuff */
    get_info_from_all_sessions(proxy);

    /* FIXME for now we have no way to learn about removals, and we don't
     * handle them
     */
    hippo_dbus_proxy_unref(proxy);
}

static void
handle_service_unavailable(DBusConnection *connection,
                           const char     *well_known_name,
                           const char     *unique_name,
                           void           *data)
{
    g_debug("local: %s (%s) going away", well_known_name, unique_name);

    g_free(local_export_unique_name);
    local_export_unique_name = NULL;

    update_local_export_info(connection);
}

static void
on_data_model_ready(DDMDataModel *ddm_model,
                    void         *data)
{
    DBusConnection *connection;
    DDMDataResource *resource;
    
    connection = data;
    
    resource = ddm_data_model_get_self_resource(ddm_model);

    g_debug("local: Data model ready, connecting to name on self resource '%p'", resource);
    if (resource) {
        ddm_data_resource_connect(resource, "name",
                                  on_self_properties_changed,
                                  connection);
    }
}

static const HippoDBusSignalTracker signal_handlers[] = {
    /* it would be slightly nicer if we also matched arg0=org.mugshot.Mugshot
     * on these signals
     */
    { "org.freedesktop.od.LocalExport", "InfoChanged", handle_info_changed },
    { "org.freedesktop.od.LocalExport", "InfoRemoved", handle_info_removed },
    { NULL, NULL, NULL }
};

static const HippoDBusServiceTracker service_tracker = {
    0,
    handle_service_available,
    handle_service_unavailable
};

void
hippo_dbus_init_local(DBusConnection *connection)
{
    HippoDataCache *cache;
    HippoConnection *hippo_connection;
    DDMDataModel *ddm_model;
    
    local_buddies = g_hash_table_new(g_str_hash, g_str_equal);
    
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.od.LocalExport",
                                               &service_tracker,
                                               signal_handlers,
                                               NULL);

    cache = hippo_engine_app_get_data_cache(hippo_get_engine_app());
    hippo_connection = hippo_data_cache_get_connection(cache);

    g_signal_connect(G_OBJECT(hippo_connection),
                     "has-auth-changed",
                     G_CALLBACK(connection_has_auth_changed),
                     connection);

    ddm_model = hippo_engine_app_get_data_model(hippo_get_engine_app());
    
    g_signal_connect(G_OBJECT(ddm_model), "ready",
                     G_CALLBACK(on_data_model_ready),
                     connection);
}

#if 0

/* cc -Wall -ggdb -O2 `pkg-config --cflags --libs dbus-glib-1 glib-2.0 dbus-1` -I ../build/config hippo-dbus-local.c hippo-dbus-helper.c -o foo && ./foo */

#include <dbus/dbus-glib-lowlevel.h>

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    DBusConnection *connection;

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    dbus_connection_setup_with_g_main(connection, NULL);

    hippo_dbus_init_local(connection);
    
    loop = g_main_loop_new(NULL, FALSE);
    
    g_main_loop_run(loop);

    return 0;
}

#endif
