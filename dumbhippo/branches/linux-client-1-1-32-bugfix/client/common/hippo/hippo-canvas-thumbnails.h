/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_THUMBNAILS_H__
#define __HIPPO_CANVAS_THUMBNAILS_H__

/* A canvas item that renders a set of thumbnails */

#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasThumbnails      HippoCanvasThumbnails;
typedef struct _HippoCanvasThumbnailsClass HippoCanvasThumbnailsClass;

#define HIPPO_TYPE_CANVAS_THUMBNAILS              (hippo_canvas_thumbnails_get_type ())
#define HIPPO_CANVAS_THUMBNAILS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_THUMBNAILS, HippoCanvasThumbnails))
#define HIPPO_CANVAS_THUMBNAILS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_THUMBNAILS, HippoCanvasThumbnailsClass))
#define HIPPO_IS_CANVAS_THUMBNAILS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_THUMBNAILS))
#define HIPPO_IS_CANVAS_THUMBNAILS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_THUMBNAILS))
#define HIPPO_CANVAS_THUMBNAILS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_THUMBNAILS, HippoCanvasThumbnailsClass))

GType            hippo_canvas_thumbnails_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_thumbnails_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_THUMBNAILS_H__ */
