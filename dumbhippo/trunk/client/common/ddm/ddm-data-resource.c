/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

#include "ddm-data-fetch.h"
#include "ddm-data-model-internal.h"
#include "ddm-data-resource-internal.h"

typedef enum {
    CONNECTION_TYPE_ANY,
    CONNECTION_TYPE_QNAME,
    CONNECTION_TYPE_NAME
} DataConnectionType;

typedef struct _DataProperty DataProperty;
typedef struct _DataConnection DataConnection;
typedef struct _DataClient DataClient;

struct _DDMDataProperty {
    DDMQName *qname;
    DDMDataValue value;
    DDMDataFetch *default_children;
    guint cardinality : 4;
    guint default_include : 1;
};

struct _DataConnection {
    DataConnectionType type;
    union {
        DDMQName *qname;
        const char *name;
    } match;
    
    DDMDataFunction function;
    gpointer user_data;
};

struct _DataClient {
    DDMClient *client;
    DDMDataFetch *fetch;
};

struct _DDMDataResource
{
    DDMDataModel *model;
    char *resource_id;
    char *class_id;
    gboolean local;

    GSList *clients;
    GSList *connections; /* Local connections */
    GSList *properties;
    GSList *changed_properties;

    DDMDataFetch *received_fetch;
    DDMDataFetch *requested_fetch;
    gint64 requested_serial;

};

GQuark
ddm_data_error_quark (void)
{
  return g_quark_from_static_string ("ddm-data-error-quark");
}

void
ddm_data_value_get_element(DDMDataValue *value,
                           GSList       *node,
                           DDMDataValue *element)
{
    g_return_if_fail(DDM_DATA_IS_LIST(value->type));

    element->type = DDM_DATA_BASE(value->type);
    
    switch (element->type) {
    case DDM_DATA_BOOLEAN:
        element->u.boolean =  *(gboolean *)node->data;
        return;
    case DDM_DATA_INTEGER:
        element->u.integer =  *(int *)node->data;
        return;
    case DDM_DATA_LONG:
        element->u.long_ =  *(gint64 *)node->data;
        return;
    case DDM_DATA_FLOAT:
        element->u.float_ =  *(double *)node->data;
        return;
    case DDM_DATA_STRING:
        element->u.string  = (char *)node->data;
        return;
    case DDM_DATA_RESOURCE:
        element->u.resource = (DDMDataResource *)node->data;
        return;
    case DDM_DATA_URL:
        element->u.string  = (char *)node->data;
        return;
    case DDM_DATA_NONE:
    case DDM_DATA_LIST:
        break;
    }

    g_warning("Type value '%d' not valid", element->type);
}

static void
ddm_data_value_free_element(DDMDataValue *value,
                            GSList         *node)
{
    g_return_if_fail(DDM_DATA_IS_LIST(value->type));

    switch (DDM_DATA_BASE(value->type)) {
    case DDM_DATA_BOOLEAN:
    case DDM_DATA_INTEGER:
    case DDM_DATA_LONG:
    case DDM_DATA_FLOAT:
    case DDM_DATA_STRING:
    case DDM_DATA_URL:
        g_free(node->data);
        return;
    case DDM_DATA_RESOURCE:
    case DDM_DATA_NONE:
    case DDM_DATA_LIST:
        return;
    }

    g_warning("Type value '%d' not valid", DDM_DATA_BASE(value->type));
}

DDMQName *
ddm_data_property_get_qname(DDMDataProperty *property)
{
    g_return_val_if_fail(property != NULL, NULL);

    return property->qname;
}

void
ddm_data_property_get_value(DDMDataProperty *property,
                            DDMDataValue    *value)
{
    g_return_if_fail(property != NULL);

    *value = property->value;
}

DDMDataType
ddm_data_property_get_type(DDMDataProperty *property)
{
    g_return_val_if_fail(property != NULL, DDM_DATA_NONE);
    
    return DDM_DATA_BASE(property->value.type);
}

DDMDataCardinality
ddm_data_property_get_cardinality(DDMDataProperty *property)
{
    g_return_val_if_fail(property != NULL, DDM_DATA_CARDINALITY_01);

    return property->cardinality;
}

gboolean
ddm_data_property_get_default_include(DDMDataProperty *property)
{
    g_return_val_if_fail(property != NULL, FALSE);
    
    return property->default_include;
}

DDMDataFetch *
ddm_data_property_get_default_children(DDMDataProperty *property)
{
    g_return_val_if_fail(property != NULL, NULL);
    
    return property->default_children;
}

DDMDataResource *
_ddm_data_resource_new(DDMDataModel *model,
                       const char   *resource_id,
                       const char   *class_id,
                       gboolean      local)
{
    DDMDataResource *resource = g_new0(DDMDataResource, 1);

    resource->model = model;
    resource->resource_id = g_strdup(resource_id);
    ddm_data_resource_set_class_id(resource, class_id);
    resource->local = local != FALSE;
    resource->requested_fetch = NULL;
    resource->requested_serial = -1;
    resource->received_fetch = NULL;
    resource->local = local;

    return resource;
}

