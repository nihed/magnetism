/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"

static void      hippo_canvas_init                (HippoCanvas       *canvas);
static void      hippo_canvas_class_init          (HippoCanvasClass  *klass);
static void      hippo_canvas_finalize            (GObject           *object);

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


struct _HippoCanvas {
    GtkWidget parent;

    HippoCanvasItem *root;
};

struct _HippoCanvasClass {
    GtkWidgetClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE(HippoCanvas, hippo_canvas, GTK_TYPE_WIDGET);

static void
hippo_canvas_init(HippoCanvas *canvas)
{
    /* should maybe have a window, but for now I'm too lazy to implement _realize() */
    GTK_WIDGET_SET_FLAGS(canvas, GTK_NO_WINDOW);
}

static void
hippo_canvas_class_init(HippoCanvasClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    GtkWidgetClass *widget_class = GTK_WIDGET_CLASS(klass);

    object_class->set_property = hippo_canvas_set_property;
    object_class->get_property = hippo_canvas_get_property;

    object_class->finalize = hippo_canvas_finalize;

    widget_class->expose_event = hippo_canvas_expose_event;
    widget_class->size_request = hippo_canvas_size_request;
    widget_class->size_allocate = hippo_canvas_size_allocate;
}

static void
hippo_canvas_finalize(GObject *object)
{
    HippoCanvas *canvas = HIPPO_CANVAS(object);

    hippo_canvas_set_root(canvas, NULL);
    
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

static gboolean
hippo_canvas_expose_event(GtkWidget         *widget,
                          GdkEventExpose    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    HippoDrawable *hdrawable;
    cairo_t *cr;
    
    if (canvas->root == NULL)
        return FALSE;

    cr = gdk_cairo_create(event->window);
    hdrawable = hippo_drawable_new(cr);
    
    hippo_canvas_item_paint(canvas->root, hdrawable);

    hippo_drawable_free(hdrawable);
    cairo_destroy(cr);
    
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
}

static void
hippo_canvas_size_allocate(GtkWidget         *widget,
                           GtkAllocation     *allocation)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    /* assign widget->allocation and resize widget->window */
    GTK_WIDGET_CLASS(hippo_canvas_parent_class)->size_allocate(widget, allocation);
    
    if (canvas->root != NULL) {
        int x, y;
        if (GTK_WIDGET_NO_WINDOW(widget)) {
            x = allocation->x;
            y = allocation->y;
        } else {
            x = 0;
            y = 0;
        }
            
        hippo_canvas_item_allocate(canvas->root,
                                   x, y,
                                   allocation->width,
                                   allocation->height);
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
canvas_root_request_changed(HippoCanvasItem *root,
                            HippoCanvas     *canvas)
{
    gtk_widget_queue_resize(GTK_WIDGET(canvas));
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
        g_object_unref(canvas->root);
        canvas->root = NULL;
    }

    if (root != NULL) {
        g_object_ref(root);
        g_signal_connect(root, "request-changed",
                         G_CALLBACK(canvas_root_request_changed),
                         canvas);
        canvas->root = root;
    }

    gtk_widget_queue_resize(GTK_WIDGET(canvas));
}

/*
 * Move this HippoDrawable stuff cross-platform
 * once we have Cairo set up on Windows.
 * Though I'm not sure this is what we want; we might
 * want it platform-specific so we can have each paint
 * method get a new cairo_t from gdk_cairo_create() which
 * requires the GdkDrawable
 */
struct _HippoDrawable {
    cairo_t *cr;
};

HippoDrawable*
hippo_drawable_new(cairo_t *cr)
{
    HippoDrawable *d;

    d = g_new0(HippoDrawable, 1);
    d->cr = cr;
    cairo_reference(d->cr);

    return d;
}

void
hippo_drawable_free(HippoDrawable *drawable)
{
    cairo_destroy(drawable->cr);
    g_free(drawable);
}

cairo_t*
hippo_drawable_get_cairo(HippoDrawable *drawable)
{
    return drawable->cr;
}

/* FIXME ideally these are called automatically, not inside paint() implementations,
 * but right now we don't have cairo available in hippo-canvas-box.c so can't
 */
void
hippo_canvas_item_push_cairo(HippoCanvasItem *item,
                             cairo_t         *cr)
{
    int x, y, width, height;
    
    cairo_save(cr);

    hippo_canvas_item_get_allocation(item, &x, &y, &width, &height);
    /* Make our allocation x,y the origin */
    cairo_translate(cr, x, y);
    /* Set our allocation as the clip region */
    cairo_rectangle(cr, 0, 0, width, height);
    cairo_clip(cr);
}

void
hippo_canvas_item_pop_cairo (HippoCanvasItem *item,
                             cairo_t         *cr)
{
    cairo_restore(cr);
}


/* TEST CODE */
#if 1
#include <gtk/gtk.h>
#include <hippo/hippo-canvas-box.h>
#include "hippo-canvas-shape.h"

void
hippo_canvas_open_test_window(void)
{
    GtkWidget *window;
    GtkWidget *canvas;
    HippoCanvasItem *root;
    HippoCanvasItem *shape1;
    HippoCanvasItem *shape2;

    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);
    gtk_container_add(GTK_CONTAINER(window), canvas);
    
    root = HIPPO_CANVAS_ITEM(hippo_canvas_box_new());

    shape1 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 100,
                          NULL);
                          
    shape2 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 70, "height", 40,
                          "color", 0x00ff00ff,
                          NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape1, 0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape2, 0);
    
    hippo_canvas_set_root(HIPPO_CANVAS(canvas), root);
    g_object_unref(root);

    gtk_widget_show(window);
}

#endif /* test code */
