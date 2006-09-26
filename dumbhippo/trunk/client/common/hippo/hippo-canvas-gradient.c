/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-gradient.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_gradient_init                (HippoCanvasGradient       *gradient);
static void      hippo_canvas_gradient_class_init          (HippoCanvasGradientClass  *klass);
static void      hippo_canvas_gradient_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_gradient_finalize            (GObject                *object);

static void hippo_canvas_gradient_set_property (GObject      *object,
                                                guint         prop_id,
                                                const GValue *value,
                                                GParamSpec   *pspec);
static void hippo_canvas_gradient_get_property (GObject      *object,
                                                guint         prop_id,
                                                GValue       *value,
                                                GParamSpec   *pspec);


/* Canvas box methods */
static void hippo_canvas_gradient_paint_below_children (HippoCanvasBox *box,
                                                        cairo_t        *cr,
                                                        HippoRectangle *damaged_box);

struct _HippoCanvasGradient {
    HippoCanvasBox box;
    guint32 start_color;
    guint32 end_color;
};

struct _HippoCanvasGradientClass {
    HippoCanvasBoxClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_START_COLOR,
    PROP_END_COLOR
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasGradient, hippo_canvas_gradient, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_gradient_iface_init));

static void
hippo_canvas_gradient_init(HippoCanvasGradient *gradient)
{

}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_gradient_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_gradient_class_init(HippoCanvasGradientClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);
    
    object_class->set_property = hippo_canvas_gradient_set_property;
    object_class->get_property = hippo_canvas_gradient_get_property;

    object_class->finalize = hippo_canvas_gradient_finalize;

    box_class->paint_below_children = hippo_canvas_gradient_paint_below_children;
    
    g_object_class_install_property(object_class,
                                    PROP_START_COLOR,
                                    g_param_spec_uint("start-color",
                                                     _("Start color"),
                                                     _("First color in the gradient"),
                                                      0,
                                                      G_MAXUINT,
                                                      0,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_END_COLOR,
                                    g_param_spec_uint("end-color",
                                                     _("End color"),
                                                     _("Last color in the gradient"),
                                                      0,
                                                      G_MAXUINT,
                                                      0,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_gradient_finalize(GObject *object)
{
    /* HippoCanvasGradient *gradient = HIPPO_CANVAS_GRADIENT(object); */

    G_OBJECT_CLASS(hippo_canvas_gradient_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_gradient_new(void)
{
    HippoCanvasGradient *gradient = g_object_new(HIPPO_TYPE_CANVAS_GRADIENT, NULL);


    return HIPPO_CANVAS_ITEM(gradient);
}

static void
hippo_canvas_gradient_set_property(GObject         *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasGradient *gradient;

    gradient = HIPPO_CANVAS_GRADIENT(object);

    switch (prop_id) {
    case PROP_START_COLOR:
        {
            guint32 color = g_value_get_uint(value);
            if (color != gradient->start_color) {
                gradient->start_color = color;
                hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(gradient),
                                                    0, 0, -1, -1);
            }
        }
        break;
    case PROP_END_COLOR:
        {
            guint32 color = g_value_get_uint(value);
            if (color != gradient->end_color) {
                gradient->end_color = color;
                hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(gradient),
                                                    0, 0, -1, -1);
            }
        }
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_gradient_get_property(GObject         *object,
                                   guint            prop_id,
                                   GValue          *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasGradient *gradient;

    gradient = HIPPO_CANVAS_GRADIENT (object);

    switch (prop_id) {
    case PROP_START_COLOR:
        g_value_set_uint(value, gradient->start_color);
        break;
    case PROP_END_COLOR:
        g_value_set_uint(value, gradient->end_color);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_gradient_paint_below_children(HippoCanvasBox  *box,
                                           cairo_t         *cr,
                                           HippoRectangle  *damaged_box)
{
    HippoCanvasGradient *gradient = HIPPO_CANVAS_GRADIENT(box);
    cairo_pattern_t *pattern;
    HippoRectangle area;

    hippo_canvas_box_get_background_area(box, &area);

    /* just hardcoded vertical for now */
    pattern = cairo_pattern_create_linear(area.x,
                                          area.y,
                                          area.x,
                                          area.y + area.height);
    
    hippo_cairo_pattern_add_stop_rgba32(pattern, 0.0, gradient->start_color);
    hippo_cairo_pattern_add_stop_rgba32(pattern, 1.0, gradient->end_color);

    cairo_set_source(cr, pattern);
    
    cairo_rectangle(cr, area.x, area.y, area.width, area.height);
    cairo_fill(cr);

    cairo_pattern_destroy(pattern);
}
