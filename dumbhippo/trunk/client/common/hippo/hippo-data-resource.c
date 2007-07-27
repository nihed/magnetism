/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdarg.h>
#include <string.h>

#include "hippo-data-fetch.h"
#include "hippo-data-resource-internal.h"

typedef enum {
    CONNECTION_TYPE_ANY,
    CONNECTION_TYPE_QNAME,
    CONNECTION_TYPE_NAME
} DataConnectionType;

typedef struct _DataProperty DataProperty;
typedef struct _DataConnection DataConnection;

struct _HippoDataProperty {
    HippoQName *qname;
    HippoDataValue value;
    HippoDataFetch *default_children;
    guint cardinality : 4;
    guint default_include : 1;
};

struct _DataConnection {
    DataConnectionType type;
    union {
        HippoQName *qname;
        const char *name;
    } match;
    
    HippoDataFunction function;
    gpointer user_data;
};

struct _HippoDataResource
{
    char *resource_id;
    char *class_id;

    GSList *connections;
    GSList *properties;
};

void
hippo_data_value_get_element(HippoDataValue *value,
                             GSList         *node,
                             HippoDataValue *element)
{
    g_return_if_fail(HIPPO_DATA_IS_LIST(value->type));

    element->type = HIPPO_DATA_BASE(value->type);
    
    switch (element->type) {
    case HIPPO_DATA_BOOLEAN:
        element->u.boolean =  *(gboolean *)node->data;
        return;
    case HIPPO_DATA_INTEGER:
        element->u.integer =  *(int *)node->data;
        return;
    case HIPPO_DATA_LONG:
        element->u.long_ =  *(gint64 *)node->data;
        return;
    case HIPPO_DATA_FLOAT:
        element->u.float_ =  *(double *)node->data;
        return;
    case HIPPO_DATA_STRING:
        element->u.string  = (char *)node->data;
        return;
    case HIPPO_DATA_RESOURCE:
        element->u.resource = (HippoDataResource *)node->data;
        return;
    case HIPPO_DATA_URL:
        element->u.string  = (char *)node->data;
        return;
    case HIPPO_DATA_NONE:
    case HIPPO_DATA_LIST:
        break;
    }

    g_warning("Type value '%d' not valid", element->type);
}

static void
hippo_data_value_free_element(HippoDataValue *value,
                              GSList         *node)
{
    g_return_if_fail(HIPPO_DATA_IS_LIST(value->type));

    switch (HIPPO_DATA_BASE(value->type)) {
    case HIPPO_DATA_BOOLEAN:
    case HIPPO_DATA_INTEGER:
    case HIPPO_DATA_LONG:
    case HIPPO_DATA_FLOAT:
    case HIPPO_DATA_STRING:
    case HIPPO_DATA_URL:
        g_free(node->data);
        return;
    case HIPPO_DATA_RESOURCE:
    case HIPPO_DATA_NONE:
    case HIPPO_DATA_LIST:
        return;
    }

    g_warning("Type value '%d' not valid", HIPPO_DATA_BASE(value->type));
}

HippoQName *
hippo_data_property_get_qname(HippoDataProperty *property)
{
    g_return_val_if_fail(property != NULL, NULL);

    return property->qname;
}

void
hippo_data_property_get_value(HippoDataProperty *property,
                              HippoDataValue    *value)
{
    g_return_if_fail(property != NULL);
    
    *value = property->value;
}

HippoDataType
hippo_data_property_get_type(HippoDataProperty *property)
{
    g_return_val_if_fail(property != NULL, HIPPO_DATA_NONE);
    
    return HIPPO_DATA_BASE(property->value.type);
}

HippoDataCardinality
hippo_data_property_get_cardinality(HippoDataProperty *property)
{
    g_return_val_if_fail(property != NULL, HIPPO_DATA_CARDINALITY_01);

    return property->cardinality;
}

gboolean
hippo_data_property_get_default_include(HippoDataProperty *property)
{
    g_return_val_if_fail(property != NULL, FALSE);
    
    return property->default_include;
}

HippoDataFetch *
hippo_data_property_get_default_children(HippoDataProperty *property)
{
    g_return_val_if_fail(property != NULL, NULL);
    
    return property->default_children;
}

HippoDataResource *
_hippo_data_resource_new(const char *resource_id,
                         const char *class_id)
{
    HippoDataResource *resource = g_new0(HippoDataResource, 1);

    resource->resource_id = g_strdup(resource_id);
    _hippo_data_resource_set_class_id(resource, class_id);

    return resource;
}

