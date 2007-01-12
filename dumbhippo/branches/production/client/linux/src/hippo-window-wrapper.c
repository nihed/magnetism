/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <hippo/hippo-window.h>
#include "hippo-window-gtk.h"
#include "hippo-window-wrapper.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtkcontainer.h>
#include <hippo/hippo-canvas-window.h>

static void      hippo_window_wrapper_init                (HippoWindowWrapper       *wrapper);
static void      hippo_window_wrapper_class_init          (HippoWindowWrapperClass  *klass);
static void      hippo_window_wrapper_iface_init          (HippoWindowClass     *window_class);
static void      hippo_window_wrapper_dispose             (GObject              *object);
static void      hippo_window_wrapper_finalize            (GObject              *object);

static void hippo_window_wrapper_set_property (GObject      *object,
                                               guint         prop_id,
                                               const GValue *value,
                                               GParamSpec   *pspec);
static void hippo_window_wrapper_get_property (GObject      *object,
                                               guint         prop_id,
                                               GValue       *value,
                                               GParamSpec   *pspec);

/* Signal handlers */
static gboolean hippo_window_wrapper_focus_in_event          (GtkWidget           *widget,
                                                              GdkEventFocus       *event,
                                                              HippoWindowWrapper  *wrapper);
static gboolean hippo_window_wrapper_focus_out_event         (GtkWidget           *widget,
                                                              GdkEventFocus       *event,
                                                              HippoWindowWrapper  *wrapper);
static gboolean hippo_window_wrapper_unmap_event             (GtkWidget           *widget,
                                                              GdkEventAny         *event,
                                                              HippoWindowWrapper  *wrapper);
static gboolean hippo_window_wrapper_visibility_notify_event (GtkWidget           *widget,
                                                              GdkEventVisibility  *event,
                                                              HippoWindowWrapper  *wrapper);

/* Window methods */
static void hippo_window_wrapper_set_contents      (HippoWindow      *window,
                                                    HippoCanvasItem  *item);
static void hippo_window_wrapper_set_visible       (HippoWindow      *window,
                                                    gboolean          visible);
static void hippo_window_wrapper_hide_to_icon      (HippoWindow      *window,
                                                    HippoRectangle   *icon_rectangle);
static void hippo_window_wrapper_set_position      (HippoWindow      *window,
                                                    int               x,
                                                    int               y);
static void hippo_window_wrapper_set_size          (HippoWindow      *window,
                                                    int               width,
                                                    int               height);
static void hippo_window_wrapper_get_position      (HippoWindow      *window,
                                                    int              *x_p,
                                                    int              *y_p);
static void hippo_window_wrapper_get_size          (HippoWindow      *window,
                                                    int              *width_p,
                                                    int              *height_p);
static void hippo_window_wrapper_set_resizable     (HippoWindow      *window,
                                                    HippoOrientation  orientation,
                                                    gboolean          value);
static void hippo_window_wrapper_begin_move_drag   (HippoWindow      *window,
                                                HippoEvent       *event);
static void hippo_window_wrapper_begin_resize_drag (HippoWindow      *window,
                                                    HippoSide         side,
                                                    HippoEvent       *event);
static void hippo_window_wrapper_present           (HippoWindow      *window);

struct _HippoWindowWrapper {
    GObject parent;
    
    HippoWindowGtk *window;

    guint visible : 1;
    guint positioned : 1; /* Set if hippo_window_position was even called */
    guint hresizable : 1;
    guint vresizable : 1;
    guint active : 1;
    guint onscreen : 1;
};

struct _HippoWindowWrapperClass {
    GObjectClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_ROLE,
    PROP_RESIZE_GRAVITY,
    PROP_ACTIVE,
    PROP_ONSCREEN
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowWrapper, hippo_window_wrapper, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_wrapper_iface_init));

static void
hippo_window_wrapper_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_wrapper_set_contents;
    window_class->set_visible = hippo_window_wrapper_set_visible;
    window_class->hide_to_icon = hippo_window_wrapper_hide_to_icon;
    window_class->set_position = hippo_window_wrapper_set_position;
    window_class->set_size = hippo_window_wrapper_set_size;
    window_class->get_position = hippo_window_wrapper_get_position;
    window_class->get_size = hippo_window_wrapper_get_size;
    window_class->set_resizable = hippo_window_wrapper_set_resizable;
    window_class->begin_move_drag = hippo_window_wrapper_begin_move_drag;
    window_class->begin_resize_drag = hippo_window_wrapper_begin_resize_drag;
    window_class->present = hippo_window_wrapper_present;
}

static void
hippo_window_wrapper_class_init(HippoWindowWrapperClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_wrapper_set_property;
    object_class->get_property = hippo_window_wrapper_get_property;

    object_class->dispose = hippo_window_wrapper_dispose;
    object_class->finalize = hippo_window_wrapper_finalize;
    
    g_object_class_override_property(object_class, PROP_ROLE, "role");
    g_object_class_override_property(object_class, PROP_RESIZE_GRAVITY, "resize-gravity");
    g_object_class_override_property(object_class, PROP_ACTIVE, "active");
    g_object_class_override_property(object_class, PROP_ONSCREEN, "onscreen");
}

