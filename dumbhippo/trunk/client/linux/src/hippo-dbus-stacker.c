/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-stacker.h"
#include "hippo-dbus-client.h"
#include <stacker/hippo-stack-manager.h>
#include "main.h"

typedef struct _HippoDBusStackerListener HippoDBusStackerListener;


static void hippo_dbus_stacker_finalize     (GObject          *object);
static void hippo_dbus_stacker_disconnected (HippoDBusStacker *dbus);

static DBusHandlerResult handle_message         (DBusConnection     *connection,
                                                 DBusMessage        *message,
                                                 void               *user_data);

enum {
    DISCONNECTED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];  

struct _HippoDBusStacker {
    GObject parent;
    char           *stacker_bus_name;    
    DBusConnection *connection;
    unsigned int in_dispatch : 1; /* dbus is broken and we can't recurse right now */
    unsigned int emitted_disconnected : 1;
};

struct _HippoDBusStackerClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoDBusStacker, hippo_dbus_stacker, G_TYPE_OBJECT);

static void
hippo_dbus_stacker_init(HippoDBusStacker  *dbus)
{

}

static void
hippo_dbus_stacker_class_init(HippoDBusStackerClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_dbus_stacker_finalize;
    
    signals[DISCONNECTED] =
        g_signal_new ("disconnected",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

DBusConnection*
hippo_dbus_stacker_get_connection(HippoDBusStacker *dbus)
{
    return dbus->connection;
}

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

static gboolean
acquire_bus_name(DBusConnection *connection,
                 const char     *server,
                 gboolean        replace_existing,
                 const char     *bus_name,
                 GError        **error)
{
    DBusError derror;
    unsigned int flags;
    int result;
    
    flags = DBUS_NAME_FLAG_DO_NOT_QUEUE | DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace_existing)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
    
    dbus_error_init(&derror);
    result = dbus_bus_request_name(connection, bus_name,
                                   flags,
                                   &derror);
    if (dbus_error_is_set(&derror)) {
        propagate_dbus_error(error, &derror);
        return FALSE;
    }
    
    if (!(result == DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER ||
          result == DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_ALREADY_RUNNING,
                    _("Another copy of %s is already running in this session for server %s"),
                    g_get_application_name(), server);               
        return FALSE;
    }

    g_debug("Acquired bus name %s", bus_name);
    return TRUE;
}

HippoDBusStacker*
hippo_dbus_stacker_try_to_acquire(const char  *stacker_server,
                                  gboolean     replace_existing,
                                  GError     **error)
{
    HippoDBusStacker *dbus;
    DBusGConnection *gconnection;
    DBusConnection *connection;
    char *stacker_bus_name;
    char *old_bus_name;
    DBusError derror;
    
    dbus_error_init(&derror);

    /* dbus_bus_get is a little hosed since you can't unref 
     * unless you know it's disconnected. I guess it turns
     * out we more or less want to do that anyway.
     */
    
    gconnection = dbus_g_bus_get(DBUS_BUS_SESSION, error);
    if (gconnection == NULL)
        return NULL;
    
    connection = dbus_g_connection_get_connection(gconnection);
    
    /* the purpose of this check is to be sure we will get a "Disconnected"
     * message in the future
     */
    if (!dbus_connection_get_is_connected(connection)) {
        dbus_connection_unref(connection);
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED, 
            _("No active connection to the session's message bus"));
        return NULL;
    }

    stacker_bus_name = hippo_dbus_full_bus_name_com_dumbhippo_with_forward_hex(stacker_server);
    if (!acquire_bus_name(connection, stacker_server, replace_existing, stacker_bus_name, error)) {
        g_free(stacker_bus_name);
        /* FIXME leak bus connection since unref isn't allowed */
        return NULL;
    }

    /* We also acquire our old broken bus name for compatibility with old versions
     * of client apps, and so that when this version is run with --replace
     * old versions will exit. We *don't* exit if we lose ownership of this
     * name ourself, since we running the older version with --replace to replace
     * the new version isn't very interesting
     *
     * Since this is compat gunk, it only uses the stacker server.
     */
    
    old_bus_name = hippo_dbus_full_bus_name_com_dumbhippo_with_backward_hex(stacker_server);
    if (!acquire_bus_name(connection, stacker_server, replace_existing, old_bus_name, error)) {
        /* FIXME leak bus connection since unref isn't allowed */

        /* We need to give up the new bus name because we call ShowBrowser on
         * it in main.c if we fail to get both names, which deadlocks if
         * we own the new name
         */
        dbus_bus_release_name(connection, stacker_bus_name, NULL);
        
        g_free(old_bus_name);
        g_free(stacker_bus_name);
        
        return NULL;
    }

    g_free(stacker_bus_name);
    g_free(old_bus_name);

    /* Now we acquire the names without the server host/port appended,
     * we only optionally acquire these if nobody else has them.
     * This allows apps to avoid adding the server to the name - 
     * they can just use the "normal" name
     */
    {
    	DBusError tmp_derror;
        dbus_uint32_t flags;
        
    	dbus_error_init(&tmp_derror);        
        
        /* We do want to be queued if we don't get this right away */
        flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
        if (replace_existing)
            flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
        
        /* we just ignore errors on this */
        dbus_bus_request_name(connection, HIPPO_DBUS_ENGINE_BASE_BUS_NAME,
                              flags,
                              &tmp_derror);
        if (dbus_error_is_set(&tmp_derror))
        	g_debug("Failed to get bus name %s: %s", HIPPO_DBUS_ENGINE_BASE_BUS_NAME, tmp_derror.message);
       	else
       		g_debug("Acquired bus name %s", HIPPO_DBUS_ENGINE_BASE_BUS_NAME);       	
    }

    {
    	DBusError tmp_derror;    
        dbus_uint32_t flags;
        
    	dbus_error_init(&tmp_derror);        
        
        /* We do want to be queued if we don't get this right away */
        flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
        if (replace_existing)
            flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
        
        /* we just ignore errors on this */
        dbus_bus_request_name(connection, HIPPO_DBUS_STACKER_BASE_BUS_NAME,
                              flags,
                              &tmp_derror);
        if (dbus_error_is_set(&tmp_derror))
        	g_debug("Failed to get bus name %s: %s", HIPPO_DBUS_STACKER_BASE_BUS_NAME, tmp_derror.message);
       	else
       		g_debug("Acquired bus name %s", HIPPO_DBUS_STACKER_BASE_BUS_NAME);                              
    }
    
    /* the connection is already set up with the main loop. 
     * We just need to create our object, filters, etc. 
     */
    g_debug("D-BUS connection established");

    dbus = g_object_new(HIPPO_TYPE_DBUS_STACKER, NULL);
    dbus->stacker_bus_name = stacker_bus_name;
    dbus->connection = connection;
    
    if (!dbus_connection_add_filter(connection, handle_message,
                                    dbus, NULL))
        g_error("no memory adding dbus connection filter");

    /* add an extra ref, which is owned by the "connected" state on 
     * the connection. We drop it in our filter func if we get 
     * the disconnected message or lose our bus name.
     */
    g_object_ref(dbus);

    /* we'll deal with this ourselves */
    dbus_connection_set_exit_on_disconnect(connection, FALSE);

    /* also returning a ref to the caller */    
    return dbus;
}