void
_hippo_data_resource_set_class_id(HippoDataResource    *resource,
                                  const char           *class_id)
{
    if (resource->class_id != NULL)
        g_free(resource->class_id);
    
    // TODO: strdup() isn't really necessary, since class_id is currently g_intern_string
    // in most (but not all!) cases
    resource->class_id = g_strdup(class_id);
}

const char *
hippo_data_resource_get_resource_id (HippoDataResource *resource)
{
    g_return_val_if_fail(resource != NULL, NULL);

    return resource->resource_id;
}

const char *
hippo_data_resource_get_class_id (HippoDataResource *resource)
{
    g_return_val_if_fail(resource != NULL, NULL);

    return resource->class_id;
}

static void
set_value(HippoDataType   type,
          HippoDataValue *value,
          void           *location)
{
    if (HIPPO_DATA_IS_LIST(type)) {
        *((GSList **)location) = value->u.list;
    } else {
        switch (HIPPO_DATA_BASE(type)) {
        case HIPPO_DATA_NONE:
            break;
        case HIPPO_DATA_BOOLEAN:
            *((gboolean *)location) = value->u.boolean;
            break;
        case HIPPO_DATA_INTEGER:
            *((int *)location) = value->u.integer;
            break;
        case HIPPO_DATA_LONG:
            *((gint64 *)location) = value->u.long_;
            break;
        case HIPPO_DATA_FLOAT:
            *((double *)location) = value->u.float_;
            break;
        case HIPPO_DATA_STRING:
            *((const char **)location) = value->u.string;
            break;
        case HIPPO_DATA_RESOURCE:
            *((HippoDataResource **)location) = value->u.resource;
            break;
        case HIPPO_DATA_URL:
            *((const char **)location) = value->u.string;
            break;
        }
    }
}

static void
set_default_value(HippoDataType  type,
                  void          *location)
                  
{
    if (HIPPO_DATA_IS_LIST(type)) {
        *((GSList **)location) = NULL;
    } else {
        switch (type) {
        case HIPPO_DATA_BOOLEAN:
            *((gboolean *)location) = FALSE;
            break;
        case HIPPO_DATA_INTEGER:
            *((int *)location) = 0;
            break;
        case HIPPO_DATA_LONG:
            *((gint64 *)location) = 0;
            break;
        case HIPPO_DATA_FLOAT:
            *((double *)location) = 0.0;
            break;
        case HIPPO_DATA_STRING:
            *((const char **)location) = NULL;
            break;
        case HIPPO_DATA_RESOURCE:
            *((HippoDataResource **)location) = NULL;
            break;
        case HIPPO_DATA_URL:
            *((const char **)location) = NULL;
            break;
        case HIPPO_DATA_LIST:
        case HIPPO_DATA_NONE:
            break;
        }
    }
}

void
hippo_data_resource_get(HippoDataResource *resource,
                        ...)
{
    va_list vap;

    va_start(vap, resource);

    while (TRUE) {
        const char *name;
        HippoDataType type;
        void *location;
        GSList *l;

        name = va_arg(vap, char *);
        if (name == NULL)
            break;

        type = va_arg(vap, HippoDataType);
        location = va_arg(vap, void *);

        for (l = resource->properties; l; l = l->next) {
            HippoDataProperty *property = l->data;
            if (strcmp(name, property->qname->name) == 0 && type == property->value.type) {
                set_value(type, &property->value, location);
                goto next_property;
            }
        }

        set_default_value(type, location);
        
    next_property:
        ;
    }

    va_end(vap);
}

void
hippo_data_resource_get_by_qname(HippoDataResource *resource,
                                 ...)
{
    va_list vap;

    va_start(vap, resource);

    while (TRUE) {
        HippoQName *qname;
        HippoDataType type;
        void *location;
        GSList *l;

        qname = va_arg(vap, HippoQName *);
        if (qname == NULL)
            break;

        type = va_arg(vap, HippoDataType);
        location = va_arg(vap, void *);

        for (l = resource->properties; l; l = l->next) {
            HippoDataProperty *property = l->data;
            if (qname == property->qname && type == property->value.type) {
                set_value(type, &property->value, location);
                goto next_property;
            }
        }

        set_default_value(type, location);

    next_property:
            ;
    }

    va_end(vap);
}

