/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_FETCH_H__
#define __HIPPO_DATA_FETCH_H__

#include <hippo/hippo-data-resource.h>

G_BEGIN_DECLS

typedef struct _HippoDataFetchIter HippoDataFetchIter;

struct _HippoDataFetchIter
{
    HippoDataResource *resource;
    HippoDataFetch *fetch;
    int property_index;
    HippoDataProperty *next_property;
    HippoDataFetch *next_children;
    GSList *default_properties;
};

HippoDataFetch *hippo_data_fetch_ref         (HippoDataFetch *fetch);
void            hippo_data_fetch_unref       (HippoDataFetch *fetch);
HippoDataFetch *hippo_data_fetch_from_string (const char     *str);
HippoDataFetch *hippo_data_fetch_merge       (HippoDataFetch *fetch,
					      HippoDataFetch *other);
HippoDataFetch *hippo_data_fetch_subtract    (HippoDataFetch *fetch,
                                              HippoDataFetch *other);

void hippo_data_fetch_iter_init  (HippoDataFetchIter *iter,
                                  HippoDataResource  *resource,
                                  HippoDataFetch     *fetch);
void hippo_data_fetch_iter_clear (HippoDataFetchIter *iter);

gboolean hippo_data_fetch_iter_has_next (HippoDataFetchIter  *iter);
void     hippo_data_fetch_iter_next     (HippoDataFetchIter  *iter,
                                         HippoDataProperty  **property,
                                         HippoDataFetch     **children);

G_END_DECLS

#endif /* __HIPPO_DATA_FETCH_H__ */
