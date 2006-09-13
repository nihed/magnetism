/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_PIXBUF_CACHE_H__
#define __HIPPO_PIXBUF_CACHE_H__

#include <gdk-pixbuf/gdk-pixbuf.h>
#include "hippo-object-cache.h"

G_BEGIN_DECLS

/* pixbuf == NULL on error */
typedef void (* HippoPixbufCacheLoadFunc) (GdkPixbuf       *pixbuf,
                                           void            *data);

typedef struct _HippoPixbufCache      HippoPixbufCache;
typedef struct _HippoPixbufCacheClass HippoPixbufCacheClass;

#define HIPPO_TYPE_PIXBUF_CACHE              (hippo_pixbuf_cache_get_type ())
#define HIPPO_PIXBUF_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PIXBUF_CACHE, HippoPixbufCache))
#define HIPPO_PIXBUF_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PIXBUF_CACHE, HippoPixbufCacheClass))
#define HIPPO_IS_PIXBUF_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PIXBUF_CACHE))
#define HIPPO_IS_PIXBUF_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PIXBUF_CACHE))
#define HIPPO_PIXBUF_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_PIXBUF_CACHE, HippoPixbufCacheClass))

GType        	  hippo_pixbuf_cache_get_type               (void) G_GNUC_CONST;

HippoPixbufCache* hippo_pixbuf_cache_new                    (HippoPlatform             *platform);

/* callback is invoked synchronously if there's a cache hit */
void              hippo_pixbuf_cache_load                   (HippoPixbufCache          *cache,
                                                             const char                *url,
                                                             HippoPixbufCacheLoadFunc   func,
                                                             void                      *data);

G_END_DECLS

#endif /* __HIPPO_PIXBUF_CACHE_H__ */
