#include "StdAfx.h"
#include "HippoUtil.h"
#include "HippoITunesMonitor.h"
#include "HippoLogWindow.h"
#include <glib.h>
#include <map>

#undef GetFreeSpace
// iTunesLib
#import <libid:9E93C96F-CF0D-43F6-8BA8-B807A3370712> named_guids raw_dispinterfaces
//#import <C:\Program Files\iTunes\iTunes.exe>

using namespace iTunesLib;

// don't know how to autogenerate this, though we should
class IiTunesEvents : public IDispatch
{
public:
#define DISPID_ONDATABASECHANGEDEVENT 1
    virtual HRESULT __stdcall OnDatabaseChangedEvent (
        const _variant_t & deletedObjectIDs,
        const _variant_t & changedObjectIDs ) = 0;
#define DISPID_ONPLAYERPLAYEVENT 2
    virtual HRESULT __stdcall OnPlayerPlayEvent (
        const _variant_t & iTrack ) = 0;
#define DISPID_ONPLAYERSTOPEVENT 3
    virtual HRESULT __stdcall OnPlayerStopEvent (
        const _variant_t & iTrack ) = 0;
#define DISPID_ONPLAYERPLAYINGTRACKCHANGEDEVENT 4
    virtual HRESULT __stdcall OnPlayerPlayingTrackChangedEvent (
        const _variant_t & iTrack ) = 0;
#define DISPID_ONUSERINTERFACEENABLEDEVENT 5
    virtual HRESULT __stdcall OnUserInterfaceEnabledEvent ( ) = 0;
#define DISPID_ONCOMCALLSDISABLEDEVENT 6
    virtual HRESULT __stdcall OnCOMCallsDisabledEvent (
        ITCOMDisabledReason reason ) = 0;
#define DISPID_ONCOMCALLSENABLEDEVENT 7
    virtual HRESULT __stdcall OnCOMCallsEnabledEvent ( ) = 0;
#define DISPID_ONQUITTINGEVENT 8
    virtual HRESULT __stdcall OnQuittingEvent ( ) = 0;
#define DISPID_ONABOUTTOPROMPTUSERTOQUITEVENT 9
    virtual HRESULT __stdcall OnAboutToPromptUserToQuitEvent ( ) = 0;
#define DISPID_ONSOUNDVOLUMECHANGEDEVENT 10
    virtual HRESULT __stdcall OnSoundVolumeChangedEvent (long newVolume ) = 0;
};

 
// From iTunes developer docs:
// 
// An IITObject will always have a valid, non-zero source ID.
//
// An IITObject corresponding to a playlist or track will always have a valid playlist ID.
// The playlist ID will be zero for a source.
//
// An IITObject corresponding to a track will always have a valid track and track database ID.
// These IDs will be zero for a source or playlist.
//
// A track ID is unique within the track's playlist. A track database ID is unique across all playlists.
// For example, if the same music file is in two different playlists, each of the tracks could have different track IDs, but they will have the same track database ID. 
//

class ITunesObjectId
{
public:
    ITunesObjectId()
    {
        ITunesObjectId(0,0,0,0);
    }

    ITunesObjectId(long source, long playlist, long track, long trackDB)
    {
        data_[0] = source;
        data_[1] = playlist;
        data_[2] = track;
        data_[3] = trackDB;
    }

    // default assignment and copy should be OK

    bool isSource() const {
        return data_[0] != 0 && data_[1] == 0;
    }

    bool isPlaylist() const {
        return data_[0] != 0 && data_[1] != 0 && data_[2] == 0;
    }

    bool isTrack() const {
        return data_[0] != 0 && data_[1] != 0 && data_[2] != 0;
    }

    // javatastic!
    std::wstring toString() const {
        std::wostringstream s;
        if (isSource())
            s << "Source ";
        else if (isPlaylist())
            s << "Playlist ";
        else if (isTrack())
            s << "Track ";
        else
            s << "Unknown ";
        s << data_[0] << L":" << data_[1] << L":" << data_[2] << L":" << data_[3];
        return s.str();
    }

    // so we can be a map key
    bool operator<(const ITunesObjectId &other) {
        for (int i = 0; i < 4; ++i) {
            if (data_[i] < other.data_[i])
                return true;
            else if (data_[i] > other.data_[i])
                return false;
            else
                continue;
        }
    }

    bool operator==(const ITunesObjectId &other) {
        for (int i = 0; i < 4; ++i) {
            if (data_[i] != other.data_[i])
                return false;
        }
        return true;
    }

private:
    long data_[4];
};

class BasicPlaylist
    : public HippoPlaylist
{
public:
    virtual int size() const {
        return static_cast<int>(tracks_.size());
    }
    
    virtual const HippoTrackInfo& getTrack(int i) const {
        return tracks_.at(i);
    }

    void add(const HippoTrackInfo &track) {
        tracks_.push_back(track);
    }

    void clear() {
        tracks_.clear();
    }

private:
    std::vector<HippoTrackInfo> tracks_;
};

class ITunesPlaylist
    : public BasicPlaylist
{
public:
    ITunesPlaylist(const ITunesObjectId & id)
        : id_(id)
    {
    }

    const ITunesObjectId& getId() const {
        return id_;
    }

private:
    ITunesObjectId id_;
};

