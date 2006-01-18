#include "StdAfx.h"
#include "HippoMusic.h"
#include "HippoUI.h"

HippoMusic::HippoMusic()
    : ui_(0), iTunes_(0), yahoo_(0)
{

}

HippoMusic::~HippoMusic()
{
    if (iTunes_ != 0) {
        iTunes_->removeListener(this);
	    delete iTunes_;
    }

    if (yahoo_ != 0) {
        yahoo_->removeListener(this);
        delete yahoo_;
    }
}

void
HippoMusic::setUI(HippoUI *ui)
{
    ui_ = ui;
    if (iTunes_ == 0) {
        iTunes_ = HippoMusicMonitor::createITunesMonitor();
        iTunes_->addListener(this);
    }

    if (yahoo_ == 0) {
        yahoo_ = HippoMusicMonitor::createYahooMonitor();
        yahoo_->addListener(this);
    }

}

void
HippoMusic::onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack)
{
    hippoDebugLogW(L"Track changed notification");
    ui_->onCurrentTrackChanged(haveTrack, newTrack);
}
