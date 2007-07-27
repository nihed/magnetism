/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

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

    HippoDataCache *data_cache;
    HippoDiskCache *disk_cache;
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

static void
open_disk_cache(HippoDataModel *model)
{
    model->disk_cache = _hippo_disk_cache_new(model->data_cache);
}

static void
close_disk_cache(HippoDataModel *model)
{
    if (model->disk_cache) {
        _hippo_disk_cache_close(model->disk_cache);
        g_object_unref(model->disk_cache);
        model->disk_cache = NULL;
    }
}

static void
on_connection_has_auth_changed(HippoConnection *connection,
                               HippoDataModel  *model)
{
    gboolean has_auth = hippo_connection_get_has_auth(connection);

    /* A change in auth normally comes as a !has_auth / has_auth pair,
     * though this isn't enforced in HippoConnection; for now, don't
     * worry about the possibility of a change to the username without
     * forgetting the auth in between.
     */
    if (has_auth && !model->disk_cache)
        open_disk_cache(model);
    else if (!has_auth && model->disk_cache)
        close_disk_cache(model);
}

HippoDataModel *
_hippo_data_model_new(HippoDataCache *cache)
{
    HippoDataModel *model = g_object_new(HIPPO_TYPE_DATA_MODEL, NULL);
    HippoConnection *connection = hippo_data_cache_get_connection(cache);

    model->data_cache = cache;

    g_signal_connect(connection, "has-auth-changed",
                     G_CALLBACK(on_connection_has_auth_changed), model);

    open_disk_cache(model);

    return model;
}

HippoDiskCache *
_hippo_data_model_get_disk_cache(HippoDataModel *model)
{
    g_return_val_if_fail(HIPPO_IS_DATA_MODEL(model), NULL);

    return model->disk_cache;
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

static gboolean
do_offline_query(gpointer data)
{
    HippoDataQuery *query = data;
    HippoDataModel *model = hippo_data_query_get_model(data);

    _hippo_disk_cache_do_query(model->disk_cache, query);
 
    return FALSE;
}

static void
queue_offline_query(HippoDataModel *model,
                    HippoDataQuery *query)
{
    g_idle_add(do_offline_query, query);
}

HippoDataQuery *
hippo_data_model_query_params(HippoDataModel *model,
                              const char     *method,
                              const char     *fetch,
                              GHashTable     *params)
{
    HippoConnection *connection;
    HippoDataQuery *query;
    HippoQName *method_qname;
    
    g_return_val_if_fail (HIPPO_IS_DATA_MODEL(model), NULL);

    connection = hippo_data_cache_get_connection(model->data_cache);

    method_qname = hippo_qname_from_uri(method);
    if (method_qname == NULL)
        return NULL;

    query = _hippo_data_query_new(model, method_qname, fetch, params);

    if (hippo_connection_get_connected(connection))
        hippo_connection_send_query(connection, query);
    else
        queue_offline_query(model, query);

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

gboolean
_hippo_data_parse_type(const char           *type_string,
                       HippoDataType        *type,
                       HippoDataCardinality *cardinality,
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
        *type = HIPPO_DATA_BOOLEAN;
        break;
    case 'i':
        *type = HIPPO_DATA_INTEGER;
        break;
    case 'l':
        *type = HIPPO_DATA_LONG;
        break;
    case 'f':
        *type = HIPPO_DATA_FLOAT;
        break;
    case 's':
        *type = HIPPO_DATA_STRING;
        break;
    case 'r':
        *type = HIPPO_DATA_RESOURCE;
        break;
    case 'u':
        *type = HIPPO_DATA_URL;
        break;
    default:
        g_warning("Can't understand type string '%s'", type_string);
        return FALSE;
    }
        
    p++;

    switch (*p) {
    case '*':
        *cardinality = HIPPO_DATA_CARDINALITY_N;
        p++;
        break;
    case '?':
        *cardinality = HIPPO_DATA_CARDINALITY_01;
        p++;
        break;
    case '\0':
        *cardinality = HIPPO_DATA_CARDINALITY_1;
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

