/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include <string.h>
#include <cairo/cairo.h>
#include "hippo-canvas-grip.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_grip_init                (HippoCanvasGrip       *grip);
static void      hippo_canvas_grip_class_init          (HippoCanvasGripClass  *klass);
static void      hippo_canvas_grip_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_grip_finalize            (GObject                *object);

static void hippo_canvas_grip_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_grip_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_grip_paint              (HippoCanvasItem *item,
                                                      cairo_t         *cr);
static int      hippo_canvas_grip_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_grip_get_height_request (HippoCanvasItem *item,
                                                      int              for_width);

struct _HippoCanvasGrip {
    HippoCanvasBox box;
    HippoSide side;
    guint prelighted : 1;
};

struct _HippoCanvasGripClass {
    HippoCanvasBoxClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_SIDE
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasGrip, hippo_canvas_grip, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_grip_iface_init));

static void
hippo_canvas_grip_init(HippoCanvasGrip *grip)
{
    grip->side = HIPPO_SIDE_TOP;
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_grip_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_grip_paint;
    item_class->get_width_request = hippo_canvas_grip_get_width_request;
    item_class->get_height_request = hippo_canvas_grip_get_height_request;
}

static void
hippo_canvas_grip_class_init(HippoCanvasGripClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_grip_set_property;
    object_class->get_property = hippo_canvas_grip_get_property;

    object_class->finalize = hippo_canvas_grip_finalize;

    g_object_class_install_property(object_class,
                                    PROP_SIDE,
                                    g_param_spec_int("side",
                                                     _("Side"),
                                                     _("Side of the window it's on"),
                                                     0,
                                                     G_MAXINT,
                                                     HIPPO_SIDE_TOP,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_grip_finalize(GObject *object)
{
    /* HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(object); */

    G_OBJECT_CLASS(hippo_canvas_grip_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_grip_new(void)
{
    HippoCanvasGrip *grip = g_object_new(HIPPO_TYPE_CANVAS_GRIP, NULL);


    return HIPPO_CANVAS_ITEM(grip);
}

static void
hippo_canvas_grip_set_property(GObject         *object,
                               guint            prop_id,
                               const GValue    *value,
                               GParamSpec      *pspec)
{
    HippoCanvasGrip *grip;

    grip = HIPPO_CANVAS_GRIP(object);

    switch (prop_id) {
    case PROP_SIDE:
        {
            HippoSide side = g_value_get_int(value);
            if (side != grip->side) {
                grip->side = side;
                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(grip));
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_grip_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasGrip *grip;

    grip = HIPPO_CANVAS_GRIP (object);

    switch (prop_id) {
    case PROP_SIDE:
        g_value_set_int(value, grip->side);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

#define GRIP_SIZE 8

static void
get_grip_request(HippoCanvasGrip *grip,
                 int             *w_p,
                 int             *h_p)
{
   if (grip->side == HIPPO_SIDE_TOP || grip->side == HIPPO_SIDE_BOTTOM) {
       if (w_p)
           *w_p = 0;
       if (h_p)
           *h_p = GRIP_SIZE;
    } else {
       if (w_p)
           *w_p = GRIP_SIZE;
       if (h_p)
           *h_p = 0;
    }
}

static void
hippo_canvas_grip_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(item);
    int x, y, w, h;

    /* Draw the background and any children */
    item_parent_class->paint(item, cr);

    /* Draw the grip */

    x = 0;
    y = 0;
    get_grip_request(grip, &w, &h);

    hippo_canvas_box_align(HIPPO_CANVAS_BOX(item), &x, &y, &w, &h);

    cairo_rectangle(cr, x, y, w, h);
    cairo_clip(cr);

    /* FIXME pick a sane color - add canvas_context_get_color(PRELIGHT) ? */
    if (grip->prelighted)
        hippo_cairo_set_source_rgba32(cr, 0xff0000ff);
    else
        hippo_cairo_set_source_rgba32(cr, 0x770000ff);
    
    cairo_paint(cr);
}

static int
hippo_canvas_grip_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_width;

    /* get width of children and the box padding */
    children_width = item_parent_class->get_width_request(item);

    if (hippo_canvas_box_get_fixed_width(HIPPO_CANVAS_BOX(item)) < 0) {
        int grip_width;
        get_grip_request(grip, &grip_width, NULL);
        return MAX(grip_width + box->padding_left + box->padding_right, children_width);
    } else {
        return children_width;
    }
}

static int
hippo_canvas_grip_get_height_request(HippoCanvasItem *item,
                                      int              for_width)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_height;
    int grip_height;

    /* get height of children and the box padding */
    children_height = item_parent_class->get_height_request(item, for_width);

    get_grip_request(grip, NULL, &grip_height);
    return MAX(grip_height + box->padding_top + box->padding_bottom, children_height);
}
