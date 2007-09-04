/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_MODEL_H__
#define __DDM_DATA_MODEL_H__

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */


#include <glib-object.h>

G_BEGIN_DECLS

typedef struct _DDMDataModel      DDMDataModel;
typedef struct _DDMDataModelClass DDMDataModelClass;
typedef struct _DDMDataQuery      DDMDataQuery;

#define DDM_TYPE_DATA_MODEL              (ddm_data_model_get_type ())
#define DDM_DATA_MODEL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), DDM_TYPE_DATA_MODEL, DDMDataModel))
#define DDM_DATA_MODEL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), DDM_TYPE_DATA_MODEL, DDMDataModelClass))
#define DDM_IS_DATA_MODEL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), DDM_TYPE_DATA_MODEL))
#define DDM_IS_DATA_MODEL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), DDM_TYPE_DATA_MODEL))
#define DDM_DATA_MODEL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), DDM_TYPE_DATA_MODEL, DDMDataModelClass))

GType            ddm_data_model_get_type               (void) G_GNUC_CONST;

DDMDataQuery *ddm_data_model_query          (DDMDataModel *model,
                                             const char     *method,
                                             const char     *fetch,
                                             ...) G_GNUC_NULL_TERMINATED;
DDMDataQuery *ddm_data_model_query_params   (DDMDataModel *model,
                                             const char     *method,
                                             const char     *fetch,
                                             GHashTable     *params);
DDMDataQuery *ddm_data_model_query_resource (DDMDataModel *model,
                                             const char     *resource_id,
                                             const char     *fetch);
DDMDataQuery *ddm_data_model_update         (DDMDataModel *model,
                                             const char     *method,
                                             ...) G_GNUC_NULL_TERMINATED;
DDMDataQuery *ddm_data_model_update_params  (DDMDataModel *model,
                                             const char     *method,
                                             GHashTable     *params);

G_END_DECLS

#endif /* __DDM_DATA_MODEL_H__ */
