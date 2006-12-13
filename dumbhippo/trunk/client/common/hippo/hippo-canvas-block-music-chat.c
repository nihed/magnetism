/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include "hippo-canvas-block-music-chat.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_block_music_chat_init                (HippoCanvasBlockMusicChat       *block);
static void      hippo_canvas_block_music_chat_class_init          (HippoCanvasBlockMusicChatClass  *klass);
static void      hippo_canvas_block_music_chat_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_music_chat_dispose             (GObject                *object);
static void      hippo_canvas_block_music_chat_finalize            (GObject                *object);

static void hippo_canvas_block_music_chat_set_property (GObject      *object,
                                                        guint         prop_id,
                                                        const GValue *value,
                                                        GParamSpec   *pspec);
static void hippo_canvas_block_music_chat_get_property (GObject      *object,
                                                        guint         prop_id,
                                                        GValue       *value,
                                                        GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_music_chat_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

struct _HippoCanvasBlockMusicChat {
    HippoCanvasBlock canvas_block;
    HippoPerson *person;
    HippoTrack *track;
    HippoCanvasBox *downloads_box;
};

struct _HippoCanvasBlockMusicChatClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMusicChat, hippo_canvas_block_music_chat, HIPPO_TYPE_CANVAS_BLOCK_MUSIC,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_music_chat_iface_init));

static void
hippo_canvas_block_music_chat_init(HippoCanvasBlockMusicChat *block_music_chat)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music_chat);

    block->required_type = HIPPO_BLOCK_TYPE_MUSIC_CHAT;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_music_chat_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_music_chat_class_init(HippoCanvasBlockMusicChatClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_music_chat_set_property;
    object_class->get_property = hippo_canvas_block_music_chat_get_property;

    object_class->dispose = hippo_canvas_block_music_chat_dispose;
    object_class->finalize = hippo_canvas_block_music_chat_finalize;

    canvas_block_class->set_block = hippo_canvas_block_music_chat_set_block;
}

static void
hippo_canvas_block_music_chat_dispose(GObject *object)
{
    /* HippoCanvasBlockMusicChat *block_music_chat = HIPPO_CANVAS_BLOCK_MUSIC_CHAT(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_chat_parent_class)->dispose(object);
}

static void
hippo_canvas_block_music_chat_finalize(GObject *object)
{
    /* HippoCanvasBlockMusicChat *block = HIPPO_CANVAS_BLOCK_MUSIC_CHAT(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_chat_parent_class)->finalize(object);
}

static void
hippo_canvas_block_music_chat_set_property(GObject         *object,
                                           guint            prop_id,
                                           const GValue    *value,
                                           GParamSpec      *pspec)
{
    /* HippoCanvasBlockMusicChat *block_music_chat = HIPPO_CANVAS_BLOCK_MUSIC_CHAT(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_music_chat_get_property(GObject         *object,
                                           guint            prop_id,
                                           GValue          *value,
                                           GParamSpec      *pspec)
{
    /* HippoCanvasBlockMusicChat *block_music_chat = HIPPO_CANVAS_BLOCK_MUSIC_CHAT(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
on_track_changed(HippoBlock *block,
                 GParamSpec *arg, /* null when first calling this */
                 HippoCanvasBlockMusicChat *block_music_chat)
{
    HippoTrack *track = NULL;
    g_object_get(G_OBJECT(block), "track", &track, NULL);

    hippo_canvas_block_music_set_track(HIPPO_CANVAS_BLOCK_MUSIC(block_music_chat), track);

    g_object_unref(track);
}

static void
hippo_canvas_block_music_chat_set_block(HippoCanvasBlock *canvas_block,
                                          HippoBlock       *block)
{
    /* g_debug("canvas-block-music-chat set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_track_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_chat_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::track",
                         G_CALLBACK(on_track_changed),
                         canvas_block);
        on_track_changed(canvas_block->block, NULL,
                         HIPPO_CANVAS_BLOCK_MUSIC_CHAT(canvas_block));
    }
}
