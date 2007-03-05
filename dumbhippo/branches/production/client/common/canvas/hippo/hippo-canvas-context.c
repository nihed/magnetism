/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-context.h"
#include "hippo-canvas-marshal.h"
#include "hippo-canvas-item.h"

static void     hippo_canvas_context_base_init (void                  *klass);

enum {
    STYLE_CHANGED,
    LAST_SIGNAL
};
static int signals[LAST_SIGNAL];

GType
hippo_canvas_context_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasContextIface),
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

        signals[STYLE_CHANGED] =
            g_signal_new ("style-changed",
                          HIPPO_TYPE_CANVAS_CONTEXT,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasContextIface, style_changed),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__BOOLEAN,
                          G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
        
        initialized = TRUE;
    }
}

PangoLayout*
hippo_canvas_context_create_layout(HippoCanvasContext *context)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    return HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->create_layout(context);
}

cairo_surface_t*
hippo_canvas_context_load_image(HippoCanvasContext *context,
                                const char         *image_name)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), NULL);

    return HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->load_image(context, image_name);
}

guint32
hippo_canvas_context_get_color(HippoCanvasContext *context,
                               HippoStockColor     color)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_CONTEXT(context), 0);

    return HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->get_color(context, color);
}

void
hippo_canvas_context_register_widget_item(HippoCanvasContext *context,
                                          HippoCanvasItem    *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->register_widget_item(context, item);
}

void
hippo_canvas_context_unregister_widget_item (HippoCanvasContext *context,
                                             HippoCanvasItem    *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->unregister_widget_item(context, item);
}
    
void
hippo_canvas_context_translate_to_widget(HippoCanvasContext *context,
                                         HippoCanvasItem    *item,
                                         int                *x_p,
                                         int                *y_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->translate_to_widget(context, item, x_p, y_p);
}
    
void
hippo_canvas_context_translate_to_screen(HippoCanvasContext *context,
                                         HippoCanvasItem    *item,
                                         int                *x_p,
                                         int                *y_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(item));
    
    HIPPO_CANVAS_CONTEXT_GET_IFACE(context)->translate_to_screen(context, item, x_p, y_p);
}

void
hippo_canvas_context_affect_color(HippoCanvasContext     *context,
                                  guint32                *color_rgba_p)
{
    HippoCanvasContextIface *iface;
    
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));

    iface = HIPPO_CANVAS_CONTEXT_GET_IFACE(context);

    if (iface->affect_color)
        (* iface->affect_color) (context, color_rgba_p);
}

void
hippo_canvas_context_affect_font_desc(HippoCanvasContext     *context,
                                      PangoFontDescription   *font_desc)
{
    HippoCanvasContextIface *iface;
    
    g_return_if_fail(HIPPO_IS_CANVAS_CONTEXT(context));
    
    iface = HIPPO_CANVAS_CONTEXT_GET_IFACE(context);
    
    if (iface->affect_font_desc)
        (* iface->affect_font_desc) (context, font_desc);
}

void
hippo_canvas_context_emit_style_changed(HippoCanvasContext *context,
                                        gboolean            resize_needed)
{
    g_signal_emit(context, signals[STYLE_CHANGED], 0,
                  resize_needed);
}
