/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-internal.h"
#include "ddm-data-model-dbus.h"
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
    DDMDataModel *model = DDM_DATA_MODEL(object);

    if (model->backend != NULL) {
        model->backend->remove_model(model);
        model->backend = NULL;
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

static DDMDataModel *default_model = NULL;

DDMDataModel*
ddm_data_model_get_default (void)
{
    if (default_model == NULL) {
        default_model = ddm_data_model_new_with_backend(ddm_data_model_get_dbus_backend());
    }

    g_object_ref(default_model);
    return default_model;
}

DDMDataModel*
ddm_data_model_new_with_backend (const DDMDataModelBackend *backend)
{
    DDMDataModel *model;

    g_return_val_if_fail(backend != NULL, NULL);
    
    model = g_object_new(DDM_TYPE_DATA_MODEL, NULL);

    model->backend = backend;

    model->backend->add_model (model);
    
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

DDMDataQuery *
ddm_data_model_query_params(DDMDataModel *model,
                            const char     *method,
                            const char     *fetch,
                            GHashTable     *params)
{
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL)
        return NULL;

    query = _ddm_data_query_new(model, method_qname, fetch, params);

    model->backend->send_query(model, query);
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
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

#if 0
    /* FIXME */
    model->backend->send_update(model, query, method, params);
#endif
    
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

