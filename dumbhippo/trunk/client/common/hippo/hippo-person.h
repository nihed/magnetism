/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_PERSON_H__
#define __HIPPO_PERSON_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoPerson      HippoPerson;
typedef struct _HippoPersonClass HippoPersonClass;

#define HIPPO_TYPE_PERSON              (hippo_person_get_type ())
#define HIPPO_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_PERSON, HippoPerson))
#define HIPPO_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_PERSON, HippoPersonClass))
#define HIPPO_IS_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_PERSON))
#define HIPPO_IS_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_PERSON))
#define HIPPO_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_PERSON, HippoPersonClass))

GType            hippo_person_get_type                  (void) G_GNUC_CONST;
HippoPerson*     hippo_person_new                       (const char  *guid);

const char*      hippo_person_get_current_song          (HippoPerson *person);
const char*      hippo_person_get_current_artist        (HippoPerson *person);
gboolean         hippo_person_get_music_playing         (HippoPerson *person);

void      hippo_person_set_current_song          (HippoPerson *person,
                                                  const char  *song);
void      hippo_person_set_current_artist        (HippoPerson *person,
                                                  const char  *artist);
void      hippo_person_set_music_playing         (HippoPerson *person,
                                                  gboolean     is_playing);


G_END_DECLS

#endif /* __HIPPO_PERSON_H__ */
