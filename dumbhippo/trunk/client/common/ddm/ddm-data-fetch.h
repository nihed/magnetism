/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DDM_DATA_FETCH_H__
#define __DDM_DATA_FETCH_H__

#include <ddm/ddm-data-resource.h>

G_BEGIN_DECLS

typedef struct _DDMDataFetchIter DDMDataFetchIter;

struct _DDMDataFetchIter
{
    DDMDataResource *resource;
    DDMDataFetch *fetch;
    int property_index;
    DDMDataProperty *next_property;
    DDMDataFetch *next_children;
    GSList *default_properties;
};

DDMDataFetch *ddm_data_fetch_ref         (DDMDataFetch *fetch);
void            ddm_data_fetch_unref       (DDMDataFetch *fetch);
DDMDataFetch *ddm_data_fetch_from_string (const char     *str);
DDMDataFetch *ddm_data_fetch_merge       (DDMDataFetch *fetch,
                                          DDMDataFetch *other);
DDMDataFetch *ddm_data_fetch_subtract    (DDMDataFetch *fetch,
                                          DDMDataFetch *other);
char *          ddm_data_fetch_to_string   (DDMDataFetch *fetch);


void ddm_data_fetch_iter_init  (DDMDataFetchIter *iter,
                                DDMDataResource  *resource,
                                DDMDataFetch     *fetch);
void ddm_data_fetch_iter_clear (DDMDataFetchIter *iter);

gboolean ddm_data_fetch_iter_has_next (DDMDataFetchIter  *iter);
void     ddm_data_fetch_iter_next     (DDMDataFetchIter  *iter,
                                       DDMDataProperty  **property,
                                       DDMDataFetch     **children);

G_END_DECLS

#endif /* __DDM_DATA_FETCH_H__ */
