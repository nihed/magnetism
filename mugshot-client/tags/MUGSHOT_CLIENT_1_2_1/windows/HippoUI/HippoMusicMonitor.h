/*
 * These abstract interfaces are intended to hide iTunes/WMP/etc. (various ways someone might be playing music)
 */
#pragma once

#include <assert.h>
#include "HippoArray.h"
#include "HippoUtil.h"

#include <vector>

#define HIPPO_TRACK_INFO_PROP(prop)                                                     \
public:                                                                                 \
    bool has ## prop() const { return prop ## _.Length() > 0; }                         \
    const HippoBSTR& get ## prop() const { assert(has ## prop()); return prop ## _; }   \
    void set ## prop(const HippoBSTR& val) { prop ## _ = val; }                         \
private:                                                                                \
    HippoBSTR prop ## _

#define HIPPO_TRACK_INFO_PROP_LONG(prop)                                                \
    HIPPO_TRACK_INFO_PROP(prop);                                                        \
public:                                                                                 \
    void set ## prop(long val) { fromLong(prop ## _, val); }

// has to match Java TrackType enum
#define HIPPO_TRACK_TYPE_UNKNOWN        L"UNKNOWN"
#define HIPPO_TRACK_TYPE_FILE           L"FILE"
#define HIPPO_TRACK_TYPE_CD             L"CD"
#define HIPPO_TRACK_TYPE_NETWORK_STREAM L"NETWORK_STREAM"
#define HIPPO_TRACK_TYPE_PODCAST        L"PODCAST"

// has to match Java MediaFileFormat enum
#define HIPPO_MEDIA_FILE_FORMAT_UNKNOWN L"UNKNOWN"
#define HIPPO_MEDIA_FILE_FORMAT_MP3     L"MP3"
#define HIPPO_MEDIA_FILE_FORMAT_WMA     L"WMA"
#define HIPPO_MEDIA_FILE_FORMAT_AAC     L"AAC"

// Keep the fields here in sync with Java Track class on the server,
// and with the code that stuffs HippoTrackInfo into an XMPP message,
// with toGStringVectors() method,
// and with any music player backends...
// one exception is the Location property which doesn't leave the local 
// system since it would have no meaning on the server
class HippoTrackInfo
{
public:
    HippoTrackInfo() 
    {
    }

    // this includes only the "remote" properties,
    // i.e. excludes location
#define HIPPO_TRACK_N_PROPERTIES 10
#define HIPPO_TRACK_N_PROPERTIES_WITH_LOCAL (HIPPO_TRACK_N_PROPERTIES + 1)

    HIPPO_TRACK_INFO_PROP(Type);
    HIPPO_TRACK_INFO_PROP(Format);
    HIPPO_TRACK_INFO_PROP(Name);
    HIPPO_TRACK_INFO_PROP(Artist);
    HIPPO_TRACK_INFO_PROP(Album);
    HIPPO_TRACK_INFO_PROP(Url);
    HIPPO_TRACK_INFO_PROP_LONG(Duration);
    HIPPO_TRACK_INFO_PROP_LONG(FileSize);
    HIPPO_TRACK_INFO_PROP_LONG(TrackNumber);
    HIPPO_TRACK_INFO_PROP(DiscIdentifier);
    HIPPO_TRACK_INFO_PROP(Location);

public:
    void clear() {
        Type_ = 0;
        Format_ = 0;
        Name_ = 0;
        Artist_ = 0;
        Album_ = 0;
        Url_ = 0;
        Duration_ = 0;
        FileSize_ = 0;
        TrackNumber_ = 0;
        DiscIdentifier_ = 0;
        Location_ = 0;
    }

    bool operator==(const HippoTrackInfo &other) const {
        if (&other == this)
            return true;
        return Type_ == other.Type_ &&
            Format_ == other.Format_ &&
            Name_ == other.Name_ &&
            Artist_ == other.Artist_ &&
            Album_ == other.Album_ &&
            Url_ == other.Url_ &&
            Duration_ == other.Duration_ &&
            FileSize_ == other.FileSize_ &&
            TrackNumber_ == other.TrackNumber_ && 
            DiscIdentifier_ == other.DiscIdentifier_ && 
            Location_ == other.Location_;
    }

    bool operator!=(const HippoTrackInfo &other) const {
        return !(*this == other);
    }

    // default operator= and copy should be OK in theory...

    HippoBSTR toString() const {
        if (hasName())
            return getName();
        else if (hasArtist())
            return getArtist();
        else
            return HippoBSTR(L"[track ???]");
    }

    // free with g_strfreev
    void toGStringVectors(char ***keysReturn,
                          char ***valuesReturn) const;

private:
#define BUFSIZE 32

    void fromLong(HippoBSTR &s, long val) {
        WCHAR buf[BUFSIZE];
        StringCchPrintfW(buf, BUFSIZE, L"%ld", val);
        s = buf;
    }
};

class HippoMusicListener;

class HippoMusicMonitor
{
public:
    
    virtual void setEnabled(bool enabled) = 0;
    virtual bool hasCurrentTrack() const = 0;
    virtual const HippoTrackInfo& getCurrentTrack() const = 0;

    void addListener(HippoMusicListener *listener);
    void removeListener(HippoMusicListener *listener);

    virtual ~HippoMusicMonitor() {}

    static HippoPtr<HippoMusicMonitor> createYahooMonitor();

    HIPPO_DECLARE_REFCOUNTING;

protected:
    void fireCurrentTrackChanged(bool haveTrack, const HippoTrackInfo & newTrack);
    void fireMusicAppRunning(bool nowRunning);

    HippoMusicMonitor() : refCount_(1) {}

private:
    HippoArray<HippoMusicListener*> listeners_;

    // private so they aren't used
    HippoMusicMonitor(const HippoMusicMonitor &other);
    HippoMusicMonitor& operator=(const HippoMusicMonitor &other);

    DWORD refCount_;
};

// making this implement the STL container contract is just way too much work,
// so doing something ad hoc instead
class HippoPlaylist
{
public:
    HIPPO_DECLARE_REFCOUNTING;

    virtual ~HippoPlaylist() {}

    virtual int size() const = 0;
    virtual const HippoTrackInfo& getTrack(int i) const = 0;

protected:
    HippoPlaylist() : refCount_(1) {}

private:
    HippoPlaylist(const HippoPlaylist &other);
    HippoPlaylist& operator=(const HippoPlaylist &other);

    DWORD refCount_;
};

class HippoPlaylistSource
    : public HippoMusicMonitor
{
public:

    virtual std::vector<HippoPtr<HippoPlaylist> > getPlaylists() const = 0;
    virtual HippoPtr<HippoPlaylist> getPrimingTracks() const = 0;

    virtual ~HippoPlaylistSource() {}

    static HippoPtr<HippoPlaylistSource> createITunesMonitor();

protected:
    HippoPlaylistSource() {}

private:
    // private so they aren't used
    HippoPlaylistSource(const HippoPlaylistSource &other);
    HippoPlaylistSource& operator=(const HippoPlaylistSource &other);

};

class HippoMusicListener
{
public:
    virtual void onCurrentTrackChanged(HippoMusicMonitor *monitor, bool haveTrack, const HippoTrackInfo & newTrack) = 0;
    virtual void onMusicAppRunning(HippoMusicMonitor *monitor, bool nowRunning) = 0;
    virtual ~HippoMusicListener() {}
};
