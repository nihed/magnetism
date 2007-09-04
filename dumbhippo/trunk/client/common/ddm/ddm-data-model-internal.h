/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_MODEL_INTERNAL_H__
#define __DDM_DATA_MODEL_INTERNAL_H__

#include "ddm-data-cache.h"
#include "ddm-disk-cache.h"
#include "ddm-data-model.h"
#include "ddm-notification-set.h"

G_BEGIN_DECLS

DDMDataModel *_ddm_data_model_new(DDMDataCache *cache);

DDMDiskCache *_ddm_data_model_get_disk_cache     (DDMDataModel *model);

DDMDataResource *_ddm_data_model_get_resource    (DDMDataModel *model,
                                                  const char     *resource_id);
DDMDataResource *_ddm_data_model_ensure_resource (DDMDataModel *model,
                                                  const char     *resource_id,
                                                  const char     *class_id);
gboolean _ddm_data_parse_type (const char           *type_string,
                               DDMDataType        *type,
                               DDMDataCardinality *cardinality,
                               gboolean             *default_include);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_INTERNAL__H__ */