void
ddm_data_resource_set_class_id(DDMDataResource    *resource,
                               const char         *class_id)
{
    if (resource->class_id != NULL)
        g_free(resource->class_id);
    
    // TODO: strdup() isn't really necessary, since class_id is currently g_intern_string
    // in most (but not all!) cases
    resource->class_id = g_strdup(class_id);
}

const char *
ddm_data_resource_get_resource_id (DDMDataResource *resource)
{
    g_return_val_if_fail(resource != NULL, NULL);

    return resource->resource_id;
}

const char *
ddm_data_resource_get_class_id (DDMDataResource *resource)
{
    g_return_val_if_fail(resource != NULL, NULL);

    return resource->class_id;
}

gboolean
ddm_data_resource_is_local (DDMDataResource *resource)
{
    g_return_val_if_fail(resource != NULL, FALSE);

    return resource->local;
}

static void
set_value(DDMDataType   type,
          DDMDataValue *value,
          void           *location)
{
    if (DDM_DATA_IS_LIST(type)) {
        *((GSList **)location) = value->u.list;
    } else {
        switch (DDM_DATA_BASE(type)) {
        case DDM_DATA_NONE:
            break;
        case DDM_DATA_BOOLEAN:
            *((gboolean *)location) = value->u.boolean;
            break;
        case DDM_DATA_INTEGER:
            *((int *)location) = value->u.integer;
            break;
        case DDM_DATA_LONG:
            *((gint64 *)location) = value->u.long_;
            break;
        case DDM_DATA_FLOAT:
            *((double *)location) = value->u.float_;
            break;
        case DDM_DATA_STRING:
            *((const char **)location) = value->u.string;
            break;
        case DDM_DATA_RESOURCE:
            *((DDMDataResource **)location) = value->u.resource;
            break;
        case DDM_DATA_URL:
            *((const char **)location) = value->u.string;
            break;
        }
    }
}

static void
set_default_value(DDMDataType  type,
                  void          *location)
                  
{
    if (DDM_DATA_IS_LIST(type)) {
        *((GSList **)location) = NULL;
    } else {
        switch (type) {
        case DDM_DATA_BOOLEAN:
            *((gboolean *)location) = FALSE;
            break;
        case DDM_DATA_INTEGER:
            *((int *)location) = 0;
            break;
        case DDM_DATA_LONG:
            *((gint64 *)location) = 0;
            break;
        case DDM_DATA_FLOAT:
            *((double *)location) = 0.0;
            break;
        case DDM_DATA_STRING:
            *((const char **)location) = NULL;
            break;
        case DDM_DATA_RESOURCE:
            *((DDMDataResource **)location) = NULL;
            break;
        case DDM_DATA_URL:
            *((const char **)location) = NULL;
            break;
        case DDM_DATA_LIST:
        case DDM_DATA_NONE:
            break;
        }
    }
}

static const char *
type_to_string(DDMDataType type)
{
    const char *result;
    
    switch (DDM_DATA_BASE(type)) {
    case DDM_DATA_NONE:
        result = "list:none";
        break;
    case DDM_DATA_BOOLEAN:
        result = "list:boolean";
        break;
    case DDM_DATA_INTEGER:
        result = "list:integer";
        break;
    case DDM_DATA_LONG:
        result = "list:long";
        break;
    case DDM_DATA_FLOAT:
        result = "list:float";
        break;
    case DDM_DATA_STRING:
        result = "list:string";
        break;
    case DDM_DATA_RESOURCE:
        result = "list:resource";
        break;
    case DDM_DATA_URL:
        result = "list:url";
        break;
    default:
        result = "list:<unknown>";
        break;
    }

    if (!DDM_DATA_IS_LIST(type))
        result += 5;

    return result;
}

void
ddm_data_resource_get(DDMDataResource *resource,
                      ...)
{
    va_list vap;

    va_start(vap, resource);

    while (TRUE) {
        const char *name;
        DDMDataType type;
        void *location;
        GSList *l;

        name = va_arg(vap, char *);
        if (name == NULL)
            break;

        type = va_arg(vap, DDMDataType);
        location = va_arg(vap, void *);

        for (l = resource->properties; l; l = l->next) {
            DDMDataProperty *property = l->data;
            if (strcmp(name, property->qname->name) == 0) {
                if (type == property->value.type) {
                    set_value(type, &property->value, location);
                    goto next_property;
                } else if (property->value.type != DDM_DATA_NONE) {
                    g_warning("Property %s: Type %s doesn't match requested type %s",
                              name,
                              type_to_string(property->value.type), type_to_string(type));
                }
            }
        }

        set_default_value(type, location);
        
    next_property:
        ;
    }

    va_end(vap);
}

