/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-message-preview.h"
#include "hippo-canvas-timestamp.h"
#include "hippo-canvas-url-image.h"
#include "hippo-canvas-url-link.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-link.h>
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

static void hippo_canvas_block_music_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_music_unexpand (HippoCanvasBlock *canvas_block);

/* Internals */
static void hippo_canvas_block_music_update_visibility(HippoCanvasBlockMusic *block_music);

static void on_love_activated(HippoCanvasItem       *item,
                              HippoCanvasBlockMusic *block_music);
static void on_hate_activated(HippoCanvasItem       *item,
                              HippoCanvasBlockMusic *block_music);


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

#define MAX_OLD_TRACKS 3

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockMusic, hippo_canvas_block_music, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_music_iface_init));

static void
hippo_canvas_block_music_init(HippoCanvasBlockMusic *block_music)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_music);

    block->skip_heading = TRUE;
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
    HippoCanvasBox *content_box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *beside_box;
    HippoCanvasBox *quip_box;
    HippoCanvasItem *item;
    
    object = G_OBJECT_CLASS(hippo_canvas_block_music_parent_class)->constructor(type,
                                                                                n_construct_properties,
                                                                                construct_properties);
    block = HIPPO_CANVAS_BLOCK(object);
    block_music = HIPPO_CANVAS_BLOCK_MUSIC(object);
    
    hippo_canvas_block_set_heading(block, _("Music Radar"));

    content_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                               NULL);
    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(content_box));

    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           "spacing", 4,
                           NULL);
    hippo_canvas_box_append(content_box, HIPPO_CANVAS_ITEM(top_box), 0);

    block_music->thumbnail = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          NULL);
    hippo_canvas_box_append(top_box, block_music->thumbnail, 0);

    beside_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_VERTICAL,
                              NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(beside_box), 0);

    block_music->artist_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                            "actions", hippo_canvas_block_get_actions(block),
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                            NULL);
    block_music->artist_link_parent = beside_box;
    hippo_canvas_box_append(beside_box, block_music->artist_link, 0);
    
    block_music->name_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          "xalign", HIPPO_ALIGNMENT_START,
                                          "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                          NULL);
    block_music->name_link_parent = beside_box;
    hippo_canvas_box_append(beside_box, block_music->name_link, 0);

    quip_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                            "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                            "padding-top", 2,
                            NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(quip_box), 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "quiplove_icon",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    HIPPO_CANVAS_BOX(item)->clickable = TRUE; /* Hack */
    hippo_canvas_box_append(quip_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), block_music);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I love it!",
                        "tooltip", "Add a quip",
                        NULL);
    hippo_canvas_box_append(quip_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_love_activated), block_music);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "padding-left", 8,
                        "image-name", "quiphate_icon",
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
    HIPPO_CANVAS_BOX(item)->clickable = TRUE; /* Hack */
    hippo_canvas_box_append(quip_box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), block_music);

    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "padding-left", 4,
                        "text", "I hate it!",
                        "tooltip", "Add a quip",
                        NULL);
    hippo_canvas_box_append(quip_box, item, 0);
    
    block_music->single_message_preview = g_object_new(HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW,
                                                       "actions", hippo_canvas_block_get_actions(block),
                                                       NULL);

    block_music->single_message_preview_parent = beside_box;
    hippo_canvas_box_append(beside_box, block_music->single_message_preview, 0);
    
    block_music->downloads_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                          "orientation", HIPPO_ORIENTATION_VERTICAL,
                                          NULL);
    block_music->downloads_box_parent = block->right_column;
    hippo_canvas_box_append(block->right_column, HIPPO_CANVAS_ITEM(block_music->downloads_box), 0);

    block_music->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                             "actions", hippo_canvas_block_get_actions(block),
                                             NULL);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hate_activated), block_music);

    block_music->chat_preview_parent = content_box;
    hippo_canvas_box_append(content_box, block_music->chat_preview, 0);

    block_music->old_tracks_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                                               NULL);
    block_music->old_tracks_box_parent = content_box;
    hippo_canvas_box_append(content_box, HIPPO_CANVAS_ITEM(block_music->old_tracks_box), 0);
    
    hippo_canvas_block_music_update_visibility(block_music);
    
    return object;
}

