/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-pidgin.h"
#include "hippo-dbus-im.h"

#define GAIM_BUS_NAME "net.sf.gaim.GaimService"
#define GAIM_OBJECT_NAME "/net/sf/gaim/GaimObject"
#define GAIM_INTERFACE_NAME "net.sf.gaim.GaimInterface"

#define PIDGIN_BUS_NAME "im.pidgin.purple.PurpleService"
#define PIDGIN_OBJECT_NAME "/im/pidgin/purple/PurpleObject"
#define PIDGIN_INTERFACE_NAME "im.pidgin.purple.PurpleInterface"

#define PIDGIN_RESOURCE_BASE "online-desktop:/o/pidgin-buddy"

typedef struct {
    dbus_int32_t id;
    char *name;
    dbus_int32_t is_online;
    dbus_int32_t presence_id;
    dbus_int32_t status_id;
} PidginBuddy;

typedef struct {
    dbus_int32_t id;
    char *protocol_id;
    char *protocol_name;
    GSList *buddies;
} PidginAccount;

typedef struct {
    dbus_int32_t id;
    char *name;
} PidginStatus;

typedef struct {
    DBusConnection *connection;
    char *bus_name; /* the well-known name we are using (GAIM_BUS_NAME or PIDGIN_BUS_NAME) */
    HippoDBusProxy *gaim_proxy;
    GSList *accounts;
    GHashTable *statuses;
    GHashTable *resource_ids;
} PidginState;

static void pidgin_buddy_update(HippoNotificationSet *notifications,
                                PidginState          *state,
                                PidginAccount        *account,
                                PidginBuddy          *buddy);

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

    g_hash_table_destroy(state->statuses);
    
    hippo_dbus_proxy_unref(state->gaim_proxy);
    
    g_free(state->bus_name);
    g_free(state);
}

typedef struct {
    HippoNotificationSet *notifications;
    GHashTable *new_resource_ids;
} FindRemovedResourcesClosure;

static void
find_removed_resources_foreach(gpointer key,
                               gpointer value,
                               gpointer data)
{
    const char *resource_id = key;
    FindRemovedResourcesClosure *closure = data;

    if (closure->new_resource_ids == NULL ||
        g_hash_table_lookup(closure->new_resource_ids, resource_id) == NULL)
        hippo_dbus_im_remove_buddy(closure->notifications, resource_id);
}

static void
pidgin_store_state(HippoNotificationSet *notifications)
{
    GSList *tmp;

    if (pidgin_state == NULL)
        return;
    
    for (tmp = pidgin_state->accounts;
         tmp != NULL;
         tmp = tmp->next)
    {
        PidginAccount *account = tmp->data;
        GSList *tmp2;

        for (tmp2 = account->buddies;
             tmp2 != NULL;
             tmp2 = tmp2->next)
        {
            PidginBuddy *buddy = tmp2->data;
            
            pidgin_buddy_update(notifications, pidgin_state, account, buddy);
        }
    }
}

void
hippo_dbus_pidgin_restore_state(void)
{
    pidgin_store_state(NULL);
}

static void
pidgin_state_set(PidginState *new_state)
{
    HippoNotificationSet *notifications;
    FindRemovedResourcesClosure closure;
    
    if (new_state == pidgin_state)
        return;

    notifications = hippo_dbus_im_start_notifications();

    if (notifications) {
        closure.notifications = notifications;
        if (new_state)
            closure.new_resource_ids = new_state->resource_ids;
        else
            closure.new_resource_ids = NULL;

        g_hash_table_foreach(pidgin_state->resource_ids,
                             find_removed_resources_foreach,
                             &closure);
    }
    
    if (pidgin_state)
        pidgin_state_free(pidgin_state);

    pidgin_state = new_state;

    if (notifications) {
        pidgin_store_state(notifications);

        hippo_dbus_im_send_notifications(notifications);
    }
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

static PidginStatus*
pidgin_state_lookup_status(PidginState *state,
                           dbus_int32_t id)
{
    PidginStatus *status;

    status = g_hash_table_lookup(state->statuses, &id);

    if (status == NULL) {
        char *name = NULL;
        
        if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                            "StatusGetName",
                                            id,
                                            &name))
            return NULL;
        
        status = g_new0(PidginStatus, 1);
        status->id = id;
        status->name = name;
        g_hash_table_replace(state->statuses, &status->id, status);

        g_debug("Cached new status %d: %s\n", status->id, status->name);
    }

    return status;
}

static char *
pidgin_buddy_make_resource_id(PidginAccount *account,
                              PidginBuddy   *buddy)
{
    return g_strdup_printf(PIDGIN_RESOURCE_BASE "/%s.%s", account->protocol_id, buddy->name);
}

