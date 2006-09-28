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


/* Window methods */
static void hippo_window_gtk_set_contents      (HippoWindow      *window,
                                                HippoCanvasItem  *item);
static void hippo_window_gtk_set_visible       (HippoWindow      *window,
                                                gboolean          visible);
static void hippo_window_gtk_set_position      (HippoWindow      *window,
                                                int               x,
                                                int               y);
static void hippo_window_gtk_get_size          (HippoWindow      *window,
                                                int              *width_p,
                                                int              *height_p);
static void hippo_window_gtk_set_resizable     (HippoWindow      *window,
                                                HippoOrientation  orientation,
                                                gboolean          value);
static void hippo_window_gtk_begin_resize_drag (HippoWindow      *window,
                                                HippoSide         side,
                                                HippoEvent       *event);


struct _HippoWindowGtk {
    GtkWindow window;
    GtkWidget *canvas;
    HippoCanvasItem *contents;
    guint hresizable : 1;
    guint vresizable : 1;
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
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowGtk, hippo_window_gtk, GTK_TYPE_WINDOW,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_gtk_iface_init));

static void
hippo_window_gtk_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_gtk_set_contents;
    window_class->set_visible = hippo_window_gtk_set_visible;
    window_class->set_position = hippo_window_gtk_set_position;
    window_class->get_size = hippo_window_gtk_get_size;
    window_class->set_resizable = hippo_window_gtk_set_resizable;
    window_class->begin_resize_drag = hippo_window_gtk_begin_resize_drag;
}

static void
hippo_window_gtk_class_init(HippoWindowGtkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_gtk_set_property;
    object_class->get_property = hippo_window_gtk_get_property;

    object_class->dispose = hippo_window_gtk_dispose;
    object_class->finalize = hippo_window_gtk_finalize;
}

static void
hippo_window_gtk_init(HippoWindowGtk *window_gtk)
{
    window_gtk->canvas = hippo_canvas_new();

    gtk_container_add(GTK_CONTAINER(window_gtk), window_gtk->canvas);

    gtk_widget_show(window_gtk->canvas);

    /* not sure there's a sane type hint for our window. it's sort of a dock, but
     * metacity forces !resizeable on docks, so that doesn't work. bad metacity.
     */
    /* gtk_window_set_type_hint(GTK_WINDOW(window_gtk), GDK_WINDOW_TYPE_HINT_DOCK); */
    gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_set_decorated(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_stick(GTK_WINDOW(window_gtk));
    gtk_window_set_skip_taskbar_hint(GTK_WINDOW(window_gtk), TRUE);
    gtk_window_set_skip_pager_hint(GTK_WINDOW(window_gtk), TRUE);

    window_gtk->hresizable = FALSE;
    window_gtk->vresizable = FALSE;
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
hippo_window_gtk_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoWindowGtk *gtk;

    gtk = HIPPO_WINDOW_GTK(object);

    switch (prop_id) {

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

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
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
hippo_window_gtk_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    gtk_window_move(GTK_WINDOW(window_gtk), x, y);
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