class HippoITunesMonitorImpl
    : public IDispatch
{
public:
    HippoITunesMonitorImpl(HippoITunesMonitor *wrapper);
    ~HippoITunesMonitorImpl();

    enum State {
        NO_ITUNES,
        CONNECTED
    };

    HippoITunesMonitor *wrapper_;
    State state_;
    HippoTrackInfo track_;
    bool haveTrack_;
    DWORD refCount_;
    HippoPtr<IiTunes> iTunes_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<IConnectionPoint> connectionPoint_;
    DWORD connectionCookie_;
#define CHECK_RUNNING_TIMEOUT (1000*30)
    unsigned int timeout_id_;
    bool firstTimeout_;
    bool enabled_;

    void setEnabled(bool enabled);
    void attemptConnect();
    void disconnect();
    void setTrack(IITTrack *track);
    bool readTrackInfo(IITTrack *track, HippoTrackInfo *info);
    void readPlaylists();
    HippoPtr<HippoPlaylist> getPrimingTracks();

    static gboolean checkRunningTimeout(void *data);

    /// COM goo

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    HIPPO_DECLARE_REFCOUNTING;

    // IDispatch methods
    STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);           
    STDMETHODIMP GetTypeInfoCount (unsigned int *);
    STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                         VARIANT *, EXCEPINFO *, unsigned int *);
};

static BOOL CALLBACK
findITunesWindow(HWND hwnd, LPARAM lParam)
{
    bool *foundp = reinterpret_cast<bool*>(lParam);

    WCHAR buf[32];
    if (GetClassName(hwnd, &buf[0], 32) != 0) {
        //hippoDebugLogW(L"Window %p class='%s'", hwnd, &buf[0]);
        if (StrCmpW(L"iTunes", &buf[0]) == 0) {
            *foundp = true;
            // can stop looking now
            return FALSE;
        }
    } else {
        hippoDebugLogW(L"Failed to get class of window");
    }

    return TRUE;
}

static bool
iTunesIsRunning()
{
    bool found = false;
    // return value of this doesn't matter, but 
    // it is 0 on error or if we ever return false from 
    // our function. To check error you have to use GetLastError
    // or something. See docs if you ever care.
    EnumWindows(findITunesWindow, reinterpret_cast<LPARAM>(&found));

    return found;
}

gboolean
HippoITunesMonitorImpl::checkRunningTimeout(void *data)
{
    HippoITunesMonitorImpl *impl = static_cast<HippoITunesMonitorImpl*>(data);
    gboolean ret;

    ret = 1; // stay connected
    
    if (impl->firstTimeout_) {
       impl->timeout_id_ = g_timeout_add(CHECK_RUNNING_TIMEOUT, checkRunningTimeout, 
                                         impl);
       ret = 0; // disconnect this one, we added a new one
       impl->firstTimeout_ = false; // in future don't disconnect again
    }

    if (impl->state_ == NO_ITUNES) {
        if (iTunesIsRunning())
            impl->attemptConnect();
    } else if (impl->state_ == CONNECTED) {
        if (!iTunesIsRunning())
            impl->disconnect();
    }

    return ret;
}

HippoITunesMonitorImpl::HippoITunesMonitorImpl(HippoITunesMonitor *wrapper)
    : wrapper_(wrapper), haveTrack_(false), state_(NO_ITUNES), refCount_(1), firstTimeout_(true), enabled_(false)
{
    // one-shot idle immediately, which converts itself to a periodic timeout
    timeout_id_ = g_timeout_add(0, checkRunningTimeout, 
                                this);
}

HippoITunesMonitorImpl::~HippoITunesMonitorImpl()
{
    g_source_remove(timeout_id_);
    timeout_id_ = 0;
    disconnect();
}

static void
listConnections(IConnectionPoint *point) 
{
    HippoPtr<IEnumConnections> connections;
    HRESULT hRes = point->EnumConnections(&connections);
    if (FAILED(hRes)) {
        hippoDebugLogU("Failed to list connections");
        return;
    }

    hippoDebugLogU("connections:");
    CONNECTDATA data;
    while ((hRes = connections->Next(1, &data, 0)) == S_OK) {
        hippoDebugLogU("  cookie %d", (int) data.dwCookie);
        hippoDebugLogU("  unknown %p", data.pUnk);
        data.pUnk->Release();
    }
}

void
HippoITunesMonitorImpl::setEnabled(bool enabled)
{
    if (enabled == enabled_)
        return;
    enabled_ = enabled;
    if (enabled_)
        attemptConnect();
    else
        disconnect();
}

