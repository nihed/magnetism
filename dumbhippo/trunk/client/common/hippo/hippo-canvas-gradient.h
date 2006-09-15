/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_GRADIENT_H__
#define __HIPPO_CANVAS_GRADIENT_H__

/* A canvas item that renders a gradient */

#include "hippo-canvas-item.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasGradient      HippoCanvasGradient;
typedef struct _HippoCanvasGradientClass HippoCanvasGradientClass;

#define HIPPO_TYPE_CANVAS_GRADIENT              (hippo_canvas_gradient_get_type ())
#define HIPPO_CANVAS_GRADIENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_GRADIENT, HippoCanvasGradient))
#define HIPPO_CANVAS_GRADIENT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_GRADIENT, HippoCanvasGradientClass))
#define HIPPO_IS_CANVAS_GRADIENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_GRADIENT))
#define HIPPO_IS_CANVAS_GRADIENT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_GRADIENT))
#define HIPPO_CANVAS_GRADIENT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_GRADIENT, HippoCanvasGradientClass))

GType        	 hippo_canvas_gradient_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_gradient_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_GRADIENT_H__ */