static void
hippo_dbus_stacker_finalize(GObject *object)
{
    HippoDBusStacker *dbus = HIPPO_DBUS_STACKER(object);

    g_debug("Finalizing dbus object");

    if (!dbus->emitted_disconnected)
        g_warning("Messed-up reference counting on HippoDBusStacker object - connected state should own a ref");
    
    g_free(dbus->stacker_bus_name);

#ifdef HAVE_DBUS_1_0
    /* pre-1.0 dbus is all f'd up and may crash if we do this when the
     * connection is still connected.
     */
    dbus_connection_unref(dbus->connection);
#endif
    
    G_OBJECT_CLASS(hippo_dbus_stacker_parent_class)->finalize(object);
}

static void
hippo_dbus_stacker_disconnected(HippoDBusStacker *dbus)
{
    if (dbus->emitted_disconnected)
        return;

    dbus->emitted_disconnected = TRUE;
    
    /* the "connected" state owns one ref on the HippoDBusStacker */
    g_signal_emit(G_OBJECT(dbus), signals[DISCONNECTED], 0);
    g_object_unref(dbus);
}

static DBusMessage*
handle_show_browser(HippoDBusStacker   *dbus,
                    DBusMessage *message)
{
    DBusMessage *reply;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected no arguments"));
    }

    hippo_stack_manager_show_browser(hippo_stacker_app_get_stack(hippo_get_stacker_app()),
                                     FALSE);
    
    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
               DBusMessage        *message,
               void               *user_data)
{
    HippoDBusStacker *dbus;
    int type;
    DBusHandlerResult result;
    
    dbus = HIPPO_DBUS_STACKER(user_data);
    
    type = dbus_message_get_type(message);

    result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    dbus->in_dispatch = TRUE;

    if (type == DBUS_MESSAGE_TYPE_METHOD_CALL) {
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);
        const char *path = dbus_message_get_path(message);        
        
        g_debug("method call from %s %s.%s on %s", sender ? sender : "NULL",
                interface ? interface : "NULL",
                member ? member : "NULL",
                path ? path : "NULL");
    
        if (path && member &&
            strcmp(path, HIPPO_DBUS_STACKER_PATH) == 0) {
            DBusMessage *reply;
            
            reply = NULL;
            result = DBUS_HANDLER_RESULT_HANDLED;
            
            if (strcmp(member, "ShowBrowser") == 0) {
                reply = handle_show_browser(dbus, message);
            } else {
                /* Set this back so the default handler can return an error */
                result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
            }
            
            if (reply != NULL) {
                dbus_connection_send(dbus->connection, reply, NULL);
                dbus_message_unref(reply);
            }
        }        
    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);

        g_debug("signal from %s %s.%s", sender ? sender : "NULL", interface, member);
   
        if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
            dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameLost")) {
            /* If we lose our name, we behave as if disconnected */
            const char *name = NULL;
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_STRING, &name, DBUS_TYPE_INVALID) && 
                (strcmp(name, dbus->stacker_bus_name) == 0)) {

                hippo_dbus_stacker_disconnected(dbus);
                dbus = NULL;
            } else {
                g_warning("NameOwnerChanged had wrong args???");
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            hippo_dbus_stacker_disconnected(dbus);
            dbus = NULL;
        }
    } else if (dbus_message_get_type(message) == DBUS_MESSAGE_TYPE_ERROR) {
        hippo_dbus_debug_log_error("main connection handler", message);
    } else {
        /* g_debug("got message type %s\n", 
           dbus_message_type_to_string(type));    */
        ;
    }
    
    if (dbus)
        dbus->in_dispatch = FALSE;
        
    return result;
}
