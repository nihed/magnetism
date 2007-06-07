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

static DBusServer *server = NULL;

static void
on_new_connection(DBusServer     *server,
                  DBusConnection *new_connection,
                  void           *data)
{
    g_debug("Got a new connection");
    
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
    char *address;
    
    g_assert(server == NULL);

    dbus_error_init(&derror);
    server = dbus_server_listen("tcp:", &derror);
    if (server == NULL) {
        g_printerr("Error listening on TCP: %s", derror.message);
        return FALSE;
    }

    dbus_server_setup_with_g_main(server, NULL);

    dbus_server_set_new_connection_function(server, on_new_connection, NULL, NULL);

    address = dbus_server_get_address(server);
    if (!avahi_advertiser_init(address)) {
        tcp_listener_shutdown();
        return FALSE;
    }
    dbus_free(address);
    
    return TRUE;
}
