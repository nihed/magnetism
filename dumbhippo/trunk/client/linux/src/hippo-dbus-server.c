#include <config.h>
#include <glib/gi18n-lib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-server.h"
#include "hippo-dbus-client.h"
#include "main.h"

static void      hippo_dbus_init                (HippoDBus       *dbus);
static void      hippo_dbus_class_init          (HippoDBusClass  *klass);

static void      hippo_dbus_finalize            (GObject               *object);

static DBusHandlerResult handle_message         (DBusConnection     *connection,
                                                 DBusMessage        *message,
                                                 void               *user_data);

enum {
    DISCONNECTED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];  

struct _HippoDBus {
    GObject parent;
    char           *bus_name;
    DBusConnection *connection;
    unsigned int in_dispatch : 1; /* dbus is broken and we can't recurse right now */
    unsigned int requested_disconnect : 1;
    unsigned int processed_disconnected : 1;
};

struct _HippoDBusClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoDBus, hippo_dbus, G_TYPE_OBJECT);

static void
hippo_dbus_init(HippoDBus  *dbus)
{

}

static void
hippo_dbus_class_init(HippoDBusClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_dbus_finalize;
    
    signals[DISCONNECTED] =
        g_signal_new ("disconnected",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);
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

HippoDBus*
hippo_dbus_try_to_acquire(const char  *server,
                          gboolean     replace_existing,
                          GError     **error)
{
    HippoDBus *dbus;
    DBusGConnection *gconnection;
    DBusConnection *connection;
    char *bus_name;
    int result;
    DBusError derror;
    unsigned int flags;
    
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

    bus_name = hippo_dbus_full_bus_name(server);
    
    flags = DBUS_NAME_FLAG_DO_NOT_QUEUE | DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace_existing)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
    
    dbus_error_init(&derror);
    result = dbus_bus_request_name(connection, bus_name,
                        flags,
                        &derror);
    if (dbus_error_is_set(&derror)) {
        g_free(bus_name);
        propagate_dbus_error(error, &derror);
        return NULL;
    }
    
    if (!(result == DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER ||
          result == DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER)) {
        g_free(bus_name);
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("Another copy of %s is already running in this session for server %s"),
                    g_get_application_name(), server);
        return NULL;
    }

    /* the connection is already set up with the main loop. 
     * We just need to create our object, filters, etc. 
     */
    g_debug("D-BUS connection established");

    dbus = g_object_new(HIPPO_TYPE_DBUS, NULL);
    dbus->bus_name = bus_name;
    dbus->connection = connection;
    
    if (!dbus_connection_add_filter(connection, handle_message,
                                    dbus, NULL))
        g_error("no memory adding dbus connection filter");

    /* add an extra ref, which is owned by the "connected" state on 
     * the connection. We drop it in our filter func if we get 
     * the disconnected message.
     */
    g_object_ref(dbus);

    /* we'll deal with this ourselves */
    dbus_connection_set_exit_on_disconnect(connection, FALSE);

    /* also returning a ref to the caller */    
    return dbus;
}

static void
hippo_dbus_finalize(GObject *object)
{
    HippoDBus *dbus = HIPPO_DBUS(object);

    g_debug("Finalizing dbus object");

    g_free(dbus->bus_name);
    /* assumes connection is disconnected */
    dbus_connection_unref(dbus->connection);
    
    G_OBJECT_CLASS(hippo_dbus_parent_class)->finalize(object);
}

void
hippo_dbus_disconnect(HippoDBus *dbus)
{
    if (dbus->requested_disconnect)
        return;
    dbus->requested_disconnect = TRUE;
    /* will send back a message, processed in the main loop, that unrefs us */
    dbus_connection_close(dbus->connection);    
}

void
hippo_dbus_blocking_shutdown(HippoDBus   *dbus)
{
    /* disconnect if we haven't */
    hippo_dbus_disconnect(dbus);

    /* this processed_disconnected flag is to avoid recursive 
     * dispatch, which current dbus doesn't like
     */
    if (!dbus->processed_disconnected) {
        while (dbus_connection_read_write_dispatch(dbus->connection, -1))
            ; /* nothing */
    }
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
               DBusMessage        *message,
               void               *user_data)
{
    HippoDBus *dbus;
    int type;
    DBusHandlerResult result;
    
    dbus = HIPPO_DBUS(user_data);
    
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
    
        if (interface && path && member && 
            strcmp(interface, HIPPO_DBUS_INTERFACE) == 0 &&
            strcmp(path, HIPPO_DBUS_PATH) == 0) {
            DBusMessage *reply;
            
            reply = NULL;
            result = DBUS_HANDLER_RESULT_HANDLED;
            
            if (strcmp(member, "JoinChat") == 0) {
                const char *chat_id = NULL;
                const char *kind_str = NULL;
                if (!dbus_message_get_args(message, NULL,
                    DBUS_TYPE_STRING, &chat_id,
                    DBUS_TYPE_STRING, &kind_str,
                    DBUS_TYPE_INVALID)) {
                    reply = dbus_message_new_error(message,
                                                   DBUS_ERROR_INVALID_ARGS,
                                                   _("Expected two string args, chat ID and chat kind"));
                } else {
                    HippoChatKind kind;
                    
                    kind = hippo_parse_chat_kind(kind_str);
                    
                    if (!hippo_verify_guid(chat_id)) {
                        reply = dbus_message_new_error_printf(message,
                                                       DBUS_ERROR_INVALID_ARGS,
                                                       _("Invalid chat ID '%s'"), chat_id);
                    } else if (kind == HIPPO_CHAT_KIND_BROKEN) {
                        reply = dbus_message_new_error_printf(message, DBUS_ERROR_INVALID_ARGS,
                                                       _("Invalid chat kind '%s' try group,post,unknown"),
                                                       kind_str);
                    } else {
                        /* heh, for now we don't even use the kind after all that trouble */
                        hippo_app_join_chat(hippo_get_app(), chat_id);
                        reply = dbus_message_new_method_return(message);
                    }
                }
                g_assert(reply != NULL);
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
            /* If we lose our name, we disconnect and exit */
            const char *name = NULL;
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_STRING, &name, DBUS_TYPE_INVALID) && 
                strcmp(name, dbus->bus_name) == 0) {
                hippo_dbus_disconnect(dbus);
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            /* the "connected" state owns one ref on the HippoDBus */
            dbus->processed_disconnected = TRUE;
            g_signal_emit(G_OBJECT(dbus), signals[DISCONNECTED], 0);
            g_object_unref(dbus);
            dbus = NULL;
        }
    } else {
        g_debug("got message type %s\n", 
                dbus_message_type_to_string(type));    
    }
    
    if (dbus)
        dbus->in_dispatch = FALSE;
        
    return result;
}
