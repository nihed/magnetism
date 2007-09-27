/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-internal.h"
#include "ddm-data-model-backend.h"
#include "ddm-data-resource-internal.h"
#include "ddm-data-query-internal.h"

static void      ddm_data_model_init                (DDMDataModel       *model);
static void      ddm_data_model_class_init          (DDMDataModelClass  *klass);

static void      ddm_data_model_dispose             (GObject              *object);
static void      ddm_data_model_finalize            (GObject              *object);

struct _DDMDataModel {
    GObject parent;

    const DDMDataModelBackend *backend;
    void                      *backend_data;
    GFreeFunc                  free_backend_data_func;
    
    GHashTable *resources;

    guint connected : 1;
};

struct _DDMDataModelClass {
    GObjectClass parent_class;
};

enum {
    CONNECTED_CHANGED,
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

    signals[CONNECTED_CHANGED] =
        g_signal_new ("connected-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__BOOLEAN,
                      G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
}

static void
ddm_data_model_dispose(GObject *object)
{
    DDMDataModel *model = DDM_DATA_MODEL(object);

    if (model->backend != NULL) {
        model->backend->remove_model(model, model->backend_data);
        if (model->free_backend_data_func)
            (* model->free_backend_data_func) (model->backend_data);
        model->backend = NULL;
        model->backend_data = NULL;
        model->free_backend_data_func = NULL;
    }
    
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

DDMDataModel*
ddm_data_model_new_with_backend (const DDMDataModelBackend *backend,
                                 void                      *backend_data,
                                 GFreeFunc                  free_backend_data_func)
{
    DDMDataModel *model;

    g_return_val_if_fail(backend != NULL, NULL);
    
    model = g_object_new(DDM_TYPE_DATA_MODEL, NULL);

    model->backend = backend;
    model->backend_data = backend_data;
    model->free_backend_data_func = free_backend_data_func;

    model->backend->add_model (model, model->backend_data);
    
    return model;
}

gboolean
ddm_data_model_get_connected(DDMDataModel   *model)
{
    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), FALSE);

    return model->connected;
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

DDMDataQuery *
ddm_data_model_query_params(DDMDataModel *model,
                            const char   *method,
                            const char   *fetch,
                            GHashTable   *params)
{
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL)
        return NULL;

    query = _ddm_data_query_new(model, method_qname, fetch, params);

    model->backend->send_query(model, query, model->backend_data);
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
                             const char   *method,
                             GHashTable   *params)
{
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL) /* Invalid method URI */
        return NULL;

    query = _ddm_data_query_new_update(model, method_qname, params);
    if (query == NULL) /* Bad fetch string */
        return NULL;

    model->backend->send_update(model, query, model->backend_data);
    
    return query;
}

DDMDataQuery *
ddm_data_model_update(DDMDataModel *model,
                      const char   *method,
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
ddm_data_model_lookup_resource(DDMDataModel *model,
                               const char     *resource_id)
{
    return g_hash_table_lookup(model->resources, resource_id);
}

DDMDataResource *
ensure_resource_internal(DDMDataModel *model,
                         const char   *resource_id,
                         const char   *class_id,
                         gboolean      local)
{
    DDMDataResource *resource;

    local = local != FALSE;

    resource = g_hash_table_lookup(model->resources, resource_id);
    if (resource) {
        if ((local != FALSE) != ddm_data_resource_get_local(resource)) {
            g_warning("Mismatch for 'local' nature of resource '%s', old=%d, new=%d",
                      resource_id, !local, local);
        }

        if (class_id) {
            const char *old_class_id = ddm_data_resource_get_class_id(resource);
            if (old_class_id && strcmp(class_id, old_class_id) != 0)
                g_warning("Mismatch for class_id of resource '%s', old=%s, new=%s",
                          resource_id, old_class_id, class_id);
        }

    } else {
        resource = _ddm_data_resource_new(resource_id, class_id, local);
        g_hash_table_insert(model->resources, (char *)ddm_data_resource_get_resource_id(resource), resource);
    }

    return resource;
}

DDMDataResource *
ddm_data_model_ensure_resource(DDMDataModel *model,
                               const char   *resource_id,
                               const char   *class_id)
{
    ensure_resource_internal(model, resource_id, class_id, FALSE);
}

DDMDataResource *
ddm_data_model_ensure_local_resource(DDMDataModel *model,
                                     const char   *resource_id,
                                     const char   *class_id)
{
    ensure_resource_internal(model, resource_id, class_id, TRUE);
}

void
ddm_data_model_set_connected (DDMDataModel   *model,
                              gboolean        connected)
{
    connected = connected != FALSE;
    if (connected == model->connected)
        return;

    model->connected = connected;
    g_signal_emit(G_OBJECT(model), signals[CONNECTED_CHANGED], 0, connected);
}
