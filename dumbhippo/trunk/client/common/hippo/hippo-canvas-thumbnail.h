/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_THUMBNAIL_H__
#define __HIPPO_CANVAS_THUMBNAIL_H__

/* A canvas item that renders a single thumbnail */

#include <ddm/ddm.h>
#include <hippo/hippo-actions.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasThumbnail      HippoCanvasThumbnail;
typedef struct _HippoCanvasThumbnailClass HippoCanvasThumbnailClass;

#define HIPPO_TYPE_CANVAS_THUMBNAIL              (hippo_canvas_thumbnail_get_type ())
#define HIPPO_CANVAS_THUMBNAIL(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_THUMBNAIL, HippoCanvasThumbnail))
#define HIPPO_CANVAS_THUMBNAIL_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_THUMBNAIL, HippoCanvasThumbnailClass))
#define HIPPO_IS_CANVAS_THUMBNAIL(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_THUMBNAIL))
#define HIPPO_IS_CANVAS_THUMBNAIL_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_THUMBNAIL))
#define HIPPO_CANVAS_THUMBNAIL_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_THUMBNAIL, HippoCanvasThumbnailClass))

GType            hippo_canvas_thumbnail_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_thumbnail_new    (DDMDataResource *resource,
                                                HippoActions    *actions);

G_END_DECLS

#endif /* __HIPPO_CANVAS_THUMBNAIL_H__ */
