/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#include <hippo/hippo-canvas-context.h>
#include <gtk/gtkcontainer.h>
#include "hippo-canvas-widget.h"
#include <gtk/gtkprivate.h> /* for GTK_WIDGET_ALLOC_NEEDED */
#include <gtk/gtkwindow.h>
#include <gtk/gtklabel.h>

typedef struct
{
    HippoCanvasItem *item;
    GtkWidget       *widget;
} RegisteredWidgetItem;

static void hippo_canvas_init       (HippoCanvas             *canvas);
static void hippo_canvas_class_init (HippoCanvasClass        *klass);
static void hippo_canvas_dispose    (GObject                 *object);
static void hippo_canvas_finalize   (GObject                 *object);
static void hippo_canvas_iface_init (HippoCanvasContextIface *klass);


static void hippo_canvas_set_property (GObject      *object,
                                       guint         prop_id,
                                       const GValue *value,
                                       GParamSpec   *pspec);
static void hippo_canvas_get_property (GObject      *object,
                                       guint         prop_id,
                                       GValue       *value,
                                       GParamSpec   *pspec);

static gboolean  hippo_canvas_expose_event        (GtkWidget         *widget,
            	       	                           GdkEventExpose    *event);
static void      hippo_canvas_size_request        (GtkWidget         *widget,
            	       	                           GtkRequisition    *requisition);
static void      hippo_canvas_size_allocate       (GtkWidget         *widget,
            	       	                           GtkAllocation     *allocation);
static gboolean  hippo_canvas_button_press        (GtkWidget         *widget,
            	       	                           GdkEventButton    *event);
static gboolean  hippo_canvas_button_release      (GtkWidget         *widget,
            	       	                           GdkEventButton    *event);
static gboolean  hippo_canvas_enter_notify        (GtkWidget         *widget,
            	       	                           GdkEventCrossing  *event);
static gboolean  hippo_canvas_leave_notify        (GtkWidget         *widget,
            	       	                           GdkEventCrossing  *event);
static gboolean  hippo_canvas_motion_notify       (GtkWidget         *widget,
            	       	                           GdkEventMotion    *event);

static void  hippo_canvas_realize    (GtkWidget    *widget);
static void  hippo_canvas_add        (GtkContainer *container,
                                      GtkWidget    *widget);
static void  hippo_canvas_remove     (GtkContainer *container,
                                      GtkWidget    *widget);
static void  hippo_canvas_forall     (GtkContainer *container,
                                      gboolean      include_internals,
                                      GtkCallback   callback,
                                      gpointer      callback_data);
static GType hippo_canvas_child_type (GtkContainer *container);



static PangoLayout*     hippo_canvas_create_layout          (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_load_image             (HippoCanvasContext *context,
                                                             const char         *image_name);
static guint32          hippo_canvas_get_color              (HippoCanvasContext *context,
                                                             HippoStockColor     color);
static void             hippo_canvas_register_widget_item   (HippoCanvasContext *context,
                                                             HippoCanvasItem    *item);
static void             hippo_canvas_unregister_widget_item (HippoCanvasContext *context,
                                                             HippoCanvasItem    *item);
static void             hippo_canvas_translate_to_widget    (HippoCanvasContext *context,
                                                             HippoCanvasItem    *item,
                                                             int                *x_p,
                                                             int                *y_p);

static void             hippo_canvas_fixup_resize_state     (HippoCanvas        *canvas);

static void       tooltip_window_show (GtkWidget  *tip,
                                       GtkWidget  *for_widget,
                                       int         root_x,
                                       int         root_y,
                                       const char *text);
static GtkWidget* tooltip_window_new  (void);


struct _HippoCanvas {
    GtkContainer parent;

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

struct _HippoCanvasClass {
    GtkContainerClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvas, hippo_canvas, GTK_TYPE_CONTAINER,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT,
                                              hippo_canvas_iface_init));

static void
hippo_canvas_init(HippoCanvas *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);

    gtk_widget_add_events(widget, GDK_POINTER_MOTION_MASK | GDK_POINTER_MOTION_HINT_MASK |
                          GDK_ENTER_NOTIFY_MASK | GDK_LEAVE_NOTIFY_MASK | GDK_BUTTON_PRESS);

    canvas->pointer = HIPPO_CANVAS_POINTER_UNSET;
    canvas->last_window_x = -1;
    canvas->last_window_y = -1;
}

