/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#include "hippo-canvas-context.h"
#include <gtk/gtkeventbox.h>
#include "hippo-embedded-image.h"

static void hippo_canvas_init       (HippoCanvas             *canvas);
static void hippo_canvas_class_init (HippoCanvasClass        *klass);
static void hippo_canvas_finalize   (GObject                 *object);
static void hippo_canvas_iface_init (HippoCanvasContextClass *klass);


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

static PangoLayout*     hippo_canvas_create_layout    (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_load_image       (HippoCanvasContext *context,
                                                       const char         *image_name);

struct _HippoCanvas {
    GtkEventBox parent;

    HippoCanvasItem *root;

    HippoCanvasPointer pointer;
};

struct _HippoCanvasClass {
    GtkEventBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvas, hippo_canvas, GTK_TYPE_EVENT_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT,
                                              hippo_canvas_iface_init));

static void
hippo_canvas_init(HippoCanvas *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);
    
    /* tells event box to create an input-only window on top */
    GTK_WIDGET_SET_FLAGS(widget, GTK_NO_WINDOW);

    gtk_widget_add_events(widget, GDK_POINTER_MOTION_MASK | GDK_POINTER_MOTION_HINT_MASK |
                          GDK_ENTER_NOTIFY_MASK | GDK_LEAVE_NOTIFY_MASK | GDK_BUTTON_PRESS);

    canvas->pointer = HIPPO_CANVAS_POINTER_UNSET;
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
    widget_class->button_press_event = hippo_canvas_button_press;
    widget_class->button_release_event = hippo_canvas_button_release;
    widget_class->motion_notify_event = hippo_canvas_motion_notify;
    widget_class->enter_notify_event = hippo_canvas_enter_notify;
    widget_class->leave_notify_event = hippo_canvas_leave_notify;
}

static void
hippo_canvas_iface_init (HippoCanvasContextClass *klass)
{
    klass->create_layout = hippo_canvas_create_layout;
    klass->load_image = hippo_canvas_load_image;
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

/* whee, circumvent GtkEventBoxPrivate */
static GdkWindow*
event_box_get_event_window(GtkEventBox *event_box)
{
    GList *children;
    GdkWindow *event_window;
    GList *link;
    void *user_data;
    GtkWidget *widget;
    
    g_return_val_if_fail(GTK_IS_EVENT_BOX(event_box), NULL);
    g_return_val_if_fail(GTK_WIDGET_REALIZED(event_box), NULL);
    
    widget = GTK_WIDGET(event_box);
    g_return_val_if_fail(widget->window != NULL, NULL);
    
    if (gtk_event_box_get_visible_window(event_box)) {
        return widget->window;
    }
    
    /* event_box->window is the parent window of the event box */
    
    children = gdk_window_get_children(widget->window);
    
    event_window = NULL;
    for (link = children; link != NULL; link = link->next) {
        event_window = children->data;
        user_data = NULL;
        gdk_window_get_user_data(event_window, &user_data);        
        if (GDK_WINDOW_OBJECT(event_window)->input_only &&
            user_data == event_box) {
            break;
        }
        event_window = NULL;
    }

    if (event_window == NULL) {
        g_warning("did not find event box input window, %d children of %s",
                  g_list_length(children), G_OBJECT_TYPE_NAME(event_box));
    }
    
    g_list_free(children);
    
    return event_window;
}

static void
set_pointer(HippoCanvas       *canvas,
            HippoCanvasPointer pointer)
{
    GdkCursor *cursor;
    GdkWindow *event_window;
    GtkWidget *widget;
    GtkEventBox *event_box;

    /* important optimization since we do this on all motion notify */
    if (canvas->pointer == pointer)
        return;

    widget = GTK_WIDGET(canvas);
    event_box = GTK_EVENT_BOX(canvas);

    canvas->pointer = pointer;

    if (pointer == HIPPO_CANVAS_POINTER_UNSET ||
        pointer == HIPPO_CANVAS_POINTER_DEFAULT)
        cursor = NULL;
    else {
        GdkCursorType type = GDK_X_CURSOR;
        switch (pointer) {
        case HIPPO_CANVAS_POINTER_HAND:
            type = GDK_HAND2;
            break;
        case HIPPO_CANVAS_POINTER_UNSET:
        case HIPPO_CANVAS_POINTER_DEFAULT:
            g_assert_not_reached();
            break;
            /* don't add a default, breaks compiler warnings */
        }
        cursor = gdk_cursor_new_for_display(gtk_widget_get_display(widget),
                                            type);
    }

    event_window = event_box_get_event_window(event_box);
    gdk_window_set_cursor(event_window, cursor);
    
    gdk_display_flush(gtk_widget_get_display(widget));

    if (cursor != NULL)
        gdk_cursor_unref(cursor);
}

static gboolean
hippo_canvas_expose_event(GtkWidget         *widget,
                          GdkEventExpose    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    cairo_t *cr;
    
    if (canvas->root == NULL)
        return FALSE;

    cr = gdk_cairo_create(event->window);
    hippo_canvas_item_process_paint(canvas->root, cr,
                                    widget->allocation.x, widget->allocation.y);
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
        hippo_canvas_item_allocate(canvas->root,
                                   allocation->width,
                                   allocation->height);
    }
}

