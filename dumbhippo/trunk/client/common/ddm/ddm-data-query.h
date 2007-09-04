/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_QUERY_H__
#define __DDM_DATA_QUERY_H__

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */


#include <ddm/ddm-data-model.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

typedef enum {
    DDM_DATA_ERROR_NO_CONNECTION = -1,
    DDM_DATA_ERROR_BAD_REPLY = -2,
    DDM_DATA_ERROR_INTERNAL = -3,
    DDM_DATA_ERROR_BAD_REQUEST = 400,
    DDM_DATA_ERROR_FORBIDDEN = 403,
    DDM_DATA_ERROR_ITEM_NOT_FOUND = 404,
    DDM_DATA_ERROR_INTERNAL_SERVER_ERROR = 500
} DDMDataError;

typedef void (*DDMSingleHandler) (DDMDataResource *result,
                                  gpointer           user_data);
typedef void (*DDMMultiHandler)  (GSList            *results,
                                  gpointer           user_data);
typedef void (*DDMUpdateHandler) (gpointer           user_data);
typedef void (*DDMErrorHandler)  (DDMDataError     error,
                                  const char        *message,
                                  gpointer           user_data);

DDMDataModel *ddm_data_query_get_model (DDMDataQuery *query);
DDMQName *    ddm_data_query_get_qname (DDMDataQuery *query);
const char *    ddm_data_query_get_fetch (DDMDataQuery *query);

void ddm_data_query_set_single_handler (DDMDataQuery     *query,
                                        DDMSingleHandler  handler,
                                        gpointer            user_data);
void ddm_data_query_set_multi_handler  (DDMDataQuery     *query,
                                        DDMMultiHandler   handler,
                                        gpointer            user_data);
void ddm_data_query_set_update_handler (DDMDataQuery     *query,
                                        DDMUpdateHandler  handler,
                                        gpointer            user_data);
void ddm_data_query_set_error_handler  (DDMDataQuery     *query,
                                        DDMErrorHandler   handler,
                                        gpointer            user_data);

G_END_DECLS

#endif /* __DDM_DATA_QUERY_H__ */
