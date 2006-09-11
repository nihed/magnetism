/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include <hippo/hippo-post.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-post.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-image.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-link.h"

static void      hippo_canvas_block_post_init                (HippoCanvasBlockPost       *block);
static void      hippo_canvas_block_post_class_init          (HippoCanvasBlockPostClass  *klass);
static void      hippo_canvas_block_post_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_block_post_dispose             (GObject                *object);
static void      hippo_canvas_block_post_finalize            (GObject                *object);

static void hippo_canvas_block_post_set_property (GObject      *object,
                                                  guint         prop_id,
                                                  const GValue *value,
                                                  GParamSpec   *pspec);
static void hippo_canvas_block_post_get_property (GObject      *object,
                                                  guint         prop_id,
                                                  GValue       *value,
                                                  GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_block_post_paint              (HippoCanvasItem *item,
                                                            cairo_t         *cr);

/* Canvas block methods */
static void hippo_canvas_block_post_set_block (HippoCanvasBlock *canvas_block,
                                               HippoBlock       *block);


/* Our own methods */
static void hippo_canvas_block_post_set_post (HippoCanvasBlockPost *canvas_block_post,
                                              HippoPost            *post);

struct _HippoCanvasBlockPost {
    HippoCanvasBlock canvas_block;
    HippoPost *post;
};

struct _HippoCanvasBlockPostClass {
    HippoCanvasBlockClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockPost, hippo_canvas_block_post, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_post_iface_init));

static void
hippo_canvas_block_post_init(HippoCanvasBlockPost *block_post)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_post);

    block->required_type = HIPPO_BLOCK_TYPE_POST;

    hippo_canvas_block_set_heading(block, _("Web Swarm"));
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_block_post_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_block_post_paint;
}

static void
hippo_canvas_block_post_class_init(HippoCanvasBlockPostClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);
    
    object_class->set_property = hippo_canvas_block_post_set_property;
    object_class->get_property = hippo_canvas_block_post_get_property;

    object_class->dispose = hippo_canvas_block_post_dispose;
    object_class->finalize = hippo_canvas_block_post_finalize;

    canvas_block_class->set_block = hippo_canvas_block_post_set_block;
}

static void
hippo_canvas_block_post_dispose(GObject *object)
{
    HippoCanvasBlockPost *block_post = HIPPO_CANVAS_BLOCK_POST(object);

    hippo_canvas_block_post_set_post(block_post, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_post_parent_class)->dispose(object);
}

static void
hippo_canvas_block_post_finalize(GObject *object)
{
    /* HippoCanvasBlockPost *block = HIPPO_CANVAS_BLOCK_POST(object); */

    G_OBJECT_CLASS(hippo_canvas_block_post_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_block_post_new(void)
{
    HippoCanvasBlockPost *block_post;

    block_post = g_object_new(HIPPO_TYPE_CANVAS_BLOCK_POST, NULL);

    return HIPPO_CANVAS_ITEM(block_post);
}

static void
hippo_canvas_block_post_set_property(GObject         *object,
                                     guint            prop_id,
                                     const GValue    *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasBlockPost *block_post;

    block_post = HIPPO_CANVAS_BLOCK_POST(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_post_get_property(GObject         *object,
                                     guint            prop_id,
                                     GValue          *value,
                                     GParamSpec      *pspec)
{
    HippoCanvasBlockPost *block_post;

    block_post = HIPPO_CANVAS_BLOCK_POST (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_post_paint(HippoCanvasItem *item,
                              cairo_t         *cr)
{
    /* HippoCanvasBlockPost *block = HIPPO_CANVAS_BLOCK_POST(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr);
}

static void
on_cached_post_changed(HippoBlock *block,
                       GParamSpec *arg, /* null when first calling this */
                       void       *data)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(data);
    HippoPost *post;

    post = NULL;
    g_object_get(G_OBJECT(block),
                 "cached-post", &post,
                 NULL);

    g_debug("canvas block post notified of new cached-post %s",
            post ? hippo_post_get_guid(post) : "null");
    
    hippo_canvas_block_post_set_post(canvas_block_post, post);
}

static void
hippo_canvas_block_post_set_block(HippoCanvasBlock *canvas_block,
                                  HippoBlock       *block)
{
    /* g_debug("canvas-block-post set block %p", block); */
    
    if (block == canvas_block->block)
        return;
    
    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_cached_post_changed),
                                             canvas_block);
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_post_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::cached-post",
                         G_CALLBACK(on_cached_post_changed),
                         canvas_block);
        
        on_cached_post_changed(canvas_block->block, NULL, canvas_block);
    }
}

static void
update_post(HippoCanvasBlockPost *canvas_block_post)
{
    HippoPost *post;

    post = canvas_block_post->post;

    if (post == NULL) {
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                     NULL, NULL);
    } else {
        hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_block_post),
                                     hippo_post_get_title(post),
                                     NULL);
    }
}

static void
on_post_changed(HippoPost *post,
                void      *data)
{
    HippoCanvasBlockPost *canvas_block_post = HIPPO_CANVAS_BLOCK_POST(data);

    g_assert(post == canvas_block_post->post);
    
    update_post(canvas_block_post);
}

static void
hippo_canvas_block_post_set_post (HippoCanvasBlockPost *canvas_block_post,
                                  HippoPost            *post)
{
#if 0
    g_debug("setting canvas block post to %s from %s",
            post ? hippo_post_get_guid(post) : "null",
            canvas_block_post->post ? hippo_post_get_guid(canvas_block_post->post) :
            "null");
#endif
    
    if (post == canvas_block_post->post)
        return;
    
    if (canvas_block_post->post) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block_post->post),
                                             G_CALLBACK(on_post_changed),
                                             canvas_block_post);
        g_object_unref(canvas_block_post->post);
        canvas_block_post->post = NULL;
    }

    if (post != NULL) {
        g_object_ref(post);
        g_signal_connect(G_OBJECT(post),
                         "changed",
                         G_CALLBACK(on_post_changed),
                         canvas_block_post);
        canvas_block_post->post = post;
    }

    update_post(canvas_block_post);
}
