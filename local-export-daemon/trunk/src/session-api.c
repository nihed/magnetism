/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/*
 * Copyright (C) 2007 Red Hat Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
#include <config.h>
#include "session-api.h"
#include "session-info.h"
#include "avahi-scanner.h"
#include "avahi-advertiser.h"
#include "hippo-dbus-helper.h"
#include "main.h"


static DBusConnection *session_bus = NULL;
static SessionInfos *infos = NULL;

static DBusMessage*
handle_add_info_to_our_session(void            *object,
                               DBusMessage     *message,
                               DBusError       *error)
{
    Info *info;
    
    /* signature was already checked by dbus helper */
    
    info = info_new_from_message(message);

    session_infos_add(infos, info);
    /* will do nothing if change serial didn't really change */
    avahi_advertiser_queue_republish();
    
    info_unref(info);

    /* FIXME track the client that added this and remove the info when the client disappears?
     * Or allow clients to add info without opening a persistent connection?
     */
    
    return dbus_message_new_method_return(message);
}

static DBusMessage*
handle_remove_info_from_our_session(void            *object,
                                    DBusMessage     *message,
                                    DBusError       *error)
{
    DBusMessageIter iter;
    const char *name;
    
    /* signature was already checked by dbus helper */
    
    dbus_message_iter_init(message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    session_infos_remove(infos, name);
    /* will do nothing if change serial didn't really change */
    avahi_advertiser_queue_republish();
    
    return dbus_message_new_method_return(message);
}

static DBusMessage*
handle_get_info_from_all_sessions(void            *object,
                                  DBusMessage     *message,
                                  DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter iter, sessions_array_iter;
    const char *requested_info;

    requested_info = NULL;

    if (!dbus_message_get_args(message, error, DBUS_TYPE_STRING, &requested_info,
                               DBUS_TYPE_INVALID))
        return NULL;
    
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(a{sv}a{sv})", &sessions_array_iter);

    if (!avahi_scanner_append_infos_with_name(requested_info, &sessions_array_iter)) {
        dbus_set_error(error, DBUS_ERROR_FAILED, "No memory to return requested data");
        dbus_message_unref(reply);
        return NULL;
    }
    
    dbus_message_iter_close_container(&iter, &sessions_array_iter);
    
    return reply;
}

static const HippoDBusMember local_export_members[] = {
    /* args are "s" the namespaced name of the info, and "a{sv}" the
     * dict of (string,variant) to export on the LAN
     */
    { HIPPO_DBUS_MEMBER_METHOD, "AddInfoToOurSession", "sa{sv}", "", handle_add_info_to_our_session },

    /* arg is "s" the info to remove from our session */
    { HIPPO_DBUS_MEMBER_METHOD, "RemoveInfoFromOurSession", "s", "", handle_remove_info_from_our_session },

    /* args are "s" the namespaced name of the info, and returns an
     * array of struct; each struct is a session that provided the
     * requested info, and the first dict in the struct is props of
     * said session, while the second dict is the info.  Props of the
     * session would be machine ID, session ID, and IP addresses of
     * the session.
     */
    { HIPPO_DBUS_MEMBER_METHOD, "GetInfoFromAllSessions", "s", "a(a{sv}a{sv})", handle_get_info_from_all_sessions },

    /* args are "s" the namespaced name of the info, and then a new value for that info for some
     * session (as in GetInfoFromOtherSessions)
     */
    { HIPPO_DBUS_MEMBER_SIGNAL, "InfoChanged", "", "s(a{sv}a{sv})", NULL },

    /* Args are the info name removed, and the session details */
    { HIPPO_DBUS_MEMBER_SIGNAL, "InfoRemoved", "", "sa{sv}", NULL },
    
    { 0, NULL }
};

static gboolean
churn_bogus_changes_timeout(void *data)
{
    session_infos_churn_bogus_info(infos);
    avahi_advertiser_queue_republish();
    return TRUE;
}

gboolean
session_api_init(DBusConnection *session_bus_,
                 const char      *machine_id,
                 const char      *session_id)
{    
    session_bus = session_bus_;

    infos = session_infos_new_with_builtins(machine_id, session_id);
    
    hippo_dbus_helper_register_interface(session_bus, LOCAL_EXPORT_INTERFACE,
                                         local_export_members, NULL);
    
    hippo_dbus_helper_register_object(session_bus, LOCAL_EXPORT_OBJECT_PATH,
                                      NULL, LOCAL_EXPORT_INTERFACE,
                                      NULL);

#if 0
    /* disable this in production! */
    g_timeout_add(3000, churn_bogus_changes_timeout, NULL);
#endif
    
    return TRUE;
}

gboolean
session_api_append_all_infos(DBusMessageIter *array_iter)
{
    return session_infos_append_all(infos, array_iter);
}

guint32
session_api_get_change_serial(void)
{
    return session_infos_get_change_serial(infos);
}


typedef struct {
    SessionInfos *infos;
    Info *info;
    char *removed_name;
} AppenderData;

static dbus_bool_t
append_info_changed(DBusMessage *message,
                    void        *data)
{
    AppenderData *ad = data;
    const char *name;
    DBusMessageIter iter;

    dbus_message_iter_init_append(message, &iter);
    
    name = info_get_name(ad->info);

    if (!dbus_message_iter_append_basic(&iter,
                                        DBUS_TYPE_STRING,
                                        &name))
        return FALSE;

    if (!session_infos_write_with_info(ad->infos,
                                       ad->info,
                                       &iter))
        return FALSE;

    return TRUE;
}

static dbus_bool_t
append_info_removed(DBusMessage *message,
                    void        *data)
{
    AppenderData *ad = data;
    DBusMessageIter iter;

    dbus_message_iter_init_append(message, &iter);
    
    if (!dbus_message_iter_append_basic(&iter,
                                        DBUS_TYPE_STRING,
                                        &ad->removed_name))
        return FALSE;
    
    if (!session_infos_write(ad->infos,
                             &iter))
        return FALSE;

    return TRUE;
}

void
session_api_notify_changed (SessionInfos           *infos,
                            SessionChangeNotifySet *set)
{
    Info *info;
    char *removed_name;

    /* SessionChangeNotifySet is supposed to guarantee no overlap
     * between "changed" and "removed" items
     */
    
    while ((info = session_change_notify_set_pop(set)) != NULL) {
        AppenderData ad;
        
        ad.infos = infos;
        ad.info = info;
        ad.removed_name = NULL;
        hippo_dbus_helper_emit_signal_appender(session_bus,
                                               LOCAL_EXPORT_OBJECT_PATH,
                                               LOCAL_EXPORT_INTERFACE,
                                               "InfoChanged",
                                               append_info_changed,
                                               &ad);
        info_unref(info);
    }

    while ((removed_name = session_change_notify_set_pop_removal(set)) != NULL) {
        AppenderData ad;
        
        ad.infos = infos;
        ad.info = NULL;
        ad.removed_name = removed_name;
        hippo_dbus_helper_emit_signal_appender(session_bus,
                                               LOCAL_EXPORT_OBJECT_PATH,
                                               LOCAL_EXPORT_INTERFACE,
                                               "InfoRemoved",
                                               append_info_removed,
                                               &ad);
        g_free(removed_name);
    }
}
