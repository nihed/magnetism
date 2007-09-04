/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_RESOURCE_H__
#define __DDM_DATA_RESOURCE_H__

#include <ddm/ddm-qname.h>
#include <glib.h>

G_BEGIN_DECLS

typedef enum {
    DDM_DATA_UPDATE_ADD,
    DDM_DATA_UPDATE_REPLACE,
    DDM_DATA_UPDATE_DELETE,
    DDM_DATA_UPDATE_CLEAR
} DDMDataUpdate;

typedef enum {
    DDM_DATA_NONE       = 0,
    DDM_DATA_BOOLEAN    = 1,
    DDM_DATA_INTEGER    = 2,
    DDM_DATA_LONG       = 3,
    DDM_DATA_FLOAT      = 4,
    DDM_DATA_STRING     = 5,
    DDM_DATA_RESOURCE   = 6,
    DDM_DATA_URL        = 7,
    DDM_DATA_LIST       = 0x10
} DDMDataType;

#define DDM_DATA_BASE(type) (type & 0xf)
#define DDM_DATA_IS_LIST(type) ((type & DDM_DATA_LIST) != 0)

typedef enum {
    DDM_DATA_CARDINALITY_01,
    DDM_DATA_CARDINALITY_1,
    DDM_DATA_CARDINALITY_N
} DDMDataCardinality;

typedef struct _DDMDataValue         DDMDataValue;
typedef struct _DDMDataProperty      DDMDataProperty;
typedef struct _DDMDataResource      DDMDataResource;
typedef struct _DDMDataFetch         DDMDataFetch; /* Avoid circular include */

typedef void (*DDMDataFunction) (DDMDataResource *resource,
                                 GSList            *changed_properties,
                                 gpointer           user_data);

struct _DDMDataValue {
    DDMDataType type;

    union {
        gboolean boolean;
        int integer;
        gint64 long_;
        double float_;
        char *string;
        DDMDataResource *resource;
        GSList *list;
    } u;
};

void ddm_data_value_get_element(DDMDataValue *value,
                                GSList         *element_node,
                                DDMDataValue *element);

const char *ddm_data_resource_get_resource_id (DDMDataResource *resource);
const char *ddm_data_resource_get_class_id    (DDMDataResource *resource);

void ddm_data_resource_get          (DDMDataResource *resource,
                                     ...) G_GNUC_NULL_TERMINATED;
void ddm_data_resource_get_by_qname (DDMDataResource *resource,
                                     ...) G_GNUC_NULL_TERMINATED;

void ddm_data_resource_connect          (DDMDataResource *resource,
                                         const char        *property,
                                         DDMDataFunction  function,
                                         gpointer           user_data);
void ddm_data_resource_connect_by_qname (DDMDataResource *resource,
                                         DDMQName        *property,
                                         DDMDataFunction  function,
                                         gpointer           user_data);
void ddm_data_resource_disconnect       (DDMDataResource *resource,
                                         DDMDataFunction  function,
                                         gpointer           user_data);

DDMQName *         ddm_data_property_get_qname            (DDMDataProperty *property);
void                 ddm_data_property_get_value            (DDMDataProperty *property,
                                                             DDMDataValue    *value);
DDMDataType        ddm_data_property_get_type             (DDMDataProperty *property);
DDMDataCardinality ddm_data_property_get_cardinality      (DDMDataProperty *property);
gboolean             ddm_data_property_get_default_include  (DDMDataProperty *property);
DDMDataFetch      *ddm_data_property_get_default_children (DDMDataProperty *property);

G_END_DECLS

#endif /* __DDM_DATA_RESOURCE_H__ */
