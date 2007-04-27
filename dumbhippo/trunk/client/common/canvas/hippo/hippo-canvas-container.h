/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_CONTAINER_H__
#define __HIPPO_CANVAS_CONTAINER_H__

/*
 * The HippoCanvasContainer interface is implemented by a HippoCanvasItem that can contain
 * other items. Also read the comment in hippo-canvas-context.h about the difference between
 * the container and the context; some methods you might expect to be container methods in
 * GTK+ are context methods in HippoCanvas.
 *
 * Some methods on HippoCanvasBox should really be in this interface, if you find you need them
 * then feel free to move them here. For example get_children(), remove(), remove_all(), etc.
 */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

#define HIPPO_TYPE_CANVAS_CONTAINER              (hippo_canvas_container_get_type ())
#define HIPPO_CANVAS_CONTAINER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_CONTAINER, HippoCanvasContainer))
#define HIPPO_IS_CANVAS_CONTAINER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_CONTAINER))
#define HIPPO_CANVAS_CONTAINER_GET_IFACE(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_CONTAINER, HippoCanvasContainerIface))

struct _HippoCanvasContainerIface {
    GTypeInterface base_iface;

    gboolean (* get_child_visible) (HippoCanvasContainer        *container,
                                    HippoCanvasItem             *child);
    void     (* set_child_visible) (HippoCanvasContainer        *container,
                                    HippoCanvasItem             *child,
                                    gboolean                     visible);
};

GType            hippo_canvas_container_get_type               (void) G_GNUC_CONST;

gboolean         hippo_canvas_container_get_child_visible      (HippoCanvasContainer        *container,
                                                                HippoCanvasItem             *child);
void             hippo_canvas_container_set_child_visible      (HippoCanvasContainer        *container,
                                                                HippoCanvasItem             *child,
                                                                gboolean                     visible);

G_END_DECLS

#endif /* __HIPPO_CANVAS_CONTAINER_H__ */
