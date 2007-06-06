/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DATA_RESOURCE_H__
#define __HIPPO_DATA_RESOURCE_H__

#include <hippo/hippo-qname.h>
#include <glib.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_DATA_NONE,
    HIPPO_DATA_STRING,
    HIPPO_DATA_STRING_LIST,
    HIPPO_DATA_RESOURCE,
    HIPPO_DATA_RESOURCE_LIST
} HippoDataType;

typedef struct _HippoDataResource      HippoDataResource;

typedef void (*HippoDataFunction) (HippoDataResource *resource,
                                   GSList            *changed_properties,
                                   gpointer           user_data);

const char *hippo_data_resource_get_resource_id (HippoDataResource *resource);
const char *hippo_data_resource_get_class_id    (HippoDataResource *resource);

void hippo_data_resource_get          (HippoDataResource *resource,
                                       ...) G_GNUC_NULL_TERMINATED;
void hippo_data_resource_get_by_qname (HippoDataResource *resource,
                                       ...) G_GNUC_NULL_TERMINATED;

void hippo_data_resource_connect          (HippoDataResource *resource,
                                           const char        *property,
                                           HippoDataFunction  function,
                                           gpointer           user_data);
void hippo_data_resource_connect_by_qname (HippoDataResource *resource,
                                           HippoQName        *property,
                                           HippoDataFunction  function,
                                           gpointer           user_data);
void hippo_data_resource_disconnect       (HippoDataResource *resource,
                                           HippoDataFunction  function,
                                           gpointer           user_data);

G_END_DECLS

#endif /* __HIPPO_DATA_RESOURCE_H__ */