void
ddm_data_resource_get_by_qname(DDMDataResource *resource,
                               ...)
{
    va_list vap;

    va_start(vap, resource);

    while (TRUE) {
        DDMQName *qname;
        DDMDataType type;
        void *location;
        GSList *l;

        qname = va_arg(vap, DDMQName *);
        if (qname == NULL)
            break;

        type = va_arg(vap, DDMDataType);
        location = va_arg(vap, void *);

        for (l = resource->properties; l; l = l->next) {
            DDMDataProperty *property = l->data;
            if (qname == property->qname) {
                if (type == property->value.type) {
                    set_value(type, &property->value, location);
                    goto next_property;
                } else if (property->value.type != DDM_DATA_NONE) {
                    g_warning("Property %s#%s: Type %s doesn't match requested type %s",
                              qname->uri, qname->name,
                              type_to_string(property->value.type), type_to_string(type));
                }
            }
        }

        set_default_value(type, location);

    next_property:
        ;
    }

    va_end(vap);
}

DDMDataProperty *
ddm_data_resource_get_property(DDMDataResource *resource,
                               const char        *name)
{
    GSList *l;
    
    for (l = resource->properties; l; l = l->next) {
        DDMDataProperty *property = l->data;
        
        if (strcmp(name, property->qname->name) == 0)
            return property;
    }

    return NULL;
}

DDMDataProperty *
ddm_data_resource_get_property_by_qname(DDMDataResource *resource,
                                        DDMQName        *qname)
{
    GSList *l;
    
    for (l = resource->properties; l; l = l->next) {
        DDMDataProperty *property = l->data;
        
        if (qname == property->qname)
            return property;    }
    
    return NULL;
}

GSList*
ddm_data_resource_get_properties(DDMDataResource    *resource)
{
    return g_slist_copy(resource->properties);
}

GSList *
_ddm_data_resource_get_default_properties(DDMDataResource *resource)
{
    GSList *result = NULL;
    GSList *l;

    for (l = resource->properties; l; l = l->next) {
        DDMDataProperty *property = l->data;
        if (property->default_include)
            result = g_slist_prepend(result, property);
    }

    return result;
}

void
ddm_data_resource_connect (DDMDataResource *resource,
                           const char      *property,
                           DDMDataFunction  function,
                           gpointer         user_data)
{
    DataConnection *connection;

    connection = g_new0(DataConnection, 1);
    connection->function = function;
    connection->user_data = user_data;
    
    if (property == NULL) {
        connection->type = CONNECTION_TYPE_ANY;
    } else {
        connection->type = CONNECTION_TYPE_NAME;
        connection->match.name = g_intern_string(property);
    }

    resource->connections = g_slist_prepend(resource->connections, connection);
}
   
void
ddm_data_resource_connect_by_qname (DDMDataResource *resource,
                                    DDMQName        *property,
                                    DDMDataFunction  function,
                                    gpointer         user_data)
{
    DataConnection *connection;

    connection = g_new0(DataConnection, 1);
    connection->function = function;
    connection->user_data = user_data;
    
    if (property == NULL) {
        connection->type = CONNECTION_TYPE_ANY;
    } else {
        connection->type = CONNECTION_TYPE_QNAME;
        connection->match.qname = property;
    }

    resource->connections = g_slist_prepend(resource->connections, connection);
}

void
ddm_data_resource_set_client_fetch (DDMDataResource *resource,
                                    DDMClient       *client,
                                    DDMDataFetch    *fetch)
{
    GSList *l;
    DataClient *data_client;

    for (l = resource->clients; l; l = l->next) {
        data_client = l->data;
        if (data_client->client == client) {
            if (fetch)
                ddm_data_fetch_ref(fetch);
            
            ddm_data_fetch_unref(data_client->fetch);
            if (fetch) {
                data_client->fetch = fetch;
            } else {
                resource->clients = g_slist_remove(resource->clients, data_client);
                g_free(data_client);
            }

            return;
        }
    }

    data_client = g_new(DataClient, 1);
    data_client->client = client;
    data_client->fetch = ddm_data_fetch_ref(fetch);
}

void
ddm_data_resource_disconnect (DDMDataResource *resource,
                              DDMDataFunction  function,
                              gpointer           user_data)
{
    GSList *l;

    for (l = resource->connections; l; l = l->next) {
        DataConnection *connection = l->data;
        if (connection->function == function && connection->user_data == user_data) {
            resource->connections = g_slist_delete_link(resource->connections, l);
            g_free(connection);
            return;
        }
    }
}

