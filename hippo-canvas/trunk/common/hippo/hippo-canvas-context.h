/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_CONTEXT_H__
#define __HIPPO_CANVAS_CONTEXT_H__

/*
 * Setting a context is sort of like the GTK concept of "realization"
 * - it does not necessarily map to the item being in a container, and
 * in fact the root item has a context but would not have a parent
 * container, for example.  Also an item can be in a container item and not
 * have a context if the container item does not have a context, for
 * example if the whole tree of items is not in a HippoCanvas widget.
 * 
 * If for example we were printing a tree of canvas items, we
 * might have to temporarily set the context to a special print
 * context (though this has problems, such as temporarily unrealizing
 * any widget items; so print support may require larger changes than
 * just creating a print context).
 *
 */

#include <hippo/hippo-graphics.h>
#include <pango/pango-layout.h>
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasItem      HippoCanvasItem;
typedef struct _HippoCanvasItemIface HippoCanvasItemIface;

typedef struct _HippoCanvasContainer      HippoCanvasContainer;
typedef struct _HippoCanvasContainerIface HippoCanvasContainerIface;

typedef enum {
    HIPPO_STOCK_COLOR_BG_NORMAL,
    HIPPO_STOCK_COLOR_BG_PRELIGHT
} HippoStockColor;

typedef struct _HippoCanvasContext      HippoCanvasContext;
typedef struct _HippoCanvasContextIface HippoCanvasContextIface;

#define HIPPO_TYPE_CANVAS_CONTEXT              (hippo_canvas_context_get_type ())
#define HIPPO_CANVAS_CONTEXT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CONTEXT, HippoCanvasContext))
#define HIPPO_IS_CANVAS_CONTEXT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CONTEXT))
#define HIPPO_CANVAS_CONTEXT_GET_IFACE(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_CONTEXT, HippoCanvasContextIface))

struct _HippoCanvasContextIface {
    GTypeInterface base_iface;

    PangoLayout*     (* create_layout)  (HippoCanvasContext  *context);

    cairo_surface_t* (* load_image)     (HippoCanvasContext  *context,
                                         const char          *image_name);

    guint32          (* get_color)      (HippoCanvasContext  *context,
                                         HippoStockColor      color);

    void             (* register_widget_item)   (HippoCanvasContext *context,
                                                 HippoCanvasItem    *item);
    void             (* unregister_widget_item) (HippoCanvasContext *context,
                                                 HippoCanvasItem    *item);    
    void             (* translate_to_widget)    (HippoCanvasContext *context,
                                                 HippoCanvasItem    *item,
                                                 int                *x_p,
                                                 int                *y_p);
    void             (* translate_to_screen)    (HippoCanvasContext *context,
                                                 HippoCanvasItem    *item,
                                                 int                *x_p,
                                                 int                *y_p);

    /* Style methods (should probably be on a separate interface eventually) */
    void             (* affect_color)           (HippoCanvasContext   *context,
                                                 guint32              *color_rgba_p);
    void             (* affect_font_desc)       (HippoCanvasContext   *context,
                                                 PangoFontDescription *font_desc);
    
    /* Signals */

    /* Inherited style properties (see affect_* methods) have changed.
     * resize_needed means the change needs a resize not just repaint.
     */
    void             (* style_changed)    (HippoCanvasContext *context,
                                           gboolean            resize_needed);
};

GType            hippo_canvas_context_get_type               (void) G_GNUC_CONST;

PangoLayout*     hippo_canvas_context_create_layout          (HippoCanvasContext *context);

cairo_surface_t* hippo_canvas_context_load_image             (HippoCanvasContext *context,
                                                              const char         *image_name);
guint32          hippo_canvas_context_get_color              (HippoCanvasContext *context,
                                                              HippoStockColor     color);

void hippo_canvas_context_register_widget_item   (HippoCanvasContext *context,
                                                  HippoCanvasItem    *item);
void hippo_canvas_context_unregister_widget_item (HippoCanvasContext *context,
                                                  HippoCanvasItem    *item);
void hippo_canvas_context_translate_to_widget    (HippoCanvasContext *context,
                                                  HippoCanvasItem    *item,
                                                  int                *x_p,
                                                  int                *y_p);
void hippo_canvas_context_translate_to_screen    (HippoCanvasContext *context,
                                                  HippoCanvasItem    *item,
                                                  int                *x_p,
                                                  int                *y_p);

void hippo_canvas_context_affect_color       (HippoCanvasContext   *context,
                                              guint32              *color_rgba_p);
void hippo_canvas_context_affect_font_desc   (HippoCanvasContext   *context,
                                              PangoFontDescription *font_desc);
void hippo_canvas_context_emit_style_changed (HippoCanvasContext   *context,
                                              gboolean              resize_needed);

G_END_DECLS

#endif /* __HIPPO_CANVAS_CONTEXT_H__ */
