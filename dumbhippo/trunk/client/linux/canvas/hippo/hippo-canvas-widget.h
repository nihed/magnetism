/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_WIDGET_H__
#define __HIPPO_CANVAS_WIDGET_H__

/* A canvas item that holds a spot in the canvas for a widget */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-canvas-box.h>
#include <cairo.h>
#include <gtk/gtkwidget.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasWidget      HippoCanvasWidget;
typedef struct _HippoCanvasWidgetClass HippoCanvasWidgetClass;

#define HIPPO_TYPE_CANVAS_WIDGET              (hippo_canvas_widget_get_type ())
#define HIPPO_CANVAS_WIDGET(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_WIDGET, HippoCanvasWidget))
#define HIPPO_CANVAS_WIDGET_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_WIDGET, HippoCanvasWidgetClass))
#define HIPPO_IS_CANVAS_WIDGET(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_WIDGET))
#define HIPPO_IS_CANVAS_WIDGET_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_WIDGET))
#define HIPPO_CANVAS_WIDGET_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_WIDGET, HippoCanvasWidgetClass))

struct _HippoCanvasWidget {
    HippoCanvasBox box;
    GtkWidget *widget;
};

struct _HippoCanvasWidgetClass {
    HippoCanvasBoxClass parent_class;
};

GType        	 hippo_canvas_widget_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_widget_new    (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_WIDGET_H__ */
