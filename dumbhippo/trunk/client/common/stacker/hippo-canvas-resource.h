/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_RESOURCE_H__
#define __HIPPO_CANVAS_RESOURCE_H__

/* A canvas item that renders a single data model resource */

#include <ddm/ddm.h>
#include <hippo/hippo-canvas-box.h>
#include "hippo-actions.h"

G_BEGIN_DECLS

typedef struct _HippoCanvasResource      HippoCanvasResource;
typedef struct _HippoCanvasResourceClass HippoCanvasResourceClass;

struct _HippoCanvasResource {
    HippoCanvasBox parent;

    HippoActions *actions;
    DDMDataResource *resource;
};

struct _HippoCanvasResourceClass {
    HippoCanvasBoxClass parent_class;

    void (* create_children) (HippoCanvasResource *canvas_resource);
    void (* update)          (HippoCanvasResource *canvas_resource);
};

#define HIPPO_TYPE_CANVAS_RESOURCE              (hippo_canvas_resource_get_type ())
#define HIPPO_CANVAS_RESOURCE(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_RESOURCE, HippoCanvasResource))
#define HIPPO_CANVAS_RESOURCE_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_RESOURCE, HippoCanvasResourceClass))
#define HIPPO_IS_CANVAS_RESOURCE(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_RESOURCE))
#define HIPPO_IS_CANVAS_RESOURCE_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_RESOURCE))
#define HIPPO_CANVAS_RESOURCE_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_RESOURCE, HippoCanvasResourceClass))

GType            hippo_canvas_resource_get_type               (void) G_GNUC_CONST;

DDMDataResource *hippo_canvas_resource_get_resource (HippoCanvasResource *resource);

G_END_DECLS

#endif /* __HIPPO_CANVAS_RESOURCE_H__ */
