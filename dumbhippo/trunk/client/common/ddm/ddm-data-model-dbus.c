/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"
#include "ddm-data-model-backend.h"
#include "hippo-dbus-helper.h"

#include <dbus/dbus.h>

typedef struct {
    char           *path;
    DDMDataModel   *ddm_model;
    DBusConnection *connection;

} DBusModel;


static DBusModel*
get_dbus_model(DDMDataModel *ddm_model)
{
    DBusModel *dbus_model = g_object_get_data(G_OBJECT(ddm_model), "dbus-data-model");

    return dbus_model;
}

static DBusMessage*
handle_notify (void            *object,
               DBusMessage     *message,
               DBusError       *error)
{
    

    /* with no error set, returns a plain ack */
    return NULL;
}

static const HippoDBusMember model_client_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "Notify", "a(ssba(ssyyyv))", "", handle_notify },

    { 0, NULL }
};

static const HippoDBusProperty model_client_properties[] = {
    { NULL }
};

static void
handle_engine_available(DBusConnection *connection,
                        const char     *well_known_name,
                        const char     *unique_name,
                        void           *data)
{
    DBusModel *dbus_model = data;

    
    
    /* FIXME get initial connected state */
}

static void
handle_engine_unavailable(DBusConnection *connection,
                          const char     *well_known_name,
                          const char     *unique_name,
                          void           *data)
{
    DBusModel *dbus_model = data;

    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);
}

static HippoDBusServiceTracker engine_tracker = {
    handle_engine_available,
    handle_engine_unavailable
};

static void
handle_connected_changed(DBusConnection *connection,
                         DBusMessage    *message,
                         void           *data)
{
    DBusModel *dbus_model = data;
    dbus_bool_t is_connected;
    const char *self_id;

    is_connected = FALSE;
    self_id = NULL;

    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_BOOLEAN, &is_connected,
                               DBUS_TYPE_STRING, &self_id,
                               DBUS_TYPE_INVALID)) {
        g_warning("bad args to ConnectedChanged signal");
        return;
    }

    ddm_data_model_set_connected(dbus_model->ddm_model, is_connected);
}

static HippoDBusSignalTracker engine_signal_handlers[] = {
    { "org.freedesktop.od.Model", "ConnectedChanged",
      handle_connected_changed },
    { NULL, NULL, NULL }
};

static char*
generate_path(void)
{
    static int counter = 0;

    counter += 1;

    return g_strdup_printf("/org/freedesktop/od/ddm/client/%d", counter);
}

static void
handle_session_bus_connected(DBusConnection *connection,
                             void           *data)
{
    DBusModel *dbus_model = data;

    g_assert(dbus_model->connection == NULL);
    
    dbus_model->connection = connection;
    dbus_connection_ref(dbus_model->connection);
    
    /* dbus helper ignores multiple interface registrations */
    hippo_dbus_helper_register_interface(connection, "org.freedesktop.od.ModelClient",
                                         model_client_members, model_client_properties);

    dbus_model->path = generate_path();
    
    hippo_dbus_helper_register_object(connection, dbus_model->path,
                                      dbus_model, "org.freedesktop.od.ModelClient",
                                      NULL);
    
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.od.Engine",
                                               &engine_tracker,
                                               engine_signal_handlers,
                                               dbus_model);
}

static void
handle_session_bus_disconnected(DBusConnection *connection,
                                void           *data)
{
    DBusModel *dbus_model = data;

    g_assert(dbus_model->connection != NULL);
    g_assert(dbus_model->connection == connection);

    hippo_dbus_helper_unregister_service_tracker(connection,
                                                 "org.freedesktop.od.Engine",
                                                 &engine_tracker,
                                                 dbus_model);

    hippo_dbus_helper_unregister_object(connection, dbus_model->path);
    
    /* there's no interface unregistration right now */
    
    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);

    dbus_connection_unref(dbus_model->connection);
    dbus_model->connection = NULL;
}

static HippoDBusConnectionTracker connection_tracker = {
    handle_session_bus_connected,
    handle_session_bus_disconnected
};

static void
ddm_dbus_add_model (DDMDataModel *ddm_model,
                    void         *backend_data)
{
    DBusModel *dbus_model;
    
    dbus_model = g_new0(DBusModel, 1);
    dbus_model->ddm_model = ddm_model;
    
    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", dbus_model);

    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);

    hippo_dbus_helper_register_connection_tracker(DBUS_BUS_SESSION,
                                                  &connection_tracker,
                                                  dbus_model);
}

static void
ddm_dbus_remove_model (DDMDataModel *ddm_model,
                       void         *backend_data)
{
    DBusModel *dbus_model;

    dbus_model = get_dbus_model(ddm_model);
    
    hippo_dbus_helper_unregister_connection_tracker(DBUS_BUS_SESSION,
                                                    &connection_tracker,
                                                    dbus_model);

    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", NULL);
    
    g_free(dbus_model);
}

static void
ddm_dbus_send_query   (DDMDataModel *model,
                       DDMDataQuery *query,
                       void         *backend_data)
{
    


}

static void
ddm_dbus_send_update (DDMDataModel *model,
                      DDMDataQuery *query,
                      const char   *method,
                      GHashTable   *params,
                      void         *backend_data)
{



}

static const DDMDataModelBackend dbus_backend = {
    ddm_dbus_add_model,
    ddm_dbus_remove_model,
    ddm_dbus_send_query,
    ddm_dbus_send_update,
    NULL,
};

const DDMDataModelBackend*
ddm_data_model_get_dbus_backend(void)
{
    return &dbus_backend;
}

/* This is the default model for platforms that have dbus only.
 * Other platforms will have to do something else.
 */

static DDMDataModel *default_model = NULL;

DDMDataModel*
ddm_data_model_get_default (void)
{
    if (default_model == NULL) {
        default_model = ddm_data_model_new_with_backend(ddm_data_model_get_dbus_backend(),
                                                        NULL, NULL);
    }

    g_object_ref(default_model);
    return default_model;
}