HippoDataProperty *
_hippo_data_resource_get_property(HippoDataResource *resource,
                                  const char        *name)
{
    GSList *l;
    
    for (l = resource->properties; l; l = l->next) {
        HippoDataProperty *property = l->data;
        
        if (strcmp(name, property->qname->name) == 0)
            return property;
    }

    return NULL;
}

HippoDataProperty *
_hippo_data_resource_get_property_by_qname(HippoDataResource *resource,
                                           HippoQName        *qname)
{
    GSList *l;
    
    for (l = resource->properties; l; l = l->next) {
        HippoDataProperty *property = l->data;
        
        if (qname == property->qname)
            return property;
    }
    
    return NULL;
}

GSList *
_hippo_data_resource_get_default_properties(HippoDataResource *resource)
{
    GSList *result = NULL;
    GSList *l;

    for (l = resource->properties; l; l = l->next) {
        HippoDataProperty *property = l->data;
        if (property->default_include)
            result = g_slist_prepend(result, property);
    }

    return result;
}

void
hippo_data_resource_connect(HippoDataResource *resource,
                            const char        *property,
                            HippoDataFunction  function,
                            gpointer           user_data)
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
hippo_data_resource_connect_by_qname (HippoDataResource *resource,
                                      HippoQName        *property,
                                      HippoDataFunction  function,
                                      gpointer           user_data)
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
hippo_data_resource_disconnect (HippoDataResource *resource,
                                HippoDataFunction  function,
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

static void
data_value_clear(HippoDataValue *value)
{
    if (HIPPO_DATA_IS_LIST(value->type)) {
        switch (HIPPO_DATA_BASE(value->type)) {
        case HIPPO_DATA_BOOLEAN:
        case HIPPO_DATA_INTEGER:
        case HIPPO_DATA_LONG:
        case HIPPO_DATA_FLOAT:
        case HIPPO_DATA_STRING:
        case HIPPO_DATA_URL:
            g_slist_foreach(value->u.list, (GFunc)g_free, NULL);
            break;
        case HIPPO_DATA_RESOURCE:
        case HIPPO_DATA_NONE:
        case HIPPO_DATA_LIST:
            break;
        }

        g_slist_free(value->u.list);
    } else {
        switch (value->type) {
        case HIPPO_DATA_NONE:
        case HIPPO_DATA_BOOLEAN:
        case HIPPO_DATA_INTEGER:
        case HIPPO_DATA_LONG:
        case HIPPO_DATA_FLOAT:
        case HIPPO_DATA_RESOURCE:
        case HIPPO_DATA_LIST:
            break;
        case HIPPO_DATA_STRING:
        case HIPPO_DATA_URL:
            g_free(value->u.string);
            break;
        }
    }

    value->type = HIPPO_DATA_NONE;
}

static gboolean
data_value_matches(HippoDataValue      *value_a,
                   HippoDataValue      *value_b)
                   
{
    if (value_a->type != value_b->type)
        return FALSE;
    
    switch (value_b->type) {
    case HIPPO_DATA_NONE:
        return TRUE;
    case HIPPO_DATA_BOOLEAN:
        return value_a->u.boolean == value_b->u.boolean;
    case HIPPO_DATA_INTEGER:
        return value_a->u.integer == value_b->u.integer;
    case HIPPO_DATA_LONG:
        return value_a->u.long_ == value_b->u.long_;
    case HIPPO_DATA_FLOAT:
        return value_a->u.float_ == value_b->u.float_;
    case HIPPO_DATA_RESOURCE:
        return value_a->u.resource == value_b->u.resource;
    case HIPPO_DATA_STRING:
    case HIPPO_DATA_URL:
        return strcmp(value_a->u.string, value_b->u.string) == 0;
    case HIPPO_DATA_LIST:
        break;
    }

    g_assert_not_reached();
    return FALSE;
}    

static void
data_property_set(HippoDataProperty *property,
                  HippoDataValue    *value)
{
    data_value_clear(&property->value);

    if (HIPPO_DATA_IS_LIST(property->value.type)) {
        g_warning("data_property_set() called with a list type");
        return;
    }

    if (value->type == HIPPO_DATA_STRING || value->type == HIPPO_DATA_URL) {
        property->value.type = value->type;
        property->value.u.string = g_strdup(value->u.string);
    } else {
        property->value = *value;
    }
}

static void
data_property_append_value(HippoDataProperty *property,
                           HippoDataValue    *value)
{
    if (property->value.type == HIPPO_DATA_NONE) {
        property->value.type = value->type | HIPPO_DATA_LIST;
        property->value.u.list = NULL;
    } else {
        if (!HIPPO_DATA_IS_LIST(property->value.type)) {
            g_warning("data_property_append_value() called with a non-list existing property");
            return;
        }
        
        if (HIPPO_DATA_BASE(property->value.type) != value->type) {
            g_warning("data_property_append_value() called with a mismatched type");
            return;
        }
    }

    switch (value->type) {
    case HIPPO_DATA_BOOLEAN:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.boolean, sizeof(gboolean)));
        return;
    case HIPPO_DATA_INTEGER:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.integer, sizeof(int)));
        return;
    case HIPPO_DATA_LONG:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.long_, sizeof(gint64)));
        return;
    case HIPPO_DATA_FLOAT:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_memdup(&value->u.float_, sizeof(double)));
        return;
    case HIPPO_DATA_RESOURCE:
        property->value.u.list = g_slist_prepend(property->value.u.list, value->u.resource);
        return;
    case HIPPO_DATA_STRING:
    case HIPPO_DATA_URL:
        property->value.u.list = g_slist_prepend(property->value.u.list, g_strdup(value->u.string));
        return;
    case HIPPO_DATA_NONE:
    case HIPPO_DATA_LIST:
        break;
    }

    g_warning("data_property_append_value() called with a bad type %d", value->type);
}

