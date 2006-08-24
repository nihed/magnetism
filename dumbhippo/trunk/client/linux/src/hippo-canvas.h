/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_H__
#define __HIPPO_CANVAS_H__

/* A widget that contains a root HippoCanvasItem */

#include <gtk/gtkwidget.h>
#include "hippo-canvas-item.h"

G_BEGIN_DECLS

typedef struct _HippoCanvas      HippoCanvas;
typedef struct _HippoCanvasClass HippoCanvasClass;

#define HIPPO_TYPE_CANVAS              (hippo_canvas_get_type ())
#define HIPPO_CANVAS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS, HippoCanvas))
#define HIPPO_CANVAS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS, HippoCanvasClass))
#define HIPPO_IS_CANVAS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS))
#define HIPPO_IS_CANVAS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS))
#define HIPPO_CANVAS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS, HippoCanvasClass))

GType        	 hippo_canvas_get_type               (void) G_GNUC_CONST;

GtkWidget*   hippo_canvas_new      (void);
void         hippo_canvas_set_root (HippoCanvas     *canvas,
                                    HippoCanvasItem *root);

void hippo_canvas_open_test_window(void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_H__ */
