/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#include "hippo-canvas-helper.h"
#include <hippo/hippo-canvas-context.h>
#include <gtk/gtkcontainer.h>
#include <gtk/gtkwindow.h>
#include <gtk/gtklabel.h>

static void hippo_canvas_init       (HippoCanvas             *canvas);
static void hippo_canvas_class_init (HippoCanvasClass        *klass);
static void hippo_canvas_dispose    (GObject                 *object);
static void hippo_canvas_finalize   (GObject                 *object);


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

static void  hippo_canvas_realize           (GtkWidget    *widget);
static void  hippo_canvas_unmap             (GtkWidget    *widget);
static void  hippo_canvas_hierarchy_changed (GtkWidget    *widget,
                                             GtkWidget    *old_toplevel);
static void  hippo_canvas_add               (GtkContainer *container,
                                             GtkWidget    *widget);
static void  hippo_canvas_remove            (GtkContainer *container,
                                             GtkWidget    *widget);
static void  hippo_canvas_forall            (GtkContainer *container,
                                             gboolean      include_internals,
                                             GtkCallback   callback,
                                             gpointer      callback_data);
static GType hippo_canvas_child_type        (GtkContainer *container);

struct _HippoCanvas {
    GtkContainer parent;

    HippoCanvasHelper *helper;
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

G_DEFINE_TYPE(HippoCanvas, hippo_canvas, GTK_TYPE_CONTAINER)

static void
hippo_canvas_init(HippoCanvas *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);

    canvas->helper = hippo_canvas_helper_new(GTK_CONTAINER(canvas));

    gtk_widget_add_events(widget, GDK_POINTER_MOTION_MASK | GDK_POINTER_MOTION_HINT_MASK |
                          GDK_ENTER_NOTIFY_MASK | GDK_LEAVE_NOTIFY_MASK | GDK_BUTTON_PRESS);
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
    widget_class->unmap = hippo_canvas_unmap;
    widget_class->hierarchy_changed = hippo_canvas_hierarchy_changed;
    
    container_class->add = hippo_canvas_add;
    container_class->remove = hippo_canvas_remove;
    container_class->forall = hippo_canvas_forall;
    container_class->child_type = hippo_canvas_child_type;
}

static void
hippo_canvas_dispose(GObject *object)
{
    HippoCanvas *canvas = HIPPO_CANVAS(object);

    if (canvas->helper) {
        g_object_run_dispose(G_OBJECT(canvas->helper));
        g_object_unref(canvas->helper);
        canvas->helper = NULL;
    }
    
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

static gboolean
hippo_canvas_expose_event(GtkWidget         *widget,
                          GdkEventExpose    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        hippo_canvas_helper_expose_event(canvas->helper, event);

    return GTK_WIDGET_CLASS(hippo_canvas_parent_class)->expose_event(widget, event);
}

static void
hippo_canvas_size_request(GtkWidget         *widget,
                          GtkRequisition    *requisition)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    hippo_canvas_helper_size_request(canvas->helper, requisition);
}

static void
hippo_canvas_size_allocate(GtkWidget         *widget,
                           GtkAllocation     *allocation)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* g_debug("gtk allocate on canvas root %p canvas %p", canvas->root, canvas); */

    widget->allocation = *allocation;
    
    if (GTK_WIDGET_REALIZED(widget))
        gdk_window_move_resize(widget->window,
                               allocation->x, 
                               allocation->y,
                               allocation->width, 
                               allocation->height);    

    hippo_canvas_helper_size_allocate(canvas->helper, allocation);
}

static gboolean
hippo_canvas_button_press(GtkWidget         *widget,
                          GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        return hippo_canvas_helper_button_press(canvas->helper, event);
    else
        return FALSE;
}

static gboolean
hippo_canvas_button_release(GtkWidget         *widget,
                            GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        return hippo_canvas_helper_button_release(canvas->helper, event);
    else
        return FALSE;
}

static gboolean
hippo_canvas_enter_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        return hippo_canvas_helper_enter_notify(canvas->helper, event);
    else
        return FALSE;
}

static gboolean
hippo_canvas_leave_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        return hippo_canvas_helper_leave_notify(canvas->helper, event);
    else
        return FALSE;
}

static gboolean
hippo_canvas_motion_notify(GtkWidget         *widget,
                           GdkEventMotion    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (event->window == widget->window)
        return hippo_canvas_helper_motion_notify(canvas->helper, event);
    else
        return FALSE;
}

static void
hippo_canvas_realize(GtkWidget    *widget)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
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
    
    hippo_canvas_helper_realize(canvas->helper);
}

static void
hippo_canvas_unmap(GtkWidget    *widget)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    hippo_canvas_helper_unmap(canvas->helper);

    GTK_WIDGET_CLASS(hippo_canvas_parent_class)->unmap(widget);
}

static void
hippo_canvas_hierarchy_changed (GtkWidget    *widget,
                                GtkWidget    *old_toplevel)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (canvas->helper) /* hierarchy changed can happen during dispose */
        hippo_canvas_helper_hierarchy_changed(canvas->helper, old_toplevel);

    if (GTK_WIDGET_CLASS(hippo_canvas_parent_class)->hierarchy_changed)
        GTK_WIDGET_CLASS(hippo_canvas_parent_class)->hierarchy_changed(widget, old_toplevel);
}

static void
hippo_canvas_add(GtkContainer *container,
                 GtkWidget    *widget)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);

    hippo_canvas_helper_add(canvas->helper, widget);
}

static void
hippo_canvas_remove(GtkContainer *container,
                    GtkWidget    *widget)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);

    hippo_canvas_helper_remove(canvas->helper, widget);
}

static void
hippo_canvas_forall(GtkContainer *container,
                    gboolean      include_internals,
                    GtkCallback   callback,
                    gpointer      callback_data)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);

    if (canvas->helper) /* can happen during dispose */
        hippo_canvas_helper_forall(canvas->helper, include_internals, callback, callback_data);
}

static GType
hippo_canvas_child_type(GtkContainer *container)
{
    HippoCanvas *canvas = HIPPO_CANVAS(container);
    
    return hippo_canvas_helper_child_type(canvas->helper);
}

void
hippo_canvas_set_load_image_hook(HippoCanvasLoadImageHook hook)
{
    hippo_canvas_helper_set_load_image_hook(hook);
}

void
hippo_canvas_set_root(HippoCanvas     *canvas,
                      HippoCanvasItem *root)
{
    g_return_if_fail(HIPPO_IS_CANVAS(canvas));
    g_return_if_fail(root == NULL || HIPPO_IS_CANVAS_ITEM(root));

    hippo_canvas_helper_set_root(canvas->helper, root);
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