static void
hippo_canvas_class_init(HippoCanvasClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(klass);
    GtkContainerClass *container_class = GTK_CONTAINER_CLASS(klass);
    
    object_class->set_property = hippo_canvas_set_property;
    object_class->get_property = hippo_canvas_get_property;

    object_class->dispose = hippo_canvas_dispose;
    object_class->finalize = hippo_canvas_finalize;

    widget_class->expose_event = hippo_canvas_expose_event;
    widget_class->size_request = hippo_canvas_size_request;
    widget_class->size_allocate = hippo_canvas_size_allocate;
    widget_class->button_press_event = hippo_canvas_button_press;
    widget_class->button_release_event = hippo_canvas_button_release;
    widget_class->motion_notify_event = hippo_canvas_motion_notify;
    widget_class->enter_notify_event = hippo_canvas_enter_notify;
    widget_class->leave_notify_event = hippo_canvas_leave_notify;

    widget_class->realize = hippo_canvas_realize;

    container_class->add = hippo_canvas_add;
    container_class->remove = hippo_canvas_remove;
    container_class->forall = hippo_canvas_forall;
    container_class->child_type = hippo_canvas_child_type;
}

static void
hippo_canvas_iface_init (HippoCanvasContextIface *klass)
{
    klass->create_layout = hippo_canvas_create_layout;
    klass->load_image = hippo_canvas_load_image;
    klass->get_color = hippo_canvas_get_color;
    klass->register_widget_item = hippo_canvas_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_translate_to_widget;
}

static void
cancel_tooltip(HippoCanvas *canvas)
{
    if (canvas->tooltip_timeout_id) {
        g_source_remove(canvas->tooltip_timeout_id);
        canvas->tooltip_timeout_id = 0;
    }
}

static void
hippo_canvas_dispose(GObject *object)
{
    HippoCanvas *canvas = HIPPO_CANVAS(object);

    hippo_canvas_set_root(canvas, NULL);

    g_assert(canvas->widget_items == NULL);

    cancel_tooltip(canvas);
    
    G_OBJECT_CLASS(hippo_canvas_parent_class)->dispose(object);
}

static void
hippo_canvas_finalize(GObject *object)
{
    /* HippoCanvas *canvas = HIPPO_CANVAS(object); */

    G_OBJECT_CLASS(hippo_canvas_parent_class)->finalize(object);
}

