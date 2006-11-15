/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_LINK_H__
#define __HIPPO_CANVAS_LINK_H__

/* An href-link canvas item. */

#include "hippo-canvas-text.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasLink      HippoCanvasLink;
typedef struct _HippoCanvasLinkClass HippoCanvasLinkClass;

#define HIPPO_TYPE_CANVAS_LINK              (hippo_canvas_link_get_type ())
#define HIPPO_CANVAS_LINK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_LINK, HippoCanvasLink))
#define HIPPO_CANVAS_LINK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_LINK, HippoCanvasLinkClass))
#define HIPPO_IS_CANVAS_LINK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_LINK))
#define HIPPO_IS_CANVAS_LINK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_LINK))
#define HIPPO_CANVAS_LINK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_LINK, HippoCanvasLinkClass))

struct _HippoCanvasLink {
    HippoCanvasText text;
};

struct _HippoCanvasLinkClass {
    HippoCanvasTextClass parent_class;
};

GType            hippo_canvas_link_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_link_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_LINK_H__ */