static void
hippo_window_wrapper_init(HippoWindowWrapper *wrapper)
{
    wrapper->hresizable = FALSE;
    wrapper->vresizable = FALSE;

    wrapper->window = hippo_window_gtk_new();

    g_signal_connect_after(wrapper->window, "focus-in-event",
                           G_CALLBACK(hippo_window_wrapper_focus_in_event), wrapper);
    g_signal_connect_after(wrapper->window, "focus-out-event",
                           G_CALLBACK(hippo_window_wrapper_focus_out_event), wrapper);
    g_signal_connect_after(wrapper->window, "unmap-event",
                           G_CALLBACK(hippo_window_wrapper_unmap_event), wrapper);
    g_signal_connect_after(wrapper->window, "visibility-notify-event",
                           G_CALLBACK(hippo_window_wrapper_visibility_notify_event), wrapper);
}

static void
hippo_window_wrapper_dispose(GObject *object)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(object);

    if (wrapper->visible) {
        wrapper->visible = FALSE;
        g_object_unref(wrapper);
    }

    if (wrapper->window) {
        gtk_widget_destroy(GTK_WIDGET(wrapper->window));
        wrapper->window = NULL;
    }

    G_OBJECT_CLASS(hippo_window_wrapper_parent_class)->dispose(object);
}

static void
hippo_window_wrapper_finalize(GObject *object)
{
    /* HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(object); */

    G_OBJECT_CLASS(hippo_window_wrapper_parent_class)->finalize(object);
}

HippoWindow*
hippo_window_wrapper_new(void)
{
    HippoWindowWrapper *wrapper;

    wrapper = g_object_new(HIPPO_TYPE_WINDOW_WRAPPER, NULL);
    
    return HIPPO_WINDOW(wrapper);
}

static void
hippo_window_wrapper_set_property(GObject         *object,
                                  guint            prop_id,
                                  const GValue    *value,
                                  GParamSpec      *pspec)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(object);

    switch (prop_id) {
    case PROP_ROLE:
        hippo_window_gtk_set_role(wrapper->window, g_value_get_int(value));
        break;
    case PROP_RESIZE_GRAVITY:
        hippo_window_gtk_set_resize_gravity(wrapper->window, g_value_get_int(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_wrapper_get_property(GObject         *object,
                                  guint            prop_id,
                                  GValue          *value,
                                  GParamSpec      *pspec)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(object);

    switch (prop_id) {
    case PROP_ROLE:
        g_value_set_int(value, hippo_window_gtk_get_role(wrapper->window));
        break;
    case PROP_RESIZE_GRAVITY:
        g_value_set_int(value, hippo_window_gtk_get_resize_gravity(wrapper->window));
        break;
    case PROP_ACTIVE:
        g_value_set_boolean(value, wrapper->active);
        break;
    case PROP_ONSCREEN:
        g_value_set_boolean(value, wrapper->onscreen);
        break;
        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_active(HippoWindowWrapper *wrapper,
           gboolean        active)
{
    active = active != FALSE;

    if (active != wrapper->active) {
        wrapper->active = active;
        g_object_notify(G_OBJECT(wrapper), "active");
    }
}

static void
set_onscreen(HippoWindowWrapper *wrapper,
             gboolean        onscreen)
{
    onscreen = onscreen != FALSE;

    if (onscreen != wrapper->onscreen) {
        wrapper->onscreen = onscreen;
        g_object_notify(G_OBJECT(wrapper), "onscreen");
    }
}

static gboolean
hippo_window_wrapper_focus_in_event(GtkWidget           *widget,
                                    GdkEventFocus       *event,
                                    HippoWindowWrapper  *wrapper)
{
    set_active(wrapper, TRUE);

    return FALSE;
}

static gboolean
hippo_window_wrapper_focus_out_event(GtkWidget           *widget,
                                     GdkEventFocus       *event,
                                     HippoWindowWrapper  *wrapper)
{
    set_active(wrapper, FALSE);

    return FALSE;
}

static gboolean
hippo_window_wrapper_unmap_event(GtkWidget          *widget,
                                 GdkEventAny        *event,
                                 HippoWindowWrapper *wrapper)
{
    set_onscreen(wrapper, FALSE);

    return FALSE;
}

static gboolean
hippo_window_wrapper_visibility_notify_event(GtkWidget           *widget,
                                             GdkEventVisibility  *event,
                                             HippoWindowWrapper  *wrapper)
{
    set_onscreen(wrapper, event->state != GDK_VISIBILITY_FULLY_OBSCURED);

    return FALSE;
}

static void
hippo_window_wrapper_set_contents(HippoWindow     *window,
                                  HippoCanvasItem *item)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    hippo_canvas_window_set_root(HIPPO_CANVAS_WINDOW(wrapper->window), item);
}

static void
hippo_window_wrapper_set_visible(HippoWindow     *window,
                                 gboolean         visible)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    visible = visible != FALSE;

    if (visible == wrapper->visible)
        return;

    wrapper->visible = visible;

    if (visible) {
        g_object_ref(wrapper);
        gtk_widget_show(GTK_WIDGET(wrapper->window));
    } else {
        gtk_widget_hide(GTK_WIDGET(wrapper->window));
        g_object_unref(wrapper);
    }
}

static void 
hippo_window_wrapper_hide_to_icon(HippoWindow    *window,
                                  HippoRectangle *icon_rect)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);
    
    if (!wrapper->visible)
        return;

    wrapper->visible = FALSE;

    /* What this is supposed to do (and does on Windows) is hides
     * the window and shows the window-minimization animation going
     * to icon_rect.
     * 
     * On Linux the only way to get the real minimization animation is
     * to really minimize. Which would have a fun interaction with the
     * tasklist. (We could set our icon location in the window
     * property for the place to minimize to, but the tasklist would
     * sometimes overwrite it)
     *
     * So, maybe it's just best to omit ourselves from the task list
     * and draw a minimization animation from scratch. The code from
     * metacity could be stolen.
     */
    gtk_widget_hide(GTK_WIDGET(wrapper->window));
    
    g_object_unref(wrapper);
}

