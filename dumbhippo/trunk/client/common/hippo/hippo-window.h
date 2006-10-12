/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_WINDOW_H__
#define __HIPPO_WINDOW_H__

/* cross-platform interface to an undecorated toplevel window
 * that can contain a canvas item.
 */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

#define HIPPO_TYPE_WINDOW              (hippo_window_get_type ())
#define HIPPO_WINDOW(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_WINDOW, HippoWindow))
#define HIPPO_WINDOW_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_WINDOW, HippoWindowClass))
#define HIPPO_IS_WINDOW(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_WINDOW))
#define HIPPO_IS_WINDOW_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_WINDOW))
#define HIPPO_WINDOW_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_WINDOW, HippoWindowClass))

struct _HippoWindowClass {
    GTypeInterface base_iface;
    
    void (* set_contents)      (HippoWindow     *window,
                                HippoCanvasItem *item);
    void (* set_visible)       (HippoWindow     *window,
                                gboolean         visible);
    void (* set_position)      (HippoWindow     *window,
                                int              x,
                                int              y);
    void (* set_size)          (HippoWindow     *window,
                                int              width,
                                int              height);
    void (* get_position)      (HippoWindow     *window,
                                int             *x_p,
                                int             *y_p);
    void (* get_size)          (HippoWindow     *window,
                                int             *width_p,
                                int             *height_p);
    void (* set_resizable)     (HippoWindow     *window,
                                HippoOrientation orientation,
                                gboolean         visible);
    void (* begin_move_drag)   (HippoWindow      *window,
                                HippoEvent       *event);
    void (* begin_resize_drag) (HippoWindow      *window,
                                HippoSide         side,
                                HippoEvent       *event);
    
};

GType            hippo_window_get_type               (void) G_GNUC_CONST;


void hippo_window_set_contents      (HippoWindow      *window,
                                     HippoCanvasItem  *item);
void hippo_window_set_visible       (HippoWindow      *window,
                                     gboolean          visible);
void hippo_window_set_position      (HippoWindow      *window,
                                     int               x,
                                     int               y);
void hippo_window_set_size          (HippoWindow      *window,
                                     int               width,
                                     int               height);
void hippo_window_get_position      (HippoWindow      *window,
                                     int              *x_p,
                                     int              *y_p);
void hippo_window_get_size          (HippoWindow      *window,
                                     int              *width_p,
                                     int              *height_p);
void hippo_window_set_resizable     (HippoWindow      *window,
                                     HippoOrientation  orientation,
                                     gboolean          value);
void hippo_window_begin_move_drag   (HippoWindow      *window,
                                     HippoEvent       *event);
void hippo_window_begin_resize_drag (HippoWindow      *window,
                                     HippoSide         side,
                                     HippoEvent       *event);

G_END_DECLS

#endif /* __HIPPO_WINDOW_H__ */
