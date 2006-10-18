/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <hippo/hippo-window.h>
#include "hippo-window-gtk.h"
#include <gtk/gtkwindow.h>
#include <gtk/gtkcontainer.h>
#include <hippo/hippo-canvas-window.h>

#include <gdk/gdkx.h>

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
static void     hippo_window_gtk_realize                 (GtkWidget           *widget);
static gboolean hippo_window_gtk_expose_event            (GtkWidget           *widget,
                                                          GdkEventExpose      *event);
static gboolean hippo_window_gtk_focus_in_event          (GtkWidget           *widget,
                                                          GdkEventFocus       *event);
static gboolean hippo_window_gtk_focus_out_event         (GtkWidget           *widget,
                                                          GdkEventFocus       *event);
static gboolean hippo_window_gtk_unmap_event             (GtkWidget           *widget,
                                                          GdkEventAny         *event);
static gboolean hippo_window_gtk_visibility_notify_event (GtkWidget           *widget,
                                                          GdkEventVisibility  *event);

/* Container methods */
static void hippo_window_gtk_check_resize (GtkContainer *container);

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
    HippoGravity resize_gravity;
    GtkRequisition last_position_requisition;
    guint positioned : 1; /* Set if hippo_window_position was even called */
    guint expose_during_configure : 1; /* Set if we get an expose while waiting for
                                        * for a window manager response. */
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
    PROP_RESIZE_GRAVITY,
    PROP_ACTIVE,
    PROP_ONSCREEN
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowGtk, hippo_window_gtk, HIPPO_TYPE_CANVAS_WINDOW,
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
    GtkContainerClass *container_class = GTK_CONTAINER_CLASS (klass);

    object_class->set_property = hippo_window_gtk_set_property;
    object_class->get_property = hippo_window_gtk_get_property;

    object_class->dispose = hippo_window_gtk_dispose;
    object_class->finalize = hippo_window_gtk_finalize;
    
    widget_class->realize = hippo_window_gtk_realize;
    widget_class->expose_event = hippo_window_gtk_expose_event;
    widget_class->focus_in_event = hippo_window_gtk_focus_in_event;
    widget_class->focus_out_event = hippo_window_gtk_focus_out_event;
    widget_class->unmap_event = hippo_window_gtk_unmap_event;
    widget_class->visibility_notify_event = hippo_window_gtk_visibility_notify_event;

    container_class->check_resize = hippo_window_gtk_check_resize;
        
    g_object_class_override_property(object_class, PROP_APP_WINDOW, "app-window");
    g_object_class_override_property(object_class, PROP_RESIZE_GRAVITY, "resize-gravity");
    g_object_class_override_property(object_class, PROP_ACTIVE, "active");
    g_object_class_override_property(object_class, PROP_ONSCREEN, "onscreen");
}

