/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_TEXT_H__
#define __HIPPO_CANVAS_TEXT_H__

/* A cairo path canvas item. (will be cross-platform once windows has Cairo support) */

#include <hippo/hippo-canvas-item.h>
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasText      HippoCanvasText;
typedef struct _HippoCanvasTextClass HippoCanvasTextClass;

#define HIPPO_TYPE_CANVAS_TEXT              (hippo_canvas_text_get_type ())
#define HIPPO_CANVAS_TEXT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_TEXT, HippoCanvasText))
#define HIPPO_CANVAS_TEXT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_TEXT, HippoCanvasTextClass))
#define HIPPO_IS_CANVAS_TEXT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_TEXT))
#define HIPPO_IS_CANVAS_TEXT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_TEXT))
#define HIPPO_CANVAS_TEXT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_TEXT, HippoCanvasTextClass))

GType        	 hippo_canvas_text_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_text_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_TEXT_H__ */