void
HippoITunesMonitorImpl::attemptConnect()
{
    hippoDebugLogU("ITUNES CONNECT %s", __FUNCTION__);

    if (state_ == CONNECTED || !enabled_)
        return;

    HRESULT hRes;
    
    ifaceTypeInfo_ = 0; // in case we half-connected earlier
    connectionPoint_ = 0;
    connectionCookie_ = 0;

    hRes = hippoLoadRegTypeInfo(LIBID_iTunesLib, 1, 5, &DIID__IiTunesEvents, &ifaceTypeInfo_, 0);

    if (FAILED(hRes)) {
        HippoBSTR s;
        hippoHresultToString(hRes, s);
        hippoDebugLogW(L"%s", s.m_str);
        return;
    } else if (ifaceTypeInfo_ == 0) {
        hippoDebugLogU("Type info was null");
        return;
    } else {
        hippoDebugLogU("loaded type info OK");
    }

    HippoPtr<IiTunes> iTunesPtr;
#if 0
    // Does not work... iTunes doesn't register itself I guess
    HippoPtr<IUnknown> unknown;
    hRes = GetActiveObject(CLSID_iTunesApp, 0, &unknown);
    if (SUCCEEDED(hRes)) {
        hippoDebugLogW(L"iTunes already running");
        unknown->QueryInterface<IiTunes>(&iTunesPtr);
    }
#endif
    if (!iTunesIsRunning()) {
        hippoDebugLogW(L"iTunes doesn't have a window open, not monitoring it");
        return;
    } else {
        hippoDebugLogW(L"Found an iTunes window, trying to connect");
    }

    // force-launches itunes if not running
    hRes = ::CoCreateInstance(CLSID_iTunesApp, NULL, CLSCTX_LOCAL_SERVER, IID_IiTunes, (PVOID *)&iTunesPtr);

    if (FAILED(hRes) || iTunesPtr == 0) {
        hippoDebugLogW(L"Failed to get the iTunes app");
        return;
    }
    
    HippoQIPtr<IConnectionPointContainer> container(iTunesPtr);
    if (container == 0) {
        hippoDebugLogW(L"Failed to get connection point container");
        return;
    }
    
    hRes = container->FindConnectionPoint(DIID__IiTunesEvents, &connectionPoint_);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed to get connection point");
        return;
    }
    
    listConnections(connectionPoint_);

    hRes = connectionPoint_->Advise(this, &connectionCookie_);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed to connect to connection point");
        connectionPoint_ = 0;
        connectionCookie_ = 0;
        return;
    }

    iTunes_ = iTunesPtr;

    hippoDebugLogW(L"All connected up, supposedly; cookie: %d", (int) connectionCookie_);

    listConnections(connectionPoint_);

    state_ = CONNECTED;
    wrapper_->fireMusicAppRunning(true);

    if (state_ == CONNECTED) {
        HippoPtr<IITTrack> trackPtr;
        hRes = iTunesPtr->get_CurrentTrack(&trackPtr);
        if (FAILED(hRes)) {
            hippoDebugLogW(L"Failed to get current track after reconnecting");
            disconnect();
            return;
        }

        setTrack(trackPtr);
    }

    // debug-only to have these here instead of "as needed"
    // readPlaylists();
    // getPrimingTracks();
}

void
HippoITunesMonitorImpl::disconnect() {
    hippoDebugLogU("ITUNES DISCONNECT %s", __FUNCTION__);

    ifaceTypeInfo_ = 0;

    if (state_ == NO_ITUNES)
        return;

    if (connectionPoint_ != 0) {
        HRESULT hRes = connectionPoint_->Unadvise(connectionCookie_);
        if (FAILED(hRes)) {
            hippoDebugLogW(L"Failed to disconnect from connection point");
            // continue anyway
        } else {
            connectionPoint_ = 0;
            connectionCookie_ = 0;
            hippoDebugLogW(L"Successfully unadvised");
        }
    } else {
        hippoDebugLogW(L"Connection point was null");
    }
    
    iTunes_ = 0;

    state_ = NO_ITUNES;
    wrapper_->fireMusicAppRunning(false);
}


#define GET_PROPERTY_START(obj, varType, iProp, hProp)          \
    do {                                                        \
    if (obj != 0) {                                             \
        hRes = obj->get_ ## iProp(&val ## hProp);               \
        if (FAILED(hRes)) {                                     \
            HippoBSTR e;                                        \
            hippoHresultToString(hRes, e);                      \
            hippoDebugLogW(L"error getting prop: %s", e.m_str); \
            disconnect();                                       \
            return false;                                       \
        }                                                       \
    }                                                           \
    } while(0)

