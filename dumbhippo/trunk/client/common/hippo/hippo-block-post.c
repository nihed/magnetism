/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-block.h"
#include "hippo-block-post.h"
#include "hippo-post.h"
#include <string.h>

static void      hippo_block_post_init                (HippoBlockPost       *block_post);
static void      hippo_block_post_class_init          (HippoBlockPostClass  *klass);

static void      hippo_block_post_dispose             (GObject              *object);

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
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_post_set_property;
    object_class->get_property = hippo_block_post_get_property;

    object_class->dispose = hippo_block_post_dispose;

    g_object_class_install_property(object_class,
                                    PROP_POST,
                                    g_param_spec_object("post",
                                                        _("Post"),
                                                        _("Post matching the block"),
                                                        HIPPO_TYPE_POST,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
set_post(HippoBlockPost *block_post,
         HippoPost      *post)
{
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
}

static void
hippo_block_post_dispose(GObject *object)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    set_post(block_post, NULL);
    
    G_OBJECT_CLASS(hippo_block_post_parent_class)->dispose(object); 
}

HippoBlock*
hippo_block_post_new(void);
{
    HippoBlockPost *block_post;

    block_post = g_object_new(HIPPO_TYPE_BLOCK_POST, NULL);

    return HIPPO_BLOCK(block_post);
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
    case PROP_POST:
        {
            HippoPost *new_post = (HippoPost*) g_value_get_object(value);
            set_post(block_post, new_post);
        }
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
    case PROP_POST:
        g_value_set_object(value, G_OBJECT(block_post->post));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}
