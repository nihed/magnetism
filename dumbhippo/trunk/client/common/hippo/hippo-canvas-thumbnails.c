/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-thumbnails.h"
#include "hippo-thumbnails.h"
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-url-link.h>

static void      hippo_canvas_thumbnails_init                (HippoCanvasThumbnails       *thumbnails);
static void      hippo_canvas_thumbnails_class_init          (HippoCanvasThumbnailsClass  *klass);
static void      hippo_canvas_thumbnails_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_thumbnails_dispose             (GObject                *object);
static void      hippo_canvas_thumbnails_finalize            (GObject                *object);

static void hippo_canvas_thumbnails_set_property (GObject      *object,
                                                  guint         prop_id,
                                                  const GValue *value,
                                                  GParamSpec   *pspec);
static void hippo_canvas_thumbnails_get_property (GObject      *object,
                                                  guint         prop_id,
                                                  GValue       *value,
                                                  GParamSpec   *pspec);

static void set_thumbnails (HippoCanvasThumbnails *canvas_thumbnails,
                            HippoThumbnails       *thumbnails);
static void set_actions    (HippoCanvasThumbnails *canvas_thumbnails,
                            HippoActions          *actions);
static void set_display_count (HippoCanvasThumbnails *canvas_thumbnails,
                               int                    count);                            


struct _HippoCanvasThumbnails {
    HippoCanvasBox parent;

    HippoActions *actions;
    HippoThumbnails *thumbnails;
    int display_count;
};

struct _HippoCanvasThumbnailsClass {
    HippoCanvasBoxClass parent_class;
};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_THUMBNAILS,
    PROP_ACTIONS,
    PROP_DISPLAY_COUNT
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasThumbnails, hippo_canvas_thumbnails, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_thumbnails_iface_init));

static void
hippo_canvas_thumbnails_init(HippoCanvasThumbnails *thumbnails)
{
        thumbnails->display_count = 2; /* default */
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_thumbnails_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    /* item_class->motion_notify_event = hippo_canvas_thumbnails_motion_notify_event; */
}

static void
hippo_canvas_thumbnails_class_init(HippoCanvasThumbnailsClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    /* HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass); */

    object_class->set_property = hippo_canvas_thumbnails_set_property;
    object_class->get_property = hippo_canvas_thumbnails_get_property;

    object_class->dispose = hippo_canvas_thumbnails_dispose;
    object_class->finalize = hippo_canvas_thumbnails_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));
    
    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The thumbnails to display"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
                                                        
    g_object_class_install_property(object_class,
                                    PROP_DISPLAY_COUNT,
                                    g_param_spec_int("display-count",
                                                     _("Display Counter"),
                                                     _("The maxiumum thumbnails to display"),
                                                      0, G_MAXINT, 2,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));                                                        
}

static void
hippo_canvas_thumbnails_dispose(GObject *object)
{
    HippoCanvasThumbnails *thumbnails = HIPPO_CANVAS_THUMBNAILS(object);

    set_actions(thumbnails, NULL);
    set_thumbnails(thumbnails, NULL);

    G_OBJECT_CLASS(hippo_canvas_thumbnails_parent_class)->dispose(object);
}

