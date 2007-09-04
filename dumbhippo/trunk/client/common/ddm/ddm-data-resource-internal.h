/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_RESOURCE_INTERNAL_H__
#define __DDM_DATA_RESOURCE_INTERNAL_H__

#include <ddm/ddm-data-fetch.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

DDMDataResource *_ddm_data_resource_new (const char *resource_id,
                                         const char *class_id);

GSList *_ddm_data_resource_get_default_properties (DDMDataResource *resource);

void _ddm_data_resource_dump(DDMDataResource *resource);

G_END_DECLS

#endif /* __DDM_DATA_RESOURCE_INTERNAL_H__ */
