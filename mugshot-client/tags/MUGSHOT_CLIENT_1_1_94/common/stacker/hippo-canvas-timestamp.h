/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_TIMESTAMP_H__
#define __HIPPO_CANVAS_TIMESTAMP_H__

/* An href-link canvas item. */

#include <hippo/hippo-canvas-text.h>
#include "hippo-actions.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasTimestamp      HippoCanvasTimestamp;
typedef struct _HippoCanvasTimestampClass HippoCanvasTimestampClass;

#define HIPPO_TYPE_CANVAS_TIMESTAMP              (hippo_canvas_timestamp_get_type ())
#define HIPPO_CANVAS_TIMESTAMP(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_TIMESTAMP, HippoCanvasTimestamp))
#define HIPPO_CANVAS_TIMESTAMP_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_TIMESTAMP, HippoCanvasTimestampClass))
#define HIPPO_IS_CANVAS_TIMESTAMP(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_TIMESTAMP))
#define HIPPO_IS_CANVAS_TIMESTAMP_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_TIMESTAMP))
#define HIPPO_CANVAS_TIMESTAMP_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_TIMESTAMP, HippoCanvasTimestampClass))

struct _HippoCanvasTimestamp {
    HippoCanvasText text;
    HippoActions *actions;
    GTime time;
};

struct _HippoCanvasTimestampClass {
    HippoCanvasTextClass parent_class;
};

GType            hippo_canvas_timestamp_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_timestamp_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_TIMESTAMP_H__ */
