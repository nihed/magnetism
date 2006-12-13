/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <hippo/hippo-canvas-context.h>
#include <gtk/gtkcontainer.h>
#include "hippo-canvas-widget.h"
#include "hippo-canvas-helper.h"
#include <gtk/gtkprivate.h> /* for GTK_WIDGET_ALLOC_NEEDED */
#include <gtk/gtkwindow.h>
#include <gtk/gtklabel.h>

typedef struct
{
    HippoCanvasItem *item;
    GtkWidget       *widget;
} RegisteredWidgetItem;

static void hippo_canvas_helper_init       (HippoCanvasHelper       *helper);
static void hippo_canvas_helper_class_init (HippoCanvasHelperClass  *klass);
static void hippo_canvas_helper_dispose    (GObject                 *object);
static void hippo_canvas_helper_finalize   (GObject                 *object);
static void hippo_canvas_helper_iface_init (HippoCanvasContextIface *klass);


static void hippo_canvas_helper_set_property (GObject      *object,
                                              guint         prop_id,
                                              const GValue *value,
                                              GParamSpec   *pspec);
static void hippo_canvas_helper_get_property (GObject      *object,
                                              guint         prop_id,
                                              GValue       *value,
                                              GParamSpec   *pspec);

static PangoLayout*     hippo_canvas_helper_create_layout          (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_helper_load_image             (HippoCanvasContext *context,
                                                                    const char         *image_name);
static guint32          hippo_canvas_helper_get_color              (HippoCanvasContext *context,
                                                                    HippoStockColor     color);
static void             hippo_canvas_helper_register_widget_item   (HippoCanvasContext *context,
                                                                    HippoCanvasItem    *item);
static void             hippo_canvas_helper_unregister_widget_item (HippoCanvasContext *context,
                                                                    HippoCanvasItem    *item);
static void             hippo_canvas_helper_translate_to_widget    (HippoCanvasContext *context,
                                                                    HippoCanvasItem    *item,
                                                                    int                *x_p,
                                                                    int                *y_p);

static void             hippo_canvas_helper_fixup_resize_state     (HippoCanvasHelper  *canvas);

static void       tooltip_window_update   (GtkWidget  *tip,
                                           GtkWidget  *for_widget,
                                           int         root_x,
                                           int         root_y,
                                           const char *text);
static GtkWidget* tooltip_window_new      (void);



struct _HippoCanvasHelper {
    GObject parent;

    GtkWidget *widget;

    HippoCanvasItem *root;

    HippoCanvasPointer pointer;

    GtkWidget *tooltip_window;

    guint tooltip_timeout_id;
    int last_window_x;
    int last_window_y;
    
    GSList *widget_items;

    unsigned int root_hovering : 1;
    unsigned int fixing_up_resize_state : 1;
};

struct _HippoCanvasHelperClass {
    GObjectClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasHelper, hippo_canvas_helper, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT,
                                              hippo_canvas_helper_iface_init));

static void
hippo_canvas_helper_init(HippoCanvasHelper *helper)
{
    helper->pointer = HIPPO_CANVAS_POINTER_UNSET;
    helper->last_window_x = -1;
    helper->last_window_y = -1;
}

static void
hippo_canvas_helper_class_init(HippoCanvasHelperClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    
    object_class->set_property = hippo_canvas_helper_set_property;
    object_class->get_property = hippo_canvas_helper_get_property;

    object_class->dispose = hippo_canvas_helper_dispose;
    object_class->finalize = hippo_canvas_helper_finalize;
}

static void
hippo_canvas_helper_iface_init (HippoCanvasContextIface *klass)
{
    klass->create_layout = hippo_canvas_helper_create_layout;
    klass->load_image = hippo_canvas_helper_load_image;
    klass->get_color = hippo_canvas_helper_get_color;
    klass->register_widget_item = hippo_canvas_helper_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_helper_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_helper_translate_to_widget;
}

