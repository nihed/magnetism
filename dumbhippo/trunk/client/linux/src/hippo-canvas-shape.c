/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-shape.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_shape_init                (HippoCanvasShape       *shape);
static void      hippo_canvas_shape_class_init          (HippoCanvasShapeClass  *klass);
static void      hippo_canvas_shape_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_shape_finalize            (GObject                *object);

static void hippo_canvas_shape_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_shape_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_shape_paint              (HippoCanvasItem *item,
                                                       cairo_t         *cr);
static int      hippo_canvas_shape_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_shape_get_height_request (HippoCanvasItem *item,
                                                       int              for_width);
static gboolean hippo_canvas_shape_button_press_event (HippoCanvasItem *item,
                                                       HippoEvent      *event);

struct _HippoCanvasShape {
    HippoCanvasBox box;
    int width;
    int height;
    guint32 color_rgba;
};

struct _HippoCanvasShapeClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_COLOR,
    PROP_WIDTH,
    PROP_HEIGHT
};

#define DEFAULT_FOREGROUND 0x000000ff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasShape, hippo_canvas_shape, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_shape_iface_init));

static void
hippo_canvas_shape_init(HippoCanvasShape *shape)
{
    shape->color_rgba = DEFAULT_FOREGROUND;
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_shape_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_shape_paint;
    item_class->get_width_request = hippo_canvas_shape_get_width_request;
    item_class->get_height_request = hippo_canvas_shape_get_height_request;
    item_class->button_press_event = hippo_canvas_shape_button_press_event;
}

static void
hippo_canvas_shape_class_init(HippoCanvasShapeClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_shape_set_property;
    object_class->get_property = hippo_canvas_shape_get_property;

    object_class->finalize = hippo_canvas_shape_finalize;

    g_object_class_install_property(object_class,
                                    PROP_WIDTH,
                                    g_param_spec_int("width",
                                                     _("Width"),
                                                     _("Width of the shape"),
                                                     0,
                                                     G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_HEIGHT,
                                    g_param_spec_int("height",
                                                     _("Height"),
                                                     _("Height of the shape"),
                                                     0,
                                                     G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_COLOR,
                                    g_param_spec_uint("color",
                                                      _("Foreground Color"),
                                                      _("32-bit RGBA foreground color"),
                                                      0,
                                                      G_MAXUINT,
                                                      DEFAULT_FOREGROUND,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_shape_finalize(GObject *object)
{
    /* HippoCanvasShape *shape = HIPPO_CANVAS_SHAPE(object); */


    G_OBJECT_CLASS(hippo_canvas_shape_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_shape_new(void)
{
    HippoCanvasShape *shape = g_object_new(HIPPO_TYPE_CANVAS_SHAPE, NULL);


    return HIPPO_CANVAS_ITEM(shape);
}

static void
hippo_canvas_shape_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasShape *shape;

    shape = HIPPO_CANVAS_SHAPE(object);

    switch (prop_id) {
    case PROP_WIDTH:
        shape->width = g_value_get_int(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(shape));
        break;
    case PROP_HEIGHT:
        shape->height = g_value_get_int(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(shape));        
        break;
    case PROP_COLOR:
        shape->color_rgba = g_value_get_uint(value);
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(shape), 0, 0, -1, -1);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_shape_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasShape *shape;

    shape = HIPPO_CANVAS_SHAPE (object);

    switch (prop_id) {
    case PROP_WIDTH:
        g_value_set_int(value, shape->width);
        break;
    case PROP_HEIGHT:
        g_value_set_int(value, shape->height);
        break;
    case PROP_COLOR:
        g_value_set_uint(value, shape->color_rgba);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_shape_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    HippoCanvasShape *shape = HIPPO_CANVAS_SHAPE(item);
    int x, y, w, h;
    
    /* Draw the background and any children */
    item_parent_class->paint(item, cr);

    /* Draw the shape */
    x = 0;
    y = 0;
    w = shape->width;
    h = shape->height;

    hippo_canvas_box_align(HIPPO_CANVAS_BOX(item), &x, &y, &w, &h);
    
    if ((shape->color_rgba & 0xff) != 0) {
        hippo_cairo_set_source_rgba32(cr, shape->color_rgba);
        cairo_set_line_width(cr, 2.0);
        cairo_rectangle(cr, x, y, w, h);
        cairo_stroke(cr);
    }
}

static int
hippo_canvas_shape_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasShape *shape = HIPPO_CANVAS_SHAPE(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_width;

    /* get width of children and the box padding */
    children_width = item_parent_class->get_width_request(item);

    if (hippo_canvas_box_get_fixed_width(HIPPO_CANVAS_BOX(item)) < 0) {
        return MAX(shape->width + box->padding_left + box->padding_right, children_width);
    } else {
        return children_width;
    }
}

static int
hippo_canvas_shape_get_height_request(HippoCanvasItem *item,
                                      int              for_width)
{
    HippoCanvasShape *shape = HIPPO_CANVAS_SHAPE(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_height;

    /* get height of children and the box padding */
    children_height = item_parent_class->get_height_request(item, for_width);

    return MAX(shape->height + box->padding_top + box->padding_bottom, children_height);
}

static gboolean
hippo_canvas_shape_button_press_event (HippoCanvasItem *item,
                                       HippoEvent      *event)
{
    /* HippoCanvasShape *shape = HIPPO_CANVAS_SHAPE(item); */

    /* see if a child wants it */
    return item_parent_class->button_press_event(item, event);
}
