/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdarg.h>
#include <string.h>

#include "hippo-data-resource-internal.h"

typedef enum {
    CONNECTION_TYPE_ANY,
    CONNECTION_TYPE_QNAME,
    CONNECTION_TYPE_NAME
} DataConnectionType;

typedef struct _DataProperty DataProperty;
typedef struct _DataConnection DataConnection;

struct _DataProperty {
    HippoQName *qname;
    HippoDataCardinality cardinality;
    HippoDataValue value;
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

HippoDataResource *
_hippo_data_resource_new(const char *resource_id,
                         const char *class_id)
{
    HippoDataResource *resource = g_new0(HippoDataResource, 1);

    resource->resource_id = g_strdup(resource_id);
    // TODO: strdup() isn't really necessary, since class_id is currently g_intern_string
    // in all cases.
    resource->class_id = g_strdup(class_id);

    return resource;
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
            DataProperty *property = l->data;
            if (strcmp(name, property->qname->name) == 0 && type == property->value.type) {
                switch (type) {
                case HIPPO_DATA_NONE:
                    break;
                case HIPPO_DATA_STRING:
                    *((const char **)location) = property->value.u.string;
                    break;
                case HIPPO_DATA_RESOURCE:
                    *((HippoDataResource **)location) = property->value.u.resource;
                    break;
                case HIPPO_DATA_STRING_LIST:
                case HIPPO_DATA_RESOURCE_LIST:
                    *((GSList **)location) = property->value.u.list;
                    break;
                }
                goto next_property;
            }
        }

        switch (type) {
        case HIPPO_DATA_NONE:
            break;
        case HIPPO_DATA_STRING:
            *((const char **)location) = NULL;
            break;
        case HIPPO_DATA_RESOURCE:
            *((HippoDataResource **)location) = NULL;
            break;
        case HIPPO_DATA_STRING_LIST:
        case HIPPO_DATA_RESOURCE_LIST:
            *((GSList **)location) = NULL;
            break;
        }
        
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
            DataProperty *property = l->data;
            if (qname == property->qname && type == property->value.type) {
                switch (type) {
                case HIPPO_DATA_NONE:
                    break;
                case HIPPO_DATA_STRING:
                    *((const char **)location) = property->value.u.string;
                    break;
                case HIPPO_DATA_RESOURCE:
                    *((HippoDataResource **)location) = property->value.u.resource;
                    break;
                case HIPPO_DATA_STRING_LIST:
                case HIPPO_DATA_RESOURCE_LIST:
                    *((GSList **)location) = property->value.u.list;
                    break;
                }
                goto next_property;
            }
        }

        switch (type) {
        case HIPPO_DATA_NONE:
            break;
        case HIPPO_DATA_STRING:
            *((const char **)location) = NULL;
            break;
        case HIPPO_DATA_RESOURCE:
            *((HippoDataResource **)location) = NULL;
            break;
        case HIPPO_DATA_STRING_LIST:
        case HIPPO_DATA_RESOURCE_LIST:
            *((GSList **)location) = NULL;
            break;
        }
        
        next_property:
            ;
    }

    va_end(vap);
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
data_property_clear(DataProperty *property)
{
    switch (property->value.type) {
    case HIPPO_DATA_NONE:
        break;
    case HIPPO_DATA_RESOURCE:
        break;
    case HIPPO_DATA_STRING:
        g_free(property->value.u.string);
        break;
    case HIPPO_DATA_RESOURCE_LIST:
        g_slist_free(property->value.u.list);
        break;
    case HIPPO_DATA_STRING_LIST:
        g_slist_foreach(property->value.u.list, (GFunc)g_free, NULL);
        g_slist_free(property->value.u.list);
        break;
    }

    property->value.type = HIPPO_DATA_NONE;
}

static gboolean
data_property_value_matches(DataProperty   *property,
                            HippoDataValue *value)
{
    if (value->type != property->value.type)
        return FALSE;
    
    switch (property->value.type) {
    case HIPPO_DATA_NONE:
        return TRUE;
        break;
    case HIPPO_DATA_RESOURCE:
        return value->u.resource != property->value.u.resource;
    case HIPPO_DATA_STRING:
        return strcmp(value->u.string, property->value.u.string) == 0;
    case HIPPO_DATA_RESOURCE_LIST:
    case HIPPO_DATA_STRING_LIST:
        g_assert_not_reached();
        break;
    }
    
    return FALSE;
}    

static void
data_property_set(DataProperty      *property,
                  HippoDataValue    *value)
{

    data_property_clear(property);

    switch (value->type) {
    case HIPPO_DATA_NONE:
        break;
    case HIPPO_DATA_RESOURCE:
        property->value.type = HIPPO_DATA_RESOURCE;
        property->value.u.resource = value->u.resource;
        break;
    case HIPPO_DATA_STRING:
        property->value.type =  HIPPO_DATA_STRING;
        property->value.u.string = g_strdup(value->u.string);
        break;
    case HIPPO_DATA_RESOURCE_LIST:
    case HIPPO_DATA_STRING_LIST:
        g_warning("data_property_set() called with a list type");
        break;
    }
}