static void
hippo_window_wrapper_set_position(HippoWindow     *window,
                                  int              x,
                                  int              y)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    hippo_window_gtk_set_position(wrapper->window, x, y);
}

static void
hippo_window_wrapper_set_size(HippoWindow     *window,
                              int              width,
                              int              height)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);
    
    gtk_window_resize(GTK_WINDOW(wrapper->window), width, height);
}

static void
hippo_window_wrapper_get_position(HippoWindow     *window,
                                  int             *x_p,
                                  int             *y_p)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    gtk_window_get_position(GTK_WINDOW(wrapper->window), x_p, y_p);
}

static void
hippo_window_wrapper_get_size(HippoWindow     *window,
                              int             *width_p,
                              int             *height_p)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    gtk_window_get_size(GTK_WINDOW(wrapper->window), width_p, height_p);
}

static void
hippo_window_wrapper_set_resizable(HippoWindow      *window,
                                   HippoOrientation  orientation,
                                   gboolean          value)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);
    
    value = value != FALSE;

    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (wrapper->vresizable == value)
            return;
        wrapper->vresizable = value;
    } else {
        if (wrapper->hresizable == value)
            return;
        wrapper->hresizable = value;
    }


    if (wrapper->hresizable || wrapper->vresizable) {
        GdkGeometry geometry;

        gtk_window_set_resizable(GTK_WINDOW(wrapper->window), TRUE);

        /* -1 = size request */
        geometry.min_width = -1;
        geometry.min_height = -1;
        geometry.max_width = wrapper->hresizable ? G_MAXSHORT : -1;
        geometry.max_height = wrapper->vresizable ? G_MAXSHORT : -1;
        gtk_window_set_geometry_hints(GTK_WINDOW (wrapper->window), NULL,
                                      &geometry,
                                      GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE);        
    } else {
        gtk_window_set_resizable(GTK_WINDOW(wrapper->window), FALSE);
        gtk_window_set_geometry_hints(GTK_WINDOW(wrapper->window), NULL, NULL, 0);
    }
}

static void
hippo_window_wrapper_begin_move_drag(HippoWindow *window,
                                     HippoEvent  *event)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);    
    
    gtk_window_begin_move_drag(GTK_WINDOW(wrapper->window),
                               event->u.button.button,
                               event->u.button.x11_x_root,
                               event->u.button.x11_y_root,
                               event->u.button.x11_time);
}
static void
hippo_window_wrapper_begin_resize_drag(HippoWindow *window,
                                       HippoSide    side,
                                       HippoEvent  *event)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);    
    GdkWindowEdge edge;
    
    switch (side) {
    case HIPPO_SIDE_TOP:
        edge = GDK_WINDOW_EDGE_NORTH;
        break;
    case HIPPO_SIDE_BOTTOM:
        edge = GDK_WINDOW_EDGE_SOUTH;
        break;
    case HIPPO_SIDE_LEFT:
        edge = GDK_WINDOW_EDGE_WEST;
        break;
    case HIPPO_SIDE_RIGHT:
        edge = GDK_WINDOW_EDGE_EAST;
        break;
    default:
        g_warning("Unknown window side");
        return;
    }
    
    gtk_window_begin_resize_drag(GTK_WINDOW(wrapper->window),
                                 edge,
                                 event->u.button.button,
                                 event->u.button.x11_x_root,
                                 event->u.button.x11_y_root,
                                 event->u.button.x11_time);
}

static void
hippo_window_wrapper_present(HippoWindow *window)
{
    HippoWindowWrapper *wrapper = HIPPO_WINDOW_WRAPPER(window);

    if (!wrapper->visible) {
        g_object_ref(window);
        wrapper->visible = TRUE;
    }

    gtk_window_present(GTK_WINDOW(wrapper->window));
}
