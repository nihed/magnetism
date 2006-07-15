#include "StdAfx.h"
#include "HippoMusicMonitor.h"
#include "HippoITunesMonitor.h"
#include "HippoYahooMonitor.h"
#include <glib-object.h>
#include "HippoUIUtil.h"

void
HippoTrackInfo::toGStringVectors(char ***keysReturn,
                                 char ***valuesReturn) const
{
    int    i;
    char **keys;
    char **values;
    int    n_props_check;

    n_props_check = 0;

#define ADD_PROP(lower, upper)              \
  do {                                      \
    ++n_props_check;                        \
    if (has ## upper ()) {                  \
        HippoUStr value(get ## upper ());   \
        keys[i] = g_strdup(#lower);         \
        values[i] = value.steal();          \
        ++i;                                \
    }                                       \
  } while(0)

    keys = g_new0(char*, HIPPO_TRACK_N_PROPERTIES + 1);
    values = g_new0(char*, HIPPO_TRACK_N_PROPERTIES + 1);

    i = 0;
    ADD_PROP(type, Type);
    ADD_PROP(format, Format);
    ADD_PROP(name, Name);
    ADD_PROP(artist, Artist);
    ADD_PROP(album, Album);
    ADD_PROP(url, Url);
    ADD_PROP(duration, Duration);
    ADD_PROP(fileSize, FileSize);
    ADD_PROP(trackNumber, TrackNumber);
    ADD_PROP(discIdentifier, DiscIdentifier);

    g_assert(n_props_check == HIPPO_TRACK_N_PROPERTIES);

    *keysReturn = keys;
    *valuesReturn = values;
}

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

void
HippoMusicMonitor::fireMusicAppRunning(bool nowRunning)
{
    for (unsigned int i = 0; i < listeners_.length(); ++i) {
        listeners_[i]->onMusicAppRunning(this, nowRunning);
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
