#include "StdAfx.h"
#include "HippoMusicMonitor.h"
#include "HippoITunesMonitor.h"
#include "HippoYahooMonitor.h"

void
HippoMusicMonitor::addListener(HippoMusicListener *listener)
{
	listeners_.append(listener);
}

void
HippoMusicMonitor::removeListener(HippoMusicListener *listener)
{
	for (int i = listeners_.length() - 1; i >= 0; --i)
	{
		if (listeners_[i] == listener) {
			listeners_.remove(i);
			return;
		}
	}

	assert(false);
}

void 
HippoMusicMonitor::fireCurrentTrackChanged(bool haveTrack, const HippoTrackInfo & newTrack)
{
	// totally unsafe, but not fixable without refcounting on the listeners
	for (unsigned int i = 0; i < listeners_.length(); ++i) {
		listeners_[i]->onCurrentTrackChanged(this, haveTrack, newTrack);
	}
}

HippoPtr<HippoMusicMonitor>
HippoMusicMonitor::createYahooMonitor()
{
	HippoPtr<HippoMusicMonitor> m = new HippoYahooMonitor();
    m->Release();
    return m;
}

HIPPO_DEFINE_REFCOUNTING(HippoMusicMonitor);

HippoPtr<HippoPlaylistSource>
HippoPlaylistSource::createITunesMonitor()
{
    HippoPtr<HippoPlaylistSource> m = new HippoITunesMonitor();
    m->Release();
    return m;
}

HIPPO_DEFINE_REFCOUNTING(HippoPlaylist);
