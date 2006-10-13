/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <hippo/hippo-window.h>
#include "hippo-window-gtk.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtkcontainer.h>
#include <hippo/hippo-canvas.h>

static void      hippo_window_gtk_init                (HippoWindowGtk       *window_gtk);
static void      hippo_window_gtk_class_init          (HippoWindowGtkClass  *klass);
static void      hippo_window_gtk_iface_init          (HippoWindowClass     *window_class);
static void      hippo_window_gtk_dispose             (GObject              *object);
static void      hippo_window_gtk_finalize            (GObject              *object);

static void hippo_window_gtk_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_window_gtk_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);

/* Widget methods */
static gboolean hippo_window_gtk_focus_in_event          (GtkWidget           *widget,
                                                          GdkEventFocus       *event);
static gboolean hippo_window_gtk_focus_out_event         (GtkWidget           *widget,
                                                          GdkEventFocus       *event);
static gboolean hippo_window_gtk_unmap_event             (GtkWidget           *widget,
                                                          GdkEventAny         *event);
static gboolean hippo_window_gtk_visibility_notify_event (GtkWidget           *widget,
                                                          GdkEventVisibility  *event);

/* Window methods */
static void hippo_window_gtk_set_contents      (HippoWindow      *window,
                                                HippoCanvasItem  *item);
static void hippo_window_gtk_set_visible       (HippoWindow      *window,
                                                gboolean          visible);
static void hippo_window_gtk_hide_to_icon      (HippoWindow      *window,
                                                HippoRectangle   *icon_rectangle);
static void hippo_window_gtk_set_position      (HippoWindow      *window,
                                                int               x,
                                                int               y);
static void hippo_window_gtk_set_size          (HippoWindow      *window,
                                                int               width,
                                                int               height);
static void hippo_window_gtk_get_position      (HippoWindow      *window,
                                                int              *x_p,
                                                int              *y_p);
static void hippo_window_gtk_get_size          (HippoWindow      *window,
                                                int              *width_p,
                                                int              *height_p);
static void hippo_window_gtk_set_resizable     (HippoWindow      *window,
                                                HippoOrientation  orientation,
                                                gboolean          value);
static void hippo_window_gtk_begin_move_drag   (HippoWindow      *window,
                                                HippoEvent       *event);
static void hippo_window_gtk_begin_resize_drag (HippoWindow      *window,
                                                HippoSide         side,
                                                HippoEvent       *event);
static void hippo_window_gtk_present           (HippoWindow      *window);

struct _HippoWindowGtk {
    GtkWindow window;
    GtkWidget *canvas;
    HippoCanvasItem *contents;
    guint hresizable : 1;
    guint vresizable : 1;
    guint app_window : 1;
    guint active : 1;
    guint onscreen : 1;
};

struct _HippoWindowGtkClass {
    GtkWindowClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_APP_WINDOW,
    PROP_ACTIVE,
    PROP_ONSCREEN
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowGtk, hippo_window_gtk, GTK_TYPE_WINDOW,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_gtk_iface_init));

static void
hippo_window_gtk_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_gtk_set_contents;
    window_class->set_visible = hippo_window_gtk_set_visible;
    window_class->hide_to_icon = hippo_window_gtk_hide_to_icon;
    window_class->set_position = hippo_window_gtk_set_position;
    window_class->set_size = hippo_window_gtk_set_size;
    window_class->get_position = hippo_window_gtk_get_position;
    window_class->get_size = hippo_window_gtk_get_size;
    window_class->set_resizable = hippo_window_gtk_set_resizable;
    window_class->begin_move_drag = hippo_window_gtk_begin_move_drag;
    window_class->begin_resize_drag = hippo_window_gtk_begin_resize_drag;
    window_class->present = hippo_window_gtk_present;
}

static void
hippo_window_gtk_class_init(HippoWindowGtkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS (klass);

    object_class->set_property = hippo_window_gtk_set_property;
    object_class->get_property = hippo_window_gtk_get_property;

    object_class->dispose = hippo_window_gtk_dispose;
    object_class->finalize = hippo_window_gtk_finalize;
    
    widget_class->focus_in_event = hippo_window_gtk_focus_in_event;
    widget_class->focus_out_event = hippo_window_gtk_focus_out_event;
    widget_class->unmap_event = hippo_window_gtk_unmap_event;
    widget_class->visibility_notify_event = hippo_window_gtk_visibility_notify_event;
        
    g_object_class_override_property(object_class, PROP_APP_WINDOW, "app-window");
    g_object_class_override_property(object_class, PROP_ACTIVE, "active");
    g_object_class_override_property(object_class, PROP_ONSCREEN, "onscreen");
}

