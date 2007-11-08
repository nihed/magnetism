/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-dbus-model-client.h"
#include "hippo-dbus-server.h"
#include "hippo-dbus-model.h"

typedef struct _DataClientConnection DataClientConnection;
typedef struct _DataClientQueryClosure DataClientQueryClosure;

static void hippo_dbus_model_client_iface_init(DDMClientIface *iface);

static void add_resource_to_message (HippoDBusModelClient *client,
                                     DBusMessageIter      *resource_array_iter,
                                     DDMDataResource      *resource,
                                     DDMDataFetch         *fetch,
                                     gboolean              indirect,
                                     gboolean              is_notification,
                                     GSList               *changed_properties);

struct _HippoDBusModelClient {
    GObject parent;

    DBusConnection *connection;
    DDMDataModel *model;
    
    char *bus_name;
    char *path;
    
    GHashTable *connections;
    gboolean disconnected;
};

struct _HippoDBusModelClientClass {
    GObjectClass parent_class;
};

struct _DataClientConnection {
    HippoDBusModelClient *client;
    DDMDataResource *resource;
    DDMDataFetch *fetch;
};

struct _DataClientQueryClosure {
    HippoDBusModelClient *client;
    DBusMessage *message;
    DDMDataFetch *fetch;
};

G_DEFINE_TYPE_WITH_CODE(HippoDBusModelClient, hippo_dbus_model_client, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(DDM_TYPE_CLIENT, hippo_dbus_model_client_iface_init);)

static DataClientConnection *
data_client_connection_new (HippoDBusModelClient  *client,
                            DDMDataResource       *resource)
{
    DataClientConnection *connection = g_new0(DataClientConnection, 1);

    connection->client = client;
    connection->resource = ddm_data_resource_ref(resource);
    connection->fetch = NULL;

    return connection;
}

static void
data_client_connection_set_fetch (DataClientConnection *connection,
                                  DDMDataFetch       *fetch)
{
    if (fetch)
        ddm_data_fetch_ref(fetch);

    ddm_data_resource_set_client_fetch(connection->resource, DDM_CLIENT(connection->client), fetch);
    
    if (connection->fetch)
        ddm_data_fetch_unref(connection->fetch);

    connection->fetch = fetch;
}

static void
data_client_connection_destroy(DataClientConnection *connection)
{
    ddm_data_resource_set_client_fetch(connection->resource, DDM_CLIENT(connection->client), NULL);
    ddm_data_resource_unref(connection->resource);

    g_free(connection);
}

/*****************************************************************/

static DataClientQueryClosure *
data_client_query_closure_new (HippoDBusModelClient *client,
                               DBusMessage          *message,
                               DDMDataFetch         *fetch)
{
    DataClientQueryClosure *closure = g_new0(DataClientQueryClosure, 1);
    closure->client = client ? g_object_ref(client) : NULL;
    closure->message = dbus_message_ref(message);
    closure->fetch = fetch ? ddm_data_fetch_ref(fetch) : NULL;

    return closure;
}

static void
data_client_query_closure_destroy (DataClientQueryClosure *closure)
{
    if (closure->client)
        g_object_unref(closure->client);
    if (closure->fetch)
        ddm_data_fetch_unref(closure->fetch);
    dbus_message_unref(closure->message);
    g_free(closure);
}

/*****************************************************************/

