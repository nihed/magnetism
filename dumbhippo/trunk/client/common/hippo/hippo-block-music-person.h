/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_MUSIC_MEMBER_H__
#define __HIPPO_BLOCK_MUSIC_PERSON_H__

#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoBlockMusicPerson      HippoBlockMusicPerson;
typedef struct _HippoBlockMusicPersonClass HippoBlockMusicPersonClass;


#define HIPPO_TYPE_BLOCK_MUSIC_PERSON              (hippo_block_music_person_get_type ())
#define HIPPO_BLOCK_MUSIC_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_MUSIC_PERSON, HippoBlockMusicPerson))
#define HIPPO_BLOCK_MUSIC_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_MUSIC_PERSON, HippoBlockMusicPersonClass))
#define HIPPO_IS_BLOCK_MUSIC_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_MUSIC_PERSON))
#define HIPPO_IS_BLOCK_MUSIC_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_MUSIC_PERSON))
#define HIPPO_BLOCK_MUSIC_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_MUSIC_PERSON, HippoBlockMusicPersonClass))

GType            hippo_block_music_person_get_type               (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_BLOCK_MUSIC_PERSON_H__ */
