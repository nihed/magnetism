/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_BLOCK_FACEBOOK_EVENT_H__
#define __HIPPO_BLOCK_FACEBOOK_EVENT_H__

#include <hippo/hippo-block.h>
#include <hippo/hippo-thumbnails.h>

G_BEGIN_DECLS

typedef struct _HippoBlockFacebookEvent      HippoBlockFacebookEvent;
typedef struct _HippoBlockFacebookEventClass HippoBlockFacebookEventClass;


#define HIPPO_TYPE_BLOCK_FACEBOOK_EVENT              (hippo_block_facebook_event_get_type ())
#define HIPPO_BLOCK_FACEBOOK_EVENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_BLOCK_FACEBOOK_EVENT, HippoBlockFacebookEvent))
#define HIPPO_BLOCK_FACEBOOK_EVENT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_BLOCK_FACEBOOK_EVENT, HippoBlockFacebookEventClass))
#define HIPPO_IS_BLOCK_FACEBOOK_EVENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_BLOCK_FACEBOOK_EVENT))
#define HIPPO_IS_BLOCK_FACEBOOK_EVENT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_BLOCK_FACEBOOK_EVENT))
#define HIPPO_BLOCK_FACEBOOK_EVENT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_BLOCK_FACEBOOK_EVENT, HippoBlockFacebookEventClass))

GType            hippo_block_facebook_event_get_type               (void) G_GNUC_CONST;

HippoThumbnails*  hippo_block_facebook_event_get_thumbnails  (HippoBlockFacebookEvent *block_facebook_event);
const char*       hippo_block_facebook_event_get_title       (HippoBlockFacebookEvent *block_facebook_event);

G_END_DECLS

#endif /* __HIPPO_BLOCK_FACEBOOK_EVENT_H__ */
