/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_MUSIC_H__
#define __HIPPO_CANVAS_BLOCK_MUSIC_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>
#include <hippo/hippo-track.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockMusic      HippoCanvasBlockMusic;
typedef struct _HippoCanvasBlockMusicClass HippoCanvasBlockMusicClass;

#define HIPPO_TYPE_CANVAS_BLOCK_MUSIC              (hippo_canvas_block_music_get_type ())
#define HIPPO_CANVAS_BLOCK_MUSIC(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_MUSIC, HippoCanvasBlockMusic))
#define HIPPO_CANVAS_BLOCK_MUSIC_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_MUSIC, HippoCanvasBlockMusicClass))
#define HIPPO_IS_CANVAS_BLOCK_MUSIC(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_MUSIC))
#define HIPPO_IS_CANVAS_BLOCK_MUSIC_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_MUSIC))
#define HIPPO_CANVAS_BLOCK_MUSIC_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_MUSIC, HippoCanvasBlockMusicClass))

GType            hippo_canvas_block_music_get_type    (void) G_GNUC_CONST;

void hippo_canvas_block_music_set_track(HippoCanvasBlockMusic *block_music_person,
                                        HippoTrack            *track);

struct _HippoCanvasBlockMusic {
    HippoCanvasBlock canvas_block;
    HippoPerson *person;
    HippoTrack *track;
    HippoCanvasBox *downloads_box;
};

struct _HippoCanvasBlockMusicClass {
    HippoCanvasBlockClass parent_class;

};

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_MUSIC_H__ */
