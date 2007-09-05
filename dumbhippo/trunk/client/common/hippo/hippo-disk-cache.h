/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DISK_CACHE_H__
#define __HIPPO_DISK_CACHE_H__

#include <glib-object.h>
#include <ddm/ddm.h>
#include "hippo-data-cache.h"

G_BEGIN_DECLS

typedef struct _HippoDiskCache      HippoDiskCache;
typedef struct _HippoDiskCacheClass HippoDiskCacheClass;

#define HIPPO_TYPE_DISK_CACHE              (hippo_disk_cache_get_type ())
#define HIPPO_DISK_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DISK_CACHE, HippoDiskCache))
#define HIPPO_DISK_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DISK_CACHE, HippoDiskCacheClass))
#define HIPPO_IS_DISK_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DISK_CACHE))
#define HIPPO_IS_DISK_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DISK_CACHE))
#define HIPPO_DISK_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DISK_CACHE, HippoDiskCacheClass))

GType            hippo_disk_cache_get_type               (void) G_GNUC_CONST;

HippoDiskCache *_hippo_disk_cache_new (HippoDataCache *data_cache);

void _hippo_disk_cache_close (HippoDiskCache *disk_cache);

void _hippo_disk_cache_do_query (HippoDiskCache *cache,
                                 DDMDataQuery   *query);

void _hippo_disk_cache_save_properties_to_disk (HippoDiskCache       *cache,
                                                DDMDataResource      *resource,
                                                GSList               *properties,
                                                gint64                timestamp);
void _hippo_disk_cache_save_query_to_disk      (HippoDiskCache       *cache,
                                                DDMDataQuery         *query,
                                                GSList               *resources,
                                                DDMNotificationSet   *properties);
void _hippo_disk_cache_save_update_to_disk     (HippoDiskCache       *cache,
                                                DDMNotificationSet   *properties);

HippoDiskCache*  _hippo_data_model_get_disk_cache (DDMDataModel     *model);

G_END_DECLS

#endif /* __HIPPO_DISK_CACHE_H__ */