static void
data_property_remove_value(HippoDataProperty *property,
                           HippoDataValue    *value)
{
    GSList *l;
    
    if (property->value.type == HIPPO_DATA_NONE) {
        g_warning("Attempt to remove a property value not there");
        return;
    } else {
        if (!HIPPO_DATA_IS_LIST(property->value.type)) {
            g_warning("data_property_remove_value() called with a non-list existing property");
            return;
        }
        
        if (HIPPO_DATA_BASE(property->value.type) != value->type) {
            g_warning("data_property_remove_value() called with a mismatched type");
            return;
        }
    }

    for (l = property->value.u.list; l; l = l->next) {
        HippoDataValue element;
        hippo_data_value_get_element(&property->value, l, &element);

        if (data_value_matches(value, &element)) {
            hippo_data_value_free_element(&property->value, l);
            property->value.u.list = g_slist_delete_link(property->value.u.list, l);

            return;
        }
    }

    g_warning("Attempt to remove a property value not there");
}

static HippoDataProperty *
add_property(HippoDataResource   *resource,
             HippoQName          *qname,
             HippoDataCardinality cardinality)
{
    HippoDataProperty *property = g_new0(HippoDataProperty, 1);

    property->qname = qname;
    property->cardinality = cardinality;
    property->value.type = HIPPO_DATA_NONE;

    resource->properties = g_slist_prepend(resource->properties, property);

    return property;
}

static void
remove_property(HippoDataResource *resource,
                HippoDataProperty *property)
{
    resource->properties = g_slist_remove(resource->properties, property);
    data_value_clear(&property->value);
    if (property->default_children) {
        hippo_data_fetch_unref(property->default_children);
    }

    g_free(property);
}

