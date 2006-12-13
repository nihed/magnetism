/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-common-internal.h"
#include "hippo-data-cache.h"
#include "hippo-track.h"
#include "hippo-xml-utils.h"

static void     hippo_track_finalize             (GObject *object);

static void hippo_track_set_property (GObject      *object,
                                      guint         prop_id,
                                      const GValue *value,
                                      GParamSpec   *pspec);
static void hippo_track_get_property (GObject      *object,
                                      guint         prop_id,
                                      GValue       *value,
                                      GParamSpec   *pspec);

static HippoSongDownload *hippo_song_download_new_from_xml (HippoDataCache    *cache,
                                                            LmMessageNode     *node);
static void               hippo_song_download_free         (HippoSongDownload *download);

struct _HippoTrack {
    GObject parent;
    
    char *artist;
    char *album;
    char *name;
    char *play_id;
    char *url;
    gint64 last_listen_time;
    gint64 duration;
    gboolean now_playing;
    GSList *downloads;

    guint track_end_timeout;
};

struct _HippoTrackClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoTrack, hippo_track, G_TYPE_OBJECT);

/*
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
*/

enum {
    PROP_0,
    PROP_ARTIST,
    PROP_ALBUM,
    PROP_NAME,
    PROP_PLAY_ID,
    PROP_URL,
    PROP_NOW_PLAYING,
    PROP_DOWNLOADS
};

static void
hippo_track_init(HippoTrack *track)
{
}