static void
cancel_tooltip(HippoCanvasHelper *helper)
{
    if (helper->tooltip_timeout_id) {
        g_source_remove(helper->tooltip_timeout_id);
        helper->tooltip_timeout_id = 0;
        if (helper->tooltip_window)
            gtk_widget_hide(helper->tooltip_window);
    }
}

static void
hippo_canvas_helper_dispose(GObject *object)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(object);

    hippo_canvas_helper_set_root(helper, NULL);

    g_assert(helper->widget_items == NULL);

    cancel_tooltip(helper);
    if (helper->tooltip_window) {
        gtk_object_destroy(GTK_OBJECT(helper->tooltip_window));
        helper->tooltip_window = NULL;
    }
    
    G_OBJECT_CLASS(hippo_canvas_helper_parent_class)->dispose(object);
}

static void
hippo_canvas_helper_finalize(GObject *object)
{
    /* HippoCanvasHelper *helper = HIPPO_CANVAS(object); */

    G_OBJECT_CLASS(hippo_canvas_helper_parent_class)->finalize(object);
}

static void
hippo_canvas_helper_set_property(GObject      *object,
                                 guint         prop_id,
                                 const GValue *value,
                                 GParamSpec   *pspec)
{
#if 0
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(object);
#endif

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_helper_get_property(GObject    *object,
                                 guint       prop_id,
                                 GValue     *value,
                                 GParamSpec *pspec)
{
#if 0
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(object);
#endif

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

HippoCanvasHelper*
hippo_canvas_helper_new(GtkContainer *base_container)
{
    HippoCanvasHelper *helper;

    g_return_val_if_fail(GTK_IS_CONTAINER(base_container), NULL);

    helper = g_object_new(HIPPO_TYPE_CANVAS_HELPER, NULL);

    helper->widget = GTK_WIDGET(base_container);

    return helper;
}

static void
set_pointer(HippoCanvasHelper *helper,
            HippoCanvasPointer pointer)
{
    GdkCursor *cursor;
    GtkWidget *widget;
    GdkWindow *event_window;
    
    /* important optimization since we do this on all motion notify */
    if (helper->pointer == pointer)
        return;

    widget = helper->widget;

    helper->pointer = pointer;

    if (pointer == HIPPO_CANVAS_POINTER_UNSET ||
        pointer == HIPPO_CANVAS_POINTER_DEFAULT)
        cursor = NULL;
    else {
        GdkCursorType type = GDK_X_CURSOR;
        switch (pointer) {
        case HIPPO_CANVAS_POINTER_HAND:
            type = GDK_HAND2;
            break;
        case HIPPO_CANVAS_POINTER_UNSET:
        case HIPPO_CANVAS_POINTER_DEFAULT:
            g_assert_not_reached();
            break;
            /* don't add a default, breaks compiler warnings */
        }
        cursor = gdk_cursor_new_for_display(gtk_widget_get_display(widget),
                                            type);
    }

    event_window = widget->window;
    gdk_window_set_cursor(event_window, cursor);
    
    gdk_display_flush(gtk_widget_get_display(widget));

    if (cursor != NULL)
        gdk_cursor_unref(cursor);
}

static void
get_root_item_window_coords(HippoCanvasHelper *helper,
                            int               *x_p,
                            int               *y_p)
{
    GtkWidget *widget = helper->widget;

    if (x_p)
        *x_p = GTK_CONTAINER(widget)->border_width;
    if (y_p)
        *y_p = GTK_CONTAINER(widget)->border_width;
    
    if (GTK_WIDGET_NO_WINDOW(widget)) {
        if (x_p)
            *x_p += widget->allocation.x;
        if (y_p)
            *y_p += widget->allocation.y;
    }
}

static void
update_tooltip(HippoCanvasHelper *helper,
               gboolean           show_if_not_already)
{
    char *tip;
    HippoRectangle for_area;
    GtkWidget *toplevel;

    if ((helper->tooltip_window == NULL || !GTK_WIDGET_VISIBLE(helper->tooltip_window)) &&
        !show_if_not_already)
        return;
    
    toplevel = gtk_widget_get_ancestor(helper->widget,
                                       GTK_TYPE_WINDOW);

    tip = NULL;
    if (helper->root != NULL &&
        toplevel && GTK_WIDGET_VISIBLE(toplevel) &&
        GTK_WIDGET_VISIBLE(helper->widget)) {
        int window_x, window_y;
        get_root_item_window_coords(helper, &window_x, &window_y);
        tip = hippo_canvas_item_get_tooltip(helper->root,
                                            helper->last_window_x - window_x,
                                            helper->last_window_y - window_y,
                                            &for_area);
        for_area.x += window_x;
        for_area.y += window_y;
    }

    if (tip != NULL) {
        int screen_x, screen_y;
        
        if (helper->tooltip_window == NULL) {
            helper->tooltip_window = tooltip_window_new();
        }

        gdk_window_get_origin(helper->widget->window, &screen_x, &screen_y);

        for_area.x += screen_x;
        for_area.y += screen_y;

        tooltip_window_update(helper->tooltip_window,
                              helper->widget,
                              for_area.x,
                              for_area.y + for_area.height,
                              tip);

        gtk_widget_show(helper->tooltip_window);
        
        g_free(tip);
    }
}

gboolean
hippo_canvas_helper_expose_event(HippoCanvasHelper *helper,
                                 GdkEventExpose    *event)
{
    
    cairo_t *cr;
    int window_x, window_y;
    HippoRectangle damage_box;

    if (helper->root == NULL)
        return FALSE;

    cr = gdk_cairo_create(event->window);
    get_root_item_window_coords(helper, &window_x, &window_y);

    damage_box.x = event->area.x;
    damage_box.y = event->area.y;
    damage_box.width = event->area.width;
    damage_box.height = event->area.height;
    hippo_canvas_item_process_paint(helper->root, cr, &damage_box,
                                    window_x, window_y);
    cairo_destroy(cr);

    return FALSE;
}

void
hippo_canvas_helper_size_request(HippoCanvasHelper *helper,
                                 GtkRequisition    *requisition)
{
    /* g_debug("gtk request on canvas root %p canvas %p", helper->root, canvas); */

    hippo_canvas_helper_fixup_resize_state(helper);
    
    requisition->width = 0;
    requisition->height = 0;

    if (helper->root != NULL) {
        hippo_canvas_item_get_request(helper->root,
                                      &requisition->width,
                                      &requisition->height);
    }

    requisition->width += GTK_CONTAINER(helper->widget)->border_width * 2;
    requisition->height += GTK_CONTAINER(helper->widget)->border_width * 2;
}

void
hippo_canvas_helper_size_allocate(HippoCanvasHelper *helper,
                                  GtkAllocation     *allocation)
{
    /* g_debug("gtk allocate on canvas root %p canvas %p", helper->root, canvas); */

    hippo_canvas_helper_fixup_resize_state(helper);
    
    if (helper->root != NULL) {
        hippo_canvas_item_allocate(helper->root,
                                   allocation->width - GTK_CONTAINER(helper->widget)->border_width * 2,
                                   allocation->height  - GTK_CONTAINER(helper->widget)->border_width * 2);

        /* Tooltip might be in the wrong place now */
        update_tooltip(helper, FALSE);
    }
}

gboolean
hippo_canvas_helper_button_press(HippoCanvasHelper *helper,
                                 GdkEventButton    *event)
{
    int window_x, window_y;
    
    if (helper->root == NULL)
        return FALSE;

    get_root_item_window_coords(helper, &window_x, &window_y);
    
    /*
    g_debug("canvas button press at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */

    hippo_canvas_item_emit_button_press_event(helper->root,
                                              event->x - window_x, event->y - window_y,
                                              event->button,
                                              event->x_root, event->y_root,
                                              event->time);

    return TRUE;
}

gboolean
hippo_canvas_helper_button_release(HippoCanvasHelper *helper,
                                   GdkEventButton    *event)
{
    int window_x, window_y;
    
    if (helper->root == NULL)
        return FALSE;

    get_root_item_window_coords(helper, &window_x, &window_y);
    
    /*
    g_debug("canvas button release at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */
    
    hippo_canvas_item_emit_button_release_event(helper->root,
                                                event->x - window_x, event->y - window_y,
                                                event->button,
                                                event->x_root, event->y_root,
                                                event->time);

    return TRUE;
}

static gboolean
tooltip_timeout(void *data)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(data);

    update_tooltip(helper, TRUE);
    
    helper->tooltip_timeout_id = 0;
    return FALSE;
}

#define TIP_DELAY (1000*1.5)

static void
add_tooltip_timeout(HippoCanvasHelper *helper,
                    int                delay)
{
    if (helper->tooltip_timeout_id != 0)
        g_source_remove(helper->tooltip_timeout_id);
    
    helper->tooltip_timeout_id = g_timeout_add(delay, tooltip_timeout, helper);
}

static void
handle_new_mouse_location(HippoCanvasHelper *helper,
                          GdkWindow         *event_window,
                          HippoMotionDetail  detail)
{
    int mouse_x, mouse_y;
    int root_x_origin, root_y_origin;
    int root_x, root_y;
    int w, h;
    gboolean was_hovering;

    if (event_window != helper->widget->window)
        return;
    
    gdk_window_get_pointer(event_window, &mouse_x, &mouse_y, NULL);

    if (mouse_x != helper->last_window_x || mouse_y != helper->last_window_y) {
        
        cancel_tooltip(helper);       
        helper->last_window_x = mouse_x;
        helper->last_window_y = mouse_y;
        add_tooltip_timeout(helper, TIP_DELAY);
    }

    get_root_item_window_coords(helper, &root_x_origin, &root_y_origin);
    root_x = mouse_x - root_x_origin;
    root_y = mouse_y - root_y_origin;
    
    hippo_canvas_item_get_allocation(helper->root, &w, &h);

#if 0
    g_debug("%p mouse %d,%d root origin %d,%d root %d,%d root size %dx%d", helper->root,
            mouse_x, mouse_y, root_x_origin, root_y_origin, root_x, root_y, w, h);
#endif

    was_hovering = helper->root_hovering;

    if (detail == HIPPO_MOTION_DETAIL_LEAVE)
        helper->root_hovering = FALSE;
    else
        helper->root_hovering = TRUE;

    /* g_debug("   was_hovering %d root_hovering %d", was_hovering, helper->root_hovering); */
    
    if (was_hovering && !helper->root_hovering) {
        set_pointer(helper, HIPPO_CANVAS_POINTER_UNSET);
        hippo_canvas_item_emit_motion_notify_event(helper->root, root_x, root_y,
                                                   HIPPO_MOTION_DETAIL_LEAVE);
    } else {
        HippoCanvasPointer pointer;
        
        pointer = hippo_canvas_item_get_pointer(helper->root, root_x, root_y);
        set_pointer(helper, pointer);
    
        if (helper->root_hovering && !was_hovering) {
            hippo_canvas_item_emit_motion_notify_event(helper->root, root_x, root_y,
                                                       HIPPO_MOTION_DETAIL_ENTER);
        } else if (helper->root_hovering) {
            hippo_canvas_item_emit_motion_notify_event(helper->root, root_x, root_y,
                                                       HIPPO_MOTION_DETAIL_WITHIN);
        }
    }
}

gboolean
hippo_canvas_helper_enter_notify(HippoCanvasHelper *helper,
                                 GdkEventCrossing  *event)
{
    HippoMotionDetail detail;

    /* g_debug("motion notify GDK ENTER on %p root %p root_hovering %d", widget, helper->root, helper->root_hovering); */
    
    if (helper->root == NULL)
        return FALSE;

    if (event->detail == GDK_NOTIFY_INFERIOR || event->window != helper->widget->window)
        detail = HIPPO_MOTION_DETAIL_WITHIN;
    else
        detail = HIPPO_MOTION_DETAIL_ENTER;
        
    handle_new_mouse_location(helper, event->window, detail);
    
    return FALSE;
}

gboolean
hippo_canvas_helper_leave_notify(HippoCanvasHelper *helper,
                                 GdkEventCrossing  *event)
{
    HippoMotionDetail detail;

    /* g_debug("motion notify GDK LEAVE on %p root %p root_hovering %d", widget, helper->root, helper->root_hovering); */
    
    if (helper->root == NULL)
        return FALSE;

    if (event->detail == GDK_NOTIFY_INFERIOR || event->window != helper->widget->window)
        detail = HIPPO_MOTION_DETAIL_WITHIN;
    else
        detail = HIPPO_MOTION_DETAIL_LEAVE;
        
    handle_new_mouse_location(helper, event->window, detail);
    
    return FALSE;
}

gboolean
hippo_canvas_helper_motion_notify(HippoCanvasHelper *helper,
                                  GdkEventMotion    *event)
{
    /* g_debug("motion notify GDK MOTION on %p root %p root_hovering %d", widget, helper->root, helper->root_hovering); */
    
    if (helper->root == NULL)
        return FALSE;

    handle_new_mouse_location(helper, event->window, HIPPO_MOTION_DETAIL_WITHIN);
    
    return FALSE;
}

void
hippo_canvas_helper_realize(HippoCanvasHelper *helper)
{
}

void
hippo_canvas_helper_unmap(HippoCanvasHelper *helper)
{
    cancel_tooltip(helper);
}

void
hippo_canvas_helper_hierarchy_changed (HippoCanvasHelper *helper,
                                       GtkWidget         *old_toplevel)
{
    cancel_tooltip(helper);
}

void
hippo_canvas_helper_add(HippoCanvasHelper *helper,
                        GtkWidget         *widget)
{
    g_warning("hippo_canvas_add called, you have to just add an item with a widget in it, you can't do gtk_container_add directly");
}

void
hippo_canvas_helper_remove(HippoCanvasHelper *helper,
                           GtkWidget         *widget)
{
    GSList *link;

    /* We go a little roundabout here - we remove the widget from the canvas
     * item, which causes us to remove it from ourselves.
     * The only time we expect gtk_container_remove to be called is from
     * gtk_object_destroy on e.g. the toplevel window, or something of
     * that nature.
     */
    
    for (link = helper->widget_items;
         link != NULL;
         link = link->next) {
        RegisteredWidgetItem *witem = link->data;

        if (witem->widget == widget) {
            g_object_set(G_OBJECT(witem->item), "widget", NULL, NULL);
            return;
        }
    }

    g_warning("tried to remove widget %p that is not in the canvas", widget);
}

void
hippo_canvas_helper_forall(HippoCanvasHelper *helper,
                           gboolean           include_internals,
                           GtkCallback        callback,
                           gpointer           callback_data)
{
    GSList *link;
    
    for (link = helper->widget_items;
         link != NULL;
         link = link->next) {
        RegisteredWidgetItem *witem = link->data;

        if (witem->widget)
            (* callback) (witem->widget, callback_data);
    }
}

GType
hippo_canvas_helper_child_type(HippoCanvasHelper *helper)
{
    /* FIXME: this is wrong, since you can't call add() */
    return GTK_TYPE_WIDGET;
}

static PangoLayout*
hippo_canvas_helper_create_layout(HippoCanvasContext *context)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(context);
    return gtk_widget_create_pango_layout(helper->widget, NULL);
}

