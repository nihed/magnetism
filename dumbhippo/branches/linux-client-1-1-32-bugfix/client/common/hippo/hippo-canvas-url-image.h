/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_URL_IMAGE_H__
#define __HIPPO_CANVAS_URL_IMAGE_H__

/* An href-image canvas item. */

#include <hippo/hippo-canvas-image.h>
#include "hippo-actions.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasUrlImage      HippoCanvasUrlImage;
typedef struct _HippoCanvasUrlImageClass HippoCanvasUrlImageClass;

#define HIPPO_TYPE_CANVAS_URL_IMAGE              (hippo_canvas_url_image_get_type ())
#define HIPPO_CANVAS_URL_IMAGE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_URL_IMAGE, HippoCanvasUrlImage))
#define HIPPO_CANVAS_URL_IMAGE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_URL_IMAGE, HippoCanvasUrlImageClass))
#define HIPPO_IS_CANVAS_URL_IMAGE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_URL_IMAGE))
#define HIPPO_IS_CANVAS_URL_IMAGE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_URL_IMAGE))
#define HIPPO_CANVAS_URL_IMAGE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_URL_IMAGE, HippoCanvasUrlImageClass))

struct _HippoCanvasUrlImage {
    HippoCanvasImage parent;
    HippoActions *actions;
    char *url;
};

struct _HippoCanvasUrlImageClass {
    HippoCanvasImageClass parent_class;
};

GType            hippo_canvas_url_image_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_url_image_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_URL_IMAGE_H__ */