void
ddm_data_value_clear(DDMDataValue *value)
{
    if (DDM_DATA_IS_LIST(value->type)) {
        switch (DDM_DATA_BASE(value->type)) {
        case DDM_DATA_BOOLEAN:
        case DDM_DATA_INTEGER:
        case DDM_DATA_LONG:
        case DDM_DATA_FLOAT:
        case DDM_DATA_STRING:
        case DDM_DATA_URL:
            g_slist_foreach(value->u.list, (GFunc)g_free, NULL);
            break;
        case DDM_DATA_RESOURCE:
        case DDM_DATA_NONE:
        case DDM_DATA_LIST:
            break;
        }

        g_slist_free(value->u.list);
    } else {
        switch (value->type) {
        case DDM_DATA_NONE:
        case DDM_DATA_BOOLEAN:
        case DDM_DATA_INTEGER:
        case DDM_DATA_LONG:
        case DDM_DATA_FLOAT:
        case DDM_DATA_RESOURCE:
        case DDM_DATA_LIST:
            break;
        case DDM_DATA_STRING:
        case DDM_DATA_URL:
            g_free(value->u.string);
            break;
        }
    }

    value->type = DDM_DATA_NONE;
}

/**
 * Casts a primitive C type to a byte array and then indexes
 * a particular byte of the array.
 */
#define BYTE_OF_PRIMITIVE(p, i) \
    (((const char*)&(p))[(i)])
/** On x86 there is an 80-bit FPU, and if you do "a == b" it may have a
 * or b in an 80-bit register, thus failing to compare the two 64-bit
 * doubles for bitwise equality. So this macro compares the two doubles
 * bitwise.
 */
#define DOUBLES_BITWISE_EQUAL(a, b)                                       \
     (BYTE_OF_PRIMITIVE (a, 0) == BYTE_OF_PRIMITIVE (b, 0) &&       \
      BYTE_OF_PRIMITIVE (a, 1) == BYTE_OF_PRIMITIVE (b, 1) &&       \
      BYTE_OF_PRIMITIVE (a, 2) == BYTE_OF_PRIMITIVE (b, 2) &&       \
      BYTE_OF_PRIMITIVE (a, 3) == BYTE_OF_PRIMITIVE (b, 3) &&       \
      BYTE_OF_PRIMITIVE (a, 4) == BYTE_OF_PRIMITIVE (b, 4) &&       \
      BYTE_OF_PRIMITIVE (a, 5) == BYTE_OF_PRIMITIVE (b, 5) &&       \
      BYTE_OF_PRIMITIVE (a, 6) == BYTE_OF_PRIMITIVE (b, 6) &&       \
      BYTE_OF_PRIMITIVE (a, 7) == BYTE_OF_PRIMITIVE (b, 7))

static gboolean
data_value_matches(DDMDataValue      *value_a,
                   DDMDataValue      *value_b)
                   
{
    if (value_a->type != value_b->type)
        return FALSE;
    
    switch (value_b->type) {
    case DDM_DATA_NONE:
        return TRUE;
    case DDM_DATA_BOOLEAN:
        return value_a->u.boolean == value_b->u.boolean;
    case DDM_DATA_INTEGER:
        return value_a->u.integer == value_b->u.integer;
    case DDM_DATA_LONG:
        return value_a->u.long_ == value_b->u.long_;
    case DDM_DATA_FLOAT:
        return DOUBLES_BITWISE_EQUAL(value_a->u.float_, value_b->u.float_);
    case DDM_DATA_RESOURCE:
        return value_a->u.resource == value_b->u.resource;
    case DDM_DATA_STRING:
    case DDM_DATA_URL:
        return strcmp(value_a->u.string, value_b->u.string) == 0;
    case DDM_DATA_LIST:
        break;
    }

    g_assert_not_reached();
    return FALSE;
}    

/* returns whether the property ended up changed */
static gboolean
data_property_set(DDMDataProperty *property,
                  DDMDataValue    *value)
{
    ddm_data_value_clear(&property->value);

    if (DDM_DATA_IS_LIST(property->value.type)) {
        g_warning("data_property_set() called with a list type");
        return FALSE;
    }

    if (data_value_matches(&property->value, value))
        return FALSE;
    
    if (value->type == DDM_DATA_STRING || value->type == DDM_DATA_URL) {
        property->value.type = value->type;
        property->value.u.string = g_strdup(value->u.string);
    } else {
        property->value = *value;
    }

    return TRUE;
}

