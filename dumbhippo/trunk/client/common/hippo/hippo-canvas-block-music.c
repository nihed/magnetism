/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>

#include "hippo-common-internal.h"
#include <hippo/hippo-person.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-music.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-quipper.h"
#include "hippo-canvas-timestamp.h"
#include "hippo-canvas-url-image.h"
#include "hippo-canvas-url-link.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image-button.h>
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

/* Canvas block methods */
static void hippo_canvas_block_music_append_content_items (HippoCanvasBlock *canvas_block,
                                                           HippoCanvasBox   *parent_box);
static void hippo_canvas_block_music_append_right_items   (HippoCanvasBlock *canvas_block,
                                                           HippoCanvasBox   *parent_box);
static void hippo_canvas_block_music_set_block       (HippoCanvasBlock *canvas_block,
                                                      HippoBlock       *block);

static void hippo_canvas_block_music_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_music_unexpand (HippoCanvasBlock *canvas_block);

/* Internals */
static void hippo_canvas_block_music_update_visibility(HippoCanvasBlockMusic *block_music);

struct _HippoCanvasBlockMusic {
    HippoCanvasBlock canvas_block;
    HippoPerson *person;

    HippoTrack *track;
    DDMFeed *tracks_feed;
    DDMDataResource *track_resource;

    HippoCanvasItem *thumbnail;
    HippoCanvasItem *artist_link;
    HippoCanvasItem *name_link;
    HippoCanvasItem *quipper;
    HippoCanvasItem *last_message_preview;
    HippoCanvasItem *chat_preview;

    HippoCanvasBox *downloads_box;
    
    HippoCanvasBox *old_tracks_box;
};

struct _HippoCanvasBlockMusicClass {
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

    object_class->dispose = hippo_canvas_block_music_dispose;
    object_class->finalize = hippo_canvas_block_music_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_music_append_content_items;
    canvas_block_class->append_right_items = hippo_canvas_block_music_append_right_items;
    canvas_block_class->set_block = hippo_canvas_block_music_set_block;
    canvas_block_class->expand = hippo_canvas_block_music_expand;
    canvas_block_class->unexpand = hippo_canvas_block_music_unexpand;
}

static void
hippo_canvas_block_music_dispose(GObject *object)
{
    HippoCanvasBlockMusic *block_music;

    block_music = HIPPO_CANVAS_BLOCK_MUSIC(object);

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

static void
hippo_canvas_block_music_append_content_items (HippoCanvasBlock *block,
                                               HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(block);
    HippoCanvasBox *box;
    HippoCanvasBox *top_box;
    HippoCanvasBox *beside_box;
    
    hippo_canvas_block_set_heading(block, _("Music Radar"));

    top_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                           "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                           "spacing", 4,
                           "border-bottom", 3,
                           NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(top_box), 0);

    block_music->thumbnail = g_object_new(HIPPO_TYPE_CANVAS_URL_IMAGE,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          "xalign", HIPPO_ALIGNMENT_CENTER,
                                          "yalign", HIPPO_ALIGNMENT_CENTER,
                                          NULL);
    hippo_canvas_box_append(top_box, block_music->thumbnail, 0);

    beside_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                              "orientation", HIPPO_ORIENTATION_VERTICAL,
                              NULL);
    hippo_canvas_box_append(top_box, HIPPO_CANVAS_ITEM(beside_box), 0);

    /* An extra box to keep the artist link from expanding beyond its text */
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);

    block_music->artist_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, 
                                            "actions", hippo_canvas_block_get_actions(block),
                                            "xalign", HIPPO_ALIGNMENT_START,
                                            "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                            NULL);
    hippo_canvas_box_append(box, block_music->artist_link, 0);
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(beside_box, HIPPO_CANVAS_ITEM(box), 0);

    block_music->name_link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          "xalign", HIPPO_ALIGNMENT_START,
                                          "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                          NULL);
    hippo_canvas_box_append(box, block_music->name_link, 0);

    block_music->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                        "actions", hippo_canvas_block_get_actions(block),
                                        "padding-top", 2,
                                        NULL);
    hippo_canvas_box_append(beside_box, block_music->quipper, 0);

    block_music->last_message_preview = g_object_new(HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW,
                                                     "actions", hippo_canvas_block_get_actions(block),
                                                     NULL);

    hippo_canvas_box_append(beside_box, block_music->last_message_preview, 0);
    
    block_music->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                             "actions", hippo_canvas_block_get_actions(block),
                                             NULL);

    hippo_canvas_box_append(parent_box, block_music->chat_preview, 0);

    block_music->old_tracks_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                                               NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(block_music->old_tracks_box), 0);
    
    hippo_canvas_block_music_update_visibility(block_music);
}

