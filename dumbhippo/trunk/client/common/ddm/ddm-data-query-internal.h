/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_QUERY_INTERNAL_H__
#define __DDM_DATA_QUERY_INTERNAL_H__

#include "ddm-data-model.h"
#include "ddm-data-query.h"

G_BEGIN_DECLS

DDMDataQuery *_ddm_data_query_new(DDMDataModel   *model,
                                  DDMQName       *qname,
                                  const char     *fetch,
                                  GHashTable     *params);

G_END_DECLS

#endif /* __DDM_DATA_QUERY_INTERNAL_H__ */
