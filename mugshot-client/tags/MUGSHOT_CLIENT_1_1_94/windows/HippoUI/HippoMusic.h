#pragma once
#include "HippoMusicMonitor.h"
#include "HippoGSignal.h"

class HippoUI;

/*
 * Creates and manages all the music monitor thingies, for now only iTunes is supported 
 */
class HippoMusic
    : public HippoMusicListener
{
public:
    HippoMusic();
    ~HippoMusic();

    void setUI(HippoUI *ui);

    ////// HippoMusicListener methods
    virtual void onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack);
    virtual void onMusicAppRunning(HippoMusicMonitor *monitor, bool nowRunning);

private:
    bool enabled_;
    HippoUI *ui_;
    HippoPtr<HippoPlaylistSource> iTunes_;
    HippoPtr<HippoMusicMonitor> yahoo_;

    GConnection0<void> musicSharingChanged_;

    void updateEnabled();

    //// signal handlers
    void onMusicSharingChanged();
};
