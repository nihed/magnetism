/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-item.h"
#include "hippo-canvas-container.h"
#include "hippo-canvas-marshal.h"

static void     hippo_canvas_item_base_init (void                  *klass);

enum {
    PAINT,
    REQUEST_CHANGED,      /* The size we want to request may have changed */
    PAINT_NEEDED,
    BUTTON_PRESS_EVENT,
    BUTTON_RELEASE_EVENT,
    MOTION_NOTIFY_EVENT,
    KEY_PRESS_EVENT,
    ACTIVATED,
    TOOLTIP_CHANGED,
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

        /**
         * HippoCanvasItem::paint
         *
         * All drawing of a canvas item happens in the handlers for
         * this signal. The rectangle is the bounding box of the
         * damage region. Most concrete items derive from #HippoCanvasBox,
         * whose default paint handler invokes a series of more fine-grained
         * paint handlers to paint the background, content, etc.; usually you
         * should override one of those fine-grained handlers rather than this
         * all-encompassing paint.
         */
        signals[PAINT] =
            g_signal_new ("paint",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, paint),
                          NULL, NULL,
                          hippo_canvas_marshal_VOID__POINTER_BOXED,
                          G_TYPE_NONE, 2, G_TYPE_POINTER, HIPPO_TYPE_RECTANGLE);
        /**
         * HippoCanvasItem::request-changed
         * Signal emitted when the natural or minimum size of the canvas item
         * may have changed. The parent canvas or parent canvas item will normally
         * need to recompute its layout in response.
         */
        signals[REQUEST_CHANGED] =
            g_signal_new ("request-changed",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, request_changed),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);
        /**
         * HippoCanvasItem::paint-needed
         * Signal emitted when a canvas item needs to be repainted. The
         * rectangle is the bounding box of the areas that need repainting.
         */
        signals[PAINT_NEEDED] =
            g_signal_new ("paint-needed",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, paint_needed),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__BOXED,
                          G_TYPE_NONE, 1, HIPPO_TYPE_RECTANGLE);
        /**
         * HippoCanvasItem::button-press-event
         * Signal emitted when a mouse button is pressed down on the canvas item.
         */
        signals[BUTTON_PRESS_EVENT] =
            g_signal_new ("button-press-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, button_press_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                      G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        /**
         * HippoCanvasItem::button-release-event
         * Signal emitted when a mouse button is released on the canvas item.
         */        
        signals[BUTTON_RELEASE_EVENT] =
            g_signal_new ("button-release-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, button_release_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                          G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        /**
         * HippoCanvasItem::motion-notify-event
         * Signal emitted when the mouse pointer enters, leaves, or moves within
         * a canvas item. Note that unlike #GtkWidget, there are not separate
         * events for enter and leave.
         */                
        signals[MOTION_NOTIFY_EVENT] =
            g_signal_new ("motion-notify-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, motion_notify_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                          G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        /**
         * HippoCanvasItem::key-press-event
         *
         * Signal emitted when a key is pressed while the canvas item is focused.
         */
        signals[KEY_PRESS_EVENT] =
            g_signal_new ("key-press-event",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, key_press_event),
                          g_signal_accumulator_true_handled, NULL,
                          hippo_canvas_marshal_BOOLEAN__BOXED,
                          G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
        /**
         * HippoCanvasItem::activated
         *
         * Signal emitted when the canvas item is "activated" (e.g. if a button is clicked or
         * an url is clicked).
         */        
        signals[ACTIVATED] =
            g_signal_new ("activated",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, activated),
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);
        /**
         * HippoCanvasItem::tooltip-changed
         *
         * Signal emitted when the canvas item's tooltip changes. The code displaying the
         * tooltip may need this signal in order to update in response to changes.
         */                
        signals[TOOLTIP_CHANGED] =
            g_signal_new ("tooltip-changed",
                          HIPPO_TYPE_CANVAS_ITEM,
                          G_SIGNAL_RUN_LAST,
                          G_STRUCT_OFFSET(HippoCanvasItemIface, tooltip_changed),
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

void
hippo_canvas_item_set_parent(HippoCanvasItem      *canvas_item,
                             HippoCanvasContainer *container)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->set_parent(canvas_item, container);
}

HippoCanvasContainer*
hippo_canvas_item_get_parent(HippoCanvasItem      *canvas_item)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), NULL);

    return HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_parent(canvas_item);
}

