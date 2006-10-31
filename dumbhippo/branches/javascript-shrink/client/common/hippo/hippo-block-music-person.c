/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-music-person.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_music_person_init                (HippoBlockMusicPerson       *block_music_person);
static void      hippo_block_music_person_class_init          (HippoBlockMusicPersonClass  *klass);

static void      hippo_block_music_person_dispose             (GObject              *object);
static void      hippo_block_music_person_finalize            (GObject              *object);

static gboolean  hippo_block_music_person_update_from_xml     (HippoBlock           *block,
                                                               HippoDataCache       *cache,
                                                               LmMessageNode        *node);

static void hippo_block_music_person_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_block_music_person_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);

struct _HippoBlockMusicPerson {
    HippoBlock            parent;
    HippoPerson          *user;
};

struct _HippoBlockMusicPersonClass {
    HippoBlockClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_USER
};

G_DEFINE_TYPE(HippoBlockMusicPerson, hippo_block_music_person, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_music_person_init(HippoBlockMusicPerson *block_music_person)
{
}

static void
hippo_block_music_person_class_init(HippoBlockMusicPersonClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_music_person_set_property;
    object_class->get_property = hippo_block_music_person_get_property;

    object_class->dispose = hippo_block_music_person_dispose;
    object_class->finalize = hippo_block_music_person_finalize;

    block_class->update_from_xml = hippo_block_music_person_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_USER,
                                    g_param_spec_object("user",
                                                        _("User"),
                                                        _("User who's music has changed"),
                                                        HIPPO_TYPE_PERSON,
                                                        G_PARAM_READABLE));
}

static void
set_user(HippoBlockMusicPerson *block_music_person,
         HippoPerson           *user)
{
    if (user == block_music_person->user)
        return;
    
    if (block_music_person->user) {
        g_object_unref(block_music_person->user);
        block_music_person->user = NULL;
    }

    if (user) {
        g_object_ref(user);
        block_music_person->user = user;
    }

    g_object_notify(G_OBJECT(block_music_person), "user");
}

static void
hippo_block_music_person_dispose(GObject *object)
{
    HippoBlockMusicPerson *block_music_person = HIPPO_BLOCK_MUSIC_PERSON(object);

    set_user(block_music_person, NULL);
    
    G_OBJECT_CLASS(hippo_block_music_person_parent_class)->dispose(object); 
}

static void
hippo_block_music_person_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_music_person_parent_class)->finalize(object); 
}

static void
hippo_block_music_person_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_music_person_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoBlockMusicPerson *block_music_person = HIPPO_BLOCK_MUSIC_PERSON(object);

    switch (prop_id) {
    case PROP_USER:
        g_value_set_object(value, G_OBJECT(block_music_person->user));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_music_person_update_from_xml (HippoBlock           *block,
                                          HippoDataCache       *cache,
                                          LmMessageNode        *node)
{
    HippoBlockMusicPerson *block_music_person = HIPPO_BLOCK_MUSIC_PERSON(block);
    LmMessageNode *music_node;
    HippoPerson *user;

    if (!HIPPO_BLOCK_CLASS(hippo_block_music_person_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "musicPerson", HIPPO_SPLIT_NODE, &music_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, music_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         NULL))
        return FALSE;

    set_user(block_music_person, user);

    return TRUE;
}
