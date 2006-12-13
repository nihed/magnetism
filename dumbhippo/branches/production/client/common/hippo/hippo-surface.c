/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-surface.h"
#include <string.h>


static void      hippo_surface_init                (HippoSurface       *surface);
static void      hippo_surface_class_init          (HippoSurfaceClass  *klass);

static void      hippo_surface_dispose             (GObject            *object);
static void      hippo_surface_finalize            (GObject            *object);

struct _HippoSurface {
    GObject parent;
    cairo_surface_t *csurface;
};

struct _HippoSurfaceClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoSurface, hippo_surface, G_TYPE_OBJECT);

static void
hippo_surface_init(HippoSurface  *surface)
{
}

static void
hippo_surface_class_init(HippoSurfaceClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->dispose = hippo_surface_dispose;
    object_class->finalize = hippo_surface_finalize;
}

static void
hippo_surface_dispose(GObject *object)
{
    HippoSurface *surface = HIPPO_SURFACE(object);

    if (surface->csurface) {
        cairo_surface_destroy(surface->csurface);
        surface->csurface = NULL;
    }
    
    G_OBJECT_CLASS(hippo_surface_parent_class)->dispose(object);
}

static void
hippo_surface_finalize(GObject *object)
{
    /* HippoSurface *surface = HIPPO_SURFACE(object); */

    G_OBJECT_CLASS(hippo_surface_parent_class)->finalize(object);
}

HippoSurface*
hippo_surface_new(cairo_surface_t *csurface)
{
    HippoSurface *surface;

    surface = g_object_new(HIPPO_TYPE_SURFACE,
                           NULL);

    cairo_surface_reference(csurface);
    surface->csurface = csurface;
    
    return surface;
}

cairo_surface_t*
hippo_surface_get_surface(HippoSurface    *surface)
{
    return surface->csurface;
}
