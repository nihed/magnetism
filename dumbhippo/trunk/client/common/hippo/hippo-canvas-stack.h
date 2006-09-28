/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_STACK_H__
#define __HIPPO_CANVAS_STACK_H__

/* canvas item for a stack of blocks */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasStack      HippoCanvasStack;
typedef struct _HippoCanvasStackClass HippoCanvasStackClass;

#define HIPPO_TYPE_CANVAS_STACK              (hippo_canvas_stack_get_type ())
#define HIPPO_CANVAS_STACK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_STACK, HippoCanvasStack))
#define HIPPO_CANVAS_STACK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_STACK, HippoCanvasStackClass))
#define HIPPO_IS_CANVAS_STACK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_STACK))
#define HIPPO_IS_CANVAS_STACK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_STACK))
#define HIPPO_CANVAS_STACK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_STACK, HippoCanvasStackClass))

GType        	 hippo_canvas_stack_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_stack_new    (void);

void hippo_canvas_stack_add_block   (HippoCanvasStack *canvas_stack,
                                     HippoBlock       *block);

void hippo_canvas_stack_remove_block(HippoCanvasStack *canvas_stack,
                                     HippoBlock       *block);

G_END_DECLS

#endif /* __HIPPO_CANVAS_STACK_H__ */
