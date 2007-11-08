/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef DDM_COMPILATION
#ifndef DDM_INSIDE_DDM_H
#error "Do not include this file directly, include ddm.h instead"
#endif /* DDM_INSIDE_DDM_H */
#endif /* DDM_COMPILATION */


#ifndef __DDM_DATA_QUERY_H__
#define __DDM_DATA_QUERY_H__

#include <ddm/ddm-data-fetch.h>
#include <ddm/ddm-data-model.h>
#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

typedef void (*DDMSingleHandler) (DDMDataResource *result,
                                  gpointer           user_data);
typedef void (*DDMMultiHandler)  (GSList            *results,
                                  gpointer           user_data);
typedef void (*DDMUpdateHandler) (gpointer           user_data);
typedef void (*DDMErrorHandler)  (DDMDataError     error,
                                  const char        *message,
                                  gpointer           user_data);

/******* For applications *******/

void          ddm_data_query_set_single_handler (DDMDataQuery     *query,
                                                 DDMSingleHandler  handler,
                                                 gpointer          user_data);
void          ddm_data_query_set_multi_handler  (DDMDataQuery     *query,
                                                 DDMMultiHandler   handler,
                                                 gpointer          user_data);
void          ddm_data_query_set_update_handler (DDMDataQuery     *query,
                                                 DDMUpdateHandler  handler,
                                                 gpointer          user_data);
void          ddm_data_query_set_error_handler  (DDMDataQuery     *query,
                                                 DDMErrorHandler   handler,
                                                 gpointer          user_data);

/***** For data model backends ******/

DDMDataModel *ddm_data_query_get_model        (DDMDataQuery *query);
DDMQName *    ddm_data_query_get_qname        (DDMDataQuery *query);
const char *  ddm_data_query_get_fetch_string (DDMDataQuery *query);
DDMDataFetch *ddm_data_query_get_fetch        (DDMDataQuery *query);
gboolean      ddm_data_query_is_update        (DDMDataQuery *query);
GHashTable *  ddm_data_query_get_params       (DDMDataQuery *query);
GSList *      ddm_data_query_get_results      (DDMDataQuery *query);
gboolean      ddm_data_query_has_error        (DDMDataQuery *query);

/* Called by the backend when a response is received from upstream */

void ddm_data_query_response (DDMDataQuery *query,
                              GSList       *results);

void ddm_data_query_error    (DDMDataQuery *query,
                              DDMDataError  error,
                              const char   *message);

/* Like ddm_data_query_error(), but calls the callback asynchronously;
 * this should generally be used when an error occurs while sending
 * a DDMDataQuery.
 */
void ddm_data_query_error_async (DDMDataQuery *query,
                                 DDMDataError  error,
                                 const char   *message);

/* Debugging convenience */
const char *ddm_data_query_get_id_string (DDMDataQuery *query);

G_END_DECLS

#endif /* __DDM_DATA_QUERY_H__ */
