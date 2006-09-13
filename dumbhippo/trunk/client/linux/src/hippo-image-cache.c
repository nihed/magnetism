/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-image-cache.h"
#include "hippo-object-cache.h"
#include "main.h"
#include <string.h>

static void      hippo_image_cache_init                (HippoImageCache       *cache);
static void      hippo_image_cache_class_init          (HippoImageCacheClass  *klass);

static void      hippo_image_cache_finalize            (GObject               *object);

static GObject*  hippo_image_cache_parse               (HippoObjectCache      *cache,
                                                        const char            *url,
                                                        const char            *content_type,
                                                        GString               *content,
                                                        GError               **error);

struct _HippoImageCache {
    HippoObjectCache parent;
};

struct _HippoImageCacheClass {
    HippoObjectCacheClass parent_class;

};

G_DEFINE_TYPE(HippoImageCache, hippo_image_cache, HIPPO_TYPE_OBJECT_CACHE);

static void
hippo_image_cache_init(HippoImageCache  *cache)
{
}

static void
hippo_image_cache_class_init(HippoImageCacheClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoObjectCacheClass *cache_class = HIPPO_OBJECT_CACHE_CLASS(klass);
    
    object_class->finalize = hippo_image_cache_finalize;

    cache_class->parse = hippo_image_cache_parse;
}

HippoImageCache*
hippo_image_cache_new(HippoPlatform *platform)
{
    HippoImageCache *cache;

    cache = g_object_new(HIPPO_TYPE_IMAGE_CACHE,
                         "platform", platform,
                         NULL);

    return cache;
}

static void
hippo_image_cache_finalize(GObject *object)
{
    /* HippoImageCache *cache = HIPPO_IMAGE_CACHE(object); */

    G_OBJECT_CLASS(hippo_image_cache_parent_class)->finalize(object);
}


static GObject*
hippo_image_cache_parse(HippoObjectCache      *cache,
                        const char            *url,
                        const char            *content_type,
                        GString               *content,
                        GError               **error_p)
{
    GdkPixbufLoader *loader;
    GdkPixbuf *pixbuf;

    loader = gdk_pixbuf_loader_new();

    if (!gdk_pixbuf_loader_write(loader, (guchar*) content->str, content->len, error_p))
        goto failed;
    
    if (!gdk_pixbuf_loader_close(loader, error_p))
        goto failed;

    pixbuf = gdk_pixbuf_loader_get_pixbuf(loader);
    if (pixbuf == NULL) {
        g_set_error(error_p,
                    GDK_PIXBUF_ERROR,
                    GDK_PIXBUF_ERROR_FAILED,
                    _("Could not load image"));
        goto failed;
    }

    g_object_ref(pixbuf);
    g_object_unref(loader);
    return G_OBJECT(pixbuf);

  failed:
    g_assert(error_p == NULL || *error_p != NULL);
    
    if (loader)
        g_object_unref(loader);

    return NULL;
}

void
hippo_image_cache_load(HippoImageCache          *cache,
                       const char               *url,
                       HippoImageCacheLoadFunc   func,
                       void                     *data)
{
    hippo_object_cache_load(HIPPO_OBJECT_CACHE(cache),
                            url,
                            (HippoObjectCacheLoadFunc) func,
                            data);
}
