#include "hippo-post.h"

/* === HippoPost implementation === */

static void     hippo_post_finalize             (GObject *object);

struct _HippoPost {
    GObject parent;
};

struct _HippoPostClass {
    GObjectClass parent;
};

G_DEFINE_TYPE(HippoPost, hippo_post, G_TYPE_OBJECT);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static void
hippo_post_init(HippoPost *post)
{
}

static void
hippo_post_class_init(HippoPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    object_class->finalize = hippo_post_finalize;
}

static void
hippo_post_finalize(GObject *object)
{
    HippoPost *post = HIPPO_POST(object);

    G_OBJECT_CLASS(hippo_post_parent_class)->finalize(object); 
}

/* === HippoPost exported API === */

HippoPost*
hippo_post_new(void)
{
    HippoPost *post = g_object_new(HIPPO_TYPE_POST, NULL);
    
    return post;
}
