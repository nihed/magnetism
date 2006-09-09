/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_POST_H__
#define __HIPPO_CANVAS_BLOCK_POST_H__

/* A canvas item that displays a stacker block */

#include "hippo-canvas-item.h"
#include <cairo/cairo.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockPost      HippoCanvasBlockPost;
typedef struct _HippoCanvasBlockPostClass HippoCanvasBlockPostClass;

#define HIPPO_TYPE_CANVAS_BLOCK_POST              (hippo_canvas_block_post_get_type ())
#define HIPPO_CANVAS_BLOCK_POST(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_POST, HippoCanvasBlockPost))
#define HIPPO_CANVAS_BLOCK_POST_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_POST, HippoCanvasBlockPostClass))
#define HIPPO_IS_CANVAS_BLOCK_POST(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_POST))
#define HIPPO_IS_CANVAS_BLOCK_POST_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_POST))
#define HIPPO_CANVAS_BLOCK_POST_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_POST, HippoCanvasBlockPostClass))

GType        	 hippo_canvas_block_post_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_post_new         (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_POST_H__ */
