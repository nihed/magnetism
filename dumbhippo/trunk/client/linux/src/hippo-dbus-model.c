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

static void        on_connected_changed    (DDMDataModel    *ddm_model,
                                            gboolean         connected,
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

    if (!hippo_dbus_model_client_do_update(model, message, method_uri, params)) {
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
handle_get_connected(void            *object,
                     const char      *prop_name,
                     DBusMessageIter *append_iter,
                     DBusError       *error)
{
    DDMDataModel *ddm_model;
    dbus_bool_t connected;
    
    ddm_model = hippo_app_get_data_model(hippo_get_app());

    connected = ddm_data_model_get_connected(ddm_model);

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

    { HIPPO_DBUS_MEMBER_METHOD, "Update", "sa{ss}", "", handle_update },
    
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


    /* FIXME This is probably broken to have here, since it's just a special
     * case of something that should be in the data model anyway, right?
     */
    { "SelfId",     "s", handle_get_self_id, NULL },

    /* this should return the server we are encoding in the dbus bus name, like foo.bar.org:8080 */
    { "Server",     "s", handle_get_server, NULL },

    /* right now this will be the server with http:// in front, but
     * in theory could have https or a path on the host or
     * something. The url should NOT have a trailing '/', though
     * the one returned by the old "org.mugshot.Mugshot" API does.
     */

    /* All URLs in the data model have already been made absolute
     * on receipt from the server, so this is just useful if you want to construct
     * an URL like /account. FIXME: Such URL's perhaps should also be in the data model
     */
    { "WebBaseUrl", "s", handle_get_web_base_url, NULL },
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
    
    g_signal_connect(G_OBJECT(ddm_model), "connected-changed", G_CALLBACK(on_connected_changed),
                     connection);
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
on_connected_changed(DDMDataModel *ddm_model,
                     gboolean      connected,
                     void         *data)
{
    DBusConnection *connection = data;
    
    if (connected) {
        DataClientMap *map = data_client_map_get(ddm_model);
        char *resource_id = get_self_id();

        /* Once a client receives ConnectedChanged, it must forget its current
         * state and start over. It would be a little more efficent to just remove
         * all connections and leave the D-BUS disconnection-watches in place,
         * that might be important if we had dozens of clients.
         */
        g_hash_table_remove_all(map->clients);
        
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

        /* Including an empty string for the self-id might make you think that being disconnected
         * means that there is no longer a self-id. That's, however, wrong. The self_id is
         * *independent* of the connected state and can be used to fetch things out of the
         * offline cache.
         *
         * In fact, I think the whole existence of a ConnectedChanged signal is possibly a confusing
         * thing. There are two different events:
         *
         * Connected:
         * - The connected boolean is true
         * - All prior notification's you've registered have been removed
         * - All data you might be caching is invalid, dump it
         * - The selfId might have changed
         * - Start fetching your data from scratch with the assumption that everything has changed
         *
         * Disconnected:
         * - The connected boolean is false
         *
         * So while the two events *do* signal changes in the Connected boolean, they otherwise require
         * entirely different handling on the part of the recipient.
         *
         * This also means that even if the connected boolean moved in the global resource, you'd
         * still want the "Connected" signal.
         */
        hippo_dbus_helper_emit_signal(connection,
                                      HIPPO_DBUS_MODEL_PATH, HIPPO_DBUS_MODEL_INTERFACE,
                                      "ConnectedChanged",
                                      DBUS_TYPE_BOOLEAN, &connected,
                                      DBUS_TYPE_STRING, &empty_string,
                                      DBUS_TYPE_INVALID);
    }
}
