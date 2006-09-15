/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-image-cache.h"
#include "hippo-object-cache.h"
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

typedef struct {
    GString *str;
    unsigned int already_read;
} GStringReader;

static cairo_status_t
gstring_read_func(void		*closure,
                  unsigned char	*buffer,
                  unsigned int	 buffer_length)
{
    GStringReader *reader = closure;
    unsigned int remaining;

    remaining = reader->str->len - reader->already_read;

    if (buffer_length > remaining) {
        return CAIRO_STATUS_READ_ERROR;
    }

    memcpy(buffer, reader->str->str + reader->already_read,
           buffer_length);

    reader->already_read += buffer_length;

    return CAIRO_STATUS_SUCCESS;
}

static GObject*
hippo_image_cache_parse(HippoObjectCache      *cache,
                        const char            *url,
                        const char            *content_type,
                        GString               *content,
                        GError               **error_p)
{
    cairo_surface_t *csurface;
    HippoSurface *surface;
    GStringReader reader;

    reader.str = content;
    reader.already_read = 0;
    
    csurface =
        cairo_image_surface_create_from_png_stream(gstring_read_func,
                                                   &reader);
    

    if (csurface != NULL && cairo_surface_status(csurface) == CAIRO_STATUS_SUCCESS) {
        surface = hippo_surface_new(csurface);
        cairo_surface_destroy(csurface);
    } else {
        const char *msg;
        
        if (csurface) {
            msg = cairo_status_to_string(cairo_surface_status(csurface));
            cairo_surface_destroy(csurface);
        } else {
            msg = _("Corrupt image");
        }
        
        surface = NULL;
        g_set_error(error_p,
                    g_quark_from_string("cairo-surface-error"),
                    0, msg);                    
        goto failed;
    }

    return G_OBJECT(surface);
    
  failed:
    g_assert(error_p == NULL || *error_p != NULL);    

    return NULL;
}

void
hippo_image_cache_load(HippoImageCache          *cache,
                       const char               *url,
                       HippoImageCacheLoadFunc   func,
                       void                     *data)
{
    /* g_debug("image cache load '%s'", url); */
    hippo_object_cache_load(HIPPO_OBJECT_CACHE(cache),
                            url,
                            (HippoObjectCacheLoadFunc) func,
                            data);
}