static HippoCanvasLoadImageHook hippo_canvas_helper_load_image_hook = NULL;

void
hippo_canvas_helper_set_load_image_hook(HippoCanvasLoadImageHook hook)
{
    hippo_canvas_helper_load_image_hook = hook;
}

static cairo_surface_t*
hippo_canvas_helper_load_image(HippoCanvasContext *context,
                               const char         *image_name)
{
    if (hippo_canvas_helper_load_image_hook) {
        return hippo_canvas_helper_load_image_hook(context, image_name);
    } else {
        return NULL;
    }
}

static guint32
convert_color(GdkColor *gdk_color)
{
    guint32 rgba;
    
    rgba = gdk_color->red / 256;
    rgba <<= 8;
    rgba &= gdk_color->green / 256;
    rgba <<= 8;
    rgba &= gdk_color->blue / 256;
    rgba <<= 8;
    rgba &= 0xff; /* alpha */

    return rgba;
}

static guint32
hippo_canvas_helper_get_color(HippoCanvasContext *context,
                              HippoStockColor     color)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(context);
    GtkWidget *widget = helper->widget;
    GtkStyle *style = gtk_widget_get_style(widget);

    if (style == NULL) /* not realized yet, should not happen really */
        return 0;
    
    switch (color) {
    case HIPPO_STOCK_COLOR_BG_NORMAL:
        return convert_color(&style->bg[GTK_STATE_NORMAL]);
        break;
    case HIPPO_STOCK_COLOR_BG_PRELIGHT:
        return convert_color(&style->bg[GTK_STATE_PRELIGHT]);
        break;
    }

    g_warning("unknown stock color %d", color);
    return 0;
}

