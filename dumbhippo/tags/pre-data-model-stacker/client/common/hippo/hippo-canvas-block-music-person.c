/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-block-music-person.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include "hippo-canvas-block-music-person.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_block_music_person_init                (HippoCanvasBlockMusicPerson       *block);
static void      hippo_canvas_block_music_person_class_init          (HippoCanvasBlockMusicPersonClass  *klass);
static void      hippo_canvas_block_music_person_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_music_person_dispose             (GObject                *object);
static void      hippo_canvas_block_music_person_finalize            (GObject                *object);

static void hippo_canvas_block_music_person_set_property (GObject      *object,
                                                          guint         prop_id,
                                                          const GValue *value,
                                                          GParamSpec   *pspec);
static void hippo_canvas_block_music_person_get_property (GObject      *object,
                                                          guint         prop_id,
                                                          GValue       *value,
                                                          GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_music_person_set_block       (HippoCanvasBlock *canvas_block,
                                                             HippoBlock       *block);

struct _HippoCanvasBlockMusicPerson {
    HippoCanvasBlockMusic parent;
    HippoPerson *person;
    HippoTrack *track;
    HippoCanvasBox *downloads_box;
};

struct _HippoCanvasBlockMusicPersonClass {
    HippoCanvasBlockMusicClass parent_class;

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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMusicPerson, hippo_canvas_block_music_person, HIPPO_TYPE_CANVAS_BLOCK_MUSIC,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_music_person_iface_init));

static void
hippo_canvas_block_music_person_init(HippoCanvasBlockMusicPerson *block_music_person)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music_person);

    block->required_type = HIPPO_BLOCK_TYPE_MUSIC_PERSON;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_music_person_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_music_person_class_init(HippoCanvasBlockMusicPersonClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_music_person_set_property;
    object_class->get_property = hippo_canvas_block_music_person_get_property;

    object_class->dispose = hippo_canvas_block_music_person_dispose;
    object_class->finalize = hippo_canvas_block_music_person_finalize;

    canvas_block_class->set_block = hippo_canvas_block_music_person_set_block;
}

static void
hippo_canvas_block_music_person_dispose(GObject *object)
{
    /* HippoCanvasBlockMusicPerson *block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_person_parent_class)->dispose(object);
}

static void
hippo_canvas_block_music_person_finalize(GObject *object)
{
    /* HippoCanvasBlockMusicPerson *block = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_person_parent_class)->finalize(object);
}

static void
hippo_canvas_block_music_person_set_property(GObject         *object,
                                             guint            prop_id,
                                             const GValue    *value,
                                             GParamSpec      *pspec)
{
    /* HippoCanvasBlockMusicPerson *block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_music_person_get_property(GObject         *object,
                                             guint            prop_id,
                                             GValue          *value,
                                             GParamSpec      *pspec)
{
    /* HippoCanvasBlockMusicPerson *block_music_person = HIPPO_CANVAS_BLOCK_MUSIC_PERSON(object); */

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
on_track_history_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         HippoCanvasBlockMusicPerson *block_music_person)
{
    GSList *track_history = NULL;
    
    g_object_get(G_OBJECT(block), "track-history", &track_history, NULL);
    
    hippo_canvas_block_music_set_track_history(HIPPO_CANVAS_BLOCK_MUSIC(block_music_person), track_history);
}

static void
hippo_canvas_block_music_person_set_block(HippoCanvasBlock *canvas_block,
                                          HippoBlock       *block)
{
    /* g_debug("canvas-block-music-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_track_history_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_person_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::track-history",
                         G_CALLBACK(on_track_history_changed),
                         canvas_block);
        on_track_history_changed(canvas_block->block, NULL,
                                 HIPPO_CANVAS_BLOCK_MUSIC_PERSON(canvas_block));
    }
}
