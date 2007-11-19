/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include <ddm/ddm.h>
#include "hippo-dbus-model.h"
#include "hippo-dbus-model-client.h"
#include "main.h"

/* FIXME it's probably a broken layering whenever we need
 * HippoDataCache in here, because in principle this file just proxies
 * the ddm.h API over dbus, which means we should only need the ddm.h
 * API.
 */

#include <hippo/hippo-data-cache.h>

typedef struct _DataClientId           DataClientId;
typedef struct _DataClientMap          DataClientMap;

struct _DataClientId {
    char *bus_name;
    char *path;
};

struct _DataClientMap {
    DDMDataModel *model;
    GHashTable *clients;
};

static void        on_ready    (DDMDataModel    *ddm_model,
                                void            *data);

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

static DataClientId *
data_client_id_copy(const DataClientId *other)
{
    DataClientId *id = g_new(DataClientId, 1);
    id->bus_name = g_strdup(other->bus_name);
    id->path = g_strdup(other->path);

    return id;
}

static void
data_client_id_free(DataClientId *id)
{
    g_free(id->bus_name);
    g_free(id->path);

    g_free(id);
}

static void
data_client_map_destroy(DataClientMap *map)
{
    g_hash_table_destroy(map->clients);
    g_free(map);
}

static DataClientMap *
data_client_map_get(DDMDataModel *ddm_model)
{
    DataClientMap *map = g_object_get_data(G_OBJECT(ddm_model), "hippo-client-map");
    if (map == NULL) {
        map = g_new0(DataClientMap, 1);
        map->model = ddm_model;
        map->clients = g_hash_table_new_full((GHashFunc)data_client_id_hash, (GEqualFunc)data_client_id_equal,
                                             (GDestroyNotify)data_client_id_free, (GDestroyNotify)g_object_unref);
        g_object_set_data_full(G_OBJECT(ddm_model), "hippo-client-map",
                               map, (GDestroyNotify)data_client_map_destroy);
    }

    return map;
}

static HippoDBusModelClient *
data_client_map_get_client(DataClientMap *map,
                           const char    *bus_name,
                           const char    *path)
{
    HippoDBusModelClient *client;
    DataClientId id;
    id.bus_name = (char *)bus_name;
    id.path = (char *)path;

    client = g_hash_table_lookup(map->clients, &id);
    if (client == NULL) {
        DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));

        client = hippo_dbus_model_client_new(connection, map->model, bus_name, path);
        g_hash_table_insert(map->clients, data_client_id_copy(&id), client);
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

static DBusMessage*
handle_query (void            *object,
              DBusMessage     *message,
              DBusError       *error)
{
    DDMDataModel *model;
    const char *notification_path;
    const char *method_uri;
    const char *fetch_string;
    DDMDataFetch *fetch;
    GHashTable *params = NULL;
    DBusMessageIter iter;
    DataClientMap *client_map;
    HippoDBusModelClient *client;
    
    model = hippo_app_get_data_model(hippo_get_app());

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
        
    client_map = data_client_map_get(model);
    client = data_client_map_get_client(client_map, dbus_message_get_sender(message), notification_path);

    if (!hippo_dbus_model_client_do_query(client, message, method_uri, fetch, params)) {
        /* We've already validated most arguments, so don't worry too much about getting a
         * good error message if something goes wrong at this point
         */
        return dbus_message_new_error(message,
                                      DBUS_ERROR_FAILED,
                                      _("Couldn't send query"));
    }

    g_hash_table_destroy(params);

    ddm_data_fetch_unref(fetch);

    return NULL;
}

static DBusMessage*
handle_update (void            *object,
               DBusMessage     *message,
               DBusError       *error)
{
    DDMDataModel *model;
    const char *method_uri;
    GHashTable *params = NULL;
    DBusMessageIter iter;
    
    model = hippo_app_get_data_model(hippo_get_app());

    dbus_message_iter_init (message, &iter);
    
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

    if (!hippo_dbus_model_client_do_update(model,
                                           hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app())),
                                           message, method_uri, params)) {
        /* We've already validated most arguments, so don't worry too much about getting a
         * good error message if something goes wrong at this point
         */
        return dbus_message_new_error(message,
                                      DBUS_ERROR_FAILED,
                                      _("Couldn't send update"));
    }

    g_hash_table_destroy(params);
    
    return NULL;
}

