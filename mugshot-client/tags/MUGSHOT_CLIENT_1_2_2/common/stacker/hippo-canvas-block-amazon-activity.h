/* -* C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY_H__
#define __HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockAmazonActivity      HippoCanvasBlockAmazonActivity;
typedef struct _HippoCanvasBlockAmazonActivityClass HippoCanvasBlockAmazonActivityClass;

#define HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY              (hippo_canvas_block_amazon_activity_get_type ())
#define HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY, HippoCanvasBlockAmazonActivity))
#define HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY, HippoCanvasBlockAmazonActivityClass))
#define HIPPO_IS_CANVAS_BLOCK_AMAZON_ACTIVITY(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY))
#define HIPPO_IS_CANVAS_BLOCK_AMAZON_ACTIVITY_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY))
#define HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_AMAZON_ACTIVITY, HippoCanvasBlockAmazonActivityClass))

GType            hippo_canvas_block_amazon_activity_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_AMAZON_ACTIVITY_H__ */
