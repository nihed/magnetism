/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include <hippo/hippo-data-cache.h>
#include <ddm/ddm.h>
#include "hippo-dbus-model.h"
#include "main.h"

typedef struct _DataClientId           DataClientId;
typedef struct _DataClientConnection   DataClientConnection;
typedef struct _DataClient             DataClient;
typedef struct _DataClientMap          DataClientMap;
typedef struct _DataClientQueryClosure DataClientQueryClosure;

struct _DataClientFetch
{
    guint ref_count;
    
    char **properties;
    char **fetch_children;
};

struct _DataClientId {
    char *bus_name;
    char *path;
};

struct _DataClientConnection {
    DataClient *client;
    DDMDataResource *resource;
    DDMDataFetch *fetch;
};

struct _DataClient {
    guint ref_count;
    
    DataClientId id;
    
    GHashTable *connections;

    gboolean disconnected;
};

struct _DataClientMap {
    GHashTable *clients;
};

struct _DataClientQueryClosure {
    DataClient *client;
    DBusMessage *message;
    DDMDataFetch *fetch;
};

static DataClient *data_client_ref   (DataClient *client);
static void        data_client_unref (DataClient *client);

static void add_resource_to_message(DataClient        *client,
                                    DBusMessageIter   *resource_array_iter,
                                    DDMDataResource *resource,
                                    DDMDataFetch    *fetch,
                                    gboolean           indirect,
                                    gboolean           is_notification,
                                    GSList            *changed_properties);

static void
on_resource_changed(DDMDataResource *resource,
                    GSList            *changed_properties,
                    gpointer           data)
{
    DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    DataClientConnection *client_connection = data;
    DataClient *client = client_connection->client;
    DBusMessage *message;
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    
    message = dbus_message_new_method_call(client->id.bus_name, client->id.path,
                                           HIPPO_DBUS_MODEL_CLIENT_INTERFACE, "Notify");
    
    dbus_message_iter_init_append(message, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "(ssba(ssyyyv))", &array_iter);

    add_resource_to_message(client, &array_iter,
                            client_connection->resource, client_connection->fetch,
                            FALSE,
                            TRUE, changed_properties);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    dbus_connection_send(connection, message, NULL);

    /* FIXME: We should catch errors, and kick the client connection on error */

    dbus_message_unref(message);
}

static guint
data_client_id_hash(const DataClientId *id)
{
    return 13 * g_str_hash(id->bus_name) + 17 * g_str_hash(id->path);
}

static gboolean
data_client_id_equal(const DataClientId *a,
                     const DataClientId *b)
{
    return strcmp(a->bus_name, b->bus_name) == 0 && strcmp(a->path, b->path) == 0;
}

static DataClientConnection *
data_client_connection_new(DataClient        *client,
                           DDMDataResource *resource)
{
    DataClientConnection *connection = g_new0(DataClientConnection, 1);

    connection->client = client;
    connection->resource = resource;
    connection->fetch = NULL;

    ddm_data_resource_connect(connection->resource, NULL,
                                on_resource_changed, connection);

    return connection;
}

static void
data_client_connection_set_fetch(DataClientConnection *connection,
                                 DDMDataFetch       *fetch)
{
    if (fetch)
        ddm_data_fetch_ref(fetch);
    
    if (connection->fetch)
        ddm_data_fetch_unref(connection->fetch);

    connection->fetch = fetch;
}

static void
data_client_connection_destroy(DataClientConnection *connection)
{
    ddm_data_resource_disconnect(connection->resource,
                                   on_resource_changed, connection);
    
    ddm_data_fetch_unref(connection->fetch);
    g_free(connection);
}

static DataClientQueryClosure *
data_client_query_closure_new(DataClient     *client,
                              DBusMessage    *message,
                              DDMDataFetch *fetch)
{
    DataClientQueryClosure *closure = g_new0(DataClientQueryClosure, 1);
    closure->client = data_client_ref(client);
    closure->message = dbus_message_ref(message);
    closure->fetch = ddm_data_fetch_ref(fetch);

    return closure;
}