static void
add_property_value_to_message(DBusMessageIter    *property_array_iter,
                              DDMQName           *property_qname,
                              DDMDataUpdate       update,
                              DDMDataValue       *value,
                              DDMDataCardinality  cardinality)
{
    DBusMessageIter property_iter;
    DBusMessageIter value_iter;
    char update_byte;
    char type_byte;
    char cardinality_byte;
    const char *value_signature = NULL;
    
    switch (update) {
    case DDM_DATA_UPDATE_ADD:
        update_byte = 'a';
        break;
    case DDM_DATA_UPDATE_REPLACE:
        update_byte = 'r';
        break;
    case DDM_DATA_UPDATE_DELETE:
        update_byte = 'd';
        break;
    case DDM_DATA_UPDATE_CLEAR:
        update_byte = 'c';
        break;
    }
    
    switch (value->type) {
    case DDM_DATA_BOOLEAN:
        type_byte = 'b';
        value_signature = "b";
        break;
    case DDM_DATA_INTEGER:
        type_byte = 'i';
        value_signature = "i";
        break;
    case DDM_DATA_LONG:
        type_byte = 'l';
        value_signature = "x";
        break;
    case DDM_DATA_FLOAT:
        type_byte = 'f';
        value_signature = "d";
        break;
    case DDM_DATA_NONE: /* Empty list, type doesn't matter */
    case DDM_DATA_STRING:
        type_byte = 's';
        value_signature = "s";
        break;
    case DDM_DATA_RESOURCE:
        type_byte = 'r';
        value_signature = "s";
        break;
    case DDM_DATA_URL:
        type_byte = 'u';
        value_signature = "s";
        break;
    case DDM_DATA_LIST:
        break;
    }

    g_assert(value_signature != NULL);
    
    switch (cardinality) {
    case DDM_DATA_CARDINALITY_01:
        cardinality_byte = '?';
        break;
    case DDM_DATA_CARDINALITY_1:
        cardinality_byte = '.';
        break;
    case DDM_DATA_CARDINALITY_N:
        cardinality_byte = '*';
        break;
    }
    
    dbus_message_iter_open_container(property_array_iter, DBUS_TYPE_STRUCT, NULL, &property_iter);
    dbus_message_iter_append_basic(&property_iter, DBUS_TYPE_STRING, &property_qname->uri);
    dbus_message_iter_append_basic(&property_iter, DBUS_TYPE_STRING, &property_qname->name);
    dbus_message_iter_append_basic(&property_iter, DBUS_TYPE_BYTE, &update_byte);
    dbus_message_iter_append_basic(&property_iter, DBUS_TYPE_BYTE, &type_byte);
    dbus_message_iter_append_basic(&property_iter, DBUS_TYPE_BYTE, &cardinality_byte);

    dbus_message_iter_open_container(&property_iter, DBUS_TYPE_VARIANT, value_signature, &value_iter);
    
    switch (value->type) {
    case DDM_DATA_BOOLEAN:
        {
            dbus_bool_t v = value->u.boolean;
            dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_BOOLEAN, &v);
        }
        break;
    case DDM_DATA_INTEGER:
        dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_INT32, &value->u.integer);
        break;
    case DDM_DATA_LONG:
        dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_INT64, &value->u.long_);
        break;
    case DDM_DATA_FLOAT:
        dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_DOUBLE, &value->u.float_);
        break;
    case DDM_DATA_NONE:
        {
            const char *v = "";
            dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_STRING, &v);
        }
        break;
    case DDM_DATA_STRING:
    case DDM_DATA_URL:
        dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_STRING, &value->u.string);
        break;
    case DDM_DATA_RESOURCE:
        {
            const char *v = ddm_data_resource_get_resource_id(value->u.resource);

            dbus_message_iter_append_basic(&value_iter, DBUS_TYPE_STRING, &v);
            
        }
        break;
    case DDM_DATA_LIST:
        break;
    }
    
    dbus_message_iter_close_container(&property_iter, &value_iter);
    dbus_message_iter_close_container(property_array_iter, &property_iter);
}

static void
add_property_children_to_message(HippoDBusModelClient *client,
                                 DBusMessageIter      *resource_array_iter,
                                 DDMDataProperty      *property,
                                 DDMDataFetch         *children)
{
    DDMDataValue value;
            
    ddm_data_property_get_value(property, &value);
    
    if (value.type == DDM_DATA_RESOURCE) {
        add_resource_to_message(client, resource_array_iter, value.u.resource, children, TRUE, FALSE, NULL);
    } else if (value.type == (DDM_DATA_RESOURCE | DDM_DATA_LIST)) {
        GSList *l;
        for (l = value.u.list; l; l = l->next)
            add_resource_to_message(client, resource_array_iter, l->data, children, TRUE, FALSE, NULL);
    }
}