static void
hippo_window_gtk_init(HippoWindowGtk *window_gtk)
{
    window_gtk->app_window = TRUE;
    window_gtk->canvas = hippo_canvas_new();

    gtk_container_add(GTK_CONTAINER(window_gtk), window_gtk->canvas);

    gtk_widget_show(window_gtk->canvas);

    gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_set_decorated(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_stick(GTK_WINDOW(window_gtk));

    window_gtk->hresizable = FALSE;
    window_gtk->vresizable = FALSE;

    gtk_widget_set_events(GTK_WIDGET(window_gtk), GDK_VISIBILITY_NOTIFY_MASK);
}

static void
hippo_window_gtk_dispose(GObject *object)
{
    /* HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(object); */


    G_OBJECT_CLASS(hippo_window_gtk_parent_class)->dispose(object);
}

static void
hippo_window_gtk_finalize(GObject *object)
{
    /* HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(object); */


    G_OBJECT_CLASS(hippo_window_gtk_parent_class)->finalize(object);
}

HippoWindow*
hippo_window_gtk_new(void)
{
    HippoWindowGtk *gtk;

    gtk = g_object_new(HIPPO_TYPE_WINDOW_GTK,
                       NULL);
    
    return HIPPO_WINDOW(gtk);
}

static void
set_app_window(HippoWindowGtk *window_gtk,
               gboolean        app_window)
{
    GtkWindow *window = GTK_WINDOW(window_gtk);
    
    window_gtk->app_window = app_window != FALSE;
    gtk_window_set_skip_taskbar_hint(window, !app_window);
    gtk_window_set_skip_pager_hint(window, !app_window);
    if (app_window) {
        gtk_window_set_type_hint(window, GDK_WINDOW_TYPE_HINT_NORMAL);
        gtk_window_set_accept_focus(window, TRUE);
    } else {
        /* NOTIFICATION is the right type hint, but that's new in GTK+-2.10 */
        gtk_window_set_type_hint(window, GDK_WINDOW_TYPE_HINT_DOCK);
        gtk_window_set_accept_focus(window, FALSE);
    }
}

static void
hippo_window_gtk_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoWindowGtk *gtk;

    gtk = HIPPO_WINDOW_GTK(object);

    switch (prop_id) {
    case PROP_APP_WINDOW:
        set_app_window(gtk, g_value_get_boolean(value));
        break;
        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_gtk_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoWindowGtk *gtk;

    gtk = HIPPO_WINDOW_GTK (object);

    switch (prop_id) {
    case PROP_APP_WINDOW:
        g_value_set_boolean(value, gtk->app_window);
        break;
    case PROP_ACTIVE:
        g_value_set_boolean(value, gtk->active);
        break;
    case PROP_ONSCREEN:
        g_value_set_boolean(value, gtk->onscreen);
        break;
        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_active(HippoWindowGtk *window_gtk,
           gboolean        active)
{
    active = active != FALSE;

    if (active != window_gtk->active) {
        window_gtk->active = active;
        g_object_notify(G_OBJECT(window_gtk), "active");
    }
}

static void
set_onscreen(HippoWindowGtk *window_gtk,
             gboolean        onscreen)
{
    onscreen = onscreen != FALSE;

    if (onscreen != window_gtk->onscreen) {
        window_gtk->onscreen = onscreen;
        g_object_notify(G_OBJECT(window_gtk), "onscreen");
    }
}

static gboolean
hippo_window_gtk_focus_in_event(GtkWidget           *widget,
                                GdkEventFocus       *event)
{
    HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(widget);
    
    gboolean result = GTK_WIDGET_CLASS(hippo_window_gtk_parent_class)->focus_in_event(widget, event);

    set_active(gtk, TRUE);

    return result;
}

static gboolean
hippo_window_gtk_focus_out_event(GtkWidget           *widget,
                                 GdkEventFocus       *event)
{
    HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(widget);

    gboolean result = GTK_WIDGET_CLASS(hippo_window_gtk_parent_class)->focus_out_event(widget, event);

    set_active(gtk, FALSE);

    return result;
}

static gboolean
hippo_window_gtk_unmap_event(GtkWidget   *widget,
                             GdkEventAny *event)
{
    HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(widget);

    set_onscreen(gtk, FALSE);

    return FALSE;
}

static gboolean
hippo_window_gtk_visibility_notify_event(GtkWidget           *widget,
                                         GdkEventVisibility  *event)
{
    HippoWindowGtk *gtk = HIPPO_WINDOW_GTK(widget);

    set_onscreen(gtk, event->state != GDK_VISIBILITY_FULLY_OBSCURED);

    return FALSE;
}

static void
hippo_window_gtk_set_contents(HippoWindow     *window,
                              HippoCanvasItem *item)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    if (item == window_gtk->contents)
        return;

    if (window_gtk->contents) {
        g_object_unref(window_gtk->contents);
        window_gtk->contents = NULL;
    }

    if (item) {
        g_object_ref(item);
        window_gtk->contents = item;
    }

    hippo_canvas_set_root(HIPPO_CANVAS(window_gtk->canvas), window_gtk->contents);
}

static void
hippo_window_gtk_set_visible(HippoWindow     *window,
                             gboolean         visible)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    if (visible)
        gtk_widget_show(GTK_WIDGET(window_gtk));
    else
        gtk_widget_hide(GTK_WIDGET(window_gtk));
}

static void 
hippo_window_gtk_hide_to_icon(HippoWindow    *window,
                              HippoRectangle *icon_rect)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);
    
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
    gtk_widget_hide(GTK_WIDGET(window_gtk));
}

