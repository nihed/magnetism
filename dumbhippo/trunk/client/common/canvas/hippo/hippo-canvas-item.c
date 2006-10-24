/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-item.h"
#include "hippo-canvas-marshal.h"

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
                sizeof(HippoCanvasItemIface),
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
                          G_STRUCT_OFFSET(HippoCanvasItemIface, paint),
                          NULL, NULL,
                          hippo_canvas_marshal_VOID__POINTER_BOXED,
                          G_TYPE_NONE, 2, G_TYPE_POINTER, HIPPO_TYPE_RECTANGLE);
        signals[REQUEST_CHANGED] =
            g_signal_new ("request-changed",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, request_changed),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);
        signals[PAINT_NEEDED] =
            g_signal_new ("paint-needed",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, paint_needed),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__BOXED,
                          G_TYPE_NONE, 1, HIPPO_TYPE_RECTANGLE);
        signals[BUTTON_PRESS_EVENT] =
            g_signal_new ("button-press-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, button_press_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                      G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        signals[BUTTON_RELEASE_EVENT] =
            g_signal_new ("button-release-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, button_release_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                          G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        signals[MOTION_NOTIFY_EVENT] =
            g_signal_new ("motion-notify-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, motion_notify_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                          G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        signals[ACTIVATED] =
            g_signal_new ("activated",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, activated),
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

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->sink(canvas_item);
}

void
hippo_canvas_item_set_context(HippoCanvasItem    *canvas_item,
                              HippoCanvasContext *context)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->set_context(canvas_item, context);
}

int
hippo_canvas_item_get_width_request(HippoCanvasItem *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), 0);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_width_request(canvas_item);
}

/* returns -1 to just use the width request. The natural width should
 * be thought of as the width at which alignment
 * (HIPPO_ALIGNMENT_START etc.)  makes no difference but at which
 * nothing will be chopped off or wrapped.  There is no real guarantee
 * a container won't give an item more than the natural, this is just
 * a hint for containers that can do something useful with it, or
 * something.
 */
int
hippo_canvas_item_get_natural_width(HippoCanvasItem *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), -1);
    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_natural_width(canvas_item);
}

int
hippo_canvas_item_get_height_request(HippoCanvasItem *canvas_item,
                                     int              for_width)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), 0);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_height_request(canvas_item, for_width);
}

void
hippo_canvas_item_allocate(HippoCanvasItem *canvas_item,
                           int              width,
                           int              height)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->allocate(canvas_item, width, height);

    /* GTK doesn't let us assume this, e.g. GtkScrolledWindow will queue
     * requests from its allocate. But it's supposed to be true for
     * canvas items.
     */
    if (hippo_canvas_item_get_needs_request(canvas_item))
        g_warning("Item %s %p still needs resize after being allocated",
                  g_type_name_from_instance((GTypeInstance*) canvas_item),
                  canvas_item);
}

void
hippo_canvas_item_get_allocation(HippoCanvasItem *canvas_item,
                                 int             *width_p,
                                 int             *height_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));
    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_allocation(canvas_item, width_p, height_p);
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
hippo_canvas_item_get_needs_request(HippoCanvasItem *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_needs_request(canvas_item);
}

char*
hippo_canvas_item_get_tooltip(HippoCanvasItem *canvas_item,
                              int              x,
                              int              y,
                              HippoRectangle  *for_area)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), NULL);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_tooltip(canvas_item, x, y, for_area);
}

HippoCanvasPointer
hippo_canvas_item_get_pointer(HippoCanvasItem *canvas_item,
                              int              x,
                              int              y)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_pointer(canvas_item, x, y);
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
    HippoRectangle damage_box;

    damage_box.x = x;
    damage_box.y = y;
    damage_box.width = width;
    damage_box.height = height;

    if (width < 0 || height < 0) {
        int w, h;
        hippo_canvas_item_get_allocation(canvas_item, &w, &h);
        if (width < 0)
            damage_box.width = w;
        if (height < 0)
            damage_box.height = h;
    }

    g_signal_emit(canvas_item, signals[PAINT_NEEDED], 0,
                  &damage_box);
}

void
hippo_canvas_item_emit_request_changed(HippoCanvasItem *canvas_item)
{
    if (!hippo_canvas_item_get_needs_request(canvas_item)) {
#if 0
        g_debug("Item %s %p now needs resize, emitting request-changed",
                g_type_name_from_instance((GTypeInstance*) canvas_item),
                canvas_item);
#endif
        
        g_signal_emit(canvas_item, signals[REQUEST_CHANGED], 0);
        
        if (!hippo_canvas_item_get_needs_request(canvas_item))
            g_warning("Item %s %p does not need resize after emitting request-changed",
                      g_type_name_from_instance((GTypeInstance*) canvas_item),
                      canvas_item);
    } else {
#if 0
        g_debug("Item %s %p already needs resize, not emitting request-changed",
                g_type_name_from_instance((GTypeInstance*) canvas_item),
                canvas_item);
#endif
    }
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
