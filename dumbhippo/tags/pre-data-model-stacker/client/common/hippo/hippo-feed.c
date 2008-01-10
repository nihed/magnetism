/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-feed.h"
#include "hippo-entity-protected.h"
#include <string.h>

/* === HippoFeed implementation === */

static void     hippo_feed_finalize             (GObject *object);

struct _HippoFeed {
    HippoEntity parent;
};

struct _HippoFeedClass {
    HippoEntityClass parent;
};

G_DEFINE_TYPE(HippoFeed, hippo_feed, HIPPO_TYPE_ENTITY);

#if 0
enum {
    NONE_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

static void
hippo_feed_init(HippoFeed *feed)
{
}

static void
hippo_feed_class_init(HippoFeedClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    object_class->finalize = hippo_feed_finalize;
}

static void
hippo_feed_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_feed_parent_class)->finalize(object); 
}

/* === HippoFeed exported API === */

                               