static gboolean
hippo_canvas_button_press(GtkWidget         *widget,
                          GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (canvas->root == NULL)
        return FALSE;

    /*
    g_debug("canvas button press at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */
    
    return hippo_canvas_item_emit_button_press_event(canvas->root,
                                                     event->x, event->y,
                                                     event->button);
}

static gboolean
hippo_canvas_button_release(GtkWidget         *widget,
                            GdkEventButton    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);

    if (canvas->root == NULL)
        return FALSE;

    /*
    g_debug("canvas button release at %d,%d allocation %d,%d", (int) event->x, (int) event->y,
            widget->allocation.x, widget->allocation.y);
    */
    
    return hippo_canvas_item_emit_button_release_event(canvas->root,
                                                       event->x, event->y,
                                                       event->button);
}

static void
handle_new_mouse_location(HippoCanvas *canvas,
                          GdkWindow   *event_window,
                          HippoMotionDetail detail)
{
    int x, y;
    HippoCanvasPointer pointer;
    
    gdk_window_get_pointer(event_window, &x, &y, NULL);

    pointer = hippo_canvas_item_get_pointer(canvas->root, x, y);
    set_pointer(canvas, pointer);
    
    hippo_canvas_item_emit_motion_notify_event(canvas->root,
                                               x, y, detail);
}

static gboolean
hippo_canvas_enter_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    
    if (canvas->root == NULL)
        return FALSE;

    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_ENTER);
    
    return FALSE;
}

static gboolean
hippo_canvas_leave_notify(GtkWidget         *widget,
                          GdkEventCrossing  *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);
    
    if (canvas->root == NULL)
        return FALSE;
    
    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_LEAVE);
    
    return FALSE;
}

static gboolean
hippo_canvas_motion_notify(GtkWidget         *widget,
                           GdkEventMotion    *event)
{
    HippoCanvas *canvas = HIPPO_CANVAS(widget);    
    
    if (canvas->root == NULL)
        return FALSE;

    handle_new_mouse_location(canvas, event->window, HIPPO_MOTION_DETAIL_WITHIN);
    
    return FALSE;
}

static PangoLayout*
hippo_canvas_create_layout(HippoCanvasContext *context)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);
    return gtk_widget_create_pango_layout(GTK_WIDGET(canvas), NULL);
}

/* This is copied from gdk_cairo_set_source_pixbuf()
 * in GDK
 */
static cairo_surface_t*
cairo_surface_from_pixbuf(GdkPixbuf *pixbuf)
{
    int width = gdk_pixbuf_get_width (pixbuf);
    int height = gdk_pixbuf_get_height (pixbuf);
    guchar *gdk_pixels = gdk_pixbuf_get_pixels (pixbuf);
    int gdk_rowstride = gdk_pixbuf_get_rowstride (pixbuf);
    int n_channels = gdk_pixbuf_get_n_channels (pixbuf);
    guchar *cairo_pixels;
    cairo_format_t format;
    cairo_surface_t *surface;
    static const cairo_user_data_key_t key;
    int j;
    
    if (n_channels == 3)
        format = CAIRO_FORMAT_RGB24;
    else
        format = CAIRO_FORMAT_ARGB32;

    cairo_pixels = g_malloc(4 * width * height);
    surface = cairo_image_surface_create_for_data((unsigned char *)cairo_pixels,
                                                  format,
                                                  width, height, 4 * width);
    cairo_surface_set_user_data(surface, &key,
                                cairo_pixels, (cairo_destroy_func_t)g_free);

    for (j = height; j; j--) {
        guchar *p = gdk_pixels;
        guchar *q = cairo_pixels;

        if (n_channels == 3) {
            guchar *end = p + 3 * width;
	  
            while (p < end) {
#if G_BYTE_ORDER == G_LITTLE_ENDIAN
                q[0] = p[2];
                q[1] = p[1];
                q[2] = p[0];
#else	  
                q[1] = p[0];
                q[2] = p[1];
                q[3] = p[2];
#endif
                p += 3;
                q += 4;
            }
        } else {
            guchar *end = p + 4 * width;
            guint t1,t2,t3;
	    
#define MULT(d,c,a,t) G_STMT_START { t = c * a + 0x7f; d = ((t >> 8) + t) >> 8; } G_STMT_END

            while (p < end) {
#if G_BYTE_ORDER == G_LITTLE_ENDIAN
                MULT(q[0], p[2], p[3], t1);
                MULT(q[1], p[1], p[3], t2);
                MULT(q[2], p[0], p[3], t3);
                q[3] = p[3];
#else	  
                q[0] = p[3];
                MULT(q[1], p[0], p[3], t1);
                MULT(q[2], p[1], p[3], t2);
                MULT(q[3], p[2], p[3], t3);
#endif
                
                p += 4;
                q += 4;
            }            
#undef MULT
        }

        gdk_pixels += gdk_rowstride;
        cairo_pixels += 4 * width;
    }
    return surface;
}

