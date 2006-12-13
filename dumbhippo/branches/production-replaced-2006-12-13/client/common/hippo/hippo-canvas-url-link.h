/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_URL_LINK_H__
#define __HIPPO_CANVAS_URL_LINK_H__

/* An href-link canvas item. */

#include <hippo/hippo-canvas-text.h>
#include "hippo-actions.h"
#include <cairo.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasUrlLink      HippoCanvasUrlLink;
typedef struct _HippoCanvasUrlLinkClass HippoCanvasUrlLinkClass;

#define HIPPO_TYPE_CANVAS_URL_LINK              (hippo_canvas_url_link_get_type ())
#define HIPPO_CANVAS_URL_LINK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_URL_LINK, HippoCanvasUrlLink))
#define HIPPO_CANVAS_URL_LINK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_URL_LINK, HippoCanvasUrlLinkClass))
#define HIPPO_IS_CANVAS_URL_LINK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_URL_LINK))
#define HIPPO_IS_CANVAS_URL_LINK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_URL_LINK))
#define HIPPO_CANVAS_URL_LINK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_URL_LINK, HippoCanvasUrlLinkClass))

struct _HippoCanvasUrlLink {
    HippoCanvasText text;
    HippoActions *actions;
    char *url;
};

struct _HippoCanvasUrlLinkClass {
    HippoCanvasTextClass parent_class;
};

GType            hippo_canvas_url_link_get_type               (void) G_GNUC_CONST;

HippoCanvasItem* hippo_canvas_url_link_new    (void);


G_END_DECLS

#endif /* __HIPPO_CANVAS_URL_LINK_H__ */
