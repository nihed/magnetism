/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-thumbnails.h"
#include "hippo-canvas-thumbnail.h"
#include "hippo-canvas-resource.h"
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-url-image.h>
#include <hippo/hippo-canvas-url-link.h>

static void      hippo_canvas_thumbnails_dispose             (GObject                *object);
static void      hippo_canvas_thumbnails_finalize            (GObject                *object);

static void hippo_canvas_thumbnails_create_children (HippoCanvasResource *canvas_resource);
static void hippo_canvas_thumbnails_update          (HippoCanvasResource *canvas_resource);

static void hippo_canvas_thumbnails_get_property(GObject         *object,
                                                 guint            prop_id,
                                                 GValue          *value,
                                                 GParamSpec      *pspec);

struct _HippoCanvasThumbnails {
    HippoCanvasResource parent;

    HippoCanvasItem *more_link;
    HippoCanvasBox *thumbs_box;

    GSList *thumbnails;
};

struct _HippoCanvasThumbnailsClass {
    HippoCanvasResourceClass parent_class;
};

enum {
    PROP_0,
    PROP_HAS_THUMBNAILS
};

G_DEFINE_TYPE(HippoCanvasThumbnails, hippo_canvas_thumbnails, HIPPO_TYPE_CANVAS_RESOURCE)

static void
hippo_canvas_thumbnails_init(HippoCanvasThumbnails *thumbnail)
{
}

static void
hippo_canvas_thumbnails_class_init(HippoCanvasThumbnailsClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasResourceClass *resource_class = HIPPO_CANVAS_RESOURCE_CLASS(klass);

    object_class->dispose = hippo_canvas_thumbnails_dispose;
    object_class->finalize = hippo_canvas_thumbnails_finalize;

    object_class->get_property = hippo_canvas_thumbnails_get_property;

    resource_class->create_children = hippo_canvas_thumbnails_create_children;
    resource_class->update = hippo_canvas_thumbnails_update;
    
    g_object_class_install_property(object_class,
                                    PROP_HAS_THUMBNAILS,
                                    g_param_spec_boolean("has-thumbnails",
                                                         _("Has Thumbnails"),
                                                         _("Whether the block has any thumbnails"),
                                                         FALSE,
                                                         G_PARAM_READABLE));
}

static void
hippo_canvas_thumbnails_dispose (GObject *object)
{
    HippoCanvasThumbnails *canvas_thumbnails = HIPPO_CANVAS_THUMBNAILS(object);
    
    g_slist_free(canvas_thumbnails->thumbnails);
    canvas_thumbnails->thumbnails = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_thumbnails_parent_class)->dispose(object);
}

static void
hippo_canvas_thumbnails_finalize (GObject *object)
{
    G_OBJECT_CLASS(hippo_canvas_thumbnails_parent_class)->finalize(object);
}

static void
hippo_canvas_thumbnails_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasThumbnails *canvas_thumbnails;

    canvas_thumbnails = HIPPO_CANVAS_THUMBNAILS (object);

    switch (prop_id) {
    case PROP_HAS_THUMBNAILS:
        g_value_set_boolean(value, canvas_thumbnails->thumbnails != NULL);
        break;
    }
}

static void
hippo_canvas_thumbnails_create_children (HippoCanvasResource *canvas_resource)
{
    HippoCanvasThumbnails *canvas_thumbnails = HIPPO_CANVAS_THUMBNAILS(canvas_resource);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(canvas_resource);
    
    HippoCanvasBox *no_expand_box;
    
    canvas_thumbnails->thumbs_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                                 "spacing", 8,
                                                 "border", 8,
                                                 NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(canvas_thumbnails->thumbs_box), HIPPO_PACK_EXPAND);
    
    no_expand_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                 NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(no_expand_box), 0);

    canvas_thumbnails->more_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                                "actions", canvas_resource->actions,
                                                "text", "More...",
                                                "xalign", HIPPO_ALIGNMENT_START,
                                                "yalign", HIPPO_ALIGNMENT_END,
                                                NULL);
    hippo_canvas_box_append(no_expand_box, canvas_thumbnails->more_link, 0);
}

static gboolean
lists_equal(GSList *l,
            GSList *m)
{
    while (l && m) {
        if (l->data != m->data)
            return FALSE;
        
        l = l->next;
        m = m->next;
    }

    return (l == NULL && m == NULL);
}

static void
hippo_canvas_thumbnails_update  (HippoCanvasResource *canvas_resource)
{
    HippoCanvasThumbnails *canvas_thumbnails = HIPPO_CANVAS_THUMBNAILS(canvas_resource);
    
    const char *link = NULL;
    const char *title = NULL;
    GSList *thumbnails = NULL;
    GSList *l;
    gboolean has_thumbnails_changed;
    
    if (canvas_resource->resource != NULL)
        ddm_data_resource_get(canvas_resource->resource,
                              "moreThumbnailsLink",   DDM_DATA_URL,     &link,
                              "moreThumbnailsTitle",  DDM_DATA_STRING,  &title,
                              "thumbnails",           DDM_DATA_RESOURCE | DDM_DATA_LIST, &thumbnails,
                              NULL);

    g_object_set(canvas_thumbnails->more_link,
                 "url", link,
                 "tooltip", title,
                 NULL);

    if (lists_equal(thumbnails, canvas_thumbnails->thumbnails))
        return;

    has_thumbnails_changed = (thumbnails == NULL) != (canvas_thumbnails->thumbnails == NULL);

    g_slist_free(canvas_thumbnails->thumbnails);
    canvas_thumbnails->thumbnails = g_slist_copy(thumbnails);
                      
    hippo_canvas_box_remove_all(canvas_thumbnails->thumbs_box);
    for (l = thumbnails; l; l = l->next) {
        HippoCanvasItem *thumbnail = hippo_canvas_thumbnail_new(l->data, canvas_resource->actions);
        hippo_canvas_box_append(canvas_thumbnails->thumbs_box, thumbnail, 0);
    }

    if (has_thumbnails_changed)
        g_object_notify(G_OBJECT(canvas_thumbnails), "has-thumbnails");
}
