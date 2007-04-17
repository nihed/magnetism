/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-flickr-person.h"
#include "hippo-block-generic.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include "hippo-feed-entry.h"
#include <string.h>

static void      hippo_block_flickr_person_init                (HippoBlockFlickrPerson       *block_flickr_person);
static void      hippo_block_flickr_person_class_init          (HippoBlockFlickrPersonClass  *klass);

static void      hippo_block_flickr_person_dispose             (GObject              *object);
static void      hippo_block_flickr_person_finalize            (GObject              *object);

static gboolean  hippo_block_flickr_person_update_from_xml     (HippoBlock           *block,
                                                                HippoDataCache       *cache,
                                                                LmMessageNode        *node);

struct _HippoBlockFlickrPerson {
    HippoBlockGeneric      parent;
};

struct _HippoBlockFlickrPersonClass {
    HippoBlockGeneric      parent_class;
};

G_DEFINE_TYPE(HippoBlockFlickrPerson, hippo_block_flickr_person, HIPPO_TYPE_BLOCK_GENERIC);

static void
hippo_block_flickr_person_init(HippoBlockFlickrPerson *block_flickr_person)
{
}

static void
hippo_block_flickr_person_class_init(HippoBlockFlickrPersonClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = hippo_block_flickr_person_dispose;
    object_class->finalize = hippo_block_flickr_person_finalize;

    block_class->update_from_xml = hippo_block_flickr_person_update_from_xml;
}

static void
hippo_block_flickr_person_dispose(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_flickr_person_parent_class)->dispose(object);
}

static void
hippo_block_flickr_person_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_flickr_person_parent_class)->finalize(object);
}

static gboolean
hippo_block_flickr_person_update_from_xml (HippoBlock           *block,
                                           HippoDataCache       *cache,
                                           LmMessageNode        *node)
{
    LmMessageNode *flickr_node;
    LmMessageNode *thumbnails_node;
    HippoPerson *user;
    HippoThumbnails *thumbnails;

    if (!HIPPO_BLOCK_CLASS(hippo_block_flickr_person_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "flickrPerson", HIPPO_SPLIT_NODE, &flickr_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, flickr_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "thumbnails", HIPPO_SPLIT_NODE, &thumbnails_node,
                         NULL))
        return FALSE;

    thumbnails = hippo_thumbnails_new_from_xml(cache, thumbnails_node);
    if (thumbnails == NULL)
        return FALSE;

    g_object_set(block,
                 "thumbnails", thumbnails,
                 "source", user,
                 "title-link", hippo_thumbnails_get_more_link(thumbnails),
                 NULL);

    g_object_unref(thumbnails);
    
    return TRUE;
}
