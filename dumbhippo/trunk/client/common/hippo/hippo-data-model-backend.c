/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-data-model-backend.h"
#include "hippo-data-cache.h"
#include "hippo-disk-cache.h"
#include <string.h>

typedef struct {
    DDMDataModel   *ddm_model;
    HippoDataCache *data_cache;
    HippoDiskCache *disk_cache;
    DDMDataQuery   *self_query;
} HippoModel;

static HippoModel*
get_hippo_model(DDMDataModel *ddm_model)
{
    HippoModel *hippo_model = g_object_get_data(G_OBJECT(ddm_model), "hippo-data-model");

    return hippo_model;
}

HippoDataCache*
hippo_data_model_get_data_cache(DDMDataModel *ddm_model)
{
    HippoModel *hippo_model = get_hippo_model(ddm_model);

    return hippo_model->data_cache;
}

HippoDiskCache*
_hippo_data_model_get_disk_cache(DDMDataModel *ddm_model)
{
    HippoModel *hippo_model = get_hippo_model(ddm_model);

    return hippo_model->disk_cache;
}

static void
open_disk_cache(HippoModel *hippo_model)
{
    g_assert(hippo_model->disk_cache == NULL);
    hippo_model->disk_cache = _hippo_disk_cache_new(hippo_model->data_cache);
}

static void
close_disk_cache(HippoModel *hippo_model)
{
    if (hippo_model->disk_cache) {
        _hippo_disk_cache_close(hippo_model->disk_cache);
        g_object_unref(hippo_model->disk_cache);
        hippo_model->disk_cache = NULL;
    }
}

static void
on_connection_has_auth_changed(HippoConnection *connection,
                               HippoModel      *hippo_model)
{
    gboolean has_auth = hippo_connection_get_has_auth(connection);

    /* A change in auth normally comes as a !has_auth / has_auth pair,
     * though this isn't enforced in DDMConnection; for now, don't
     * worry about the possibility of a change to the username without
     * forgetting the auth in between.
     */
    if (has_auth && !hippo_model->disk_cache)
        open_disk_cache(hippo_model);
    else if (!has_auth && hippo_model->disk_cache)
        close_disk_cache(hippo_model);
}

static void
model_set_initial_properties(HippoModel *hippo_model)
{
    HippoConnection *connection = hippo_data_cache_get_connection(hippo_model->data_cache);
    HippoPlatform *platform = hippo_connection_get_platform(connection);
    DDMDataResource *global_resource;
    DDMDataResource *self_resource;
    const char *self_id;
    DDMDataValue value;
    char *web_server;
    char *web_base_url;
    char *fallback_user_photo_url;
    
    global_resource = ddm_data_model_ensure_local_resource(hippo_model->ddm_model,
                                                           DDM_GLOBAL_RESOURCE, DDM_GLOBAL_RESOURCE_CLASS);
    ddm_data_model_set_global_resource(hippo_model->ddm_model, global_resource);
                                       
    self_id = hippo_connection_get_self_resource_id(connection);
    if (self_id) {
        self_resource = ddm_data_model_ensure_resource(hippo_model->ddm_model,
                                                       self_id, "http://mugshot.org/p/o/user");
        value.type = DDM_DATA_RESOURCE;
        value.u.resource = self_resource;
    } else {
        self_resource = NULL;
        value.type = DDM_DATA_NONE;
    }
    
    ddm_data_model_set_self_resource(hippo_model->ddm_model, self_resource);

    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "self"),
                                      self_id ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_DELETE,
                                      DDM_DATA_CARDINALITY_01,
                                      FALSE, NULL,
                                      &value);
    
    /* DESKTOP hardcoded here since this is the data model API, nothing to do with stacker */
    web_server = hippo_platform_get_web_server(platform, HIPPO_SERVER_DESKTOP);
    /* Note, no trailing '/', don't add one here because relative urls are given
     * starting with '/' and so you want to be able to do baseurl + relativeurl
     */
    web_base_url = g_strdup_printf("http://%s", web_server);

    fallback_user_photo_url = g_strdup_printf("%s/images2/user_pix1/nophoto.png", web_base_url);
    
    value.type = DDM_DATA_STRING;
    value.u.string = web_base_url;

    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "webBaseUrl"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);

    /* This should eventually come from the server, but right now the server has no
     * global object so it would be a pain; putting a hack here to create the property
     * seems cleaner than hardcoding the no photo url all over BigBoard etc.
     */
    value.type = DDM_DATA_URL;
    value.u.string = fallback_user_photo_url;

    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "fallbackUserPhotoUrl"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);

    g_free(fallback_user_photo_url);
    g_free(web_server);
    g_free(web_base_url);

    value.type = DDM_DATA_BOOLEAN;
    value.u.boolean = hippo_connection_get_connected(connection);

    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "online"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);
    
    value.type = DDM_DATA_STRING;
    value.u.string = hippo_data_cache_get_client_info(hippo_model->data_cache)->ddm_protocol_version;
    if (value.u.string == NULL)
        value.u.string = "0";
    
    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "ddmProtocolVersion"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);    
}

