/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-internal.h"
#include "ddm-data-resource-internal.h"
#include "ddm-data-query-internal.h"
#include "ddm-connection.h"

static void      ddm_data_model_init                (DDMDataModel       *model);
static void      ddm_data_model_class_init          (DDMDataModelClass  *klass);

static void      ddm_data_model_dispose             (GObject              *object);
static void      ddm_data_model_finalize            (GObject              *object);

struct _DDMDataModel {
    GObject parent;

    DDMDataCache *data_cache;
    DDMDiskCache *disk_cache;
    GHashTable *resources;
};

struct _DDMDataModelClass {
    GObjectClass parent_class;
};

enum {
    CONNECTED,
    DISCONNECTED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(DDMDataModel, ddm_data_model, G_TYPE_OBJECT);

static void
ddm_data_model_init(DDMDataModel *model)
{
    model->resources = g_hash_table_new(g_str_hash, g_str_equal);
}

static void
ddm_data_model_class_init(DDMDataModelClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = ddm_data_model_dispose;
    object_class->finalize = ddm_data_model_finalize;

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
ddm_data_model_dispose(GObject *object)
{
#if 0
    DDMDataModel *model = DDM_DATA_MODEL(object);
#endif

    G_OBJECT_CLASS(ddm_data_model_parent_class)->dispose(object);
}

static void
ddm_data_model_finalize(GObject *object)
{
#if 0
    DDMDataModel *model = DDM_DATA_MODEL(object);
#endif

    G_OBJECT_CLASS(ddm_data_model_parent_class)->finalize(object);
}

static void
open_disk_cache(DDMDataModel *model)
{
    model->disk_cache = _ddm_disk_cache_new(model->data_cache);
}

static void
close_disk_cache(DDMDataModel *model)
{
    if (model->disk_cache) {
        _ddm_disk_cache_close(model->disk_cache);
        g_object_unref(model->disk_cache);
        model->disk_cache = NULL;
    }
}

static void
on_connection_has_auth_changed(DDMConnection *connection,
                               DDMDataModel  *model)
{
    gboolean has_auth = ddm_connection_get_has_auth(connection);

    /* A change in auth normally comes as a !has_auth / has_auth pair,
     * though this isn't enforced in DDMConnection; for now, don't
     * worry about the possibility of a change to the username without
     * forgetting the auth in between.
     */
    if (has_auth && !model->disk_cache)
        open_disk_cache(model);
    else if (!has_auth && model->disk_cache)
        close_disk_cache(model);
}

DDMDataModel *
_ddm_data_model_new(DDMDataCache *cache)
{
    DDMDataModel *model = g_object_new(DDM_TYPE_DATA_MODEL, NULL);
    DDMConnection *connection = ddm_data_cache_get_connection(cache);

    model->data_cache = cache;

    g_signal_connect(connection, "has-auth-changed",
                     G_CALLBACK(on_connection_has_auth_changed), model);

    open_disk_cache(model);

    return model;
}

DDMDiskCache *
_ddm_data_model_get_disk_cache(DDMDataModel *model)
{
    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), NULL);

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
    DDMDataQuery *query = data;
    DDMDataModel *model = ddm_data_query_get_model(data);

    _ddm_disk_cache_do_query(model->disk_cache, query);

    return FALSE;
}

static void
queue_offline_query(DDMDataModel *model,
                    DDMDataQuery *query)
{
    g_idle_add(do_offline_query, query);
}

DDMDataQuery *
ddm_data_model_query_params(DDMDataModel *model,
                            const char     *method,
                            const char     *fetch,
                            GHashTable     *params)
{
    DDMConnection *connection;
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    connection = ddm_data_cache_get_connection(model->data_cache);

    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL)
        return NULL;

    query = _ddm_data_query_new(model, method_qname, fetch, params);

    if (ddm_connection_get_connected(connection))
        ddm_connection_send_query(connection, query);
    else
        queue_offline_query(model, query);

    return query;
}

DDMDataQuery *
ddm_data_model_query(DDMDataModel *model,
                     const char     *method,
                     const char     *fetch,
                     ...)
{
    DDMDataQuery *query;
    GHashTable *params;
    va_list vap;

    va_start(vap, fetch);
    params = params_from_valist(vap);
    va_end(vap);

    query = ddm_data_model_query_params(model, method, fetch, params);

    g_hash_table_destroy(params);

    return query;
}

DDMDataQuery *
ddm_data_model_query_resource(DDMDataModel *model,
                              const char     *resource_id,
                              const char     *fetch)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    return ddm_data_model_query(model, "http://mugshot.org/p/system#getResource", fetch,
                                "resourceId", resource_id,
                                NULL);
}

DDMDataQuery *
ddm_data_model_update_params(DDMDataModel *model,
                             const char     *method,
                             GHashTable     *params)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    g_warning("%s is not implemented", G_STRFUNC);

    return NULL;
}

DDMDataQuery *
ddm_data_model_update(DDMDataModel *model,
                      const char     *method,
                      ...)
{
    DDMDataQuery *query;
    GHashTable *params;
    va_list vap;

    va_start(vap, method);
    params = params_from_valist(vap);
    va_end(vap);

    query = ddm_data_model_update_params(model, method, params);

    g_hash_table_destroy(params);

    return query;
}

DDMDataResource *
_ddm_data_model_get_resource(DDMDataModel *model,
                             const char     *resource_id)
{
    return g_hash_table_lookup(model->resources, resource_id);
}

DDMDataResource *
_ddm_data_model_ensure_resource(DDMDataModel *model,
                                const char     *resource_id,
                                const char     *class_id)
{
    DDMDataResource *resource;

    resource = g_hash_table_lookup(model->resources, resource_id);
    if (resource == NULL) {
        resource = _ddm_data_resource_new(resource_id, class_id);
        g_hash_table_insert(model->resources, (char *)ddm_data_resource_get_resource_id(resource), resource);
    }

    return resource;
}

gboolean
_ddm_data_parse_type(const char           *type_string,
                     DDMDataType        *type,
                     DDMDataCardinality *cardinality,
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
        *type = DDM_DATA_BOOLEAN;
        break;
    case 'i':
        *type = DDM_DATA_INTEGER;
        break;
    case 'l':
        *type = DDM_DATA_LONG;
        break;
    case 'f':
        *type = DDM_DATA_FLOAT;
        break;
    case 's':
        *type = DDM_DATA_STRING;
        break;
    case 'r':
        *type = DDM_DATA_RESOURCE;
        break;
    case 'u':
        *type = DDM_DATA_URL;
        break;
    default:
        g_warning("Can't understand type string '%s'", type_string);
        return FALSE;
    }

    p++;

    switch (*p) {
    case '*':
        *cardinality = DDM_DATA_CARDINALITY_N;
        p++;
        break;
    case '?':
        *cardinality = DDM_DATA_CARDINALITY_01;
        p++;
        break;
    case '\0':
        *cardinality = DDM_DATA_CARDINALITY_1;
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

