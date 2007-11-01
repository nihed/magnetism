/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-data-query-internal.h"
#include "ddm-data-model-internal.h"
#include "ddm-data-resource-internal.h"

typedef enum {
    HANDLER_NONE,
    HANDLER_SINGLE,
    HANDLER_MULTI,
    HANDLER_UPDATE
} HandlerType;

struct _DDMDataQuery {
    DDMDataModel *model;
    DDMQName *qname;
    gint64 serial;
    gboolean is_update;
    char *fetch_string;
    DDMDataFetch *fetch;
    GHashTable *params;
    GSList *results;

    HandlerType handler_type;
    union {
        DDMSingleHandler single;
        DDMMultiHandler multi;
        DDMUpdateHandler update;
    } handler;
    gpointer handler_data;
    DDMErrorHandler error_handler;
    gpointer error_handler_data;
    char *id_string;
};

DDMDataModel *
ddm_data_query_get_model (DDMDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->model;
}

DDMQName *
ddm_data_query_get_qname (DDMDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->qname;
}

gboolean
ddm_data_query_is_update (DDMDataQuery *query)
{
    g_return_val_if_fail(query != NULL, FALSE);

    return query->is_update;
}

const char *
ddm_data_query_get_fetch_string (DDMDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->fetch_string;
}

DDMDataFetch *
ddm_data_query_get_fetch (DDMDataQuery     *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->fetch;
}

GHashTable *
ddm_data_query_get_params (DDMDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->params;
}

GSList *
ddm_data_query_get_results (DDMDataQuery *query)
{
    return query->results;
}

void
ddm_data_query_set_single_handler (DDMDataQuery     *query,
                                   DDMSingleHandler  handler,
                                   gpointer          user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_SINGLE;
    query->handler.single = handler;
    query->handler_data = user_data;
}

void
ddm_data_query_set_multi_handler (DDMDataQuery     *query,
                                  DDMMultiHandler   handler,
                                  gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_MULTI;
    query->handler.multi = handler;
    query->handler_data = user_data;
}

void
ddm_data_query_set_update_handler (DDMDataQuery     *query,
                                   DDMUpdateHandler  handler,
                                   gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_UPDATE;
    query->handler.update = handler;
    query->handler_data = user_data;
}

void
ddm_data_query_set_error_handler (DDMDataQuery     *query,
                                  DDMErrorHandler   handler,
                                  gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->error_handler = handler;
    query->error_handler_data = user_data;
}

static void
add_param_foreach(gpointer key,
                  gpointer val,
                  gpointer data)
{
    const char *name = key;
    const char *value = val;
    DDMDataQuery *query = data;

    g_hash_table_replace(query->params, g_strdup(name), g_strdup(value));
}

DDMDataQuery *
_ddm_data_query_new (DDMDataModel *model,
                     DDMQName     *qname,
                     const char   *fetch_string,
                     GHashTable   *params,
                     gint64        serial)
{
    DDMDataQuery *query;
    DDMDataFetch *fetch;

    if (fetch_string != NULL) {
        fetch = ddm_data_fetch_from_string(fetch_string);
        if (fetch == NULL) {
            g_warning("Invalid fetch string '%s'", fetch_string);
            return NULL;
        }
    } else {
        fetch = NULL;
    }

    query =  g_new0(DDMDataQuery, 1);
    query->model = model;
    query->qname = qname;
    query->is_update = FALSE;
    query->fetch_string = g_strdup(fetch_string);
    query->fetch = fetch;
    query->params = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          (GDestroyNotify)g_free, (GDestroyNotify)g_free);
    g_hash_table_foreach(params, add_param_foreach, query);

    query->handler_type = HANDLER_NONE;
    query->serial = serial;

    query->id_string = g_strdup_printf("Query-%" G_GINT64_MODIFIER "d", serial);

    return query;
}

DDMDataQuery *
_ddm_data_query_new_update (DDMDataModel *model,
                            DDMQName     *qname,
                            GHashTable   *params,
                            gint64        serial)
{
    DDMDataQuery *query =  g_new0(DDMDataQuery, 1);

    query->model = model;
    query->qname = qname;
    query->is_update = TRUE;
    query->fetch_string = NULL;
    query->is_update = TRUE;
    query->params = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          (GDestroyNotify)g_free, (GDestroyNotify)g_free);
    g_hash_table_foreach(params, add_param_foreach, query);

    query->handler_type = HANDLER_NONE;
    query->serial = serial;

    query->id_string = g_strdup_printf("Update-%" G_GINT64_MODIFIER "d", serial);

    return query;
}

static void
ddm_data_query_free (DDMDataQuery *query)
{
    if (query->fetch)
        ddm_data_fetch_unref(query->fetch);
    
    g_free(query->fetch_string);
    g_hash_table_destroy(query->params);

    g_slist_free(query->results);
    g_free(query->id_string);
    
    g_free(query);
}

