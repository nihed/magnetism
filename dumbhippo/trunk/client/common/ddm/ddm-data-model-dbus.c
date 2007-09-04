/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"

static void
ddm_dbus_add_model    (DDMDataModel *model)
{


}

static void
ddm_dbus_remove_model (DDMDataModel *model)
{


}

static void
ddm_dbus_send_query   (DDMDataModel *model,
                       DDMDataQuery *query)
{



}

static void
ddm_dbus_send_update (DDMDataModel *model,
                      DDMDataQuery *query,
                      const char   *method,
                      GHashTable   *params)
{



}

static const DDMDataModelBackend dbus_backend = {
    ddm_dbus_add_model,
    ddm_dbus_remove_model
    ddm_dbus_send_query,
    ddm_dbus_send_update,
    NULL,
};

const DDMDataModelBackend*
ddm_data_model_get_dbus_backend(void)
{
    return &dbus_backend;
}
