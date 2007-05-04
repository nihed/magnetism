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
    dbus_int32_t id;
    char *name;
    dbus_int32_t is_online;
} PidginBuddy;

typedef struct {
    dbus_int32_t id;
    char *protocol_id;
    char *protocol_name;
    GSList *buddies;
} PidginAccount;

typedef struct {
    char *bus_name;
    HippoDBusProxy *gaim_proxy;
    GSList *accounts;
} PidginState;

static PidginState *pidgin_state = NULL;

static void
pidgin_buddy_free(PidginBuddy *buddy)
{
    g_free(buddy->name);
    g_free(buddy);
}

static void
pidgin_account_free(PidginAccount *account)
{
    while (account->buddies) {
        PidginBuddy *buddy = account->buddies->data;
        account->buddies = g_slist_remove(account->buddies, account->buddies->data);

        pidgin_buddy_free(buddy);
    }

    g_free(account->protocol_id);
    g_free(account->protocol_name);
    g_free(account);
}

static void
pidgin_state_free(PidginState *state)
{
    while (state->accounts) {
        PidginAccount *account = state->accounts->data;
        state->accounts = g_slist_remove(state->accounts, state->accounts->data);

        pidgin_account_free(account);
    }
    
    hippo_dbus_proxy_unref(state->gaim_proxy);
    
    g_free(state->bus_name);
    g_free(state);
}

static void
pidgin_state_set(PidginState *new_state)
{
    if (new_state == pidgin_state)
        return;
    
    if (pidgin_state)
        pidgin_state_free(pidgin_state);

    pidgin_state = new_state;
}

static PidginBuddy*
pidgin_account_get_buddy(PidginAccount *account,
                         dbus_int32_t   buddy_id)
{
    GSList *tmp;

    for (tmp = account->buddies; tmp != NULL; tmp = tmp->next) {
        PidginBuddy *buddy = tmp->data;

        if (buddy->id == buddy_id) {
            return buddy;
        }
    }
    return NULL;
}

static void
pidgin_buddy_set_online(PidginBuddy *buddy,
                        dbus_bool_t  is_online)
{
    g_printerr("Buddy %s is_online = %d\n", buddy->name, is_online);
    buddy->is_online = is_online;
}