static void
hippo_canvas_set_property(GObject         *object,
                          guint            prop_id,
                          const GValue    *value,
                          GParamSpec      *pspec)
{
    HippoCanvas *canvas;

    canvas = HIPPO_CANVAS(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_get_property(GObject         *object,
                          guint            prop_id,
                          GValue          *value,
                          GParamSpec      *pspec)
{
    HippoCanvas *canvas;

    canvas = HIPPO_CANVAS (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

GtkWidget*
hippo_canvas_new(void)
{
    HippoCanvas *canvas;

    canvas = g_object_new(HIPPO_TYPE_CANVAS, NULL);

    return GTK_WIDGET(canvas);
}

static void
set_pointer(HippoCanvas       *canvas,
            HippoCanvasPointer pointer)
{
    GdkCursor *cursor;
    GtkWidget *widget;
    GdkWindow *event_window;
    
    /* important optimization since we do this on all motion notify */
    if (canvas->pointer == pointer)
        return;

    widget = GTK_WIDGET(canvas);

    canvas->pointer = pointer;

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
get_root_item_window_coords(HippoCanvas *canvas,
                            int         *x_p,
                            int         *y_p)
{
    GtkWidget *widget = GTK_WIDGET(canvas);

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

static gboolean
hippo_canvas_expose_event(GtkWidget         *widget,
                          GdkEventExpose    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    cairo_t *cr;
    int window_x, window_y;
    HippoRectangle damage_box;

    if (canvas->root == NULL)
        return FALSE;

    cr = gdk_cairo_create(event->window);
    get_root_item_window_coords(canvas, &window_x, &window_y);

    damage_box.x = event->area.x;
    damage_box.y = event->area.y;
    damage_box.width = event->area.width;
    damage_box.height = event->area.height;
    hippo_canvas_item_process_paint(canvas->root, cr, &damage_box,
                                    window_x, window_y);
    cairo_destroy(cr);

    /* default GtkContainer::expose_event will use forall
     * to draw the child widget items
     */
    GTK_WIDGET_CLASS(hippo_canvas_parent_class)->expose_event(widget, event);
    
    return FALSE;
}

static void
hippo_canvas_size_request(GtkWidget         *widget,
                          GtkRequisition    *requisition)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* g_debug("gtk request on canvas root %p canvas %p", canvas->root, canvas); */

    hippo_canvas_fixup_resize_state(canvas);
    
    requisition->width = 0;
    requisition->height = 0;

    if (canvas->root != NULL) {
        hippo_canvas_item_get_request(canvas->root,
                                      &requisition->width,
                                      &requisition->height);
    }

    requisition->width += GTK_CONTAINER(widget)->border_width * 2;
    requisition->height += GTK_CONTAINER(widget)->border_width * 2;
}

static void
hippo_canvas_size_allocate(GtkWidget         *widget,
                           GtkAllocation     *allocation)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* g_debug("gtk allocate on canvas root %p canvas %p", canvas->root, canvas); */

    hippo_canvas_fixup_resize_state(canvas);
    
    widget->allocation = *allocation;
    
    if (GTK_WIDGET_REALIZED(widget))
        gdk_window_move_resize(widget->window,
                               allocation->x, 
                               allocation->y,
                               allocation->width, 
                               allocation->height);    

    if (canvas->root != NULL) {
        hippo_canvas_item_allocate(canvas->root,
                                   allocation->width - GTK_CONTAINER(widget)->border_width * 2,
                                   allocation->height  - GTK_CONTAINER(widget)->border_width * 2);
    }
}

static gboolean
hippo_canvas_button_press(GtkWidget         *widget,
                          GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    int window_x, window_y;
    
    if (canvas->root == NULL)
        return FALSE;

    get_root_item_window_coords(canvas, &window_x, &window_y);
    
    /*
    g_debug("canvas button press at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */
    
    return hippo_canvas_item_emit_button_press_event(canvas->root,
                                                     event->x - window_x, event->y - window_y,
                                                     event->button,
                                                     event->x_root, event->y_root,
                                                     event->time);
}

static gboolean
hippo_canvas_button_release(GtkWidget         *widget,
                            GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    int window_x, window_y;
    
    if (canvas->root == NULL)
        return FALSE;

    get_root_item_window_coords(canvas, &window_x, &window_y);
    
    /*
    g_debug("canvas button release at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */
    
    return hippo_canvas_item_emit_button_release_event(canvas->root,
                                                       event->x - window_x, event->y - window_y,
                                                       event->button,
                                                       event->x_root, event->y_root,
                                                       event->time);
}

static gboolean
tooltip_timeout(void *data)
{
    HippoCanvas *canvas = HIPPO_CANVAS(data);
    char *tip;
    HippoRectangle for_area;
    
    tip = NULL;
    if (canvas->root != NULL) {
        int window_x, window_y;
        get_root_item_window_coords(canvas, &window_x, &window_y);
        tip = hippo_canvas_item_get_tooltip(canvas->root,
                                            canvas->last_window_x - window_x,
                                            canvas->last_window_y - window_y,
                                            &for_area);
        for_area.x += window_x;
        for_area.y += window_y;
    }

    if (tip != NULL) {
        int screen_x, screen_y;
        
        if (canvas->tooltip_window == NULL) {
            canvas->tooltip_window = tooltip_window_new();
        }

        gdk_window_get_origin(GTK_WIDGET(canvas)->window, &screen_x, &screen_y);

        for_area.x += screen_x;
        for_area.y += screen_y;
        
        tooltip_window_show(canvas->tooltip_window,
                            GTK_WIDGET(canvas),
                            for_area.x,
                            for_area.y + for_area.height,
                            tip);

        g_free(tip);
    }
    
    canvas->tooltip_timeout_id = 0;
    return FALSE;
}

#define TIP_DELAY (1000*1.5)

static void
handle_new_mouse_location(HippoCanvas      *canvas,
                          GdkWindow        *event_window,
                          HippoMotionDetail detail) /* FIXME detail is totally ignored, remove ... */
{
    int mouse_x, mouse_y;
    int root_x_origin, root_y_origin;
    int root_x, root_y;
    int w, h;
    gboolean was_hovering;

    if (event_window != GTK_WIDGET(canvas)->window)
        return;
    
    gdk_window_get_pointer(event_window, &mouse_x, &mouse_y, NULL);

    if (mouse_x != canvas->last_window_x || mouse_y != canvas->last_window_y) {
        
        cancel_tooltip(canvas);       
        canvas->tooltip_timeout_id = g_timeout_add(TIP_DELAY, tooltip_timeout, canvas);
        canvas->last_window_x = mouse_x;
        canvas->last_window_y = mouse_y;
    }

    get_root_item_window_coords(canvas, &root_x_origin, &root_y_origin);
    root_x = mouse_x - root_x_origin;
    root_y = mouse_y - root_y_origin;
    
    hippo_canvas_item_get_allocation(canvas->root, &w, &h);

#if 0
    g_debug("%p mouse %d,%d root origin %d,%d root %d,%d root size %dx%d", canvas->root,
            mouse_x, mouse_y, root_x_origin, root_y_origin, root_x, root_y, w, h);
#endif
    
    was_hovering = canvas->root_hovering;
    if (root_x < 0 || root_y < 0 || root_x >= w || root_y >= h) {
        canvas->root_hovering = FALSE;
    } else {
        canvas->root_hovering = TRUE;
    }

    /* g_debug("   was_hovering %d root_hovering %d", was_hovering, canvas->root_hovering); */
    
    if (was_hovering && !canvas->root_hovering) {
        set_pointer(canvas, HIPPO_CANVAS_POINTER_UNSET);
        hippo_canvas_item_emit_motion_notify_event(canvas->root, root_x, root_y,
                                                   HIPPO_MOTION_DETAIL_LEAVE);
    } else {
        HippoCanvasPointer pointer;
        
        pointer = hippo_canvas_item_get_pointer(canvas->root, root_x, root_y);
        set_pointer(canvas, pointer);
    
        if (canvas->root_hovering && !was_hovering) {
            hippo_canvas_item_emit_motion_notify_event(canvas->root, root_x, root_y,
                                                       HIPPO_MOTION_DETAIL_ENTER);
        } else if (canvas->root_hovering) {
            hippo_canvas_item_emit_motion_notify_event(canvas->root, root_x, root_y,
                                                       HIPPO_MOTION_DETAIL_WITHIN);
        }
    }
}

static gboolean
hippo_canvas_enter_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* g_debug("motion notify GDK ENTER on %p root %p root_hovering %d", widget, canvas->root, canvas->root_hovering); */
    
    if (canvas->root == NULL)
        return FALSE;

    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_ENTER);
    
    return FALSE;
}

static gboolean
hippo_canvas_leave_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* g_debug("motion notify GDK LEAVE on %p root %p root_hovering %d", widget, canvas->root, canvas->root_hovering); */
    
    if (canvas->root == NULL)
        return FALSE;
    
    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_LEAVE);
    
    return FALSE;
}