static void
hippo_canvas_block_music_append_right_items (HippoCanvasBlock *block,
                                             HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(block);

    block_music->downloads_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                          "orientation", HIPPO_ORIENTATION_VERTICAL,
                                          NULL);
    hippo_canvas_box_append(parent_box, HIPPO_CANVAS_ITEM(block_music->downloads_box), HIPPO_PACK_FLOAT_RIGHT);
}

static void
set_track(HippoCanvasBlockMusic *block_music,
          DDMDataResource       *track_resource)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    HippoActions *actions = hippo_canvas_block_get_actions(canvas_block);
    char *title = NULL;

    if (track_resource == block_music->track_resource)
        return;

    if (block_music->track_resource) {
        ddm_data_resource_unref(block_music->track_resource);
    }

    block_music->track_resource = track_resource;
    
    if (block_music->track_resource) {
        ddm_data_resource_ref(block_music->track_resource);
    }
    
    if (track_resource != NULL) {
        const char *artist;
        const char *name;
        const char *link;
        const char *image_url;
        GSList *downloads;
        GSList *sorted_downloads;
        GSList *l;
        int image_width;
        int image_height;
        gboolean have_added_header = FALSE;

        ddm_data_resource_get(track_resource,
                              "artist", DDM_DATA_STRING, &artist,
                              "name", DDM_DATA_STRING, &name,
                              "link", DDM_DATA_URL, &link,
                              "imageUrl", DDM_DATA_URL, &image_url,
                              "imageWidth", DDM_DATA_INTEGER, &image_width,
                              "imageHeight", DDM_DATA_INTEGER, &image_height,
                              "downloads", DDM_DATA_STRING | DDM_DATA_LIST, &downloads,
                              NULL);

        hippo_canvas_item_set_visible(block_music->artist_link,
                                      artist != NULL);
            
        if (artist)
            g_object_set(block_music->artist_link,
                         "text", artist,
                         "font", "12px",
                         "tooltip", "More information about this song",
                         "url", link,
                         NULL);
        
        hippo_canvas_item_set_visible(block_music->name_link,
                                      name != NULL);
            
        if (name)
            g_object_set(block_music->name_link,
                         "text", name,
                         "font", "12px",
                         "tooltip", "More information about this song",
                         "url", link,
                         NULL);

        
        if (image_url) {
            g_object_set(block_music->thumbnail,
                         "image-name", "noart",
                         "tooltip", "More information about this song",
                         "url", link,
                         "box-width", image_width,
                         "box-height", image_height,
                         NULL);
            hippo_actions_load_music_thumbnail_async(hippo_canvas_block_get_actions(canvas_block),
                                                     image_url,
                                                     block_music->thumbnail);
        } else {
            g_object_set(block_music->thumbnail,
                         "image-name", "noart",
                         "tooltip", "More information about this song",
                         "url", link,
                         "box-width", 60,
                         "box-height", 60,
                         NULL);
        }

        hippo_canvas_box_remove_all(block_music->downloads_box);

        sorted_downloads = g_slist_sort(g_slist_copy(downloads), (GCompareFunc)strcmp);

        for (l = sorted_downloads; l; l = l->next) {
            HippoSongDownload *download;
            HippoCanvasItem *link;
            char *link_name;
            
            download = hippo_song_download_new_from_string(l->data);
            if (!download)
                goto next;

            if (!hippo_actions_can_play_song_download(actions, download))
                goto next;

            if (!have_added_header) {
                HippoCanvasItem *header;
                
                header = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                      "text", "Play song with:",
                                      "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                      "xalign", HIPPO_ALIGNMENT_END,
                                      NULL);
                hippo_canvas_box_append(block_music->downloads_box,
                                        header, 0);
                have_added_header = TRUE;
            }
        
            link_name = g_strdup_printf(hippo_song_download_source_get_name(hippo_song_download_get_source(download)));
            
            link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK,
                                "actions", actions,
                                "text", link_name,
                                "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                "url", hippo_song_download_get_url(download),
                                "xalign", HIPPO_ALIGNMENT_END,
                                NULL);

            g_free(link_name);

            hippo_canvas_box_append(block_music->downloads_box,
                                    link, 0);

        next:
            hippo_song_download_free(download);
        }

        g_slist_free(sorted_downloads);
        
        if (artist && name)
            title = g_strdup_printf("%s - %s", artist, name);
        else if (artist)
            title = g_strdup(artist);
        else if (name)
            title = g_strdup(name);
        else
            title = g_strdup("Unknown Song");
    } else {
        hippo_canvas_block_set_title(canvas_block, NULL, NULL, FALSE);
        hippo_canvas_box_remove_all(block_music->downloads_box);
    }

    g_object_set(G_OBJECT(block_music->quipper),
                 "title", title,
                 NULL);

    g_free(title);
}

