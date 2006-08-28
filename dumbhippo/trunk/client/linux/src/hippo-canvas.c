/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#include "hippo-canvas-context.h"

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

static PangoLayout* hippo_canvas_create_layout    (HippoCanvasContext *context);

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

G_DEFINE_TYPE_WITH_CODE(HippoCanvas, hippo_canvas, GTK_TYPE_WIDGET,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT,
                                              hippo_canvas_iface_init));

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
    widget_class->button_press_event = hippo_canvas_button_press;
}

static void
hippo_canvas_iface_init (HippoCanvasContextClass *klass)
{
    klass->create_layout = hippo_canvas_create_layout;
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

    return hippo_canvas_item_emit_button_press_event(canvas->root,
                                                     event->x - widget->allocation.x,
                                                     event->y - widget->allocation.y);
}

static PangoLayout*
hippo_canvas_create_layout(HippoCanvasContext *context)
{
    HippoCanvas *canvas = HIPPO_CANVAS(context);
    return gtk_widget_create_pango_layout(GTK_WIDGET(canvas), NULL);
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
#include "hippo-canvas-shape.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-link.h"

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
        
        shape = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                             "width", attrs->width,
                             "height", attrs->height,
                             "color", attrs->color,
                             "background-color", 0xffffffff,
                             "xalign", attrs->alignment,
                             NULL);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), shape, attrs->flags);
        g_object_unref(shape);
        
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
        g_object_unref(label);
    }
    return row;
}

void
hippo_canvas_open_test_window(void)
{
    GtkWidget *window;
    GtkWidget *canvas;
    HippoCanvasItem *root;
    HippoCanvasItem *shape1;
    HippoCanvasItem *shape2;
    HippoCanvasItem *text;
    int i;
    
    window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
    gtk_container_set_border_width(GTK_CONTAINER(window), 10);
    canvas = hippo_canvas_new();
    gtk_widget_show(canvas);
    gtk_container_add(GTK_CONTAINER(window), canvas);

    root = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "spacing", 8,
                        NULL);

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
    g_object_unref(shape1);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape2, 0);
    g_object_unref(shape2);
#endif
    
    for (i = 0; i < (int) G_N_ELEMENTS(box_rows); ++i) {
        HippoCanvasItem *row = create_row(box_rows[i]);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, 0);
        g_object_unref(row);
    }

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Click here",
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
    g_object_unref(text);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "fixed-width", 400,
                        "text",
                        "This is some long text that may help in testing resize behavior. It goes "
                        "on for a while, so don't get impatient. More and more and  more text. "
                        "Text text text. Lorem ipsum! Text! This is the story of text.",
                        "background-color", 0x0000ff00,
                        "yalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
    g_object_unref(text);
    
    hippo_canvas_set_root(HIPPO_CANVAS(canvas), root);
    g_object_unref(root);

    gtk_widget_show(window);
}

#endif /* test code */
