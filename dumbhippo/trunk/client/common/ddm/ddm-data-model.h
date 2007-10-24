/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_DATA_MODEL_H__
#define __DDM_DATA_MODEL_H__

#include <glib-object.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

#define DDM_GLOBAL_RESOURCE       "online-desktop:/o/global"
#define DDM_GLOBAL_RESOURCE_CLASS "online-desktop:/p/o/global"

typedef struct _DDMDataModel      DDMDataModel;
typedef struct _DDMDataModelClass DDMDataModelClass;

typedef struct _DDMDataModelBackend  DDMDataModelBackend;
typedef struct _DDMDataQuery         DDMDataQuery;

#define DDM_TYPE_DATA_MODEL              (ddm_data_model_get_type ())
#define DDM_DATA_MODEL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), DDM_TYPE_DATA_MODEL, DDMDataModel))
#define DDM_DATA_MODEL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), DDM_TYPE_DATA_MODEL, DDMDataModelClass))
#define DDM_IS_DATA_MODEL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), DDM_TYPE_DATA_MODEL))
#define DDM_IS_DATA_MODEL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), DDM_TYPE_DATA_MODEL))
#define DDM_DATA_MODEL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), DDM_TYPE_DATA_MODEL, DDMDataModelClass))

GType            ddm_data_model_get_type               (void) G_GNUC_CONST;

DDMDataModel *ddm_data_model_get_default        (void);
DDMDataModel *ddm_data_model_new_with_backend   (const DDMDataModelBackend *backend,
                                                 void                      *backend_data,
                                                 GFreeFunc                  free_backend_data_func);
/* Used testing purposes; you can't call query or update on such a backend */
DDMDataModel *ddm_data_model_new_no_backend     (void);
gboolean      ddm_data_model_get_connected      (DDMDataModel   *model);
DDMDataQuery *ddm_data_model_query              (DDMDataModel   *model,
                                                 const char     *method,
                                                 const char     *fetch,
                                                 ...) G_GNUC_NULL_TERMINATED;
DDMDataQuery *ddm_data_model_query_params       (DDMDataModel   *model,
                                                 const char     *method,
                                                 const char     *fetch,
                                                 GHashTable     *params);
DDMDataQuery *ddm_data_model_query_resource     (DDMDataModel   *model,
                                                 const char     *resource_id,
                                                 const char     *fetch);
DDMDataQuery *ddm_data_model_update             (DDMDataModel   *model,
                                                 const char     *method,
                                                 ...) G_GNUC_NULL_TERMINATED;
DDMDataQuery *ddm_data_model_update_params      (DDMDataModel   *model,
                                                 const char     *method,
                                                 GHashTable     *params);
DDMDataResource *ddm_data_model_lookup_resource (DDMDataModel   *model,
                                                 const char     *resource_id);
DDMDataResource *ddm_data_model_ensure_resource (DDMDataModel   *model,
                                                 const char     *resource_id,
                                                 const char     *class_id);

/* Like ddm_data_model_ensure_resource, but the resource is flagged as local. This means
 *
 * - Don't query remotely for properties that aren't there
 * - Don't expunge on reconnect
 *
 */
DDMDataResource *ddm_data_model_ensure_local_resource (DDMDataModel   *model,
                                                       const char     *resource_id,
                                                       const char     *class_id);

/* should only be called by backends */
void          ddm_data_model_set_connected      (DDMDataModel   *model,
                                                 gboolean        connected);


G_END_DECLS

#endif /* __DDM_DATA_MODEL_H__ */
