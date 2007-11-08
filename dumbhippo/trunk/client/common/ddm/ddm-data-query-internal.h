/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_QUERY_INTERNAL_H__
#define __DDM_DATA_QUERY_INTERNAL_H__

#include "ddm-data-model.h"
#include "ddm-data-query.h"

G_BEGIN_DECLS

DDMDataQuery *_ddm_data_query_new(DDMDataModel   *model,
                                  DDMQName       *qname,
                                  const char     *fetch_string,
                                  GHashTable     *params,
                                  gint64          serial);

DDMDataQuery *_ddm_data_query_new_update(DDMDataModel   *model,
                                         DDMQName       *qname,
                                         GHashTable     *params,
                                         gint64          serial);

/* Like ddm_data_query_error_async(), but for use when a
 * QueryResponse work item is already in the work queue.
 */
void _ddm_data_query_mark_error (DDMDataQuery *query,
                                 DDMDataError  error,
                                 const char   *message);

/* Called when we short-circuit a getResource response and throw
 * it immediately on the work-item pile
 */
void _ddm_data_query_local_response (DDMDataQuery *query,
                                     GSList       *results);

void _ddm_data_query_run_response (DDMDataQuery *query);

gint64 _ddm_data_query_get_serial (DDMDataQuery *query);

G_END_DECLS

#endif /* __DDM_DATA_QUERY_INTERNAL_H__ */
