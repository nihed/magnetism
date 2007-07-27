/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_RESOURCE_INTERNAL_H__
#define __HIPPO_DATA_RESOURCE_INTERNAL_H__

#include <hippo/hippo-data-fetch.h>
#include <hippo/hippo-data-resource.h>
#include <hippo/hippo-notification-set.h>

G_BEGIN_DECLS

HippoDataResource *_hippo_data_resource_new (const char *resource_id,
                                             const char *class_id);

/* Sometimes it's useful to create the resource object first, and set the class_id later */
void     _hippo_data_resource_set_class_id    (HippoDataResource    *resource,
                                               const char           *class_id);

gboolean _hippo_data_resource_update_property (HippoDataResource    *resource,
                                               HippoQName           *property_id,
                                               HippoDataUpdate       update,
                                               HippoDataCardinality  cardinality,
                                               gboolean              default_include,
                                               const char           *default_children,
                                               HippoDataValue       *value);

void _hippo_data_resource_on_resource_change (HippoDataResource *resource,
                                              GSList            *changed_properties);

HippoDataProperty *_hippo_data_resource_get_property          (HippoDataResource *resource,
                                                               const char        *name);
HippoDataProperty *_hippo_data_resource_get_property_by_qname (HippoDataResource *resource,
                                                               HippoQName        *qname);

GSList *_hippo_data_resource_get_default_properties (HippoDataResource *resource);

void _hippo_data_resource_dump(HippoDataResource *resource);

G_END_DECLS

#endif /* __HIPPO_DATA_RESOURCE_INTERNAL_H__ */
