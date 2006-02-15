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

    updateEnabled();
}

void
HippoMusic::setEnabled(bool enabled)
{
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
    ui_->onCurrentTrackChanged(haveTrack, newTrack);
}

void
HippoMusic::onMusicAppRunning(HippoMusicMonitor *monitor, bool nowRunning)
{
    hippoDebugLogW(L"Music app running = %d", (int) nowRunning);
    if (nowRunning && ui_->getNeedPrimingTracks() && monitor == iTunes_) {
        HippoPtr<HippoPlaylist> tracks = iTunes_->getPrimingTracks();
        if (tracks != 0)
            ui_->providePrimingTracks(tracks);
        else
            hippoDebugLogW(L"Failed to get priming tracks from iTunes");
    }
}
