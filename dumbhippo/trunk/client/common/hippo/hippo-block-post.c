/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-post.h"
#include "hippo-post.h"
#include <string.h>

static void      hippo_block_post_init                (HippoBlockPost       *block_post);
static void      hippo_block_post_class_init          (HippoBlockPostClass  *klass);

static void      hippo_block_post_dispose             (GObject              *object);
static void      hippo_block_post_finalize            (GObject              *object);

static void hippo_block_post_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_block_post_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);

struct _HippoBlockPost {
    HippoBlock       parent;
    char            *post_id;
    HippoPost       *cached_post;
};

struct _HippoBlockPostClass {
    HippoBlockClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_CACHED_POST,
    PROP_POST_ID
};

G_DEFINE_TYPE(HippoBlockPost, hippo_block_post, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_post_init(HippoBlockPost *block_post)
{

}

static void
hippo_block_post_class_init(HippoBlockPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_post_set_property;
    object_class->get_property = hippo_block_post_get_property;

    object_class->dispose = hippo_block_post_dispose;
    object_class->finalize = hippo_block_post_finalize;
    
    g_object_class_install_property(object_class,
                                    PROP_CACHED_POST,
                                    g_param_spec_object("cached-post",
                                                        _("Cached post"),
                                                        _("Cached object matching the post ID"),
                                                        HIPPO_TYPE_POST,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_POST_ID,
                                    g_param_spec_string("post-id",
                                                        _("Post ID"),
                                                        _("Post ID matching the block"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));    
}

static void
set_post(HippoBlockPost *block_post,
         HippoPost      *post)
{
#if 0
    g_debug("set_post old post %s new post %s",
            block_post->cached_post ?
            hippo_post_get_guid(block_post->cached_post) : "null",
            post ?
            hippo_post_get_guid(post) : "null");
#endif
    
    if (post == block_post->cached_post)
        return;
    
    if (block_post->cached_post) {
        g_object_unref(block_post->cached_post);
        block_post->cached_post = NULL;
    }

    if (post) {
        g_object_ref(post);
        block_post->cached_post = post;
    }

    g_object_notify(G_OBJECT(block_post), "cached-post");
}

static void
hippo_block_post_dispose(GObject *object)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    set_post(block_post, NULL);
    
    G_OBJECT_CLASS(hippo_block_post_parent_class)->dispose(object); 
}

static void
hippo_block_post_finalize(GObject *object)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);
    
    g_free(block_post->post_id);
    
    G_OBJECT_CLASS(hippo_block_post_parent_class)->finalize(object); 
}

static void
hippo_block_post_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoBlockPost *block_post;

    block_post = HIPPO_BLOCK_POST(object);

    switch (prop_id) {
    case PROP_CACHED_POST:
        {
            HippoPost *new_post = (HippoPost*) g_value_get_object(value);
            set_post(block_post, new_post);
        }
        break;
    case PROP_POST_ID:
        g_free(block_post->post_id);
        block_post->post_id = g_value_dup_string(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_post_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoBlockPost *block_post;

    block_post = HIPPO_BLOCK_POST(object);

    switch (prop_id) {
    case PROP_CACHED_POST:
        g_value_set_object(value, (GObject*) block_post->cached_post);
        break;
    case PROP_POST_ID:
        g_value_set_string(value, block_post->post_id);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}