/* The natural width should be thought of as the width at which
 * alignment (HIPPO_ALIGNMENT_START etc.) makes no difference but at
 * which nothing will be chopped off or wrapped.  There is no real
 * guarantee a container won't give an item more than the natural,
 * this is just a hint for containers that can do something useful
 * with it, like giving extra space to some other child that can use it.
 */
void
hippo_canvas_item_get_width_request(HippoCanvasItem  *canvas_item,
                                    int              *min_width_p,
                                    int              *natural_width_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_width_request(canvas_item, min_width_p, natural_width_p);
}

void
hippo_canvas_item_get_height_request(HippoCanvasItem  *canvas_item,
                                     int               for_width,
                                     int              *min_height_p,
                                     int              *natural_height_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_height_request(canvas_item, for_width,
                                                                 min_height_p, natural_height_p);
}

void
hippo_canvas_item_allocate(HippoCanvasItem *canvas_item,
                           int              width,
                           int              height,
                           gboolean         origin_changed)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->allocate(canvas_item, width, height, origin_changed);
}

void
hippo_canvas_item_get_allocation(HippoCanvasItem *canvas_item,
                                 int             *width_p,
                                 int             *height_p)
{
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));
    HIPPO_CANVAS_ITEM_GET_IFACE(canvas_item)->get_allocation(canvas_item, width_p, height_p);
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
hippo_canvas_item_get_visible(HippoCanvasItem    *canvas_item)
{
    HippoCanvasContainer *parent;
    
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    parent = hippo_canvas_item_get_parent(canvas_item);
    if (parent == NULL) {
        g_warning("Visibility is a property of the container+item pair, not just the item; so you can't get visibility on an item that isn't in a container");
        return FALSE;
    }

    return hippo_canvas_container_get_child_visible(parent, canvas_item);
}

void
hippo_canvas_item_set_visible(HippoCanvasItem    *canvas_item,
                              gboolean            visible)
{
    HippoCanvasContainer *parent;
    
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item));

    parent = hippo_canvas_item_get_parent(canvas_item);
    if (parent == NULL) {
        g_warning("Visibility is a property of the container+item pair, not just the item; so you can't set visibility on an item that isn't in a container");
        return;
    }

    hippo_canvas_container_set_child_visible(parent, canvas_item, visible != FALSE);
}

gboolean
hippo_canvas_item_emit_button_press_event (HippoCanvasItem  *canvas_item,
                                           int               x,
                                           int               y,
                                           int               button,
                                           int               x11_x_root,
                                           int               x11_y_root,
                                           guint32           x11_time,
                                           int               count)
{
    HippoEvent event;

    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_BUTTON_PRESS;
    event.x = x;
    event.y = y;
    event.u.button.button = button;
    event.u.button.count = count;
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


gboolean
hippo_canvas_item_emit_key_press_event (HippoCanvasItem  *canvas_item,
                                        HippoKey          key,
                                        gunichar          character,
                                        guint             modifiers)
{
    HippoEvent event;
    gboolean result;
    
    g_return_val_if_fail(HIPPO_IS_CANVAS_ITEM(canvas_item), FALSE);

    event.type = HIPPO_EVENT_KEY_PRESS;
    event.x = 0;
    event.y = 0;    
    event.u.key.key = key;
    event.u.key.character = character;
    event.u.key.modifiers = modifiers;
    
    result = hippo_canvas_item_process_event(canvas_item, &event, 0, 0);

    return result;
}


void
hippo_canvas_item_emit_activated(HippoCanvasItem *canvas_item)
{
    g_signal_emit(canvas_item, signals[ACTIVATED], 0);
}

void
hippo_canvas_item_emit_tooltip_changed(HippoCanvasItem *canvas_item)
{
    g_signal_emit(canvas_item, signals[TOOLTIP_CHANGED], 0);
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
    case HIPPO_EVENT_KEY_PRESS:
        g_signal_emit(canvas_item, signals[KEY_PRESS_EVENT], 0, &translated, &handled);
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

#if 0
        hippo_cairo_set_source_rgba32(cr, 0x0000aa27);
        cairo_rectangle(cr, 0, 0, item_box.width, item_box.height);
        cairo_fill(cr);
#endif
        
        g_signal_emit(canvas_item, signals[PAINT], 0, cr, &translated_box);
        
        cairo_restore(cr);
    }
}
