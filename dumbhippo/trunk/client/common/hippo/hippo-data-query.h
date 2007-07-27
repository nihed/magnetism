/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_QUERY_H__
#define __HIPPO_DATA_QUERY_H__

#include <hippo/hippo-data-model.h>
#include <hippo/hippo-data-resource.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_DATA_ERROR_NO_CONNECTION = -1,
    HIPPO_DATA_ERROR_BAD_REPLY = -2,
    HIPPO_DATA_ERROR_INTERNAL = -3,
    HIPPO_DATA_ERROR_BAD_REQUEST = 400,
    HIPPO_DATA_ERROR_FORBIDDEN = 403,
    HIPPO_DATA_ERROR_ITEM_NOT_FOUND = 404,
    HIPPO_DATA_ERROR_INTERNAL_SERVER_ERROR = 500
} HippoDataError;

typedef void (*HippoSingleHandler) (HippoDataResource *result,
                                    gpointer           user_data);
typedef void (*HippoMultiHandler)  (GSList            *results,
                                    gpointer           user_data);
typedef void (*HippoUpdateHandler) (gpointer           user_data);
typedef void (*HippoErrorHandler)  (HippoDataError     error,
                                    const char        *message,
                                    gpointer           user_data);

HippoDataModel *hippo_data_query_get_model (HippoDataQuery *query);
HippoQName *    hippo_data_query_get_qname (HippoDataQuery *query);
const char *    hippo_data_query_get_fetch (HippoDataQuery *query);

void hippo_data_query_set_single_handler (HippoDataQuery     *query,
                                          HippoSingleHandler  handler,
                                          gpointer            user_data);
void hippo_data_query_set_multi_handler  (HippoDataQuery     *query,
                                          HippoMultiHandler   handler,
                                          gpointer            user_data);
void hippo_data_query_set_update_handler (HippoDataQuery     *query,
                                          HippoUpdateHandler  handler,
                                          gpointer            user_data);
void hippo_data_query_set_error_handler  (HippoDataQuery     *query,
                                          HippoErrorHandler   handler,
                                          gpointer            user_data);

G_END_DECLS

#endif /* __HIPPO_DATA_QUERY_H__ */