static void
add_old_track(HippoCanvasBlockMusic *block_music,
              DDMDataResource       *track_resource,
              gint64                 play_time)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);
    
    const char *artist;
    const char *name;
    const char *link;
    
    HippoCanvasBox *track_box;
    HippoCanvasItem *item;

    ddm_data_resource_get(track_resource,
                          "artist", DDM_DATA_STRING, &artist,
                          "name", DDM_DATA_STRING, &name,
                          "link", DDM_DATA_URL, &link,
                          NULL);

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
                            "url", link,
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
                            "url", link,
                            "xalign", HIPPO_ALIGNMENT_START,
                            "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                            NULL);
        hippo_canvas_box_append(track_box, item, 0);
    }
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TIMESTAMP,
                        "actions", hippo_canvas_block_get_actions(canvas_block),
                        "time", (int)(play_time / 1000),
                        "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                        "xalign", HIPPO_ALIGNMENT_START,
                        "padding-left", 6,
                        NULL);
    hippo_canvas_box_append(track_box, item, 0);
}

static void
update_tracks(HippoCanvasBlockMusic *block_music)
{
    DDMDataResource *track_resource = NULL;

    hippo_canvas_box_remove_all(block_music->old_tracks_box);
        
    if (block_music->tracks_feed != NULL) {
        DDMFeedIter iter;

        ddm_feed_iter_init(&iter, block_music->tracks_feed);
        if (ddm_feed_iter_next(&iter, &track_resource, NULL)) {
            DDMDataResource *old_resource;
            gint64 old_play_time;

            while (ddm_feed_iter_next(&iter, &old_resource, &old_play_time)) {
                add_old_track(block_music, old_resource, old_play_time);
            }
        }
    } else {
        HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_music);

        if (canvas_block->block)
            ddm_data_resource_get(hippo_block_get_resource(canvas_block->block),
                                  "track", DDM_DATA_RESOURCE, &track_resource,
                                  NULL);
    }
    
    set_track(block_music, track_resource);
}

static void
on_block_chat_id_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         void       *data)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(data);
    
    hippo_canvas_block_music_update_visibility(block_music);
}

static void
on_track_changed(DDMDataResource *block_resource,
                 GSList          *changed_properties,
                 gpointer         data)
{
    update_tracks(data);
}

static void
on_item_added(DDMFeed               *feed,
              DDMDataResource       *resource,
              gint64                 timestamp,
              HippoCanvasBlockMusic *block_music)
{
    update_tracks(block_music);
}
              
static void
on_item_changed(DDMFeed               *feed,
                DDMDataResource       *resource,
                gint64                 timestamp,
                HippoCanvasBlockMusic *block_music)
{
    update_tracks(block_music);
}
              
static void
on_item_removed(DDMFeed               *feed,
                DDMDataResource       *resource,
                HippoCanvasBlockMusic *block_music)
{
    update_tracks(block_music);
}
              