static void
data_property_append_value(DDMDataProperty *property,
                           DDMDataValue    *value)
{
    if (property->value.type == DDM_DATA_NONE) {
        property->value.type = value->type | DDM_DATA_LIST;
        property->value.u.list = NULL;
    } else {
        if (!DDM_DATA_IS_LIST(property->value.type)) {
            g_warning("data_property_append_value() called with a non-list existing property");
            return;
        }
        
        if (DDM_DATA_BASE(property->value.type) != value->type) {
            g_warning("data_property_append_value() called with a mismatched type");
            return;
        }
    }

    switch (value->type) {
    case DDM_DATA_BOOLEAN:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.boolean, sizeof(gboolean)));
        return;
    case DDM_DATA_INTEGER:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.integer, sizeof(int)));
        return;
    case DDM_DATA_LONG:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.long_, sizeof(gint64)));
        return;
    case DDM_DATA_FLOAT:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.float_, sizeof(double)));
        return;
    case DDM_DATA_RESOURCE:
        property->value.u.list = g_slist_prepend(property->value.u.list, value->u.resource);
        return;
    case DDM_DATA_STRING:
    case DDM_DATA_URL:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_strdup(value->u.string));
        return;
    case DDM_DATA_NONE:
    case DDM_DATA_LIST:
        break;
    }

    g_warning("data_property_append_value() called with a bad type %d", value->type);
}

static void
data_property_remove_value(DDMDataProperty *property,
                           DDMDataValue    *value)
{
    GSList *l;
    
    if (property->value.type == DDM_DATA_NONE) {
        g_warning("Attempt to remove a property value not there");
        return;
    } else {
        if (!DDM_DATA_IS_LIST(property->value.type)) {
            g_warning("data_property_remove_value() called with a non-list existing property");
            return;
        }
        
        if (DDM_DATA_BASE(property->value.type) != value->type) {
            g_warning("data_property_remove_value() called with a mismatched type");
            return;
        }
    }

    for (l = property->value.u.list; l; l = l->next) {
        DDMDataValue element;
        ddm_data_value_get_element(&property->value, l, &element);

        if (data_value_matches(value, &element)) {
            ddm_data_value_free_element(&property->value, l);
            property->value.u.list = g_slist_delete_link(property->value.u.list, l);

            return;
        }
    }

    g_warning("Attempt to remove a property value not there");
}

static DDMDataProperty *
add_property(DDMDataResource   *resource,
             DDMQName          *qname,
             DDMDataCardinality cardinality)
{
    DDMDataProperty *property = g_new0(DDMDataProperty, 1);

    property->qname = qname;
    property->cardinality = cardinality;
    property->value.type = DDM_DATA_NONE;

    resource->properties = g_slist_prepend(resource->properties, property);

    return property;
}

static void
remove_property(DDMDataResource *resource,
                DDMDataProperty *property)
{
    resource->properties = g_slist_remove(resource->properties, property);
    ddm_data_value_clear(&property->value);
    if (property->default_children) {
        ddm_data_fetch_unref(property->default_children);
    }

    g_free(property);
}

/* return value is whether something changed (we need to emit notification) */
gboolean
ddm_data_resource_update_property(DDMDataResource    *resource,
                                  DDMQName           *property_id,
                                  DDMDataUpdate       update,
                                  DDMDataCardinality  cardinality,
                                  gboolean            default_include,
                                  const char         *default_children,
                                  DDMDataValue       *value)
{
    DDMDataProperty *property = NULL;
    GSList *l;
    gboolean changed;

#if 0
    g_debug("updating resource '%s' property %s update %d new value %s",
            resource->resource_id, property_id->name, update,
            value ? ddm_data_value_to_string(value) : "NULL" ); /* leak! FIXME remove from production */
#endif       
    
    /* it's important to only ever set this to TRUE,
     * never assign it a value that could be FALSE,
     * or you could unset an earlier TRUE. If making
     * errors, err on the side of changed=TRUE since
     * duplicate notifications don't break correctness,
     * just efficiency.
     */
    changed = FALSE;
    
    for (l = resource->properties; l; l = l->next) {
        if (((DDMDataProperty *)l->data)->qname == property_id) {
            property = l->data;
            break;
        }
    }

    if (update == DDM_DATA_UPDATE_DELETE && property == NULL) {
        g_warning("Remove of a property we don't have %s#%s", property_id->uri, property_id->name);
        return FALSE;
    }

    if (property != NULL && cardinality != property->cardinality) {
        g_warning("Previous cardinality of not compatible with new property, discarding old values %s#%s", property_id->uri, property_id->name);
        remove_property(resource, property);
        changed = TRUE;
    }

    switch (cardinality) {
    case DDM_DATA_CARDINALITY_01:
        switch (update) {
        case DDM_DATA_UPDATE_ADD:
            if (property != NULL) {
                g_warning("add update for cardinality 01 with a property already there %s#%s", property_id->uri, property_id->name);
                break;
            }
            property = add_property(resource, property_id, cardinality);
            data_property_set(property, value);
            changed = TRUE;
            break;
        case DDM_DATA_UPDATE_REPLACE:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
                changed = TRUE;
            }
            if (data_property_set(property, value))
                changed = TRUE;
            break;
        case DDM_DATA_UPDATE_DELETE:
            if (property == NULL || !data_value_matches(&property->value, value)) {
                g_warning("remove of a property value not there %s#%s", property_id->uri, property_id->name);
                return FALSE;
            }
            remove_property(resource, property);
            changed = TRUE;
            break;
        case DDM_DATA_UPDATE_CLEAR:
            if (property) {
                remove_property(resource, property);
                changed = TRUE;
            }
            break;
        }
        break;
    case DDM_DATA_CARDINALITY_1:
        switch (update) {
        case DDM_DATA_UPDATE_ADD:
            if (property != NULL) {
                g_warning("add update for cardinality 1 with a property already there %s#%s", property_id->uri, property_id->name);
                break;
            }
            property = add_property(resource, property_id, cardinality);
            data_property_set(property, value);
            changed = TRUE;
            break;
        case DDM_DATA_UPDATE_REPLACE:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
                changed = TRUE;
            }
            if (data_property_set(property, value))
                changed = TRUE;
            break;
        case DDM_DATA_UPDATE_DELETE:
            g_warning("Remove of a property %s#%s with cardinality 1", property_id->uri, property_id->name);
            break;
        case DDM_DATA_UPDATE_CLEAR:
            g_warning("Clear of a property %s#%s with cardinality 1", property_id->uri, property_id->name);
            break;
        }
        break;
    case DDM_DATA_CARDINALITY_N:
        switch (update) {
        case DDM_DATA_UPDATE_ADD:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_append_value(property, value);
            changed = TRUE;
            break;
        case DDM_DATA_UPDATE_REPLACE:
            if (property != NULL) {
                ddm_data_value_clear(&property->value);                
            } else {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_append_value(property, value);
            changed = TRUE;
            break;
        case DDM_DATA_UPDATE_DELETE:
            if (property) {
                data_property_remove_value(property, value);
                changed = TRUE;
            } else {
                g_warning("remove of a property value not there %s#%s", property_id->uri, property_id->name);
            }
            break;
        case DDM_DATA_UPDATE_CLEAR:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            } else {
                ddm_data_value_clear(&property->value);
            }
            changed = TRUE;
            break;
        }
        break;
    }

    if (property != NULL) {
        property->default_include = default_include;
        if (default_children && !property->default_children)
            property->default_children = ddm_data_fetch_from_string(default_children);
    }

    if (changed && g_slist_find(resource->changed_properties, property->qname) == NULL) {
        resource->changed_properties = g_slist_prepend(resource->changed_properties, property->qname);
        _ddm_data_model_mark_changed(resource->model, resource);
    }

    return changed;
}