static void
data_client_query_closure_destroy(DataClientQueryClosure *closure)
{
    data_client_unref(closure->client);
    ddm_data_fetch_unref(closure->fetch);
    dbus_message_unref(closure->message);
    g_free(closure);
}

static DataClient *
data_client_new(DataClientId *id)
{
    DataClient *client = g_new(DataClient, 1);

    client->ref_count = 1;
    
    client->id.bus_name = g_strdup(id->bus_name);
    client->id.path = g_strdup(id->path);
    client->connections = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                NULL, (GDestroyNotify)data_client_connection_destroy);

    client->disconnected = FALSE;

    hippo_dbus_watch_for_disconnect(hippo_app_get_dbus(hippo_get_app()),
                                    client->id.bus_name);

    return client;
}

static DataClient *
data_client_ref(DataClient *client)
{
    client->ref_count++;

    return client;
}

static void
data_client_unref(DataClient *client)
{
    client->ref_count--;

    if (client->ref_count == 0) {
        hippo_dbus_unwatch_for_disconnect(hippo_app_get_dbus(hippo_get_app()),
                                          client->id.bus_name);

        g_free(client->id.bus_name);
        g_free(client->id.path);
        g_hash_table_destroy(client->connections);
        g_free(client);
    }
}

static void
data_client_map_destroy(DataClientMap *map)
{
    g_hash_table_destroy(map->clients);
    g_free(map);
}

static DataClientMap *
data_client_map_get(HippoDataCache *cache)
{
    DataClientMap *map = g_object_get_data(G_OBJECT(cache), "hippo-client-map");
    if (map == NULL) {
        map = g_new0(DataClientMap, 1);
        map->clients = g_hash_table_new_full((GHashFunc)data_client_id_hash, (GEqualFunc)data_client_id_equal,
                                             NULL, (GDestroyNotify)data_client_unref);
        g_object_set_data_full(G_OBJECT(cache), "hippo-client-map", map, (GDestroyNotify)data_client_map_destroy);
    }

    return map;
}

static DataClient *
data_client_map_get_client(DataClientMap *map,
                           const char    *bus_name,
                           const char    *path)
{
    DataClient *client;
    DataClientId id;
    id.bus_name = (char *)bus_name;
    id.path = (char *)path;

    client = g_hash_table_lookup(map->clients, &id);
    if (client == NULL) {
        client = data_client_new(&id);
        g_hash_table_insert(map->clients, &client->id, client);
    }

    return client;
}

static GHashTable *
read_params_dictionary(DBusMessageIter *iter)
{
    DBusMessageIter array_iter;
    DBusMessageIter dict_entry_iter;
    GHashTable *params = g_hash_table_new(g_str_hash, g_str_equal);

    if (dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_ARRAY)
        goto error;
    dbus_message_iter_recurse(iter, &array_iter);

    while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
        const char *name;
        const char *value;
            
        if (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_DICT_ENTRY)
            goto error;
        dbus_message_iter_recurse(&array_iter, &dict_entry_iter);

        if (dbus_message_iter_get_arg_type(&dict_entry_iter) != DBUS_TYPE_STRING)
            goto error;
        dbus_message_iter_get_basic(&dict_entry_iter, &name);
        dbus_message_iter_next(&dict_entry_iter);
        
        if (dbus_message_iter_get_arg_type(&dict_entry_iter) != DBUS_TYPE_STRING) {
            goto error;
        }
        
        dbus_message_iter_get_basic(&dict_entry_iter, &value);
        dbus_message_iter_next(&dict_entry_iter);

        g_hash_table_insert(params, (char *)name, (char *)value);

        dbus_message_iter_next (&array_iter);
    }

    return params;

 error:
    g_hash_table_destroy(params);
    return NULL;
}

