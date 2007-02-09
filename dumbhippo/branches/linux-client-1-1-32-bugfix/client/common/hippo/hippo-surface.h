/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SURFACE_H__
#define __HIPPO_SURFACE_H__

/* Trivially wraps a cairo_surface_t to make it a GObject, useful with
 * HippoObjectCache
 */

#include <hippo/hippo-basics.h>
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoSurface      HippoSurface;
typedef struct _HippoSurfaceClass HippoSurfaceClass;

#define HIPPO_TYPE_SURFACE              (hippo_surface_get_type ())
#define HIPPO_SURFACE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_SURFACE, HippoSurface))
#define HIPPO_SURFACE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_SURFACE, HippoSurfaceClass))
#define HIPPO_IS_SURFACE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_SURFACE))
#define HIPPO_IS_SURFACE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_SURFACE))
#define HIPPO_SURFACE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_SURFACE, HippoSurfaceClass))

GType            hippo_surface_get_type               (void) G_GNUC_CONST;

HippoSurface*    hippo_surface_new                    (cairo_surface_t *csurface);

cairo_surface_t* hippo_surface_get_surface            (HippoSurface    *surface); 

G_END_DECLS

#endif /* __HIPPO_SURFACE_H__ */
