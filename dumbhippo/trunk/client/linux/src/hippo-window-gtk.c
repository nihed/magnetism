/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-window.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-window-gtk.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtkscrolledwindow.h>
#include <gtk/gtkhbox.h>
#include <gtk/gtkvbox.h>
#include <gtk/gtkdrawingarea.h>
#include "hippo-canvas.h"
#include "hippo-canvas-grip.h"

static void      hippo_window_gtk_init                (HippoWindowGtk       *window_gtk);
static void      hippo_window_gtk_class_init          (HippoWindowGtkClass  *klass);
static void      hippo_window_gtk_iface_init          (HippoWindowClass     *window_class);
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
static void     hippo_window_gtk_set_contents       (HippoWindow     *window,
                                                     HippoCanvasItem *item);
static void     hippo_window_gtk_set_visible        (HippoWindow     *window,
                                                     gboolean         visible);
static void     hippo_window_gtk_set_position       (HippoWindow     *window,
                                                     int              x,
                                                     int              y);
static void     hippo_window_gtk_get_size           (HippoWindow     *window,
                                                     int             *width_p,
                                                     int             *height_p);
static void     hippo_window_gtk_set_scrollbar      (HippoWindow     *window,
                                                     HippoOrientation orientation,
                                                     gboolean         visible);
static void     hippo_window_gtk_set_resize_grip    (HippoWindow     *window,
                                                     HippoSide        side,
                                                     gboolean         visible);

/* internal stuff */

static GtkWidget* create_resize_grip                (HippoSide        side);

struct _HippoWindowGtk {
    GtkWindow window;
    GtkWidget *vbox;
    GtkWidget *hbox;
    GtkWidget *scroll;
    GtkWidget *canvas;
    GtkWidget *left_grip;
    GtkWidget *right_grip;
    GtkWidget *top_grip;
    GtkWidget *bottom_grip;
    HippoCanvasItem *contents;
    guint has_vscrollbar : 1;
    guint has_hscrollbar : 1;
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
    window_class->set_scrollbar = hippo_window_gtk_set_scrollbar;
    window_class->set_resize_grip = hippo_window_gtk_set_resize_grip;
}

static void
hippo_window_gtk_class_init(HippoWindowGtkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_gtk_set_property;
    object_class->get_property = hippo_window_gtk_get_property;

    object_class->finalize = hippo_window_gtk_finalize;
}

static void
hippo_window_gtk_init(HippoWindowGtk *window_gtk)
{
    window_gtk->scroll = gtk_scrolled_window_new(NULL, NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(window_gtk->scroll),
                                   GTK_POLICY_NEVER,
                                   GTK_POLICY_NEVER);
    window_gtk->vbox = gtk_vbox_new(FALSE, 0);
    window_gtk->hbox = gtk_hbox_new(FALSE, 0);
    window_gtk->canvas = hippo_canvas_new();

    gtk_container_add(GTK_CONTAINER(window_gtk), window_gtk->vbox);
    gtk_container_add(GTK_CONTAINER(window_gtk->vbox), window_gtk->hbox);
    gtk_container_add(GTK_CONTAINER(window_gtk->hbox), window_gtk->scroll);
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(window_gtk->scroll), window_gtk->canvas);

    gtk_widget_show_all(window_gtk->vbox);

    /* not sure there's a sane type hint for our window. it's sort of a dock, but
     * metacity forces !resizeable on docks, so that doesn't work. bad metacity.
     */
    /* gtk_window_set_type_hint(GTK_WINDOW(window_gtk), GDK_WINDOW_TYPE_HINT_DOCK); */
    gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_set_decorated(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_stick(GTK_WINDOW(window_gtk));
    gtk_window_set_skip_taskbar_hint(GTK_WINDOW(window_gtk), TRUE);
    gtk_window_set_skip_pager_hint(GTK_WINDOW(window_gtk), TRUE);
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
                       "resizable", FALSE,
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
hippo_window_gtk_set_scrollbar(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          visible)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);
    
    visible = visible != FALSE;

    if (orientation == HIPPO_ORIENTATION_VERTICAL) {
        if (window_gtk->has_vscrollbar == visible)
            return;
        window_gtk->has_vscrollbar = visible;
    } else {
        if (window_gtk->has_hscrollbar == visible)
            return;
        window_gtk->has_hscrollbar = visible;
    }


    if (window_gtk->has_vscrollbar || window_gtk->has_hscrollbar) {
        GdkGeometry geometry;

        gtk_window_set_resizable(GTK_WINDOW(window_gtk), TRUE);

        /* -1 = size request */
        geometry.min_width = -1;
        geometry.min_height = -1;
        geometry.max_width = window_gtk->has_hscrollbar ? G_MAXSHORT : -1;
        geometry.max_height = window_gtk->has_vscrollbar ? G_MAXSHORT : -1;
        gtk_window_set_geometry_hints(GTK_WINDOW (window), NULL,
                                      &geometry,
                                      GDK_HINT_MIN_SIZE | GDK_HINT_MAX_SIZE);        
    } else {
        gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
        gtk_window_set_geometry_hints(GTK_WINDOW(window_gtk), NULL, NULL, 0);
    }
    
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(window_gtk->scroll),
                                   window_gtk->has_hscrollbar ?
                                   GTK_POLICY_AUTOMATIC :
                                   GTK_POLICY_NEVER,
                                   window_gtk->has_vscrollbar ?
                                   GTK_POLICY_AUTOMATIC :
                                   GTK_POLICY_NEVER);
}

