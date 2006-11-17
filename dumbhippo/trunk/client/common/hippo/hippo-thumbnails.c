/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-thumbnails.h"
#include <string.h>

static void      hippo_thumbnails_init                (HippoThumbnails       *thumbnails);
static void      hippo_thumbnails_class_init          (HippoThumbnailsClass  *klass);

static void      hippo_thumbnails_dispose             (GObject                *object);
static void      hippo_thumbnails_finalize            (GObject                *object);

static void hippo_thumbnails_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_thumbnails_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


struct _HippoThumbnail {
    char *src;
    char *href;
    int width;
    int height;
    char *title;
};

struct _HippoThumbnails {
    GObject parent;
    int count;
    int max_width;
    int max_height;
    int total_items;
    char *total_items_string;
    HippoThumbnail *thumbs;
    char *more_title;
    char *more_link;    
};

struct _HippoThumbnailsClass {
    GObjectClass parent_class;
};

enum {
    PROP_0
};

G_DEFINE_TYPE(HippoThumbnails, hippo_thumbnails, G_TYPE_OBJECT);


static void
hippo_thumbnails_init(HippoThumbnails  *thumbnails)
{

}

static void
hippo_thumbnails_class_init(HippoThumbnailsClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->set_property = hippo_thumbnails_set_property;
    object_class->get_property = hippo_thumbnails_get_property;

    object_class->dispose = hippo_thumbnails_dispose;
    object_class->finalize = hippo_thumbnails_finalize;
}

static void
hippo_thumbnails_dispose(GObject *object)
{

    G_OBJECT_CLASS(hippo_thumbnails_parent_class)->dispose(object);
}

static void
hippo_thumbnails_finalize(GObject *object)
{
    HippoThumbnails *thumbnails = HIPPO_THUMBNAILS(object);
    int i;
    
    g_free(thumbnails->total_items_string);
    g_free(thumbnails->more_title);
    g_free(thumbnails->more_link);

    /* ->thumbs is NULL if count is 0; also, not all thumbs
     * are initialized if we finalize due to an xml error
     * while creating the object
     */
    for (i = 0; i < thumbnails->count; ++i) {
        HippoThumbnail *thumb = &thumbnails->thumbs[i];
        g_free(thumb->src);
        g_free(thumb->href);
        g_free(thumb->title);
    }

    g_free(thumbnails->thumbs);
    
    G_OBJECT_CLASS(hippo_thumbnails_parent_class)->finalize(object);
}

static void
hippo_thumbnails_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoThumbnails *thumbnails;

    thumbnails = HIPPO_THUMBNAILS (object);

    switch (prop_id) {

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_thumbnails_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoThumbnails *thumbnails;

    thumbnails = HIPPO_THUMBNAILS (object);

    switch (prop_id) {

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

HippoThumbnails*
hippo_thumbnails_new_from_xml (HippoDataCache       *cache,
                               LmMessageNode        *node)
{
    int max_width;
    int max_height;
    int count;
    int total_items;
    const char *total_items_str;
    const char *more_title;
    const char *more_link;
    HippoThumbnails *thumbnails;
    LmMessageNode *child;
    int i;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "totalItems", HIPPO_SPLIT_INT32, &total_items,
                         "totalItemsString", HIPPO_SPLIT_STRING, &total_items_str,
                         /* FIXME maxWidth, maxHeight */
                         "width", HIPPO_SPLIT_INT32, &max_width,
                         "height", HIPPO_SPLIT_INT32, &max_height,
                         "count", HIPPO_SPLIT_INT32, &count,
                         "moreTitle", HIPPO_SPLIT_STRING, &more_title,
                         "moreLink", HIPPO_SPLIT_URI_ABSOLUTE, &more_link,
                         NULL))
        return NULL;

    if (count < 0) {
        g_warning("negative count in <thumbnails>");
        return NULL;
    }

    thumbnails = g_object_new(HIPPO_TYPE_THUMBNAILS, NULL);
    thumbnails->count = count;
    thumbnails->max_width = max_width;
    thumbnails->max_height = max_height;
    thumbnails->total_items = total_items;
    thumbnails->total_items_string = g_strdup(total_items_str);
    thumbnails->more_title = g_strdup(more_title);
    thumbnails->more_link = g_strdup(more_link);

    /* note if count is 0, this results in thumbs = NULL */
    thumbnails->thumbs = g_new0(HippoThumbnail, count);

    i = 0;
    for (child = node->children; child != NULL; child = child->next) {
        const char *src;
        const char *href;
        const char *child_title;
        int width, height;
        HippoThumbnail *thumb;

        if (strcmp(child->name, "thumbnail") != 0)
            continue;
        
        thumb = &thumbnails->thumbs[i];
        ++i;
        
        if (!hippo_xml_split(cache, child, NULL,
                             "src", HIPPO_SPLIT_STRING, &src,
                             "href", HIPPO_SPLIT_URI_ABSOLUTE, &href,
                             "title", HIPPO_SPLIT_STRING, &child_title,
                             "width", HIPPO_SPLIT_INT32, &width,
                             "height", HIPPO_SPLIT_INT32, &height,
                             NULL)) {
            g_object_unref(thumbnails);
            return NULL;
        }

        thumb->src = g_strdup(src);
        thumb->href = g_strdup(href);
        thumb->width = width;
        thumb->height = height;
        thumb->title = g_strdup(child_title);
    }

    if (i != count) {
        g_object_unref(thumbnails);
        return NULL;
    }
    
    return thumbnails;
}

int
hippo_thumbnails_get_count(HippoThumbnails *thumbnails)
{
    return thumbnails->count;
}

int
hippo_thumbnails_get_max_width(HippoThumbnails *thumbnails)
{
    return thumbnails->max_width;
}

int
hippo_thumbnails_get_max_height(HippoThumbnails *thumbnails)
{
    return thumbnails->max_height;
}

int
hippo_thumbnails_get_total_items(HippoThumbnails *thumbnails)
{
    return thumbnails->total_items;
}

const char*
hippo_thumbnails_get_total_items_string(HippoThumbnails *thumbnails)
{
    return thumbnails->total_items_string;
}

const char*
hippo_thumbnails_get_more_title(HippoThumbnails *thumbnails)
{
    return thumbnails->more_title;
}

const char*
hippo_thumbnails_get_more_link(HippoThumbnails *thumbnails)
{
    return thumbnails->more_link;
}

HippoThumbnail*
hippo_thumbnails_get_nth(HippoThumbnails *thumbnails,
                         int              which)
{
    g_return_val_if_fail(which < thumbnails->count, NULL);
    return &thumbnails->thumbs[which];
}


const char*
hippo_thumbnail_get_src(HippoThumbnail  *thumbnail)
{
    return thumbnail->src;
}

const char*
hippo_thumbnail_get_href(HippoThumbnail  *thumbnail)
{
    return thumbnail->href;
}

int
hippo_thumbnail_get_width(HippoThumbnail  *thumbnail)
{
    return thumbnail->width;
}

int
hippo_thumbnail_get_height(HippoThumbnail  *thumbnail)
{
    return thumbnail->height;
}

const char*
hippo_thumbnail_get_title(HippoThumbnail  *thumbnail)
{
    return thumbnail->title;
}

