/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BOX_H__
#define __HIPPO_CANVAS_BOX_H__

#include "hippo-canvas-item.h"

G_BEGIN_DECLS

typedef enum
{
    HIPPO_ORIENTATION_VERTICAL,
    HIPPO_ORIENTATION_HORIZONTAL
} HippoOrientation;

typedef enum
{
    HIPPO_PACK_EXPAND = 1, /* This is equivalent to both EXPAND and FILL for GtkBox,
                            * the way you'd get FILL=false is to have a child item
                            * like GtkMisc or with its own "align" property.
                            */
    HIPPO_PACK_END = 2
} HippoPackFlags;

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
    HippoOrientation orientation;
    GSList *children;
    int allocated_x;
    int allocated_y;
    int allocated_width;
    int allocated_height;

    int forced_width; /* -1 if unset, use "natural" */

    guint8 padding_top;
    guint8 padding_bottom;
    guint8 padding_left;
    guint8 padding_right;

    guint request_changed_since_allocate : 1;
};

struct _HippoCanvasBoxClass {
    GObjectClass parent_class;

};


GType        	 hippo_canvas_box_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_box_new    (void);

void            hippo_canvas_box_append (HippoCanvasBox  *box,
                                         HippoCanvasItem *child,
                                         HippoPackFlags   flags);
void            hippo_canvas_box_remove (HippoCanvasBox  *box,
                                         HippoCanvasItem *child);




G_END_DECLS

#endif /* __HIPPO_CANVAS_BOX_H__ */
