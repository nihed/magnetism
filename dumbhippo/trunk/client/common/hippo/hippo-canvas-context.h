/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_CONTEXT_H__
#define __HIPPO_CANVAS_CONTEXT_H__

/*
 * Canvas context gives an item a way to communicate with its "parent"
 * but in a more controlled fashion than just giving each item a pointer
 * to the canvas widget and parent item. Setting a context is sort
 * of like the GTK concept of "realization" - it does not necessarily
 * map to the item being in a container, and in fact the root item
 * has a context but would not have a parent item, for example.
 * Also an item can be in a container item and not have a context if the
 * container item does not have a context, for example if the tree of
 * items is not in a HippoCanvas widget.
 */

#include <hippo/hippo-basics.h>
#include <pango/pango-layout.h>
#include <cairo.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_STOCK_COLOR_BG_NORMAL,
    HIPPO_STOCK_COLOR_BG_PRELIGHT
} HippoStockColor;

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

    PangoLayout*     (* create_layout)  (HippoCanvasContext  *context);

    cairo_surface_t* (* load_image)     (HippoCanvasContext  *context,
                                         const char          *image_name);

    guint32          (* get_color)      (HippoCanvasContext  *context,
                                         HippoStockColor      color);
};

GType        	 hippo_canvas_context_get_type               (void) G_GNUC_CONST;

PangoLayout*     hippo_canvas_context_create_layout          (HippoCanvasContext *context);

cairo_surface_t* hippo_canvas_context_load_image             (HippoCanvasContext *context,
                                                              const char         *image_name);
guint32          hippo_canvas_context_get_color              (HippoCanvasContext *context,
                                                              HippoStockColor     color);

/* Random utility function that shouldn't really be in this file */
void hippo_cairo_set_source_rgba32       (cairo_t         *cr,
                                          guint32          color);
void hippo_cairo_pattern_add_stop_rgba32 (cairo_pattern_t *pattern,
                                          double           offset,
                                          guint32          color);


G_END_DECLS

#endif /* __HIPPO_CANVAS_CONTEXT_H__ */
