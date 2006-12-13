/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_FEED_H__
#define __HIPPO_FEED_H__

#include <hippo/hippo-entity.h>

G_BEGIN_DECLS

typedef struct _HippoFeed      HippoFeed;
typedef struct _HippoFeedClass HippoFeedClass;

#define HIPPO_TYPE_FEED              (hippo_feed_get_type ())
#define HIPPO_FEED(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_FEED, HippoFeed))
#define HIPPO_FEED_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_FEED, HippoFeedClass))
#define HIPPO_IS_FEED(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_FEED))
#define HIPPO_IS_FEED_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_FEED))
#define HIPPO_FEED_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_FEED, HippoFeedClass))

GType           hippo_feed_get_type                  (void) G_GNUC_CONST;
HippoFeed*      hippo_feed_new                       (const char  *guid);

G_END_DECLS

#endif /* __HIPPO_FEED_H__ */
