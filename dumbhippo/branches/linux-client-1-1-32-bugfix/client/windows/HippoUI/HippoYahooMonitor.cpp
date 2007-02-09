#include "stdafx-hippoui.h"
#include "HippoUtil.h"
#include "HippoLogWindow.h"
#include <glib.h>
#include "HippoYahooMonitor.h"

#undef GetFreeSpace
// Yahoo music engine
#import "libid:2C5EBFB1-9174-4fe8-88DB-D1F460A9E83B" named_guids raw_dispinterfaces raw_interfaces_only

using namespace RMPMediaPlayerLib;


enum ReadTrackResult
{
    COM_FAILURE,
    NO_TRACK,
    HAVE_TRACK
};

class HippoYahooMonitorImpl
    : public IDispatch
{
public:
    HippoYahooMonitorImpl(HippoYahooMonitor *wrapper);
    ~HippoYahooMonitorImpl();

    enum State {
        NO_YAHOO,
        CONNECTED
    };

    HippoYahooMonitor *wrapper_;
    State state_;
    HippoTrackInfo track_;
    bool haveTrack_;
    DWORD refCount_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<IConnectionPoint> connectionPoint_;
    DWORD connectionCookie_;
    long remoteCookie_;
    HippoPtr<IRMPRemote> yahooRemote_;
    bool enabled_;

#define CHECK_RUNNING_TIMEOUT (1000*30)
    unsigned int timeout_id_;
    bool firstTimeout_;

    unsigned int updateTrackTimeout_;

    void attemptConnect();
    void disconnect();
    void setEnabled(bool enabled);

    static gboolean checkRunningTimeout(void *data);

    bool getSongInfoVariant(PredefinedMetadataField field, VARIANT *val);
    bool getSongInfoString(PredefinedMetadataField field, BSTR *val);
    bool getSongInfoInt(PredefinedMetadataField field, INT32 *val);

    ReadTrackResult tryReadTrackOnce(HippoTrackInfo *info);
    ReadTrackResult tryReadTrack(HippoTrackInfo *info);

    void trackNeedsUpdate();
    static gboolean updateTrackTimeout(void *data);
    void updateTrackNow();

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
findYahooWindow(HWND hwnd, LPARAM lParam)
{
    bool *foundp = reinterpret_cast<bool*>(lParam);

    WCHAR buf[32];
    if (GetClassName(hwnd, &buf[0], 32) != 0) {
        //hippoDebugLogW(L"Window %p class='%s'", hwnd, &buf[0]);
        if (StrCmpW(L"YMPFrame", &buf[0]) == 0) {
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
yahooIsRunning()
{
    bool found = false;
    // return value of this doesn't matter, but 
    // it is 0 on error or if we ever return false from 
    // our function. To check error you have to use GetLastError
    // or something. See docs if you ever care.
    EnumWindows(findYahooWindow, reinterpret_cast<LPARAM>(&found));

    return found;
}

gboolean
HippoYahooMonitorImpl::checkRunningTimeout(void *data)
{
    HippoYahooMonitorImpl *impl = static_cast<HippoYahooMonitorImpl*>(data);
    gboolean ret;

    ret = 1; // stay connected
    
    if (impl->firstTimeout_) {
       impl->timeout_id_ = g_timeout_add(CHECK_RUNNING_TIMEOUT, checkRunningTimeout, 
                                         impl);
       ret = 0; // disconnect this one, we added a new one
       impl->firstTimeout_ = false; // in future don't disconnect again
    }

    if (impl->state_ == NO_YAHOO) {
        if (yahooIsRunning())
            impl->attemptConnect();
    } else if (impl->state_ == CONNECTED) {
        if (!yahooIsRunning())
            impl->disconnect();
    }

    return ret;
}

HippoYahooMonitorImpl::HippoYahooMonitorImpl(HippoYahooMonitor *wrapper)
: wrapper_(wrapper), haveTrack_(false), state_(NO_YAHOO), refCount_(1), firstTimeout_(true), updateTrackTimeout_(0), enabled_(false)
{
    // one-shot idle immediately, which converts itself to a periodic timeout
    timeout_id_ = g_timeout_add(0, checkRunningTimeout, 
                                this);
}

HippoYahooMonitorImpl::~HippoYahooMonitorImpl()
{
    g_source_remove(timeout_id_);
    timeout_id_ = 0;
    disconnect();
    if (updateTrackTimeout_ != 0) {
        g_source_remove(updateTrackTimeout_);
        updateTrackTimeout_ = 0;
    }
}

void
HippoYahooMonitorImpl::setEnabled(bool enabled)
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
HippoYahooMonitorImpl::attemptConnect()
{
    hippoDebugLogU("YAHOO CONNECT %s enabled = %d", __FUNCTION__, enabled_);

    if (state_ == CONNECTED || !enabled_)
        return;

    HRESULT hRes;
    
    ifaceTypeInfo_ = 0; // in case we half-connected earlier
    connectionPoint_ = 0;
    connectionCookie_ = 0;

    hRes = hippoLoadRegTypeInfo(LIBID_RMPMediaPlayerLib, 1, 0, &DIID_DRMPRemoteEvents, &ifaceTypeInfo_, 0);

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

#if 0
    // Does not work... yahoo doesn't register itself I guess
    HippoPtr<IUnknown> unknown;
    hRes = GetActiveObject(CLSID_RMPRemote, 0, &unknown);
    if (SUCCEEDED(hRes)) {
        hippoDebugLogW(L"yahoo already running");
        hRes = unknown->QueryInterface<IRMPRemote>(&yahooRemote_);
    }
#else
    if (!yahooIsRunning()) {
        hippoDebugLogW(L"yahoo doesn't have a window open, not monitoring it");
        return;
    } else {
        hippoDebugLogW(L"Found an yahoo window, trying to connect");
    }

    // force-launches yahoo if not running
    hRes = ::CoCreateInstance(CLSID_RMPRemote, NULL, CLSCTX_LOCAL_SERVER, IID_IRMPRemote, (PVOID *)&yahooRemote_);
#endif

    if (FAILED(hRes) || yahooRemote_ == 0) {
        hippoDebugLogW(L"Failed to get the yahoo app");
        return;
    }
    
    hRes = yahooRemote_->Initialize(&remoteCookie_);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed to initialize Yahoo remote");
        return;
    }

    HippoQIPtr<IConnectionPointContainer> container(yahooRemote_);
    if (container == 0) {
        hippoDebugLogW(L"Failed to get connection point container");
        return;
    }
    
    hRes = container->FindConnectionPoint(DIID_DRMPRemoteEvents, &connectionPoint_);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed to get connection point");
        return;
    }

    hRes = connectionPoint_->Advise(this, &connectionCookie_);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed to connect to connection point");
        connectionPoint_ = 0;
        connectionCookie_ = 0;
        return;
    }
    hippoDebugLogW(L"Yahoo all connected up, supposedly; cookie: %d", (int) connectionCookie_);

    state_ = CONNECTED;
    wrapper_->fireMusicAppRunning(true);

    trackNeedsUpdate();
}

void
HippoYahooMonitorImpl::disconnect()
{
    hippoDebugLogU("YAHOO DISCONNECT %s", __FUNCTION__);

    ifaceTypeInfo_ = 0;

    if (state_ == NO_YAHOO)
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

    if (yahooRemote_ != 0) {
        // ignore errors on this one
        HRESULT hRes = yahooRemote_->Shutdown(remoteCookie_);
        if (FAILED(hRes)) {
            hippoDebugLogW(L"Failed to shut down yahoo remote");
        }
        yahooRemote_ = 0;
    }

    state_ = NO_YAHOO;
    wrapper_->fireMusicAppRunning(false);
}

bool
HippoYahooMonitorImpl::getSongInfoVariant(PredefinedMetadataField field, VARIANT *val)
{
    if (yahooRemote_ == 0)
        return false;
    VARIANT request;
    request.vt = VT_I4;
    request.intVal = field;
    HRESULT hRes = yahooRemote_->Info(YRI_GET_SONG_INFO, request, val);
    if (FAILED(hRes)) {
        hippoDebugLogW(L"Failed YRI_GET_SONG_INFO %d", field);
        return false;
    }
    return true;
}

bool
HippoYahooMonitorImpl::getSongInfoString(PredefinedMetadataField field, BSTR *val)
{
    VARIANT retval;
    if (!getSongInfoVariant(field, &retval))
        return false;
    
    if (!(retval.vt & VT_BSTR)) {
        hippoDebugLogW(L"Unexpected type for YRI_GET_SONG_INFO return field %d", field);
        return false;
    }

    *val = retval.bstrVal;
    return true;
}

bool
HippoYahooMonitorImpl::getSongInfoInt(PredefinedMetadataField field, INT32 *val)
{
    VARIANT retval;
    if (!getSongInfoVariant(field, &retval))
        return false;
    
    if (!(retval.vt & VT_I4)) {
        hippoDebugLogW(L"Unexpected type for YRI_GET_SONG_INFO return field %d", field);
        return false;
    }
    *val = retval.intVal;
    return true;
}

ReadTrackResult
HippoYahooMonitorImpl::tryReadTrackOnce(HippoTrackInfo *info)
{
    INT32 duration;
    HippoBSTR artist;
    HippoBSTR title;
    HippoBSTR album;
    HippoBSTR track;
    HippoBSTR isrc;
    HippoBSTR fileSize;
    HippoBSTR discNumber;

    // FIXME we don't try to get the local path to the file (HippoTrackInfo::Location) 
    // because it's not available via the Remote interface; we'd have to write a DLL 
    // plugin to get at this, or the playlist stuff.

    // FIXME apparently if no song is playing, we get the wrong type in the variant 
    // return, and we disconnect and reconnect over and over; should fix, though 
    // it's harmless really afaik

    if (!getSongInfoInt(METADATA_DURATION, &duration))
        return COM_FAILURE;

#define EMPTY_TO_NULL(s) do { if ((s).Length() == 0) (s) = 0; } while (0)

    if (!getSongInfoString(METADATA_ARTIST, &artist))
        return COM_FAILURE;
    EMPTY_TO_NULL(artist);
    if (!getSongInfoString(METADATA_TITLE, &title))
        return COM_FAILURE;
    EMPTY_TO_NULL(title);
    if (!getSongInfoString(METADATA_ALBUM, &album))
        return COM_FAILURE;
    EMPTY_TO_NULL(album);
    if (!getSongInfoString(METADATA_TRACK, &track))
        return COM_FAILURE;
    EMPTY_TO_NULL(track);
    if (!getSongInfoString(METADATA_ISRC, &isrc))
        return COM_FAILURE;
    EMPTY_TO_NULL(isrc);
    if (!getSongInfoString(METADATA_FILESIZE, &fileSize))
        return COM_FAILURE;
    EMPTY_TO_NULL(fileSize);
    if (!getSongInfoString(METADATA_DISCNUMBER, &discNumber))
        return COM_FAILURE;
    EMPTY_TO_NULL(discNumber);

#if 0
    hippoDebugLogW(L"Duration %d", duration);
#define SPAM_STRING(s) if (s != 0) hippoDebugLogW(L#s L" %s", (s).m_str); else hippoDebugLogW(L#s L" not set")
    SPAM_STRING(artist);
    SPAM_STRING(title);
    SPAM_STRING(album);
    SPAM_STRING(track);
    SPAM_STRING(isrc);
    SPAM_STRING(fileSize);
    SPAM_STRING(discNumber);
#endif

    // YME does weird stuff, like have a track with only a title 
    // and the title is its window title, when it isn't playing anything.
    // so this check should filter out junk.
    if (artist.m_str == 0 || title.m_str == 0 || album.m_str == 0)
        return NO_TRACK;

    info->setDuration(duration / 1000); // YME duration is in milliseconds
    info->setArtist(artist);
    info->setName(title);
    info->setAlbum(album);

    // The other YME fields don't show up on the Y! Unlimited tracks so 
    // I don't know what they look like or how to parse the strings...
    // not even sure what discNumber for example _is_
    return HAVE_TRACK;
}

// YME setup is inherently racy, since we are asking the player 
// not some kind of track object for each property, in separate calls.
// so we query all properties twice and see if any changed midstream
ReadTrackResult
HippoYahooMonitorImpl::tryReadTrack(HippoTrackInfo *info)
{
    HippoTrackInfo firstTry;
    HippoTrackInfo secondTry;
    ReadTrackResult result;

    result = tryReadTrackOnce(&firstTry);
    if (result != HAVE_TRACK)
        return result;

    result = tryReadTrackOnce(&secondTry);
    if (result != HAVE_TRACK)
        return result;
    
    if (firstTry != secondTry) {
        hippoDebugLogW(L"Track properties changed midstream! Trying again");

        // try once more
        HippoTrackInfo thirdTry;
        HippoTrackInfo fourthTry;

        result = tryReadTrackOnce(&thirdTry);
        if (result != HAVE_TRACK)
            return result;
        result = tryReadTrackOnce(&fourthTry);
        if (result != HAVE_TRACK)
            return result;
        
        if (thirdTry != fourthTry) {
            hippoDebugLogW(L"Track properties changed midstream AGAIN - giving up");
            return NO_TRACK;
        } else {
            *info = fourthTry;
            return HAVE_TRACK;
        }
    } else {
        *info = secondTry;
        return HAVE_TRACK;
    }
}

gboolean
HippoYahooMonitorImpl::updateTrackTimeout(void *data)
{
    HippoYahooMonitorImpl *impl = static_cast<HippoYahooMonitorImpl*>(data);

    impl->updateTrackNow();

    impl->updateTrackTimeout_ = 0;
    return 0; // remove the timeout
}

// We do this in an update since we get an event for every property of the track from YME,
// and we don't want to propagate a billion track changed events out to the rest of the world.
void
HippoYahooMonitorImpl::trackNeedsUpdate()
{
    if (updateTrackTimeout_ != 0) {
        g_source_remove(updateTrackTimeout_);
        updateTrackTimeout_ = 0;
    }

    updateTrackTimeout_ = g_timeout_add(1500, updateTrackTimeout, this);
}

void
HippoYahooMonitorImpl::updateTrackNow()
{
    bool oldHaveTrack = haveTrack_;
    HippoTrackInfo oldTrack = track_;

    ReadTrackResult result = tryReadTrack(&track_);
    haveTrack_ = (result == HAVE_TRACK);

    if (!haveTrack_)
        track_.clear();

    hippoDebugLogW(L"yahoo track result: %d disconnect = %d", haveTrack_, result == COM_FAILURE);

    if (result == COM_FAILURE)
        disconnect();

    if (oldHaveTrack != haveTrack_ || oldTrack != track_) {
        if (wrapper_)
            wrapper_->fireCurrentTrackChanged(haveTrack_, track_);
    }

    hippoDebugLogW(L"yahoo fired track change event");
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoYahooMonitorImpl::QueryInterface(const IID &ifaceID,
                            void   **result)
{
    //hippoDebugLogU("%s", __FUNCTION__);
    if (IsEqualIID(ifaceID, IID_IUnknown)) {
        *result = static_cast<IUnknown *>(this);
    } else if (IsEqualIID(ifaceID, IID_IDispatch)) {
        *result = static_cast<IDispatch *>(this);
    } else if (IsEqualIID(ifaceID, DIID_DRMPRemoteEvents)) {
        *result = static_cast<IDispatch *>(this);
    } else {
        *result = 0;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;
}                                         

HIPPO_DEFINE_REFCOUNTING(HippoYahooMonitorImpl)

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoYahooMonitorImpl::GetTypeInfoCount(UINT *pctinfo)
{
    hippoDebugLogU("%s", __FUNCTION__);

    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoYahooMonitorImpl::GetTypeInfo(UINT        iTInfo,
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
HippoYahooMonitorImpl::GetIDsOfNames (REFIID    riid,
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
        
STDMETHODIMP
HippoYahooMonitorImpl::Invoke (DISPID        member,
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

#define DISPID_ONINFO 0x4014
    switch (member) {
    case DISPID_ONINFO:
        if (dispParams->cArgs != 1) {
            hippoDebugLogW(L"wrong number of args to OnInfo");
            return DISP_E_BADPARAMCOUNT;
        }

        if (!(dispParams->rgvarg[0].vt == VT_I4)) {
            hippoDebugLogW(L"no VT_I4");
            return DISP_E_BADVARTYPE;
        }

        {
            RemoteInfo info = static_cast<RemoteInfo>(dispParams->rgvarg[0].intVal);
            switch (info) {
                case YRI_GET_SONG_INFO:
                    hippoDebugLogW(L"YME song info event");
                    trackNeedsUpdate();
                    break;
                case YRI_SHUTDOWN:
                    hippoDebugLogW(L"YME shutdown event, disconnecting");
                    disconnect();
                    break;
                case YRI_GETCONTROLSTATE:
                    break;
                default:
                    hippoDebugLogW(L"YME event RemoteInfo=%d", (int) info);
                    break;
            }
        }
        break;
    default:
        hippoDebugLogW(L"Got event dispid %d", member);
        break;
    }

    return S_OK;
}

/////////////////////// public API ///////////////////////

HippoYahooMonitor::HippoYahooMonitor()
{
    impl_ = new HippoYahooMonitorImpl(this);
    impl_->Release();
}

HippoYahooMonitor::~HippoYahooMonitor()
{
    impl_->disconnect(); // called manually here since it requires the wrapper_
    impl_->wrapper_ = 0;
    impl_ = 0;
}

void
HippoYahooMonitor::setEnabled(bool enabled)
{
    impl_->setEnabled(enabled);
}

bool 
HippoYahooMonitor::hasCurrentTrack() const
{
    return impl_->haveTrack_;
}

const HippoTrackInfo&
HippoYahooMonitor::getCurrentTrack() const
{
    assert(hasCurrentTrack());
    return impl_->track_;
}