static gboolean
hippo_canvas_motion_notify(GtkWidget         *widget,
                           GdkEventMotion    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);    

    /* g_debug("motion notify GDK MOTION on %p root %p root_hovering %d", widget, canvas->root, canvas->root_hovering); */
    
    if (canvas->root == NULL)
        return FALSE;

    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_WITHIN);
    
    return FALSE;
}

static void
hippo_canvas_realize(GtkWidget    *widget)
{
  GdkWindowAttr attributes;
  gint attributes_mask;
  
  GTK_WIDGET_SET_FLAGS (widget, GTK_REALIZED);

  attributes.window_type = GDK_WINDOW_CHILD;
  attributes.x = widget->allocation.x;
  attributes.y = widget->allocation.y;
  attributes.width = widget->allocation.width;
  attributes.height = widget->allocation.height;
  attributes.wclass = GDK_INPUT_OUTPUT;
  attributes.visual = gtk_widget_get_visual (widget);
  attributes.colormap = gtk_widget_get_colormap (widget);
  attributes.event_mask = gtk_widget_get_events (widget);
  attributes.event_mask |= GDK_EXPOSURE_MASK | GDK_BUTTON_PRESS_MASK;
      
  attributes_mask = GDK_WA_X | GDK_WA_Y | GDK_WA_VISUAL | GDK_WA_COLORMAP;
      
  widget->window = gdk_window_new (gtk_widget_get_parent_window (widget), &attributes, 
                                   attributes_mask);
  gdk_window_set_user_data (widget->window, widget);
      
  widget->style = gtk_style_attach (widget->style, widget->window);
  gtk_style_set_background (widget->style, widget->window, GTK_STATE_NORMAL);
}

