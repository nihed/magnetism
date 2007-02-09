/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-flickr-photoset.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include "hippo-thumbnails.h"
#include <string.h>

static void      hippo_block_flickr_photoset_init                (HippoBlockFlickrPhotoset       *block_flickr_photoset);
static void      hippo_block_flickr_photoset_class_init          (HippoBlockFlickrPhotosetClass  *klass);

static void      hippo_block_flickr_photoset_dispose             (GObject              *object);
static void      hippo_block_flickr_photoset_finalize            (GObject              *object);

static gboolean  hippo_block_flickr_photoset_update_from_xml     (HippoBlock           *block,
                                                                  HippoDataCache       *cache,
                                                                  LmMessageNode        *node);

static void hippo_block_flickr_photoset_set_property (GObject      *object,
                                                      guint         prop_id,
                                                      const GValue *value,
                                                      GParamSpec   *pspec);
static void hippo_block_flickr_photoset_get_property (GObject      *object,
                                                      guint         prop_id,
                                                      GValue       *value,
                                                      GParamSpec   *pspec);
static void set_thumbnails (HippoBlockFlickrPhotoset *block_flickr_photoset,
                            HippoThumbnails          *thumbnails);

struct _HippoBlockFlickrPhotoset {
    HippoBlockAbstractPerson      parent;
    HippoThumbnails *thumbnails;
    char *title;
};

struct _HippoBlockFlickrPhotosetClass {
    HippoBlockAbstractPersonClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_THUMBNAILS,
    PROP_TITLE
};

G_DEFINE_TYPE(HippoBlockFlickrPhotoset, hippo_block_flickr_photoset, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_flickr_photoset_init(HippoBlockFlickrPhotoset *block_flickr_photoset)
{
}

static void
hippo_block_flickr_photoset_class_init(HippoBlockFlickrPhotosetClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_flickr_photoset_set_property;
    object_class->get_property = hippo_block_flickr_photoset_get_property;

    object_class->dispose = hippo_block_flickr_photoset_dispose;
    object_class->finalize = hippo_block_flickr_photoset_finalize;

    block_class->update_from_xml = hippo_block_flickr_photoset_update_from_xml;

    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The photoset thumbnails"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("title"),
                                                        _("Title of the photoset"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    
}

static void
hippo_block_flickr_photoset_dispose(GObject *object)
{
    HippoBlockFlickrPhotoset *block_flickr_photoset = HIPPO_BLOCK_FLICKR_PHOTOSET(object);

    set_thumbnails(block_flickr_photoset, NULL);

    G_OBJECT_CLASS(hippo_block_flickr_photoset_parent_class)->dispose(object);
}

static void
hippo_block_flickr_photoset_finalize(GObject *object)
{
    HippoBlockFlickrPhotoset *block_flickr_photoset = HIPPO_BLOCK_FLICKR_PHOTOSET(object);

    g_free(block_flickr_photoset->title);

    G_OBJECT_CLASS(hippo_block_flickr_photoset_parent_class)->finalize(object);
}

static void
hippo_block_flickr_photoset_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    HippoBlockFlickrPhotoset *block_flickr_photoset = HIPPO_BLOCK_FLICKR_PHOTOSET(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        set_thumbnails(block_flickr_photoset, (HippoThumbnails*) g_value_get_object(value));
        break;
    case PROP_TITLE: /* read-only */
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_flickr_photoset_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    HippoBlockFlickrPhotoset *block_flickr_photoset = HIPPO_BLOCK_FLICKR_PHOTOSET(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        g_value_set_object(value, (GObject*) block_flickr_photoset->thumbnails);
        break;
    case PROP_TITLE:
        g_value_set_string(value, block_flickr_photoset->title);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_thumbnails (HippoBlockFlickrPhotoset *block_flickr_photoset,
                HippoThumbnails       *thumbnails)
{
    if (block_flickr_photoset->thumbnails == thumbnails)
        return;

    if (block_flickr_photoset->thumbnails) {
        g_object_unref(block_flickr_photoset->thumbnails);

        block_flickr_photoset->thumbnails = NULL;
    }

    if (thumbnails) {
        g_object_ref(thumbnails);
        block_flickr_photoset->thumbnails = thumbnails;
    }

    g_object_notify(G_OBJECT(block_flickr_photoset), "thumbnails");
}

static gboolean
hippo_block_flickr_photoset_update_from_xml (HippoBlock           *block,
                                             HippoDataCache       *cache,
                                             LmMessageNode        *node)
{
    HippoBlockFlickrPhotoset *block_flickr_photoset = HIPPO_BLOCK_FLICKR_PHOTOSET(block);
    LmMessageNode *flickr_node;
    LmMessageNode *thumbnails_node;
    HippoPerson *user;
    HippoThumbnails *thumbnails;
    const char *title;

    if (!HIPPO_BLOCK_CLASS(hippo_block_flickr_photoset_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "flickrPhotoset", HIPPO_SPLIT_NODE, &flickr_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, flickr_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "title", HIPPO_SPLIT_STRING, &title,
                         "thumbnails", HIPPO_SPLIT_NODE, &thumbnails_node,
                         NULL))
        return FALSE;

    thumbnails = hippo_thumbnails_new_from_xml(cache, thumbnails_node);
    if (thumbnails == NULL)
        return FALSE;
    
    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_flickr_photoset), user);

    set_thumbnails(block_flickr_photoset, thumbnails);
    
    g_free(block_flickr_photoset->title);
    block_flickr_photoset->title = g_strdup(title);    
    g_object_notify(G_OBJECT(block_flickr_photoset), "title");
    
    return TRUE;
}

HippoThumbnails*
hippo_block_flickr_photoset_get_thumbnails(HippoBlockFlickrPhotoset *block_flickr_photoset)
{
    return block_flickr_photoset->thumbnails;
}

const char*
hippo_block_flickr_photoset_get_title(HippoBlockFlickrPhotoset *block_flickr_photoset)
{
    return block_flickr_photoset->title;
}
