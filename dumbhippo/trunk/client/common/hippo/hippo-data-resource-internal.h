/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_RESOURCE_INTERNAL_H__
#define __HIPPO_DATA_RESOURCE_INTERNAL_H__

#include <hippo/hippo-data-resource.h>
#include <hippo/hippo-notification-set.h>

G_BEGIN_DECLS

typedef struct _HippoDataValue HippoDataValue;

struct _HippoDataValue {
    HippoDataType type;
    
    union {
        char *string;
        HippoDataResource *resource;
        GSList *list;
    } u;
};

typedef enum {
    HIPPO_DATA_UPDATE_ADD,
    HIPPO_DATA_UPDATE_REPLACE,
    HIPPO_DATA_UPDATE_REMOVE,
    HIPPO_DATA_UPDATE_CLEAR
} HippoDataUpdate;

typedef enum {
    HIPPO_DATA_CARDINALITY_01,
    HIPPO_DATA_CARDINALITY_1,
    HIPPO_DATA_CARDINALITY_N
} HippoDataCardinality;
    
HippoDataResource *_hippo_data_resource_new (const char *resource_id,
                                             const char *class_id);

void _hippo_data_resource_update_property (HippoDataResource    *resource,
                                           HippoQName           *property_id,
                                           HippoDataUpdate       update,
                                           HippoDataCardinality  cardinality,
                                           HippoDataValue       *value,
                                           HippoNotificationSet *notifications);

void _hippo_data_resource_on_resource_change (HippoDataResource *resource,
                                              GSList            *changed_properties);

void _hippo_data_resource_dump(HippoDataResource *resource);

G_END_DECLS

#endif /* __HIPPO_DATA_RESOURCE_INTERNAL_H__ */