static void
hippo_window_gtk_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_move(GTK_WINDOW(window_gtk), x, y);
}

static void
hippo_window_gtk_set_size(HippoWindow     *window,
                          int              width,
                          int              height)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_resize(GTK_WINDOW(window_gtk), width, height);
}

static void
hippo_window_gtk_get_position(HippoWindow     *window,
                              int             *x_p,
                              int             *y_p)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_get_position(GTK_WINDOW(window_gtk), x_p, y_p);
}

static void
hippo_window_gtk_get_size(HippoWindow     *window,
                          int             *width_p,
                          int             *height_p)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_get_size(GTK_WINDOW(window_gtk), width_p, height_p);
}

static void
hippo_window_gtk_set_resizable(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          value)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);
    
    value = value != FALSE;

    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (window_gtk->vresizable == value)
            return;
        window_gtk->vresizable = value;
    } else {
        if (window_gtk->hresizable == value)
            return;
        window_gtk->hresizable = value;
    }


    if (window_gtk->hresizable || window_gtk->vresizable) {
        GdkGeometry geometry;

        gtk_window_set_resizable(GTK_WINDOW(window_gtk), TRUE);

        /* -1 = size request */
        geometry.min_width = -1;
        geometry.min_height = -1;
        geometry.max_width = window_gtk->hresizable ? G_MAXSHORT : -1;
        geometry.max_height = window_gtk->vresizable ? G_MAXSHORT : -1;
        gtk_window_set_geometry_hints(GTK_WINDOW (window), NULL,
                                      &geometry,
                                      GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE);        
    } else {
        gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
        gtk_window_set_geometry_hints(GTK_WINDOW(window_gtk), NULL, NULL, 0);
    }
}

static void
hippo_window_gtk_begin_move_drag(HippoWindow *window,
                                 HippoEvent  *event)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);    
    
    gtk_window_begin_move_drag(GTK_WINDOW(window_gtk),
                               event->u.button.button,
                               event->u.button.x11_x_root,
                               event->u.button.x11_y_root,
                               event->u.button.x11_time);
}
static void
hippo_window_gtk_begin_resize_drag(HippoWindow *window,
                                   HippoSide    side,
                                   HippoEvent  *event)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);    
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
    
    gtk_window_begin_resize_drag(GTK_WINDOW(window_gtk),
                                 edge,
                                 event->u.button.button,
                                 event->u.button.x11_x_root,
                                 event->u.button.x11_y_root,
                                 event->u.button.x11_time);
}

static void
hippo_window_gtk_present(HippoWindow *window)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_present(GTK_WINDOW(window_gtk));
}

