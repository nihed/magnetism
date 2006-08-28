/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_IMAGE_H__
#define __HIPPO_CANVAS_IMAGE_H__

/* A canvas item that renders a Cairo image surface */

#include "hippo-canvas-item.h"
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasImage      HippoCanvasImage;
typedef struct _HippoCanvasImageClass HippoCanvasImageClass;

#define HIPPO_TYPE_CANVAS_IMAGE              (hippo_canvas_image_get_type ())
#define HIPPO_CANVAS_IMAGE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_IMAGE, HippoCanvasImage))
#define HIPPO_CANVAS_IMAGE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_IMAGE, HippoCanvasImageClass))
#define HIPPO_IS_CANVAS_IMAGE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_IMAGE))
#define HIPPO_IS_CANVAS_IMAGE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_IMAGE))
#define HIPPO_CANVAS_IMAGE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_IMAGE, HippoCanvasImageClass))

GType        	 hippo_canvas_image_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_image_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_IMAGE_H__ */