static void
hippo_window_gtk_init(HippoWindowGtk *window_gtk)
{
    window_gtk->app_window = TRUE;

    gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_set_decorated(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_stick(GTK_WINDOW(window_gtk));

    window_gtk->hresizable = FALSE;
    window_gtk->vresizable = FALSE;
    window_gtk->resize_gravity = HIPPO_GRAVITY_NORTH_WEST;

    gtk_widget_add_events(GTK_WIDGET(window_gtk), GDK_VISIBILITY_NOTIFY_MASK);

    /* See note about double buffering in hippo_window_gtk_expose_event */
    gtk_widget_set_double_buffered(GTK_WIDGET(window_gtk), FALSE);
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
    GtkWidget *widget = GTK_WIDGET(window_gtk);

    app_window = app_window != FALSE;

    if (window_gtk->app_window == app_window)
        return;
    
    window_gtk->app_window = app_window;

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
   
    /* See note about "background none" in hippo_window_gtk_realize() */
    if (GTK_WIDGET_REALIZED(widget)) {
        if (app_window) {
            /* This should cause the right pixel to be set, replacing background-none */
            gtk_style_set_background (widget->style, widget->window, widget->state);
        } else {
            gdk_window_set_back_pixmap(widget->window, NULL, FALSE);
        }
    }
}

static void
set_resize_gravity(HippoWindowGtk *window_gtk,
                   HippoGravity    resize_gravity)
{
    window_gtk->resize_gravity = resize_gravity;

#if 0
    /* We don't set the GdkGravity because we don't need it (our window
     * isn't decorated) and if GTK+ is made to automatically do the
     * sort of thing we do in hippo_window_gtk_check-resize(), the window
     * would get moved twice.
     */
    GdkGravity gdk_gravity = GDK_GRAVITY_NORTH_WEST;

    switch (resize_gravity) {
    case HIPPO_GRAVITY_NORTH_WEST:
        gdk_gravity = GDK_GRAVITY_NORTH_WEST;
        break;
    case HIPPO_GRAVITY_NORTH_EAST:
        gdk_gravity = GDK_GRAVITY_NORTH_EAST;
        break;
    case HIPPO_GRAVITY_SOUTH_EAST:
        gdk_gravity = GDK_GRAVITY_SOUTH_EAST;
        break;
    case HIPPO_GRAVITY_SOUTH_WEST:
        gdk_gravity = GDK_GRAVITY_SOUTH_WEST;
        break;
    }

    gtk_window_set_gravity(GTK_WINDOW(window_gtk), gdk_gravity);
#endif
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
    case PROP_RESIZE_GRAVITY:
        set_resize_gravity(gtk, g_value_get_int(value));
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
    case PROP_RESIZE_GRAVITY:
        g_value_set_int(value, gtk->resize_gravity);
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

static void
set_static_bit_gravity(GtkWidget *widget)
{
  XSetWindowAttributes xattributes;
  guint xattributes_mask = 0;
  
  xattributes.bit_gravity = StaticGravity;
  xattributes_mask |= CWBitGravity;
  XChangeWindowAttributes (GDK_WINDOW_XDISPLAY(widget->window),
			   GDK_WINDOW_XID(widget->window),
			   CWBitGravity,  &xattributes);
}

static void
hippo_window_gtk_realize(GtkWidget *widget)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(widget);
    
    GTK_WIDGET_CLASS(hippo_window_gtk_parent_class)->realize(widget);

    /* Static bit gravity means that when we are simultaneously moved
     * and resized, the windowing system should just leave our image
     * bits where they were before. This prevents bouncing of elements
     * that are glued to a right/bottom edge that doesn't move -
     * otherwise the windowing system would move the bits, then we'd
     * redraw them back where they were before.
     */
    set_static_bit_gravity(widget);

    /* When we are a notification window (not an "app window") then
     * we'll never get exposed by someone dragging another window
     * on top, so it will work well to use a background of "none";
     * this considerably improves the appearance when we are
     * spontaneously resizing, since you don't get a blank area first.
     */
    if (!window_gtk->app_window)
        gdk_window_set_back_pixmap(widget->window, NULL, FALSE);
}

static gboolean 
hippo_window_gtk_expose_event(GtkWidget           *widget,
                              GdkEventExpose      *event)
{
    /* We don't want to redraw between requesting a new position/size
     * from the window manager and getting allocated that size because
     * we won't yet have made all the reallocations for the new size,
     * but may already have the new coordinates after the resize. We
     * count here on a) the resize not being denied by the window manager
     * b) on the complete redraw we currently do at each new size.
     */
    if (GTK_WINDOW(widget)->configure_request_count > 0)
        return FALSE;

    /* Ignoring the expose event while we are waiting for the window manager
     * to resize us would cause problems if we were normally double-buffered
     * because when we ignored the expose event, a blank double-buffer would
     * get drawn. So what we do is turn off double-buffering at the widget
     * level and do it ourselves here.
     */
    gdk_window_begin_paint_region(widget->window, event->region);
    GTK_WIDGET_CLASS(hippo_window_gtk_parent_class)->expose_event(widget, event);
    gdk_window_end_paint(widget->window);

    return FALSE;
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
hippo_window_gtk_check_resize (GtkContainer *container)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(container);

    /**
     * Is gtk_window_gravity_sufficient() to make our resize gravity work?
     *
     * The ICCCM is unclear on what a ConfigureRequest that doesn't specify
     * a new position means when gravity is set. (ICCCM section 4.1.5:
     * "Client configure requests are interpreted by the window manager in 
     * the same manner as the initial window geometry mapped from the Withdrawn
     * state, as described in section 4.1.2.3.")  But in any case, we can't
     * count on the window manager to turn a a Resize() on our part into
     * a MoveResize(), because any handling by the window manager only happens
     * after our window is managed, which which happens at a point we don't
     * control. So on spontaneous size changes, we need to do the right
     * MoveRequest. (Actually, it's  probably a GTK+ bug that it doesn't take
     * care of this for us: filed as http://bugzilla.gnome.org/show_bug.cgi?id=362383)
     *
     * Note that a limitation of this code is that it only works when the
     * window is exactly sized to its requisition. (not resizable) In other
     * cases, we have no access to the new size that GTK+ will use, so we
     * can't do the necessary arithmetic.
     *
     * There is also a remaining race condition here: we first set our new
     * geometry hints, then do the resize. If the window manager processes the
     * new hints and uses them to constrain the window to the new size before
     * it handles the ConfigureRequest, then we'll still get a move/resize
     * rather than a MoveResize. This is very hard to work around within the
     * limits of the existing GtkWindow since "non-resizable" is how we indicate
     * to GTK+ that we want the size to match the requisition. The easiest
     * way to get around this would likely be to override all the GtkWindow 
     * complex resizing mechanics with something much simpler for the 
     * non-app-window case.
     *
     * Luckily it also usually doesn't occur with metacity because metacity
     * queues up the hint change and handles it asynchronously. I've only
     * seen it running --sync. But other window managers might be less smart.
     */
    if (window_gtk->resize_gravity != HIPPO_GRAVITY_NORTH_WEST && window_gtk->positioned) {
        GtkRequisition new_requisition;
        int x, y;
        int new_x, new_y;

        gtk_window_get_position(GTK_WINDOW(window_gtk), &x, &y);
        new_x = x;
        new_y = y;

        gtk_widget_size_request(GTK_WIDGET(window_gtk), &new_requisition);

        switch (window_gtk->resize_gravity) {
        case HIPPO_GRAVITY_NORTH_WEST:
            /* Not hit */
            break;
        case HIPPO_GRAVITY_NORTH_EAST:
            new_x -= new_requisition.width - window_gtk->last_position_requisition.width;
            break;
        case HIPPO_GRAVITY_SOUTH_EAST:
            new_x -= new_requisition.width - window_gtk->last_position_requisition.width;
            new_y -= new_requisition.height - window_gtk->last_position_requisition.height;
            break;
        case HIPPO_GRAVITY_SOUTH_WEST:
            new_y -= new_requisition.height - window_gtk->last_position_requisition.height;
            break;
        }

        if (x != new_x || y != new_y) {
            /* Horrible hack - we don't want want gtk_window_move() to gdk_window_move(),
             * which it would normally when we are mapped, we instead want to coalesce
             * the move with the resize we know we are about to do into a single
             * gkd_window_move_resize() so the gravity setting works right.
             */
            gboolean was_mapped = GTK_WIDGET_MAPPED(window_gtk);
            if (was_mapped)
                GTK_WIDGET_UNSET_FLAGS(window_gtk, GTK_MAPPED);
            GTK_WINDOW(window_gtk)->need_default_position = TRUE;
            gtk_window_move(GTK_WINDOW(window_gtk), new_x, new_y);
            if (was_mapped)
                GTK_WIDGET_SET_FLAGS(window_gtk, GTK_MAPPED);
        }
        
        window_gtk->last_position_requisition = new_requisition;
    }
    
    GTK_CONTAINER_CLASS(hippo_window_gtk_parent_class)->check_resize(container);
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

    hippo_canvas_window_set_root(HIPPO_CANVAS_WINDOW(window_gtk), window_gtk->contents);
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

    gtk_widget_size_request(GTK_WIDGET(window_gtk), &window_gtk->last_position_requisition);
    window_gtk->positioned = TRUE;

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

