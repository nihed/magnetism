/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-youtube-person.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include "hippo-feed-entry.h"
#include <string.h>

static void      hippo_block_youtube_person_init                (HippoBlockYouTubePerson       *block_youtube_person);
static void      hippo_block_youtube_person_class_init          (HippoBlockYouTubePersonClass  *klass);

static void      hippo_block_youtube_person_dispose             (GObject              *object);
static void      hippo_block_youtube_person_finalize            (GObject              *object);

static gboolean  hippo_block_youtube_person_update_from_xml     (HippoBlock           *block,
                                                                HippoDataCache       *cache,
                                                                LmMessageNode        *node);

static void hippo_block_youtube_person_set_property (GObject      *object,
                                                    guint         prop_id,
                                                    const GValue *value,
                                                    GParamSpec   *pspec);
static void hippo_block_youtube_person_get_property (GObject      *object,
                                                    guint         prop_id,
                                                    GValue       *value,
                                                    GParamSpec   *pspec);
static void set_thumbnails (HippoBlockYouTubePerson *block_youtube_person,
                            HippoThumbnails        *thumbnails);

struct _HippoBlockYouTubePerson {
    HippoBlockAbstractPerson      parent;
    HippoThumbnails *thumbnails;
};

struct _HippoBlockYouTubePersonClass {
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
    PROP_THUMBNAILS
};

G_DEFINE_TYPE(HippoBlockYouTubePerson, hippo_block_youtube_person, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_youtube_person_init(HippoBlockYouTubePerson *block_youtube_person)
{
}

static void
hippo_block_youtube_person_class_init(HippoBlockYouTubePersonClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_youtube_person_set_property;
    object_class->get_property = hippo_block_youtube_person_get_property;

    object_class->dispose = hippo_block_youtube_person_dispose;
    object_class->finalize = hippo_block_youtube_person_finalize;

    block_class->update_from_xml = hippo_block_youtube_person_update_from_xml;

    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The YouTube thumbnails"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_block_youtube_person_dispose(GObject *object)
{
    HippoBlockYouTubePerson *block_youtube_person = HIPPO_BLOCK_YOUTUBE_PERSON(object);

    set_thumbnails(block_youtube_person, NULL);

    G_OBJECT_CLASS(hippo_block_youtube_person_parent_class)->dispose(object);
}

static void
hippo_block_youtube_person_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_youtube_person_parent_class)->finalize(object);
}

static void
hippo_block_youtube_person_set_property(GObject         *object,
                                       guint            prop_id,
                                       const GValue    *value,
                                       GParamSpec      *pspec)
{
    HippoBlockYouTubePerson *block_youtube_person = HIPPO_BLOCK_YOUTUBE_PERSON(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        set_thumbnails(block_youtube_person, (HippoThumbnails*) g_value_get_object(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_youtube_person_get_property(GObject         *object,
                                       guint            prop_id,
                                       GValue          *value,
                                       GParamSpec      *pspec)
{
    HippoBlockYouTubePerson *block_youtube_person = HIPPO_BLOCK_YOUTUBE_PERSON(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        g_value_set_object(value, (GObject*) block_youtube_person->thumbnails);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_thumbnails (HippoBlockYouTubePerson *block_youtube_person,
                HippoThumbnails       *thumbnails)
{
    if (block_youtube_person->thumbnails == thumbnails)
        return;

    if (block_youtube_person->thumbnails) {
        g_object_unref(block_youtube_person->thumbnails);

        block_youtube_person->thumbnails = NULL;
    }

    if (thumbnails) {
        g_object_ref(thumbnails);
        block_youtube_person->thumbnails = thumbnails;
    }

    g_object_notify(G_OBJECT(block_youtube_person), "thumbnails");
}

static gboolean
hippo_block_youtube_person_update_from_xml (HippoBlock           *block,
                                           HippoDataCache       *cache,
                                           LmMessageNode        *node)
{
    HippoBlockYouTubePerson *block_youtube_person = HIPPO_BLOCK_YOUTUBE_PERSON(block);
    LmMessageNode *youtube_node;
    LmMessageNode *thumbnails_node;
    HippoPerson *user;
    HippoThumbnails *thumbnails;

    if (!HIPPO_BLOCK_CLASS(hippo_block_youtube_person_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "youTubePerson", HIPPO_SPLIT_NODE, &youtube_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, youtube_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "thumbnails", HIPPO_SPLIT_NODE, &thumbnails_node,
                         NULL))
        return FALSE;

    thumbnails = hippo_thumbnails_new_from_xml(cache, thumbnails_node);
    if (thumbnails == NULL)
        return FALSE;
    
    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_youtube_person), user);
    
    set_thumbnails(block_youtube_person, thumbnails);

    return TRUE;
}

HippoThumbnails*
hippo_block_youtube_person_get_thumbnails(HippoBlockYouTubePerson *block_youtube_person)
{
    return block_youtube_person->thumbnails;
}
