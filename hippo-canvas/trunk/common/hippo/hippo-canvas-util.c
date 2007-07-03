/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <cairo.h>
#include "hippo-canvas-util.h"

/*
 * Box Cairo types to GObject types
 */
GType
hippo_cairo_surface_get_type (void)
{
  static GType hippo_cairo_surface_type = 0;
  
  if (hippo_cairo_surface_type == 0)
    hippo_cairo_surface_type = g_boxed_type_register_static
      ("HippoCairoSurface",
       (GBoxedCopyFunc) cairo_surface_reference,
       (GBoxedFreeFunc) cairo_surface_destroy);

  return hippo_cairo_surface_type;
}

