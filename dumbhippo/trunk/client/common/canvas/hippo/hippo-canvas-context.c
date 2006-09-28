/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-context.h"
#include "hippo-canvas-marshal.h"
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
    
    HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->register_widget_item(context, item);
}

void
hippo_canvas_context_unregister_widget_item (HippoCanvasContext *context,
                                             HippoCanvasItem    *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->unregister_widget_item(context, item);
}
    
void
hippo_canvas_context_translate_to_widget(HippoCanvasContext *context,
                                         HippoCanvasItem    *item,
                                         int                *x_p,
                                         int                *y_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_CLASS(context)->translate_to_widget(context, item, x_p, y_p);
}