static cairo_surface_t*
hippo_canvas_load_image(HippoCanvasContext *context,
                        const char         *image_name)
{
    /* HippoCanvas *canvas = HIPPO_CANVAS(context); */
    GdkPixbuf *pixbuf;
    cairo_surface_t *surface;

    pixbuf = hippo_embedded_image_get(image_name);
    if (pixbuf == NULL) {
        return NULL;
    }

    surface = g_object_get_data(G_OBJECT(pixbuf),
                                "hippo-cairo-surface");
    if (surface == NULL) {
        surface = cairo_surface_from_pixbuf(pixbuf);
        g_object_set_data_full(G_OBJECT(pixbuf),
                               "hippo-cairo-surface",
                               surface,
                               (GDestroyNotify) cairo_surface_destroy);
    }

    cairo_surface_reference(surface);
    return surface;
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

static void
canvas_root_paint_needed(HippoCanvasItem *root,
                         int              x,
                         int              y,
                         int              width,
                         int              height,
                         HippoCanvas     *canvas)
{
    GtkWidget *widget = GTK_WIDGET(canvas);
    
    gtk_widget_queue_draw_area(widget,
                               widget->allocation.x + x,
                               widget->allocation.y + y,
                               width, height);
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
        g_signal_handlers_disconnect_by_func(canvas->root,
                                             G_CALLBACK(canvas_root_paint_needed),
                                             canvas);        
        hippo_canvas_item_set_context(canvas->root, NULL);
        g_object_unref(canvas->root);
        canvas->root = NULL;
    }

    if (root != NULL) {
        g_object_ref(root);
        hippo_canvas_item_sink(root);
        canvas->root = root;
        g_signal_connect(root, "request-changed",
                         G_CALLBACK(canvas_root_request_changed),
                         canvas);
        g_signal_connect(root, "paint-needed",
                         G_CALLBACK(canvas_root_paint_needed),
                         canvas);
        hippo_canvas_item_set_context(canvas->root, HIPPO_CANVAS_CONTEXT(canvas));
    }

    gtk_widget_queue_resize(GTK_WIDGET(canvas));
}

/* TEST CODE */
#if 1
#include <gtk/gtk.h>
#include "hippo-canvas-box.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-link.h"
#include "hippo-canvas-image.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-stack.h"

typedef struct {
    int width;
    int height;
    guint32 color;
    HippoPackFlags flags;
    HippoItemAlignment alignment;
} BoxAttrs;

