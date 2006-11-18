/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-thumbnails.h"
#include "hippo-thumbnails.h"
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>

static void      hippo_canvas_thumbnails_init                (HippoCanvasThumbnails       *thumbnails);
static void      hippo_canvas_thumbnails_class_init          (HippoCanvasThumbnailsClass  *klass);
static void      hippo_canvas_thumbnails_iface_init          (HippoCanvasItemIface   *item_class);
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

struct _HippoCanvasThumbnails {
    HippoCanvasBox parent;

    HippoThumbnails *thumbnails;
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
    PROP_THUMBNAILS
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasThumbnails, hippo_canvas_thumbnails, HIPPO_TYPE_CANVAS_ITEM,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_thumbnails_iface_init));

static void
hippo_canvas_thumbnails_init(HippoCanvasThumbnails *thumbnails)
{

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

    object_class->finalize = hippo_canvas_thumbnails_finalize;

    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The thumbnails to display"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_thumbnails_finalize(GObject *object)
{
    HippoCanvasThumbnails *thumbnails = HIPPO_CANVAS_THUMBNAILS(object);

    set_thumbnails(thumbnails, NULL);

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
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_thumbnails (HippoCanvasThumbnails *canvas_thumbnails,
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
    
    g_object_notify(G_OBJECT(canvas_thumbnails), "thumbnails");
}

