/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_DATA_MODEL_DBUS_H__
#define __DDM_DATA_MODEL_DBUS_H__


#include <ddm/ddm-data-model.h>

G_BEGIN_DECLS

const DDMDataModelBackend* ddm_data_model_get_dbus_backend(void);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_DBUS_H__ */
