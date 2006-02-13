#pragma once
#include "HippoMusicMonitor.h"

class HippoITunesMonitorImpl;

class HippoITunesMonitor :
	public HippoPlaylistSource
{
public:
	HippoITunesMonitor();
	virtual ~HippoITunesMonitor();

	virtual bool hasCurrentTrack() const;
	virtual const HippoTrackInfo& getCurrentTrack() const;

    virtual std::vector<HippoPlaylist::Id> getPlaylists() const;
    virtual HippoPtr<HippoPlaylist> getPlaylist(const HippoPlaylist::Id &id) const;
    virtual HippoPtr<HippoPlaylist> getPrimingTracks() const;

private:
	friend class HippoITunesMonitorImpl;
	HippoPtr<HippoITunesMonitorImpl> impl_;
};
