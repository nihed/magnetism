/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas-helper.h"
#include "hippo-canvas-window.h"
#include "hippo-canvas-window-child.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtkcontainer.h>
#include <hippo/hippo-canvas.h>

static void      hippo_canvas_window_init                (HippoCanvasWindow       *canvas_window);
static void      hippo_canvas_window_class_init          (HippoCanvasWindowClass  *klass);
static void      hippo_canvas_window_dispose             (GObject              *object);
static void      hippo_canvas_window_finalize            (GObject              *object);

static void hippo_canvas_window_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_canvas_window_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);

static gboolean  hippo_canvas_window_button_press        (GtkWidget         *widget,
                                                          GdkEventButton    *event);
static gboolean  hippo_canvas_window_button_release      (GtkWidget         *widget,
                                                          GdkEventButton    *event);
static gboolean  hippo_canvas_window_enter_notify        (GtkWidget         *widget,
                                                          GdkEventCrossing  *event);
static gboolean  hippo_canvas_window_leave_notify        (GtkWidget         *widget,
                                                          GdkEventCrossing  *event);
static gboolean  hippo_canvas_window_motion_notify       (GtkWidget         *widget,
                                                          GdkEventMotion    *event);

struct _HippoCanvasWindow {
    GtkWindow parent;

    HippoCanvasHelper *helper;
};

struct _HippoCanvasWindowClass {
    GtkWindowClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
};

G_DEFINE_TYPE(HippoCanvasWindow, hippo_canvas_window, GTK_TYPE_WINDOW);

static void
hippo_canvas_window_class_init(HippoCanvasWindowClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS (klass);

    object_class->set_property = hippo_canvas_window_set_property;
    object_class->get_property = hippo_canvas_window_get_property;

    object_class->dispose = hippo_canvas_window_dispose;
    object_class->finalize = hippo_canvas_window_finalize;

    widget_class->button_press_event = hippo_canvas_window_button_press;
    widget_class->button_release_event = hippo_canvas_window_button_release;
    widget_class->motion_notify_event = hippo_canvas_window_motion_notify;
    widget_class->enter_notify_event = hippo_canvas_window_enter_notify;
    widget_class->leave_notify_event = hippo_canvas_window_leave_notify;
}

static void
hippo_canvas_window_init(HippoCanvasWindow *canvas_window)
{
    GtkWidget *widget = GTK_WIDGET(canvas_window);
    GtkWidget *window_child;

    gtk_widget_add_events(widget, GDK_POINTER_MOTION_MASK | GDK_POINTER_MOTION_HINT_MASK |
                          GDK_ENTER_NOTIFY_MASK | GDK_LEAVE_NOTIFY_MASK | GDK_BUTTON_PRESS_MASK);

    window_child = hippo_canvas_window_child_new();

    canvas_window->helper = hippo_canvas_window_child_get_helper(HIPPO_CANVAS_WINDOW_CHILD(window_child));

    gtk_widget_show(window_child);
    gtk_container_add(GTK_CONTAINER(canvas_window), window_child);
}

static void
hippo_canvas_window_dispose(GObject *object)
{
    /* HippoCanvasWindow *canvas = HIPPO_CANVAS_WINDOW(object); */

    G_OBJECT_CLASS(hippo_canvas_window_parent_class)->dispose(object);
}

static void
hippo_canvas_window_finalize(GObject *object)
{
    /* HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(object); */


    G_OBJECT_CLASS(hippo_canvas_window_parent_class)->finalize(object);
}

GtkWidget *
hippo_canvas_window_new(void)
{
    HippoCanvasWindow *canvas_window;

    canvas_window = g_object_new(HIPPO_TYPE_CANVAS_WINDOW,
                                 NULL);
    
    return GTK_WIDGET(canvas_window);
}

static void
hippo_canvas_window_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
#if 0
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW (object);
#endif

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_window_get_property(GObject         *object,
                                 guint            prop_id,
                                 GValue          *value,
                                 GParamSpec      *pspec)
{
#if 0
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW (object);
#endif

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_canvas_window_button_press(GtkWidget         *widget,
                                 GdkEventButton    *event)
{
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(widget);

    return hippo_canvas_helper_button_press(canvas_window->helper, event);
}

static gboolean
hippo_canvas_window_button_release(GtkWidget         *widget,
                                   GdkEventButton    *event)
{
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(widget);

    return hippo_canvas_helper_button_release(canvas_window->helper, event);
}

static gboolean
hippo_canvas_window_enter_notify(GtkWidget         *widget,
                                 GdkEventCrossing  *event)
{
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(widget);

    return hippo_canvas_helper_enter_notify(canvas_window->helper, event);
}

static gboolean
hippo_canvas_window_leave_notify(GtkWidget         *widget,
                                 GdkEventCrossing  *event)
{
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(widget);

    return hippo_canvas_helper_leave_notify(canvas_window->helper, event);
}

static gboolean
hippo_canvas_window_motion_notify(GtkWidget         *widget,
                                  GdkEventMotion    *event)
{
    HippoCanvasWindow *canvas_window = HIPPO_CANVAS_WINDOW(widget);

    return hippo_canvas_helper_motion_notify(canvas_window->helper, event);
}

void
hippo_canvas_window_set_root(HippoCanvasWindow *canvas_window,
                             HippoCanvasItem   *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_WINDOW(canvas_window));

    hippo_canvas_helper_set_root(canvas_window->helper, item);
}
