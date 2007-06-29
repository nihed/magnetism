/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_OBJECT_CACHE_H__
#define __HIPPO_OBJECT_CACHE_H__

/* A cache of objects (such as images) that are retrieved via http */

#include <hippo/hippo-platform.h>

G_BEGIN_DECLS

/* Callback when an object arrives.
 * object == NULL on error
 */
typedef void (* HippoObjectCacheLoadFunc) (GObject         *cached_obj,
                                           void            *data);


typedef struct _HippoObjectCache      HippoObjectCache;
typedef struct _HippoObjectCacheClass HippoObjectCacheClass;

#define HIPPO_TYPE_OBJECT_CACHE              (hippo_object_cache_get_type ())
#define HIPPO_OBJECT_CACHE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_OBJECT_CACHE, HippoObjectCache))
#define HIPPO_OBJECT_CACHE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_OBJECT_CACHE, HippoObjectCacheClass))
#define HIPPO_IS_OBJECT_CACHE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_OBJECT_CACHE))
#define HIPPO_IS_OBJECT_CACHE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_OBJECT_CACHE))
#define HIPPO_OBJECT_CACHE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_OBJECT_CACHE, HippoObjectCacheClass))

struct _HippoObjectCache {
    GObject parent;

};

struct _HippoObjectCacheClass {
    GObjectClass parent_class;
    
    GObject* (* parse) (HippoObjectCache *cache,
                        const char       *url,
                        const char       *content_type,
                        GString          *content,
                        GError          **error);
};

GType            hippo_object_cache_get_type               (void) G_GNUC_CONST;

HippoObjectCache* hippo_object_cache_new                    (HippoPlatform *platform);

/* callback is invoked synchronously if there's a cache hit */
void             hippo_object_cache_load                   (HippoObjectCache          *cache,
                                                            const char                *url,
                                                            HippoObjectCacheLoadFunc   func,
                                                            void                      *data);

void hippo_object_cache_debug_dump (HippoObjectCache *cache);

G_END_DECLS

#endif /* __HIPPO_OBJECT_CACHE_H__ */
