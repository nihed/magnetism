#include "hippo-person.h"
#include "hippo-entity-protected.h"
#include <string.h>

/* === HippoPerson implementation === */

static void     hippo_person_finalize             (GObject *object);

struct _HippoPerson {
    HippoEntity parent;
    char *current_song;
    char *current_artist;
    guint music_playing : 1;
};

struct _HippoPersonClass {
    HippoEntityClass parent;
};

G_DEFINE_TYPE(HippoPerson, hippo_person, HIPPO_TYPE_ENTITY);

enum {
    NONE_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_person_init(HippoPerson *person)
{
}

static void
hippo_person_class_init(HippoPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    object_class->finalize = hippo_person_finalize;
}

static void
hippo_person_finalize(GObject *object)
{
    HippoPerson *person = HIPPO_PERSON(object);

    g_free(person->current_song);
    g_free(person->current_artist);

    G_OBJECT_CLASS(hippo_person_parent_class)->finalize(object); 
}

/* === HippoPerson exported API === */

HippoPerson*
hippo_person_new(const char *guid)
{
    HippoPerson *person = HIPPO_PERSON(hippo_entity_new(HIPPO_ENTITY_PERSON, guid));
    
    return person;
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
        hippo_entity_emit_changed(HIPPO_ENTITY(person));
    }
}
                               
