/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_MODEL_INTERNAL_H__
#define __DDM_DATA_MODEL_INTERNAL_H__

#include "ddm-data-model.h"
#include "ddm-data-resource.h"
#include "ddm-work-item.h"

G_BEGIN_DECLS

void _ddm_data_model_mark_changed   (DDMDataModel    *model,
                                     DDMDataResource *resource);
void _ddm_data_model_add_work_item  (DDMDataModel    *model,
                                     DDMWorkItem     *item);
void _ddm_data_model_query_answered (DDMDataModel    *model,
                                     DDMDataQuery    *query);

DDMDataQuery *_ddm_data_model_query_remote_resource (DDMDataModel *model,
                                                     const char     *resource_id,
                                                     const char     *fetch);

DDMClient *_ddm_data_model_get_local_client (DDMDataModel *model);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_INTERNAL__H__ */
