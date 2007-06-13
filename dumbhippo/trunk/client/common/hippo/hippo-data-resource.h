/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_RESOURCE_H__
#define __HIPPO_DATA_RESOURCE_H__

#include <hippo/hippo-qname.h>
#include <glib.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_DATA_UPDATE_ADD,
    HIPPO_DATA_UPDATE_REPLACE,
    HIPPO_DATA_UPDATE_DELETE,
    HIPPO_DATA_UPDATE_CLEAR
} HippoDataUpdate;

typedef enum {
    HIPPO_DATA_NONE       = 0,
    HIPPO_DATA_BOOLEAN    = 1,
    HIPPO_DATA_INTEGER    = 2,
    HIPPO_DATA_LONG       = 3,
    HIPPO_DATA_FLOAT      = 4,
    HIPPO_DATA_STRING     = 5,
    HIPPO_DATA_RESOURCE   = 6,
    HIPPO_DATA_LIST       = 0x10
} HippoDataType;

#define HIPPO_DATA_BASE(type) (type & 0xf)
#define HIPPO_DATA_IS_LIST(type) ((type & HIPPO_DATA_LIST) != 0)

typedef enum {
    HIPPO_DATA_CARDINALITY_01,
    HIPPO_DATA_CARDINALITY_1,
    HIPPO_DATA_CARDINALITY_N
} HippoDataCardinality;

typedef struct _HippoDataValue         HippoDataValue;
typedef struct _HippoDataProperty      HippoDataProperty;
typedef struct _HippoDataResource      HippoDataResource;
typedef struct _HippoDataFetch         HippoDataFetch; /* Avoid circular include */

typedef void (*HippoDataFunction) (HippoDataResource *resource,
                                   GSList            *changed_properties,
                                   gpointer           user_data);

struct _HippoDataValue {
    HippoDataType type;
    
    union {
        gboolean boolean;
        int integer;
        gint64 long_;
        double float_;
        char *string;
        HippoDataResource *resource;
        GSList *list;
    } u;
};

void hippo_data_value_get_element(HippoDataValue *value,
                                  GSList         *element_node,
                                  HippoDataValue *element);

const char *hippo_data_resource_get_resource_id (HippoDataResource *resource);
const char *hippo_data_resource_get_class_id    (HippoDataResource *resource);

void hippo_data_resource_get          (HippoDataResource *resource,
                                       ...) G_GNUC_NULL_TERMINATED;
void hippo_data_resource_get_by_qname (HippoDataResource *resource,
                                       ...) G_GNUC_NULL_TERMINATED;

void hippo_data_resource_connect          (HippoDataResource *resource,
                                           const char        *property,
                                           HippoDataFunction  function,
                                           gpointer           user_data);
void hippo_data_resource_connect_by_qname (HippoDataResource *resource,
                                           HippoQName        *property,
                                           HippoDataFunction  function,
                                           gpointer           user_data);
void hippo_data_resource_disconnect       (HippoDataResource *resource,
                                           HippoDataFunction  function,
                                           gpointer           user_data);

HippoQName *         hippo_data_property_get_qname            (HippoDataProperty *property);
void                 hippo_data_property_get_value            (HippoDataProperty *property,
                                                               HippoDataValue    *value);
HippoDataType        hippo_data_property_get_type             (HippoDataProperty *property);
HippoDataCardinality hippo_data_property_get_cardinality      (HippoDataProperty *property);
gboolean             hippo_data_property_get_default_include  (HippoDataProperty *property);
HippoDataFetch      *hippo_data_property_get_default_children (HippoDataProperty *property);

G_END_DECLS

#endif /* __HIPPO_DATA_RESOURCE_H__ */
