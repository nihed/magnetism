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
#include <string.h>
#include <stdlib.h>
#include <glib.h>
#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>
#include "avahi-advertiser.h"
#include "avahi-scanner.h"
#include "session-api.h"
#include "main.h"

static char *machine_id;
static char *session_id;

static DBusHandlerResult handle_message         (DBusConnection     *connection,
                                                 DBusMessage        *message,
                                                 void               *user_data);
static void              on_disconnected        (void);

void
get_machine_and_session_ids(const char **machine_id_p,
                            const char **session_id_p)
{
    g_assert(machine_id != NULL);
    g_assert(session_id != NULL);
    
    if (machine_id_p)
        *machine_id_p = machine_id;
    if (session_id_p)
        *session_id_p = session_id;
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
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
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);

        /* g_debug("signal from %s %s.%s", sender ? sender : "NULL", interface, member); */
   
        if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
            dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameLost")) {
            /* If we lose our name, we behave as if disconnected */
            const char *name = NULL;
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_STRING, &name, DBUS_TYPE_INVALID) && 
                strcmp(name, LOCAL_EXPORT_BUS_NAME) == 0) {
                on_disconnected();
            }
        } else if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
                   dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameOwnerChanged")) {
            const char *name = NULL;
            const char *old = NULL;
            const char *new = NULL;
            if (dbus_message_get_args(message, NULL,
                                      DBUS_TYPE_STRING, &name,
                                      DBUS_TYPE_STRING, &old,
                                      DBUS_TYPE_STRING, &new,
                                      DBUS_TYPE_INVALID)) {
                /* g_debug("NameOwnerChanged %s '%s' -> '%s'", name, old, new); */
                if (*old == '\0')
                    old = NULL;
                if (*new == '\0')
                    new = NULL;
                if (old && strcmp(name, old) == 0) {
                    /* this means a unique name was lost */
                }
            } else {
                g_warning("NameOwnerChanged had wrong args???");
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            on_disconnected();
        }        
    }

    return result;
}

static void
on_disconnected(void)
{

    exit(0);
}

static gboolean
request_bus_name(DBusConnection *connection,
                 gboolean        replace_existing)
{
    dbus_uint32_t flags;
    dbus_uint32_t result;
    DBusError derror;
    
    flags = DBUS_NAME_FLAG_DO_NOT_QUEUE | DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace_existing)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
    
    dbus_error_init(&derror);
    result = dbus_bus_request_name(connection, LOCAL_EXPORT_BUS_NAME,
                                   flags,
                                   &derror);
    if (dbus_error_is_set(&derror)) {
        g_printerr("Failed to take bus name %s: %s", LOCAL_EXPORT_BUS_NAME, derror.message);
        return FALSE;
    }
    
    if (!(result == DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER ||
          result == DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER)) {
        g_printerr("Failed to take bus name %s (another copy of the daemon is probably running already)", LOCAL_EXPORT_BUS_NAME);
        return FALSE;
    }

    return TRUE;
}

static char*
get_session_guid_hack(void)
{
    /* We need a dbus_connection_get_server_guid(); until then we have to do this */
    const char *address = g_getenv("DBUS_SESSION_BUS_ADDRESS");
    const char *s;

    if (address == NULL)
        goto fallback;
    
    s = strstr(address, "guid=");
    if (s == NULL)
        goto fallback;

    s += 5;
    if (strlen(s) < 32)
        goto fallback;

    return g_strndup(s, 32);
    
 fallback:
    /* The fallback is to make up an id for the local-export-daemon, which is less useful
     * since it doesn't have any meaning
     */
    {
        char buf[32];
        int i;

        /* glib seeds the process with urandom, which should be enough for network uniqueness,
         * though this GUID will allow other processes to guess our next random number
         * (which conceivably could matter, but in any case where it does
         * the code should be using a new random seed and not the glib-global one)
         */
        for (i = 0; i < (int) sizeof(buf); ++i) {
            buf[i] = g_random_int_range('a', 'z' + 1);
        }

        return g_strndup(buf, sizeof(buf));
    }
}

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    DBusConnection *connection;
    DBusError derror;
    
    dbus_error_init(&derror);
    connection = dbus_bus_get(DBUS_BUS_SESSION, &derror);

    if (connection == NULL) {
        g_printerr("Failed to connect to session bus: %s", derror.message);
        dbus_error_free(&derror);
        exit(1);
    }

    dbus_connection_setup_with_g_main(connection, NULL);

    if (!dbus_connection_add_filter(connection, handle_message,
                                    NULL, NULL))
        g_error("no memory adding dbus connection filter");

    /* FIXME don't --replace by default, that's screwy */
    if (!request_bus_name(connection, TRUE))
        exit(1);

    session_id = get_session_guid_hack();
    machine_id = dbus_get_local_machine_id();

    /* g_printerr("Session '%s' on machine '%s'\n", session_id, machine_id); */

    if (!avahi_advertiser_init())
        exit(1);
    
    if (!avahi_glue_init())
        exit(1);

    if (!avahi_scanner_init())
        exit(1);

    if (!session_api_init(connection))
        exit(1);
    
    loop = g_main_loop_new(NULL, FALSE);

    g_main_loop_run(loop);

    return 0;
}

