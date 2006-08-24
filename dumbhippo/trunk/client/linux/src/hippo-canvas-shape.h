/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_SHAPE_H__
#define __HIPPO_CANVAS_SHAPE_H__

/* A cairo path canvas item. (will be cross-platform once windows has Cairo support) */

#include "hippo-canvas-item.h"
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasShape      HippoCanvasShape;
typedef struct _HippoCanvasShapeClass HippoCanvasShapeClass;

#define HIPPO_TYPE_CANVAS_SHAPE              (hippo_canvas_shape_get_type ())
#define HIPPO_CANVAS_SHAPE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_SHAPE, HippoCanvasShape))
#define HIPPO_CANVAS_SHAPE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_SHAPE, HippoCanvasShapeClass))
#define HIPPO_IS_CANVAS_SHAPE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_SHAPE))
#define HIPPO_IS_CANVAS_SHAPE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_SHAPE))
#define HIPPO_CANVAS_SHAPE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_SHAPE, HippoCanvasShapeClass))

GType        	 hippo_canvas_shape_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_shape_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_SHAPE_H__ */
