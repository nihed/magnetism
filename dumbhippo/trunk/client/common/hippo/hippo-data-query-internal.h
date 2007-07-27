/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_QUERY_INTERNAL_H__
#define __HIPPO_DATA_QUERY_INTERNAL_H__

#include "hippo-data-model.h"
#include "hippo-data-query.h"

G_BEGIN_DECLS

HippoDataQuery *_hippo_data_query_new(HippoDataModel *model,
                                      HippoQName     *qname,
                                      const char     *fetch,
                                      GHashTable     *params);

GHashTable *_hippo_data_query_get_params (HippoDataQuery *query);
void        _hippo_data_query_response   (HippoDataQuery *query,
                                          GSList         *results);
void        _hippo_data_query_error      (HippoDataQuery *query,
                                          HippoDataError  error,
                                          const char     *message);

G_END_DECLS

#endif /* __HIPPO_DATA_QUERY_INTERNAL_H__ */
