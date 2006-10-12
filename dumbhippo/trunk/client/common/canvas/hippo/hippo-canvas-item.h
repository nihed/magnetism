/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_ITEM_H__
#define __HIPPO_CANVAS_ITEM_H__

#include <cairo.h>
#include <hippo/hippo-event.h>
#include <hippo/hippo-graphics.h>
#include <hippo/hippo-canvas-context.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_CANVAS_POINTER_UNSET,
    HIPPO_CANVAS_POINTER_DEFAULT,
    HIPPO_CANVAS_POINTER_HAND
} HippoCanvasPointer;

/* How an item deals with extra allocation in a single (x or y) dimension */
typedef enum {
    HIPPO_ALIGNMENT_FILL,
    HIPPO_ALIGNMENT_START, /* left or top */
    HIPPO_ALIGNMENT_CENTER,
    HIPPO_ALIGNMENT_END    /* right or bottom */
} HippoItemAlignment;

#define HIPPO_TYPE_CANVAS_ITEM              (hippo_canvas_item_get_type ())
#define HIPPO_CANVAS_ITEM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_ITEM, HippoCanvasItem))
#define HIPPO_IS_CANVAS_ITEM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_ITEM))
#define HIPPO_CANVAS_ITEM_GET_IFACE(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_ITEM, HippoCanvasItemIface))

struct _HippoCanvasItemIface {
    GTypeInterface base_iface;

    void     (* sink)               (HippoCanvasItem  *canvas_item);
    
    void     (* set_context)        (HippoCanvasItem    *canvas_item,
                                     HippoCanvasContext *context);
    void     (* paint)              (HippoCanvasItem  *canvas_item,
                                     cairo_t          *cr,
                                     HippoRectangle   *damaged_box);
    int      (* get_width_request)  (HippoCanvasItem *canvas_item);
    int      (* get_natural_width)  (HippoCanvasItem *canvas_item);
    int      (* get_height_request) (HippoCanvasItem *canvas_item,
                                     int              for_width);
    void     (* allocate)           (HippoCanvasItem *canvas_item,
                                     int              width,
                                     int              height);
    void     (* get_allocation)     (HippoCanvasItem *canvas_item,
                                     int             *width_p,
                                     int             *height_p);
    gboolean (* button_press_event) (HippoCanvasItem *canvas_item,
                                     HippoEvent      *event);
    gboolean (* button_release_event) (HippoCanvasItem *canvas_item,
                                     HippoEvent      *event);
    gboolean (* motion_notify_event)(HippoCanvasItem *canvas_item,
                                     HippoEvent      *event);
    void     (* activated)          (HippoCanvasItem *canvas_item);
    void     (* request_changed)    (HippoCanvasItem *canvas_item);
    void     (* paint_needed)       (HippoCanvasItem *canvas_item,
                                     const HippoRectangle *damage_box);
    gboolean (* get_needs_resize)   (HippoCanvasItem *canvas_item);

    char*              (* get_tooltip) (HippoCanvasItem *canvas_item,
                                        int              x,
                                        int              y,
                                        HippoRectangle  *for_area);
    HippoCanvasPointer (* get_pointer) (HippoCanvasItem *canvas_item,
                                        int              x,
                                        int              y);
};

GType            hippo_canvas_item_get_type               (void) G_GNUC_CONST;

void               hippo_canvas_item_sink               (HippoCanvasItem    *canvas_item);

void               hippo_canvas_item_set_context        (HippoCanvasItem    *canvas_item,
                                                         HippoCanvasContext *context);
int                hippo_canvas_item_get_width_request  (HippoCanvasItem    *canvas_item);
int                hippo_canvas_item_get_natural_width  (HippoCanvasItem    *canvas_item);
int                hippo_canvas_item_get_height_request (HippoCanvasItem    *canvas_item,
                                                         int                 for_width);
void               hippo_canvas_item_allocate           (HippoCanvasItem    *canvas_item,
                                                         int                 width,
                                                         int                 height);
void               hippo_canvas_item_get_allocation     (HippoCanvasItem    *canvas_item,
                                                         int                *width_p,
                                                         int                *height_p);
gboolean           hippo_canvas_item_get_needs_resize   (HippoCanvasItem    *canvas_item);
char*              hippo_canvas_item_get_tooltip        (HippoCanvasItem    *canvas_item,
                                                         int                 x,
                                                         int                 y,
                                                         HippoRectangle     *for_area);
HippoCanvasPointer hippo_canvas_item_get_pointer        (HippoCanvasItem    *canvas_item,
                                                         int                 x,
                                                         int                 y);


void     hippo_canvas_item_get_request             (HippoCanvasItem *canvas_item,
                                                    int             *width_p,
                                                    int             *height_p);
gboolean hippo_canvas_item_emit_button_press_event (HippoCanvasItem *canvas_item,
                                                    int              x,
                                                    int              y,
                                                    int              button,
                                                    int              x11_x_root,
                                                    int              x11_y_root,
                                                    guint32          x11_time);
gboolean hippo_canvas_item_emit_button_release_event (HippoCanvasItem *canvas_item,
                                                      int              x,
                                                      int              y,
                                                      int              button,
                                                      int              x11_x_root,
                                                      int              x11_y_root,
                                                      guint32          x11_time);
gboolean hippo_canvas_item_emit_motion_notify_event (HippoCanvasItem  *canvas_item,
                                                     int               x,
                                                     int               y,
                                                     HippoMotionDetail detail);
void     hippo_canvas_item_emit_activated          (HippoCanvasItem *canvas_item);
void     hippo_canvas_item_emit_paint_needed       (HippoCanvasItem *canvas_item,
                                                    int              x,
                                                    int              y,
                                                    int              width,
                                                    int              height);
void     hippo_canvas_item_emit_request_changed    (HippoCanvasItem *canvas_item);
gboolean hippo_canvas_item_process_event           (HippoCanvasItem *canvas_item,
                                                    HippoEvent      *event,
                                                    int              allocation_x,
                                                    int              allocation_y);

void     hippo_canvas_item_process_paint           (HippoCanvasItem *canvas_item,
                                                    cairo_t         *cr,
                                                    HippoRectangle  *damaged_box,
                                                    int              allocation_x,
                                                    int              allocation_y);

G_END_DECLS

#endif /* __HIPPO_CANVAS_ITEM_H__ */
