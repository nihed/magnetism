/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_TRACK_H__
#define __HIPPO_TRACK_H__

#include <hippo/hippo-entity.h>
#include <loudmouth/loudmouth.h>

G_BEGIN_DECLS

typedef struct _HippoTrack      HippoTrack;
typedef struct _HippoTrackClass HippoTrackClass;

#define HIPPO_TYPE_TRACK              (hippo_track_get_type ())
#define HIPPO_TRACK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_TRACK, HippoTrack))
#define HIPPO_TRACK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_TRACK, HippoTrackClass))
#define HIPPO_IS_TRACK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_TRACK))
#define HIPPO_IS_TRACK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_TRACK))
#define HIPPO_TRACK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_TRACK, HippoTrackClass))

GType hippo_track_get_type(void) G_GNUC_CONST;

HippoTrack* hippo_track_new_from_xml   (HippoDataCache *cache,
                                        LmMessageNode  *node);
HippoTrack* hippo_track_new_deprecated (const char *artist,
                                        const char *name,
                                        gboolean    now_playing);

const char* hippo_track_get_artist           (HippoTrack *track);
const char* hippo_track_get_name             (HippoTrack *track);
const char* hippo_track_get_url              (HippoTrack *track);
const char* hippo_track_get_play_id          (HippoTrack *track);
gboolean    hippo_track_get_now_playing      (HippoTrack *track);
GTime       hippo_track_get_last_listen_time (HippoTrack *track);

const char* hippo_track_get_thumbnail_url    (HippoTrack *track);
int         hippo_track_get_thumbnail_width  (HippoTrack *track);
int         hippo_track_get_thumbnail_height (HippoTrack *track);

char      * hippo_track_get_display_title    (HippoTrack *track);

typedef struct _HippoSongDownload HippoSongDownload;

typedef enum {
    HIPPO_SONG_DOWNLOAD_ITUNES,
    HIPPO_SONG_DOWNLOAD_YAHOO,
    HIPPO_SONG_DOWNLOAD_RHAPSODY
} HippoSongDownloadSource;

HippoSongDownloadSource hippo_song_download_get_source(HippoSongDownload *download);
const char* hippo_song_download_get_url(HippoSongDownload *download);
const char* hippo_song_download_source_get_name(HippoSongDownloadSource source);

G_END_DECLS

#endif /* __HIPPO_TRACK_H__ */