static void
hippo_canvas_add(GtkContainer *container,
                 GtkWidget    *widget)
{
    g_warning("hippo_canvas_add called, you have to just add an item with a widget in it, you can't do gtk_container_add directly");
}

static void
hippo_canvas_remove(GtkContainer *container,
                    GtkWidget    *widget)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);
    GSList *link;

    /* We go a little roundabout here - we remove the widget from the canvas
     * item, which causes us to remove it from ourselves.
     * The only time we expect gtk_container_remove to be called is from
     * gtk_object_destroy on e.g. the toplevel window, or something of
     * that nature.
     */
    
    for (link = canvas->widget_items;
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

static void
hippo_canvas_forall(GtkContainer *container,
                    gboolean      include_internals,
                    GtkCallback   callback,
                    gpointer      callback_data)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);
    GSList *link;
    
    for (link = canvas->widget_items;
         link != NULL;
         link = link->next) {
        RegisteredWidgetItem *witem = link->data;

        if (witem->widget)
            (* callback) (witem->widget, callback_data);
    }
}

static GType
hippo_canvas_child_type(GtkContainer *container)
{
    return GTK_TYPE_WIDGET;
}

static PangoLayout*
hippo_canvas_create_layout(HippoCanvasContext *context)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);
    return gtk_widget_create_pango_layout(GTK_WIDGET(canvas), NULL);
}

static HippoCanvasLoadImageHook hippo_canvas_load_image_hook = NULL;

void
hippo_canvas_set_load_image_hook(HippoCanvasLoadImageHook hook)
{
    hippo_canvas_load_image_hook = hook;
}

