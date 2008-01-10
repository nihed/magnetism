/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-facebook-event.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include "hippo-thumbnails.h"
#include <string.h>

static void      hippo_block_facebook_event_init                (HippoBlockFacebookEvent       *block_facebook_event);
static void      hippo_block_facebook_event_class_init          (HippoBlockFacebookEventClass  *klass);

static void      hippo_block_facebook_event_dispose             (GObject              *object);
static void      hippo_block_facebook_event_finalize            (GObject              *object);

static gboolean  hippo_block_facebook_event_update_from_xml     (HippoBlock           *block,
                                                                  HippoDataCache       *cache,
                                                                  LmMessageNode        *node);

static void hippo_block_facebook_event_set_property (GObject      *object,
                                                      guint         prop_id,
                                                      const GValue *value,
                                                      GParamSpec   *pspec);
static void hippo_block_facebook_event_get_property (GObject      *object,
                                                      guint         prop_id,
                                                      GValue       *value,
                                                      GParamSpec   *pspec);
static void set_thumbnails (HippoBlockFacebookEvent *block_facebook_event,
                            HippoThumbnails          *thumbnails);

struct _HippoBlockFacebookEvent {
    HippoBlockAbstractPerson      parent;
    HippoThumbnails *thumbnails;
    char *title;
};

struct _HippoBlockFacebookEventClass {
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

G_DEFINE_TYPE(HippoBlockFacebookEvent, hippo_block_facebook_event, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_facebook_event_init(HippoBlockFacebookEvent *block_facebook_event)
{
}

static void
hippo_block_facebook_event_class_init(HippoBlockFacebookEventClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_facebook_event_set_property;
    object_class->get_property = hippo_block_facebook_event_get_property;

    object_class->dispose = hippo_block_facebook_event_dispose;
    object_class->finalize = hippo_block_facebook_event_finalize;

    block_class->update_from_xml = hippo_block_facebook_event_update_from_xml;

    g_object_class_install_property(object_class,
                                    PROP_THUMBNAILS,
                                    g_param_spec_object("thumbnails",
                                                        _("Thumbnails"),
                                                        _("The event thumbnails"),
                                                        HIPPO_TYPE_THUMBNAILS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_TITLE,
                                    g_param_spec_string("title",
                                                        _("title"),
                                                        _("Title of the event"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    
}

static void
hippo_block_facebook_event_dispose(GObject *object)
{
    HippoBlockFacebookEvent *block_facebook_event = HIPPO_BLOCK_FACEBOOK_EVENT(object);

    set_thumbnails(block_facebook_event, NULL);

    G_OBJECT_CLASS(hippo_block_facebook_event_parent_class)->dispose(object);
}

static void
hippo_block_facebook_event_finalize(GObject *object)
{
    HippoBlockFacebookEvent *block_facebook_event = HIPPO_BLOCK_FACEBOOK_EVENT(object);

    g_free(block_facebook_event->title);

    G_OBJECT_CLASS(hippo_block_facebook_event_parent_class)->finalize(object);
}

static void
hippo_block_facebook_event_set_property(GObject         *object,
                                         guint            prop_id,
                                         const GValue    *value,
                                         GParamSpec      *pspec)
{
    HippoBlockFacebookEvent *block_facebook_event = HIPPO_BLOCK_FACEBOOK_EVENT(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        set_thumbnails(block_facebook_event, (HippoThumbnails*) g_value_get_object(value));
        break;
    case PROP_TITLE: /* read-only */
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_facebook_event_get_property(GObject         *object,
                                         guint            prop_id,
                                         GValue          *value,
                                         GParamSpec      *pspec)
{
    HippoBlockFacebookEvent *block_facebook_event = HIPPO_BLOCK_FACEBOOK_EVENT(object);

    switch (prop_id) {
    case PROP_THUMBNAILS:
        g_value_set_object(value, (GObject*) block_facebook_event->thumbnails);
        break;
    case PROP_TITLE:
        g_value_set_string(value, block_facebook_event->title);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_thumbnails (HippoBlockFacebookEvent *block_facebook_event,
                HippoThumbnails       *thumbnails)
{
    if (block_facebook_event->thumbnails == thumbnails)
        return;

    if (block_facebook_event->thumbnails) {
        g_object_unref(block_facebook_event->thumbnails);

        block_facebook_event->thumbnails = NULL;
    }

    if (thumbnails) {
        g_object_ref(thumbnails);
        block_facebook_event->thumbnails = thumbnails;
    }

    g_object_notify(G_OBJECT(block_facebook_event), "thumbnails");
}

static gboolean
hippo_block_facebook_event_update_from_xml (HippoBlock           *block,
                                             HippoDataCache       *cache,
                                             LmMessageNode        *node)
{
    HippoBlockFacebookEvent *block_facebook_event = HIPPO_BLOCK_FACEBOOK_EVENT(block);
    LmMessageNode *facebook_node;
    LmMessageNode *thumbnails_node;
    HippoPerson *user;
    HippoThumbnails *thumbnails;
    const char *title;

    if (!HIPPO_BLOCK_CLASS(hippo_block_facebook_event_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "facebookEvent", HIPPO_SPLIT_NODE, &facebook_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, facebook_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "title", HIPPO_SPLIT_STRING, &title,
                         "thumbnails", HIPPO_SPLIT_NODE, &thumbnails_node,
                         NULL))
        return FALSE;

    thumbnails = hippo_thumbnails_new_from_xml(cache, thumbnails_node);
    if (thumbnails == NULL)
        return FALSE;
    
    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_facebook_event), user);

    set_thumbnails(block_facebook_event, thumbnails);
    
    g_free(block_facebook_event->title);
    block_facebook_event->title = g_strdup(title);    
    g_object_notify(G_OBJECT(block_facebook_event), "title");
    
    return TRUE;
}

HippoThumbnails*
hippo_block_facebook_event_get_thumbnails(HippoBlockFacebookEvent *block_facebook_event)
{
    return block_facebook_event->thumbnails;
}

const char*
hippo_block_facebook_event_get_title(HippoBlockFacebookEvent *block_facebook_event)
{
    return block_facebook_event->title;
}
