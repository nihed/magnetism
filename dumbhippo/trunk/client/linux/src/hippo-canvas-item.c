/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-item.h"
#include <hippo/hippo-common-marshal.h>

static void     hippo_canvas_item_base_init (void                  *klass);

enum {
    PAINT,
    REQUEST_CHANGED,      /* The size we want to request may have changed */
    PAINT_NEEDED,
    BUTTON_PRESS_EVENT,
    MOTION_NOTIFY_EVENT,
    LAST_SIGNAL
};
static int signals[LAST_SIGNAL];

GType
hippo_canvas_item_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoCanvasItemClass),
                hippo_canvas_item_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoCanvasItem",
                                      &info, 0);
    }

    return type;
}

static void
hippo_canvas_item_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        /* create signals in here */
        signals[PAINT] =
            g_signal_new ("paint",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, paint),
            		  NULL, NULL,
                          g_cclosure_marshal_VOID__POINTER,
                          G_TYPE_NONE, 1, G_TYPE_POINTER);
        signals[REQUEST_CHANGED] =
            g_signal_new ("request-changed",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, request_changed),
            		  NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);
        signals[PAINT_NEEDED] =
            g_signal_new ("paint-needed",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, paint_needed),
            		  NULL, NULL,
                          hippo_common_marshal_VOID__INT_INT_INT_INT,
                          G_TYPE_NONE, 4, G_TYPE_INT, G_TYPE_INT, G_TYPE_INT, G_TYPE_INT);
        signals[BUTTON_PRESS_EVENT] =
            g_signal_new ("button-press-event",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
            		  G_STRUCT_OFFSET(HippoCanvasItemClass, button_press_event),
            		  g_signal_accumulator_true_handled, NULL,
                          hippo_common_marshal_BOOLEAN__POINTER,
            		  G_TYPE_BOOLEAN, 1, G_TYPE_POINTER);
        signals[MOTION_NOTIFY_EVENT] =
            g_signal_new ("motion-notify-event",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
            		  G_STRUCT_OFFSET(HippoCanvasItemClass, motion_notify_event),
            		  g_signal_accumulator_true_handled, NULL,
                          hippo_common_marshal_BOOLEAN__POINTER,
            		  G_TYPE_BOOLEAN, 1, G_TYPE_POINTER);
        
        initialized = TRUE;
    }
}

void
hippo_canvas_item_sink(HippoCanvasItem    *canvas_item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->sink(canvas_item);
}

void
hippo_canvas_item_set_context(HippoCanvasItem    *canvas_item,
                              HippoCanvasContext *context)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->set_context(canvas_item, context);
}

int
hippo_canvas_item_get_width_request(HippoCanvasItem *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), 0);

    return HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_width_request(canvas_item);
}

int
hippo_canvas_item_get_height_request(HippoCanvasItem *canvas_item,
                                     int              for_width)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), 0);

    return HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_height_request(canvas_item, for_width);
}

void
hippo_canvas_item_allocate(HippoCanvasItem *canvas_item,
                           int              width,
                           int              height)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->allocate(canvas_item, width, height);
}

void
hippo_canvas_item_get_allocation(HippoCanvasItem *canvas_item,
                                 int             *width_p,
                                 int             *height_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));
    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_allocation(canvas_item, width_p, height_p);
}

void
hippo_canvas_item_get_request (HippoCanvasItem *canvas_item,
                               int             *width_p,
                               int             *height_p)
{
    int width, height;

    width = hippo_canvas_item_get_width_request(canvas_item);
    height = hippo_canvas_item_get_height_request(canvas_item, width);
    if (width_p)
        *width_p = width;
    if (height_p)
        *height_p = height;
}

gboolean
hippo_canvas_item_get_needs_resize(HippoCanvasItem *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    return HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_needs_resize(canvas_item);
}

char*
hippo_canvas_item_get_tooltip(HippoCanvasItem *canvas_item,
                              int              x,
                              int              y)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), NULL);

    return HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_tooltip(canvas_item, x, y);
}

HippoCanvasPointer
hippo_canvas_item_get_pointer(HippoCanvasItem *canvas_item,
                              int              x,
                              int              y)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    return HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_pointer(canvas_item, x, y);
}

gboolean
hippo_canvas_item_emit_button_press_event (HippoCanvasItem  *canvas_item,
                                           int               x,
                                           int               y)
{
    HippoEvent event;

    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_BUTTON_PRESS;
    event.x = x;
    event.y = y;

    return hippo_canvas_item_process_event(canvas_item, &event, 0, 0);
}

gboolean
hippo_canvas_item_emit_motion_notify_event (HippoCanvasItem  *canvas_item,
                                            int               x,
                                            int               y)
{
    HippoEvent event;

    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_MOTION_NOTIFY;
    event.x = x;
    event.y = y;

    return hippo_canvas_item_process_event(canvas_item, &event, 0, 0);
}

void
hippo_canvas_item_emit_paint_needed(HippoCanvasItem *canvas_item,
                                    int              x,
                                    int              y,
                                    int              width,
                                    int              height)
{
    if (width < 0 || height < 0) {
        int w, h;
        hippo_canvas_item_get_allocation(canvas_item, &w, &h);
        if (width < 0)
            width = w;
        if (height < 0)
            height = h;
    }
    
    g_signal_emit(canvas_item, signals[PAINT_NEEDED], 0,
                  x, y, width, height);
}

void
hippo_canvas_item_emit_request_changed(HippoCanvasItem *canvas_item)
{
    if (!hippo_canvas_item_get_needs_resize(canvas_item))
        g_signal_emit(canvas_item, signals[REQUEST_CHANGED], 0);
}

gboolean
hippo_canvas_item_process_event(HippoCanvasItem *canvas_item,
                                HippoEvent      *event,
                                int              allocation_x,
                                int              allocation_y)
{
    gboolean handled;
    HippoEvent translated;

    translated = *event;
    translated.x -= allocation_x;
    translated.y -= allocation_y;
    
    handled = FALSE;
    switch (event->type) {
    case HIPPO_EVENT_BUTTON_PRESS:
        g_signal_emit(canvas_item, signals[BUTTON_PRESS_EVENT], 0, &translated, &handled);
        break;
    case HIPPO_EVENT_MOTION_NOTIFY:
        g_signal_emit(canvas_item, signals[MOTION_NOTIFY_EVENT], 0, &translated, &handled);
        break;
        /* don't add a default, you'll break the compiler warnings */
    }

    return handled;
}

void
hippo_canvas_item_process_paint(HippoCanvasItem *canvas_item,
                                cairo_t         *cr,
                                int              allocation_x,
                                int              allocation_y)
{
    int width, height;

    cairo_save(cr);

    hippo_canvas_item_get_allocation(canvas_item, &width, &height);
    cairo_translate(cr, allocation_x, allocation_y);

    g_signal_emit(canvas_item, signals[PAINT], 0, cr);

    cairo_restore(cr);
}
