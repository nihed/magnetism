/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-music-person.h"
#include "hippo-block-abstract-person.h"
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

static void hippo_block_music_person_set_track_history(HippoBlockMusicPerson *block_music_person,
                                                       GSList                *track_history);

struct _HippoBlockMusicPerson {
    HippoBlockAbstractPerson            parent;

    GSList *track_history;
};

struct _HippoBlockMusicPersonClass {
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
    PROP_TRACK_HISTORY
};

G_DEFINE_TYPE(HippoBlockMusicPerson, hippo_block_music_person, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);
                       
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
                                    PROP_TRACK_HISTORY,
                                    g_param_spec_pointer("track-history",
                                                         _("Track History"),
                                                         _("Recent songs played by the user"),
                                                         G_PARAM_READABLE));
}

static void
hippo_block_music_person_dispose(GObject *object)
{
    HippoBlockMusicPerson *block_music_person = HIPPO_BLOCK_MUSIC_PERSON(object);

    hippo_block_music_person_set_track_history(block_music_person, NULL);
    
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
    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_music_person_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoBlockMusicPerson *block_music_person = HIPPO_BLOCK_MUSIC_PERSON(object);

    switch (prop_id) {
    case PROP_TRACK_HISTORY:
        g_value_set_pointer(value, block_music_person->track_history);
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
    LmMessageNode *history_node;
    HippoPerson *user;
    GSList *track_history;

    if (!HIPPO_BLOCK_CLASS(hippo_block_music_person_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "musicPerson", HIPPO_SPLIT_NODE, &music_node,
                         NULL))
        return FALSE;

    history_node = NULL;
    if (!hippo_xml_split(cache, music_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "trackHistory", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &history_node,
                         NULL))
        return FALSE;

    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block), user);

    track_history = NULL;
    
    if (history_node) {
        LmMessageNode *subchild;
        
        for (subchild = history_node->children; subchild; subchild = subchild->next) {
            HippoTrack *track;
            
            if (!strcmp(subchild->name, "track") == 0)
                continue;
            
            track = hippo_track_new_from_xml(cache, subchild);
            if (!track)
                continue;
            
            track_history = g_slist_prepend(track_history, track);
        }

        track_history = g_slist_reverse(track_history);
    } else {
        /* trackHistory is always present for servers after 2006-12-06, this is just for brief back-compat */
        HippoTrack *track = hippo_person_get_current_track(user);
        if (track)
            track_history = g_slist_prepend(track_history, g_object_ref(track));
    }

    hippo_block_music_person_set_track_history(block_music_person, track_history);

    g_slist_foreach(track_history, (GFunc)g_object_unref, NULL);
    g_slist_free(track_history);

    return TRUE;
}

static void 
hippo_block_music_person_set_track_history(HippoBlockMusicPerson *block_music_person,
                                           GSList                *track_history)
{
    if (block_music_person->track_history) {
        g_slist_foreach(block_music_person->track_history, (GFunc)g_object_unref, NULL);
        g_slist_free(block_music_person->track_history);
    }

    block_music_person->track_history = g_slist_copy(track_history);
    g_slist_foreach(block_music_person->track_history, (GFunc)g_object_ref, NULL);

    g_object_notify(G_OBJECT(block_music_person), "track-history");
}