void
_ddm_data_resource_send_local_notifications (DDMDataResource *resource,
                                             GSList          *changed_properties)
{
    GSList *connection_node = resource->connections;
    GSList *property_node;

    while (connection_node) {
        GSList *next = connection_node->next;
        DataConnection *connection = connection_node->data;
        
        if (connection->type == CONNECTION_TYPE_ANY)
            connection->function(resource, changed_properties, connection->user_data);

        connection_node = next;
    }

    for (property_node = changed_properties; property_node != NULL; property_node = property_node->next) {
        DDMQName *property_id = property_node->data;
            
        connection_node = resource->connections;
        
        while (connection_node) {
            GSList *next = connection_node->next;
            DataConnection *connection = connection_node->data;
            
            if (connection->type == CONNECTION_TYPE_NAME) {
                if (connection->match.name == property_id->name)
                    connection->function(resource, changed_properties, connection->user_data);
            } else if (connection->type == CONNECTION_TYPE_QNAME) {
                if (connection->match.qname == property_id)
                    connection->function(resource, changed_properties, connection->user_data);
            }
            
            connection_node = next;
        }
    }
}

static gboolean
data_resource_needs_local_notifications (DDMDataResource *resource,
                                         GSList          *changed_properties)
{
    GSList *connection_node = resource->connections;
    GSList *property_node;

    while (connection_node) {
        GSList *next = connection_node->next;
        DataConnection *connection = connection_node->data;
        
        if (connection->type == CONNECTION_TYPE_ANY)
            return TRUE;

        connection_node = next;
    }

    for (property_node = changed_properties; property_node != NULL; property_node = property_node->next) {
        DDMQName *property_id = property_node->data;
            
        connection_node = resource->connections;
        
        while (connection_node) {
            GSList *next = connection_node->next;
            DataConnection *connection = connection_node->data;
            
            if (connection->type == CONNECTION_TYPE_NAME) {
                if (connection->match.name == property_id->name)
                    return TRUE;
            } else if (connection->type == CONNECTION_TYPE_QNAME) {
                if (connection->match.qname == property_id)
                    return TRUE;
            }
            
            connection_node = next;
        }
    }

    return FALSE;
}

