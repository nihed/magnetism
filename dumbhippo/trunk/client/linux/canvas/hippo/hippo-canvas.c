/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#include <hippo/hippo-canvas-context.h>
#include <gtk/gtkcontainer.h>
#include "hippo-canvas-widget.h"

typedef struct
{
    HippoCanvasItem *item;
    GtkWidget       *widget;
} RegisteredWidgetItem;

static void hippo_canvas_init       (HippoCanvas             *canvas);
static void hippo_canvas_class_init (HippoCanvasClass        *klass);
static void hippo_canvas_dispose    (GObject                 *object);
static void hippo_canvas_finalize   (GObject                 *object);
static void hippo_canvas_iface_init (HippoCanvasContextClass *klass);


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

struct _HippoCanvas {
    GtkContainer parent;

    HippoCanvasItem *root;

    HippoCanvasPointer pointer;

    GSList *widget_items;
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
hippo_canvas_iface_init (HippoCanvasContextClass *klass)
{
    klass->create_layout = hippo_canvas_create_layout;
    klass->load_image = hippo_canvas_load_image;
    klass->get_color = hippo_canvas_get_color;
    klass->register_widget_item = hippo_canvas_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_translate_to_widget;
}

static void
hippo_canvas_dispose(GObject *object)
{
    HippoCanvas *canvas = HIPPO_CANVAS(object);

    hippo_canvas_set_root(canvas, NULL);

    g_assert(canvas->widget_items == NULL);
    
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

    requisition->width = 0;
    requisition->height = 0;

    if (canvas->root == NULL)
        return;

    hippo_canvas_item_get_request(canvas->root,
                                  &requisition->width,
                                  &requisition->height);

    requisition->width += GTK_CONTAINER(widget)->border_width * 2;
    requisition->height += GTK_CONTAINER(widget)->border_width * 2;
}

static void
hippo_canvas_size_allocate(GtkWidget         *widget,
                           GtkAllocation     *allocation)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

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

static void
handle_new_mouse_location(HippoCanvas *canvas,
                          GdkWindow   *event_window,
                          HippoMotionDetail detail)
{
    int x, y;
    HippoCanvasPointer pointer;
    int window_x, window_y;

    get_root_item_window_coords(canvas, &window_x, &window_y);
    
    gdk_window_get_pointer(event_window, &x, &y, NULL);

    pointer = hippo_canvas_item_get_pointer(canvas->root, x - window_x, y - window_y);
    set_pointer(canvas, pointer);
    
    hippo_canvas_item_emit_motion_notify_event(canvas->root,
                                               x - window_x, y - window_y, detail);
}

static gboolean
hippo_canvas_enter_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    
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
    gtk_widget_queue_resize(GTK_WIDGET(canvas));
}

static void
canvas_root_paint_needed(HippoCanvasItem *root,
                         int              x,
                         int              y,
                         int              width,
                         int              height,
                         HippoCanvas     *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);
    int window_x, window_y;
    
    get_root_item_window_coords(canvas, &window_x, &window_y);
    
    gtk_widget_queue_draw_area(widget,
                               x + window_x,
                               y + window_y,
                               width, height);
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

/* TEST CODE */
#if 1
#include <gtk/gtk.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-image.h>

typedef struct {
    int width;
    int height;
    guint32 color;
    HippoPackFlags flags;
    HippoItemAlignment alignment;
} BoxAttrs;

