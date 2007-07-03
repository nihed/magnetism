/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_LAYOUT_H__
#define __HIPPO_CANVAS_LAYOUT_H__

#include <hippo/hippo-canvas-box.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasLayoutIface HippoCanvasLayoutIface;

#define HIPPO_TYPE_CANVAS_LAYOUT              (hippo_canvas_layout_get_type ())
#define HIPPO_CANVAS_LAYOUT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_LAYOUT, HippoCanvasLayout))
#define HIPPO_IS_CANVAS_LAYOUT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_LAYOUT))
#define HIPPO_CANVAS_LAYOUT_GET_IFACE(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_LAYOUT, HippoCanvasLayoutIface))

/**
 * HippoCanvasLayout:
 * 
 * The #HippoCanvasLayout interface represents a custom layout strategy for
 * the contents of a #HippoCanvasBox. The implementor is responsible for
 * implementing the three basic operations get_width_request, get_height_request,
 * and allocate. In each case, the layout manager should loop over all children
 * returned by hippo_canvas_box_get_layout_children(), and call the corresponding
 * method on #HippoCanvasBoxChild for each child. (You must do this, even
 * if you aren't going to use the returned values to compute the returned values
 * from get_width_request or get_height_request.)
 *
 * The widths, heights, and  positions referenced in the method parameters are the
 * widths, heights, and positions of the content area, after padding, border,
 * and alignment have already been dealt with.
 *
 * The box that the layout manager acts upon is passed to set_box when the user
 * calls hippo_canvas_box_set_layout(). set_box is called passing in %NULL when
 * the box is destroyed or a different layout manager set.
 *
 * The implementor should store the box for future reference, but does not need
 * to add an explicit reference. (If you do add an explicit reference, then there
 * is some danger of a memory leak if the box is unreferenced without being
 * explicitely destroyed with hippo_canvas_item_destroy().)
 */
struct _HippoCanvasLayoutIface {
    GTypeInterface base_iface;

    void (* set_box)            (HippoCanvasLayout  *layout,
                                 HippoCanvasBox     *box);
    void (* get_width_request)  (HippoCanvasLayout  *layout,
                                 int                *min_width_p,
                                 int                *natural_width_p);
    void (* get_height_request) (HippoCanvasLayout  *layout,
                                 int                 for_width,
                                 int                *min_height_p,
                                 int                *natural_height_p);
    void (* allocate)           (HippoCanvasLayout  *layout,
                                 int                 x,
                                 int                 y,
                                 int                 width,
                                 int                 height,
                                 int                 requested_width,
                                 int                 requested_height,
                                 gboolean            origin_changed);
};

GType hippo_canvas_layout_get_type(void);

void hippo_canvas_layout_set_box            (HippoCanvasLayout *layout,
                                             HippoCanvasBox    *box);
void hippo_canvas_layout_get_width_request  (HippoCanvasLayout *layout,
                                             int               *min_width_p,
                                             int               *natural_width_p);
void hippo_canvas_layout_get_height_request (HippoCanvasLayout *layout,
                                             int                for_width,
                                             int               *min_height_p,
                                             int               *natural_height_p);
void hippo_canvas_layout_allocate           (HippoCanvasLayout *layout,
                                             int                x,
                                             int                y,
                                             int                width,
                                             int                height,
                                             int                requested_width,
                                             int                requested_height,
                                             gboolean           origin_changed);

G_END_DECLS

#endif /* __HIPPO_CANVAS_LAYOUT_H__ */
