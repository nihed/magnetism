/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas-window-child.h"
#include "hippo-canvas-helper.h"
#include <hippo/hippo-canvas-context.h>
#include <gtk/gtkcontainer.h>
#include <gtk/gtkwindow.h>
#include <gtk/gtklabel.h>

static void hippo_canvas_window_child_init       (HippoCanvasWindowChild      *canvas);
static void hippo_canvas_window_child_class_init (HippoCanvasWindowChildClass *klass);
static void hippo_canvas_window_child_dispose    (GObject                     *object);
static void hippo_canvas_window_child_finalize   (GObject                     *object);


static gboolean  hippo_canvas_window_child_expose_event  (GtkWidget         *widget,
                                                          GdkEventExpose    *event);

static void      hippo_canvas_window_child_size_request        (GtkWidget         *widget,
                                                                GtkRequisition    *requisition);
static void      hippo_canvas_window_child_size_allocate       (GtkWidget         *widget,
                                                                GtkAllocation     *allocation);

static void  hippo_canvas_window_child_realize           (GtkWidget    *widget);
static void  hippo_canvas_window_child_unmap             (GtkWidget    *widget);
static void  hippo_canvas_window_child_hierarchy_changed (GtkWidget    *widget,
                                                          GtkWidget    *old_toplevel);
static void  hippo_canvas_window_child_add               (GtkContainer *container,
                                                          GtkWidget    *widget);
static void  hippo_canvas_window_child_remove            (GtkContainer *container,
                                                          GtkWidget    *widget);
static void  hippo_canvas_window_child_forall            (GtkContainer *container,
                                                          gboolean      include_internals,
                                                          GtkCallback   callback,
                                                          gpointer      callback_data);
static GType hippo_canvas_window_child_child_type        (GtkContainer *container);

struct _HippoCanvasWindowChild {
    GtkContainer parent;

    HippoCanvasHelper *helper;
};

struct _HippoCanvasWindowChildClass {
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

G_DEFINE_TYPE(HippoCanvasWindowChild, hippo_canvas_window_child, GTK_TYPE_CONTAINER)

static void
hippo_canvas_window_child_init(HippoCanvasWindowChild *window_child)
{
    window_child->helper = hippo_canvas_helper_new(GTK_CONTAINER(window_child));
    
    GTK_WIDGET_SET_FLAGS(window_child, GTK_NO_WINDOW);
}

static void
hippo_canvas_window_child_class_init(HippoCanvasWindowChildClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(klass);
    GtkContainerClass *container_class = GTK_CONTAINER_CLASS(klass);
    
    object_class->dispose = hippo_canvas_window_child_dispose;
    object_class->finalize = hippo_canvas_window_child_finalize;

    widget_class->expose_event = hippo_canvas_window_child_expose_event;
    widget_class->size_request = hippo_canvas_window_child_size_request;
    widget_class->size_allocate = hippo_canvas_window_child_size_allocate;

    widget_class->realize = hippo_canvas_window_child_realize;
    widget_class->unmap = hippo_canvas_window_child_unmap;
    widget_class->hierarchy_changed = hippo_canvas_window_child_hierarchy_changed;
    
    container_class->add = hippo_canvas_window_child_add;
    container_class->remove = hippo_canvas_window_child_remove;
    container_class->forall = hippo_canvas_window_child_forall;
    container_class->child_type = hippo_canvas_window_child_child_type;
}

static void
hippo_canvas_window_child_dispose(GObject *object)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(object);

    window_child->helper = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_window_child_parent_class)->dispose(object);
}

static void
hippo_canvas_window_child_finalize(GObject *object)
{
    /* HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(object); */

    G_OBJECT_CLASS(hippo_canvas_window_child_parent_class)->finalize(object);
}

GtkWidget*
hippo_canvas_window_child_new(void)
{
    HippoCanvasWindowChild *window_child;

    window_child = g_object_new(HIPPO_TYPE_CANVAS_WINDOW_CHILD, NULL);

    return GTK_WIDGET(window_child);
}

HippoCanvasHelper *
hippo_canvas_window_child_get_helper(HippoCanvasWindowChild *window_child)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_WINDOW_CHILD(window_child), NULL);

    return window_child->helper;
}

static gboolean
hippo_canvas_window_child_expose_event(GtkWidget         *widget,
                                       GdkEventExpose    *event)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    if (event->window == widget->window)
        hippo_canvas_helper_expose_event(window_child->helper, event);

    return GTK_WIDGET_CLASS(hippo_canvas_window_child_parent_class)->expose_event(widget, event);
}

static void
hippo_canvas_window_child_size_request(GtkWidget         *widget,
                                       GtkRequisition    *requisition)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    hippo_canvas_helper_size_request(window_child->helper, requisition);
}

static void
hippo_canvas_window_child_size_allocate(GtkWidget         *widget,
                                        GtkAllocation     *allocation)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    /* g_debug("gtk allocate on canvas root %p canvas %p", window_child->root, canvas); */

    widget->allocation = *allocation;

    hippo_canvas_helper_size_allocate(window_child->helper, allocation);
}

static void
hippo_canvas_window_child_realize(GtkWidget *widget)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    GTK_WIDGET_CLASS(hippo_canvas_window_child_parent_class)->realize(widget);

    hippo_canvas_helper_realize(window_child->helper);
}

static void
hippo_canvas_window_child_unmap(GtkWidget    *widget)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    hippo_canvas_helper_unmap(window_child->helper);

    GTK_WIDGET_CLASS(hippo_canvas_window_child_parent_class)->unmap(widget);
}

static void
hippo_canvas_window_child_hierarchy_changed (GtkWidget    *widget,
                                             GtkWidget    *old_toplevel)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(widget);

    hippo_canvas_helper_hierarchy_changed(window_child->helper, old_toplevel);

    if (GTK_WIDGET_CLASS(hippo_canvas_window_child_parent_class)->hierarchy_changed)
        GTK_WIDGET_CLASS(hippo_canvas_window_child_parent_class)->hierarchy_changed(widget, old_toplevel);
}

static void
hippo_canvas_window_child_add(GtkContainer *container,
                              GtkWidget    *widget)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(container);

    hippo_canvas_helper_add(window_child->helper, widget);
}

static void
hippo_canvas_window_child_remove(GtkContainer *container,
                                 GtkWidget    *widget)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(container);

    hippo_canvas_helper_remove(window_child->helper, widget);
}

static void
hippo_canvas_window_child_forall(GtkContainer *container,
                                 gboolean      include_internals,
                                 GtkCallback   callback,
                                 gpointer      callback_data)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(container);

    hippo_canvas_helper_forall(window_child->helper, include_internals, callback, callback_data);
}

static GType
hippo_canvas_window_child_child_type(GtkContainer *container)
{
    HippoCanvasWindowChild *window_child = HIPPO_CANVAS_WINDOW_CHILD(container);
    
    return hippo_canvas_helper_child_type(window_child->helper);
}