gboolean
ddm_data_parse_type(const char           *type_string,
                    DDMDataType          *type,
                    DDMDataCardinality   *cardinality,
                    gboolean             *default_include)
{
    const char *p = type_string;
    if (*p == '+') {
        *default_include = TRUE;
        p++;
    } else {
        *default_include = FALSE;
    }

    switch (*p) {
    case 'b':
        *type = DDM_DATA_BOOLEAN;
        break;
    case 'i':
        *type = DDM_DATA_INTEGER;
        break;
    case 'l':
        *type = DDM_DATA_LONG;
        break;
    case 'f':
        *type = DDM_DATA_FLOAT;
        break;
    case 's':
        *type = DDM_DATA_STRING;
        break;
    case 'r':
        *type = DDM_DATA_RESOURCE;
        break;
    case 'u':
        *type = DDM_DATA_URL;
        break;
    default:
        g_warning("Can't understand type string '%s'", type_string);
        return FALSE;
    }

    p++;

    switch (*p) {
    case '*':
        *cardinality = DDM_DATA_CARDINALITY_N;
        p++;
        break;
    case '?':
        *cardinality = DDM_DATA_CARDINALITY_01;
        p++;
        break;
    case '\0':
        *cardinality = DDM_DATA_CARDINALITY_1;
        break;
    default:
        g_warning("Can't understand type string '%s'", type_string);
        return FALSE;
    }

    if (*p != '\0') {
        g_warning("Can't understand type string '%s'", type_string);
        return FALSE;
    }

    return TRUE;
}

char*
ddm_data_value_to_string(DDMDataValue *value)
{
    if (DDM_DATA_IS_LIST(value->type)) {
        GSList *l;
        GString *str;

        str = g_string_new(NULL);
        g_string_append(str, "[");

        for (l = value->u.list; l; l = l->next) {
            DDMDataValue element;
            char *s;
            
            ddm_data_value_get_element(value, l, &element);

            s = ddm_data_value_to_string(&element);
            g_string_append(str, s);
            g_free(s);
            
            if (l->next)
                g_string_append(str, ", ");
        }
        g_string_append(str, "]");

        return g_string_free(str, FALSE);
    }
    
    switch (value->type) {
    case DDM_DATA_NONE:
        return g_strdup("[]");
    case DDM_DATA_BOOLEAN:
        return g_strdup_printf("'%s'", value->u.boolean ? "true" : "false");
    case DDM_DATA_INTEGER:
        return g_strdup_printf("%d", value->u.integer);
    case DDM_DATA_LONG:
        return g_strdup_printf("%" G_GINT64_FORMAT "\n", value->u.long_);
    case DDM_DATA_FLOAT:
        return g_strdup_printf("%f", value->u.float_);
    case DDM_DATA_STRING:
        return g_strdup_printf("'%s'", value->u.string);
    case DDM_DATA_RESOURCE:
        return g_strdup_printf("%s", value->u.resource->resource_id);
    case DDM_DATA_URL:
        return g_strdup_printf("%s", value->u.string);
    default:
        return g_strdup("UNKNOWN");
    }
}

static char*
make_absolute_url(const char *maybe_relative,
                  const char *base_url,
                  GError    **error)
{
    if (*maybe_relative == '/') {
        if (base_url == NULL) {
            g_set_error(error,
                        DDM_DATA_ERROR,
                        DDM_DATA_ERROR_BAD_REPLY,
                        "Relative URL with no base URL");
            return NULL;
        }
        return g_strconcat(base_url, maybe_relative, NULL);
    } else if (g_str_has_prefix(maybe_relative, "http:") || g_str_has_prefix(maybe_relative, "https:")) {
        return g_strdup(maybe_relative);
    } else {
        g_warning("weird url '%s', not sure what to do with it", maybe_relative);
        return g_strdup(maybe_relative);
    }
}