static void
update_widget(HippoCanvasHelper    *helper,
              RegisteredWidgetItem *witem)
{
    GtkWidget *new_widget;

    new_widget = NULL;
    g_object_get(G_OBJECT(witem->item), "widget", &new_widget, NULL);

    if (new_widget == witem->widget) {
        if (new_widget)
            g_object_unref(new_widget);
        return;
    }
    
    if (new_widget) {
        /* note that this ref/sinks the widget */
        gtk_widget_set_parent(new_widget, helper->widget);
    }

    if (witem->widget) {
        /* and this unrefs the widget */
        gtk_widget_unparent(witem->widget);
    }
    
    witem->widget = new_widget;

    if (new_widget)
        g_object_unref(new_widget);
}

static void
on_item_widget_changed(HippoCanvasItem *item,
                       GParamSpec      *arg,
                       void            *data)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(data);
    RegisteredWidgetItem *witem;
    GSList *link;
    
    witem = NULL;
    for (link = helper->widget_items;
         link != NULL;
         link = link->next) {
        witem = link->data;
        if (witem->item == item) {
            update_widget(helper, witem);
            return;
        }
    }

    g_warning("got widget changed for an unregistered widget item");
}

static void
add_widget_item(HippoCanvasHelper *helper,
                HippoCanvasItem   *item)
{
    RegisteredWidgetItem *witem = g_new0(RegisteredWidgetItem, 1);

    witem->item = item;
    g_object_ref(witem->item);
    helper->widget_items = g_slist_prepend(helper->widget_items, witem);

    update_widget(helper, witem);
    
    g_signal_connect(G_OBJECT(item), "notify::widget",
                     G_CALLBACK(on_item_widget_changed),
                     helper);
}