static void
on_tracks_changed(DDMDataResource *block_resource,
                  GSList          *changed_properties,
                  gpointer         data)
{
    HippoCanvasBlockMusic *block_music = data;
    DDMFeed *tracks_feed = NULL;

    if (block_resource)
        ddm_data_resource_get(block_resource,
                              "tracks", DDM_DATA_FEED, &tracks_feed,
                              NULL);

    if (tracks_feed == block_music->tracks_feed)
        return;

    if (block_music->tracks_feed) {
        g_signal_handlers_disconnect_by_func(block_music->tracks_feed,
                                             (gpointer)on_item_added,
                                             block_music);
        g_signal_handlers_disconnect_by_func(block_music->tracks_feed,
                                             (gpointer)on_item_changed,
                                             block_music);
        g_signal_handlers_disconnect_by_func(block_music->tracks_feed,
                                             (gpointer)on_item_removed,
                                             block_music);
        
        g_object_unref(block_music->tracks_feed);
    }

    block_music->tracks_feed = tracks_feed;

    if (block_music->tracks_feed) {
        g_object_ref(block_music->tracks_feed);
        
        g_signal_connect(block_music->tracks_feed, "item-added",
                         G_CALLBACK(on_item_added), block_music);
        g_signal_connect(block_music->tracks_feed, "item-changed",
                         G_CALLBACK(on_item_changed), block_music);
        g_signal_connect(block_music->tracks_feed, "item-removed",
                         G_CALLBACK(on_item_removed), block_music);
    }
    
    update_tracks(block_music);
}

static void
on_owner_changed(DDMDataResource *block_resource,
                 GSList          *changed_properties,
                 gpointer         data)
{
    HippoCanvasBlockMusic *block_music = data;
    DDMDataResource *owner_resource = NULL;
    HippoPerson *owner = NULL;

    if (block_resource)
        ddm_data_resource_get(block_resource,
                              "owner", DDM_DATA_RESOURCE, &owner_resource,
                              NULL);

    if (owner_resource != NULL)
        owner = hippo_person_get_for_resource(owner_resource);

    hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(block_music),
                                  owner ? HIPPO_ENTITY(owner) : NULL);

    if (owner != NULL)
        g_object_unref(owner);
}

static void
hippo_canvas_block_music_set_block(HippoCanvasBlock *canvas_block,
                                   HippoBlock       *block)
{
    HippoCanvasBlockMusic *block_music = HIPPO_CANVAS_BLOCK_MUSIC(canvas_block);

    /* g_debug("canvas-block-music set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_chat_id_changed),
                                             canvas_block);
        ddm_data_resource_disconnect(hippo_block_get_resource(canvas_block->block),
                                     on_track_changed, block_music);
        ddm_data_resource_disconnect(hippo_block_get_resource(canvas_block->block),
                                     on_tracks_changed, block_music);
        ddm_data_resource_disconnect(hippo_block_get_resource(canvas_block->block),
                                     on_owner_changed, block_music);
    }

    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_music_parent_class)->set_block(canvas_block, block);

    g_object_set(G_OBJECT(block_music->quipper),
                 "block", canvas_block->block,
                 NULL);
    g_object_set(G_OBJECT(block_music->last_message_preview),
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    g_object_set(G_OBJECT(block_music->chat_preview),
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::chat-id",
                         G_CALLBACK(on_block_chat_id_changed),
                         canvas_block);
        
        ddm_data_resource_connect(hippo_block_get_resource(canvas_block->block),
                                  "track", on_track_changed, block_music);
        ddm_data_resource_connect(hippo_block_get_resource(canvas_block->block),
                                  "tracks", on_tracks_changed, block_music);
        ddm_data_resource_connect(hippo_block_get_resource(canvas_block->block),
                                  "owner", on_owner_changed, block_music);

    }
    
    on_track_changed(canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL, NULL, block_music);
    on_tracks_changed(canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL, NULL, block_music);
    on_owner_changed(canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL, NULL, block_music);
    hippo_canvas_block_music_update_visibility(block_music);
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
    HippoStackReason stack_reason;
    
    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    hippo_canvas_item_set_visible(block_music->last_message_preview,
                                  !canvas_block->expanded && stack_reason == HIPPO_STACK_CHAT_MESSAGE);
    hippo_canvas_item_set_visible(block_music->chat_preview,
                                  canvas_block->expanded);
    hippo_canvas_item_set_visible(HIPPO_CANVAS_ITEM(block_music->downloads_box),
                                  canvas_block->expanded);
    hippo_canvas_item_set_visible(HIPPO_CANVAS_ITEM(block_music->old_tracks_box),
                                  canvas_block->expanded);
}