gboolean
ddm_data_value_from_string (DDMDataValue *value,
                            DDMDataType   type,
                            const char   *str,
                            const char   *base_url,
                            GError      **error)
{
    char *str_stripped;
    
    g_return_val_if_fail(value != NULL, FALSE);
    g_return_val_if_fail(str != NULL, FALSE);
    
    str_stripped = g_strdup(str);
    g_strstrip(str_stripped);

    value->type = type;
    
    switch (type) {
    case DDM_DATA_BOOLEAN:
        value->u.boolean = g_ascii_strcasecmp(str_stripped, "true") == 0;
        goto success;
    case DDM_DATA_INTEGER:
        {
            char *end;
            long v = strtol(str_stripped, &end, 10);
            if (*str == '\0' || *end != '\0') {
                g_set_error(error,
                            DDM_DATA_ERROR,
                            DDM_DATA_ERROR_BAD_REPLY,
                            "Invalid float property value '%s'", str);
                goto error;
            }
            value->u.integer = CLAMP(v, G_MININT, G_MAXINT);
        }
        goto success;
    case DDM_DATA_LONG:
        {
            char *end;
            value->u.long_ = g_ascii_strtoll(str_stripped, &end, 10);
            if (*str == '\0' || *end != '\0') {
                g_set_error(error,
                            DDM_DATA_ERROR,
                            DDM_DATA_ERROR_BAD_REPLY,
                            "Invalid long property value '%s'", str);
                goto error;
            }
        }
        goto success;
    case DDM_DATA_FLOAT:
        {
            char *end;
            value->u.float_ = g_ascii_strtod(str_stripped, &end);
            if (*str == '\0' || *end != '\0') {
                g_set_error(error,
                            DDM_DATA_ERROR,
                            DDM_DATA_ERROR_BAD_REPLY,
                            "Invalid float property value '%s'", str);
                goto error;
            }
        }
        goto success;
    case DDM_DATA_STRING:
        value->u.string = g_strdup(str);
        goto success;
    case DDM_DATA_URL:
        value->u.string = make_absolute_url(str_stripped, base_url, error);
        if (value->u.string == NULL)
            goto error;
        goto success;
    case DDM_DATA_RESOURCE:
        g_critical("Data type DDM_DATA_RESOURCE invalid in ddm_data_value_from_string()");
        goto error;
    case DDM_DATA_NONE:
        g_critical("Data type DDM_DATA_RESOURCE invalid in ddm_data_value_from_string()");
        goto error;
    case DDM_DATA_LIST:
        g_critical("Data type DDM_DATA_RESOURCE invalid in ddm_data_value_from_string()");
        goto error;
    }

    g_critical("Invalid data type %d in ddm_data_value_from_string()", type);

 error:
    g_free(str_stripped);
    return FALSE;

 success:
    g_free(str_stripped);
    return TRUE;
}

static void
print_value(DDMDataValue *value)
{
    char *s;
    s = ddm_data_value_to_string(value);
    g_print("%s\n", s);
    g_free(s);
}

void
_ddm_data_resource_dump(DDMDataResource *resource)
{
    GSList *property_node;
    
    g_print("%s %s\n", resource->resource_id, resource->class_id);

    for (property_node = resource->properties; property_node; property_node = property_node->next) {
        DDMDataProperty *property = property_node->data;
        char *display;

        if (strcmp(property->qname->uri, resource->class_id) == 0)
            display = g_strdup(property->qname->name);
        else
            display = g_strdup_printf("%s#%s", property->qname->uri, property->qname->name);

        g_print("   %s: ", display);
        g_free(display);

        print_value(&property->value);        
    }
}

DDMDataFetch *
_ddm_data_resource_get_received_fetch (DDMDataResource *resource)
{
    return resource->received_fetch;
}

DDMDataFetch *
_ddm_data_resource_get_requested_fetch (DDMDataResource *resource)
{
    return resource->requested_fetch;
}

gint64
_ddm_data_resource_get_requested_serial (DDMDataResource *resource)
{
    return resource->requested_serial;
}

void
_ddm_data_resource_fetch_requested (DDMDataResource *resource,
                                    DDMDataFetch    *fetch,
                                    guint64          serial)
{
    if (resource->requested_fetch == NULL) {
        resource->requested_fetch = ddm_data_fetch_ref(fetch);
    } else {
        DDMDataFetch *old_requested = resource->requested_fetch;
        resource->requested_fetch = ddm_data_fetch_merge(old_requested, fetch);
        ddm_data_fetch_unref(old_requested);
    }

    resource->requested_serial = serial;
}

void
_ddm_data_resource_fetch_received (DDMDataResource *resource,
                                   DDMDataFetch    *received_fetch)
{
    if (resource->received_fetch == NULL) {
        resource->received_fetch = ddm_data_fetch_ref(received_fetch);
    } else {
        DDMDataFetch *old_received = resource->received_fetch;
        resource->received_fetch = ddm_data_fetch_merge(old_received, received_fetch);
        ddm_data_fetch_unref(old_received);
    }
}

void
_ddm_data_resource_resolve_notifications (DDMDataResource          *resource,
                                          DDMClientNotificationSet *notification_set)
{
    GSList *l;
    
    for (l = resource->clients; l; l = l->next) {
        DataClient *data_client = l->data;

        _ddm_client_notification_set_add(notification_set,
                                         resource,
                                         data_client->client,
                                         data_client->fetch,
                                         resource->changed_properties);
    }

    if (data_resource_needs_local_notifications(resource, resource->changed_properties)) {
        /* received_fetch here is an overestimate, since it covers both local connections
         * and connections on behalf of clients; we possibly should keep separetely
         * the fetches resulting from locally-generated queries. And overestimate doesn't
         * hurt much, however.
         *
         * See also comment in ddm-data-query.c:mark_received_fetches()
         */
        _ddm_client_notification_set_add(notification_set,
                                         resource,
                                         _ddm_data_model_get_local_client(resource->model),
                                         resource->received_fetch,
                                         resource->changed_properties);
    }

    g_slist_free(resource->changed_properties);
    resource->changed_properties = NULL;
}
