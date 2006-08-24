/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_ITEM_H__
#define __HIPPO_CANVAS_ITEM_H__

#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* not sure we need this or can just use cairo_t; maybe we want to
 * stick our own extensions onto the cairo_t though or something.
 */
typedef struct _HippoDrawable HippoDrawable;

typedef enum {
    HIPPO_EVENT_BUTTON_PRESS
} HippoEventType;

typedef struct _HippoEvent HippoEvent;

struct _HippoEvent {
    HippoEventType type;
    int x;
    int y;
};

typedef struct _HippoCanvasItem      HippoCanvasItem;
typedef struct _HippoCanvasItemClass HippoCanvasItemClass;

#define HIPPO_TYPE_CANVAS_ITEM              (hippo_canvas_item_get_type ())
#define HIPPO_CANVAS_ITEM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_ITEM, HippoCanvasItem))
#define HIPPO_CANVAS_ITEM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_ITEM, HippoCanvasItemClass))
#define HIPPO_IS_CANVAS_ITEM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_ITEM))
#define HIPPO_IS_CANVAS_ITEM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_ITEM))
#define HIPPO_CANVAS_ITEM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_INTERFACE ((obj), HIPPO_TYPE_CANVAS_ITEM, HippoCanvasItemClass))

struct _HippoCanvasItemClass {
    GTypeInterface base_iface;

    void     (* paint)              (HippoCanvasItem  *canvas_item,
                                     HippoDrawable    *drawable);
    int      (* get_width_request)  (HippoCanvasItem *canvas_item);
    int      (* get_height_request) (HippoCanvasItem *canvas_item,
                                     int              for_width);
    void     (* allocate)           (HippoCanvasItem *canvas_item,
                                     int              x,
                                     int              y,
                                     int              width,
                                     int              height);
    void     (* get_allocation)     (HippoCanvasItem *canvas_item,
                                     int             *x_p,
                                     int             *y_p,
                                     int             *width_p,
                                     int             *height_p);
    gboolean (* button_press_event) (HippoCanvasItem *canvas_item,
                                     HippoEvent      *event);
};

GType        	 hippo_canvas_item_get_type               (void) G_GNUC_CONST;

void     hippo_canvas_item_paint                   (HippoCanvasItem *canvas_item,
                                                    HippoDrawable   *drawable);
int      hippo_canvas_item_get_width_request       (HippoCanvasItem *canvas_item);
int      hippo_canvas_item_get_height_request      (HippoCanvasItem *canvas_item,
                                                    int              for_width);
void     hippo_canvas_item_allocate                (HippoCanvasItem *canvas_item,
                                                    int              x,
                                                    int              y,
                                                    int              width,
                                                    int              height);
void     hippo_canvas_item_get_allocation          (HippoCanvasItem *canvas_item,
                                                    int             *x_p,
                                                    int             *y_p,
                                                    int             *width_p,
                                                    int             *height_p);


void     hippo_canvas_item_get_request             (HippoCanvasItem *canvas_item,
                                                    int             *width_p,
                                                    int             *height_p);
gboolean hippo_canvas_item_emit_button_press_event (HippoCanvasItem *canvas_item,
                                                    HippoEvent      *event);
void     hippo_canvas_item_emit_request_changed    (HippoCanvasItem *canvas_item);


G_END_DECLS

#endif /* __HIPPO_CANVAS_ITEM_H__ */
