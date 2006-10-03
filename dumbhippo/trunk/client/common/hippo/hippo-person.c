/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-person.h"
#include "hippo-entity-protected.h"
#include "hippo-xml-utils.h"
#include <string.h>

/* === HippoPerson implementation === */

static void     hippo_person_finalize             (GObject *object);

static gboolean hippo_person_update_from_xml(HippoEntity    *entity,
                                             HippoDataCache *cache,
                                             LmMessageNode  *node);

struct _HippoPerson {
    HippoEntity parent;
    char *current_song;
    char *current_artist;
    guint music_playing : 1;
    HippoTrack *current_track;
};

struct _HippoPersonClass {
    HippoEntityClass parent;
};

G_DEFINE_TYPE(HippoPerson, hippo_person, HIPPO_TYPE_ENTITY);

#if 0
enum {
    NONE_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

static void
hippo_person_init(HippoPerson *person)
{
}

static void
hippo_person_class_init(HippoPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoEntityClass *entity_class = HIPPO_ENTITY_CLASS (klass);
          
    object_class->finalize = hippo_person_finalize;
    entity_class->update_from_xml = hippo_person_update_from_xml;
}

static void
hippo_person_finalize(GObject *object)
{
    HippoPerson *person = HIPPO_PERSON(object);

    g_free(person->current_song);
    g_free(person->current_artist);
    if (person->current_track)
        g_object_unref(person->current_track);

    G_OBJECT_CLASS(hippo_person_parent_class)->finalize(object); 
}

static gboolean
hippo_person_update_from_xml(HippoEntity    *entity,
                             HippoDataCache *cache,
                             LmMessageNode  *node)
{
    HippoPerson *person = HIPPO_PERSON(entity);
    LmMessageNode *current_track_node = NULL;
    HippoTrack *track;
    
    if (!HIPPO_ENTITY_CLASS(hippo_person_parent_class)->update_from_xml(entity, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "currentTrack", HIPPO_SPLIT_NODE | HIPPO_SPLIT_OPTIONAL, &current_track_node,
                         NULL))
        return FALSE;

    if (current_track_node) {
        track = hippo_track_new_from_xml(cache, current_track_node);
        if (!track)
            return FALSE;

        if (person->current_track)
            g_object_unref(person->current_track);

        person->current_track = track;

        hippo_entity_notify(HIPPO_ENTITY(person));
    }

    return TRUE;
}

/* === HippoPerson exported API === */

HippoPerson*
hippo_person_new(const char *guid)
{
    HippoPerson *person = HIPPO_PERSON(hippo_entity_new(HIPPO_ENTITY_PERSON, guid));
    
    return person;
}

HippoTrack *
hippo_person_get_current_track(HippoPerson *person)
{
    return person->current_track;
}

const char*
hippo_person_get_current_song(HippoPerson *person)
{
    g_return_val_if_fail(HIPPO_IS_PERSON(person), NULL);
    
    return person->current_song;    
}

const char*
hippo_person_get_current_artist(HippoPerson *person)
{
    g_return_val_if_fail(HIPPO_IS_PERSON(person), NULL);
    
    return person->current_artist;
}

gboolean
hippo_person_get_music_playing(HippoPerson *person)
{
    g_return_val_if_fail(HIPPO_IS_PERSON(person), FALSE);
    
    return person->music_playing;
}

void
hippo_person_set_current_song(HippoPerson *person,
                              const char  *song)
{
    g_return_if_fail(HIPPO_IS_PERSON(person));
    
    hippo_entity_set_string(HIPPO_ENTITY(person), &person->current_song, song);    
}
                                        
void
hippo_person_set_current_artist(HippoPerson *person,
                                const char  *artist)
{
    g_return_if_fail(HIPPO_IS_PERSON(person));
    
    hippo_entity_set_string(HIPPO_ENTITY(person), &person->current_artist, artist);    
}
                                
void
hippo_person_set_music_playing(HippoPerson *person,
                               gboolean     is_playing)
{
    g_return_if_fail(HIPPO_IS_PERSON(person));

    is_playing = is_playing != FALSE;
    if (person->music_playing != is_playing) {
        person->music_playing = is_playing;
        hippo_entity_notify(HIPPO_ENTITY(person));
    }
}
                               