static void
set_track(HippoCanvasBlockMusic *block_music,
          HippoTrack            *track)
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
        gboolean have_added_header = FALSE;
        char *url;
        const char *thumbnail_url;

        g_object_ref(track);
        
        g_object_get(G_OBJECT(track),
                     "artist", &artist,
                     "name", &name,
                     "downloads", &downloads,
                     "url", &url,
                     NULL);

        hippo_canvas_box_set_child_visible(block_music->artist_link_parent,
                                           block_music->artist_link,
                                           artist != NULL);
            
        if (artist)
            g_object_set(block_music->artist_link,
                         "text", artist,
                         "font", "12px",
                         "tooltip", "More information about this song",
                         "url", url,
                         NULL);
        
        hippo_canvas_box_set_child_visible(block_music->name_link_parent,
                                           block_music->name_link,
                                           name != NULL);
            
        if (name)
            g_object_set(block_music->name_link,
                         "text", name,
                         "font", "12px",
                         "tooltip", "More information about this song",
                         "url", url,
                         NULL);

        
        thumbnail_url = hippo_track_get_thumbnail_url(track);
        if (thumbnail_url) {
            g_object_set(block_music->thumbnail,
                         "image-name", "noart",
                         "tooltip", "More information about this song",
                         "url", url,
                         "box-width", hippo_track_get_thumbnail_width(track),
                         "box-height", hippo_track_get_thumbnail_height(track),
                         NULL);
            hippo_actions_load_music_thumbnail_async(hippo_canvas_block_get_actions(canvas_block),
                                                     thumbnail_url,
                                                     block_music->thumbnail);
        } else {
            g_object_set(block_music->thumbnail,
                         "image-name", "noart",
                         "tooltip", "More information about this song",
                         "url", url,
                         "box-width", 60,
                         "box-height", 60,
                         NULL);
        }

        hippo_canvas_box_remove_all(block_music->downloads_box);

        while (downloads) {
            HippoCanvasItem *link;
            char *link_name;
            HippoCanvasItem *separator;
            HippoSongDownload *d = downloads->data;
            downloads = downloads->next;

            if (!hippo_actions_can_play_song_download(actions, d))
                continue;
            
            if (!have_added_header) {
                HippoCanvasItem *header;
                
                header = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                      "text", "Play song in:",
                                      "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                      "xalign", HIPPO_ALIGNMENT_END,
                                      NULL);
                hippo_canvas_box_append(block_music->downloads_box,
                                        header, 0);
                have_added_header = TRUE;
            }
        
            link_name = g_strdup_printf(hippo_song_download_source_get_name(hippo_song_download_get_source(d)));
            
            link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                "actions", actions,
                                "text", link_name,
                                "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                "url", hippo_song_download_get_url(d),
                                "xalign", HIPPO_ALIGNMENT_END,
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
        
        g_free(artist);
        g_free(name);
        g_free(url);
    } else {
        hippo_canvas_block_set_title(canvas_block, NULL, NULL, FALSE);
        hippo_canvas_box_remove_all(block_music->downloads_box);
    }

#if 0
    /* Setting the chat-id gives us a "Chat" link; that can be useful for
     * debugging, but if we want to expose that to the user, we'd probably
     * want to move the chat link up to the Love It / Hate it quip line.
     */
    g_object_set(G_OBJECT(block_music->chat_preview),
                 "chat-id", chat_id,
                 NULL);
#endif
}

