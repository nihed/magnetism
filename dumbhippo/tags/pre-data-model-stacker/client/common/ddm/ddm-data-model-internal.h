/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_MODEL_INTERNAL_H__
#define __DDM_DATA_MODEL_INTERNAL_H__

#include "ddm-data-model.h"
#include "ddm-data-resource.h"
#include "ddm-rule.h"
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

/* Result should be freed by caller */
GSList *_ddm_data_model_find_sources (DDMDataModel *model,
                                      const char   *source_class_id,
                                      DDMCondition *condition);
GSList *_ddm_data_model_find_targets (DDMDataModel *model,
                                      const char   *target_class_id,
                                      DDMCondition *condition);

/* Result owned by model */
GSList *_ddm_data_model_get_target_rules(DDMDataModel *model,
                                         const char   *class_id);
GSList *_ddm_data_model_get_source_rules(DDMDataModel *model,
                                         const char   *class_id);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_INTERNAL__H__ */