gboolean
_hippo_data_resource_update_property(HippoDataResource    *resource,
                                     HippoQName           *property_id,
                                     HippoDataUpdate       update,
                                     HippoDataCardinality  cardinality,
                                     gboolean              default_include,
                                     const char           *default_children,
                                     HippoDataValue       *value)
{
    HippoDataProperty *property = NULL;
    GSList *l;

    for (l = resource->properties; l; l = l->next) {
        if (((HippoDataProperty *)l->data)->qname == property_id) {
            property = l->data;
            break;
        }
    }

    if (update == HIPPO_DATA_UPDATE_DELETE && property == NULL) {
        g_warning("Remove of a property we don't have");
        return FALSE;
    }

    if (property != NULL && cardinality != property->cardinality) {
        g_warning("Previous cardinality of not compatible with new property, discarding old values");
        remove_property(resource, property);
    }

    switch (cardinality) {
    case HIPPO_DATA_CARDINALITY_01:
        switch (update) {
        case HIPPO_DATA_UPDATE_ADD:
            if (property != NULL) {
                g_warning("add update for cardinality 01 with a property already there");
                break;
            }
            property = add_property(resource, property_id, cardinality);
            data_property_set(property, value);
            break;
        case HIPPO_DATA_UPDATE_REPLACE:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_set(property, value);
            break;
        case HIPPO_DATA_UPDATE_DELETE:
            if (property == NULL || !data_value_matches(&property->value, value)) {
                g_warning("remove of a property value not there");
                return FALSE;
            }
            remove_property(resource, property);
            break;
        case HIPPO_DATA_UPDATE_CLEAR:
            if (property)
                remove_property(resource, property);
            break;
        }
        break;
    case HIPPO_DATA_CARDINALITY_1:
        switch (update) {
        case HIPPO_DATA_UPDATE_ADD:
            if (property != NULL) {
                g_warning("add update for cardinality 1 with a property already there");
                break;
            }
            property = add_property(resource, property_id, cardinality);
            data_property_set(property, value);
            break;
        case HIPPO_DATA_UPDATE_REPLACE:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_set(property, value);
            break;
        case HIPPO_DATA_UPDATE_DELETE:
            g_warning("Remove of a property with cardinality 1");
            break;
        case HIPPO_DATA_UPDATE_CLEAR:
            g_warning("Clear of a property with cardinality 1");
            break;
        break;
        }
        break;
    case HIPPO_DATA_CARDINALITY_N:
        switch (update) {
        case HIPPO_DATA_UPDATE_ADD:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_append_value(property, value);
            break;
        case HIPPO_DATA_UPDATE_REPLACE:
            if (property != NULL) {
                data_value_clear(&property->value);
            } else {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_append_value(property, value);
            break;
        case HIPPO_DATA_UPDATE_DELETE:
            if (property) {
                data_property_remove_value(property, value);
            } else {
                g_warning("remove of a property value not there");
            }
            break;
        case HIPPO_DATA_UPDATE_CLEAR:
            if (property == NULL) {
                property = add_property(resource, property_id, cardinality);
            } else {
                data_value_clear(&property->value);
            }
            break;
        }
        break;
    }

    if (property != NULL) {
        property->default_include = default_include;
        if (default_children && !property->default_children)
            property->default_children = hippo_data_fetch_from_string(default_children);
    }

    return TRUE;
}

void
_hippo_data_resource_on_resource_change(HippoDataResource *resource,
                                        GSList            *changed_properties)
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
        HippoQName *property_id = property_node->data;
            
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

static void
print_value(HippoDataValue *value)
{
    switch (value->type) {
    case HIPPO_DATA_NONE:
        g_print("[]\n");
        break;
    case HIPPO_DATA_BOOLEAN:
        g_print("'%s'\n", value->u.boolean ? "true" : "false");
        break;
    case HIPPO_DATA_INTEGER:
        g_print("%d\n", value->u.integer);
            break;
    case HIPPO_DATA_LONG:
        g_print("%" G_GINT64_FORMAT "\n", value->u.long_);
        break;
    case HIPPO_DATA_FLOAT:
        g_print("%f\n", value->u.float_);
        break;
    case HIPPO_DATA_STRING:
        g_print("'%s'\n", value->u.string);
        break;
    case HIPPO_DATA_RESOURCE:
        g_print("%s\n", value->u.resource->resource_id);
        break;
    case HIPPO_DATA_URL:
        g_print("%s\n", value->u.string);
        break;
    case HIPPO_DATA_LIST:
        break;
    }
}

void
_hippo_data_resource_dump(HippoDataResource *resource)
{
    GSList *property_node;
    GSList *l;
    
    g_print("%s %s\n", resource->resource_id, resource->class_id);

    for (property_node = resource->properties; property_node; property_node = property_node->next) {
        HippoDataProperty *property = property_node->data;
        char *display;

        if (strcmp(property->qname->uri, resource->class_id) == 0)
            display = g_strdup(property->qname->name);
        else
            display = g_strdup_printf("%s#%s", property->qname->uri, property->qname->name);

        g_print("   %s: ", display);
        g_free(display);

        if (HIPPO_DATA_IS_LIST(property->value.type)) {
            g_print("[");
            for (l = property->value.u.list; l; l = l->next) {
                HippoDataValue element;
                hippo_data_value_get_element(&property->value, l, &element);
                print_value(&element);
                
                if (l->next)
                    g_print(", ");
            }
            g_print("]");
        } else {
            print_value(&property->value);
            g_print("\n");
        }
        
    }
}