static void
add_property_to_message(DBusMessageIter   *property_array_iter,
                        DDMDataProperty *property)
{
    DDMDataCardinality cardinality;
    DDMDataValue value;
    DDMQName *property_qname;
    
    ddm_data_property_get_value(property, &value);
    cardinality = ddm_data_property_get_cardinality(property);
    property_qname = ddm_data_property_get_qname(property);
    
    if (value.type == DDM_DATA_NONE) {
        add_property_value_to_message(property_array_iter, property_qname,
                                      DDM_DATA_UPDATE_CLEAR,
                                      &value, cardinality);
    } else if (DDM_DATA_IS_LIST(value.type)) {
        GSList *l;
        
        for (l = value.u.list; l; l = l->next) {
            DDMDataValue element;
            ddm_data_value_get_element(&value, l, &element);
            
            add_property_value_to_message(property_array_iter, property_qname,
                                          l == value.u.list ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_ADD,
                                          &element, cardinality);
        }
    } else {
        add_property_value_to_message(property_array_iter, property_qname,
                                      DDM_DATA_UPDATE_REPLACE,
                                      &value, cardinality);
    }
}

static void
add_resource_to_message(HippoDBusModelClient *client,
                        DBusMessageIter      *resource_array_iter,
                        DDMDataResource      *resource,
                        DDMDataFetch         *fetch,
                        gboolean              indirect,
                        gboolean              is_notification,
                        GSList               *changed_properties)
{
    DDMDataFetchIter fetch_iter;
    DataClientConnection *connection;
    DDMDataFetch *new_fetch;
    DDMDataFetch *total_fetch;
    DBusMessageIter resource_iter;
    DBusMessageIter property_array_iter;
    const char *resource_id;
    const char *class_id;
    dbus_bool_t indirect_bool;

    connection = g_hash_table_lookup(client->connections, ddm_data_resource_get_resource_id(resource));
    if (connection == NULL) {
        connection = data_client_connection_new(client, resource);
        g_hash_table_insert(client->connections, (char *)ddm_data_resource_get_resource_id(resource), connection);
    }

    if (is_notification) {
        new_fetch = ddm_data_fetch_ref(fetch);
        total_fetch = ddm_data_fetch_ref(fetch);
    } else {
        if (connection->fetch)
            new_fetch = ddm_data_fetch_subtract(fetch, connection->fetch);
        else
            new_fetch = ddm_data_fetch_ref(fetch);
        
        if (new_fetch == NULL && indirect)
            return;
        
        if (connection->fetch)
            total_fetch = ddm_data_fetch_merge(fetch, connection->fetch);
        else
            total_fetch = ddm_data_fetch_ref(fetch);
        
        data_client_connection_set_fetch(connection, total_fetch);
    }

    if (new_fetch) {
        ddm_data_fetch_iter_init(&fetch_iter, resource, new_fetch);
        while (ddm_data_fetch_iter_has_next(&fetch_iter)) {
            DDMDataProperty *property;
            DDMDataFetch *children;

            ddm_data_fetch_iter_next(&fetch_iter, &property, &children);

            /* FIXME: This check on children isn't really right ... if we have a resource-value
             * property that is default-fetched without default-children, then we should
             * send an empty resource element for it, because the recipient needs at least
             * the classId. */
            if (!children)
                continue;
            
            if (is_notification && g_slist_find(changed_properties, ddm_data_property_get_qname(property)) == NULL)
                continue;
            
            add_property_children_to_message(client, resource_array_iter, property, children);
        }
        ddm_data_fetch_iter_clear(&fetch_iter);
    }
    
    resource_id = ddm_data_resource_get_resource_id(resource);
    class_id = ddm_data_resource_get_class_id(resource);
    indirect_bool = indirect;

    dbus_message_iter_open_container(resource_array_iter, DBUS_TYPE_STRUCT, NULL, &resource_iter);
    dbus_message_iter_append_basic(&resource_iter, DBUS_TYPE_STRING, &resource_id);
    dbus_message_iter_append_basic(&resource_iter, DBUS_TYPE_STRING, &class_id);
    dbus_message_iter_append_basic(&resource_iter, DBUS_TYPE_BOOLEAN, &indirect_bool);
    
    dbus_message_iter_open_container(&resource_iter, DBUS_TYPE_ARRAY, "(ssyyyv)", &property_array_iter);

    if (new_fetch) {
        ddm_data_fetch_iter_init(&fetch_iter, resource, new_fetch);
        while (ddm_data_fetch_iter_has_next(&fetch_iter)) {
            DDMDataProperty *property;

            ddm_data_fetch_iter_next(&fetch_iter, &property, NULL);

            if (is_notification && g_slist_find(changed_properties, ddm_data_property_get_qname(property)) == NULL)
                continue;
            
            add_property_to_message(&property_array_iter, property);
        }
        
        ddm_data_fetch_iter_clear(&fetch_iter);
    }
    
    dbus_message_iter_close_container(&resource_iter, &property_array_iter);
    dbus_message_iter_close_container(resource_array_iter, &resource_iter);

    if (new_fetch)
        ddm_data_fetch_unref(new_fetch);
    ddm_data_fetch_unref(total_fetch);
}