static PidginState*
reload_from_new_owner(DBusConnection *connection,
                      const char     *bus_name)
{
    PidginState *state;
    int i;
    dbus_int32_t *accounts;
    dbus_int32_t n_accounts;
    GSList *tmp;
    
    state = g_new0(PidginState, 1);
    state->bus_name = g_strdup(bus_name);

    if (strcmp(bus_name, GAIM_BUS_NAME) == 0)
        state->gaim_proxy = hippo_dbus_proxy_new(connection, bus_name, GAIM_OBJECT_NAME,
                                                 GAIM_INTERFACE_NAME);
    else if (strcmp(bus_name, PIDGIN_BUS_NAME) == 0)
        state->gaim_proxy = hippo_dbus_proxy_new(connection, bus_name, PIDGIN_OBJECT_NAME,
                                                 PIDGIN_INTERFACE_NAME);
    else
        goto failed;
    
    if (!hippo_dbus_proxy_ARRAYINT32__VOID(state->gaim_proxy,
                                           "GaimAccountsGetAllActive",
                                           &accounts, &n_accounts))
        goto failed;                                            

    for (i = 0; i < n_accounts; ++i) {
        PidginAccount *account;

        account = g_new0(PidginAccount, 1);
        account->id = accounts[i];
        state->accounts = g_slist_append(state->accounts, account);
    }
    g_free(accounts);
    
    for (tmp = state->accounts; tmp != NULL; tmp = tmp->next) {
        PidginAccount *account;

        account = tmp->data;
        
        if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                            "GaimAccountGetProtocolId",
                                            account->id,
                                            &account->protocol_id))
            goto failed;

        if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                            "GaimAccountGetProtocolName",
                                            account->id,
                                            &account->protocol_name))
            goto failed;
        
        g_printerr("Account %d id '%s' name '%s'\n", account->id,
                   account->protocol_id, account->protocol_name);
    }

    for (tmp = state->accounts; tmp != NULL; tmp = tmp->next) {
        PidginAccount *account;
        dbus_int32_t *buddies = NULL;
        dbus_int32_t buddies_len = 0;

        account = tmp->data;
        
        if (!hippo_dbus_proxy_ARRAYINT32__INT32_STRING(state->gaim_proxy,
                                                       "GaimFindBuddies",
                                                       account->id, "",
                                                       &buddies, &buddies_len))
            goto failed;
        g_printerr("Found %d buddies in account %d\n", buddies_len, account->id);

        {
            int j;
            for (j = 0; j < buddies_len; ++j) {
                PidginBuddy *buddy;

                buddy = g_new0(PidginBuddy, 1);
                buddy->id = buddies[j];
                account->buddies = g_slist_prepend(account->buddies, buddy);
                
                if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                                   "GaimBuddyIsOnline",
                                                   buddy->id, &buddy->is_online))
                    goto failed;
                
                if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                                    "GaimBuddyGetName",
                                                    buddy->id, &buddy->name))
                    goto failed;
                
                g_printerr("buddy %d '%s' is_online=%d\n", buddy->id, buddy->name, buddy->is_online);
            }
        }
        
        g_free(buddies);
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

        g_print("got signal %s signature %s\n", dbus_message_get_member(message),
                dbus_message_get_signature(message));

        if (dbus_message_has_member(message, "BuddySignedOn") ||
            dbus_message_has_member(message, "BuddySignedOff")) {
            dbus_int32_t buddy_id = 0;
            dbus_uint64_t buddy_id_64 = 0; /* if Gaim was compiled on a 64-bit system it does this */
            dbus_bool_t is_online;
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_INT32, &buddy_id, DBUS_TYPE_INVALID))
                g_print(" buddy id was %d\n", buddy_id);
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_UINT64, &buddy_id_64, DBUS_TYPE_INVALID)) {
                g_print(" buddy id was %d\n", (dbus_int32_t) buddy_id_64);
                buddy_id = (dbus_int32_t) buddy_id_64;
            }

            is_online = dbus_message_has_member(message, "BuddySignedOn");
            
            if (buddy_id != 0 && pidgin_state) {
                GSList *tmp;

                for (tmp = pidgin_state->accounts;
                     tmp != NULL;
                     tmp = tmp->next) {
                    PidginAccount *account = tmp->data;
                    PidginBuddy *buddy;

                    buddy = pidgin_account_get_buddy(account, buddy_id);
                    if (buddy != NULL) {
                        pidgin_buddy_set_online(buddy, is_online);
                        break;
                    }
                }
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_DBUS,
                                          "NameOwnerChanged")) {
            const char *name = NULL;
            const char *old = NULL;
            const char *new = NULL;
            if (dbus_message_get_args(message, NULL,
                                      DBUS_TYPE_STRING, &name,
                                      DBUS_TYPE_STRING, &old,
                                      DBUS_TYPE_STRING, &new,
                                      DBUS_TYPE_INVALID)) {
                g_debug("pidgin.c NameOwnerChanged %s '%s' -> '%s'", name, old, new);
                if (*old == '\0')
                    old = NULL;
                if (*new == '\0')
                    new = NULL;

                /* If old gaim goes away, drop the state */
                if (old && pidgin_state &&
                    strcmp(pidgin_state->bus_name, old) == 0) {
                    g_debug("Old Gaim/Pidgin (%s) going away", old);
                    pidgin_state_set(NULL);
                }
                
                if (new && (strcmp(new, GAIM_BUS_NAME) == 0 || strcmp(new, PIDGIN_BUS_NAME) == 0)) {
                    PidginState *state;

                    g_debug("New Gaim/Pidgin (%s) appeared", new);
                    
                    state = reload_from_new_owner(connection, new);
                    if (state != NULL) {
                        pidgin_state_set(state);
                    }
                }
            } else {
                g_warning("NameOwnerChanged had wrong args???");
            }
        } else if (dbus_message_is_signal(message, GAIM_INTERFACE_NAME, "BuddyAdded") ||
                   dbus_message_is_signal(message, GAIM_INTERFACE_NAME, "BuddyRemoved") ||
                   dbus_message_is_signal(message, PIDGIN_INTERFACE_NAME, "BuddyAdded") ||
                   dbus_message_is_signal(message, PIDGIN_INTERFACE_NAME, "BuddyRemoved")) {
            PidginState *state;
            
            g_debug("Reloading Pidgin state due to buddy list change");
            
            if (pidgin_state) {
                state = reload_from_new_owner(connection, pidgin_state->bus_name);
                if (state != NULL) {
                    pidgin_state_set(state);
                }
            }
        }
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
    PidginState *state;
    
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

    state = reload_from_new_owner(connection, PIDGIN_BUS_NAME);
    if (state == NULL)
        state = reload_from_new_owner(connection, GAIM_BUS_NAME);
    if (state != NULL) {
        pidgin_state_set(state);
    }
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
