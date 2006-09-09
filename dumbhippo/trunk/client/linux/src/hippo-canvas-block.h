/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_H__
#define __HIPPO_CANVAS_BLOCK_H__

/* A canvas item that displays a stacker block */

#include "hippo-canvas-item.h"
#include "hippo-canvas-box.h"
#include <cairo/cairo.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlock      HippoCanvasBlock;
typedef struct _HippoCanvasBlockClass HippoCanvasBlockClass;

#define HIPPO_TYPE_CANVAS_BLOCK              (hippo_canvas_block_get_type ())
#define HIPPO_CANVAS_BLOCK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlock))
#define HIPPO_CANVAS_BLOCK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlockClass))
#define HIPPO_IS_CANVAS_BLOCK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK))
#define HIPPO_IS_CANVAS_BLOCK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK))
#define HIPPO_CANVAS_BLOCK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK, HippoCanvasBlockClass))

struct _HippoCanvasBlock {
    HippoCanvasBox box;
    HippoBlockType required_type;
    HippoBlock *block;
    HippoCanvasItem *age_item;
};

struct _HippoCanvasBlockClass {
    HippoCanvasBoxClass parent_class;

};

GType        	 hippo_canvas_block_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_block_new         (HippoBlockType type);

void             hippo_canvas_block_set_block   (HippoCanvasBlock *canvas_block,
                                                 HippoBlock       *block);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_H__ */
