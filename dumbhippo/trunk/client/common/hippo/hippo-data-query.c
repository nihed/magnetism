/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-data-query-internal.h"

typedef enum {
    HANDLER_NONE,
    HANDLER_SINGLE,
    HANDLER_MULTI,
    HANDLER_UPDATE
} HandlerType;

struct _HippoDataQuery {
    HippoDataQuery *query;
    HippoQName *qname;
    
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

HippoQName *
hippo_data_query_get_qname (HippoDataQuery *query)
{
    g_return_val_if_fail(query != NULL, NULL);

    return query->qname;
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

HippoDataQuery *
_hippo_data_query_new (HippoQName *qname)
{
    HippoDataQuery *query =  g_new0(HippoDataQuery, 1);
    
    query->qname = qname;
    query->handler_type = HANDLER_NONE;
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

    g_free(query);
}

void
_hippo_data_query_error (HippoDataQuery  *query,
                         HippoDataError   error,
                         const char      *message)
{
    g_return_if_fail(query != NULL);

    query->error_handler(error, message, query->error_handler_data);

    g_free(query);
}