static void
set_old_tracks(HippoCanvasBlockMusic *block_music,
               GSList                *old_tracks)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    GSList *old_old_tracks = block_music->old_tracks;
    GSList *l;
    int count = 0;
    
    block_music->old_tracks = g_slist_copy(old_tracks);
    g_slist_foreach(old_tracks, (GFunc)g_object_ref, NULL);
    
    g_slist_foreach(old_old_tracks, (GFunc)g_object_unref, NULL);
    g_slist_free(old_old_tracks);

    hippo_canvas_box_remove_all(block_music->old_tracks_box);
    for (l = block_music->old_tracks; l && count < MAX_OLD_TRACKS; l = l->next) {
        HippoTrack *old_track = l->data;
        const char *artist = hippo_track_get_artist(old_track);
        const char *name = hippo_track_get_name(old_track);
        const char *url = hippo_track_get_url(old_track);
        GTime last_listen_time = hippo_track_get_last_listen_time(old_track);
        HippoCanvasBox *track_box;
        HippoCanvasItem *item;

        if (artist == NULL && name == NULL)
            name = "Unknown Song";

        track_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                 "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                 NULL);

        hippo_canvas_box_append(block_music->old_tracks_box, HIPPO_CANVAS_ITEM(track_box), 0);

        if (artist) {
            item = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                "actions", hippo_canvas_block_get_actions(canvas_block),
                                "text", artist,
                                "tooltip", "More information about this song",
                                "url", url,
                                "xalign", HIPPO_ALIGNMENT_START,
                                NULL);
            hippo_canvas_box_append(track_box, item, 0);
        }

        if (artist && name) {
            item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                "text", " - ",
                                "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                "xalign", HIPPO_ALIGNMENT_START,
                                NULL);
            hippo_canvas_box_append(track_box, item, 0);
        }

        if (name) {
            item = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                "actions", hippo_canvas_block_get_actions(canvas_block),
                                "text", name,
                                "tooltip", "More information about this song",
                                "url", url,
                                "xalign", HIPPO_ALIGNMENT_START,
                                NULL);
            hippo_canvas_box_append(track_box, item, 0);
        }

        item = g_object_new(HIPPO_TYPE_CANVAS_TIMESTAMP,
                            "actions", hippo_canvas_block_get_actions(canvas_block),
                            "time", last_listen_time,
                            "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                            "xalign", HIPPO_ALIGNMENT_START,
                            "padding-left", 6,
                            NULL);
        hippo_canvas_box_append(track_box, item, 0);

        count++;
    }
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
    
    hippo_canvas_box_set_child_visible(block_music->single_message_preview_parent,
                                       block_music->single_message_preview,
                                       !canvas_block->expanded && block_music->have_messages);
    hippo_canvas_box_set_child_visible(block_music->chat_preview_parent,
                                       block_music->chat_preview,
                                       canvas_block->expanded);
    hippo_canvas_box_set_child_visible(block_music->downloads_box_parent,
                                       HIPPO_CANVAS_ITEM(block_music->downloads_box),
                                       canvas_block->expanded);
    hippo_canvas_box_set_child_visible(block_music->old_tracks_box_parent,
                                       HIPPO_CANVAS_ITEM(block_music->old_tracks_box),
                                       canvas_block->expanded);
}

static void
quip_on(HippoCanvasBlockMusic *block_music,
        HippoSentiment         sentiment)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    const char *play_id;
    char *title;
    
    if (!block_music->track)
        return;

    play_id = hippo_track_get_play_id(block_music->track);
    if (!play_id)
        return;

    title = hippo_track_get_display_title(block_music->track);
    
    hippo_actions_quip(hippo_canvas_block_get_actions(canvas_block),
                       HIPPO_CHAT_KIND_MUSIC, play_id,
                       sentiment, title);

    g_free(title);
}

static void
on_love_activated(HippoCanvasItem       *item,
                  HippoCanvasBlockMusic *block_music)
{
    quip_on(block_music, HIPPO_SENTIMENT_LOVE);
}

static void
on_hate_activated(HippoCanvasItem       *item,
                  HippoCanvasBlockMusic *block_music)
{
    quip_on(block_music, HIPPO_SENTIMENT_HATE);
}
