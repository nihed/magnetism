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
#include <stdlib.h>
#include <string.h>
#include "tcp-listener.h"
#include "avahi-advertiser.h"
#include "hippo-dbus-helper.h"
#include "session-api.h"
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
    const char *machine_id;
    const char *session_id;
    char *s;
    
    reply = dbus_message_new_method_return(message);

    get_machine_and_session_ids(&machine_id, &session_id);

    /* Append session properties */
    dbus_message_iter_init_append(reply, &iter);
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &array_iter);

    append_string_pair(&array_iter, "machine", machine_id);
    append_string_pair(&array_iter, "session", session_id);

    s = g_strdup_printf("%u", session_api_get_change_serial());
    append_string_pair(&array_iter, "serial", s);
    g_free(s);
    
    dbus_message_iter_close_container(&iter, &array_iter);
    
    /* Append session infos */
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(sa{sv})", &array_iter);

    if (!session_api_append_all_infos(&array_iter)) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "Not enough memory to append everything");
        dbus_message_unref(reply);
        return NULL;
    }
    
    dbus_message_iter_close_container(&iter, &array_iter);
    
    return reply;
}

static const HippoDBusMember session_info_members[] = {
    /* the return value is dict of session props, then array of (namespacedInfoName, dict(string,variant)) */
    { HIPPO_DBUS_MEMBER_METHOD, "GetInfoForSession", "", "a{sv}a(sa{sv})", handle_get_info_for_session },
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

    if (type == DBUS_MESSAGE_TYPE_METHOD_CALL) {
        g_debug("Got method call %s", dbus_message_get_member(message));
    }
    
    result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
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

    dbus_connection_setup_with_g_main(new_connection, NULL);
    
    /* Allow anonymous connection */
    dbus_connection_set_allow_anonymous (new_connection, TRUE);
    
    hippo_dbus_helper_register_interface(new_connection, SESSION_INFO_INTERFACE,
                                         session_info_members, NULL);
    
    hippo_dbus_helper_register_object(new_connection, SESSION_INFO_OBJECT_PATH,
                                      NULL, SESSION_INFO_INTERFACE,
                                      NULL);
    
    if (!dbus_connection_add_filter(new_connection, handle_message_from_lan,
                                    NULL, NULL))
        g_error("no memory adding dbus connection filter");
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

static int
get_port(DBusServer *server)
{
    char *address;
    DBusAddressEntry **entries;
    int n_entries;
    const char *port_str;
    int port;
    
    address = dbus_server_get_address(server);

    if (!dbus_parse_address (address,
                             &entries,
                             &n_entries,
                             NULL) ||
        n_entries < 1)
        g_error("libdbus could not parse its own address '%s'", address);

    /* libdbus doesn't really guarantee this but it should be ok to assume */
    g_assert(strcmp(dbus_address_entry_get_method(entries[0]), "tcp") == 0);

    port_str = dbus_address_entry_get_value (entries[0], "port");
    if (port_str == NULL)
        g_error("libdbus returned no port in tcp address entry");
    
    port = atoi (port_str);
    g_assert (port > 0);
    
    dbus_address_entries_free (entries);    
    dbus_free(address);

    return port;
}

gboolean
tcp_listener_init(void)
{
    DBusError derror;
    const char *auth_mechanisms[] = { "ANONYMOUS", NULL };
    
    g_assert(server == NULL);

    /* missing port and bind=* in this address requires a
     * recent D-Bus, as does the anonymous login mechanism
     */
    dbus_error_init(&derror);
    server = dbus_server_listen("tcp:bind=*", &derror);
    if (server == NULL) {
        g_printerr("Error listening on TCP: %s\n", derror.message);
        return FALSE;
    }

    g_assert(dbus_server_get_is_connected(server));
    
    dbus_server_setup_with_g_main(server, NULL);

    /* Allow only anonymous auth, don't even attempt user auth
     */
    dbus_server_set_auth_mechanisms(server, auth_mechanisms);
    dbus_server_set_new_connection_function(server, on_new_connection, NULL, NULL);

    listening_on_port = get_port(server);

    g_debug("Listening on port %d", listening_on_port);
    
    return TRUE;
}

int
tcp_listener_get_port (void)
{
    return listening_on_port;
}