static DBusMessage*
handle_forget (void            *object,
               DBusMessage     *message,
               DBusError       *error)
{
    const char *notification_path;
    const char *resource_id;
    DBusMessageIter iter;
    
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

    /* FIXME: Do the forget; change the NULL return to an ACK if we aren't doing something async */

    return NULL;
}

static dbus_bool_t
handle_get_ready(void            *object,
                 const char      *prop_name,
                 DBusMessageIter *append_iter,
                 DBusError       *error)
{
    DDMDataModel *ddm_model;
    dbus_bool_t ready;
    
    ddm_model = hippo_app_get_data_model(hippo_get_app());

    ready = ddm_data_model_is_ready(ddm_model);

    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_BOOLEAN, &ready);

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

    { HIPPO_DBUS_MEMBER_METHOD, "Update", "sa{ss}", "", handle_update },
    
    /* Forget: Remove notifications resulting from an earlier Query request
     * 
     * Parameters:
     *   Object path passed to earlier Query calls
     *   Resource ID to forget notifications on
     */
    { HIPPO_DBUS_MEMBER_METHOD, "Forget", "os", "", handle_forget },

    /* Ready: Start fetching application data from the data model
     * 
     * Parameters:
     * 
     */
    { HIPPO_DBUS_MEMBER_SIGNAL, "Ready", "", "", NULL },
    
    { 0, NULL }
};

static const HippoDBusProperty model_properties[] = {
    { "Ready",  "b", handle_get_ready, NULL },

    /* this should return the server we are encoding in the dbus bus name, like foo.bar.org:8080 */
    { "Server",     "s", handle_get_server, NULL },

    { NULL }
};

void
hippo_dbus_init_model(DBusConnection *connection)
{
    DDMDataModel *ddm_model;
    
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_MODEL_INTERFACE,
                                         model_members, model_properties);
    
    hippo_dbus_helper_register_object(connection, HIPPO_DBUS_MODEL_PATH,
                                      NULL, HIPPO_DBUS_MODEL_INTERFACE,
                                      NULL);

    ddm_model = hippo_app_get_data_model(hippo_get_app());
    
    g_signal_connect(G_OBJECT(ddm_model), "ready",
                     G_CALLBACK(on_ready), connection);
}

static gboolean
name_gone_foreach(gpointer key,
                  gpointer value,
                  gpointer data)
{
    HippoDBusModelClient *client = value;
    const char *name = data;

    if (strcmp(hippo_dbus_model_client_get_bus_name(client), name) == 0) {
        hippo_dbus_model_client_disconnected(client);
        return TRUE;
    } else {
        return FALSE;
    }
}

void
hippo_dbus_model_name_gone(const char *name)
{
    DDMDataModel *model = hippo_app_get_data_model(hippo_get_app());
    DataClientMap *client_map = data_client_map_get(model);

    g_hash_table_foreach_remove(client_map->clients, name_gone_foreach, (gpointer)name);
}

static void
on_ready(DDMDataModel *ddm_model,
         void         *data)
{
    DBusConnection *connection = data;
    
    DataClientMap *map = data_client_map_get(ddm_model);

    /* Once a client receives Ready, it must forget its current
     * state and start over. It would be a little more efficent to just remove
     * all connections and leave the D-BUS disconnection-watches in place,
     * that might be important if we had dozens of clients.
     */
    g_hash_table_remove_all(map->clients);
        
    hippo_dbus_helper_emit_signal(connection,
                                  HIPPO_DBUS_MODEL_PATH, HIPPO_DBUS_MODEL_INTERFACE,
                                  "Ready",
                                  DBUS_TYPE_INVALID);
}
