/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BOX_H__
#define __HIPPO_CANVAS_BOX_H__

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef enum
{
    HIPPO_ORIENTATION_VERTICAL,
    HIPPO_ORIENTATION_HORIZONTAL
} HippoOrientation;

typedef enum
{
    HIPPO_PACK_EXPAND = 1,
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

GType        	 hippo_canvas_box_get_type               (void) G_GNUC_CONST;

HippoCanvasBox* hippo_canvas_box_new    (void);

void            hippo_canvas_box_append (HippoCanvasBox  *box,
                                         HippoCanvasItem *child,
                                         HippoPackFlags   flags);
void            hippo_canvas_box_remove (HippoCanvasBox  *box,
                                         HippoCanvasItem *child);




G_END_DECLS

#endif /* __HIPPO_CANVAS_BOX_H__ */