static void
pidgin_buddy_update(HippoNotificationSet *notifications,
                    PidginState          *state,
                    PidginAccount        *account,
                    PidginBuddy          *buddy)
{
    PidginStatus *pidgin_status;
    const char *protocol;
    const char *name;
    gboolean is_online;
    const char *status;
    char *resource_id;
            
    if (strcmp(account->protocol_id, "prpl-aim") == 0)
        protocol = "aim";
    else
        protocol = "unknown";
            
    name = buddy->name;
    is_online = buddy->is_online;
    
    pidgin_status = pidgin_state_lookup_status(state, buddy->status_id);
    if (pidgin_status != NULL)
        status = pidgin_status->name;
    else
        status = "Unknown";

    resource_id = pidgin_buddy_make_resource_id(account, buddy);

    hippo_dbus_im_update_buddy(notifications, resource_id,
                               protocol, name, is_online, status);

    g_free(resource_id);
}

static void
emit_buddy_changed(PidginState      *state,
                   PidginAccount    *account,
                   PidginBuddy      *buddy)
{
    HippoNotificationSet *notifications;

    notifications = hippo_dbus_im_start_notifications();
    if (notifications) {
        pidgin_buddy_update(notifications, state, account, buddy);
        hippo_dbus_im_send_notifications(notifications);
    }
}

static void
pidgin_buddy_set_online(PidginAccount *account,
                        PidginBuddy   *buddy,
                        dbus_bool_t    is_online)
{
    g_debug("Buddy %s is_online = %d\n", buddy->name, is_online);
    buddy->is_online = is_online;

    emit_buddy_changed(pidgin_state, account, buddy);
}

static void
pidgin_buddy_set_status(PidginAccount *account,
                        PidginBuddy   *buddy,
                        dbus_int32_t   status_id)
{
    g_debug("Buddy %s status_id = %d\n", buddy->name, status_id);
    buddy->status_id = status_id;

    emit_buddy_changed(pidgin_state, account, buddy);
}

static void
pidgin_status_free(PidginStatus *status)
{
    g_free(status->name);
    g_free(status);
}

