#include "StdAfx.h"
#include "HippoMusic.h"
#include "HippoUI.h"

HippoMusic::HippoMusic()
    : ui_(0), enabled_(false)
{

}

HippoMusic::~HippoMusic()
{
    if (iTunes_ != 0) {
        iTunes_->removeListener(this);
    }

    if (yahoo_ != 0) {
        yahoo_->removeListener(this);
    }
}

void
HippoMusic::setUI(HippoUI *ui)
{
    ui_ = ui;
    if (iTunes_ == 0) {
        iTunes_ = HippoPlaylistSource::createITunesMonitor();
        iTunes_->addListener(this);
    }

    if (yahoo_ == 0) {
        yahoo_ = HippoMusicMonitor::createYahooMonitor();
        yahoo_->addListener(this);
    }

    musicSharingChanged_.connect(G_OBJECT(ui_->getDataCache()), "music-sharing-changed",
        slot(this, &HippoMusic::onMusicSharingChanged));

    updateEnabled();
}

void
HippoMusic::onMusicSharingChanged()
{
    bool enabled = hippo_data_cache_get_music_sharing_enabled(ui_->getDataCache()) != FALSE;
    hippoDebugLogU("Music sharing changed, new enabled=%d", enabled);
    if (enabled == enabled_)
        return;
    enabled_ = enabled;
    updateEnabled();
}

void 
HippoMusic::updateEnabled()
{
    if (iTunes_ != 0)
        iTunes_->setEnabled(enabled_);
    if (yahoo_ != 0)
        yahoo_->setEnabled(enabled_);
}

void
HippoMusic::onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack)
{
    hippoDebugLogW(L"Track changed notification");
    if (haveTrack) {
        HippoSong song;
        newTrack.toGStringVectors(&song.keys, &song.values);
        hippo_connection_notify_music_changed(ui_->getConnection(), haveTrack, &song);
        g_strfreev(song.keys);
        g_strfreev(song.values);
    } else {
        hippo_connection_notify_music_changed(ui_->getConnection(), FALSE, NULL);
    }
}

void
HippoMusic::onMusicAppRunning(HippoMusicMonitor *monitor, bool nowRunning)
{
    hippoDebugLogW(L"Music app running = %d", (int) nowRunning);
    if (nowRunning 
        && hippo_data_cache_get_need_priming_music(ui_->getDataCache())
        && monitor == iTunes_) {
        HippoPtr<HippoPlaylist> playlist = iTunes_->getPrimingTracks();
        if (playlist != 0) {
            HippoSong *songs = g_new0(HippoSong, playlist->size());
            for (int i = 0; i < playlist->size(); ++i) {
                const HippoTrackInfo &track = playlist->getTrack(i);
                track.toGStringVectors(&songs[i].keys, &songs[i].values);
            }

            hippo_connection_provide_priming_music(ui_->getConnection(), songs, playlist->size());
            
            for (int i = 0; i < playlist->size(); ++i) {
                g_strfreev(songs[i].keys);
                g_strfreev(songs[i].values);
            }
            g_free(songs);
        } else {
            hippoDebugLogW(L"Failed to get priming tracks from iTunes");
        }
    }
}
