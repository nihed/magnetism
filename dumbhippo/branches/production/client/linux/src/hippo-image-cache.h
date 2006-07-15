#ifndef __HIPPO_IMAGE_CACHE_H__
#define __HIPPO_IMAGE_CACHE_H__

#include <gdk-pixbuf/gdk-pixbuf.h>

G_BEGIN_DECLS

/* pixbuf == NULL on error */
typedef void (* HippoImageCacheLoadFunc) (GdkPixbuf       *pixbuf,
                                          void            *data);

typedef struct _HippoImageCache      HippoImageCache;
typedef struct _HippoImageCacheClass HippoImageCacheClass;

#define HIPPO_TYPE_IMAGE_CACHE              (hippo_image_cache_get_type ())
#define HIPPO_IMAGE_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_IMAGE_CACHE, HippoImageCache))
#define HIPPO_IMAGE_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_IMAGE_CACHE, HippoImageCacheClass))
#define HIPPO_IS_IMAGE_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_IMAGE_CACHE))
#define HIPPO_IS_IMAGE_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_IMAGE_CACHE))
#define HIPPO_IMAGE_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_IMAGE_CACHE, HippoImageCacheClass))

GType        	 hippo_image_cache_get_type               (void) G_GNUC_CONST;

HippoImageCache* hippo_image_cache_new                    (void);

/* callback is invoked synchronously if there's a cache hit */
void             hippo_image_cache_load                   (HippoImageCache          *cache,
                                                           const char               *url,
                                                           HippoImageCacheLoadFunc   func,
                                                           void                     *data);

G_END_DECLS

#endif /* __HIPPO_IMAGE_CACHE_H__ */