static BoxAttrs single_start[] = { { 40, 80, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_end[] = { { 100, 60, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_start[] = { { 50, 90, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL },
                                   { 50, 90, 0xff000099, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_end[] = { { 45, 55, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
                                 { 45, 55, 0x00ff0077, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand_end[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs everything[] = {
    { 120, 50, 0x00ccccff, 0, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 0, 0, 0, 0 }
};
static BoxAttrs alignments[] = {
    { 120, 50, 0x00ffcccc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccffcc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_START },
    { 120, 50, 0x00cffffc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_CENTER },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_END },
    { 0, 0, 0, 0 }
};

static BoxAttrs* box_rows[] = { single_start, /* double_start,*/ single_end, /* double_end, */
                                single_expand, everything, alignments };

static HippoCanvasItem*
create_row(BoxAttrs *boxes)
{
    HippoCanvasItem *row;
    int i;
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);

    for (i = 0; boxes[i].width > 0; ++i) {
        BoxAttrs *attrs = &boxes[i];
        HippoCanvasItem *shape;
        HippoCanvasItem *label;
        const char *flags_text;
        const char *align_text;
        char *s;
        
        shape = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                             /* "width", attrs->width,
                                "height", attrs->height, */
                             /* "color", attrs->color, */
                             "background-color", 0xffffffff,
                             "xalign", attrs->alignment,
                             NULL);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), shape, attrs->flags);
        
        if (attrs->flags == (HIPPO_PACK_END | HIPPO_PACK_EXPAND))
            flags_text = "END | EXPAND";
        else if (attrs->flags == HIPPO_PACK_END)
            flags_text = "END";
        else if (attrs->flags == HIPPO_PACK_EXPAND)
            flags_text = "EXPAND";
        else
            flags_text = "0";

        switch (attrs->alignment) {
        case HIPPO_ALIGNMENT_FILL:
            align_text = "FILL";
            break;
        case HIPPO_ALIGNMENT_START:
            align_text = "START";
            break;
        case HIPPO_ALIGNMENT_END:
            align_text = "END";
            break;
        case HIPPO_ALIGNMENT_CENTER:
            align_text = "CENTER";
            break;
        default:
            align_text = NULL;
            break;
        }

        s = g_strdup_printf("%s, %s", flags_text, align_text);
        label = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                             "text", s,
                             "background-color", 0x0000ff00,
                             NULL);
        g_free(s);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape), label, HIPPO_PACK_EXPAND);
    }
    return row;
}

void
hippo_canvas_open_test_window(void)
{
    GtkWidget *window;
    GtkWidget *scrolled;
    GtkWidget *canvas;
    HippoCanvasItem *root;
    HippoCanvasItem *shape1;
    HippoCanvasItem *shape2;
    HippoCanvasItem *text;
    HippoCanvasItem *image;
    HippoCanvasItem *row;
    int i;
    
    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);

    scrolled = gtk_scrolled_window_new(NULL,NULL);
    gtk_scrolled_window_set_policy(GTK_SCROLLED_WINDOW(scrolled),
                                   GTK_POLICY_AUTOMATIC,
                                   GTK_POLICY_AUTOMATIC);
    gtk_container_add(GTK_CONTAINER(window), scrolled);
    gtk_widget_show(scrolled);
    
    gtk_scrolled_window_add_with_viewport(GTK_SCROLLED_WINDOW(scrolled),
                                          canvas);

#if 0
    root = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                        "fixed-width", 400,
                        "spacing", 8,
                        NULL);

    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
#if 0
#if 1
    shape1 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0xaeaeaeff,
                          "padding", 20,
                          NULL);

    shape2 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0x00ff00ff,
                          "padding", 10,
                          NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape1, 0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape2, 0);
#endif
    
    for (i = 0; i < (int) G_N_ELEMENTS(box_rows); ++i) {
        row = create_row(box_rows[i]);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, 0);
    }

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Click here",
                        "background-color", 0x990000ff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);

    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, HIPPO_PACK_EXPAND);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "chaticon",
                         "xalign", HIPPO_ALIGNMENT_START,
                         "yalign", HIPPO_ALIGNMENT_END,
                         "background-color", 0xffff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, 0);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "ignoreicon",
                         "xalign", HIPPO_ALIGNMENT_FILL,
                         "yalign", HIPPO_ALIGNMENT_FILL,
                         "background-color", 0x00ff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, HIPPO_PACK_EXPAND);

    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "text",
                        "This is some long text that may help in testing resize behavior. It goes "
                        "on for a while, so don't get impatient. More and more and  more text. "
                        "Text text text. Lorem ipsum! Text! This is the story of text.",
                        "background-color", 0x0000ff00,
                        "yalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
#endif
#endif

    root = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "orientation", HIPPO_ORIENTATION_VERTICAL,
                        "border", 15,
                        "border-color", 0xff0000ff,
                        "padding", 25,
                        "background-color", 0x00ff00ff,
                        NULL);

#if 1
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                        "Click here",
                        "color", 0xffffffff,
                        "background-color", 0x0000ffff,
                        NULL);
#else
    text = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "nophoto",
                        "background-color", 0x0000ffff,
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
#endif
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
    
    hippo_canvas_set_root(HIPPO_CANVAS(canvas), root);

    gtk_window_set_default_size(GTK_WINDOW(window), 300, 300);
    gtk_widget_show(window);
}

#endif /* test code */