static void
add_property_value_to_message(DBusMessageIter      *property_array_iter,
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
add_property_children_to_message(DataClient        *client,
                                 DBusMessageIter   *resource_array_iter,
                                 DDMDataProperty *property,
                                 DDMDataFetch    *children)
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
add_resource_to_message(DataClient        *client,
                        DBusMessageIter   *resource_array_iter,
                        DDMDataResource *resource,
                        DDMDataFetch    *fetch,
                        gboolean           indirect,
                        gboolean           is_notification,
                        GSList            *changed_properties)
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

static void
on_query_success(GSList  *results,
                 gpointer data)
{
    DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
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

    dbus_connection_send(connection, reply, NULL);
    dbus_message_unref(reply);
    
    data_client_query_closure_destroy(closure);
}

static void
on_query_error(DDMDataError  error,
               const char     *message,
               gpointer        data)
{
    DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    DataClientQueryClosure *closure = data;
    DBusMessage *reply;

    reply = dbus_message_new_error(closure->message,
                                   DBUS_ERROR_FAILED,
                                   message);
    dbus_connection_send(connection, reply, NULL);
    dbus_message_unref(reply);
    
    data_client_query_closure_destroy(closure);
}

static DBusMessage*
handle_query (void            *object,
              DBusMessage     *message,
              DBusError       *error)
{
    HippoDataCache *cache;
    DBusConnection *connection;
    DDMDataModel *model;
    const char *notification_path;
    const char *method_uri;
    const char *fetch_string;
    DDMDataFetch *fetch;
    GHashTable *params = NULL;
    DBusMessageIter iter;
    DDMDataQuery *query;
    DataClientMap *client_map;
    DataClient *client;
    DataClientQueryClosure *closure;
    
    connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    cache = hippo_app_get_data_cache(hippo_get_app());
    model = hippo_data_cache_get_model(cache);

    dbus_message_iter_init (message, &iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_OBJECT_PATH) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("First argument should be an object path (notification_path)"));
    }
    dbus_message_iter_get_basic(&iter, &notification_path);
    dbus_message_iter_next (&iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Second argument should be a string (method_uri)"));
    }
    dbus_message_iter_get_basic(&iter, &method_uri);
    dbus_message_iter_next (&iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Third argument should be a string (fetch_string)"));
    }
    dbus_message_iter_get_basic(&iter, &fetch_string);
    dbus_message_iter_next (&iter);

    params = read_params_dictionary(&iter);
    if (params == NULL)
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Fourth Argument should be a dictionary string=>string (params)"));

    if (dbus_message_iter_has_next(&iter))
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Too many arguments"));

    fetch = ddm_data_fetch_from_string(fetch_string);
    if (fetch == NULL) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Couldn't parse fetch string"));
    }
        
    client_map = data_client_map_get(cache);
    client = data_client_map_get_client(client_map, dbus_message_get_sender(message), notification_path);

    closure = data_client_query_closure_new(client, message, fetch);

    /* We short-circuit m:getResource requests for resources with the local
     * online-desktop scheme and handle them against the current contents of the cache.
     */
    if (strcmp(method_uri, "http://mugshot.org/p/system#getResource") == 0) {
        const char *resource_id = g_hash_table_lookup(params, "resourceId");
        if (resource_id == NULL) {
            data_client_query_closure_destroy(closure);
            return dbus_message_new_error(message,
                                          DBUS_ERROR_INVALID_ARGS,
                                          _("resourceId parameter is mandatory for m:getResource query"));
        }

        if (g_str_has_prefix(resource_id, "online-desktop:")) {
            DDMDataResource *resource = ddm_data_model_lookup_resource(model, resource_id);
            GSList *results;

            if (resource == NULL) {
                data_client_query_closure_destroy(closure);
                return dbus_message_new_error(message,
                                              DBUS_ERROR_FAILED,
                                              _("Couldn't find local resource"));
            }

            results = g_slist_prepend(NULL, resource);
            on_query_success(results, closure);
            g_slist_free(results);

            return NULL;
        }
    }

    query = ddm_data_model_query_params(model, method_uri, fetch_string, params);
    g_hash_table_destroy(params);
    
    if (query == NULL) {
        data_client_query_closure_destroy(closure);
        return dbus_message_new_error(message,
                                      DBUS_ERROR_FAILED,
                                      _("Couldn't send query"));
    }

    ddm_data_query_set_multi_handler(query, on_query_success, closure);
    ddm_data_query_set_error_handler(query, on_query_error, closure);

    ddm_data_fetch_unref(fetch);

    return NULL;
}

