/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BASE_H__
#define __HIPPO_CANVAS_BASE_H__

/* Canvas item for the purple "base" of the stacker */

#include "hippo-canvas-item.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBase      HippoCanvasBase;
typedef struct _HippoCanvasBaseClass HippoCanvasBaseClass;

#define HIPPO_TYPE_CANVAS_BASE              (hippo_canvas_base_get_type ())
#define HIPPO_CANVAS_BASE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BASE, HippoCanvasBase))
#define HIPPO_CANVAS_BASE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BASE, HippoCanvasBaseClass))
#define HIPPO_IS_CANVAS_BASE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BASE))
#define HIPPO_IS_CANVAS_BASE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BASE))
#define HIPPO_CANVAS_BASE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BASE, HippoCanvasBaseClass))

GType        	 hippo_canvas_base_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_base_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_BASE_H__ */
