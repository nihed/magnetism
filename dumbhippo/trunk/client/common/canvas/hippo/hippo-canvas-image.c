/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-util.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-image.h"

static void      hippo_canvas_image_init                (HippoCanvasImage       *image);
static void      hippo_canvas_image_class_init          (HippoCanvasImageClass  *klass);
static void      hippo_canvas_image_iface_init          (HippoCanvasItemIface   *item_class);
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
static void     hippo_canvas_image_set_context        (HippoCanvasItem    *item,
                                                       HippoCanvasContext *context);

/* Canvas box methods */
static void hippo_canvas_image_paint_below_children       (HippoCanvasBox *box,
                                                           cairo_t        *cr,
                                                           HippoRectangle  *damaged_box);
static void hippo_canvas_image_get_content_width_request  (HippoCanvasBox *box,
                                                           int            *min_width_p,
                                                           int            *natural_width_p);
static void hippo_canvas_image_get_content_height_request (HippoCanvasBox *box,
                                                           int             for_width,
                                                           int            *min_height_p,
                                                           int            *natural_height_p);


enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_IMAGE,
    PROP_IMAGE_NAME,
    PROP_SCALE_WIDTH,
    PROP_SCALE_HEIGHT
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasImage, hippo_canvas_image, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_image_iface_init));

static void
hippo_canvas_image_init(HippoCanvasImage *image)
{
    image->scale_width = -1;
    image->scale_height = -1;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_image_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->set_context = hippo_canvas_image_set_context;
}