static void
remove_widget_item(HippoCanvasHelper *helper,
                   HippoCanvasItem   *item)
{
    RegisteredWidgetItem *witem;
    GSList *link;
    
    witem = NULL;
    for (link = helper->widget_items;
         link != NULL;
         link = link->next) {
        witem = link->data;
        if (witem->item == item)
            break;
    }
    if (link == NULL) {
        g_warning("removing a not-registered widget item");
        return;
    }

    helper->widget_items = g_slist_remove(helper->widget_items, witem);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(witem->item),
                                         G_CALLBACK(on_item_widget_changed),
                                         helper);
    if (witem->widget) {
        gtk_widget_unparent(witem->widget);
        witem->widget = NULL;
    }
    g_object_unref(witem->item);
    g_free(witem);
}

static void
hippo_canvas_helper_register_widget_item(HippoCanvasContext *context,
                                         HippoCanvasItem    *item)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(context);
    
    add_widget_item(helper, item);
}

static void
hippo_canvas_helper_unregister_widget_item (HippoCanvasContext *context,
                                            HippoCanvasItem    *item)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(context);

    remove_widget_item(helper, item);
}

static void
hippo_canvas_helper_translate_to_widget(HippoCanvasContext *context,
                                        HippoCanvasItem    *item,
                                        int                *x_p,
                                        int                *y_p)
{
    HippoCanvasHelper *helper = HIPPO_CANVAS_HELPER(context);
    GtkWidget *widget = helper->widget;

    /* convert coords of root canvas item to coords of
     * widget->window
     */

    if (GTK_WIDGET_NO_WINDOW(widget)) {
        if (x_p)
            *x_p += widget->allocation.x;
        if (y_p)
            *y_p += widget->allocation.y;
    }

    if (x_p)
        *x_p += GTK_CONTAINER(widget)->border_width;
    if (y_p)
        *y_p += GTK_CONTAINER(widget)->border_width;
}

