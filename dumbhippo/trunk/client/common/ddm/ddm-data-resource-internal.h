/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_RESOURCE_INTERNAL_H__
#define __DDM_DATA_RESOURCE_INTERNAL_H__

#include <ddm/ddm-data-fetch.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

DDMDataResource *_ddm_data_resource_new (const char *resource_id,
                                         const char *class_id);

/* Sometimes it's useful to create the resource object first, and set the class_id later */
void     _ddm_data_resource_set_class_id    (DDMDataResource    *resource,
                                             const char           *class_id);

gboolean _ddm_data_resource_update_property (DDMDataResource    *resource,
                                             DDMQName           *property_id,
                                             DDMDataUpdate       update,
                                             DDMDataCardinality  cardinality,
                                             gboolean              default_include,
                                             const char           *default_children,
                                             DDMDataValue       *value);

void _ddm_data_resource_on_resource_change (DDMDataResource *resource,
                                            GSList            *changed_properties);

DDMDataProperty *_ddm_data_resource_get_property          (DDMDataResource *resource,
                                                           const char        *name);
DDMDataProperty *_ddm_data_resource_get_property_by_qname (DDMDataResource *resource,
                                                           DDMQName        *qname);

GSList *_ddm_data_resource_get_default_properties (DDMDataResource *resource);

void _ddm_data_resource_dump(DDMDataResource *resource);

G_END_DECLS

#endif /* __DDM_DATA_RESOURCE_INTERNAL_H__ */