static void
hippo_canvas_image_class_init(HippoCanvasImageClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);
    
    object_class->set_property = hippo_canvas_image_set_property;
    object_class->get_property = hippo_canvas_image_get_property;

    object_class->finalize = hippo_canvas_image_finalize;

    box_class->paint_below_children = hippo_canvas_image_paint_below_children;
    box_class->get_content_width_request = hippo_canvas_image_get_content_width_request;
    box_class->get_content_height_request = hippo_canvas_image_get_content_height_request;

    /**
     * HippoCanvasImage:image
     *
     * Specifies the Cairo surface to display.
     */
    g_object_class_install_property(object_class,
                                    PROP_IMAGE,
                                    g_param_spec_boxed("image",
                                                         _("Image"),
                                                         _("Image as cairo_surface_t"),
                                                         HIPPO_TYPE_CAIRO_SURFACE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasImage:image-name
     *
     * Specifies an image name to display; with GTK+ the image name is
     * an image name registered with the GTK+ theme system.
     */    
    g_object_class_install_property(object_class,
                                    PROP_IMAGE_NAME,
                                    g_param_spec_string("image-name",
                                                        _("Image Name"),
                                                        _("Image name to be loaded into the item"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasImage:scale-width
     *
     * A width in pixels to scale the image to, or -1 to use the image's original width.
     */
    g_object_class_install_property(object_class,
                                    PROP_SCALE_WIDTH,
                                    g_param_spec_int("scale-width",
                                                     _("Scale width"),
                                                     _("Width to scale to or -1 for no scale"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasImage:scale-height
     *
     * A height in pixels to scale the image to, or -1 to use the image's original height.
     */    
    g_object_class_install_property(object_class,
                                    PROP_SCALE_HEIGHT,
                                    g_param_spec_int("scale-height",
                                                     _("Scale height"),
                                                     _("Height to scale to or -1 for no scale"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));    
}

static void
hippo_canvas_image_finalize(GObject *object)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(object);

    if (image->surface)
        cairo_surface_destroy(image->surface);

    g_free(image->image_name);
    image->image_name = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_image_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_image_new(void)
{
    HippoCanvasImage *image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE, NULL);


    return HIPPO_CANVAS_ITEM(image);
}

static void
set_surface(HippoCanvasImage *image,
            cairo_surface_t  *surface)
{
    if (surface != image->surface) {
        int old_width, old_height;
        gboolean request_changed = FALSE;
        
#if 0
        /* The FC5 version of Cairo doesn't have this API */
        if (cairo_surface_get_type(surface) != CAIRO_SURFACE_TYPE_IMAGE) {
            g_warning("Image canvas item only supports image surfaces");
            return;
        }
#endif
        old_width = image->surface ? cairo_image_surface_get_width(image->surface) : 0;
        old_height = image->surface ? cairo_image_surface_get_height(image->surface) : 0;
        
        if (surface)
            cairo_surface_reference(surface);
        if (image->surface)
            cairo_surface_destroy(image->surface);
        image->surface = surface;

        if (image->scale_width < 0) {
            int width = image->surface ? cairo_image_surface_get_width(image->surface) : 0;
            if (width != old_width)
                request_changed = TRUE;
        }
        if (image->scale_height < 0) {
            int height = image->surface ? cairo_image_surface_get_height(image->surface) : 0;
            if (height != old_height)
                request_changed = TRUE;
        }

        if (request_changed)
            hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(image));
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(image), 0, 0, -1, -1);

        g_object_notify(G_OBJECT(image), "image");
    }
}

static void
set_surface_from_name(HippoCanvasImage *image)
{
    if (image->image_name == NULL) {
        set_surface(image, NULL);
    } else {
        cairo_surface_t *surface;
        HippoCanvasContext *context;

        context = hippo_canvas_box_get_context(HIPPO_CANVAS_BOX(image));

        /* If context is NULL, we'll call set_surface_from_name again
         * when a new context is set
         */
        if (context != NULL) {
            /* may return NULL */
            surface = hippo_canvas_context_load_image(context,
                                                      image->image_name);
            set_surface(image, surface);
            
            if (surface != NULL)
                cairo_surface_destroy(surface);
        } else {
            set_surface(image, NULL);
        }
    }
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
            cairo_surface_t *surface = g_value_get_boxed(value);

            if (image->image_name) {
                g_free(image->image_name);
                image->image_name = NULL;
                g_object_notify(G_OBJECT(image), "image-name");
            }
            
            set_surface(image, surface);
        }
        break;
    case PROP_IMAGE_NAME:
        {
            const char *name = g_value_get_string(value);

            if (!(image->image_name == name ||
                  (image->image_name && name && strcmp(image->image_name,
                                                       name) == 0))) {
                g_free(image->image_name);
                image->image_name = g_strdup(name);
                set_surface_from_name(image);

                /* will recursively call set_property("image") which
                 * will result in a request_changed if required
                 */
            }
        }
        break;
    case PROP_SCALE_WIDTH:
        {
            int w = g_value_get_int(value);
            if (w != image->scale_width) {
                image->scale_width = w;
                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(image));
            } 
        }
        break;
    case PROP_SCALE_HEIGHT:
        {
            int h = g_value_get_int(value);
            if (h != image->scale_height) {
                image->scale_height = h;
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
        g_value_set_boxed(value, image->surface);
        break;
    case PROP_IMAGE_NAME:
        g_value_set_string(value, image->image_name);
        break;
    case PROP_SCALE_WIDTH:
        g_value_set_int(value, image->scale_width);
        break;
    case PROP_SCALE_HEIGHT:
        g_value_set_int(value, image->scale_height);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_image_set_context(HippoCanvasItem    *item,
                               HippoCanvasContext *context)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    HippoCanvasContext *old = box->context;

    item_parent_class->set_context(item, context);

    if (old != box->context && image->image_name != NULL)
        set_surface_from_name(image);
}

static void
hippo_canvas_image_paint_below_children(HippoCanvasBox  *box,
                                        cairo_t         *cr,
                                        HippoRectangle  *damaged_box)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(box);
    int x, y, w, h;
    int image_width, image_height;
    double xscale;
    double yscale;

    if (image->surface == NULL)
        return;

    image_width = cairo_image_surface_get_width(image->surface);
    image_height = cairo_image_surface_get_height(image->surface);

    /* Immediately short-circuit 0-sized image or scaled image,
     * which saves special cases in the math later on, in addition
     * to saving work
     */
    if (image_width == 0 || image_height == 0 ||
        image->scale_width == 0 || image->scale_height == 0)
        return;

    if (image->scale_width >= 0) {
        w = image->scale_width;
        xscale = image->scale_width / (double) image_width;
    } else {
        w = image_width;
        xscale = 1.0;
    }
    if (image->scale_height >= 0) {
        h = image->scale_height;
        yscale = image->scale_height / (double) image_height;
    } else {
        h = image_height;
        yscale = 1.0;
    }

    /* note that if an alignment is FILL the w/h will be increased
     * beyond the image's natural size, which will result in
     * a tiled image
     */
    
    hippo_canvas_box_align(box, w, h, &x, &y, &w, &h);

    cairo_rectangle(cr, x, y, w, h);
    cairo_clip(cr);

    cairo_scale (cr, xscale, yscale);
    cairo_translate (cr, x, y);
    cairo_set_source_surface(cr, image->surface, 0, 0);
    cairo_pattern_set_extend(cairo_get_source(cr), CAIRO_EXTEND_REPEAT);
    cairo_paint(cr);

    /* g_debug("cairo status %s", cairo_status_to_string(cairo_status(cr))); */
}

static void
hippo_canvas_image_get_content_width_request(HippoCanvasBox *box,
                                             int            *min_width_p,
                                             int            *natural_width_p)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(box);
    int children_min_width, children_natural_width;
    int image_width;

    HIPPO_CANVAS_BOX_CLASS(hippo_canvas_image_parent_class)->get_content_width_request(box,
                                                                                       &children_min_width,
                                                                                       &children_natural_width);

    if (image->scale_width >= 0)
        image_width = image->scale_width;
    else
        image_width = image->surface ? cairo_image_surface_get_width(image->surface) : 0;

    if (min_width_p)
        *min_width_p = MAX(image_width, children_min_width);
    if (natural_width_p)
        *natural_width_p = MAX(image_width, children_natural_width);
}

static void
hippo_canvas_image_get_content_height_request(HippoCanvasBox  *box, 
                                              int              for_width,
                                              int             *min_height_p,
                                              int             *natural_height_p)
{
    HippoCanvasImage *image = HIPPO_CANVAS_IMAGE(box);
    int children_min_height, children_natural_height;
    int image_height;

    /* get height of children and the box padding */
    HIPPO_CANVAS_BOX_CLASS(hippo_canvas_image_parent_class)->get_content_height_request(box,
                                                                                        for_width,
                                                                                        &children_min_height,
                                                                                        &children_natural_height);

    if (image->scale_height >= 0)
        image_height = image->scale_height;
    else
        image_height = image->surface ? cairo_image_surface_get_height(image->surface) : 0;

    if (min_height_p)
        *min_height_p = MAX(image_height, children_min_height);
    if (natural_height_p)
        *natural_height_p = MAX(image_height, children_natural_height);
}