static void
canvas_root_request_changed(HippoCanvasItem   *root,
                            HippoCanvasHelper *helper)
{
    /* g_debug("queuing resize on canvas root %p canvas %p canvas container %p",
       root, canvas, helper->widget->parent); */
    if (!helper->fixing_up_resize_state)
        gtk_widget_queue_resize(helper->widget);
}

static void
canvas_root_paint_needed(HippoCanvasItem      *root,
                         const HippoRectangle *damage_box,
                         HippoCanvasHelper    *helper)
{
    GtkWidget *widget = helper->widget;
    int window_x, window_y;
    
    get_root_item_window_coords(helper, &window_x, &window_y);
    
    gtk_widget_queue_draw_area(widget,
                               damage_box->x + window_x,
                               damage_box->y + window_y,
                               damage_box->width, damage_box->height);
}

static void
canvas_root_tooltip_changed(HippoCanvasItem   *root,
                            HippoCanvasHelper *helper)
{
    update_tooltip(helper, FALSE);
}

void
hippo_canvas_helper_set_root(HippoCanvasHelper *helper,
                             HippoCanvasItem   *root)
{
    g_return_if_fail(HIPPO_IS_CANVAS_HELPER(helper));
    g_return_if_fail(root == NULL || HIPPO_IS_CANVAS_ITEM(root));

    if (root == helper->root)
        return;

    if (helper->root != NULL) {
        g_signal_handlers_disconnect_by_func(helper->root,
                                             G_CALLBACK(canvas_root_request_changed),
                                             helper);
        g_signal_handlers_disconnect_by_func(helper->root,
                                             G_CALLBACK(canvas_root_paint_needed),
                                             helper);
        g_signal_handlers_disconnect_by_func(helper->root,
                                             G_CALLBACK(canvas_root_tooltip_changed),
                                             helper);
        hippo_canvas_item_set_context(helper->root, NULL);
        g_object_unref(helper->root);
        helper->root = NULL;
    }

    if (root != NULL) {
        g_object_ref(root);
        hippo_canvas_item_sink(root);
        helper->root = root;
        g_signal_connect(root, "request-changed",
                         G_CALLBACK(canvas_root_request_changed),
                         helper);
        g_signal_connect(root, "paint-needed",
                         G_CALLBACK(canvas_root_paint_needed),
                         helper);
        g_signal_connect(root, "tooltip-changed",
                         G_CALLBACK(canvas_root_tooltip_changed),
                         helper);
        hippo_canvas_item_set_context(helper->root, HIPPO_CANVAS_CONTEXT(helper));
    }

    gtk_widget_queue_resize(helper->widget);
}

