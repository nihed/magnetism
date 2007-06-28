/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-local.h"
#include "hippo-dbus-im.h"

#define LOCAL_RESOURCE_BASE "online-desktop:/o/local-user"

typedef struct {
    const char *key;
    const char *value;
} DictStringEntry;

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
            if (entries[i].value == NULL) {
                g_warning("Missing property '%s'", entries[i].key);
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
read_info(DBusMessageIter *struct_iter,
          char           **machine_id_p,
          char           **session_id_p,
          char           **user_resource_id_p)
{
    DictStringEntry info_entries[2];
    DBusMessageIter dict_iter;
 
    dbus_message_iter_recurse(struct_iter, &dict_iter);

    if (!read_session_info(&dict_iter, machine_id_p, session_id_p))
        return FALSE;

    dbus_message_iter_next(struct_iter);

    info_entries[0].key = "user_resource_id";
    info_entries[1].key = NULL;
    
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
    
    if (user_resource_id_p)
        *user_resource_id_p = g_strdup(info_entries[0].value);
    
    return TRUE;
}

static char *
make_resource_id(const char *session_id)
{
    return g_strconcat(LOCAL_RESOURCE_BASE "/" , session_id, NULL);
}

static void
remove_buddy(HippoNotificationSet *notifications,
             const char           *session_id)
{
    char *resource_id = make_resource_id(session_id);
    
    hippo_dbus_im_remove_buddy(notifications, resource_id);
    
    g_free(resource_id);
}

static void
update_buddy(HippoNotificationSet *notifications,
             const char           *session_id,
             const char           *user_resource_id)
{
    char *resource_id = make_resource_id(session_id);
    
    hippo_dbus_im_update_buddy(notifications, resource_id,
                               "mugshot-local", user_resource_id,
                               TRUE, "Around");
    
    g_free(resource_id);
}

static void
update_info(HippoNotificationSet *notifications,
            DBusMessageIter      *struct_iter)
{
    char *machine_id;
    char *session_id;
    char *user_resource_id;

    machine_id = NULL;
    session_id = NULL;
    user_resource_id = NULL;
    if (!read_info(struct_iter, &machine_id, &session_id,
                   &user_resource_id))
        return;

    update_buddy(notifications, session_id, user_resource_id);

    g_free(machine_id);
    g_free(session_id);
    g_free(user_resource_id);
}

static gboolean
get_info_from_all_sessions(HippoDBusProxy *proxy)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const char *info_name;

    info_name = "org.mugshot.Mugshot";
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
            HippoNotificationSet *notifications;
            
            notifications = hippo_dbus_im_start_notifications();    
            
            dbus_message_iter_init(reply, &iter);
            dbus_message_iter_recurse(&iter, &array_iter);
            while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
                DBusMessageIter struct_iter;
                dbus_message_iter_recurse(&array_iter, &struct_iter);
                
                update_info(notifications, &struct_iter);
                
                dbus_message_iter_next(&array_iter);
            }

            hippo_dbus_im_send_notifications(notifications);
        }
    }

    if (reply != NULL)
        dbus_message_unref(reply);

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
    HippoNotificationSet *notifications;
    const char *name;
    
    if (!dbus_message_has_signature(message, "s(a{sv}a{sv})"))
        return;
    
    dbus_message_iter_init(message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    if (!(name && strcmp(name, "org.mugshot.Mugshot") == 0)) {
        return;
    }

    dbus_message_iter_next(&iter);
    dbus_message_iter_recurse(&iter, &struct_iter);

    notifications = hippo_dbus_im_start_notifications();    
    update_info(notifications, &struct_iter);
    hippo_dbus_im_send_notifications(notifications);
}

static void
handle_info_removed(DBusConnection *connection,
                    DBusMessage    *message,
                    void           *data)
{
    DBusMessageIter iter, session_props_iter;
    HippoNotificationSet *notifications;
    const char *name;
    char *machine_id;
    char *session_id;
    
    if (!dbus_message_has_signature(message, "sa{sv}"))
        return;
    
    dbus_message_iter_init(message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    if (!(name && strcmp(name, "org.mugshot.Mugshot") == 0)) {
        return;
    }

    dbus_message_iter_next(&iter);
    dbus_message_iter_recurse(&iter, &session_props_iter);

    machine_id = NULL;
    session_id = NULL;
    if (!read_session_info(&session_props_iter, &machine_id, &session_id))
        return;

    /* FIXME: Handle the case where a session maliciously claims
     * to have the same session ID as an existing session; we'll get mildly confused
     * by that now, though we shouldn't crash. But we're also completely vulnerable
     * to someone publishing fake information, so maybe the confusion doesn't matter
     * much.
     */
    
    notifications = hippo_dbus_im_start_notifications();
    remove_buddy(notifications, session_id);
    hippo_dbus_im_send_notifications(notifications);
    
    g_free(machine_id);
    g_free(session_id);
}

/* FIXME we need to subscribe to changes on this and update our info when it changes */
static const char*
get_self_id()
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    const char *self_resource_id = hippo_connection_get_self_resource_id(hippo_connection);

    return self_resource_id;
}

static dbus_bool_t
append_mugshot_info(DBusMessage *message,
                    void        *data)
{
    DBusMessageIter iter, prop_iter;
    const char *name;
    const char *s;
    
    dbus_message_iter_init_append(message, &iter);

    name = "org.mugshot.Mugshot";
    if (!dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &name))
        return FALSE;

    if (!dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &prop_iter))
        return FALSE;

    s = get_self_id();
    if (s != NULL) {
        if (!append_string_pair(&prop_iter, "user_resource_id", s))
            return FALSE;
    }
        
    if (!dbus_message_iter_close_container(&iter, &prop_iter))
        return FALSE;

    return TRUE;
}

static void
handle_service_available(DBusConnection *connection,
                         const char     *well_known_name,
                         const char     *unique_name,
                         void           *data)
{
    HippoDBusProxy *proxy;
    
    g_debug("%s (%s) appeared", well_known_name, unique_name);
    
    proxy = hippo_dbus_proxy_new(connection, unique_name,
                                 "/org/freedesktop/od/LocalExport",
                                 "org.freedesktop.od.LocalExport");

    hippo_dbus_proxy_call_method_async_appender(proxy, "AddInfoToOurSession",
                                                NULL, NULL, NULL,
                                                append_mugshot_info, NULL);

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
    g_debug("%s (%s) going away", well_known_name, unique_name);
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
    handle_service_available,
    handle_service_unavailable
};

void
hippo_dbus_init_local(DBusConnection *connection)
{
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.od.LocalExport",
                                               &service_tracker,
                                               signal_handlers,
                                               NULL);    
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
