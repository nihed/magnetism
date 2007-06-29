/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT_H__
#define __HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT_H__

/* A canvas item that displays a stacker block */

#include <hippo/hippo-canvas-item.h>
#include <hippo/hippo-block.h>

G_BEGIN_DECLS

typedef struct _HippoCanvasBlockFacebookEvent      HippoCanvasBlockFacebookEvent;
typedef struct _HippoCanvasBlockFacebookEventClass HippoCanvasBlockFacebookEventClass;

#define HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT              (hippo_canvas_block_facebook_event_get_type ())
#define HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT, HippoCanvasBlockFacebookEvent))
#define HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT, HippoCanvasBlockFacebookEventClass))
#define HIPPO_IS_CANVAS_BLOCK_FACEBOOK_EVENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT))
#define HIPPO_IS_CANVAS_BLOCK_FACEBOOK_EVENT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT))
#define HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CANVAS_BLOCK_FACEBOOK_EVENT, HippoCanvasBlockFacebookEventClass))

GType            hippo_canvas_block_facebook_event_get_type    (void) G_GNUC_CONST;

G_END_DECLS

#endif /* __HIPPO_CANVAS_BLOCK_FACEBOOK_EVENT_H__ */
