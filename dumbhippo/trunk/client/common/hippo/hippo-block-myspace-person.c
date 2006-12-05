/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-myspace-person.h"
#include "hippo-block-abstract-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include "hippo-feed-entry.h"
#include <string.h>

static void      hippo_block_myspace_person_init                (HippoBlockMySpacePerson       *block_myspace_person);
static void      hippo_block_myspace_person_class_init          (HippoBlockMySpacePersonClass  *klass);

static void      hippo_block_myspace_person_dispose             (GObject              *object);
static void      hippo_block_myspace_person_finalize            (GObject              *object);

static gboolean  hippo_block_myspace_person_update_from_xml     (HippoBlock           *block,
                                                              HippoDataCache       *cache,
                                                              LmMessageNode        *node);

static void hippo_block_myspace_person_set_property (GObject      *object,
                                                  guint         prop_id,
                                                  const GValue *value,
                                                  GParamSpec   *pspec);
static void hippo_block_myspace_person_get_property (GObject      *object,
                                                  guint         prop_id,
                                                  GValue       *value,
                                                  GParamSpec   *pspec);
static void set_entry (HippoBlockMySpacePerson *block_myspace_person,
                       HippoFeedEntry       *entry);

struct _HippoBlockMySpacePerson {
    HippoBlockAbstractPerson      parent;
    HippoFeedEntry *entry;
};

struct _HippoBlockMySpacePersonClass {
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
    PROP_ENTRY
};

G_DEFINE_TYPE(HippoBlockMySpacePerson, hippo_block_myspace_person, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);

static void
hippo_block_myspace_person_init(HippoBlockMySpacePerson *block_myspace_person)
{
}

static void
hippo_block_myspace_person_class_init(HippoBlockMySpacePersonClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_block_myspace_person_set_property;
    object_class->get_property = hippo_block_myspace_person_get_property;

    object_class->dispose = hippo_block_myspace_person_dispose;
    object_class->finalize = hippo_block_myspace_person_finalize;

    block_class->update_from_xml = hippo_block_myspace_person_update_from_xml;

    g_object_class_install_property(object_class,
                                    PROP_ENTRY,
                                    g_param_spec_object("entry",
                                                        _("Entry"),
                                                        _("The MySpace blog entry"),
                                                        HIPPO_TYPE_FEED_ENTRY,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_block_myspace_person_dispose(GObject *object)
{
    HippoBlockMySpacePerson *block_myspace_person = HIPPO_BLOCK_MYSPACE_PERSON(object);

    set_entry(block_myspace_person, NULL);
    
    G_OBJECT_CLASS(hippo_block_myspace_person_parent_class)->dispose(object);
}

static void
hippo_block_myspace_person_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_myspace_person_parent_class)->finalize(object);
}

static void
hippo_block_myspace_person_set_property(GObject         *object,
                                     guint            prop_id,
                                     const GValue    *value,
                                     GParamSpec      *pspec)
{
    HippoBlockMySpacePerson *block_myspace_person = HIPPO_BLOCK_MYSPACE_PERSON(object);

    switch (prop_id) {
    case PROP_ENTRY:
        set_entry(block_myspace_person, (HippoFeedEntry*) g_value_get_object(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_myspace_person_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoBlockMySpacePerson *block_myspace_person = HIPPO_BLOCK_MYSPACE_PERSON(object);

    switch (prop_id) {
    case PROP_ENTRY:
        g_value_set_object(value, (GObject*) block_myspace_person->entry);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
set_entry (HippoBlockMySpacePerson *block_myspace_person,
           HippoFeedEntry       *entry)
{
    if (block_myspace_person->entry == entry)
        return;

    if (block_myspace_person->entry) {
        g_object_unref(block_myspace_person->entry);

        block_myspace_person->entry = NULL;
    }
    
    if (entry) {
        g_object_ref(entry);
        block_myspace_person->entry = entry;
    }
    
    g_object_notify(G_OBJECT(block_myspace_person), "entry");
}

static gboolean
hippo_block_myspace_person_update_from_xml (HippoBlock           *block,
                                         HippoDataCache       *cache,
                                         LmMessageNode        *node)
{
    HippoBlockMySpacePerson *block_myspace_person = HIPPO_BLOCK_MYSPACE_PERSON(block);
    LmMessageNode *blog_node;
    LmMessageNode *entry_node;
    HippoPerson *user;
    HippoFeedEntry *entry;
    
    if (!HIPPO_BLOCK_CLASS(hippo_block_myspace_person_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "mySpacePerson", HIPPO_SPLIT_NODE, &blog_node,
                         NULL))
        return FALSE;
    
    if (!hippo_xml_split(cache, blog_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "feedEntry", HIPPO_SPLIT_NODE, &entry_node,
                         NULL))
        return FALSE;

    entry = hippo_feed_entry_new_from_xml(cache, entry_node);
    if (entry == NULL)
        return FALSE;
    
    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block_myspace_person), user);

    set_entry(block_myspace_person, entry);
    
    return TRUE;
}

HippoFeedEntry*
hippo_block_myspace_person_get_entry(HippoBlockMySpacePerson *block_myspace_person)
{
    return block_myspace_person->entry;
}
