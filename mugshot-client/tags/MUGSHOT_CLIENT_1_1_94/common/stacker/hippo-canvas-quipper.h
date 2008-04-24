/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_QUIPPER_H__
#define __HIPPO_CANVAS_QUIPPER_H__

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasQuipper      HippoCanvasQuipper;
typedef struct _HippoCanvasQuipperClass HippoCanvasQuipperClass;

#define HIPPO_TYPE_CANVAS_QUIPPER              (hippo_canvas_quipper_get_type ())
#define HIPPO_CANVAS_QUIPPER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_QUIPPER, HippoCanvasQuipper))
#define HIPPO_CANVAS_QUIPPER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_QUIPPER, HippoCanvasQuipperClass))
#define HIPPO_IS_CANVAS_QUIPPER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_QUIPPER))
#define HIPPO_IS_CANVAS_QUIPPER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_QUIPPER))
#define HIPPO_CANVAS_QUIPPER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_QUIPPER, HippoCanvasQuipperClass))

GType            hippo_canvas_quipper_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_QUIPPER_H__ */
