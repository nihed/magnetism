/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_ENTITY_PHOTO_H__
#define __HIPPO_CANVAS_ENTITY_PHOTO_H__

/* A canvas item that displays an entity's photo */

#include "hippo-canvas-item.h"
#include <cairo/cairo.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasEntityPhoto      HippoCanvasEntityPhoto;
typedef struct _HippoCanvasEntityPhotoClass HippoCanvasEntityPhotoClass;

#define HIPPO_TYPE_CANVAS_ENTITY_PHOTO              (hippo_canvas_entity_photo_get_type ())
#define HIPPO_CANVAS_ENTITY_PHOTO(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_ENTITY_PHOTO, HippoCanvasEntityPhoto))
#define HIPPO_CANVAS_ENTITY_PHOTO_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_ENTITY_PHOTO, HippoCanvasEntityPhotoClass))
#define HIPPO_IS_CANVAS_ENTITY_PHOTO(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_ENTITY_PHOTO))
#define HIPPO_IS_CANVAS_ENTITY_PHOTO_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_ENTITY_PHOTO))
#define HIPPO_CANVAS_ENTITY_PHOTO_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_ENTITY_PHOTO, HippoCanvasEntityPhotoClass))

GType        	 hippo_canvas_entity_photo_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_entity_photo_new         (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_ENTITY_PHOTO_H__ */