#define GET_PROPERTY_END_STRING(obj, iProp, hProp)              \
    do {                                                        \
    if (obj != 0) {                                             \
        info->set ## hProp (val ## hProp);                      \
        if(0) hippoDebugLogU("  got prop %s", #hProp);          \
        if(0) hippoDebugLogW(L" %s", val ## hProp .m_str);      \
    }                                                           \
    } while(0)

#define GET_PROPERTY_END_LONG(obj, iProp, hProp)                \
    do {                                                        \
    if (obj != 0) {                                             \
        info->set ## hProp (val ## hProp);                      \
        if(0) hippoDebugLogU("  got prop %s", #hProp);          \
        if(0) hippoDebugLogW(L" %ld", val ## hProp );           \
    }                                                           \
    } while(0)

#define GET_PROPERTY_END_KIND(obj, iProp, hProp)                \
    do {                                                        \
    if (obj != 0) {                                             \
        HippoBSTR k;                                            \
        switch (val ## hProp) {                                 \
        case ITTrackKindCD:                                     \
            k = HIPPO_TRACK_TYPE_CD;                            \
            break;                                              \
        case ITTrackKindFile:                                   \
            if (isPodcast)                                      \
                k = HIPPO_TRACK_TYPE_PODCAST;                   \
            else                                                \
                k = HIPPO_TRACK_TYPE_FILE;                      \
            break;                                              \
        case ITTrackKindURL:                                    \
            if (isPodcast)                                      \
                k = HIPPO_TRACK_TYPE_PODCAST;                   \
            else                                                \
                k = HIPPO_TRACK_TYPE_NETWORK_STREAM;            \
            break;                                              \
        default:                                                \
            k = HIPPO_TRACK_TYPE_UNKNOWN;                       \
            break;                                              \
        }                                                       \
        info->set ## hProp (k);                                 \
        if(0) hippoDebugLogU("  got prop %s", #hProp);          \
        if(0) hippoDebugLogW(L" %ld", (long) val ## hProp );    \
    }                                                           \
    } while(0)

#define GET_PROPERTY_STRING(obj, iProp, hProp)                  \
    do {                                                        \
    HippoBSTR val ## hProp;                                     \
    GET_PROPERTY_START(obj, HippoBSTR, iProp, hProp);           \
    GET_PROPERTY_END_STRING(obj, iProp, hProp);                 \
    } while(0)

#define GET_PROPERTY_LONG(obj, iProp, hProp)                    \
    do {                                                        \
    long val ## hProp;                                          \
    GET_PROPERTY_START(obj, long, iProp, hProp);                \
    GET_PROPERTY_END_LONG(obj, iProp, hProp);                   \
    } while(0)

#define GET_PROPERTY_KIND(obj, iProp, hProp)                    \
    do {                                                        \
    ITTrackKind val ## hProp;                                   \
    GET_PROPERTY_START(obj, ITTrackKind, iProp, hProp);         \
    GET_PROPERTY_END_KIND(obj, iProp, hProp);                   \
    } while(0)

#define GET_PROPERTY_STRING_IF_NOT_SET(obj, iProp, hProp)           \
    do {                                                            \
    if (!info->has ## hProp ()) {                                   \
        GET_PROPERTY_STRING(obj, iProp, hProp);                     \
    }                                                               \
    } while(0)


bool
HippoITunesMonitorImpl::readTrackInfo(IITTrack *track, HippoTrackInfo *info)
{
    if (track == 0) {
        hippoDebugLogW(L"null track");
        return false;
    }

    HippoQIPtr<IITFileOrCDTrack> fileTrack_(track);
    HippoQIPtr<IITURLTrack> urlTrack_(track);

    HRESULT hRes;
       
    // we need to know if it's a podcast to interpret the ITTrackKind value
    VARIANT_BOOL isPodcast = false;
    if (urlTrack_ != 0) {
        hRes = urlTrack_->get_Podcast(&isPodcast);
        if (FAILED(hRes)) {                                     
            HippoBSTR e;                                        
            hippoHresultToString(hRes, e);                      
            hippoDebugLogW(L"error getting prop: %s", e.m_str); 
            disconnect();                                       
            return false;                                       
        }                                                       
    }
    if (fileTrack_ != 0) {
        hRes = fileTrack_->get_Podcast(&isPodcast);
        if (FAILED(hRes)) {                                     
            HippoBSTR e;                                        
            hippoHresultToString(hRes, e);                      
            hippoDebugLogW(L"error getting prop: %s", e.m_str); 
            disconnect();                                       
            return false;                                       
        }                                                       
    }

    GET_PROPERTY_KIND(track, Kind, Type);
    // can't find an obvious way to get this from iTunes
    //GET_PROPERTY_STRING(track, Format, Format);
    GET_PROPERTY_STRING(fileTrack_, Name, Name);
    GET_PROPERTY_STRING_IF_NOT_SET(urlTrack_, Name, Name);
    GET_PROPERTY_STRING(fileTrack_, Location, Location);
    GET_PROPERTY_STRING(track, Artist, Artist);
    GET_PROPERTY_STRING(track, Album, Album);
    GET_PROPERTY_STRING(urlTrack_, URL, Url);
    GET_PROPERTY_LONG(track, Duration, Duration);
    GET_PROPERTY_LONG(track, Size, FileSize);
    GET_PROPERTY_LONG(track, TrackNumber, TrackNumber);
    // can't find an obvious way to get this from iTunes
    //GET_PROPERTY_STRING(track, DiscIdentifier, DiscIdentifier);

    return true;
}

void
HippoITunesMonitorImpl::setTrack(IITTrack *track)
{
    hippoDebugLogW(L"itunes setTrack");

    bool oldHaveTrack = haveTrack_;
    HippoTrackInfo oldTrack = track_;

    haveTrack_ = readTrackInfo(track, &track_);
    if (!haveTrack_)
        track_.clear();

    hippoDebugLogW(L"itunes track result: %d", haveTrack_);

    if (oldHaveTrack != haveTrack_ || oldTrack != track_) {
        if (wrapper_)
            wrapper_->fireCurrentTrackChanged(haveTrack_, track_);
    }

    hippoDebugLogW(L"itunes fired track change event");
}

static const wchar_t*
sourceKindToString(ITSourceKind kind)
{
    switch (kind)
    {
    case ITSourceKindAudioCD:
        return L"AudioCD";
    case ITSourceKindDevice:
        return L"Device";
    case ITSourceKindIPod:
        return L"IPod";
    case ITSourceKindLibrary:
        return L"Library";
    case ITSourceKindMP3CD:
        return L"MP3CD";
    case ITSourceKindRadioTuner:
        return L"RadioTuner";
    case ITSourceKindSharedLibrary:
        return L"SharedLibrary";
    case ITSourceKindUnknown:
        return L"Unknown";
    default:
        return L"Unhandled";
    }
}

static const wchar_t*
playlistKindToString(ITPlaylistKind kind)
{
    switch (kind)
    {
    case ITPlaylistKindCD:
        return L"CD";
    case ITPlaylistKindDevice:
        return L"Device";
    case ITPlaylistKindLibrary:
        return L"Library";
    case ITPlaylistKindRadioTuner:
        return L"RadioTuner";
    case ITPlaylistKindUser:
        return L"User";
    case ITPlaylistKindUnknown:
        return L"Unknown";
    default:
        return L"Unhandled";
    }
}

void
HippoITunesMonitorImpl::readPlaylists()
{
    if (state_ == NO_ITUNES)
        return;

    HippoPtr<IITSourceCollection> sources;

    HRESULT hRes = iTunes_->get_Sources(&sources);
    if (FAILED(hRes)) {
        disconnect();
        return;
    }

    // this has to be sort of a race, getting the sources by 
    // index using multiple calls, but no clear solution

    long sourceCount;
    hRes = sources->get_Count(&sourceCount);
    if (FAILED(hRes)) {
        disconnect();
        return;
    }

     // yes, 1-based, yay
    for (long i = 1; i <= sourceCount; ++i) {
        HippoPtr<IITSource> source;
        hRes = sources->get_Item(i, &source);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }

        long sourceId;
        hRes = source->get_sourceID(&sourceId);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }

        HippoBSTR sourceName;
        hRes = source->get_Name(&sourceName);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }

        ITSourceKind sourceKind;
        hRes = source->get_Kind(&sourceKind);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }        

        ITunesObjectId id(sourceId, 0, 0, 0);
        hippoDebugLogW(L"Source %s, %s '%s'", id.toString().c_str(), sourceKindToString(sourceKind), sourceName.m_str);

        HippoPtr<IITPlaylistCollection> playlists;
        hRes = source->get_Playlists(&playlists);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }

        long listCount;
        hRes = playlists->get_Count(&listCount);
        if (FAILED(hRes)) {
            disconnect();
            return;
        }
        for (long j = 1; j <= listCount; ++j) {
            HippoPtr<IITPlaylist> list;
            hRes = playlists->get_Item(j, &list);
            if (FAILED(hRes)) {
                disconnect();
                return;
            }
            HippoBSTR listName;
            hRes = list->get_Name(&listName);
            if (FAILED(hRes)) {
                disconnect();
                return;
            }
            long playlistId;
            hRes = list->get_playlistID(&playlistId);
            if (FAILED(hRes)) {
                disconnect();
                return;
            }
            ITPlaylistKind listKind;
            hRes = list->get_Kind(&listKind);
            if (FAILED(hRes)) {
                disconnect();
                return;
            }

            ITunesObjectId id(sourceId, playlistId, 0, 0);
            hippoDebugLogW(L"Playlist %s, %s '%s'", id.toString().c_str(), playlistKindToString(listKind), listName.m_str);
        }
    }
}

HippoPtr<HippoPlaylist>
HippoITunesMonitorImpl::getPrimingTracks()
{
    if (state_ == NO_ITUNES)
        return 0;
    
    try {
        HippoPtr<IITLibraryPlaylist> library;
        HRESULT hRes = iTunes_->get_LibraryPlaylist(&library);
        if (FAILED(hRes)) {
            throw HResultException(hRes, "Failed to get library playlist");
        }

        long librarySourceId;
        hRes = library->get_sourceID(&librarySourceId);
        if (FAILED(hRes)) {
            throw HResultException(hRes, "Failed to get library source ID");
        }
        long libraryPlaylistId;
        hRes = library->get_playlistID(&libraryPlaylistId);
        if (FAILED(hRes)) {
            throw HResultException(hRes, "Failed to get library playlist ID");
        }

        HippoPtr<IITTrackCollection> tracks;
        hRes = library->get_Tracks(&tracks);
        if (FAILED(hRes)) {
            throw HResultException(hRes, "Failed to get library playlist tracks");
        }

        long numTracks;
        hRes = tracks->get_Count(&numTracks);
        if (FAILED(hRes)) {
            throw HResultException(hRes, "Failed to get number of tracks in library");
        }
        
        std::multimap<long,long> playCounts;
        std::multimap<DATE,long> playDates;
        std::map<long,long> trackIds; // track IDs in the library playlist, by database id

        // yucky race condition here if numTracks changes while we're doing this, also 
        // this could take a long time, I'm not sure how fast it is
        for (int i = 1; i <= numTracks; ++i) {
            
            HippoPtr<IITTrack> track;
            hRes = tracks->get_Item(i, &track);
            if (FAILED(hRes)) {
                throw HResultException(hRes, "Failed to get track");
            }

            long databaseId;
            hRes = track->get_TrackDatabaseID(&databaseId);
            if (FAILED(hRes)) {
                throw HResultException(hRes, "Failed to get track database ID");
            }

            long trackId;
            hRes = track->get_trackID(&trackId);
            if (FAILED(hRes)) {
                throw HResultException(hRes, "Failed to get track ID");
            }
            trackIds[databaseId] = trackId;

            long playedCount;
            hRes = track->get_PlayedCount(&playedCount);
            if (FAILED(hRes)) {
                throw HResultException(hRes, "Failed to get track play count");
            }
            playCounts.insert(std::pair<long,long>(playedCount, databaseId));

            DATE playedDate;
            hRes = track->get_PlayedDate(&playedDate);
            if (FAILED(hRes)) {
                throw HResultException(hRes, "Failed to get track play date");
            }
            playDates.insert(std::pair<DATE,long>(playedDate, databaseId));
        }

        assert(playCounts.size() == playDates.size());

        // compute a "top 25" based on both frequency and recency, using some 
        // crappy wasteful algorithm. This is kind of overkilled since we 
        // used to try to create a "combined score" of frequency plus recency;
        // what we try to do now is just upload a few of each, with recency
        // items sent _second_ to the server so they show up as the user's last
        // few songs. Otherwise it's sort of confusing. But we wanted the 
        // frequency too so we don't just get all one artist from the last-played
        // album.
        // For the current algorithm this code could be a lot shorter and less
        // rube goldberg.

        std::map<long,int> ranks;

#define NUM_DESIRED_TRACKS 25

        // from oldest to newest
        int rank = 0;
        for (std::multimap<DATE,long>::iterator i = playDates.begin(); i != playDates.end(); ++i) {
            // hippoDebugLogW(L"Track %ld played at %g", i->second, i->first);
            // recency gets a "rank bonus" of half the number of desired tracks, so if 
            // we get the top 25 ranks we'll get half of them from this
            ranks[i->second] = rank + (NUM_DESIRED_TRACKS / 2);
            ++rank;
        }

        // from least to most played
        rank = 0;
        for (std::multimap<long,long>::iterator i = playCounts.begin(); i != playCounts.end(); ++i) {
            // hippoDebugLogW(L"Track %ld played %ld times", i->second, i->first);
            ranks[i->second] = rank;
            ++rank;
        }

        std::multimap<int,long> byRank;
        for (std::map<long,int>::iterator i = ranks.begin(); i != ranks.end(); ++i) {
            byRank.insert(std::pair<int,long>(i->second, i->first));
        }

        HippoPtr<BasicPlaylist> tracksList;
        *(&tracksList) = new BasicPlaylist();

        // we want the highest rank number, i.e. newest and most played
        for (std::multimap<int,long>::reverse_iterator i = byRank.rbegin(); i != byRank.rend(); ++i) {
            long databaseId = i->second;

            HippoPtr<IITObject> object;
            hRes = iTunes_->raw_GetITObjectByID(librarySourceId, libraryPlaylistId,
                trackIds[databaseId], databaseId, &object);
            if (FAILED(hRes))
                throw HResultException(hRes, "Failed to get track by database ID");
            HippoQIPtr<IITTrack> track(object);
            if (track == 0)
                throw HResultException(GetLastError(), "Failed to convert looked-up track to a track object");

            HippoTrackInfo info;
            if (!readTrackInfo(track, &info))
                throw HResultException(GetLastError(), "Failed to read information for track"); // GetLastError() kinda bogus here
            tracksList->add(info);

            // hippoDebugLogW(L"Rank %d %s", i->first, info.getName().m_str);

            if (tracksList->size() >= NUM_DESIRED_TRACKS) {
                hippoDebugLogW(L"Got enough tracks for priming, not loading any more");
                break;
            }
        }

#if 0
        for (int i = 0; i < tracksList->size(); ++i) {
            const HippoTrackInfo &info(tracksList->getTrack(i));
            hippoDebugLogW(L"Priming %d track %s", i, info.getName().m_str);
        }
#endif

        return HippoPtr<HippoPlaylist>(tracksList);
    } catch (HResultException &e) {
        hippoDebugLogU("%s", e.what());
        disconnect();
        return 0;
    }
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoITunesMonitorImpl::QueryInterface(const IID &ifaceID,
                            void   **result)
{
    //hippoDebugLogU("%s", __FUNCTION__);
    if (IsEqualIID(ifaceID, IID_IUnknown)) {
        *result = static_cast<IUnknown *>(this);
    } else if (IsEqualIID(ifaceID, IID_IDispatch)) {
        *result = static_cast<IDispatch *>(this);
    } else if (IsEqualIID(ifaceID, DIID__IiTunesEvents)) {
        *result = static_cast<IDispatch *>(this);
    } else {
        *result = 0;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}                                         

HIPPO_DEFINE_REFCOUNTING(HippoITunesMonitorImpl)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoITunesMonitorImpl::GetTypeInfoCount(UINT *pctinfo)
{
    hippoDebugLogU("%s", __FUNCTION__);

    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoITunesMonitorImpl::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    hippoDebugLogU("%s", __FUNCTION__);

    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoITunesMonitorImpl::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    hippoDebugLogU("%s", __FUNCTION__);

    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}

static HRESULT
getBounds(SAFEARRAY *array, unsigned int dimension, long *lower, long *upper)
{
    *lower = -500;
    *upper = -500;

    HRESULT hRes = SafeArrayGetLBound(array, dimension, lower);
    if (FAILED(hRes)) {
        hippoDebugLogU("Failed to get array lower bound for dimension %d: %s", dimension,
            HResultException(hRes).what());
        return hRes;
    }
    hRes = SafeArrayGetUBound(array, dimension, upper);
    if (FAILED(hRes)) {
        hippoDebugLogU("Failed to get array upper bound for dimension %d: %s", dimension,
            HResultException(hRes).what());
        return hRes;
    }
    return S_OK;
}

static HRESULT
getIdFromArray(SAFEARRAY *array, long i, ITunesObjectId *id_p)
{
    long lower;
    long upper;

    HRESULT hRes = getBounds(array, 2, &lower, &upper);
    if (FAILED(hRes))
        return hRes;
    //hippoDebugLogW(L"Bounds of dimension 2 are [%ld,%ld]", lower, upper);

    // it appears that iTunes will return less than 4 ids if the extra ones are 0? 
    // not really sure what's going on with that...
    if ((upper - lower) != 3) {
        hippoDebugLogW(L"Bad array bounds on dimension 2, [%ld,%ld]", lower, upper);
        return DISP_E_BADVARTYPE;
    }

    long idComponents[4] = { 0, 0, 0, 0 };
    long coord[2];
    coord[0] = i;
    for (coord[1] = lower; coord[1] <= upper; coord[1] += 1) {
        VARIANT vt;
        VariantInit(&vt);
        // hippoDebugLogW(L"asking for coord %d,%d", coord[0], coord[1]);
        HRESULT hRes = SafeArrayGetElement(array, &coord[0], &vt);
        idComponents[coord[1]] = vt.intVal; // safe (but useless) even if we failed the hresult
        VariantClear(&vt);
        if (FAILED(hRes)) {
            if (hRes == DISP_E_BADINDEX)
                hippoDebugLogW(L"Coordinate out of bounds");
            hippoDebugLogW(L"Failed to get element %d,%d", coord[0], coord[1]);
            return hRes;
        }
    }
    *id_p = ITunesObjectId(idComponents[0], idComponents[1], idComponents[2], idComponents[3]);
    return S_OK;
}

static HRESULT
getIdsFromArray(SAFEARRAY *array, std::vector<ITunesObjectId> *ids_p)
{
    long lower;
    long upper;

    // dimensions are counted 1-based apparently
    HRESULT hRes = getBounds(array, 1, &lower, &upper);
    if (FAILED(hRes))
        return hRes;
    // if the array is empty we seem to get lower=0 upper=-1 since upper=0 would mean 0 is valid...
    //hippoDebugLogW(L"Bounds of dimension 1 are [%ld,%ld]", lower, upper);

    for (long i = lower; i <= upper; ++i) {
        ITunesObjectId id;
        hRes = getIdFromArray(array, i, &id);
        if (FAILED(hRes))
            return hRes;
        ids_p->push_back(id);
    }
    return S_OK;
}

#define ALL_VARIANT_FLAGS (VT_VECTOR | VT_ARRAY | VT_BYREF | VT_RESERVED)

static inline VARTYPE
getBaseType(VARTYPE type)
{
    return (type) & ~ALL_VARIANT_FLAGS;
}

static inline bool
argHasBaseType(DISPPARAMS *dispParams, int argc, VARTYPE type)
{
    assert((type & ALL_VARIANT_FLAGS) == 0); // not a flag type 

    return getBaseType(dispParams->rgvarg[argc].vt) == type;
}

static inline bool
argHasTypeFlags(DISPPARAMS *dispParams, int argc, VARTYPE flags)
{
    assert((flags & ~ALL_VARIANT_FLAGS) == 0); // not a base type

    return ((dispParams->rgvarg[argc].vt) & flags) == flags;
}

static bool
checkArgType(DISPPARAMS *dispParams, int argc, VARTYPE expectedBase, VARTYPE expectedFlags)
{
    if (!argHasBaseType(dispParams, argc, expectedBase)) {
        hippoDebugLogU("arg %d expecting base type %d, got %d", argc, expectedBase, dispParams->rgvarg[argc].vt & ~ALL_VARIANT_FLAGS);
        return false;
    }
    if (!argHasTypeFlags(dispParams, argc, expectedFlags)) {
        hippoDebugLogU("arg %d expecting type flags 0x%x, got 0x%x", argc, expectedFlags, dispParams->rgvarg[argc].vt & ALL_VARIANT_FLAGS);
        return false;
    }
    return true;
}

static bool
checkArgCount(DISPPARAMS *dispParams, int expected)
{
    if (dispParams->cArgs != expected) {
        hippoDebugLogW(L"Expected %d args got %d", expected, dispParams->cArgs);
        return false;
    }
    return true;
}

STDMETHODIMP
HippoITunesMonitorImpl::Invoke (DISPID        member,
                                const IID    &iid,
                                LCID          lcid,              
                                WORD          flags,
                                DISPPARAMS   *dispParams,
                                VARIANT      *result,
                                EXCEPINFO    *excepInfo,  
                                unsigned int *argErr)
{
    //hippoDebugLogU("%s", __FUNCTION__);

    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;

    switch (member) {
    case DISPID_ONDATABASECHANGEDEVENT:
        hippoDebugLogW(L"database changed");
        // Two parameters, deletedObjects / changedOrAddedObjects
        // each parameter is a 2D SAFEARRAY of VARIANT with the variants
        // containing type VT_I4
        // The first dimension is of size # of objects, the second is size 4
        // the 4 items in the second dimension are the 
        // source, playlist, track, track database) IDs

        if (!checkArgCount(dispParams, 2))
            return DISP_E_BADPARAMCOUNT;

        if (!checkArgType(dispParams, 0, VT_VARIANT, VT_ARRAY))
            return DISP_E_BADVARTYPE;
        if (!checkArgType(dispParams, 1, VT_VARIANT, VT_ARRAY))
            return DISP_E_BADVARTYPE;

        {
            // dispParams is BACKWARD so in the IDL, deletedObjects is first
            SAFEARRAY *deletedObjects = dispParams->rgvarg[1].parray;
            SAFEARRAY *changedObjects = dispParams->rgvarg[0].parray;

            std::vector<ITunesObjectId> deleted;
            if (deletedObjects != 0) {
                if (SafeArrayGetDim(deletedObjects) != 2) {
                    hippoDebugLogW(L"Wrong number of dimensions in deletedObjects array");
                    return DISP_E_BADVARTYPE;
                }

                HRESULT hRes = getIdsFromArray(deletedObjects, &deleted);
                if (FAILED(hRes)) {
                    hippoDebugLogW(L"Failed to get IDs from the deletedObjects array");
                    return hRes;
                }
            } else {
                hippoDebugLogW(L"null deletedObjects array");
            }

            std::vector<ITunesObjectId> changed;
            if (changedObjects != 0) {                
                if (SafeArrayGetDim(changedObjects) != 2) {
                    hippoDebugLogW(L"Wrong number of dimensions in changedObjects array");
                    return DISP_E_BADVARTYPE;
                }

                HRESULT hRes = getIdsFromArray(changedObjects, &changed);
                if (FAILED(hRes)) {
                    hippoDebugLogW(L"Failed to get IDs from the changedObjects array");
                    return hRes;
                }
            } else {
                hippoDebugLogW(L"null changedObjects array");
            }

            if (deleted.size() == 0)
                hippoDebugLogW(L"No deleted items");
            for (std::vector<ITunesObjectId>::const_iterator i = deleted.begin(); i != deleted.end(); ++i) {
                hippoDebugLogW(L"Deleted %s", i->toString().c_str());
            }

            if (changed.size() == 0)
                hippoDebugLogW(L"No changed items");
            for (std::vector<ITunesObjectId>::const_iterator i = changed.begin(); i != changed.end(); ++i) {
                hippoDebugLogW(L"Changed %s", i->toString().c_str());
            }
        }
        break;
    case DISPID_ONPLAYERPLAYEVENT:
        hippoDebugLogW(L"player play");

        if (!checkArgCount(dispParams, 1))
            return DISP_E_BADPARAMCOUNT;

        if (!checkArgType(dispParams, 0, VT_DISPATCH, 0))
            return DISP_E_BADVARTYPE;

        // FIXME it's probably better to just queue an idle that gets the 
        // current track

        {
            HippoQIPtr<IITTrack> track(dispParams->rgvarg[0].pdispVal);
            setTrack(track);
        }
        break;
    case DISPID_ONPLAYERSTOPEVENT:
        hippoDebugLogW(L"player stop");
        setTrack(0);
        break;
    case DISPID_ONPLAYERPLAYINGTRACKCHANGEDEVENT:
        hippoDebugLogW(L"playing track changed");
        // this is if the properties of the track change, not if we change tracks.
        // it's apparently most likely for something called "joined CD tracks"

        if (!checkArgCount(dispParams, 1))
            return DISP_E_BADPARAMCOUNT;

        if (!checkArgType(dispParams, 0, VT_DISPATCH, 0))
            return DISP_E_BADVARTYPE;

        {
            HippoQIPtr<IITTrack> track(dispParams->rgvarg[0].pdispVal);
            setTrack(track);
        }
        break;
    case DISPID_ONUSERINTERFACEENABLEDEVENT:
        hippoDebugLogW(L"UI enabled");
        break;
    case DISPID_ONCOMCALLSDISABLEDEVENT:
        hippoDebugLogW(L"COM disabled");
        break;
    case DISPID_ONCOMCALLSENABLEDEVENT:
        hippoDebugLogW(L"COM enabled");
        break;
    case DISPID_ONQUITTINGEVENT:
        hippoDebugLogW(L"quitting");
        disconnect();
        break;
    case DISPID_ONABOUTTOPROMPTUSERTOQUITEVENT:
        hippoDebugLogW(L"about to prompt to quit");
        disconnect();
        break;
    case DISPID_ONSOUNDVOLUMECHANGEDEVENT:
        hippoDebugLogW(L"sound volume changed");
        break;
    default:
        hippoDebugLogW(L"Unknown dispid %d", member);
        break;
    }

    return S_OK;
}

/////////////////////// public API ///////////////////////

HippoITunesMonitor::HippoITunesMonitor()
{
    impl_ = new HippoITunesMonitorImpl(this);
    impl_->Release();
}

HippoITunesMonitor::~HippoITunesMonitor()
{
    impl_->disconnect(); // called manually here since it requires wrapper_
    impl_->wrapper_ = 0;
    impl_ = 0;
}

void
HippoITunesMonitor::setEnabled(bool enabled)
{
    impl_->setEnabled(enabled);
}

bool 
HippoITunesMonitor::hasCurrentTrack() const
{
    return impl_->haveTrack_;
}

const HippoTrackInfo&
HippoITunesMonitor::getCurrentTrack() const
{
    assert(hasCurrentTrack());
    return impl_->track_;
}

std::vector<HippoPtr<HippoPlaylist> >
HippoITunesMonitor::getPlaylists() const
{
    // FIXME
    return std::vector<HippoPtr<HippoPlaylist> >();
}

HippoPtr<HippoPlaylist>
HippoITunesMonitor::getPrimingTracks() const
{
    return impl_->getPrimingTracks();
}
