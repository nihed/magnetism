/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include <string.h>
#include "hippo-canvas-image-button.h"
#include "hippo-canvas-util.h"

static void      hippo_canvas_image_button_init                (HippoCanvasImageButton       *image);
static void      hippo_canvas_image_button_class_init          (HippoCanvasImageButtonClass  *klass);
static void      hippo_canvas_image_button_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_image_button_finalize            (GObject                *object);

static void hippo_canvas_image_button_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_canvas_image_button_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);

/* Box methods */
static void hippo_canvas_image_button_hovering_changed (HippoCanvasBox  *box,
                                                        gboolean         hovering);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_NORMAL_IMAGE,
    PROP_NORMAL_IMAGE_NAME,
    PROP_PRELIGHT_IMAGE,
    PROP_PRELIGHT_IMAGE_NAME
};

struct _HippoCanvasImageButton {
    HippoCanvasImage image;
    cairo_surface_t *normal_image;
    char *normal_image_name;
    cairo_surface_t *prelight_image;
    char *prelight_image_name;
};

struct _HippoCanvasImageButtonClass {
    HippoCanvasImageClass parent_class;
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasImageButton, hippo_canvas_image_button, HIPPO_TYPE_CANVAS_IMAGE,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_image_button_iface_init));

static void
hippo_canvas_image_button_init(HippoCanvasImageButton *button)
{
    HIPPO_CANVAS_BOX(button)->clickable = TRUE;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_image_button_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_image_button_class_init(HippoCanvasImageButtonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);
    
    object_class->set_property = hippo_canvas_image_button_set_property;
    object_class->get_property = hippo_canvas_image_button_get_property;

    object_class->finalize = hippo_canvas_image_button_finalize;

    box_class->hovering_changed = hippo_canvas_image_button_hovering_changed;
    
    g_object_class_install_property(object_class,
                                    PROP_NORMAL_IMAGE,
                                    g_param_spec_boxed("normal-image",
                                                         _("Normal Image"),
                                                         _("normal image as cairo_surface_t"),
                                                         HIPPO_TYPE_CAIRO_SURFACE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_NORMAL_IMAGE_NAME,
                                    g_param_spec_string("normal-image-name",
                                                        _("Normal Image Name"),
                                                        _("Name of normal image to be loaded into the item"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_PRELIGHT_IMAGE,
                                    g_param_spec_boxed("prelight-image",
                                                         _("Prelight Image"),
                                                         _("prelight image as cairo_surface_t"),
                                                         HIPPO_TYPE_CAIRO_SURFACE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_PRELIGHT_IMAGE_NAME,
                                    g_param_spec_string("prelight-image-name",
                                                        _("Prelight Image Name"),
                                                        _("Name of prelight image to be loaded into the item"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));    
}

static void
hippo_canvas_image_button_finalize(GObject *object)
{
    HippoCanvasImageButton *button = HIPPO_CANVAS_IMAGE_BUTTON(object);

    if (button->normal_image)
        cairo_surface_destroy(button->normal_image);

    g_free(button->normal_image_name);
    button->normal_image_name = NULL;

    if (button->prelight_image)
        cairo_surface_destroy(button->prelight_image);

    g_free(button->prelight_image_name);
    button->prelight_image_name = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_image_button_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_image_button_new(void)
{
    HippoCanvasImageButton *image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON, NULL);


    return HIPPO_CANVAS_ITEM(image);
}

static void
pick_image(HippoCanvasImageButton *button)
{
    HippoCanvasBox *box;
    const char *name;
    cairo_surface_t *surface;

    box = HIPPO_CANVAS_BOX(button);

    name = NULL;
    surface = NULL;
    
    if (box->hovering) {
        if (button->prelight_image_name)
            name = button->prelight_image_name;
        else if (button->prelight_image)
            surface = button->prelight_image;
    }

    /* it's allowed to have no prelight image, in which case we just use the
     * normal image
     */
    if (!box->hovering || (name == NULL && surface == NULL)) {
        if (button->normal_image_name)
            name = button->normal_image_name;
        else
            surface = button->normal_image;
    }

    /* This causes a request_changed or repaint if necessary */
    if (name)
        g_object_set(G_OBJECT(button), "image-name", name, NULL);
    else
        g_object_set(G_OBJECT(button), "image", surface, NULL); /* note, surface may be null */
}

static void
hippo_canvas_image_button_set_property(GObject         *object,
                                       guint            prop_id,
                                       const GValue    *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasImageButton *button;

    button = HIPPO_CANVAS_IMAGE_BUTTON(object);

    switch (prop_id) {
    case PROP_NORMAL_IMAGE:
        {
            cairo_surface_t *surface = g_value_get_boxed(value);
            if (surface != button->normal_image) {
                if (surface)
                    cairo_surface_reference(surface);
                if (button->normal_image)
                    cairo_surface_destroy(button->normal_image);
                button->normal_image = surface;
            }
        }
        break;
    case PROP_NORMAL_IMAGE_NAME:
        {
            const char *name = g_value_get_string(value);
            
            if (!(button->normal_image_name == name ||
                  (button->normal_image_name && name && strcmp(button->normal_image_name,
                                                       name) == 0))) {
                g_free(button->normal_image_name);
                button->normal_image_name = g_strdup(name);
            }
        }
        break;
    case PROP_PRELIGHT_IMAGE:
        {
            cairo_surface_t *surface = g_value_get_boxed(value);
            if (surface != button->prelight_image) {
                if (surface)
                    cairo_surface_reference(surface);
                if (button->prelight_image)
                    cairo_surface_destroy(button->prelight_image);
                button->prelight_image = surface;
            }
        }
        break;
    case PROP_PRELIGHT_IMAGE_NAME:
        {
            const char *name = g_value_get_string(value);
            
            if (!(button->prelight_image_name == name ||
                  (button->prelight_image_name && name && strcmp(button->prelight_image_name,
                                                       name) == 0))) {
                g_free(button->prelight_image_name);
                button->prelight_image_name = g_strdup(name);
            }
        }
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }

    /* This results in an emit_request_changed or repaint as appropriate */
    pick_image(button);
}

static void
hippo_canvas_image_button_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoCanvasImageButton *button;
    
    button = HIPPO_CANVAS_IMAGE_BUTTON (object);

    switch (prop_id) {
    case PROP_NORMAL_IMAGE:
        g_value_set_boxed(value, button->normal_image);
        break;
    case PROP_NORMAL_IMAGE_NAME:
        g_value_set_string(value, button->normal_image_name);
        break;
    case PROP_PRELIGHT_IMAGE:
        g_value_set_boxed(value, button->prelight_image);
        break;
    case PROP_PRELIGHT_IMAGE_NAME:
        g_value_set_string(value, button->prelight_image_name);
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_image_button_hovering_changed (HippoCanvasBox  *box,
                                            gboolean         hovering)
{
    HippoCanvasImageButton *button;

    button = HIPPO_CANVAS_IMAGE_BUTTON(box);
    
    pick_image(button);
}