/*
 * This is a bad hack because GTK does not have a "resize queued" signal
 * like our request-changed; this means that if a widget inside a HippoCanvasWidget
 * queues a resize, the HippoCanvasWidget does not emit request-changed.
 *
 * Because all canvas widget items are registered with the HippoCanvas widget
 * they are inside, when we get the GTK size_request or size_allocate,
 * we go through and emit the missing request-changed before we request/allocate
 * the root canvas item.
 */
static void
hippo_canvas_helper_fixup_resize_state(HippoCanvasHelper *helper)
{
    RegisteredWidgetItem *witem;
    GSList *link;

    if (helper->fixing_up_resize_state) {
        g_warning("Recursion in %s", G_GNUC_PRETTY_FUNCTION);
        return;
    }
    
    helper->fixing_up_resize_state = TRUE;
    
    witem = NULL;
    for (link = helper->widget_items;
         link != NULL;
         link = link->next) {
        witem = link->data;

        if (witem->widget &&
            (GTK_WIDGET_REQUEST_NEEDED(witem->widget) ||
             GTK_WIDGET_ALLOC_NEEDED(witem->widget))) {
            hippo_canvas_item_emit_request_changed(witem->item);
        }
    }

    helper->fixing_up_resize_state = FALSE;
}

static gint
tooltip_expose_handler(GtkWidget *tip, GdkEventExpose *event, void *data)
{
    gtk_paint_flat_box(tip->style, tip->window,
                       GTK_STATE_NORMAL, GTK_SHADOW_OUT, 
                       &event->area, tip, "tooltip",
                       0, 0, -1, -1);
    
    return FALSE;
}

