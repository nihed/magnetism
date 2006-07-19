/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus.h>
#include <hippo/hippo-basics.h>
#include "hippo-dbus-client.h"

static gboolean
propagate_dbus_error(GError **error, DBusError *derror)
{
    if (dbus_error_is_set(derror)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
            _("D-BUS error: %s"), derror->message ? derror->message : derror->name);
        dbus_error_free(derror);
        return FALSE;
    } else {
        return TRUE;
    }
}

/* this is partially fantasy at the moment, since dbus-bus.c never
 * "forgets" a connection and reconnects I don't think
 */
static DBusConnection *session_connection = NULL;
static DBusConnection*
get_connection(GError **error)
{
    DBusError derror;

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    if (session_connection && dbus_connection_get_is_connected(session_connection))
        return session_connection;
    
    /* this unref is only allowed if we know it's disconnected */
    if (session_connection)
        dbus_connection_unref(session_connection);
    
    dbus_error_init(&derror);
    session_connection = dbus_bus_get(DBUS_BUS_SESSION, &derror);
    if (session_connection == NULL) {
        propagate_dbus_error(error, &derror);
    }

    return session_connection;
}

char*
hippo_dbus_full_bus_name(const char *server)
{
    GString *str;
    const char *p;
    char *server_with_port;
    static const char hexdigits[16] =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
          'A', 'B', 'C', 'D', 'E', 'F' };

    /* we need the "80" canonically in here */
    if (strchr(server, ':') == NULL)
        server_with_port = g_strdup_printf("%s:%d", server, 80);
    else
        server_with_port = NULL;

    /* 
     * There some kind of very remote security implications if we map multiple domains
     * to the same dbus name, or vice versa, I guess, so we try to avoid that.
     * In practice (at least right now) we don't automatically start a dbus client
     * to some random server so it probably doesn't matter.
     *
     */
    str = g_string_new(HIPPO_DBUS_BASE_BUS_NAME);
    g_string_append_c(str, '.');

    if (server_with_port)
        p = server_with_port;
    else 
        p = server;
    while (*p) {
        /* only [a-z][A-Z][0-9]_ are allowed, not starting with a digit.
         * We encode any non-alphanumeric as _ followed by 2-digit hex
         * of the byte.
         */
        if ((*p >= 'a' && *p <= 'z') || 
            (*p >= 'A' && *p <= 'Z') ||
            (*p >= '0' && *p <= '9')) {
            g_string_append_c(str, *p);
        } else {
            g_string_append_c(str, '_');
            g_string_append_c(str, hexdigits[(*p) & 0xf]);
            g_string_append_c(str, hexdigits[(*p) >> 4]);
        }
        ++p;
    }
    g_free(server_with_port);
    
    return g_string_free(str, FALSE);
}

gboolean
hippo_dbus_open_chat_blocking(const char   *server,
                              HippoChatKind kind,
                              const char   *chat_id,
                              GError      **error)
{
    DBusConnection *connection;
    gboolean result;
    DBusError derror;
    DBusMessage *message;
    DBusMessage *reply;
    char *bus_name;
    const char *kind_str;

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    connection = get_connection(error);
    if (connection == NULL)
        return FALSE;

    result = FALSE;
    
    bus_name = hippo_dbus_full_bus_name(server);
    
    message = dbus_message_new_method_call(bus_name,
                                           HIPPO_DBUS_PATH,
                                           HIPPO_DBUS_INTERFACE,
                                           "ShowChatWindow");                                          
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there */
    dbus_message_set_auto_start(message, FALSE);
    
    kind_str = hippo_chat_kind_as_string(kind);
    
    if (!dbus_message_append_args(message,
                                  DBUS_TYPE_STRING, &chat_id,
                                  DBUS_TYPE_INVALID))
        g_error("out of memory");                                  

    dbus_error_init(&derror);
    reply = dbus_connection_send_with_reply_and_block(connection, message, -1,
                                                      &derror);

    dbus_message_unref (message);

    if (reply == NULL) {
        propagate_dbus_error(error, &derror);
        goto out;
    }

    dbus_message_unref(reply);
    
    result = TRUE;

  out:
    /* any cleanup goes here */

    return result;
}
