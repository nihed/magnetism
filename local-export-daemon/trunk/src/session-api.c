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
    
    info = info_new(message);

    session_infos_add(infos, info);

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
    
    return dbus_message_new_method_return(message);
}

static DBusMessage*
handle_get_info_from_other_sessions(void            *object,
                                    DBusMessage     *message,
                                    DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;
    const char *requested_info;

    requested_info = NULL;

    if (!dbus_message_get_args(message, error, DBUS_TYPE_STRING, &requested_info,
                               DBUS_TYPE_INVALID))
        return NULL;
    
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    /* open an array of dict: each remote machine is a dict, where the dict is (string,variant) pairs and represents
     * the particular info that was requested
     */
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "a{sv}", &array_iter);

    /* FIXME put the info in here that matches requested_info */
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static const HippoDBusMember local_export_members[] = {
    /* args are "s" the namespaced name of the info, and "a{sv}" the dict of (string,variant) to export on the LAN */
    { HIPPO_DBUS_MEMBER_METHOD, "AddInfoToOurSession", "sa{sv}", "", handle_add_info_to_our_session },
    /* arg is "s" the info to remove from our session */
    { HIPPO_DBUS_MEMBER_METHOD, "RemoveInfoFromOurSession", "s", "", handle_remove_info_from_our_session },
    /* args are "s" the namespaced name of the info, and returns an array of struct; each struct is a session that provided
     * the requested info, and the first dict in the struct is props of said session, while the second dict is the info.
     * Props of the session would be machine ID, session ID, and IP addresses of the session.
     */
    { HIPPO_DBUS_MEMBER_METHOD, "GetInfoFromOtherSessions", "s", "a(a{sv}a{sv})", handle_get_info_from_other_sessions },
    /* FIXME here we would need change notification of some kind */
    { 0, NULL }
};

gboolean
session_api_init(DBusConnection *session_bus_)
{    
    session_bus = session_bus_;

    infos = session_infos_new();
    
    hippo_dbus_helper_register_interface(session_bus, LOCAL_EXPORT_INTERFACE,
                                         local_export_members, NULL);
    
    hippo_dbus_helper_register_object(session_bus, LOCAL_EXPORT_OBJECT_PATH,
                                      NULL, LOCAL_EXPORT_INTERFACE,
                                      NULL);
    
    return TRUE;
}
