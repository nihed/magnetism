/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-item.h"
#include <hippo/hippo-common-marshal.h>

static void     hippo_canvas_item_base_init (void                  *klass);
static gboolean boolean_handled_accumulator (GSignalInvocationHint *ihint,
                                             GValue                *return_accumulated,
                                             const GValue          *handler_return,
                                             gpointer               dummy);


enum {
    REQUEST_CHANGED,      /* The size we want to request may have changed */
    BUTTON_PRESS_EVENT,
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
        signals[REQUEST_CHANGED] =
            g_signal_new ("request-changed",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, request_changed),
            		  NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);

        signals[BUTTON_PRESS_EVENT] =
            g_signal_new ("button-press-event",
                          HIPPO_TYPE_CANVAS_ITEM,
            		  G_SIGNAL_RUN_LAST,
            		  G_STRUCT_OFFSET(HippoCanvasItemClass, button_press_event),
            		  boolean_handled_accumulator, NULL,
                          hippo_common_marshal_BOOLEAN__POINTER,
            		  G_TYPE_BOOLEAN, 1, G_TYPE_POINTER);

        initialized = TRUE;
    }
}

void
hippo_canvas_item_paint(HippoCanvasItem *canvas_item,
                        HippoDrawable   *drawable)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->paint(canvas_item, drawable);
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
                           int              x,
                           int              y,
                           int              width,
                           int              height)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->allocate(canvas_item, x, y, width, height);
}

void
hippo_canvas_item_get_allocation(HippoCanvasItem *canvas_item,
                                 int             *x_p,
                                 int             *y_p,
                                 int             *width_p,
                                 int             *height_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));
    HIPPO_CANVAS_ITEM_GET_CLASS(canvas_item)->get_allocation(canvas_item, x_p, y_p, width_p, height_p);
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

    return hippo_canvas_item_process_event(canvas_item, &event);
}

void
hippo_canvas_item_emit_request_changed(HippoCanvasItem *canvas_item)
{
    if (!hippo_canvas_item_get_needs_resize(canvas_item))
        g_signal_emit(canvas_item, signals[REQUEST_CHANGED], 0);
}

static gboolean
boolean_handled_accumulator(GSignalInvocationHint *ihint,
                            GValue                *return_accumulated,
                            const GValue          *handler_return,
                            gpointer               dummy)
{
    gboolean continue_emission;
    gboolean signal_handled;

    signal_handled = g_value_get_boolean (handler_return);
    g_value_set_boolean (return_accumulated, signal_handled);
    continue_emission = !signal_handled;

    return continue_emission;
}

gboolean
hippo_canvas_item_process_event(HippoCanvasItem *canvas_item,
                                HippoEvent      *event)
{
    gboolean handled;

    handled = FALSE;
    switch (event->type) {
    case HIPPO_EVENT_BUTTON_PRESS:
        g_signal_emit(canvas_item, signals[BUTTON_PRESS_EVENT], 0, &event, &handled);
        break;
        /* don't add a default, you'll break the compiler warnings */
    }

    return handled;
}
