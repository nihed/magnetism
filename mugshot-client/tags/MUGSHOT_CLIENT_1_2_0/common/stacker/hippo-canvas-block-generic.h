/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_GENERIC_H__
#define __HIPPO_CANVAS_BLOCK_GENERIC_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include "hippo-block.h"

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockGeneric      HippoCanvasBlockGeneric;
typedef struct _HippoCanvasBlockGenericClass HippoCanvasBlockGenericClass;

#define HIPPO_TYPE_CANVAS_BLOCK_GENERIC              (hippo_canvas_block_generic_get_type ())
#define HIPPO_CANVAS_BLOCK_GENERIC(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_GENERIC, HippoCanvasBlockGeneric))
#define HIPPO_CANVAS_BLOCK_GENERIC_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_GENERIC, HippoCanvasBlockGenericClass))
#define HIPPO_IS_CANVAS_BLOCK_GENERIC(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_GENERIC))
#define HIPPO_IS_CANVAS_BLOCK_GENERIC_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_GENERIC))
#define HIPPO_CANVAS_BLOCK_GENERIC_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_GENERIC, HippoCanvasBlockGenericClass))

struct _HippoCanvasBlockGeneric {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *description_parent;
    HippoCanvasItem *description_item;
    HippoCanvasItem *reason_item;
    HippoCanvasItem *expand_tip;
    HippoCanvasItem *quipper;
    HippoCanvasItem *last_message_preview;
    HippoCanvasItem *chat_preview;
    HippoCanvasItem *thumbnails_item;
    unsigned int have_description : 1;
    unsigned int have_thumbnails : 1;
};

struct _HippoCanvasBlockGenericClass {
    HippoCanvasBlockClass parent_class;
};

GType            hippo_canvas_block_generic_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_GENERIC_H__ */
