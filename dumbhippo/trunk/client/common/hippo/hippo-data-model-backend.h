/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_MODEL_BACKEND_H__
#define __HIPPO_DATA_MODEL_BACKEND_H__

#include <ddm/ddm.h>
#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS

const DDMDataModelBackend* hippo_data_model_get_backend(void);

G_END_DECLS

#endif /* __HIPPO_DATA_MODEL_BACKEND_H__ */
