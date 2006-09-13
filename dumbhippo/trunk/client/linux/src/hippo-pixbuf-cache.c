/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-pixbuf-cache.h"
#include "hippo-object-cache.h"
#include "main.h"
#include <string.h>

static void      hippo_pixbuf_cache_init                (HippoPixbufCache       *cache);
static void      hippo_pixbuf_cache_class_init          (HippoPixbufCacheClass  *klass);

static void      hippo_pixbuf_cache_finalize            (GObject               *object);

static GObject*  hippo_pixbuf_cache_parse               (HippoObjectCache      *cache,
                                                        const char             *url,
                                                        const char             *content_type,
                                                        GString                *content,
                                                        GError                **error);

struct _HippoPixbufCache {
    HippoObjectCache parent;
};

struct _HippoPixbufCacheClass {
    HippoObjectCacheClass parent_class;

};

G_DEFINE_TYPE(HippoPixbufCache, hippo_pixbuf_cache, HIPPO_TYPE_OBJECT_CACHE);

static void
hippo_pixbuf_cache_init(HippoPixbufCache  *cache)
{
}

static void
hippo_pixbuf_cache_class_init(HippoPixbufCacheClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoObjectCacheClass *cache_class = HIPPO_OBJECT_CACHE_CLASS(klass);
    
    object_class->finalize = hippo_pixbuf_cache_finalize;

    cache_class->parse = hippo_pixbuf_cache_parse;
}

HippoPixbufCache*
hippo_pixbuf_cache_new(HippoPlatform *platform)
{
    HippoPixbufCache *cache;

    cache = g_object_new(HIPPO_TYPE_PIXBUF_CACHE,
                         "platform", platform,
                         NULL);

    return cache;
}

static void
hippo_pixbuf_cache_finalize(GObject *object)
{
    /* HippoPixbufCache *cache = HIPPO_PIXBUF_CACHE(object); */

    G_OBJECT_CLASS(hippo_pixbuf_cache_parent_class)->finalize(object);
}


static GObject*
hippo_pixbuf_cache_parse(HippoObjectCache      *cache,
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
                    _("Could not load pixbuf"));
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
hippo_pixbuf_cache_load(HippoPixbufCache          *cache,
                        const char                *url,
                        HippoPixbufCacheLoadFunc   func,
                        void                      *data)
{
    hippo_object_cache_load(HIPPO_OBJECT_CACHE(cache),
                            url,
                            (HippoObjectCacheLoadFunc) func,
                            data);
}