static void
model_on_connected(HippoModel *hippo_model)
{
    /* We first "reset" - deleting all non-local resources from the model, and all property
     * values that reference non-local properties; then we add back the "self" property, which
     * references the local resource, *then* we signal that we are reconnected, so that the
     * self property is already there.
     */
    ddm_data_model_reset(hippo_model->ddm_model);

    model_set_initial_properties(hippo_model);
    
    ddm_data_model_signal_ready(hippo_model->ddm_model);
}

static void
model_on_disconnected(HippoModel *hippo_model)
{
    DDMDataResource *global_resource;
    DDMDataValue value;

    global_resource = ddm_data_model_ensure_local_resource(hippo_model->ddm_model,
                                                           DDM_GLOBAL_RESOURCE, DDM_GLOBAL_RESOURCE_CLASS);

    value.type = DDM_DATA_BOOLEAN;
    value.u.boolean = FALSE;

    ddm_data_resource_update_property(global_resource,
                                      ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS, "online"),
                                      DDM_DATA_UPDATE_REPLACE,
                                      DDM_DATA_CARDINALITY_1,
                                      FALSE, NULL,
                                      &value);
}
    
static void
on_connection_connected_changed(HippoConnection *connection,
                                gboolean         connected,
                                HippoModel      *hippo_model)
{
    if (connected)
        model_on_connected(hippo_model);
    else
        model_on_disconnected(hippo_model);
}

static void
on_connection_state_changed(HippoConnection *connection,
                            HippoModel      *hippo_model)
{
    /* When we start up, we want to wait to signal ready until either:
     *
     * - We manage to connect
     * - We fail to connect and are either retrying or waiting for user input
     *
     */
    
    switch (hippo_connection_get_state(connection)) {
    case HIPPO_STATE_SIGNED_OUT:
    case HIPPO_STATE_SIGN_IN_WAIT:
    case HIPPO_STATE_RETRYING:
    case HIPPO_STATE_AUTH_WAIT:
        if (!ddm_data_model_is_ready(hippo_model->ddm_model)) {
            model_set_initial_properties(hippo_model);
            ddm_data_model_signal_ready(hippo_model->ddm_model);
        }
        break;
        
    case HIPPO_STATE_CONNECTING:
    case HIPPO_STATE_REDIRECTING:
    case HIPPO_STATE_AUTHENTICATING:
    case HIPPO_STATE_AWAITING_CLIENT_INFO:
    case HIPPO_STATE_AUTHENTICATED:
        break;
    }
}

static void
hippo_add_model    (DDMDataModel *ddm_model,
                    void         *backend_data)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(backend_data);
    HippoConnection *connection;
    HippoModel *hippo_model;

    g_assert(get_hippo_model(ddm_model) == NULL);
    
    hippo_model = g_new0(HippoModel, 1);
    hippo_model->ddm_model = ddm_model;
    hippo_model->data_cache = cache;
    
    g_object_set_data(G_OBJECT(ddm_model), "hippo-data-model", hippo_model);

    connection = hippo_data_cache_get_connection(cache);

    g_signal_connect(connection, "connected-changed",
                     G_CALLBACK(on_connection_connected_changed), hippo_model);
    
    g_signal_connect(connection, "has-auth-changed",
                     G_CALLBACK(on_connection_has_auth_changed), hippo_model);
    
    g_signal_connect(connection, "state-changed",
                     G_CALLBACK(on_connection_state_changed), hippo_model);
    
    open_disk_cache(hippo_model);
}