static DBusMessage*
handle_update (void            *object,
              DBusMessage     *message,
              DBusError       *error)
{
    HippoDataCache *cache;
    DBusConnection *connection;
    const char *method_uri;
    GHashTable *params = NULL;
    DBusMessageIter iter;
    
    connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    cache = hippo_app_get_data_cache(hippo_get_app());

    dbus_message_iter_init (message, &iter);
    
    dbus_message_iter_get_basic(&iter, &method_uri);
    dbus_message_iter_next (&iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("First argument should be a string (method_uri)"));
    }
    dbus_message_iter_get_basic(&iter, &method_uri);
    dbus_message_iter_next (&iter);

    params = read_params_dictionary(&iter);
    if (params == NULL)
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Second argument should be a dictionary string=>string (params)"));

    if (dbus_message_iter_has_next(&iter))
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Too many arguments"));

    /* Do the update */

    g_hash_table_destroy(params);

    return NULL;
}

static DBusMessage*
handle_forget (void            *object,
               DBusMessage     *message,
               DBusError       *error)
{
    HippoDataCache *cache;
    DBusConnection *connection;
    const char *notification_path;
    const char *resource_id;
    DBusMessageIter iter;
    
    connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    cache = hippo_app_get_data_cache(hippo_get_app());

    dbus_message_iter_init (message, &iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_OBJECT_PATH) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("First argument should be an object path (notification_path)"));
    }
    dbus_message_iter_get_basic(&iter, &notification_path);
    dbus_message_iter_next (&iter);

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Second argument should be a string (resource_id)"));
    }
    dbus_message_iter_get_basic(&iter, &resource_id);
    dbus_message_iter_next (&iter);

    if (dbus_message_iter_has_next(&iter))
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Too many arguments"));

    /* FIXME: Do the forget */

    return NULL;
}

static dbus_bool_t
handle_get_connected(void            *object,
                     const char      *prop_name,
                     DBusMessageIter *append_iter,
                     DBusError       *error)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    
    dbus_bool_t connected = hippo_connection_get_connected(hippo_connection);

    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_BOOLEAN, &connected);

    return TRUE;
}

static char *
get_self_id()
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    const char *self_resource_id = hippo_connection_get_self_resource_id(hippo_connection);

    if (self_resource_id == NULL) {
        return g_strdup("");
    } else {
        return g_strdup(self_resource_id);
    }
}

static dbus_bool_t
handle_get_self_id(void            *object,
                   const char      *prop_name,
                   DBusMessageIter *append_iter,
                   DBusError       *error)
{
    char *resource_id = get_self_id();
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &resource_id);

    g_free(resource_id);

    return TRUE;
}

static dbus_bool_t
handle_get_server(void            *object,
                  const char      *prop_name,
                  DBusMessageIter *append_iter,
                  DBusError       *error)
{
    char *server;
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    HippoPlatform *platform = hippo_connection_get_platform(hippo_connection);

    /* DESKTOP hardcoded here since this is the data model API, nothing to do with stacker */    
    server = hippo_platform_get_web_server(platform,
                                           HIPPO_SERVER_DESKTOP);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &server);
    
    g_free(server);
    
    return TRUE;
}

static dbus_bool_t
handle_get_web_base_url(void            *object,
                        const char      *prop_name,
                        DBusMessageIter *append_iter,
                        DBusError       *error)
{
    char *server;
    char *url;
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    HippoPlatform *platform = hippo_connection_get_platform(hippo_connection);

    /* DESKTOP hardcoded here since this is the data model API, nothing to do with stacker */
    server = hippo_platform_get_web_server(platform, HIPPO_SERVER_DESKTOP);

    /* Note, no trailing '/', don't add one here because relative urls are given
     * starting with '/' and so you want to be able to do baseurl + relativeurl
     */
    url = g_strdup_printf("http://%s", server);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &url);
    
    g_free(server);
    g_free(url);
    
    return TRUE;
}

