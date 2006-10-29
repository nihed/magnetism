/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_GRAPHICS_H__
#define __HIPPO_GRAPHICS_H__

#include <glib-object.h>
#include <cairo.h>

G_BEGIN_DECLS

#define HIPPO_TYPE_RECTANGLE (hippo_rectangle_get_type())

typedef enum
{
    HIPPO_ORIENTATION_VERTICAL,
    HIPPO_ORIENTATION_HORIZONTAL
} HippoOrientation;

typedef enum
{
    HIPPO_SIDE_TOP,
    HIPPO_SIDE_BOTTOM,
    HIPPO_SIDE_LEFT,
    HIPPO_SIDE_RIGHT
} HippoSide;

typedef enum{
    HIPPO_GRAVITY_NORTH_WEST,
    HIPPO_GRAVITY_NORTH_EAST,
    HIPPO_GRAVITY_SOUTH_EAST,
    HIPPO_GRAVITY_SOUTH_WEST
} HippoGravity;

typedef struct {
    int x;
    int y;
    int width;
    int height;
} HippoRectangle;

GType           hippo_rectangle_get_type   (void) G_GNUC_CONST;
gboolean        hippo_rectangle_intersect  (const HippoRectangle *src1,
                                            const HippoRectangle *src2,
                                            HippoRectangle       *dest);
gboolean        hippo_rectangle_equal      (const HippoRectangle *r1,
                                            const HippoRectangle *r2);
HippoRectangle *hippo_rectangle_copy       (HippoRectangle *r);
void            hippo_rectangle_free       (HippoRectangle *r);

void hippo_cairo_set_source_rgba32       (cairo_t         *cr,
                                          guint32          color);
void hippo_cairo_pattern_add_stop_rgba32 (cairo_pattern_t *pattern,
                                          double           offset,
                                          guint32          color);

G_END_DECLS

#endif /* __HIPPO_GRAPHICS_H__ */