static void
hippo_track_class_init(HippoTrackClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
          
    object_class->finalize = hippo_track_finalize;

    object_class->set_property = hippo_track_set_property;
    object_class->get_property = hippo_track_get_property;

    g_object_class_install_property(object_class,
                                    PROP_ARTIST,
                                    g_param_spec_string("artist",
                                                        _("Artist"),
                                                        _("Name of artist"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_ALBUM,
                                    g_param_spec_string("album",
                                                        _("Album"),
                                                        _("Name of album"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_NAME,
                                    g_param_spec_string("name",
                                                        _("Name"),
                                                        _("Name of track"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_URL,
                                    g_param_spec_string("url",
                                                        _("URL"),
                                                        _("An URL for more information about the track"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_PLAY_ID,
                                    g_param_spec_string("play-id",
                                                        _("Play Id"),
                                                        _("An ID for this play of the track"),
                                                        NULL,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_NOW_PLAYING,
                                    g_param_spec_boolean("now-playing",
                                                         _("Now Playing"),
                                                         _("Whether the track is currently playing"),
                                                         FALSE,
                                                         G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_DOWNLOADS,
                                    g_param_spec_pointer("downloads",
                                                         _("Downloads"),
                                                         _("List of download links"),
                                                         G_PARAM_READABLE));
}

static void
hippo_track_finalize(GObject *object)
{
    HippoTrack *track = HIPPO_TRACK(object);

    if (track->track_end_timeout)
        g_source_remove(track->track_end_timeout);

    g_free(track->artist);
    g_free(track->album);
    g_free(track->name);
    g_free(track->url);
    g_free(track->play_id);

    g_slist_foreach(track->downloads, (GFunc)hippo_song_download_free, NULL);
    g_slist_free(track->downloads);

    G_OBJECT_CLASS(hippo_track_parent_class)->finalize(object); 
}

static void
hippo_track_set_property(GObject         *object,
                         guint            prop_id,
                         const GValue    *value,
                         GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_track_get_property(GObject         *object,
                         guint            prop_id,
                         GValue          *value,
                         GParamSpec      *pspec)
{
    HippoTrack *track;

    track = HIPPO_TRACK(object);

    switch (prop_id) {
    case PROP_ARTIST:
        g_value_set_string(value, track->artist);
        break;
    case PROP_ALBUM:
        g_value_set_string(value, track->album);
        break;
    case PROP_NAME:
        g_value_set_string(value, track->name);
        break;
    case PROP_PLAY_ID:
        g_value_set_string(value, track->play_id);
        break;
    case PROP_URL:
        g_value_set_string(value, track->url);
        break;
    case PROP_NOW_PLAYING:
        g_value_set_boolean(value, track->now_playing);
        break;        
    case PROP_DOWNLOADS:
        g_value_set_pointer(value, track->downloads);
        break;        
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
track_ended(gpointer data)
{
    HippoTrack *track = data;

    track->now_playing = FALSE;
    g_object_notify(G_OBJECT(track), "now-playing");
    
    track->track_end_timeout = 0;
    
    return FALSE;
}

/* === HippoTrack exported API === */

HippoTrack *
hippo_track_new_from_xml(HippoDataCache *cache,
                         LmMessageNode  *node)
{
    GSList *downloads = NULL;
    const char *artist;
    const char *album;
    const char *name;
    gint64 last_listen_time;
    gint64 duration;
    gboolean now_playing;
    HippoTrack *track;
    LmMessageNode *child;
    const char *play_id = NULL;
    const char *url = NULL;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "artist", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &artist,
                         "album", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &album,
                         "name", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &name,
                         "playId", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &play_id,
                         "url", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &url,
                         "lastListenTime", HIPPO_SPLIT_TIME_MS, &last_listen_time,
                         "duration", HIPPO_SPLIT_TIME_MS, &duration,
                         "nowPlaying", HIPPO_SPLIT_BOOLEAN, &now_playing,
                         NULL))
        return NULL;

    for (child = node->children; child; child = child->next) {
        HippoSongDownload *download;
        
        if (strcmp(child->name, "download") != 0)
            continue;

        download = hippo_song_download_new_from_xml(cache, child);
        if (!download)
            continue;

        downloads = g_slist_prepend(downloads, download);
    }

    downloads = g_slist_reverse(downloads);

    track = g_object_new(HIPPO_TYPE_TRACK, NULL);

    track->artist = g_strdup(artist);
    track->album = g_strdup(album);
    track->name = g_strdup(name);
    track->url = g_strdup(url);
    track->last_listen_time = last_listen_time;
    track->duration = duration;
    track->downloads = downloads;

    if (now_playing) {
        HippoConnection *connection = hippo_data_cache_get_connection(cache);
        gint64 server_time_offset = hippo_connection_get_server_time_offset(connection);
        gint64 server_time_now = hippo_current_time_ms() + server_time_offset;
        gint64 server_end_time = last_listen_time + duration;

        if (server_end_time <= server_time_now)
            now_playing = FALSE;
        else {
            track->track_end_timeout = g_timeout_add((guint)server_end_time - server_time_now,
                                                     track_ended,
                                                     track);
        }
    }

    track->now_playing = now_playing;

    return track;
}

/* This can die when the old xmpp protocol stuff using it dies */
HippoTrack*
hippo_track_new_deprecated (const char *artist,
                            const char *name,
                            gboolean    now_playing)
{
    HippoTrack *track;
    
    track = g_object_new(HIPPO_TYPE_TRACK, NULL);

    track->artist = g_strdup(artist);
    track->album = NULL;
    track->name = g_strdup(name);
    track->last_listen_time = 0;
    track->duration = 0;
    track->downloads = NULL;
    track->now_playing = now_playing;

    return track;
}

const char*
hippo_track_get_artist (HippoTrack *track)
{
    return track->artist;
}

const char*
hippo_track_get_name (HippoTrack *track)
{
    return track->name;
}

const char*
hippo_track_get_play_id (HippoTrack *track)
{
    return track->play_id;
}

const char*
hippo_track_get_url (HippoTrack *track)
{
    return track->url;
}

gboolean
hippo_track_get_now_playing (HippoTrack *track)
{
    return track->now_playing;
}


/* === HippoSongDownload === */

static gboolean
song_download_source_from_string(const char              *s,
                                 HippoSongDownloadSource *result)
{
    static const struct { const char *name; HippoSongDownloadSource source; } sources[] = {
        { "ITUNES", HIPPO_SONG_DOWNLOAD_ITUNES },
        { "RHAPSODY", HIPPO_SONG_DOWNLOAD_RHAPSODY },
        { "YAHOO", HIPPO_SONG_DOWNLOAD_YAHOO  }
    };
    unsigned int i;
    for (i = 0; i < G_N_ELEMENTS(sources); ++i) {
        if (strcmp(s, sources[i].name) == 0) {
            *result = sources[i].source;
            return TRUE;
        }
    }
    g_warning("Unknown song download source '%s'", s);
    return FALSE;
}

struct _HippoSongDownload {
    HippoSongDownloadSource source;
    char *url;
};

static HippoSongDownload *
hippo_song_download_new_from_xml(HippoDataCache *cache,
                                 LmMessageNode  *node)
{
    HippoSongDownload *download;
    const char *source_str;
    const char *url;
    HippoSongDownloadSource source;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "source", HIPPO_SPLIT_STRING, &source_str,
                         "url", HIPPO_SPLIT_URI_ABSOLUTE, &url,
                         NULL))
        return NULL;

    if (!song_download_source_from_string(source_str, &source))
        return NULL;

    download = g_new(HippoSongDownload, 1);
    download->source = source;
    download->url = g_strdup(url);

    return download;
}
    
HippoSongDownloadSource
hippo_song_download_get_source(HippoSongDownload *download)
{
    return download->source;
}

const char *
hippo_song_download_get_url(HippoSongDownload *download)
{
    return download->url;
}

static void
hippo_song_download_free(HippoSongDownload *download)
{
    g_free(download->url);
    g_free(download);
}

const char*
hippo_song_download_source_get_name(HippoSongDownloadSource source)
{
    switch (source) {
    case HIPPO_SONG_DOWNLOAD_ITUNES:
        return "iTunes";
    case HIPPO_SONG_DOWNLOAD_YAHOO:
        return "Yahoo!";
    case HIPPO_SONG_DOWNLOAD_RHAPSODY:
        return "Rhapsody";
    }

    return "???";
}
