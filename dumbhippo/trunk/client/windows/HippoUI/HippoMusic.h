#pragma once
#include "HippoMusicMonitor.h"

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

private:
    HippoUI *ui_;
	HippoPtr<HippoPlaylistSource> iTunes_;
    HippoPtr<HippoMusicMonitor> yahoo_;
};