static void
hippo_window_gtk_set_resize_grip(HippoWindow      *window,
                                 HippoSide         side,
                                 gboolean          visible)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(window);

    /* we create the grips the first time they are visible,
     * but never bother destroying them again, just hide
     */
    
    switch (side) {
    case HIPPO_SIDE_TOP:
        if (visible && window_gtk->top_grip == NULL) {
            window_gtk->top_grip = create_resize_grip(side);
            gtk_box_pack_start(GTK_BOX(window_gtk->vbox), window_gtk->top_grip,
                               FALSE, FALSE, 0);
        }
        if (window_gtk->top_grip)
            g_object_set(GTK_WIDGET(window_gtk->top_grip), "visible", visible, NULL);
        break;
    case HIPPO_SIDE_BOTTOM:
        if (visible && window_gtk->bottom_grip == NULL) {
            window_gtk->bottom_grip = create_resize_grip(side);
            gtk_box_pack_end(GTK_BOX(window_gtk->vbox), window_gtk->bottom_grip,
                             FALSE, FALSE, 0);
        }
        if (window_gtk->bottom_grip)
            g_object_set(GTK_WIDGET(window_gtk->bottom_grip), "visible", visible, NULL);
        break;
    case HIPPO_SIDE_LEFT:
        if (visible && window_gtk->left_grip == NULL) {
            window_gtk->left_grip = create_resize_grip(side);
            gtk_box_pack_start(GTK_BOX(window_gtk->hbox), window_gtk->left_grip,
                               FALSE, FALSE, 0);
        }
        if (window_gtk->left_grip)
            g_object_set(GTK_WIDGET(window_gtk->left_grip), "visible", visible, NULL);
        break;
    case HIPPO_SIDE_RIGHT:
        if (visible && window_gtk->right_grip == NULL) {
            window_gtk->right_grip = create_resize_grip(side);
            gtk_box_pack_end(GTK_BOX(window_gtk->hbox), window_gtk->right_grip,
                             FALSE, FALSE, 0);
        }
        if (window_gtk->right_grip)
            g_object_set(GTK_WIDGET(window_gtk->right_grip), "visible", visible, NULL);
        break;
    }
}

static gboolean
on_grip_button_press(GtkWidget      *widget,
                     GdkEventButton *event,
                     void           *data)
{
    GtkWidget *toplevel;
    GdkWindowEdge edge;
    
    toplevel = gtk_widget_get_ancestor(widget, GTK_TYPE_WINDOW);
    if (toplevel == NULL)
        return FALSE;

    switch ((HippoSide) GPOINTER_TO_INT(data)) {
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
        return FALSE;
    }
    
    gtk_window_begin_resize_drag(GTK_WINDOW(toplevel),
                                 edge,
                                 event->button,
                                 event->x_root,
                                 event->y_root,
                                 event->time);
    return TRUE;
}

static GtkWidget*
create_resize_grip(HippoSide side)
{
    GtkWidget *grip;
    HippoCanvasItem *grip_item;

    grip = hippo_canvas_new();

    grip_item = hippo_canvas_grip_new();
    hippo_canvas_set_root(HIPPO_CANVAS(grip), grip_item);
    
    gtk_widget_add_events(grip, GDK_BUTTON_PRESS_MASK | GDK_ENTER_NOTIFY_MASK | GDK_LEAVE_NOTIFY_MASK);
    
    g_signal_connect(G_OBJECT(grip),
                     "button-press-event",
                     G_CALLBACK(on_grip_button_press),
                     GINT_TO_POINTER(side));
    
    return grip;
}
