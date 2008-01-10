/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-url-image.h"

static void      hippo_canvas_url_image_init                (HippoCanvasUrlImage       *image);
static void      hippo_canvas_url_image_class_init          (HippoCanvasUrlImageClass  *klass);
static void      hippo_canvas_url_image_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_url_image_finalize            (GObject                *object);

static void hippo_canvas_url_image_set_property (GObject      *object,
                                                 guint         prop_id,
                                                 const GValue *value,
                                                 GParamSpec   *pspec);
static void hippo_canvas_url_image_get_property (GObject      *object,
                                                 guint         prop_id,
                                                 GValue       *value,
                                                 GParamSpec   *pspec);

/* Canvas item methods */
static void  hippo_canvas_url_image_activated   (HippoCanvasItem *item);
static char *hippo_canvas_url_image_get_tooltip (HippoCanvasItem *item,
                                                 int              x,
                                                 int              y,
                                                 HippoRectangle  *for_area);

/* Our own methods */
static void hippo_canvas_url_image_set_actions (HippoCanvasUrlImage *image,
                                                HippoActions       *actions);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_ACTIONS,
    PROP_URL
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasUrlImage, hippo_canvas_url_image, HIPPO_TYPE_CANVAS_IMAGE,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_url_image_iface_init));

static void
hippo_canvas_url_image_init(HippoCanvasUrlImage *image)
{
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_url_image_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
    
    item_class->activated = hippo_canvas_url_image_activated;
    item_class->get_tooltip = hippo_canvas_url_image_get_tooltip;
}

static void
hippo_canvas_url_image_class_init(HippoCanvasUrlImageClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    box_class->default_color = 0x0033ffff;
    
    object_class->set_property = hippo_canvas_url_image_set_property;
    object_class->get_property = hippo_canvas_url_image_get_property;

    object_class->finalize = hippo_canvas_url_image_finalize;
    
    g_object_class_install_property(object_class,
                                    PROP_URL,
                                    g_param_spec_string("url",
                                                        _("URL"),
                                                        _("URL that the image points to"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
}

static void
hippo_canvas_url_image_finalize(GObject *object)
{
    HippoCanvasUrlImage *image = HIPPO_CANVAS_URL_IMAGE(object);

    hippo_canvas_url_image_set_actions(image, NULL);
    g_free(image->url);

    G_OBJECT_CLASS(hippo_canvas_url_image_parent_class)->finalize(object);
}

HippoCanvasItem *
hippo_canvas_url_image_new(void)
{
    HippoCanvasUrlImage *image = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE, NULL);

    return HIPPO_CANVAS_ITEM(image);
}

static void
hippo_canvas_url_image_set_url(HippoCanvasUrlImage *image,
                               const char         *url)
{
    if (url == image->url)
        return;
    
    if (image->url)
        g_free(image->url);
    
    image->url = g_strdup(url);

    HIPPO_CANVAS_BOX(image)->clickable = image->url != NULL;
}

static void
hippo_canvas_url_image_set_actions(HippoCanvasUrlImage *image,
                                   HippoActions       *actions)
{
    if (actions == image->actions)
        return;

    if (image->actions) {
        g_object_unref(image->actions);
        image->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        image->actions = actions;
    }

    g_object_notify(G_OBJECT(image), "actions");
}

static void
hippo_canvas_url_image_set_property(GObject         *object,
                                    guint            prop_id,
                                    const GValue    *value,
                                    GParamSpec      *pspec)
{
    HippoCanvasUrlImage *image;

    image = HIPPO_CANVAS_URL_IMAGE(object);

    switch (prop_id) {
    case PROP_URL:
        hippo_canvas_url_image_set_url(image, g_value_get_string(value));
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_url_image_set_actions(image, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_url_image_get_property(GObject         *object,
                                    guint            prop_id,
                                    GValue          *value,
                                    GParamSpec      *pspec)
{
    HippoCanvasUrlImage *image;

    image = HIPPO_CANVAS_URL_IMAGE (object);
    
    switch (prop_id) {
    case PROP_URL:
        g_value_set_string(value, image->url);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) image->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_url_image_activated(HippoCanvasItem *item)
{
    HippoCanvasUrlImage *image = HIPPO_CANVAS_URL_IMAGE(item);
    
    if (image->actions && image->url)
        hippo_actions_open_url(image->actions, image->url);
}

static char *
hippo_canvas_url_image_get_tooltip (HippoCanvasItem *item,
                                    int              x,
                                    int              y,
                                    HippoRectangle  *for_area)
{
    HippoCanvasUrlImage *image = HIPPO_CANVAS_URL_IMAGE(item);

    char *tooltip = item_parent_class->get_tooltip(item, x, y, for_area);
    if (tooltip) {
        return tooltip;
    } else if (image->url) {
        for_area->x = 0;
        for_area->y = 0;
        for_area->width = HIPPO_CANVAS_BOX(item)->allocated_width;
        for_area->height = HIPPO_CANVAS_BOX(item)->allocated_height;
        return g_strdup(image->url);
    } else {
        return NULL;
    }
}
