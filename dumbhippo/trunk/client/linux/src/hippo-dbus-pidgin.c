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
    dbus_int32_t *statuses;
    int n_statuses;
    char **status_names;
} PidginStatusList;

typedef struct {
    char *bus_name;
    HippoDBusProxy *gaim_proxy;
    dbus_int32_t blist_id;
    dbus_int32_t blist_root_id;
    dbus_int32_t *accounts;
    int n_accounts;
    dbus_int32_t *account_presences;
    PidginStatusList **account_statuses;
} PidginState;

static void
pidgin_status_list_free(PidginStatusList *statuses)
{
    g_free(statuses->statuses);
    g_free(statuses);
}


static void
pidgin_state_free(PidginState *state)
{
    int i;

    if (state->account_statuses) {
        for (i = 0; i < state->n_accounts; ++i) {
            if (state->account_statuses[i]) {
                pidgin_status_list_free(state->account_statuses[i]);
            }
        }
        g_free(state->account_statuses);
    }

    g_free(state->account_presences);
    
    g_free(state->accounts);

    hippo_dbus_proxy_unref(state->gaim_proxy);
    
    g_free(state->bus_name);
    g_free(state);
}

static void
reload_status_list(HippoDBusProxy   *proxy,
                   PidginStatusList *status_list)
{
    int i;

    for (i = 0; i < status_list->n_statuses; ++i) {
        char *status_name;
        
        if (!hippo_dbus_proxy_STRING__INT32(proxy, 
                                            "GaimStatusGetName",
                                            status_list->statuses[i],
                                            &status_name))
            return;

        g_printerr("status %d name '%s'\n", status_list->statuses[i], status_name);
        g_free(status_name);
    }
}

static void
walk_blist_node(HippoDBusProxy *proxy,
                dbus_int32_t    node_id)
{
    dbus_int32_t is_buddy = FALSE;
    dbus_int32_t is_contact = FALSE;
    dbus_int32_t is_group = FALSE;
    dbus_int32_t is_chat = FALSE;
    dbus_int32_t is_online = FALSE;
    
    if (!hippo_dbus_proxy_INT32__INT32(proxy, "GaimBlistNodeIsContact", node_id, &is_contact))
        goto failed;

    if (!is_contact) {
        if (!hippo_dbus_proxy_INT32__INT32(proxy, "GaimBlistNodeIsBuddy", node_id, &is_buddy))
            goto failed;
        if (!is_buddy) {
            if (!hippo_dbus_proxy_INT32__INT32(proxy, "GaimBlistNodeIsGroup", node_id, &is_group))
                goto failed;
            if (!is_group) {
                if (!hippo_dbus_proxy_INT32__INT32(proxy, "GaimBlistNodeIsChat", node_id, &is_chat))
                    goto failed;
            }
        }
    }

    if (is_buddy)
        if (!hippo_dbus_proxy_INT32__INT32(proxy, "GaimBuddyIsOnline", node_id, &is_online))
            goto failed;

    g_printerr("blist node %d is_buddy %d is_contact %d is_group %d is_chat %d is_online %d\n",
               node_id, is_buddy, is_contact, is_group, is_chat, is_online);

    return;
    
 failed:
    return;
}

static PidginState*
reload_from_new_owner(DBusConnection *connection,
                      const char     *bus_name)
{
    PidginState *state;
    int i;
    
    state = g_new0(PidginState, 1);
    state->bus_name = g_strdup(bus_name);

    state->gaim_proxy = hippo_dbus_proxy_new(connection, bus_name, GAIM_OBJECT_NAME,
                                             GAIM_INTERFACE_NAME);

    if (!hippo_dbus_proxy_INT32__VOID(state->gaim_proxy,
                                      "GaimGetBlist", &state->blist_id))
        goto failed;

    if (!hippo_dbus_proxy_INT32__VOID(state->gaim_proxy,
                                      "GaimBlistGetRoot", &state->blist_root_id))
        goto failed;

    walk_blist_node(state->gaim_proxy, state->blist_root_id);
    
    if (!hippo_dbus_proxy_ARRAYINT32__VOID(state->gaim_proxy,
                                           "GaimAccountsGetAllActive",
                                           &state->accounts, &state->n_accounts))
        goto failed;                                            

    for (i = 0; i < state->n_accounts; ++i) {
        dbus_int32_t *buddies = NULL;
        dbus_int32_t buddies_len = 0;
        if (!hippo_dbus_proxy_ARRAYINT32__INT32_STRING(state->gaim_proxy,
                                                       "GaimFindBuddies",
                                                       state->accounts[i], "",
                                                       &buddies, &buddies_len))
            goto failed;
        g_printerr("Found %d buddies\n", buddies_len);

        {
            int j;
            for (j = 0; j < buddies_len; ++j) {
                dbus_int32_t is_online;
                char *name;
                
                if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                                   "GaimBuddyIsOnline", buddies[j], &is_online))
                    break;

                name = NULL;
                if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                                    "GaimBuddyGetName", buddies[j], &name))
                    break;
                
                g_printerr("buddy %d '%s' is_online=%d\n", buddies[j], name, is_online);

                g_free(name);
            }
        }

        g_free(buddies);
    }
    
    state->account_presences = g_new0(dbus_int32_t, state->n_accounts);
    for (i = 0; i < state->n_accounts; ++i) {
        if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                           "GaimAccountGetPresence",
                                           state->accounts[i],
                                           &state->account_presences[i]))
            goto failed;
    }

    state->account_statuses = g_new0(PidginStatusList*, state->n_accounts);
    for (i = 0; i < state->n_accounts; ++i) {
        PidginStatusList *status_list;
        
        status_list = g_new0(PidginStatusList, 1);

        if (!hippo_dbus_proxy_ARRAYINT32__INT32(state->gaim_proxy,
                                                "GaimPresenceGetStatuses",
                                                state->account_presences[i],
                                                &status_list->statuses,
                                                &status_list->n_statuses)) {
            pidgin_status_list_free(status_list);
            goto failed;
        }

        state->account_statuses[i] = status_list;
    }

    for (i = 0; i < state->n_accounts; ++i) {
        reload_status_list(state->gaim_proxy, state->account_statuses[i]);
    }
    
    return state;

 failed:
    pidgin_state_free(state);
    return NULL;
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

        g_print("got signal %s\n", dbus_message_get_member(message));
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
