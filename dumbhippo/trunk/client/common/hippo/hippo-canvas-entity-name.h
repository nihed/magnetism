/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_ENTITY_NAME_H__
#define __HIPPO_CANVAS_ENTITY_NAME_H__

/* A canvas item that displays an entity's name */

#include "hippo-canvas-item.h"
#include <cairo.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasEntityName      HippoCanvasEntityName;
typedef struct _HippoCanvasEntityNameClass HippoCanvasEntityNameClass;

#define HIPPO_TYPE_CANVAS_ENTITY_NAME              (hippo_canvas_entity_name_get_type ())
#define HIPPO_CANVAS_ENTITY_NAME(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_ENTITY_NAME, HippoCanvasEntityName))
#define HIPPO_CANVAS_ENTITY_NAME_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_ENTITY_NAME, HippoCanvasEntityNameClass))
#define HIPPO_IS_CANVAS_ENTITY_NAME(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_ENTITY_NAME))
#define HIPPO_IS_CANVAS_ENTITY_NAME_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_ENTITY_NAME))
#define HIPPO_CANVAS_ENTITY_NAME_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_ENTITY_NAME, HippoCanvasEntityNameClass))

GType        	 hippo_canvas_entity_name_get_type    (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_entity_name_new         (void);

G_END_DECLS

#endif /* __HIPPO_CANVAS_ENTITY_NAME_H__ */
