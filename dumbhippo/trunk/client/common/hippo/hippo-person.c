/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-person.h"
#include "hippo-entity-protected.h"
#include <string.h>

/* === HippoPerson implementation === */

static void     hippo_person_finalize             (GObject *object);

static void hippo_person_update(HippoEntity *entity);

static void hippo_person_set_property (GObject      *object,
                                       guint         prop_id,
                                       const GValue *value,
                                       GParamSpec   *pspec);
static void hippo_person_get_property (GObject      *object,
                                       guint         prop_id,
                                       GValue       *value,
                                       GParamSpec   *pspec);


struct _HippoPerson {
    HippoEntity parent;
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

enum {
    PROP_0,
    PROP_CURRENT_TRACK
};

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

    object_class->set_property = hippo_person_set_property;
    object_class->get_property = hippo_person_get_property;
    
    entity_class->update = hippo_person_update;


    g_object_class_install_property(object_class,
                                    PROP_CURRENT_TRACK,
                                    g_param_spec_object("current-track",
                                                        _("Current Track"),
                                                        _("Current track someone is listening to"),
                                                        HIPPO_TYPE_TRACK,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_person_finalize(GObject *object)
{
    HippoPerson *person = HIPPO_PERSON(object);

    if (person->current_track)
        g_object_unref(person->current_track);

    G_OBJECT_CLASS(hippo_person_parent_class)->finalize(object); 
}

static void
hippo_person_update(HippoEntity    *entity)
{
    /*    HippoPerson *person = HIPPO_PERSON(entity); */
    
    HIPPO_ENTITY_CLASS(hippo_person_parent_class)->update(entity);

    /* FIXME: currentTrack */
}

static void
hippo_person_set_property(GObject         *object,
                          guint            prop_id,
                          const GValue    *value,
                          GParamSpec      *pspec)
{
    HippoPerson *person;

    person = HIPPO_PERSON(object);

    switch (prop_id) {
    case PROP_CURRENT_TRACK:
        {
            HippoTrack *new_track;
            new_track = (HippoTrack*) g_value_get_object(value);
            if (new_track != person->current_track) {
                if (person->current_track)
                    g_object_unref(person->current_track);
                if (new_track)
                    g_object_ref(new_track);
                person->current_track = new_track;
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_person_get_property(GObject         *object,
                          guint            prop_id,
                          GValue          *value,
                          GParamSpec      *pspec)
{
    HippoPerson *person;

    person = HIPPO_PERSON(object);

    switch (prop_id) {
    case PROP_CURRENT_TRACK:
        g_value_set_object(value, (GObject*) person->current_track);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}


/* === HippoPerson exported API === */

HippoPerson*
hippo_person_get_for_resource(DDMDataResource *resource)
{
    HippoPerson *person = ddm_data_resource_get_data(resource, "hippo-entity");
    if (person == NULL)
        person = HIPPO_PERSON(hippo_entity_new(HIPPO_ENTITY_PERSON, resource));
    else
        g_object_ref(person);

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

    if (person->current_track)
        return hippo_track_get_name(person->current_track);
    else
        return NULL;
}

const char*
hippo_person_get_current_artist(HippoPerson *person)
{
    g_return_val_if_fail(HIPPO_IS_PERSON(person), NULL);

    if (person->current_track)
        return hippo_track_get_artist(person->current_track);
    else
        return NULL;
}

gboolean
hippo_person_get_music_playing(HippoPerson *person)
{
    g_return_val_if_fail(HIPPO_IS_PERSON(person), FALSE);

    if (person->current_track)
        return hippo_track_get_now_playing(person->current_track);
    else
        return FALSE;
}
