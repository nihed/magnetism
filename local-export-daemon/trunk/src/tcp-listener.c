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
#include "tcp-listener.h"
#include "avahi-advertiser.h"
#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>
#include "main.h"

static DBusServer *server = NULL;
static int listening_on_port = -1;

static DBusMessage*
handle_get_info_for_session(void            *object,
                            DBusMessage     *message,
                            DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;
    
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    /* FIXME append the machine ID and session ID */
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "a(sa{sv})", &array_iter);

    /* FIXME put the info in here */
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static const HippoDBusMember session_info_members[] = {
    /* the return value is machineId, sessionId, array of (namespacedInfoName, dict(string,variant)) */
    { HIPPO_DBUS_MEMBER_METHOD, "GetInfoForSession", "", "ssa(sa{sv})", handle_get_info_for_session },
    { 0, NULL }
};


static DBusHandlerResult
handle_message_from_lan(DBusConnection     *connection,
                        DBusMessage        *message,
                        void               *user_data)
{
    int type;
    DBusHandlerResult result;
    
    type = dbus_message_get_type(message);

    result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    result = hippo_dbus_helper_filter_message(connection, message);
    if (result == DBUS_HANDLER_RESULT_HANDLED) {
        ; /* we're done, something registered with the helper did the work */
    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
        if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            /* client disconnected */
            dbus_connection_unref(connection);
        }        
    }
    
    return result;
}

static void
on_new_connection(DBusServer     *server,
                  DBusConnection *new_connection,
                  void           *data)
{
    g_debug("Got a new connection");

    dbus_connection_ref(new_connection);
    if (!dbus_connection_add_filter(new_connection, handle_message_from_lan,
                                    NULL, NULL))
        g_error("no memory adding dbus connection filter");

    hippo_dbus_helper_register_interface(new_connection, SESSION_INFO_INTERFACE,
                                         session_info_members, NULL);
    
    hippo_dbus_helper_register_object(new_connection, SESSION_INFO_OBJECT_PATH,
                                      NULL, SESSION_INFO_INTERFACE,
                                      NULL);
}

void
tcp_listener_shutdown(void)
{
    if (server != NULL) {
        dbus_server_disconnect(server);
        dbus_server_unref(server);
        server = NULL;
    }
}

gboolean
tcp_listener_init(void)
{
    DBusError derror;
    
    g_assert(server == NULL);

    /* FIXME newer versions of dbus allow omitting port so one is automatically chosen, which
     * would be a lot better. Note that we pass this random port number below as well.
     */
    dbus_error_init(&derror);
    server = dbus_server_listen("tcp:port=23523", &derror);
    if (server == NULL) {
        g_printerr("Error listening on TCP: %s\n", derror.message);
        return FALSE;
    }

    dbus_server_setup_with_g_main(server, NULL);

    dbus_server_set_new_connection_function(server, on_new_connection, NULL, NULL);

    listening_on_port = 23523; /* FIXME don't hardcode */
    
    return TRUE;
}

int
tcp_listener_get_port (void)
{
    return listening_on_port;
}
