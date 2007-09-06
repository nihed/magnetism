/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-data-model-backend.h"
#include "hippo-data-cache.h"
#include "hippo-disk-cache.h"

typedef struct {
    DDMDataModel   *ddm_model;
    HippoDataCache *data_cache;
    HippoDiskCache *disk_cache;
    
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
on_connection_connected_changed(HippoConnection *connection,
                                gboolean         connected,
                                HippoModel      *hippo_model)
{   
    DDMDataResource *global_resource;
    const char *self_id;
    DDMQName *self_id_prop;
    DDMDataValue value;
    
    global_resource = ddm_data_model_ensure_resource(hippo_model->ddm_model,
                                                     DDM_GLOBAL_RESOURCE, DDM_GLOBAL_RESOURCE_CLASS);
    if (connected) {
        self_id = hippo_connection_get_self_resource_id(connection);
    } else {
        self_id = NULL;
    }

    self_id_prop = ddm_qname_get(DDM_GLOBAL_RESOURCE_CLASS,
                                 "self");
    if (self_id) {
        value.type = DDM_DATA_RESOURCE;
        value.u.resource = ddm_data_model_ensure_resource(hippo_model->ddm_model,
                                                          self_id, "http://mugshot.org/p/o/user");
    } else {
        value.type = DDM_DATA_NONE;
    }

    if (ddm_data_resource_update_property(global_resource,
                                          self_id_prop,
                                          self_id ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_DELETE,
                                          DDM_DATA_CARDINALITY_1,
                                          FALSE, NULL,
                                          &value)) {
        GSList *changed_list;
        changed_list = g_slist_prepend(NULL, self_id_prop);
        ddm_data_resource_on_resource_change(global_resource, changed_list);
        g_slist_free(changed_list);
    }
    
    ddm_data_model_set_connected(hippo_model->ddm_model, connected);
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

    ddm_data_model_set_connected(hippo_model->ddm_model,
                                 hippo_connection_get_connected(connection));
    
    g_signal_connect(connection, "connected-changed",
                     G_CALLBACK(on_connection_connected_changed), hippo_model);
    
    g_signal_connect(connection, "has-auth-changed",
                     G_CALLBACK(on_connection_has_auth_changed), hippo_model);
    
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

static void
hippo_send_update (DDMDataModel *ddm_model,
                   DDMDataQuery *query,
                   const char   *method,
                   GHashTable   *params,
                   void         *backend_data)
{
    HippoModel *hippo_model;

    hippo_model = get_hippo_model(ddm_model);
    
    g_warning("send_update not implemented");
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
