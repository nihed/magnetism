#pragma once
#include "HippoMusicMonitor.h"

class HippoITunesMonitorImpl;

class HippoITunesMonitor :
    public HippoPlaylistSource
{
public:
    HippoITunesMonitor();
    virtual ~HippoITunesMonitor();

    virtual void setEnabled(bool enabled);
    virtual bool hasCurrentTrack() const;
    virtual const HippoTrackInfo& getCurrentTrack() const;

    virtual std::vector<HippoPtr<HippoPlaylist> > getPlaylists() const;
    virtual HippoPtr<HippoPlaylist> getPrimingTracks() const;

private:
    friend class HippoITunesMonitorImpl;
    mutable HippoPtr<HippoITunesMonitorImpl> impl_;
};
