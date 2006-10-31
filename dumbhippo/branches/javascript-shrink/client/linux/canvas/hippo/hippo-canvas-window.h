/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_WINDOW_H__
#define __HIPPO_CANVAS_WINDOW_H__

/* A window that contains only a root HippoCanvasItem */

#include <gtk/gtkwidget.h>
#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasWindow      HippoCanvasWindow;
typedef struct _HippoCanvasWindowClass HippoCanvasWindowClass;

#define HIPPO_TYPE_CANVAS_WINDOW              (hippo_canvas_window_get_type ())
#define HIPPO_CANVAS_WINDOW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_WINDOW, HippoCanvasWindow))
#define HIPPO_CANVAS_WINDOW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_WINDOW, HippoCanvasWindowClass))
#define HIPPO_IS_CANVAS_WINDOW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_WINDOW))
#define HIPPO_IS_CANVAS_WINDOW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_WINDOW))
#define HIPPO_CANVAS_WINDOW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_WINDOW, HippoCanvasWindowClass))

GType        	 hippo_canvas_window_get_type               (void) G_GNUC_CONST;

GtkWidget* hippo_canvas_window_new    (void);

void hippo_canvas_window_set_root(HippoCanvasWindow *canvas_window,
                                  HippoCanvasItem   *item);


G_END_DECLS

#endif /* __HIPPO_CANVAS_WINDOW_H__ */
