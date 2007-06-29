/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <stdarg.h>
#include <string.h>

#include "hippo-data-model-internal.h"
#include "hippo-data-resource-internal.h"
#include "hippo-data-query-internal.h"
#include "hippo-connection.h"

static void      hippo_data_model_init                (HippoDataModel       *model);
static void      hippo_data_model_class_init          (HippoDataModelClass  *klass);

static void      hippo_data_model_dispose             (GObject              *object);
static void      hippo_data_model_finalize            (GObject              *object);

struct _HippoDataModel {
    GObject parent;

    HippoDataCache *cache;
    GHashTable *resources;
};

struct _HippoDataModelClass {
    GObjectClass parent_class;
};

enum {
    CONNECTED,
    DISCONNECTED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(HippoDataModel, hippo_data_model, G_TYPE_OBJECT);

static void
hippo_data_model_init(HippoDataModel *model)
{
    model->resources = g_hash_table_new(g_str_hash, g_str_equal);
}

static void
hippo_data_model_class_init(HippoDataModelClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = hippo_data_model_dispose;
    object_class->finalize = hippo_data_model_finalize;

    signals[CONNECTED] =
        g_signal_new ("connected",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
    
    signals[DISCONNECTED] =
        g_signal_new ("disconnected",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
hippo_data_model_dispose(GObject *object)
{
#if 0
    HippoDataModel *model = HIPPO_DATA_MODEL(object);
#endif
    
    G_OBJECT_CLASS(hippo_data_model_parent_class)->dispose(object);
}

static void
hippo_data_model_finalize(GObject *object)
{
#if 0
    HippoDataModel *model = HIPPO_DATA_MODEL(object);
#endif    

    G_OBJECT_CLASS(hippo_data_model_parent_class)->finalize(object);
}

HippoDataModel *
_hippo_data_model_new(HippoDataCache *cache)
{
    HippoDataModel *model = g_object_new(HIPPO_TYPE_DATA_MODEL, NULL);

    model->cache = cache;

    return model;
}

static GHashTable *
params_from_valist(va_list vap)
{
    GHashTable *params = g_hash_table_new(g_str_hash, g_str_equal);
    
    while (TRUE) {
        const char *param_name = va_arg(vap, const char *);
        const char *param_value;
        if (param_name == NULL)
            break;
        
        param_value = va_arg(vap, const char *);

        g_hash_table_insert(params, (char *)param_name, (char *)param_value);
    }

    return params;
}

HippoDataQuery *
hippo_data_model_query_params(HippoDataModel *model,
                              const char     *method,
                              const char     *fetch,
                              GHashTable     *params)
{
    HippoConnection *connection;
    HippoDataQuery *query;
    const char *hash;
    const char *method_name;
    char *method_uri;
    HippoQName *method_qname;
    
    g_return_val_if_fail (HIPPO_IS_DATA_MODEL(model), NULL);

    connection = hippo_data_cache_get_connection(model->cache);
    
    hash = strchr(method, '#');
    if (hash == NULL) {
        g_warning("Query method URL %s doesn't have a fragment", method);
        return NULL;
    }

    method_name = hash + 1;
    method_uri = g_strndup(method, hash - method);
    method_qname = hippo_qname_get(method_uri, method_name);
    g_free(method_uri);
    
    query = _hippo_data_query_new(method_qname);

    hippo_connection_send_query(connection, query, fetch, params);

    return query;
}

HippoDataQuery *
hippo_data_model_query(HippoDataModel *model,
                       const char     *method,
                       const char     *fetch,
                       ...)
{
    HippoDataQuery *query;
    GHashTable *params;
    va_list vap;
    
    va_start(vap, fetch);
    params = params_from_valist(vap);
    va_end(vap);

    query = hippo_data_model_query_params(model, method, fetch, params);

    g_hash_table_destroy(params);

    return query;
}

HippoDataQuery *
hippo_data_model_query_resource(HippoDataModel *model,
                                const char     *resource_id,
                                const char     *fetch)
{
    g_return_val_if_fail (HIPPO_IS_DATA_MODEL(model), NULL);
    
    return hippo_data_model_query(model, "http://mugshot.org/p/system#getResource", fetch,
                                  "resourceId", resource_id,
                                  NULL);
}

HippoDataQuery *
hippo_data_model_update_params(HippoDataModel *model,
                               const char     *method,
                               GHashTable     *params)
{
    g_return_val_if_fail (HIPPO_IS_DATA_MODEL(model), NULL);

    g_warning("%s is not implemented", G_STRFUNC);

    return NULL;
}

HippoDataQuery *
hippo_data_model_update(HippoDataModel *model,
                        const char     *method,
                        ...)
{
    HippoDataQuery *query;
    GHashTable *params;
    va_list vap;
    
    va_start(vap, method);
    params = params_from_valist(vap);
    va_end(vap);

    query = hippo_data_model_update_params(model, method, params);

    g_hash_table_destroy(params);

    return query;
}

HippoDataResource *
_hippo_data_model_get_resource(HippoDataModel *model,
                               const char     *resource_id)
{
    return g_hash_table_lookup(model->resources, resource_id);
}

HippoDataResource *
_hippo_data_model_ensure_resource(HippoDataModel *model,
                                  const char     *resource_id,
                                  const char     *class_id)
{
    HippoDataResource *resource;

    resource = g_hash_table_lookup(model->resources, resource_id);
    if (resource == NULL) {
        resource = _hippo_data_resource_new(resource_id, class_id);
        g_hash_table_insert(model->resources, (char *)hippo_data_resource_get_resource_id(resource), resource);
    }

    return resource;
}
