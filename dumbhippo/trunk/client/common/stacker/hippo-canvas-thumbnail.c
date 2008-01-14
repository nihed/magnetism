/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include <string.h>
#include <cairo.h>
#include "hippo-canvas-thumbnail.h"
#include "hippo-canvas-resource.h"
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include "hippo-canvas-url-image.h"
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_thumbnail_dispose             (GObject                *object);
static void      hippo_canvas_thumbnail_finalize            (GObject                *object);

static void hippo_canvas_thumbnail_create_children (HippoCanvasResource *canvas_resource);
static void hippo_canvas_thumbnail_update          (HippoCanvasResource *canvas_resource);

struct _HippoCanvasThumbnail {
    HippoCanvasResource parent;

    HippoCanvasItem *image;
    HippoCanvasItem *caption;
    
    char *src;
};

struct _HippoCanvasThumbnailClass {
    HippoCanvasResourceClass parent_class;
};

G_DEFINE_TYPE(HippoCanvasThumbnail, hippo_canvas_thumbnail, HIPPO_TYPE_CANVAS_RESOURCE)

static void
hippo_canvas_thumbnail_init(HippoCanvasThumbnail *thumbnail)
{
}

static void
hippo_canvas_thumbnail_class_init(HippoCanvasThumbnailClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasResourceClass *resource_class = HIPPO_CANVAS_RESOURCE_CLASS(klass);

    object_class->dispose = hippo_canvas_thumbnail_dispose;
    object_class->finalize = hippo_canvas_thumbnail_finalize;

    resource_class->create_children = hippo_canvas_thumbnail_create_children;
    resource_class->update = hippo_canvas_thumbnail_update;
}

static void
hippo_canvas_thumbnail_dispose (GObject *object)
{
    G_OBJECT_CLASS(hippo_canvas_thumbnail_parent_class)->dispose(object);
}

static void
hippo_canvas_thumbnail_finalize (GObject *object)
{
    HippoCanvasThumbnail *canvas_thumbnail = HIPPO_CANVAS_THUMBNAIL(object);

    g_free(canvas_thumbnail->src);

    G_OBJECT_CLASS(hippo_canvas_thumbnail_parent_class)->finalize(object);
}

static void
hippo_canvas_thumbnail_create_children (HippoCanvasResource *canvas_resource)
{
    HippoCanvasThumbnail *canvas_thumbnail = HIPPO_CANVAS_THUMBNAIL(canvas_resource);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(canvas_resource);
    HippoCanvasBox *no_expand_box;

    g_object_set(box,
                 "spacing", 4,
                 NULL);
        
    no_expand_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                 "xalign", HIPPO_ALIGNMENT_CENTER,
                                 "yalign", HIPPO_ALIGNMENT_CENTER,
                                 NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(no_expand_box), HIPPO_PACK_EXPAND);
    
    canvas_thumbnail->image = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                           "actions", canvas_resource->actions,
                                           NULL);
    hippo_canvas_box_append(no_expand_box, canvas_thumbnail->image, 0);
    
    no_expand_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                 "xalign", HIPPO_ALIGNMENT_CENTER,
                                 "yalign", HIPPO_ALIGNMENT_CENTER,
                                 NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(no_expand_box), HIPPO_PACK_EXPAND);
    
    canvas_thumbnail->caption = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                             "actions", canvas_resource->actions,
                                             "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                             NULL);
    hippo_canvas_box_append(no_expand_box, canvas_thumbnail->caption, 0);
}

static void
hippo_canvas_thumbnail_update  (HippoCanvasResource *canvas_resource)
{
    HippoCanvasThumbnail *canvas_thumbnail = HIPPO_CANVAS_THUMBNAIL(canvas_resource);
    
    const char *src = NULL;
    const char *link = NULL;
    const char *title = NULL;
    int width = 0;
    int height = 0;
    
    if (canvas_resource->resource != NULL)
        ddm_data_resource_get(canvas_resource->resource,
                              "src",    DDM_DATA_URL,     &src,
                              "link",   DDM_DATA_URL,     &link,
                              "title",  DDM_DATA_STRING,  &title,
                              "width",  DDM_DATA_INTEGER, &width,
                              "height", DDM_DATA_INTEGER, &height,
                              NULL);

    g_object_set(canvas_thumbnail->image,
                 "url", link,
                 /* "tooltip", title, */
                 NULL);

    if (src != canvas_thumbnail->src &&
        (src == NULL || canvas_thumbnail->src == NULL || strcmp(src, canvas_thumbnail->src) != 0))
    {
        g_free(canvas_thumbnail->src);
        canvas_thumbnail->src = g_strdup(src);
        
        if (src)
            hippo_actions_load_thumbnail_async(canvas_resource->actions,
                                               src, canvas_thumbnail->image);
        else
            g_object_set(canvas_thumbnail->image,
                         "image", NULL,
                         NULL);
    }

    g_object_set(canvas_thumbnail->caption,
                 "url", link,
                 "text", title,
                 NULL);
}

HippoCanvasItem*
hippo_canvas_thumbnail_new(DDMDataResource *resource,
                           HippoActions    *actions)
{
    return (HippoCanvasItem *)g_object_new(HIPPO_TYPE_CANVAS_THUMBNAIL,
                                           "resource", resource,
                                           "actions", actions,
                                           NULL);
}
