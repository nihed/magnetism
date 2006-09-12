/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_GRIP_H__
#define __HIPPO_CANVAS_GRIP_H__

/* A canvas item that renders a resize grip */

#include "hippo-canvas-item.h"
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasGrip      HippoCanvasGrip;
typedef struct _HippoCanvasGripClass HippoCanvasGripClass;

#define HIPPO_TYPE_CANVAS_GRIP              (hippo_canvas_grip_get_type ())
#define HIPPO_CANVAS_GRIP(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_GRIP, HippoCanvasGrip))
#define HIPPO_CANVAS_GRIP_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_GRIP, HippoCanvasGripClass))
#define HIPPO_IS_CANVAS_GRIP(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_GRIP))
#define HIPPO_IS_CANVAS_GRIP_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_GRIP))
#define HIPPO_CANVAS_GRIP_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_GRIP, HippoCanvasGripClass))

GType        	 hippo_canvas_grip_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_grip_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_GRIP_H__ */
