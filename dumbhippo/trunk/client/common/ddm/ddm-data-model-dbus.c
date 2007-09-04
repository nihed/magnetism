/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"
#include "ddm-data-model-backend.h"

#include <dbus/dbus.h>

typedef struct {
    DDMDataModel   *ddm_model;
    DBusConnection *connection;

} DBusModel;


static DBusModel*
get_dbus_model(DDMDataModel *ddm_model)
{
    DBusModel *dbus_model = g_object_get_data(G_OBJECT(ddm_model), "dbus-data-model");

    return dbus_model;
}

static void
ddm_dbus_add_model (DDMDataModel *model,
                    void         *backend_data)
{
#if 0
    dbus_model = g_new0(DbusModel, 1);
    dbus_model->ddm_model = ddm_model;
    dbus_model->data_cache = cache;
    
    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", dbus_model);

    connection = dbus_data_cache_get_connection(cache);

    ddm_data_model_set_connected(dbus_model->ddm_model,
                                 dbus_connection_get_connected(connection));
#endif

}

static void
ddm_dbus_remove_model (DDMDataModel *model,
                       void         *backend_data)
{


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
