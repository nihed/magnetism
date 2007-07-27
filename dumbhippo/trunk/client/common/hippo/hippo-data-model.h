/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_MODEL_H__
#define __HIPPO_DATA_MODEL_H__

#include <glib-object.h>

G_BEGIN_DECLS

typedef struct _HippoDataModel      HippoDataModel;
typedef struct _HippoDataModelClass HippoDataModelClass;
typedef struct _HippoDataQuery      HippoDataQuery;

#define HIPPO_TYPE_DATA_MODEL              (hippo_data_model_get_type ())
#define HIPPO_DATA_MODEL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DATA_MODEL, HippoDataModel))
#define HIPPO_DATA_MODEL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DATA_MODEL, HippoDataModelClass))
#define HIPPO_IS_DATA_MODEL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DATA_MODEL))
#define HIPPO_IS_DATA_MODEL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DATA_MODEL))
#define HIPPO_DATA_MODEL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DATA_MODEL, HippoDataModelClass))

GType            hippo_data_model_get_type               (void) G_GNUC_CONST;

HippoDataQuery *hippo_data_model_query          (HippoDataModel *model,
                                                 const char     *method,
                                                 const char     *fetch,
                                                 ...) G_GNUC_NULL_TERMINATED;
HippoDataQuery *hippo_data_model_query_params   (HippoDataModel *model,
                                                 const char     *method,
                                                 const char     *fetch,
                                                 GHashTable     *params);
HippoDataQuery *hippo_data_model_query_resource (HippoDataModel *model,
                                                 const char     *resource_id,
                                                 const char     *fetch);
HippoDataQuery *hippo_data_model_update         (HippoDataModel *model,
                                                 const char     *method,
                                                 ...) G_GNUC_NULL_TERMINATED;
HippoDataQuery *hippo_data_model_update_params  (HippoDataModel *model,
                                                 const char     *method,
                                                 GHashTable     *params);

G_END_DECLS

#endif /* __HIPPO_DATA_MODEL_H__ */
