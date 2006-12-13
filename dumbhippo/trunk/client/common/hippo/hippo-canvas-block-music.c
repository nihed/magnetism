/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_block_music_init                (HippoCanvasBlockMusic       *block);
static void      hippo_canvas_block_music_class_init          (HippoCanvasBlockMusicClass  *klass);
static void      hippo_canvas_block_music_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_music_dispose             (GObject                *object);
static void      hippo_canvas_block_music_finalize            (GObject                *object);

static void hippo_canvas_block_music_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_canvas_block_music_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);
static GObject* hippo_canvas_block_music_constructor (GType                  type,
                                                      guint                  n_construct_properties,
                                                      GObjectConstructParam *construct_params);

/* Canvas block methods */
static void hippo_canvas_block_music_set_block       (HippoCanvasBlock *canvas_block,
                                                      HippoBlock       *block);

static void hippo_canvas_block_music_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_music_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_music_unexpand (HippoCanvasBlock *canvas_block);

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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMusic, hippo_canvas_block_music, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_music_iface_init));

static void
hippo_canvas_block_music_init(HippoCanvasBlockMusic *block_music)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music);

    block->expandable = FALSE; /* currently we have nothing to show on expand */
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_music_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_music_class_init(HippoCanvasBlockMusicClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_music_set_property;
    object_class->get_property = hippo_canvas_block_music_get_property;
    object_class->constructor = hippo_canvas_block_music_constructor;

    object_class->dispose = hippo_canvas_block_music_dispose;
    object_class->finalize = hippo_canvas_block_music_finalize;

    canvas_block_class->set_block = hippo_canvas_block_music_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_music_title_activated;
    canvas_block_class->expand = hippo_canvas_block_music_expand;
    canvas_block_class->unexpand = hippo_canvas_block_music_unexpand;
}

static void
hippo_canvas_block_music_dispose(GObject *object)
{
    HippoCanvasBlockMusic *block_music;

    block_music = HIPPO_CANVAS_BLOCK_MUSIC(object);

    hippo_canvas_block_music_set_track(block_music, NULL);

    G_OBJECT_CLASS(hippo_canvas_block_music_parent_class)->dispose(object);
}

static void
hippo_canvas_block_music_finalize(GObject *object)
{
    /* HippoCanvasBlockMusic *block = HIPPO_CANVAS_BLOCK_MUSIC(object); */

    G_OBJECT_CLASS(hippo_canvas_block_music_parent_class)->finalize(object);
}

static void
hippo_canvas_block_music_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasBlockMusic *block_music;

    block_music = HIPPO_CANVAS_BLOCK_MUSIC(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_music_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasBlockMusic *block_music;

    block_music = HIPPO_CANVAS_BLOCK_MUSIC (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_block_music_constructor (GType                  type,
                                      guint                  n_construct_properties,
                                      GObjectConstructParam *construct_properties)
{
    GObject *object;
    HippoCanvasBlock *block;
    HippoCanvasBlockMusic *block_music;
    HippoCanvasBox *box;

    object = G_OBJECT_CLASS(hippo_canvas_block_music_parent_class)->constructor(type,
                                                                                       n_construct_properties,
                                                                                       construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_music = HIPPO_CANVAS_BLOCK_MUSIC(object);
    
    hippo_canvas_block_set_heading(block, _("Music Radar"));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 4,
                       NULL);

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));

    block_music->downloads_box = box;

    return object;
}

void
hippo_canvas_block_music_set_track(HippoCanvasBlockMusic *block_music,
                                   HippoTrack                  *track)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    HippoActions *actions = hippo_canvas_block_get_actions(canvas_block);

    if (track == block_music->track)
        return;

    if (block_music->track) {
        g_object_unref (block_music->track);
    }

    block_music->track = track;
    
    if (track) {
        char *artist = NULL;
        char *name = NULL;
        GSList *downloads = NULL;
        char *title;

        g_object_ref(track);
        
        g_object_get(G_OBJECT(track),
                     "artist", &artist,
                     "name", &name,
                     "downloads", &downloads,
                     NULL);

        if (artist)
            title = g_strdup_printf("%s (%s)", name, artist);
        else
            title = g_strdup(name);
        hippo_canvas_block_set_title(canvas_block, title,
                                     "More information about this song", FALSE);

        hippo_canvas_box_remove_all(block_music->downloads_box);
        
        while (downloads) {
            HippoCanvasItem *link;
            char *link_name;
            HippoCanvasItem *separator;
            HippoSongDownload *d = downloads->data;
            downloads = downloads->next;

            if (!hippo_actions_can_play_song_download(actions, d))
                continue;

            link_name = g_strdup_printf("Play at %s",
                                        hippo_song_download_source_get_name(hippo_song_download_get_source(d)));
            
            link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                "actions", actions,
                                "text", link_name,
                                "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                "url", hippo_song_download_get_url(d),
                                NULL);

            g_free(link_name);

            hippo_canvas_box_append(block_music->downloads_box,
                                    link, 0);

            if (downloads) {
                separator = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                         "text", " | ",
                                         NULL);
                hippo_canvas_box_append(block_music->downloads_box,
                                        separator, 0);
            }
        }
        
        g_free(title);
        g_free(artist);
        g_free(name);
    } else {
        hippo_canvas_block_set_title(canvas_block, NULL, NULL, FALSE);
        hippo_canvas_box_remove_all(block_music->downloads_box);
    }
}

static void
on_user_changed(HippoBlock *block,
                GParamSpec *arg, /* null when first calling this */
                HippoCanvasBlockMusic *block_music)
{
    HippoPerson *person;
    person = NULL;
    g_object_get(G_OBJECT(block), "user", &person, NULL);
    
    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_music),
                                  person ? hippo_entity_get_guid(HIPPO_ENTITY(person)) : NULL);
    
    if (person)
        g_object_unref(person);
}

static void
hippo_canvas_block_music_set_block(HippoCanvasBlock *canvas_block,
                                   HippoBlock       *block)
{
    /* g_debug("canvas-block-music-person set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_user_changed),
                                             canvas_block);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::user",
                         G_CALLBACK(on_user_changed),
                         canvas_block);

        on_user_changed(canvas_block->block, NULL,
                        HIPPO_CANVAS_BLOCK_MUSIC(canvas_block));
    }
}

static void
hippo_canvas_block_music_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block);
    const char *url;

    if (block_music->track == NULL)
        return;

    url = hippo_track_get_url(block_music->track);
    if (url == NULL)
        return;
    
    hippo_actions_open_url(hippo_canvas_block_get_actions(canvas_block), url);
}

static void
hippo_canvas_block_music_expand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->expand(canvas_block);
}

static void
hippo_canvas_block_music_unexpand(HippoCanvasBlock *canvas_block)
{
    /* HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block); */

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->unexpand(canvas_block);
}
