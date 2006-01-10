#pragma once
#include "HippoMusicMonitor.h"

class HippoITunesMonitorImpl;

class HippoITunesMonitor :
	public HippoMusicMonitor
{
public:
	HippoITunesMonitor();
	virtual ~HippoITunesMonitor();

	virtual bool hasCurrentTrack() const;
	virtual const HippoTrackInfo& getCurrentTrack() const;

private:
	friend class HippoITunesMonitorImpl;
	HippoPtr<HippoITunesMonitorImpl> impl_;
};
