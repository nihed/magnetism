/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_UTIL_H__
#define __HIPPO_CANVAS_UTIL_H__

#include <glib-object.h>

G_BEGIN_DECLS

/* utility methods used in hippo canvas */

#define HIPPO_TYPE_CAIRO_SURFACE           (hippo_cairo_surface_get_type ())
GType              hippo_cairo_surface_get_type (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_UTIL_H__ */
