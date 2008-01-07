/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */

#ifndef __DDM_DATA_RESOURCE_H__
#define __DDM_DATA_RESOURCE_H__

#include <ddm/ddm-qname.h>
#include <glib.h>

G_BEGIN_DECLS

GQuark ddm_data_error_quark(void);

#define DDM_DATA_ERROR (ddm_data_error_quark())

/* We use this enumeration both as a raw error code in contexts where
 * we don't have more text, and as a set of error codes for the
 * DDM_DATA_ERROR GError domain.
 */
typedef enum {
    DDM_DATA_ERROR_NO_CONNECTION = -1,
    DDM_DATA_ERROR_BAD_REPLY = -2,
    DDM_DATA_ERROR_INTERNAL = -3,
    DDM_DATA_ERROR_BAD_REQUEST = 400,
    DDM_DATA_ERROR_FORBIDDEN = 403,
    DDM_DATA_ERROR_ITEM_NOT_FOUND = 404,
    DDM_DATA_ERROR_INTERNAL_SERVER_ERROR = 500
} DDMDataError;

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
    DDM_DATA_FEED       = 8,
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
typedef struct _DDMDataFetch         DDMDataFetch; /* Avoid circular include */
typedef struct _DDMDataResource      DDMDataResource;

typedef struct _DDMClient            DDMClient;    /* Avoid circular include */
typedef struct _DDMFeed              DDMFeed;


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
        DDMFeed *feed;
        GSList *list;
    } u;
};

DDMDataResource *ddm_data_resource_ref   (DDMDataResource *resource);
void             ddm_data_resource_unref (DDMDataResource *resource);

void     ddm_data_resource_set_data (DDMDataResource *resource,
                                     const char      *key,
                                     gpointer         data,
                                     GDestroyNotify   notify);
gpointer ddm_data_resource_get_data (DDMDataResource *resource,
                                     const char      *key);

void ddm_data_value_get_element(DDMDataValue *value,
                                GSList         *element_node,
                                DDMDataValue *element);

const char *ddm_data_resource_get_resource_id (DDMDataResource *resource);
const char *ddm_data_resource_get_class_id    (DDMDataResource *resource);
gboolean    ddm_data_resource_is_local        (DDMDataResource *resource);

void ddm_data_resource_get               (DDMDataResource *resource,
                                          ...) G_GNUC_NULL_TERMINATED;
void ddm_data_resource_get_by_qname      (DDMDataResource *resource,
                                          ...) G_GNUC_NULL_TERMINATED;

void ddm_data_resource_connect          (DDMDataResource *resource,
                                         const char      *property,
                                         DDMDataFunction  function,
                                         gpointer         user_data);
void ddm_data_resource_connect_by_qname (DDMDataResource *resource,
                                         DDMQName        *property,
                                         DDMDataFunction  function,
                                         gpointer         user_data);

void          ddm_data_resource_set_client_fetch (DDMDataResource *resource,
                                                  DDMClient       *client,
                                                  DDMDataFetch    *fetch);
DDMDataFetch *ddm_data_resource_get_client_fetch (DDMDataResource *resource,
                                                  DDMClient       *client);

void ddm_data_resource_disconnect       (DDMDataResource *resource,
                                         DDMDataFunction  function,
                                         gpointer         user_data);

DDMQName *         ddm_data_property_get_qname             (DDMDataProperty    *property);
void               ddm_data_property_get_value             (DDMDataProperty    *property,
                                                            DDMDataValue       *value);
DDMDataType        ddm_data_property_get_type              (DDMDataProperty    *property);
DDMDataCardinality ddm_data_property_get_cardinality       (DDMDataProperty    *property);
gboolean           ddm_data_property_get_default_include   (DDMDataProperty    *property);
DDMDataFetch      *ddm_data_property_get_default_children  (DDMDataProperty    *property);
gboolean           ddm_data_resource_update_property       (DDMDataResource    *resource,
                                                            DDMQName           *property_id,
                                                            DDMDataUpdate       update,
                                                            DDMDataCardinality  cardinality,
                                                            gboolean            default_include,
                                                            const char         *default_children,
                                                            DDMDataValue       *value);
gboolean           ddm_data_resource_update_feed_property  (DDMDataResource    *resource,
                                                            DDMQName           *property_id,
                                                            DDMDataUpdate       update,
                                                            gboolean            default_include,
                                                            const char         *default_children,
                                                            DDMDataResource    *item_resource,
                                                            gint64              item_timestamp);

DDMDataProperty *  ddm_data_resource_get_property          (DDMDataResource    *resource,
                                                            const char         *name);
DDMDataProperty *  ddm_data_resource_get_property_by_qname (DDMDataResource    *resource,
                                                            DDMQName           *qname);
GSList          *  ddm_data_resource_get_properties        (DDMDataResource    *resource);

gboolean ddm_data_value_from_string (DDMDataValue *value,
                                     DDMDataType   type,
                                     const char   *str,
                                     const char   *base_url,
                                     GError      **error);

void ddm_data_value_clear(DDMDataValue *value);

/* for debugging */
char* ddm_data_value_to_string (DDMDataValue *value);


/* Sometimes it's useful to create the resource object first, and set the class_id later */
void     ddm_data_resource_set_class_id    (DDMDataResource    *resource,
                                            const char         *class_id);

/* utility function useful to some backends */
gboolean      ddm_data_parse_type             (const char         *type_string,
                                               DDMDataType        *type,
                                               DDMDataCardinality *cardinality,
                                               gboolean           *default_include);

G_END_DECLS

#endif /* __DDM_DATA_RESOURCE_H__ */