static void
hippo_remove_model (DDMDataModel *ddm_model,
                    void         *backend_data)
{
    HippoModel *hippo_model;

    hippo_model = get_hippo_model(ddm_model);
    g_assert(hippo_model != NULL);
    
    g_object_set_data(G_OBJECT(ddm_model), "hippo-data-model", NULL);

    g_free(hippo_model);
}

static gboolean
do_offline_query(gpointer data)
{
    DDMDataQuery *query = data;
    DDMDataModel *ddm_model;
    HippoModel *hippo_model;

    ddm_model = ddm_data_query_get_model(data);
    if (ddm_model == NULL)
        return FALSE; /* in case model was nuked before getting to idle */
    
    hippo_model = get_hippo_model(ddm_model);

    if (hippo_model == NULL)
        return FALSE;  /* in case model was nuked before getting to idle */

    if (hippo_model->disk_cache == NULL) {
        ddm_data_query_error(query,
                             DDM_DATA_ERROR_INTERNAL,
                             "No connection and query is not cached");
        return FALSE;
    }

    _hippo_disk_cache_do_query(hippo_model->disk_cache, query);
    return FALSE;
}

static void
queue_offline_query(DDMDataModel *ddm_model,
                    DDMDataQuery *query)
{
    g_idle_add(do_offline_query, query);
}

static void
hippo_send_query   (DDMDataModel *ddm_model,
                    DDMDataQuery *query,
                    void         *backend_data)
{
    HippoDataCache *cache;
    HippoConnection *connection;    
    HippoModel *hippo_model;
    
    hippo_model = get_hippo_model(ddm_model);    

    cache = HIPPO_DATA_CACHE(backend_data);
    connection = hippo_data_cache_get_connection(cache);
    
    if (hippo_connection_get_connected(connection))
        hippo_connection_send_query(connection, query);
    else
        queue_offline_query(ddm_model, query);        
}

static gboolean
handle_local_update (HippoDataCache *cache,
                     DDMDataQuery   *query)
{
    DDMQName *qname = ddm_data_query_get_qname(query);
    if (!g_str_has_prefix(qname->uri, "online-desktop:"))
        return FALSE;

    if (strcmp(qname->uri, "online-desktop:/p/system") == 0) {
        GHashTable *params = ddm_data_query_get_params(query);
        HippoConnection *connection = hippo_data_cache_get_connection(cache);
        
        if (strcmp(qname->name, "openUrl") == 0) {
            const char *url = g_hash_table_lookup(params, "url");
            if (url == NULL) {
                ddm_data_query_error_async(query,
                                           DDM_DATA_ERROR_BAD_REQUEST,
                                           "'url' parameter missing for openUrl request");
                return TRUE;
            }

            hippo_connection_open_maybe_relative_url(connection, url);

            /* FIXME: signal success */
            
            return TRUE;
        }
    }

    ddm_data_query_error_async(query,
                               DDM_DATA_ERROR_BAD_REQUEST,
                               "Unknown local data model update");

    return TRUE;
}

static void
hippo_send_update (DDMDataModel *ddm_model,
                   DDMDataQuery *query,
                   void         *backend_data)
{
    HippoDataCache *cache;
    HippoConnection *connection;    
    HippoModel *hippo_model;
    
    hippo_model = get_hippo_model(ddm_model);    

    cache = HIPPO_DATA_CACHE(backend_data);

    if (handle_local_update(cache, query))
        return;
    
    connection = hippo_data_cache_get_connection(cache);
    if (hippo_connection_get_connected(connection))
        hippo_connection_send_query(connection, query);
    else
        ddm_data_query_error_async(query,
                                   DDM_DATA_ERROR_NO_CONNECTION,
                                   "Not connected to server");
}

static const DDMDataModelBackend hippo_backend = {
    hippo_add_model,
    hippo_remove_model,
    hippo_send_query,
    hippo_send_update,
    NULL,
};

const DDMDataModelBackend*
hippo_data_model_get_backend(void)
{
    return &hippo_backend;
}
