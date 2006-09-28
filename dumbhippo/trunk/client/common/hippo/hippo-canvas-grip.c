/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-grip.h"
#include <hippo/hippo-canvas-image.h>

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
static gboolean hippo_canvas_grip_motion_notify_event (HippoCanvasItem *item,
                                                       HippoEvent      *event);

/* Canvas box methods */
static void hippo_canvas_grip_paint_below_children       (HippoCanvasBox *box,
                                                          cairo_t        *cr,
                                                          HippoRectangle *damaged_box);
static int  hippo_canvas_grip_get_content_width_request  (HippoCanvasBox *box);
static int  hippo_canvas_grip_get_content_height_request (HippoCanvasBox *box,
                                                          int             for_width);


struct _HippoCanvasGrip {
    HippoCanvasImage image;
    HippoSide side;
    guint prelighted : 1;
};

struct _HippoCanvasGripClass {
    HippoCanvasImageClass parent_class;
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasGrip, hippo_canvas_grip, HIPPO_TYPE_CANVAS_IMAGE,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_grip_iface_init));

static void
hippo_canvas_grip_init(HippoCanvasGrip *grip)
{    
    grip->side = HIPPO_SIDE_TOP;
    g_object_set(G_OBJECT(grip),
                 "image-name", "lid",
                 "xalign", HIPPO_ALIGNMENT_CENTER,
                 "yalign", HIPPO_ALIGNMENT_CENTER,
                 "background-color", 0xecececff,
                 "border-top", 1, /* matches the default side of TOP */
                 "border-color", 0xbebebeff,
                 NULL);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_grip_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->motion_notify_event = hippo_canvas_grip_motion_notify_event;
}

static void
hippo_canvas_grip_class_init(HippoCanvasGripClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);
    
    object_class->set_property = hippo_canvas_grip_set_property;
    object_class->get_property = hippo_canvas_grip_get_property;

    object_class->finalize = hippo_canvas_grip_finalize;

    box_class->paint_below_children = hippo_canvas_grip_paint_below_children;
    box_class->get_content_width_request = hippo_canvas_grip_get_content_width_request;
    box_class->get_content_height_request = hippo_canvas_grip_get_content_height_request;
    
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

                g_object_set(G_OBJECT(grip),
                             "border-left", side == HIPPO_SIDE_LEFT ? 1 : 0,
                             "border-right", side == HIPPO_SIDE_RIGHT ? 1 : 0,
                             "border-top", side == HIPPO_SIDE_TOP ? 1 : 0,
                             "border-bottom", side == HIPPO_SIDE_BOTTOM ? 1 : 0,
                             NULL);
                
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
hippo_canvas_grip_paint_below_children(HippoCanvasBox  *box,
                                       cairo_t         *cr,
                                       HippoRectangle  *damaged_box)
{
    HIPPO_CANVAS_BOX_CLASS(hippo_canvas_grip_parent_class)->paint_below_children(box, cr, damaged_box);
    
#if 0
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(box);
    int x, y, w, h;

    get_grip_request(grip, &w, &h);

    hippo_canvas_box_align(box, w, h, &x, &y, &w, &h);

    cairo_rectangle(cr, x, y, w, h);
    cairo_clip(cr);

    if (grip->prelighted)
        hippo_cairo_set_source_rgba32(cr,
                                      hippo_canvas_context_get_color(box->context,
                                                                     HIPPO_STOCK_COLOR_BG_PRELIGHT));
    else
        hippo_cairo_set_source_rgba32(cr,
                                      hippo_canvas_context_get_color(box->context,
                                                                     HIPPO_STOCK_COLOR_BG_NORMAL));
    
    cairo_paint(cr);
#endif
}

static int
hippo_canvas_grip_get_content_width_request(HippoCanvasBox *box)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(box);
    int children_width;
    int grip_width;
    
    children_width = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_grip_parent_class)->get_content_width_request(box);

    get_grip_request(grip, &grip_width, NULL);
    return MAX(grip_width, children_width);
}

static int
hippo_canvas_grip_get_content_height_request(HippoCanvasBox *box,
                                             int             for_width)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(box);
    int children_height;
    int grip_height;
    
    children_height = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_grip_parent_class)->get_content_height_request(box, for_width);

    get_grip_request(grip, NULL, &grip_height);
    return MAX(grip_height, children_height);
}

static gboolean
hippo_canvas_grip_motion_notify_event (HippoCanvasItem *item,
                                       HippoEvent      *event)
{
    HippoCanvasGrip *grip = HIPPO_CANVAS_GRIP(item);
    gboolean result;
    gboolean prelighted;

    /* chain up so the box item can track child hovering */
    result = item_parent_class->motion_notify_event(item, event);

    /* but also update our prelight */
    if (event->u.motion.detail == HIPPO_MOTION_DETAIL_LEAVE)
        prelighted = FALSE;
    else
        prelighted = TRUE;

    if (grip->prelighted != prelighted) {
        grip->prelighted = prelighted;
        hippo_canvas_item_emit_paint_needed(item, 0, 0, -1, -1);
    }

    return result;
}
