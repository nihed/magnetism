/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BOX_H__
#define __HIPPO_CANVAS_BOX_H__

#include "hippo-canvas-item.h"

G_BEGIN_DECLS

typedef enum
{
    HIPPO_PACK_EXPAND = 1, /* This is equivalent to both EXPAND and FILL for GtkBox,
                            * the way you'd get FILL=false is to set the alignment
                            * on the child item
                            */
    HIPPO_PACK_END = 2
} HippoPackFlags;

typedef void (* HippoCanvasForeachChildFunc) (HippoCanvasItem *child,
                                              void            *data);  

typedef struct _HippoCanvasBox      HippoCanvasBox;
typedef struct _HippoCanvasBoxClass HippoCanvasBoxClass;

#define HIPPO_TYPE_CANVAS_BOX              (hippo_canvas_box_get_type ())
#define HIPPO_CANVAS_BOX(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBox))
#define HIPPO_CANVAS_BOX_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBoxClass))
#define HIPPO_IS_CANVAS_BOX(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BOX))
#define HIPPO_IS_CANVAS_BOX_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BOX))
#define HIPPO_CANVAS_BOX_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BOX, HippoCanvasBoxClass))

struct _HippoCanvasBox {
    GObject parent;
    HippoCanvasContext *context;
    GSList *children;
    int allocated_width;
    int allocated_height;

    /* these are -1 if unset, which means use natural size request */
    int box_width;
    int box_height;

    guint32 background_color_rgba;
    guint32 border_color_rgba;

    /* padding is empty space around all children with the
     * background color
     */
    guint8 padding_top;
    guint8 padding_bottom;
    guint8 padding_left;
    guint8 padding_right;

    /* padding is empty space around the padding, with
     * the border color
     */
    guint8 border_top;
    guint8 border_bottom;
    guint8 border_left;
    guint8 border_right;
    
    guint8 spacing;

    guint floating : 1;
    guint request_changed_since_allocate : 1;
    guint orientation : 2; /* enum only has 2 values so it fits with extra */
    guint x_align : 3;     /* enum only has 4 values so it fits with extra */
    guint y_align : 3;     /* enum only has 4 values so it fits with extra */
    guint clickable : 1;   /* show a hand pointer */
};

struct _HippoCanvasBoxClass {
    GObjectClass parent_class;

    void     (* paint_background)             (HippoCanvasBox   *box,
                                               cairo_t          *cr);
    void     (* paint_children)               (HippoCanvasBox   *box,
                                               cairo_t          *cr);
    void     (* paint_below_children)         (HippoCanvasBox   *box,
                                               cairo_t          *cr);    
    void     (* paint_above_children)         (HippoCanvasBox   *box,
                                               cairo_t          *cr);
    
    int      (* get_content_width_request)    (HippoCanvasBox   *box);
    int      (* get_content_height_request)   (HippoCanvasBox   *box,
                                               int               for_width);
};


GType        	 hippo_canvas_box_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_box_new    (void);

void hippo_canvas_box_append     (HippoCanvasBox  *box,
                                  HippoCanvasItem *child,
                                  HippoPackFlags   flags);
void hippo_canvas_box_remove     (HippoCanvasBox  *box,
                                  HippoCanvasItem *child);
void hippo_canvas_box_remove_all (HippoCanvasBox  *box);
void hippo_canvas_box_foreach    (HippoCanvasBox  *box,
                                  HippoCanvasForeachChildFunc func,
                                  void            *data);

/* Protected accessors for subclasses */
HippoCanvasContext* hippo_canvas_box_get_context     (HippoCanvasBox *box);
void                hippo_canvas_box_align           (HippoCanvasBox *box,
                                                      int             requested_content_width,
                                                      int             requested_content_height,
                                                      int            *x_p,
                                                      int            *y_p,
                                                      int            *width_p,
                                                      int            *height_p);

G_END_DECLS

#endif /* __HIPPO_CANVAS_BOX_H__ */