static gint
tooltip_motion_handler(GtkWidget *tip, GdkEventMotion *event, void *data)
{
    gtk_widget_hide(tip);
    return FALSE;
}

static void
tooltip_window_update(GtkWidget  *tip,
                      GtkWidget  *for_widget,
                      int         root_x,
                      int         root_y,
                      const char *text)
{
    GdkScreen *gdk_screen;
    GdkRectangle monitor;
    gint mon_num;
    int w, h;
    GtkWidget *label;
    int screen_right_edge;
    int screen_bottom_edge;
    
    gdk_screen = gtk_widget_get_screen(for_widget);
    
    gtk_window_set_screen(GTK_WINDOW(tip), gdk_screen);
    mon_num = gdk_screen_get_monitor_at_point(gdk_screen, root_x, root_y);
    gdk_screen_get_monitor_geometry(gdk_screen, mon_num, &monitor);
    screen_right_edge = monitor.x + monitor.width;
    screen_bottom_edge = monitor.y + monitor.height;

    label = GTK_BIN(tip)->child;
    
    gtk_label_set(GTK_LABEL(label), text);
    
    gtk_window_get_size(GTK_WINDOW(tip), &w, &h);
    if((root_x + w) > screen_right_edge)
        root_x -= (root_x + w) - screen_right_edge;
    
    gtk_window_move(GTK_WINDOW(tip), root_x, root_y);
}

static GtkWidget*
tooltip_window_new(void)
{
    GtkWidget *tip;
    GtkWidget *label;
    
    tip = gtk_window_new(GTK_WINDOW_POPUP);
    
    gtk_widget_set_app_paintable(tip, TRUE);
    gtk_window_set_policy(GTK_WINDOW(tip), FALSE, FALSE, TRUE);
    gtk_widget_set_name(tip, "gtk-tooltips");
    gtk_container_set_border_width(GTK_CONTAINER(tip), 4);
    
    g_signal_connect(tip, "expose-event",
                     G_CALLBACK(tooltip_expose_handler), NULL);
    g_signal_connect(tip, "motion-notify-event",
                     G_CALLBACK(tooltip_motion_handler), NULL);
    
    label = gtk_label_new(NULL);
    gtk_label_set_line_wrap(GTK_LABEL(label), TRUE);
    gtk_misc_set_alignment(GTK_MISC(label), 0.5, 0.5);
    gtk_widget_show(label);
    
    gtk_container_add(GTK_CONTAINER(tip), label);
    
    return tip;
}