static cairo_surface_t*
hippo_canvas_load_image(HippoCanvasContext *context,
                        const char         *image_name)
{
    if (hippo_canvas_load_image_hook) {
        return hippo_canvas_load_image_hook(context, image_name);
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
hippo_canvas_get_color(HippoCanvasContext *context,
                       HippoStockColor     color)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);
    GtkWidget *widget = GTK_WIDGET(canvas);
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
update_widget(HippoCanvas          *canvas,
              RegisteredWidgetItem *witem)
{
    GtkWidget *new_widget;

    new_widget = NULL;
    g_object_get(G_OBJECT(witem->item), "widget", &new_widget, NULL);

    if (new_widget == witem->widget)
        return;
    
    if (new_widget) {
        /* note that this ref/sinks the widget */
        gtk_widget_set_parent(new_widget, GTK_WIDGET(canvas));
    }

    if (witem->widget) {
        /* and this unrefs the widget */
        gtk_widget_unparent(witem->widget);
    }
    
    witem->widget = new_widget;
}

static void
on_item_widget_changed(HippoCanvasItem *item,
                       GParamSpec      *arg,
                       void            *data)
{
    HippoCanvas *canvas = HIPPO_CANVAS(data);
    RegisteredWidgetItem *witem;
    GSList *link;
    
    witem = NULL;
    for (link = canvas->widget_items;
         link != NULL;
         link = link->next) {
        witem = link->data;
        if (witem->item == item) {
            update_widget(canvas, witem);
            return;
        }
    }

    g_warning("got widget changed for an unregistered widget item");
}

static void
add_widget_item(HippoCanvas     *canvas,
                HippoCanvasItem *item)
{
    RegisteredWidgetItem *witem = g_new0(RegisteredWidgetItem, 1);

    witem->item = item;
    g_object_ref(witem->item);
    canvas->widget_items = g_slist_prepend(canvas->widget_items, witem);

    update_widget(canvas, witem);
    
    g_signal_connect(G_OBJECT(item), "notify::widget",
                     G_CALLBACK(on_item_widget_changed),
                     canvas);
}

static void
remove_widget_item(HippoCanvas     *canvas,
                   HippoCanvasItem *item)
{
    RegisteredWidgetItem *witem;
    GSList *link;
    
    witem = NULL;
    for (link = canvas->widget_items;
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

    canvas->widget_items = g_slist_remove(canvas->widget_items, witem);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(witem->item),
                                         G_CALLBACK(on_item_widget_changed),
                                         canvas);
    if (witem->widget) {
        gtk_widget_unparent(witem->widget);
        witem->widget = NULL;
    }
    g_object_unref(witem->item);
    g_free(witem);
}

static void
hippo_canvas_register_widget_item(HippoCanvasContext *context,
                                  HippoCanvasItem    *item)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);

    add_widget_item(canvas, item);
}

static void
hippo_canvas_unregister_widget_item (HippoCanvasContext *context,
                                     HippoCanvasItem    *item)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);

    remove_widget_item(canvas, item);
}

static void
hippo_canvas_translate_to_widget(HippoCanvasContext *context,
                                 HippoCanvasItem    *item,
                                 int                *x_p,
                                 int                *y_p)
{
    GtkWidget *widget = GTK_WIDGET(context);

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
canvas_root_request_changed(HippoCanvasItem *root,
                            HippoCanvas     *canvas)
{
    /* g_debug("queuing resize on canvas root %p canvas %p canvas container %p",
       root, canvas, GTK_WIDGET(canvas)->parent); */
    if (!canvas->fixing_up_resize_state)
        gtk_widget_queue_resize(GTK_WIDGET(canvas));
}

static void
canvas_root_paint_needed(HippoCanvasItem      *root,
                         const HippoRectangle *damage_box,
                         HippoCanvas          *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);
    int window_x, window_y;
    
    get_root_item_window_coords(canvas, &window_x, &window_y);
    
    gtk_widget_queue_draw_area(widget,
                               damage_box->x + window_x,
                               damage_box->y + window_y,
                               damage_box->width, damage_box->height);
}