static BoxAttrs single_start[] = { { 40, 80, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_end[] = { { 100, 60, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_start[] = { { 50, 90, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL },
                                   { 50, 90, 0xff000099, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_end[] = { { 45, 55, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
                                 { 45, 55, 0x00ff0077, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand_end[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs everything[] = {
    { 120, 50, 0x00ccccff, 0, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 0, 0, 0, 0 }
};
static BoxAttrs alignments[] = {
    { 120, 50, 0x00ffcccc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccffcc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_START },
    { 120, 50, 0x00cffffc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_CENTER },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_END },
    { 0, 0, 0, 0 }
};

static BoxAttrs* box_rows[] = { single_start, /* double_start,*/ single_end, /* double_end, */
                                single_expand, everything, alignments };

static HippoCanvasItem*
create_row(BoxAttrs *boxes)
{
    HippoCanvasItem *row;
    int i;
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);

    for (i = 0; boxes[i].width > 0; ++i) {
        BoxAttrs *attrs = &boxes[i];
        HippoCanvasItem *shape;
        HippoCanvasItem *label;
        const char *flags_text;
        const char *align_text;
        char *s;
        
        shape = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                             /* "width", attrs->width,
                                "height", attrs->height, */
                             /* "color", attrs->color, */
                             "background-color", 0xffffffff,
                             "xalign", attrs->alignment,
                             NULL);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), shape, attrs->flags);
        
        if (attrs->flags == (HIPPO_PACK_END | HIPPO_PACK_EXPAND))
            flags_text = "END | EXPAND";
        else if (attrs->flags == HIPPO_PACK_END)
            flags_text = "END";
        else if (attrs->flags == HIPPO_PACK_EXPAND)
            flags_text = "EXPAND";
        else
            flags_text = "0";

        switch (attrs->alignment) {
        case HIPPO_ALIGNMENT_FILL:
            align_text = "FILL";
            break;
        case HIPPO_ALIGNMENT_START:
            align_text = "START";
            break;
        case HIPPO_ALIGNMENT_END:
            align_text = "END";
            break;
        case HIPPO_ALIGNMENT_CENTER:
            align_text = "CENTER";
            break;
        default:
            align_text = NULL;
            break;
        }

        s = g_strdup_printf("%s, %s", flags_text, align_text);
        label = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                             "text", s,
                             "background-color", 0x0000ff00,
                             NULL);
        g_free(s);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape), label, HIPPO_PACK_EXPAND);
    }
    return row;
}

void
hippo_canvas_open_test_window(void)
{
    GtkWidget *window;
    GtkWidget *scrolled;
    GtkWidget *canvas;
    HippoCanvasItem *root;
    HippoCanvasItem *shape1;
    HippoCanvasItem *shape2;
    HippoCanvasItem *text;
    HippoCanvasItem *image;
    HippoCanvasItem *row;
    int i;
    
    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);

    scrolled = gtk_scrolled_window_new(NULL,NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                   GTK_POLICY_AUTOMATIC,
                                   GTK_POLICY_AUTOMATIC);
    gtk_container_add(GTK_CONTAINER(window), scrolled);
    gtk_widget_show(scrolled);
    
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(scrolled),
                                          canvas);

#if 0
    root = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                        "box-width", 400,
                        "spacing", 8,
                        NULL);

    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
#if 0
#if 1
    shape1 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0xaeaeaeff,
                          "padding", 20,
                          NULL);

    shape2 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0x00ff00ff,
                          "padding", 10,
                          NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape1, 0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape2, 0);
#endif
    
    for (i = 0; i < (int) G_N_ELEMENTS(box_rows); ++i) {
        row = create_row(box_rows[i]);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, 0);
    }

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Click here",
                        "background-color", 0x990000ff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);

    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, HIPPO_PACK_EXPAND);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "chaticon",
                         "xalign", HIPPO_ALIGNMENT_START,
                         "yalign", HIPPO_ALIGNMENT_END,
                         "background-color", 0xffff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, 0);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "ignoreicon",
                         "xalign", HIPPO_ALIGNMENT_FILL,
                         "yalign", HIPPO_ALIGNMENT_FILL,
                         "background-color", 0x00ff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, HIPPO_PACK_EXPAND);

    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "text",
                        "This is some long text that may help in testing resize behavior. It goes "
                        "on for a while, so don't get impatient. More and more and  more text. "
                        "Text text text. Lorem ipsum! Text! This is the story of text.",
                        "background-color", 0x0000ff00,
                        "yalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
#endif
#endif

    root = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "orientation", HIPPO_ORIENTATION_VERTICAL,
                        "border", 15,
                        "border-color", 0xff0000ff,
                        "padding", 25,
                        "background-color", 0x00ff00ff,
                        NULL);

#if 0
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                        "Click here",
                        "color", 0xffffffff,
                        "background-color", 0x0000ffff,
                        NULL);
#else
    text = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "nophoto",
                        "background-color", 0x0000ffff,
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
#endif
    {
        GtkWidget *widget = gtk_label_new("FOOO! GtkLabel");
        gtk_widget_show(widget);
        shape1 = g_object_new(HIPPO_TYPE_CANVAS_WIDGET,
                            "widget", widget,
                            "background-color", 0x0000ffff,
                            "xalign", HIPPO_ALIGNMENT_CENTER,
                            "yalign", HIPPO_ALIGNMENT_CENTER,
                            NULL);
    }
                        
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape1, HIPPO_PACK_EXPAND);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                        "Text item packed end",
                        "color", 0xffffffff,
                        "background-color", 0x0000ffff,
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_END);

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Fixed position link item",
                        "background-color", 0xffffffff,
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_FIXED);

    hippo_canvas_box_move(HIPPO_CANVAS_BOX(root), text, 150, 150);

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Fixed link inside label item",
                        "background-color", 0xffffffff,
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape1), text, HIPPO_PACK_FIXED);

    hippo_canvas_box_move(HIPPO_CANVAS_BOX(shape1), text, 50, 50);

    
    hippo_canvas_set_root(HIPPO_CANVAS(canvas), root);

    gtk_window_set_default_size(GTK_WINDOW(window), 300, 300);
    gtk_widget_show(window);
}

#endif /* test code */