static const HippoDBusMember model_members[] = {
    /* Query: Send a query to the server
     *
     * Parameters:
     *   Object path for notification callbacks
     *   Method name (URL with fragment)
     *   Fetch string
     *   Array of parameters
     *     Parameter name
     *     Parameter value
     *
     * Reply:
     *   Array of resources:
     *     Resource ID
     *     Resource class ID
     *     Indirect? (boolean)
     *     Array of parameters:
     *        Parameter ID namespace uri
     *        Parameter ID local name
     *        Update type ('a'=add, 'r'=replace, 'd'=delete, 'c'=clear)
     *        Data type ('s'=string, 'r'=resource)
     *        Cardinality ('.'=1, '?'=01, '*'=N)
     *        Value (variant)
     */
    { HIPPO_DBUS_MEMBER_METHOD, "Query", "ossa{ss}", "a(ssba(ssyyyv))", handle_query },

    /* Update: Send an update request to the server
     *
     * Parameters:
     *   Method name (URL with fragment)
     *   Array of parameters
     *     Parameter name
     *     Parameter value
     */

    { HIPPO_DBUS_MEMBER_METHOD, "Update", "sa(ss)", "", handle_update },
    
    /* Forget: Remove notifications resulting from an earlier Query request
     * 
     * Parameters:
     *   Object path passed to earlier Query calls
     *   Resource ID to forget notifications on
     */
    { HIPPO_DBUS_MEMBER_METHOD, "Forget", "os", "", handle_forget },

    /* ConnectedChanged: connected status changed (this signal will be replaced
     * eventually by moving self id and the online flag into global resource)
     * 
     * Parameters:
     *  boolean connected = true/false
     *  string selfId
     * 
     */
    { HIPPO_DBUS_MEMBER_SIGNAL, "ConnectedChanged", "", "bs", NULL },
    
    { 0, NULL }
};

static const HippoDBusProperty model_properties[] = {
    { "Connected",  "b", handle_get_connected, NULL },
    { "SelfId",     "s", handle_get_self_id, NULL },

    /* this should return the server we are encoding in the dbus bus name, like foo.bar.org:8080 */
    { "Server",     "s", handle_get_server, NULL },

    /* right now this will be the server with http:// in front, but
     * in theory could have https or a path on the host or
     * something. The url should NOT have a trailing '/', though
     * the one returned by the old "org.mugshot.Mugshot" API does.
     */
    
    { "WebBaseUrl", "s", handle_get_web_base_url, NULL },
    { NULL }
};

void
hippo_dbus_init_model(DBusConnection *connection)
{
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_MODEL_INTERFACE,
                                         model_members, model_properties);
    
    hippo_dbus_helper_register_object(connection, HIPPO_DBUS_MODEL_PATH,
                                      NULL, HIPPO_DBUS_MODEL_INTERFACE,
                                      NULL);
}

static gboolean
name_gone_foreach(gpointer key,
                  gpointer value,
                  gpointer data)
{
    DataClient *client = value;
    const char *name = data;

    if (strcmp(client->id.bus_name, name) == 0) {
        client->disconnected = TRUE;
        return TRUE;
    } else {
        return FALSE;
    }
}

void
hippo_dbus_model_name_gone(const char *name)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    DataClientMap *client_map = data_client_map_get(cache);

    g_hash_table_foreach_remove(client_map->clients, name_gone_foreach, (gpointer)name);
}

void
hippo_dbus_model_notify_connected_changed(gboolean connected)
{
    DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));

    if (connected) {
        char *resource_id = get_self_id();

        hippo_dbus_helper_emit_signal(connection,
                                      HIPPO_DBUS_MODEL_PATH, HIPPO_DBUS_MODEL_INTERFACE,
                                      "ConnectedChanged",
                                      DBUS_TYPE_BOOLEAN, &connected,
                                      DBUS_TYPE_STRING, &resource_id,
                                      DBUS_TYPE_INVALID);

        g_free(resource_id);
    } else {
        const char *empty_string;

        empty_string = "";
        
        hippo_dbus_helper_emit_signal(connection,
                                      HIPPO_DBUS_MODEL_PATH, HIPPO_DBUS_MODEL_INTERFACE,
                                      "ConnectedChanged",
                                      DBUS_TYPE_BOOLEAN, &connected,
                                      DBUS_TYPE_STRING, &empty_string,
                                      DBUS_TYPE_INVALID);
    }
}
