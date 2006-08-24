/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_CONTEXT_H__
#define __HIPPO_CANVAS_CONTEXT_H__

/*
 * Canvas context gives an item a way to communicate with its "parent"
 * but in a more controlled fashion than just giving each item a pointer
 * to the canvas widget and parent item.
 */

#include <hippo/hippo-basics.h>
#include <pango/pango-layout.h>
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasContext      HippoCanvasContext;
typedef struct _HippoCanvasContextClass HippoCanvasContextClass;

#define HIPPO_TYPE_CANVAS_CONTEXT              (hippo_canvas_context_get_type ())
#define HIPPO_CANVAS_CONTEXT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CONTEXT, HippoCanvasContext))
#define HIPPO_CANVAS_CONTEXT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_CONTEXT, HippoCanvasContextClass))
#define HIPPO_IS_CANVAS_CONTEXT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CONTEXT))
#define HIPPO_IS_CANVAS_CONTEXT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_CONTEXT))
#define HIPPO_CANVAS_CONTEXT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_CONTEXT, HippoCanvasContextClass))

struct _HippoCanvasContextClass {
    GTypeInterface base_iface;

    PangoLayout* (* create_layout)  (HippoCanvasContext  *context);
};

GType        	 hippo_canvas_context_get_type               (void) G_GNUC_CONST;

PangoLayout*     hippo_canvas_context_create_layout          (HippoCanvasContext *context);


G_END_DECLS

#endif /* __HIPPO_CANVAS_CONTEXT_H__ */
