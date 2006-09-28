/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-base.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-gradient.h>

static void      hippo_canvas_base_init                (HippoCanvasBase       *base);
static void      hippo_canvas_base_class_init          (HippoCanvasBaseClass  *klass);
static void      hippo_canvas_base_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_base_finalize            (GObject                *object);

static void hippo_canvas_base_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_base_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_base_paint              (HippoCanvasItem *item,
                                                      cairo_t         *cr,
                                                      HippoRectangle  *damaged_box);

struct _HippoCanvasBase {
    HippoCanvasBox box;

};

struct _HippoCanvasBaseClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBase, hippo_canvas_base, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_base_iface_init));

static void
hippo_canvas_base_init(HippoCanvasBase *base)
{
    HippoCanvasItem *item;
    HippoCanvasBox *box;
    
    /* Create top bar */
    
    box = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "image-name", "bar_middle", /* tile this as background */
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(base),
                            HIPPO_CANVAS_ITEM(box), 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "mugshotstacker",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "bar_x",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "bar_pipe",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "hush",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "bar_pipe",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "home",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "bar_pipe",
                        "xalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    /* Create "find" area */
    box = g_object_new(HIPPO_TYPE_CANVAS_GRADIENT,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "start-color", 0xb47accff,
                       "end-color", 0xa35abfff,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(base),
                            HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "find",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "search_x",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);    
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_base_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_base_paint;
}

static void
hippo_canvas_base_class_init(HippoCanvasBaseClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_base_set_property;
    object_class->get_property = hippo_canvas_base_get_property;

    object_class->finalize = hippo_canvas_base_finalize;
}

static void
hippo_canvas_base_finalize(GObject *object)
{
    /* HippoCanvasBase *base = HIPPO_CANVAS_BASE(object); */


    G_OBJECT_CLASS(hippo_canvas_base_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_base_new(void)
{
    HippoCanvasBase *base = g_object_new(HIPPO_TYPE_CANVAS_BASE, NULL);


    return HIPPO_CANVAS_ITEM(base);
}

static void
hippo_canvas_base_set_property(GObject         *object,
                               guint            prop_id,
                               const GValue    *value,
                               GParamSpec      *pspec)
{
    HippoCanvasBase *base;

    base = HIPPO_CANVAS_BASE(object);

    switch (prop_id) {

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_base_get_property(GObject         *object,
                               guint            prop_id,
                               GValue          *value,
                               GParamSpec      *pspec)
{
    HippoCanvasBase *base;

    base = HIPPO_CANVAS_BASE (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_base_paint(HippoCanvasItem *item,
                        cairo_t         *cr,
                        HippoRectangle  *damaged_box)
{
    /* HippoCanvasBase *base = HIPPO_CANVAS_BASE(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr, damaged_box);
}
