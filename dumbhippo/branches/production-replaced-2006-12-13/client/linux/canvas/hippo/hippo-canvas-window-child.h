/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_WINDOW_CHILD_H__
#define __HIPPO_CANVAS_WINDOW_CHILD_H__

/* Internal, non-exposed child for HoppoCanvasWindow */

#include <gtk/gtkwidget.h>
#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-helper.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasWindowChild      HippoCanvasWindowChild;
typedef struct _HippoCanvasWindowChildClass HippoCanvasWindowChildClass;

#define HIPPO_TYPE_CANVAS_WINDOW_CHILD              (hippo_canvas_window_child_get_type ())
#define HIPPO_CANVAS_WINDOW_CHILD(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_WINDOW_CHILD, HippoCanvasWindowChild))
#define HIPPO_CANVAS_WINDOW_CHILD_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_WINDOW_CHILD, HippoCanvasWindowChildClass))
#define HIPPO_IS_CANVAS_WINDOW_CHILD(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_WINDOW_CHILD))
#define HIPPO_IS_CANVAS_WINDOW_CHILD_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_WINDOW_CHILD))
#define HIPPO_CANVAS_WINDOW_CHILD_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_WINDOW_CHILD, HippoCanvasWindowChildClass))

GType     	 hippo_canvas_window_child_get_type               (void) G_GNUC_CONST;

GtkWidget* hippo_canvas_window_child_new(void);

HippoCanvasHelper *hippo_canvas_window_child_get_helper(HippoCanvasWindowChild *window_child);

G_END_DECLS

#endif /* __HIPPO_CANVAS_WINDOW_CHILD_H__ */
