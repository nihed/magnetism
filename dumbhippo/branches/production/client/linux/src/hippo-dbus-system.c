/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-system.h"
#include "hippo-dbus-client.h"
#include "main.h"

static void      hippo_system_dbus_init                (HippoSystemDBus       *dbus);
static void      hippo_system_dbus_class_init          (HippoSystemDBusClass  *klass);

static void      hippo_system_dbus_finalize            (GObject          *object);

static void      hippo_system_dbus_disconnected        (HippoSystemDBus        *dbus);

static DBusHandlerResult handle_message         (DBusConnection     *connection,
                                                 DBusMessage        *message,
                                                 void               *user_data);

enum {
    DISCONNECTED,
    NETWORK_STATUS_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];  

struct _HippoSystemDBus {
    GObject parent;
    DBusConnection *connection;
    unsigned int emitted_disconnected : 1;
};

struct _HippoSystemDBusClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoSystemDBus, hippo_system_dbus, G_TYPE_OBJECT);

static void
hippo_system_dbus_init(HippoSystemDBus  *dbus)
{

}

static void
hippo_system_dbus_class_init(HippoSystemDBusClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_system_dbus_finalize;
    
    signals[DISCONNECTED] =
        g_signal_new ("disconnected",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
    
    signals[NETWORK_STATUS_CHANGED] =
        g_signal_new ("network-status-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__INT,
                      G_TYPE_NONE, 1, G_TYPE_INT);
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

static void
hippo_system_dbus_finalize(GObject *object)
{
    HippoSystemDBus *dbus = HIPPO_SYSTEM_DBUS(object);

    g_debug("Finalizing dbus object");

    if (!dbus->emitted_disconnected)
        g_warning("Messed-up reference counting on HippoSystemDBus object - connected state should own a ref");
    

#ifdef HAVE_DBUS_1_0
    /* pre-1.0 dbus is all f'd up and may crash if we do this when the
     * connection is still connected.
     */
    dbus_connection_unref(dbus->connection);
#endif
    
    G_OBJECT_CLASS(hippo_system_dbus_parent_class)->finalize(object);
}

static void
hippo_system_dbus_disconnected(HippoSystemDBus *dbus)
{
    if (dbus->emitted_disconnected)
        return;

    dbus->emitted_disconnected = TRUE;
    
    /* the "connected" state owns one ref on the HippoSystemDBus */
    g_signal_emit(G_OBJECT(dbus), signals[DISCONNECTED], 0);
    g_object_unref(dbus);
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
               DBusMessage        *message,
               void               *user_data)
{
    HippoSystemDBus *dbus;
    int type;
    DBusHandlerResult result;
    
    dbus = HIPPO_SYSTEM_DBUS(user_data);
    
    type = dbus_message_get_type(message);

    result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
        
    if (type == DBUS_MESSAGE_TYPE_METHOD_CALL) {

    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);

        g_debug("system bus signal from %s %s.%s", sender ? sender : "NULL", interface, member);
   
        if (dbus_message_is_signal(message, "org.freedesktop.NetworkManager", "StateChange")) {
            dbus_uint32_t v_UINT32;
            
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_UINT32, &v_UINT32, DBUS_TYPE_INVALID)) {
                HippoNetworkStatus status = HIPPO_NETWORK_STATUS_UNKNOWN;
                if (v_UINT32 == 3)
                    status = HIPPO_NETWORK_STATUS_UP;
                else if (v_UINT32 == 1)
                    status = HIPPO_NETWORK_STATUS_DOWN;

                g_debug("new network status from network manager %u", v_UINT32);
                
                g_signal_emit(G_OBJECT(dbus), signals[NETWORK_STATUS_CHANGED], 0, status);
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            hippo_system_dbus_disconnected(dbus);
            dbus = NULL;
        }
    } else if (dbus_message_get_type(message) == DBUS_MESSAGE_TYPE_ERROR) {
        hippo_dbus_debug_log_error("main connection handler", message);
    } else {
        g_debug("got message type %s\n", 
                dbus_message_type_to_string(type));    
    }
        
    return result;
}

HippoSystemDBus*
hippo_system_dbus_open(GError     **error)
{
    HippoSystemDBus *dbus;
    DBusGConnection *gconnection;
    DBusConnection *connection;
    DBusError derror;

    g_debug("attempting connect to system dbus");
    
    /* dbus_bus_get is a little hosed in old versions since you can't
     * unref unless you know it's disconnected. I guess it turns out
     * we more or less want to do that anyway.
     */
    
    gconnection = dbus_g_bus_get(DBUS_BUS_SYSTEM, error);
    if (gconnection == NULL)
        return NULL;
    
    connection = dbus_g_connection_get_connection(gconnection);
    
    /* the purpose of this check is to be sure we will get a "Disconnected"
     * message in the future
     */
    if (!dbus_connection_get_is_connected(connection)) {
        dbus_connection_unref(connection);
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED, 
            _("No active connection to the system's message bus"));
        return NULL;
    }

    dbus_error_init(&derror);
    
    /* Add NetworkManager signal match */
    dbus_bus_add_match(connection,
                       "type='signal',sender='"
                       "org.freedesktop.NetworkManager"
                       "',interface='"
                       "org.freedesktop.NetworkManager"
                       "',member='"
                       "StateChange"
                       "'",
                       &derror);

    if (dbus_error_is_set(&derror)) {
        propagate_dbus_error(error, &derror);
        /* FIXME leak bus connection since unref isn't allowed */
        return NULL;
    }
    
    /* the connection is already set up with the main loop. 
     * We just need to create our object, filters, etc. 
     */
    g_debug("D-BUS system bus connection established");

    dbus = g_object_new(HIPPO_TYPE_SYSTEM_DBUS, NULL);
    dbus->connection = connection;
    
    if (!dbus_connection_add_filter(connection, handle_message,
                                    dbus, NULL))
        g_error("no memory adding system dbus connection filter");
    
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
