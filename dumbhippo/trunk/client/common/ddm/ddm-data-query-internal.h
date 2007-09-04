/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_QUERY_INTERNAL_H__
#define __DDM_DATA_QUERY_INTERNAL_H__

#include "ddm-data-model.h"
#include "ddm-data-query.h"

G_BEGIN_DECLS

DDMDataQuery *_ddm_data_query_new(DDMDataModel *model,
                                  DDMQName     *qname,
                                  const char     *fetch,
                                  GHashTable     *params);

GHashTable *_ddm_data_query_get_params (DDMDataQuery *query);
void        _ddm_data_query_response   (DDMDataQuery *query,
                                        GSList         *results);
void        _ddm_data_query_error      (DDMDataQuery *query,
                                        DDMDataError  error,
                                        const char     *message);

G_END_DECLS

#endif /* __DDM_DATA_QUERY_INTERNAL_H__ */
