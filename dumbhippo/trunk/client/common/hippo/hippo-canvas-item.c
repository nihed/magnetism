/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-item.h"
#include "hippo-common-marshal.h"

static void     hippo_canvas_item_base_init (void                  *klass);

enum {
    PAINT,
    REQUEST_CHANGED,      /* The size we want to request may have changed */
    PAINT_NEEDED,
    BUTTON_PRESS_EVENT,
    BUTTON_RELEASE_EVENT,
    MOTION_NOTIFY_EVENT,
    ACTIVATED,
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
                          hippo_common_marshal_VOID__POINTER_POINTER,
                          G_TYPE_NONE, 2, G_TYPE_POINTER, G_TYPE_POINTER);
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
        signals[BUTTON_RELEASE_EVENT] =
            g_signal_new ("button-release-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, button_release_event),
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
        signals[ACTIVATED] =
            g_signal_new ("activated",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemClass, activated),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);        
        
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
                                           int               y,
                                           int               button,
                                           int               x11_x_root,
                                           int               x11_y_root,
                                           guint32           x11_time)
{
    HippoEvent event;

    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_BUTTON_PRESS;
    event.x = x;
    event.y = y;
    event.u.button.button = button;
    event.u.button.x11_x_root = x11_x_root;
    event.u.button.x11_y_root = x11_y_root;
    event.u.button.x11_time = x11_time;
    
    return hippo_canvas_item_process_event(canvas_item, &event, 0, 0);
}

gboolean
hippo_canvas_item_emit_button_release_event(HippoCanvasItem  *canvas_item,
                                            int               x,
                                            int               y,
                                            int               button,
                                            int               x11_x_root,
                                            int               x11_y_root,
                                            guint32           x11_time)
{
    HippoEvent event;

    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_BUTTON_RELEASE;
    event.x = x;
    event.y = y;
    event.u.button.button = button;
    event.u.button.x11_x_root = x11_x_root;
    event.u.button.x11_y_root = x11_y_root;
    event.u.button.x11_time = x11_time;
    
    return hippo_canvas_item_process_event(canvas_item, &event, 0, 0);
}

gboolean
hippo_canvas_item_emit_motion_notify_event (HippoCanvasItem  *canvas_item,
                                            int               x,
                                            int               y,
                                            HippoMotionDetail detail)
{
    HippoEvent event;
    gboolean result;
    
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_MOTION_NOTIFY;
    event.x = x;
    event.y = y;    
    event.u.motion.detail = detail;
    
    result = hippo_canvas_item_process_event(canvas_item, &event, 0, 0);

    return result;
}


void
hippo_canvas_item_emit_activated(HippoCanvasItem *canvas_item)
{
    g_signal_emit(canvas_item, signals[ACTIVATED], 0);
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
    case HIPPO_EVENT_BUTTON_RELEASE:
        g_signal_emit(canvas_item, signals[BUTTON_RELEASE_EVENT], 0, &translated, &handled);
        break;
    case HIPPO_EVENT_MOTION_NOTIFY:
        g_signal_emit(canvas_item, signals[MOTION_NOTIFY_EVENT], 0, &translated, &handled);
        break;
        /* don't add a default, you'll break the compiler warnings */
    }

    return handled;
}

// the cairo_t and damaged_box are in the coordinate system in which canvas_item
// is at (allocation_x, allocation_y), so they are both translated before
// passing them to the child
void
hippo_canvas_item_process_paint(HippoCanvasItem *canvas_item,
                                cairo_t         *cr,
                                HippoRectangle  *damaged_box,
                                int              allocation_x,
                                int              allocation_y)
{
    HippoRectangle item_box;
    HippoRectangle translated_box;
    
    /* HippoCanvasItem::paint() is guaranteed to not be called if an item has any 0 allocation,
     * that invariant should be maintained here.
     */
    
    item_box.x = allocation_x;
    item_box.y = allocation_y;
    hippo_canvas_item_get_allocation(canvas_item, &item_box.width, &item_box.height);
    
    if (hippo_rectangle_intersect(damaged_box, &item_box, &translated_box)) {
        translated_box.x -= allocation_x;
        translated_box.y -= allocation_y;

        g_assert(translated_box.x >= 0);
        g_assert(translated_box.y >= 0);
        g_assert(translated_box.width > 0);
        g_assert(translated_box.height > 0);

        cairo_save(cr);
        
        cairo_translate(cr, allocation_x, allocation_y);
        
        g_signal_emit(canvas_item, signals[PAINT], 0, cr, &translated_box);
        
        cairo_restore(cr);
    }
}
