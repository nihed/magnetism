/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include <cairo/cairo.h>
#include "hippo-canvas-image.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_image_init                (HippoCanvasImage       *image);
static void      hippo_canvas_image_class_init          (HippoCanvasImageClass  *klass);
static void      hippo_canvas_image_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_image_finalize            (GObject                *object);

static void hippo_canvas_image_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_image_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_image_paint              (HippoCanvasItem *item,
                                                       cairo_t         *cr);
static int      hippo_canvas_image_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_image_get_height_request (HippoCanvasItem *item,
                                                       int              for_width);

struct _HippoCanvasImage {
    HippoCanvasBox box;
    cairo_surface_t *surface;
};

struct _HippoCanvasImageClass {
    HippoCanvasBoxClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_IMAGE
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasImage, hippo_canvas_image, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_image_iface_init));

static void
hippo_canvas_image_init(HippoCanvasImage *image)
{

}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_image_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_image_paint;
    item_class->get_width_request = hippo_canvas_image_get_width_request;
    item_class->get_height_request = hippo_canvas_image_get_height_request;
}

static void
hippo_canvas_image_class_init(HippoCanvasImageClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_image_set_property;
    object_class->get_property = hippo_canvas_image_get_property;

    object_class->finalize = hippo_canvas_image_finalize;

    g_object_class_install_property(object_class,
                                    PROP_IMAGE,
                                    g_param_spec_pointer("image",
                                                         _("Image"),
                                                         _("Image as cairo_surface_t"),
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_image_finalize(GObject *object)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(object);

    if (image->surface)
        cairo_surface_destroy(image->surface);

    G_OBJECT_CLASS(hippo_canvas_image_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_image_new(void)
{
    HippoCanvasImage *image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE, NULL);


    return HIPPO_CANVAS_ITEM(image);
}

static void
hippo_canvas_image_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasImage *image;

    image = HIPPO_CANVAS_IMAGE(object);

    switch (prop_id) {
    case PROP_IMAGE:
        {
            cairo_surface_t *surface = g_value_get_pointer(value);
            if (surface != image->surface) {
#if 0
                /* The FC5 version of Cairo doesn't have this API */
                if (cairo_surface_get_type(surface) != CAIRO_SURFACE_TYPE_IMAGE) {
                    g_warning("Image canvas item only supports image surfaces");
                    return;
                }
#endif                
                cairo_surface_reference(surface);
                if (image->surface)
                    cairo_surface_destroy(image->surface);
                image->surface = surface;
                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(image));
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_image_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasImage *image;

    image = HIPPO_CANVAS_IMAGE (object);

    switch (prop_id) {
    case PROP_IMAGE:
        g_value_set_pointer(value, image->surface);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_image_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(item);
    int x, y, w, h;
    
    /* Draw the background and any children */
    item_parent_class->paint(item, cr);

    /* Draw the image */
    if (image->surface == NULL)
        return;
    
    x = 0;
    y = 0;
    w = cairo_image_surface_get_width(image->surface);
    h = cairo_image_surface_get_height(image->surface);

    hippo_canvas_box_align(HIPPO_CANVAS_BOX(item), &x, &y, &w, &h);
    
    cairo_set_source_surface(cr, image->surface, x, y);
    cairo_rectangle(cr, x, y, w, h);
    cairo_clip(cr);
    cairo_fill(cr);
}

static int
hippo_canvas_image_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_width;

    /* get width of children and the box padding */
    children_width = item_parent_class->get_width_request(item);

    if (hippo_canvas_box_get_fixed_width(HIPPO_CANVAS_BOX(item)) < 0) {
        int image_width = image->surface ? cairo_image_surface_get_width(image->surface) : 0;
        return MAX(image_width + box->padding_left + box->padding_right, children_width);
    } else {
        return children_width;
    }
}

static int
hippo_canvas_image_get_height_request(HippoCanvasItem *item,
                                      int              for_width)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_height;
    int image_height;

    /* get height of children and the box padding */
    children_height = item_parent_class->get_height_request(item, for_width);

    image_height = image->surface ? cairo_image_surface_get_height(image->surface) : 0;
    return MAX(image_height + box->padding_top + box->padding_bottom, children_height);
}