/*****************************************************************/

static void
hippo_dbus_model_client_dispose (GObject *object)
{
    HippoDBusModelClient *dbus_client = HIPPO_DBUS_MODEL_CLIENT(object);
    if (!dbus_client->disconnected) {
        dbus_client->disconnected = TRUE;
        
        hippo_dbus_unwatch_for_disconnect(hippo_app_get_dbus(hippo_get_app()),
                                          dbus_client->bus_name);

        g_hash_table_destroy(dbus_client->connections);
        dbus_client->connections = NULL;
    }
}

static void
hippo_dbus_model_client_finalize (GObject *object)
{
    HippoDBusModelClient *dbus_client = HIPPO_DBUS_MODEL_CLIENT(object);
    
    g_free(dbus_client->bus_name);
    g_free(dbus_client->path);
}

static void
hippo_dbus_model_client_init (HippoDBusModelClient *dbus_client)
{
    dbus_client->connections = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                     NULL, (GDestroyNotify)data_client_connection_destroy);
    dbus_client->disconnected = FALSE;
}

static void
hippo_dbus_model_client_class_init (HippoDBusModelClientClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = hippo_dbus_model_client_dispose;
    object_class->finalize = hippo_dbus_model_client_finalize;
}

static gpointer
hippo_dbus_model_client_begin_notification (DDMClient *client)
{
    return NULL;
}

