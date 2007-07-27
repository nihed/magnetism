/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_MODEL_INTERNAL_H__
#define __HIPPO_DATA_MODEL_INTERNAL_H__

#include "hippo-data-cache.h"
#include "hippo-disk-cache.h"
#include "hippo-data-model.h"
#include "hippo-notification-set.h"

G_BEGIN_DECLS

HippoDataModel *_hippo_data_model_new(HippoDataCache *cache);

HippoDiskCache *_hippo_data_model_get_disk_cache     (HippoDataModel *model);

HippoDataResource *_hippo_data_model_get_resource    (HippoDataModel *model,
                                                      const char     *resource_id);
HippoDataResource *_hippo_data_model_ensure_resource (HippoDataModel *model,
                                                      const char     *resource_id,
                                                      const char     *class_id);
gboolean _hippo_data_parse_type (const char           *type_string,
                                 HippoDataType        *type,
                                 HippoDataCardinality *cardinality,
                                 gboolean             *default_include);

G_END_DECLS

#endif /* __HIPPO_DATA_MODEL_INTERNAL__H__ */