static void
mark_received_fetches(DDMDataResource *resource,
                      DDMDataFetch    *fetch,
                      gboolean         local)
{
    DDMDataFetchIter iter;

    /* For a fetch we short-circuit locally, we don't need to record
     * record any additional fetches on remote resources ... we can
     * only short-circuit a remote fetch if we already have everything
     * we need. But for a local resource (injected into the data model),
     * we want to record our fetch in resource->received_fetch, because
     * we use resource->received_fetch when calculating local interest.
     *
     * See comment in _ddm_data_resource_resolve_notification()
     */
    if (!local || ddm_data_resource_is_local(resource))
        _ddm_data_resource_fetch_received(resource, fetch);

    ddm_data_fetch_iter_init(&iter, resource, fetch);

    while (ddm_data_fetch_iter_has_next(&iter)) {
        DDMDataProperty *property;
        DDMDataFetch *children;
        DDMDataValue value;
        
        ddm_data_fetch_iter_next(&iter, &property, &children);

        if (children != NULL) {
            ddm_data_property_get_value(property, &value);
            
            g_assert (DDM_DATA_BASE(value.type) == DDM_DATA_RESOURCE);
            
            if (DDM_DATA_IS_LIST(value.type)) {
                GSList *l;
                
                for (l = value.u.list; l; l = l->next) {
                    mark_received_fetches(l->data, children, local);
                }
            } else {
                mark_received_fetches(value.u.resource, children, local);
            }
        }
    }
    
    ddm_data_fetch_iter_clear(&iter);
}

static void
data_query_response_internal (DDMDataQuery *query,
                              GSList       *results,
                              gboolean      local)
{
    DDMWorkItem *item;
    GSList *l;

    if (!local) {
        g_debug("%s: Received response", query->id_string);
        
        for (l = results; l; l = l->next) {
            g_debug("%s: result=%s", query->id_string, ddm_data_resource_get_resource_id(l->data));
        }
    }
        
    for (l = results; l; l = l->next) {
        mark_received_fetches(l->data, query->fetch, local);
    }

    _ddm_data_model_query_answered(query->model, query);
    
    query->results = g_slist_copy(results);

    item = _ddm_work_item_query_response_new(query->model, query);
    _ddm_data_model_add_work_item(query->model, item);
    _ddm_work_item_unref(item);
}

void
ddm_data_query_response (DDMDataQuery *query,
                         GSList       *results)
{
    g_return_if_fail(query != NULL);
    
    data_query_response_internal(query, results, FALSE);
}

void
_ddm_data_query_local_response (DDMDataQuery *query,
                                GSList       *results)
{
    g_return_if_fail(query != NULL);
    
    data_query_response_internal(query, results, TRUE);
}

void
_ddm_data_query_run_response (DDMDataQuery *query)
{
    g_return_if_fail(query != NULL);

    g_debug("%s: Have complete fetch, running response", query->id_string);
    
    switch (query->handler_type) {
    case HANDLER_NONE:
        return;
    case HANDLER_SINGLE:
        if (query->results == NULL) {
            ddm_data_query_error(query,
                                 DDM_DATA_ERROR_ITEM_NOT_FOUND,
                                 "No result for a query expecting a single result");
            return;
        }
        if (g_slist_length(query->results) > 1) {
            ddm_data_query_error(query,
                                 DDM_DATA_ERROR_BAD_REPLY,
                                 "Too many results for a query expecting a single result");
            return;
        }
        query->handler.single(query->results->data, query->handler_data);
        break;
    case HANDLER_MULTI:
        query->handler.multi(query->results, query->handler_data);
        break;
    case HANDLER_UPDATE:
        if (query->results != NULL) {
            ddm_data_query_error(query,
                                 DDM_DATA_ERROR_BAD_REPLY,
                                 "Got results for a query expecting no results");
            return;
        }
        query->handler.update(query->handler_data);
        break;
    }

    ddm_data_query_free(query);
}

void
ddm_data_query_error (DDMDataQuery *query,
                      DDMDataError  error,
                      const char   *message)
{
    g_return_if_fail(query != NULL);

    g_debug("%s: Got error response: %s (%d)", query->id_string, message != NULL ? message : "<null>", error);

    _ddm_data_model_query_answered(query->model, query);
    
    if (query->error_handler)    
        query->error_handler(error, message, query->error_handler_data);
    
    ddm_data_query_free(query);
}

gint64
_ddm_data_query_get_serial (DDMDataQuery *query)
{
    return query->serial;
}

const char *
ddm_data_query_get_id_string (DDMDataQuery *query)
{
    return query->id_string;
}
