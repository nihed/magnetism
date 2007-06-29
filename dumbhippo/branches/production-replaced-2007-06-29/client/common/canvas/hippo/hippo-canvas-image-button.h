/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_IMAGE_BUTTON_H__
#define __HIPPO_CANVAS_IMAGE_BUTTON_H__

/* A canvas item for image buttons (supports a prelight image) */

#include <hippo/hippo-canvas-image.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasImageButton      HippoCanvasImageButton;
typedef struct _HippoCanvasImageButtonClass HippoCanvasImageButtonClass;

#define HIPPO_TYPE_CANVAS_IMAGE_BUTTON              (hippo_canvas_image_button_get_type ())
#define HIPPO_CANVAS_IMAGE_BUTTON(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_IMAGE_BUTTON, HippoCanvasImageButton))
#define HIPPO_CANVAS_IMAGE_BUTTON_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_IMAGE_BUTTON, HippoCanvasImageButtonClass))
#define HIPPO_IS_CANVAS_IMAGE_BUTTON(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_IMAGE_BUTTON))
#define HIPPO_IS_CANVAS_IMAGE_BUTTON_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_IMAGE_BUTTON))
#define HIPPO_CANVAS_IMAGE_BUTTON_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_IMAGE_BUTTON, HippoCanvasImageButtonClass))

GType            hippo_canvas_image_button_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_image_button_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_IMAGE_BUTTON_H__ */
