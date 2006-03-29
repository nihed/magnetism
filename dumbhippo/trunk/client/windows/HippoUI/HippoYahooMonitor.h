#pragma once
#include "hippomusicmonitor.h"

class HippoYahooMonitor :
    public HippoMusicMonitor
{
public:
    HippoYahooMonitor();
    virtual ~HippoYahooMonitor();

    virtual void setEnabled(bool enabled);
    virtual bool hasCurrentTrack() const;
    virtual const HippoTrackInfo& getCurrentTrack() const;

private:
    friend class HippoYahooMonitorImpl;
    HippoPtr<HippoYahooMonitorImpl> impl_;
};