static void
pidgin_status_free_hash_value(void *data)
{
    pidgin_status_free(data);
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
    state->connection = connection;
    state->bus_name = g_strdup(bus_name);
                                                

    state->statuses = g_hash_table_new_full(g_int_hash, g_int_equal,
                                            NULL, pidgin_status_free_hash_value);
    
    state->resource_ids = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                g_free, NULL);
                                                
    if (strcmp(bus_name, GAIM_BUS_NAME) == 0) {
        state->gaim_proxy = hippo_dbus_proxy_new(connection, bus_name, GAIM_OBJECT_NAME,
                                                 GAIM_INTERFACE_NAME);
        hippo_dbus_proxy_set_method_prefix(state->gaim_proxy, "Gaim");
    } else if (strcmp(bus_name, PIDGIN_BUS_NAME) == 0) {
        state->gaim_proxy = hippo_dbus_proxy_new(connection, bus_name, PIDGIN_OBJECT_NAME,
                                                 PIDGIN_INTERFACE_NAME);
        hippo_dbus_proxy_set_method_prefix(state->gaim_proxy, "Purple");
    } else {
        goto failed;
    }
    
    if (!hippo_dbus_proxy_ARRAYINT32__VOID(state->gaim_proxy,
                                           "AccountsGetAllActive",
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
                                            "AccountGetProtocolId",
                                            account->id,
                                            &account->protocol_id))
            goto failed;

        if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                            "AccountGetProtocolName",
                                            account->id,
                                            &account->protocol_name))
            goto failed;
        
        g_debug("Account %d id '%s' name '%s'\n", account->id,
                account->protocol_id, account->protocol_name);
    }

    for (tmp = state->accounts; tmp != NULL; tmp = tmp->next) {
        PidginAccount *account;
        dbus_int32_t *buddies = NULL;
        dbus_int32_t buddies_len = 0;

        account = tmp->data;
        
        if (!hippo_dbus_proxy_ARRAYINT32__INT32_STRING(state->gaim_proxy,
                                                       "FindBuddies",
                                                       account->id, "",
                                                       &buddies, &buddies_len))
            goto failed;

        g_debug("Found %d buddies in account %d\n", buddies_len, account->id);
        
        {
            int j;
            for (j = 0; j < buddies_len; ++j) {
                PidginBuddy *buddy;

                buddy = g_new0(PidginBuddy, 1);
                buddy->id = buddies[j];
                account->buddies = g_slist_prepend(account->buddies, buddy);
                
                if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                                   "BuddyIsOnline",
                                                   buddy->id, &buddy->is_online))
                    goto failed;
                
                if (!hippo_dbus_proxy_STRING__INT32(state->gaim_proxy,
                                                    "BuddyGetName",
                                                    buddy->id, &buddy->name))
                    goto failed;

                if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                                   "BuddyGetPresence",
                                                   buddy->id, &buddy->presence_id))
                    goto failed;

                if (!hippo_dbus_proxy_INT32__INT32(state->gaim_proxy,
                                                   "PresenceGetActiveStatus",
                                                   buddy->presence_id, &buddy->status_id))
                    goto failed;
                
                g_debug("buddy %d '%s' is_online=%d presence_id=%d status_id=%d\n",
                        buddy->id, buddy->name, buddy->is_online,
                        buddy->presence_id, buddy->status_id);

                g_hash_table_replace(state->resource_ids,
                                     pidgin_buddy_make_resource_id(account, buddy), GUINT_TO_POINTER(1));
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
        
    } else if (type == DBUS_MESSAGE_TYPE_ERROR) {

    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {

        if (dbus_message_has_member(message, "BuddySignedOn") ||
            dbus_message_has_member(message, "BuddySignedOff")) {
            dbus_int32_t buddy_id = 0;
            dbus_uint64_t buddy_id_64 = 0; /* if Gaim was compiled on a 64-bit system it does this */
            dbus_bool_t is_online;
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_INT32, &buddy_id, DBUS_TYPE_INVALID))
                g_debug(" buddy id was %d\n", buddy_id);
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_UINT64, &buddy_id_64, DBUS_TYPE_INVALID)) {
                g_debug(" buddy id was %d\n", (dbus_int32_t) buddy_id_64);
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
                        pidgin_buddy_set_online(account, buddy, is_online);
                        break;
                    }
                }
            }
        } else if (dbus_message_has_member(message, "BuddyStatusChanged")) {
            dbus_int32_t buddy_id = 0;
            dbus_uint64_t buddy_id_64 = 0; /* if Gaim was compiled on a 64-bit system it does this */
            dbus_int32_t old_status_id = 0;
            dbus_uint64_t old_status_id_64 = 0;
            dbus_int32_t new_status_id = 0;
            dbus_uint64_t new_status_id_64 = 0;
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_INT32, &buddy_id,
                                      DBUS_TYPE_INT32, &old_status_id,
                                      DBUS_TYPE_INT32, &new_status_id,
                                      DBUS_TYPE_INVALID))
                g_debug("got 32-bit status changed message\n");

            if (dbus_message_get_args(message, NULL, DBUS_TYPE_UINT64, &buddy_id_64,
                                      DBUS_TYPE_UINT64, &old_status_id_64,
                                      DBUS_TYPE_UINT64, &new_status_id_64,
                                      DBUS_TYPE_INVALID)) {
                g_debug("got 64-bit status changed message\n");
                buddy_id = buddy_id_64;
                old_status_id = old_status_id_64;
                new_status_id = new_status_id_64;
            }

            g_debug(" buddy id was %d old status id %d new status id %d\n", buddy_id,
                    old_status_id, new_status_id);

            if (buddy_id != 0 && pidgin_state) {
                PidginStatus *new_status;
                GSList *tmp;

                new_status = pidgin_state_lookup_status(pidgin_state, new_status_id);
                if (new_status != NULL)
                    g_debug("New status %d '%s'\n", new_status->id, new_status->name);
                
                for (tmp = pidgin_state->accounts;
                     tmp != NULL;
                     tmp = tmp->next) {
                    PidginAccount *account = tmp->data;
                    PidginBuddy *buddy;

                    buddy = pidgin_account_get_buddy(account, buddy_id);
                    if (buddy != NULL) {
                        pidgin_buddy_set_status(account, buddy, new_status_id);
                        break;
                    }
                }
            }
        } else if (dbus_message_has_member(message, "BuddyIdleChanged")) {
            dbus_int32_t buddy_id = 0;
            dbus_uint64_t buddy_id_64 = 0; /* if Gaim was compiled on a 64-bit system it does this */
            /* the idles are always 32-bit since they aren't gaim "object ids" just ints */
            dbus_int32_t old_idle = 0;
            dbus_int32_t new_idle = 0;
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_INT32, &buddy_id,
                                      DBUS_TYPE_INT32, &old_idle,
                                      DBUS_TYPE_INT32, &new_idle,
                                      DBUS_TYPE_INVALID))
                g_debug("got 32-bit idle changed message\n");
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_UINT64, &buddy_id_64,
                                      DBUS_TYPE_INT32, &old_idle,
                                      DBUS_TYPE_INT32, &new_idle,
                                      DBUS_TYPE_INVALID)) {
                g_debug("got 64-bit idle changed message\n");
                buddy_id = (dbus_int32_t) buddy_id_64;
            }

            g_debug(" buddy id was %d old idle %d new idle %d\n", buddy_id,
                    old_idle, new_idle);
            
            if (buddy_id != 0 && pidgin_state) {
                
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
                    strcmp(pidgin_state->bus_name, name) == 0) {
                    g_debug("Old Gaim/Pidgin (%s owned by %s) going away", name, old);
                    pidgin_state_set(NULL);
                }
                
                if (new && (strcmp(name, GAIM_BUS_NAME) == 0 || strcmp(name, PIDGIN_BUS_NAME) == 0)) {
                    PidginState *state;
                    
                    g_debug("New Gaim/Pidgin (%s) appeared", new);
                    
                    state = reload_from_new_owner(connection, name);
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

    state = NULL;
    if (dbus_bus_name_has_owner(connection, PIDGIN_BUS_NAME, NULL))
        state = reload_from_new_owner(connection, PIDGIN_BUS_NAME);
    if (state == NULL && dbus_bus_name_has_owner(connection, GAIM_BUS_NAME, NULL))
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
