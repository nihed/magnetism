/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-context.h"
#include "hippo-common-marshal.h"
#include "hippo-canvas-item.h"

static void     hippo_canvas_context_base_init (void                  *klass);

enum {
    LAST_SIGNAL
};
/* static int signals[LAST_SIGNAL]; */

GType
hippo_canvas_context_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasContextClass),
                hippo_canvas_context_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoCanvasContext",
                                      &info, 0);
    }
    
    return type;
}

static void
hippo_canvas_context_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        /* create signals in here */

        initialized = TRUE;
    }
}

PangoLayout*
hippo_canvas_context_create_layout(HippoCanvasContext *context)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->create_layout(context);
}

cairo_surface_t*
hippo_canvas_context_load_image(HippoCanvasContext *context,
                                const char         *image_name)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->load_image(context, image_name);
}

guint32
hippo_canvas_context_get_color(HippoCanvasContext *context,
                               HippoStockColor     color)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), 0);

    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->get_color(context, color);
}

void
hippo_canvas_context_register_widget_item(HippoCanvasContext *context,
                                          HippoCanvasItem    *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->register_widget_item(context, item);
}

void
hippo_canvas_context_unregister_widget_item (HippoCanvasContext *context,
                                             HippoCanvasItem    *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->unregister_widget_item(context, item);
}
    
void
hippo_canvas_context_translate_to_widget(HippoCanvasContext *context,
                                         HippoCanvasItem    *item,
                                         int                *x_p,
                                         int                *y_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    return HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->translate_to_widget(context, item, x_p, y_p);
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


