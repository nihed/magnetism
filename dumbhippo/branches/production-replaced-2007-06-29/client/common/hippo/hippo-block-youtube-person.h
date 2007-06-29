/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_YOUTUBE_PERSON_H__
#define __HIPPO_BLOCK_YOUTUBE_PERSON_H__

#include <hippo/hippo-block.h>
#include <hippo/hippo-thumbnails.h>

G_BEGIN_DECLS

typedef struct _HippoBlockYouTubePerson      HippoBlockYouTubePerson;
typedef struct _HippoBlockYouTubePersonClass HippoBlockYouTubePersonClass;


#define HIPPO_TYPE_BLOCK_YOUTUBE_PERSON              (hippo_block_youtube_person_get_type ())
#define HIPPO_BLOCK_YOUTUBE_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_YOUTUBE_PERSON, HippoBlockYouTubePerson))
#define HIPPO_BLOCK_YOUTUBE_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_YOUTUBE_PERSON, HippoBlockYouTubePersonClass))
#define HIPPO_IS_BLOCK_YOUTUBE_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_YOUTUBE_PERSON))
#define HIPPO_IS_BLOCK_YOUTUBE_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_YOUTUBE_PERSON))
#define HIPPO_BLOCK_YOUTUBE_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_YOUTUBE_PERSON, HippoBlockYouTubePersonClass))

GType            hippo_block_youtube_person_get_type               (void) G_GNUC_CONST;

HippoThumbnails* hippo_block_youtube_person_get_thumbnails  (HippoBlockYouTubePerson *block_youtube_person);

G_END_DECLS

#endif /* __HIPPO_BLOCK_YOUTUBE_PERSON_H__ */