static void
hippo_canvas_thumbnails_finalize(GObject *object)
{
    /* HippoCanvasThumbnails *thumbnails = HIPPO_CANVAS_THUMBNAILS(object); */

    G_OBJECT_CLASS(hippo_canvas_thumbnails_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_thumbnails_new(void)
{
    HippoCanvasThumbnails *thumbnails = g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS, NULL);


    return HIPPO_CANVAS_ITEM(thumbnails);
}

static void
hippo_canvas_thumbnails_set_property(GObject         *object,
                                     guint            prop_id,
                                     const GValue    *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasThumbnails *thumbnails;

    thumbnails = HIPPO_CANVAS_THUMBNAILS(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        {
            HippoThumbnails *new_thumbs = (HippoThumbnails*) g_value_get_object(value);
            set_thumbnails(thumbnails, new_thumbs);
        }
        break;
    case PROP_ACTIONS:
        {
            HippoActions *actions = (HippoActions*) g_value_get_object(value);
            set_actions(thumbnails, actions);
        }
        break;
    case PROP_DISPLAY_COUNT:
        {
                set_display_count(thumbnails, g_value_get_int(value));
        }
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_thumbnails_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasThumbnails *thumbnails;

    thumbnails = HIPPO_CANVAS_THUMBNAILS (object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        g_value_set_object(value, thumbnails->thumbnails);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, thumbnails->actions);
        break;
    case PROP_DISPLAY_COUNT:
        g_value_set_int(value, thumbnails->display_count);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_thumbnails_create_children(HippoCanvasThumbnails *canvas_thumbnails)
{
    HippoCanvasBox *box;
    int i;
    HippoThumbnails *thumbnails;
    HippoCanvasBox *thumbs_box;
    HippoCanvasItem *more_link;
    
    box = HIPPO_CANVAS_BOX(canvas_thumbnails);

    hippo_canvas_box_remove_all(box);

    thumbnails = canvas_thumbnails->thumbnails;

    if (thumbnails == NULL)
        return;
    
    thumbs_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                              "spacing", 8,
                              "border", 8,
                              NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(thumbs_box), 0);
    
    for (i = 0; i < hippo_thumbnails_get_count(thumbnails) && i < canvas_thumbnails->display_count; ++i) {
        HippoThumbnail *thumb;
        HippoCanvasBox *thumb_box;
        HippoCanvasItem *image;
        HippoCanvasItem *caption;
        
        thumb = hippo_thumbnails_get_nth(thumbnails, i);
        
        thumb_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "spacing", 4,
                                 "xalign", HIPPO_ALIGNMENT_START,
                                 NULL);

        /* FIXME hook up clicking on the image */
        image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                             /* "tooltip", hippo_thumbnail_get_title(thumb), */
                             NULL);
        hippo_actions_load_thumbnail_async(canvas_thumbnails->actions,
                                           hippo_thumbnail_get_src(thumb),
                                           image);
        hippo_canvas_box_append(thumb_box, image, 0);

        caption = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                               "actions", canvas_thumbnails->actions,
                               "url", hippo_thumbnail_get_href(thumb),
                               "text", hippo_thumbnail_get_title(thumb),
                               "xalign", HIPPO_ALIGNMENT_CENTER,
                               "yalign", HIPPO_ALIGNMENT_END,
                               "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                               NULL);
        hippo_canvas_box_append(thumb_box, caption, 0);

        hippo_canvas_box_append(thumbs_box,
                                HIPPO_CANVAS_ITEM(thumb_box),
                                0);
    }

    more_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                             "actions", canvas_thumbnails->actions,
                             "url", hippo_thumbnails_get_more_link(thumbnails),
                             "text", "More...",
                             "tooltip", hippo_thumbnails_get_more_title(thumbnails),
                             "xalign", HIPPO_ALIGNMENT_START,
                             "yalign", HIPPO_ALIGNMENT_END,
                             NULL);
    hippo_canvas_box_append(box, more_link, 0);
}

static void
set_thumbnails(HippoCanvasThumbnails *canvas_thumbnails,
               HippoThumbnails       *thumbnails)
{
    if (canvas_thumbnails->thumbnails == thumbnails)
        return;

    if (canvas_thumbnails->thumbnails) {
        g_object_unref(canvas_thumbnails->thumbnails);

        canvas_thumbnails->thumbnails = NULL;
    }
    
    if (thumbnails) {
        g_object_ref(thumbnails);
        canvas_thumbnails->thumbnails = thumbnails;
    }

    hippo_canvas_thumbnails_create_children(canvas_thumbnails);
    
    g_object_notify(G_OBJECT(canvas_thumbnails), "thumbnails");
}

static void
set_actions(HippoCanvasThumbnails *canvas_thumbnails,
            HippoActions          *actions)
{
    if (actions == canvas_thumbnails->actions)
        return;

    if (canvas_thumbnails->actions) {
        g_object_unref(canvas_thumbnails->actions);
        canvas_thumbnails->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        canvas_thumbnails->actions = actions;
    }

    g_object_notify(G_OBJECT(canvas_thumbnails), "actions");
}

static void
set_display_count(HippoCanvasThumbnails *canvas_thumbnails,
                                  int                    count)
{
        canvas_thumbnails->display_count = count;
        g_object_notify(G_OBJECT(canvas_thumbnails), "display-count");
}
