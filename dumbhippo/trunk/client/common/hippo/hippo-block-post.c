/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-post.h"
#include "hippo-feed.h"
#include "hippo-post.h"
#include <string.h>

static void      hippo_block_post_init                (HippoBlockPost       *block_post);
static void      hippo_block_post_class_init          (HippoBlockPostClass  *klass);

static void      hippo_block_post_dispose             (GObject              *object);
static void      hippo_block_post_finalize            (GObject              *object);

static void      hippo_block_post_update              (HippoBlock           *block);

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
    HippoPost       *post;
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
    PROP_POST
};

G_DEFINE_TYPE(HippoBlockPost, hippo_block_post, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_post_init(HippoBlockPost *block_post)
{
}

static void
hippo_block_post_class_init(HippoBlockPostClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_post_set_property;
    object_class->get_property = hippo_block_post_get_property;

    object_class->dispose = hippo_block_post_dispose;
    object_class->finalize = hippo_block_post_finalize;

    block_class->update = hippo_block_post_update;
    
    g_object_class_install_property(object_class,
                                    PROP_POST,
                                    g_param_spec_object("post",
                                                        _("Post"),
                                                        _("Post displayed in the block"),
                                                        HIPPO_TYPE_POST,
                                                        G_PARAM_READABLE));
}

static void
set_post(HippoBlockPost *block_post,
         HippoPost      *post)
{
#if 0
    g_debug("set_post old post %s new post %s",
            block_post->post ?
            hippo_post_get_guid(block_post->post) : "null",
            post ?
            hippo_post_get_guid(post) : "null");
#endif
    
    if (post == block_post->post)
        return;
    
    if (block_post->post) {
        g_object_unref(block_post->post);
        block_post->post = NULL;
    }

    if (post) {
        g_object_ref(post);
        block_post->post = post;
    }

    g_object_notify(G_OBJECT(block_post), "post");
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
    G_OBJECT_CLASS(hippo_block_post_parent_class)->finalize(object); 
}

static void
hippo_block_post_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    switch (prop_id) {
    case PROP_POST:
        set_post(block_post, (HippoPost*) g_value_get_object(value));
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
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    switch (prop_id) {
    case PROP_POST:
        g_value_set_object(value, G_OBJECT(block_post->post));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_post_update (HippoBlock           *block)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(block);
    DDMDataResource *post_resource;
    gboolean is_feed;

    HIPPO_BLOCK_CLASS(hippo_block_post_parent_class)->update(block);

    ddm_data_resource_get(block->resource,
                          "post", DDM_DATA_RESOURCE, &post_resource,
                          "feed", DDM_DATA_BOOLEAN, &is_feed,
                          NULL);

    if (post_resource == NULL && block_post->post == NULL)
        return;

    if (post_resource != NULL && block_post->post != NULL) {
        if (hippo_post_get_resource(block_post->post) == post_resource)
            return;
    }

    if (post_resource != NULL) {
        HippoPost *post = hippo_post_new(post_resource);
        set_post(block_post, post);
        g_object_unref(post);
    } else{
        set_post(block_post, NULL);
    }

    hippo_block_set_is_feed(block, is_feed);
}