static void
data_property_append_value(DataProperty      *property,
                           HippoDataValue    *value)
{
    switch (value->type) {
    case HIPPO_DATA_NONE:
        g_warning("data_property_append_value() called with HIPPO_DATA_NONE");
        return;
    case HIPPO_DATA_RESOURCE:
        if (property->value.type == HIPPO_DATA_NONE) {
            property->value.type = HIPPO_DATA_RESOURCE_LIST;
            property->value.u.list = NULL;
        } else if (property->value.type != HIPPO_DATA_RESOURCE_LIST) {
            g_warning("Attempt to append a resource value to something other than a resource list");
            return;
        }
        property->value.u.list = g_slist_prepend(property->value.u.list, value->u.resource);
        break;
    case HIPPO_DATA_STRING:
        if (property->value.type == HIPPO_DATA_NONE) {
            property->value.type = HIPPO_DATA_STRING_LIST;
            property->value.u.list = NULL;
        } else if (property->value.type != HIPPO_DATA_STRING_LIST) {
            g_warning("Attempt to append a string value to something other than a string list");
            return;
        }
        property->value.u.list = g_slist_prepend(property->value.u.list, g_strdup(value->u.string));
        break;
    case HIPPO_DATA_RESOURCE_LIST:
    case HIPPO_DATA_STRING_LIST:
        g_warning("data_property_append_value() called with a list type");
        return;
    }
}

static void
data_property_remove_value(DataProperty   *property,
                           HippoDataValue *value)
{
    GSList *l;
    
    switch (value->type) {
    case HIPPO_DATA_NONE:
        g_warning("data_property_append_value() called with HIPPO_DATA_NONE");
        return;
    case HIPPO_DATA_RESOURCE:
        if (property->value.type == HIPPO_DATA_NONE) {
            property->value.type = HIPPO_DATA_RESOURCE_LIST;
            property->value.u.list = NULL;
        } else if (property->value.type != HIPPO_DATA_RESOURCE_LIST) {
            g_warning("Attempt to remove a resource value from something other than a resource list");
            return;
        }

        for (l = property->value.u.list; l; l = l->next) {
            if (l->data == value->u.resource) {
                property->value.u.list = g_slist_delete_link(property->value.u.list, l);
                return;
            }
        }

        break;
    case HIPPO_DATA_STRING:
        if (property->value.type == HIPPO_DATA_NONE) {
            property->value.type = HIPPO_DATA_STRING_LIST;
            property->value.u.list = NULL;
        } else if (property->value.type != HIPPO_DATA_STRING_LIST) {
            g_warning("Attempt to remove a string value from something other than a string list");
            return;
        }

        for (l = property->value.u.list; l; l = l->next) {
            if (strcmp(l->data, value->u.string) == 0) {
                property->value.u.list = g_slist_delete_link(property->value.u.list, l);
                return;
            }
        }

        break;
    case HIPPO_DATA_RESOURCE_LIST:
    case HIPPO_DATA_STRING_LIST:
        g_warning("data_property_remove_value() called with a list type");
        return;
    }

    g_warning("Attempt to remove a property value not there");
}

static DataProperty *
add_property(HippoDataResource   *resource,
             HippoQName          *qname,
             HippoDataCardinality cardinality)
{
    DataProperty *property = g_new0(DataProperty, 1);

    property->qname = qname;
    property->cardinality = cardinality;
    property->value.type = HIPPO_DATA_NONE;

    resource->properties = g_slist_prepend(resource->properties, property);

    return property;
}

static void
remove_property(HippoDataResource *resource,
                DataProperty      *property)
{
    resource->properties = g_slist_remove(resource->properties, property);
}

void
_hippo_data_resource_update_property(HippoDataResource    *resource,
                                     HippoQName           *property_id,
                                     HippoDataUpdate       update,
                                     HippoDataCardinality  cardinality,
                                     HippoDataValue       *value,
                                     HippoNotificationSet *notifications)
{
    DataProperty *property = NULL;
    GSList *l;

    for (l = resource->properties; l; l = l->next) {
        if (((DataProperty *)l->data)->qname == property_id) {
            property = l->data;
            break;
        }
    }

    if (update == HIPPO_DATA_UPDATE_REMOVE && property == NULL) {
        g_warning("Remove of a property we don't have");
        return;
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
        case HIPPO_DATA_UPDATE_REMOVE:
            if (property == NULL || !data_property_value_matches(property, value)) {
                g_warning("remove of a property value not there");
                return;
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
        case HIPPO_DATA_UPDATE_REMOVE:
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
                data_property_clear(property);
            } else {
                property = add_property(resource, property_id, cardinality);
            }
            data_property_append_value(property, value);
            break;
        case HIPPO_DATA_UPDATE_REMOVE:
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
                data_property_clear(property);
            }
            break;
        }
        break;
    }

    if (notifications)
        _hippo_notification_set_add(notifications, resource, property_id);
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

void
_hippo_data_resource_dump(HippoDataResource *resource)
{
    GSList *property_node;
    GSList *l;
    
    g_print("%s %s\n", resource->resource_id, resource->class_id);

    for (property_node = resource->properties; property_node; property_node = property_node->next) {
        DataProperty *property = property_node->data;
        char *display;

        if (strcmp(property->qname->uri, resource->class_id) == 0)
            display = g_strdup(property->qname->name);
        else
            display = g_strdup_printf("%s#%s", property->qname->uri, property->qname->name);

        g_print("   %s: ", display);
        g_free(display);

        switch (property->value.type) {
        case HIPPO_DATA_NONE:
            g_print("[]\n");
            break;
        case HIPPO_DATA_STRING:
            g_print("'%s'\n", property->value.u.string);
            break;
        case HIPPO_DATA_RESOURCE:
            g_print("%s\n", property->value.u.resource->resource_id);
            break;
        case HIPPO_DATA_STRING_LIST:
            g_print("[");
            for (l = property->value.u.list; l; l = l->next) {
                g_print("%s", (char *)l->data);
                if (l->next)
                    g_print(", ");
            }
            g_print("]\n");
            break;
        case HIPPO_DATA_RESOURCE_LIST:
            g_print("[");
            for (l = property->value.u.list; l; l = l->next) {
                g_print("%s", ((HippoDataResource *)(l->data))->resource_id);
                if (l->next)
                    g_print(", ");
            }
            g_print("]\n");
            break;
        }
    }
}
