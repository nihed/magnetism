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

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    connection = get_connection(error);
    if (connection == NULL)
        return FALSE;

    result = FALSE;
    
    bus_name = hippo_dbus_full_bus_name(server);
    
    message = dbus_message_new_method_call(bus_name,
                                           HIPPO_DBUS_STACKER_PATH,
                                           HIPPO_DBUS_STACKER_INTERFACE,
                                           "ShowChatWindow");
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there */
    dbus_message_set_auto_start(message, FALSE);
    
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

gboolean
hippo_dbus_get_chat_window_state_blocking(const char       *server,
                                          const char       *chat_id,
                                          HippoWindowState *state,
                                          GError          **error)
{
    DBusConnection *connection;
    gboolean result;
    DBusError derror;
    DBusMessage *message;
    DBusMessage *reply = NULL;
    char *bus_name;
    dbus_int32_t dbus_state;

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    connection = get_connection(error);
    if (connection == NULL)
        return FALSE;

    result = FALSE;
    
    bus_name = hippo_dbus_full_bus_name(server);
    
    message = dbus_message_new_method_call(bus_name,
                                           HIPPO_DBUS_STACKER_PATH,
                                           HIPPO_DBUS_STACKER_INTERFACE,
                                           "GetChatWindowState");
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there */
    dbus_message_set_auto_start(message, FALSE);
    
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

    if (dbus_message_get_args(message, &derror,
                              DBUS_TYPE_INT32, &dbus_state,
                              DBUS_TYPE_INVALID)) {
        propagate_dbus_error(error, &derror);
        goto out;
    }
    
    if (dbus_state < (dbus_int32_t)HIPPO_WINDOW_STATE_CLOSED ||
        dbus_state > (dbus_int32_t)HIPPO_WINDOW_STATE_ACTIVE) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("Bad value %d for window state constant"), dbus_state);
        goto out;
    }
    
    *state = (HippoWindowState)dbus_state;
    
    result = TRUE;

  out:
    if (reply != NULL)
        dbus_message_unref(reply);

    return result;
}

gboolean
hippo_dbus_show_browser_blocking(const char   *server,
                                 GError      **error)
{
    DBusConnection *connection;
    gboolean result;
    DBusError derror;
    DBusMessage *message;
    DBusMessage *reply;
    char *bus_name;

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);

    connection = get_connection(error);
    if (connection == NULL)
        return FALSE;

    result = FALSE;
    
    bus_name = hippo_dbus_full_bus_name_com_dumbhippo_with_forward_hex(server);
    
    message = dbus_message_new_method_call(bus_name,
                                           HIPPO_DBUS_STACKER_PATH,
                                           HIPPO_DBUS_STACKER_INTERFACE,
                                           "ShowBrowser");
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there */
    dbus_message_set_auto_start(message, FALSE);
    
    if (!dbus_message_append_args(message,
                                  DBUS_TYPE_INVALID))
        g_error("out of memory");                                  

    g_debug("Sending ShowBrowser to %s", bus_name);

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
    g_free(bus_name);

    return result;
}

void
hippo_dbus_debug_log_error(const char   *where,
                           DBusMessage  *message)
{
    if (dbus_message_get_type(message) == DBUS_MESSAGE_TYPE_ERROR) {
        const char *error;
        const char *text;
        
        error = dbus_message_get_error_name(message);
        text = NULL;
        if (dbus_message_get_args(message, NULL,
                                  DBUS_TYPE_STRING, &text,
                                  DBUS_TYPE_INVALID)) {
            g_debug("Got error reply at %s %s '%s'",
                    where, error ? error : "NULL", text ? text : "NULL");
        } else {
            g_debug("Got error reply at %s %s",
                    where, error ? error : "NULL");
        }
    }
}
