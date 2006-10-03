/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_MUSIC_PERSON_H__
#define __HIPPO_CANVAS_BLOCK_MUSIC_PERSON_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockMusicPerson      HippoCanvasBlockMusicPerson;
typedef struct _HippoCanvasBlockMusicPersonClass HippoCanvasBlockMusicPersonClass;

#define HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON              (hippo_canvas_block_music_person_get_type ())
#define HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON, HippoCanvasBlockMusicPerson))
#define HIPPO_CANVAS_BLOCK_MUSIC_PERSON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON, HippoCanvasBlockMusicPersonClass))
#define HIPPO_IS_CANVAS_BLOCK_MUSIC_PERSON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON))
#define HIPPO_IS_CANVAS_BLOCK_MUSIC_PERSON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON))
#define HIPPO_CANVAS_BLOCK_MUSIC_PERSON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_MUSIC_PERSON, HippoCanvasBlockMusicPersonClass))

GType            hippo_canvas_block_music_person_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_music_person_new         (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_MUSIC_PERSON_H__ */
