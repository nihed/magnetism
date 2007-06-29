/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_MODEL_INTERNAL_H__
#define __HIPPO_DATA_MODEL_INTERNAL_H__

#include "hippo-data-cache.h"
#include "hippo-data-model.h"

G_BEGIN_DECLS

HippoDataModel *_hippo_data_model_new(HippoDataCache *cache);

HippoDataResource *_hippo_data_model_get_resource    (HippoDataModel *model,
                                                      const char     *resource_id);
HippoDataResource *_hippo_data_model_ensure_resource (HippoDataModel *model,
                                                      const char     *resource_id,
                                                      const char     *class_id);

G_END_DECLS

#endif /* __HIPPO_DATA_MODEL_INTERNAL__H__ */
