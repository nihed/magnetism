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

/* Container methods */
static void hippo_window_gtk_check_resize (GtkContainer *container);

struct _HippoWindowGtk {
    GtkWindow window;
    GtkWidget *canvas;
    HippoGravity resize_gravity;
    GtkRequisition last_position_requisition;
    HippoWindowRole role;
    guint positioned : 1; /* Set if hippo_window_position was even called */
    guint expose_during_configure : 1; /* Set if we get an expose while waiting for
                                        * for a window manager response. */
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
};

G_DEFINE_TYPE(HippoWindowGtk, hippo_window_gtk, HIPPO_TYPE_CANVAS_WINDOW)

typedef struct {
    GdkWindowTypeHint type_hint;
    gboolean skip_taskbar_pager;
    gboolean accept_focus;
    gboolean no_background;
} RoleProperties;

static const RoleProperties role_properties[] = {
    /*                   type_hint,                   skip_taskbar_pager, accept_focus, no_background */
    /* APPLICATION */  { GDK_WINDOW_TYPE_HINT_NORMAL, FALSE,              TRUE,         FALSE  },
    /* NOTIFICATION */ { GDK_WINDOW_TYPE_HINT_DOCK,   TRUE,               FALSE,        TRUE  },
    /* INPUT_POPUP */  { GDK_WINDOW_TYPE_HINT_NORMAL,   FALSE,              TRUE,         TRUE  },
};

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

    container_class->check_resize = hippo_window_gtk_check_resize;
}

static void
hippo_window_gtk_init(HippoWindowGtk *window_gtk)
{
    window_gtk->role = HIPPO_WINDOW_ROLE_APPLICATION;

    gtk_window_set_resizable(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_set_decorated(GTK_WINDOW(window_gtk), FALSE);
    gtk_window_stick(GTK_WINDOW(window_gtk));

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

HippoWindowGtk *
hippo_window_gtk_new(void)
{
    return g_object_new(HIPPO_TYPE_WINDOW_GTK, NULL);
}

void
hippo_window_gtk_set_role(HippoWindowGtk *window_gtk,
                          HippoWindowRole role)
{
    GtkWindow *window = GTK_WINDOW(window_gtk);
    GtkWidget *widget = GTK_WIDGET(window_gtk);

    const RoleProperties *props;
    
    if (window_gtk->role == role)
        return;
    
    window_gtk->role = role;

    props = &role_properties[role];

    gtk_window_set_type_hint(window, props->type_hint);
    gtk_window_set_skip_taskbar_hint(window, props->skip_taskbar_pager);
    gtk_window_set_skip_pager_hint(window, props->skip_taskbar_pager);
    gtk_window_set_accept_focus(window, props->accept_focus);
   
    /* See note about "background none" in hippo_window_gtk_realize() */
    if (GTK_WIDGET_REALIZED(widget)) {
        if (props->no_background) {
            gdk_window_set_back_pixmap(widget->window, NULL, FALSE);
        } else {
            /* This should cause the right pixel to be set, replacing background-none */
            gtk_style_set_background (widget->style, widget->window, widget->state);
        }
    }
}

HippoWindowRole
hippo_window_gtk_get_role(HippoWindowGtk  *window_gtk)
{
    return window_gtk->role;
}

void
hippo_window_gtk_set_resize_gravity(HippoWindowGtk *window_gtk,
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

HippoGravity
hippo_window_gtk_get_resize_gravity (HippoWindowGtk *window_gtk)
{
    return window_gtk->resize_gravity;
}

void
hippo_window_gtk_set_position(HippoWindowGtk  *window_gtk,
                              int              x,
                              int              y)
{
    gtk_widget_size_request(GTK_WIDGET(window_gtk), &window_gtk->last_position_requisition);
    window_gtk->positioned = TRUE;

    gtk_window_move(GTK_WINDOW(window_gtk), x, y);
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

/* Windows of type INPUT_POPUP have to appear on top of all our other windows, but
 * they also have to have a type hint of NORMAL, or Compiz (at least) won't focus
 * them. To get a window with a NORMAL type hint to appear on top of a window
 * with a DOCK type hint, we have to set the transient parent of the normal
 * window. But passing in the transient parent window to here is a pain, so
 * we just grub through the global list of windows to find something appropriate.
 */
static void
hack_transient_for(HippoWindowGtk *window_gtk) 
{
    GList *toplevels = gtk_window_list_toplevels();
    GList *l;

    for (l = toplevels; l; l = l->next) {
        GtkWindow *other = l->data;
        if (other != (GtkWindow *)window_gtk &&
            GTK_WIDGET_VISIBLE(other) &&
            HIPPO_IS_WINDOW_GTK(other) &&
            HIPPO_WINDOW_GTK(other)->role == HIPPO_WINDOW_ROLE_NOTIFICATION) {
            gtk_window_set_transient_for(GTK_WINDOW(window_gtk), other);
            break;
        }
    }

    g_list_free(toplevels);
    
}

static void
hippo_window_gtk_realize(GtkWidget *widget)
{
    HippoWindowGtk *window_gtk = HIPPO_WINDOW_GTK(widget);

    if (window_gtk->role == HIPPO_WINDOW_ROLE_INPUT_POPUP) 
        hack_transient_for(window_gtk);
    
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
    if (role_properties[window_gtk->role].no_background)
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