static void
hippo_dbus_model_client_notify (DDMClient       *client,
                                DDMDataResource *resource,
                                GSList          *changed_properties,
                                gpointer         notification_data)
{
    HippoDBusModelClient *dbus_client = HIPPO_DBUS_MODEL_CLIENT(client);
    DataClientConnection *client_connection = g_hash_table_lookup(dbus_client->connections,
                                                                  ddm_data_resource_get_resource_id(resource));
    DBusMessage *message;
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    
    message = dbus_message_new_method_call(dbus_client->bus_name, dbus_client->path,
                                           HIPPO_DBUS_MODEL_CLIENT_INTERFACE, "Notify");
    
    dbus_message_iter_init_append(message, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(ssba(ssyyyv))", &array_iter);

    add_resource_to_message(dbus_client, &array_iter,
                            resource, client_connection->fetch,
                            FALSE,
                            TRUE, changed_properties);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    dbus_connection_send(dbus_client->connection, message, NULL);

    /* FIXME: We should catch errors, and kick the client connection on error */

    dbus_message_unref(message);
}

static void
hippo_dbus_model_client_end_notification (DDMClient       *client,
                                          gpointer         notification_data)
{
}

static void
hippo_dbus_model_client_iface_init(DDMClientIface *iface)
{
    iface->begin_notification = hippo_dbus_model_client_begin_notification;
    iface->notify = hippo_dbus_model_client_notify;
    iface->end_notification = hippo_dbus_model_client_end_notification;
}

/*****************************************************************/

HippoDBusModelClient *
hippo_dbus_model_client_new (DBusConnection *connection,
                             DDMDataModel   *model,
                             const char     *bus_name,
                             const char     *path)
{
    HippoDBusModelClient *dbus_client;

    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail(bus_name != NULL, NULL);
    g_return_val_if_fail(path != NULL, NULL);
    
    dbus_client = g_object_new(HIPPO_TYPE_DBUS_MODEL_CLIENT, NULL);

    dbus_client->connection = connection;
    dbus_client->model = model;
    dbus_client->bus_name = g_strdup(bus_name);
    dbus_client->path = g_strdup(path);

    hippo_dbus_watch_for_disconnect(hippo_app_get_dbus(hippo_get_app()),
                                    bus_name);

    return dbus_client;
}

const char *
hippo_dbus_model_client_get_bus_name (HippoDBusModelClient *dbus_client)
{
    g_return_val_if_fail(HIPPO_IS_DBUS_MODEL_CLIENT(dbus_client), NULL);

    return dbus_client->bus_name;
}

void
hippo_dbus_model_client_disconnected (HippoDBusModelClient *dbus_client)
{
    g_return_if_fail(HIPPO_IS_DBUS_MODEL_CLIENT(dbus_client));
    
    g_object_run_dispose(G_OBJECT(dbus_client));
}

/*****************************************************************/

static void
on_query_success (GSList  *results,
                  gpointer data)
{
    DataClientQueryClosure *closure = data;
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    DBusMessage *reply;
    GSList *l;

    reply = dbus_message_new_method_return(closure->message);
    dbus_message_iter_init_append(reply, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(ssba(ssyyyv))", &array_iter);

    for (l = results; l; l = l->next) {
        add_resource_to_message(closure->client, &array_iter, l->data, closure->fetch, FALSE, FALSE, NULL);
    }
    
    dbus_message_iter_close_container(&iter, &array_iter);

    dbus_connection_send(closure->client->connection, reply, NULL);
    dbus_message_unref(reply);
    
    data_client_query_closure_destroy(closure);
}

static void
on_query_error (DDMDataError    error,
                const char     *message,
                gpointer        data)
{
    DataClientQueryClosure *closure = data;
    DBusMessage *reply;
    DBusMessageIter iter;
    dbus_int32_t code = (dbus_int32_t)error;

    reply = dbus_message_new_error(closure->message,
                                   HIPPO_DBUS_MODEL_ERROR,
                                   message);
    
    dbus_message_iter_init_append(reply, &iter);
    dbus_message_iter_append_basic(&iter, DBUS_TYPE_INT32, &code);
    
    dbus_connection_send(closure->client->connection, reply, NULL);
    dbus_message_unref(reply);
    
    data_client_query_closure_destroy(closure);
}

gboolean
hippo_dbus_model_client_do_query (HippoDBusModelClient *client,
                                  DBusMessage          *message,
                                  const char           *method_uri,
                                  DDMDataFetch         *fetch,
                                  GHashTable           *params)
{
    DataClientQueryClosure *closure;
    DDMDataQuery *query;
    char *fetch_string;

    g_return_val_if_fail(HIPPO_IS_DBUS_MODEL_CLIENT(client), FALSE);
    g_return_val_if_fail(!client->disconnected, FALSE);
    
    closure = data_client_query_closure_new(client, message, fetch);

    fetch_string = ddm_data_fetch_to_string(fetch);
    query = ddm_data_model_query_params(client->model, method_uri, fetch_string, params);
    g_free(fetch_string);
    
    if (query == NULL) {
        data_client_query_closure_destroy(closure);
        return FALSE;
    }

    ddm_data_query_set_multi_handler(query, on_query_success, closure);
    ddm_data_query_set_error_handler(query, on_query_error, closure);

    return TRUE;
}

/*****************************************************************/

static void
on_update_success (gpointer data)
{
    DataClientQueryClosure *closure = data;
    DBusMessage *reply;

    reply = dbus_message_new_method_return(closure->message);
    dbus_connection_send(closure->client->connection, reply, NULL);
    dbus_message_unref(reply);
    
    data_client_query_closure_destroy(closure);
}

gboolean
hippo_dbus_model_client_do_update (DDMDataModel *model,
                                   DBusMessage  *message,
                                   const char   *method_uri,
                                   GHashTable   *params)
{
    DataClientQueryClosure *closure;
    DDMDataQuery *query;
    
    closure = data_client_query_closure_new(NULL, message, NULL);

    query = ddm_data_model_update_params(model, method_uri, params);
    if (query == NULL) {
        data_client_query_closure_destroy(closure);
        return FALSE;
    }

    ddm_data_query_set_update_handler(query, on_update_success, closure);
    ddm_data_query_set_error_handler(query, on_query_error, closure);

    return TRUE;
}
