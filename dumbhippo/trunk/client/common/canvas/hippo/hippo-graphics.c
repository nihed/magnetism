/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-graphics.h"

gboolean
hippo_rectangle_intersect(const HippoRectangle *src1,
                          const HippoRectangle *src2,
                          HippoRectangle       *dest)
{
    int dest_x, dest_y;
    int dest_w, dest_h;

    dest_x = MAX (src1->x, src2->x);
    dest_y = MAX (src1->y, src2->y);
    dest_w = MIN (src1->x + src1->width, src2->x + src2->width) - dest_x;
    dest_h = MIN (src1->y + src1->height, src2->y + src2->height) - dest_y;

    if (dest_w > 0 && dest_h > 0) {
        dest->x = dest_x;
        dest->y = dest_y;
        dest->width = dest_w;
        dest->height = dest_h;
        return TRUE;
    } else {
        dest->width = 0;
        dest->height = 0;
        return FALSE;
    }
}

gboolean
hippo_rectangle_equal(const HippoRectangle *r1,
                      const HippoRectangle *r2)
{
    return r1->x == r2->x && 
        r1->y == r2->y &&
        r1->width == r2->width &&
        r1->height == r2->height;
}

#define HIPPO_GET_RED(rgba)    (((rgba) >> 24)                / 255.0)
#define HIPPO_GET_GREEN(rgba)  ((((rgba) & 0x00ff0000) >> 16) / 255.0)
#define HIPPO_GET_BLUE(rgba)   ((((rgba) & 0x0000ff00) >> 8)  / 255.0)
#define HIPPO_GET_ALPHA(rgba)  (((rgba)  & 0x000000ff)        / 255.0)

void
hippo_cairo_set_source_rgba32(cairo_t *cr,
                              guint32  color)
{
    /* trying to avoid alpha 255 becoming a double alpha that isn't quite opaque ?
     * not sure this is needed.
     */
    if ((color & 0xff) == 0xff) {
        cairo_set_source_rgb(cr, HIPPO_GET_RED(color), HIPPO_GET_GREEN(color), HIPPO_GET_BLUE(color));
    } else {
        cairo_set_source_rgba(cr, HIPPO_GET_RED(color), HIPPO_GET_GREEN(color), HIPPO_GET_BLUE(color), HIPPO_GET_ALPHA(color));
    }
}

void
hippo_cairo_pattern_add_stop_rgba32(cairo_pattern_t *pattern,
                                    double           offset,
                                    guint32          color)
{
    if ((color & 0xff) == 0xff) {
        cairo_pattern_add_color_stop_rgb(pattern, offset,
                                         HIPPO_GET_RED(color), HIPPO_GET_GREEN(color), HIPPO_GET_BLUE(color));
    } else {
        cairo_pattern_add_color_stop_rgba(pattern, offset,
                                          HIPPO_GET_RED(color), HIPPO_GET_GREEN(color), HIPPO_GET_BLUE(color), HIPPO_GET_ALPHA(color));
    }
}


