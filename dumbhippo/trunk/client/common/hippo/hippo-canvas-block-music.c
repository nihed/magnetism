/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-message-preview.h"
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

/* Internals */
static void hippo_canvas_block_music_update_visibility(HippoCanvasBlockMusic *block_music);


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
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music); */
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

    hippo_canvas_block_music_set_track_history(block_music, NULL);

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
                       "orientation", HIPPO_ORIENTATION_VERTICAL,
                       "spacing", 4,
                       NULL);
    block_music->parent_box = box;
    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));

    block_music->single_message_preview = g_object_new(HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW,
                                                      "actions", hippo_canvas_block_get_actions(block),
                                                      NULL);
    hippo_canvas_box_append(box, block_music->single_message_preview, 0);
    hippo_canvas_box_set_child_visible(box,
                                       block_music->single_message_preview,
                                       FALSE); /* no messages yet */
    
    block_music->downloads_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                              "spacing", 4,
                                              NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(block_music->downloads_box), 0);

    block_music->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                             "actions", hippo_canvas_block_get_actions(block),
                                             NULL);
    hippo_canvas_box_append(box, block_music->chat_preview, 0);
    hippo_canvas_box_set_child_visible(block_music->parent_box,
                                       block_music->chat_preview,
                                       FALSE); /* not expanded at first */
    
    return object;
}

static void
set_track(HippoCanvasBlockMusic *block_music,
          HippoTrack                  *track)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    HippoActions *actions = hippo_canvas_block_get_actions(canvas_block);
    const char *chat_id = NULL;

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

        chat_id = hippo_track_get_play_id(track);
        
        g_free(title);
        g_free(artist);
        g_free(name);
    } else {
        hippo_canvas_block_set_title(canvas_block, NULL, NULL, FALSE);
        hippo_canvas_box_remove_all(block_music->downloads_box);
    }

    g_object_set(G_OBJECT(block_music->chat_preview),
                 "chat-id", chat_id,
                 NULL);
}

static void
set_old_tracks(HippoCanvasBlockMusic *block_music,
               GSList                *old_tracks)
{
    GSList *old_old_tracks = block_music->old_tracks;
    
    block_music->old_tracks = g_slist_copy(old_tracks);
    g_slist_foreach(old_tracks, (GFunc)g_object_ref, NULL);
    
    g_slist_foreach(old_old_tracks, (GFunc)g_object_unref, NULL);
    g_slist_free(old_old_tracks);
}

void 
hippo_canvas_block_music_set_track_history(HippoCanvasBlockMusic *block_music,
                                           GSList                *track_history)
{
    if (track_history) {
        set_track(block_music, track_history->data);
        set_old_tracks(block_music, track_history->next);
    } else {
        set_track(block_music, NULL);
        set_old_tracks(block_music, NULL);
    }
}

void 
hippo_canvas_block_music_set_recent_messages(HippoCanvasBlockMusic *block_music,
                                             GSList                *recent_messages)
{
    HippoChatMessage *last_message = NULL;

    if (recent_messages)
        last_message = recent_messages->data;

    g_object_set(G_OBJECT(block_music->chat_preview),
                 "recent-messages", recent_messages,
                 NULL);
    g_object_set(G_OBJECT(block_music->single_message_preview),
                 "message", last_message,
                 NULL);

    block_music->have_messages = last_message != NULL;

    hippo_canvas_block_music_update_visibility(block_music);
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
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->expand(canvas_block);

    hippo_canvas_block_music_update_visibility(block_music);
}

static void
hippo_canvas_block_music_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block);
    
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->unexpand(canvas_block);
    
    hippo_canvas_block_music_update_visibility(block_music);
}

static void
hippo_canvas_block_music_update_visibility(HippoCanvasBlockMusic *block_music)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    
    hippo_canvas_box_set_child_visible(block_music->parent_box,
                                       block_music->single_message_preview,
                                       !canvas_block->expanded && block_music->have_messages);
    hippo_canvas_box_set_child_visible(block_music->parent_box,
                                       block_music->chat_preview,
                                       canvas_block->expanded);
}