void
hippo_canvas_set_root(HippoCanvas     *canvas,
                      HippoCanvasItem *root)
{
    g_return_if_fail(HIPPO_IS_CANVAS(canvas));
    g_return_if_fail(root == NULL || HIPPO_IS_CANVAS_ITEM(root));

    if (root == canvas->root)
        return;

    if (canvas->root != NULL) {
        g_signal_handlers_disconnect_by_func(canvas->root,
                                             G_CALLBACK(canvas_root_request_changed),
                                             canvas);
        g_signal_handlers_disconnect_by_func(canvas->root,
                                             G_CALLBACK(canvas_root_paint_needed),
                                             canvas);        
        hippo_canvas_item_set_context(canvas->root, NULL);
        g_object_unref(canvas->root);
        canvas->root = NULL;
    }

    if (root != NULL) {
        g_object_ref(root);
        hippo_canvas_item_sink(root);
        canvas->root = root;
        g_signal_connect(root, "request-changed",
                         G_CALLBACK(canvas_root_request_changed),
                         canvas);
        g_signal_connect(root, "paint-needed",
                         G_CALLBACK(canvas_root_paint_needed),
                         canvas);
        hippo_canvas_item_set_context(canvas->root, HIPPO_CANVAS_CONTEXT(canvas));
    }

    gtk_widget_queue_resize(GTK_WIDGET(canvas));
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
hippo_canvas_fixup_resize_state(HippoCanvas *canvas)
{
    RegisteredWidgetItem *witem;
    GSList *link;

    if (canvas->fixing_up_resize_state) {
        g_warning("Recursion in %s", G_GNUC_PRETTY_FUNCTION);
        return;
    }
    
    canvas->fixing_up_resize_state = TRUE;
    
    witem = NULL;
    for (link = canvas->widget_items;
         link != NULL;
         link = link->next) {
        witem = link->data;

        if (witem->widget &&
            (GTK_WIDGET_REQUEST_NEEDED(witem->widget) ||
             GTK_WIDGET_ALLOC_NEEDED(witem->widget))) {
            hippo_canvas_item_emit_request_changed(witem->item);
        }
    }

    canvas->fixing_up_resize_state = FALSE;
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
    gdk_pointer_ungrab(event->time);
    gtk_widget_hide(tip);
    return FALSE;
}

static void
tooltip_window_show(GtkWidget  *tip,
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
    GdkGrabStatus status;
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

    gtk_widget_show(tip);
    
    status = gdk_pointer_grab(tip->window,
                              FALSE,
                              GDK_POINTER_MOTION_MASK,
                              NULL, NULL,
                              0);

    if (status != GDK_GRAB_SUCCESS && status != GDK_GRAB_ALREADY_GRABBED)
        gtk_widget_hide(tip);
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

/* TEST CODE */
#if 1
#include <gtk/gtk.h>
#include <hippo/hippo-canvas-test.h>

void
hippo_canvas_open_test_window(void)
{
    GtkWidget *window;
    GtkWidget *scrolled;
    GtkWidget *canvas;
    HippoCanvasItem *root;

    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);

    scrolled = gtk_scrolled_window_new(NULL,NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                   GTK_POLICY_AUTOMATIC,
                                   GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type(GTK_SCROLLED_WINDOW(scrolled),
                                        GTK_SHADOW_NONE);
    gtk_container_add(GTK_CONTAINER(window), scrolled);
    gtk_widget_show(scrolled);
    
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(scrolled),
                                          canvas);

    gtk_viewport_set_shadow_type(GTK_VIEWPORT(gtk_bin_get_child(GTK_BIN(scrolled))),
                                 GTK_SHADOW_NONE);

    root = hippo_canvas_test_get_root();

    hippo_canvas_set_root(HIPPO_CANVAS(canvas), root);

    gtk_window_set_default_size(GTK_WINDOW(window), 300, 300);
    gtk_widget_show(window);
}

#endif /* test code */
