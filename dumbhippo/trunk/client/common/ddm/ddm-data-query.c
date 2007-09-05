/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-data-query-internal.h"

typedef enum {
    HANDLER_NONE,
    HANDLER_SINGLE,
    HANDLER_MULTI,
    HANDLER_UPDATE
} HandlerType;

struct _DDMDataQuery {
    DDMDataModel *model;
    DDMQName *qname;
    char *fetch;
    GHashTable *params;

    HandlerType handler_type;
    union {
        DDMSingleHandler single;
        DDMMultiHandler multi;
        DDMUpdateHandler update;
    } handler;
    gpointer handler_data;
    DDMErrorHandler error_handler;
    gpointer error_handler_data;
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

const char *
ddm_data_query_get_fetch (DDMDataQuery *query)
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
                     const char   *fetch,
                     GHashTable   *params)
{
    DDMDataQuery *query =  g_new0(DDMDataQuery, 1);

    query->model = model;
    query->qname = qname;
    query->fetch = g_strdup(fetch);
    query->params = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          (GDestroyNotify)g_free, (GDestroyNotify)g_free);
    g_hash_table_foreach(params, add_param_foreach, query);

    query->handler_type = HANDLER_NONE;

    return query;
}

static void
ddm_data_query_free (DDMDataQuery *query)
{
    g_free(query->fetch);
    g_hash_table_destroy(query->params);
    g_free(query);
}

void
ddm_data_query_response (DDMDataQuery  *query,
                         GSList          *results)
{
    g_return_if_fail(query != NULL);

    switch (query->handler_type) {
    case HANDLER_NONE:
        return;
    case HANDLER_SINGLE:
        if (results == NULL) {
            ddm_data_query_error(query,
                                 DDM_DATA_ERROR_ITEM_NOT_FOUND,
                                 "No result for a query expecting a single result");
            return;
        }
        if (g_slist_length(results) > 1) {
            ddm_data_query_error(query,
                                 DDM_DATA_ERROR_BAD_REPLY,
                                 "Too many results for a query expecting a single result");
            return;
        }
        query->handler.single(results->data, query->handler_data);
        break;
    case HANDLER_MULTI:
        query->handler.multi(results, query->handler_data);
        break;
    case HANDLER_UPDATE:
        if (results != NULL) {
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
ddm_data_query_error (DDMDataQuery  *query,
                      DDMDataError   error,
                      const char      *message)
{
    g_return_if_fail(query != NULL);

    query->error_handler(error, message, query->error_handler_data);

    ddm_data_query_free(query);
}
