/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"
#include "ddm-data-model-backend.h"

#include <dbus/dbus.h>

static void
ddm_dbus_add_model    (DDMDataModel *model,
                       void         *backend_data)
{


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
