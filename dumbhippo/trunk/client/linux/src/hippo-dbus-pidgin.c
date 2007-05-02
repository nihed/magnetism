/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-pidgin.h"

#define GAIM_BUS_NAME "net.sf.gaim.GaimService"
#define GAIM_OBJECT_NAME "/net/sf/gaim/GaimObject"
#define GAIM_INTERFACE_NAME "net.sf.gaim.GaimInterface"

#define PIDGIN_BUS_NAME "im.pidgin.purple.PurpleService"
#define PIDGIN_OBJECT_NAME "/im/pidgin/purple/PurpleObject"
#define PIDGIN_INTERFACE_NAME "im.pidgin.purple.PurpleInterface"

typedef struct {
    char *bus_name;

} PidginState;

static void
pidgin_state_free(PidginState *state)
{

    g_free(state->bus_name);
    g_free(state);
}

static void
reload_from_new_owner(DBusConnection *connection,
                      const char     *bus_name)
{
    PidginState *state;
    DBusMessage *reply;
    dbus_int32_t active_accounts_len;
    dbus_int32_t *active_accounts;
    DBusError derror;
    
    state = g_new0(PidginState, 1);
    state->bus_name = g_strdup(bus_name);

    dbus_error_init(&derror);
    reply = hippo_dbus_client_call_method_sync(connection,
                                               bus_name,
                                               GAIM_OBJECT_NAME,
                                               GAIM_INTERFACE_NAME,
                                               "GaimAccountsGetAllActive",
                                               &derror,
                                               DBUS_TYPE_INVALID);

    active_accounts = NULL;
    active_accounts_len = 0;
    if (reply != NULL) {
        dbus_message_get_args(reply, &derror,
                              DBUS_TYPE_ARRAY, DBUS_TYPE_INT32,
                              &active_accounts, &active_accounts_len,
                              DBUS_TYPE_INVALID);
        g_printerr("%d accounts\n", active_accounts_len);

        dbus_message_unref(reply);
    }

    
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
               DBusMessage        *message,
               void               *user_data)
{
    int type;
    
    type = dbus_message_get_type(message);

    if (type == DBUS_MESSAGE_TYPE_METHOD_RETURN) {

        g_print("got method return\n");
    } else if (type == DBUS_MESSAGE_TYPE_ERROR) {
        g_print("got error\n");
    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {

        g_print("got signal\n");
    }
    
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

static void
connect_with_name_and_iface(DBusConnection *connection,
                            const char     *bus_name,
                            const char     *iface_name,
                            const char     *signal)
{
    DBusError derror;
    char *s;

    dbus_error_init(&derror);
    
    s = g_strdup_printf("type='signal',sender='"
                        "%s"
                        "',interface='"
                        "%s"
                        "',member='"
                        "%s"
                        "'", bus_name, iface_name, signal);
    dbus_bus_add_match(connection, s, &derror);
    if (dbus_error_is_set(&derror)) {
        g_warning("Failed to add match rule: %s: %s", derror.message, s);
        dbus_error_free(&derror);
    }
    g_free(s);
}

static void
connect_pidgin(DBusConnection *connection,
               const char     *signal)
{
    connect_with_name_and_iface(connection, GAIM_BUS_NAME, GAIM_INTERFACE_NAME, signal);
    connect_with_name_and_iface(connection, PIDGIN_BUS_NAME, PIDGIN_INTERFACE_NAME, signal); 
}

void
hippo_dbus_init_pidgin(DBusConnection *connection)
{
    connect_with_name_and_iface(connection,
                                DBUS_SERVICE_DBUS,
                                DBUS_INTERFACE_DBUS,
                                "NameOwnerChanged");
    
    connect_pidgin(connection, "BuddyStatusChanged");
    connect_pidgin(connection, "BuddyIdleChanged");
    connect_pidgin(connection, "BuddySignedOn");
    connect_pidgin(connection, "BuddySignedOff");
    connect_pidgin(connection, "BuddyAdded");
    connect_pidgin(connection, "BuddyRemoved");

    if (!dbus_connection_add_filter(connection, handle_message,
                                    NULL, NULL))
        g_error("no memory adding dbus connection filter");

    reload_from_new_owner(connection, GAIM_BUS_NAME);
}

#if 0

/* cc -Wall -ggdb -O2 `pkg-config --cflags --libs dbus-glib-1 glib-2.0 dbus-1` -I ../build/config hippo-dbus-pidgin.c hippo-dbus-helper.c -o foo && ./foo */

#include <dbus/dbus-glib-lowlevel.h>

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    DBusConnection *connection;

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    dbus_connection_setup_with_g_main(connection, NULL);

    hippo_dbus_init_pidgin(connection);
    
    loop = g_main_loop_new(NULL, FALSE);
    
    g_main_loop_run(loop);

    return 0;
}

#endif
