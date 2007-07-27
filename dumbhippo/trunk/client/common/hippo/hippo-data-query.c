/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-data-query-internal.h"

typedef enum {
    HANDLER_NONE,
    HANDLER_SINGLE,
    HANDLER_MULTI,
    HANDLER_UPDATE
} HandlerType;

struct _HippoDataQuery {
    HippoDataModel *model;
    HippoQName *qname;
    char *fetch;
    GHashTable *params;
    
    HandlerType handler_type;
    union {
        HippoSingleHandler single;
        HippoMultiHandler multi;
        HippoUpdateHandler update;
    } handler;
    gpointer handler_data;
    HippoErrorHandler error_handler;
    gpointer error_handler_data;
};

HippoDataModel *
hippo_data_query_get_model (HippoDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->model;
}

HippoQName *
hippo_data_query_get_qname (HippoDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->qname;
}

const char *
hippo_data_query_get_fetch (HippoDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->fetch;
}

GHashTable *
_hippo_data_query_get_params (HippoDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->params;
}

void
hippo_data_query_set_single_handler (HippoDataQuery     *query,
                                     HippoSingleHandler  handler,
                                     gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_SINGLE;
    query->handler.single = handler;
    query->handler_data = user_data;
}

void
hippo_data_query_set_multi_handler (HippoDataQuery     *query,
                                    HippoMultiHandler   handler,
                                    gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_MULTI;
    query->handler.multi = handler;
    query->handler_data = user_data;
}

void
hippo_data_query_set_update_handler (HippoDataQuery     *query,
                                     HippoUpdateHandler  handler,
                                     gpointer            user_data)
{
    g_return_if_fail(query != NULL);

    query->handler_type = HANDLER_UPDATE;
    query->handler.update = handler;
    query->handler_data = user_data;
}

void
hippo_data_query_set_error_handler (HippoDataQuery     *query,
                                    HippoErrorHandler   handler,
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
    HippoDataQuery *query = data;

    g_hash_table_replace(query->params, g_strdup(name), g_strdup(value));
}

HippoDataQuery *
_hippo_data_query_new (HippoDataModel *model,
                       HippoQName     *qname,
                       const char     *fetch,
                       GHashTable     *params)
{
    HippoDataQuery *query =  g_new0(HippoDataQuery, 1);

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
hippo_data_query_free (HippoDataQuery *query)
{
    g_free(query->fetch);
    g_hash_table_destroy(query->params);
    g_free(query);
}

void
_hippo_data_query_response (HippoDataQuery  *query,
                            GSList          *results)
{
    g_return_if_fail(query != NULL);

    switch (query->handler_type) {
    case HANDLER_NONE:
        return;
    case HANDLER_SINGLE:
        if (results == NULL) {
            _hippo_data_query_error(query,
                                    HIPPO_DATA_ERROR_ITEM_NOT_FOUND,
                                    "No result for a query expecting a single result");
            return;
        }
        if (g_slist_length(results) > 1) {
            _hippo_data_query_error(query,
                                    HIPPO_DATA_ERROR_BAD_REPLY,
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
            _hippo_data_query_error(query,
                                    HIPPO_DATA_ERROR_BAD_REPLY,
                                    "Got results for a query expecting no results");
            return;
        }
        query->handler.update(query->handler_data);
        break;
    }

    hippo_data_query_free(query);
}

void
_hippo_data_query_error (HippoDataQuery  *query,
                         HippoDataError   error,
                         const char      *message)
{
    g_return_if_fail(query != NULL);

    query->error_handler(error, message, query->error_handler_data);

    hippo_data_query_free(query);
}
