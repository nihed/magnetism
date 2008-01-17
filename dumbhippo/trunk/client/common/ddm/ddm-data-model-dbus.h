/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_DATA_MODEL_DBUS_H__
#define __DDM_DATA_MODEL_DBUS_H__

#include <ddm/ddm.h>

G_BEGIN_DECLS

DDMDataModel* ddm_data_model_get_for_bus_name(const char *bus_name);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_DBUS_H__ */
