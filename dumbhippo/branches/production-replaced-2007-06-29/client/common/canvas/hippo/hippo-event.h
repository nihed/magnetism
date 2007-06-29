/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __HIPPO_EVENT_H__
#define __HIPPO_EVENT_H__

#include <glib-object.h>
#include <hippo/hippo-graphics.h>

G_BEGIN_DECLS

#define HIPPO_TYPE_EVENT (hippo_event_get_type())

typedef enum {
    HIPPO_EVENT_BUTTON_PRESS,
    HIPPO_EVENT_BUTTON_RELEASE,
    HIPPO_EVENT_MOTION_NOTIFY,
    HIPPO_EVENT_KEY_PRESS,
} HippoEventType;

typedef enum {
    HIPPO_MOTION_DETAIL_ENTER,
    HIPPO_MOTION_DETAIL_LEAVE,
    HIPPO_MOTION_DETAIL_WITHIN
} HippoMotionDetail;

typedef struct _HippoEvent HippoEvent;

typedef enum {
    HIPPO_KEY_UNKNOWN, 
    HIPPO_KEY_RETURN,
    HIPPO_KEY_ESCAPE
} HippoKey;

struct _HippoEvent {
    HippoEventType type;
    int x;
    int y;
    union {
        struct {
            HippoMotionDetail detail;
        } motion;
        struct {
            int button;

            /* we pass these through for gtk_window_begin_resize_drag() */
            int x11_x_root;
            int x11_y_root;
            guint32 x11_time;
        } button;
        struct {
            HippoKey key;
            gunichar character; /* 0 if no translation */
        } key;
    } u;
};

GType       hippo_event_get_type (void) G_GNUC_CONST;
HippoEvent *hippo_event_copy     (HippoEvent *event);
void        hippo_event_free     (HippoRectangle *event);


G_END_DECLS

#endif /* __HIPPO_EVENT_H */
