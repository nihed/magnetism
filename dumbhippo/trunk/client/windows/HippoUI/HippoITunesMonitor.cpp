#include "StdAfx.h"
#include "HippoUtil.h"
#include "HippoITunesMonitor.h"
#include "HippoLogWindow.h"
#include <glib.h>

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

private:
    long data_[4];
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
	HippoPtr<ITypeInfo> ifaceTypeInfo_;
	HippoPtr<IConnectionPoint> connectionPoint_;
	DWORD connectionCookie_;
#define CHECK_RUNNING_TIMEOUT (1000*30)
    unsigned int timeout_id_;
    bool firstTimeout_;

	void attemptConnect();
	void disconnect();
	void setTrack(IITTrack *track);
	bool readTrackInfo(IITTrack *track, HippoTrackInfo *info);

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
	: wrapper_(wrapper), haveTrack_(false), state_(NO_ITUNES), refCount_(1), firstTimeout_(true)
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
HippoITunesMonitorImpl::attemptConnect()
{
	hippoDebugLogU("ITUNES CONNECT %s", __FUNCTION__);

	if (state_ == CONNECTED)
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
	hippoDebugLogW(L"All connected up, supposedly; cookie: %d", (int) connectionCookie_);

	listConnections(connectionPoint_);

	state_ = CONNECTED;

	HippoPtr<IITTrack> trackPtr;
	hRes = iTunesPtr->get_CurrentTrack(&trackPtr);
	if (FAILED(hRes)) {
		hippoDebugLogW(L"Failed to get current track after reconnecting");
		disconnect();
		return;
	}

	setTrack(trackPtr);
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

	state_ = NO_ITUNES;
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
        hippoDebugLogU("  got prop %s", #hProp);                \
        hippoDebugLogW(L" %s", val ## hProp .m_str);            \
    }                                                           \
    } while(0)

#define GET_PROPERTY_END_LONG(obj, iProp, hProp)                \
    do {                                                        \
	if (obj != 0) {                                             \
        info->set ## hProp (val ## hProp);                      \
        hippoDebugLogU("  got prop %s", #hProp);                \
        hippoDebugLogW(L" %ld", val ## hProp );                 \
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
        hippoDebugLogU("  got prop %s", #hProp);                \
        hippoDebugLogW(L" %ld", (long) val ## hProp );          \
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
    hippoDebugLogW(L"Bounds of dimension 2 are [%ld,%ld)", lower, upper);

    // it appears that iTunes will return less than 4 ids if the extra ones are 0? 
    // not really sure what's going on with that...
    if ((upper - lower) > 4) {
        hippoDebugLogW(L"Bad array bounds on dimension 2, [%ld,%ld)", lower, upper);
        return DISP_E_BADVARTYPE;
    }

    long idComponents[4] = { 0, 0, 0, 0 };
    long coord[2];
    coord[0] = i;
    for (coord[1] = lower; coord[1] < upper; coord[1] += 1) {
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
    // if the array is empty we seem to get lower=0 upper=-1 for some reason...
    hippoDebugLogW(L"Bounds of dimension 1 are [%ld,%ld)", lower, upper);

    for (long i = lower; i < upper; ++i) {
        ITunesObjectId id;
        hRes = getIdFromArray(array, i, &id);
        if (FAILED(hRes))
            return hRes;
        ids_p->push_back(id);
    }
    return S_OK;
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

#define BITS_SET(val, flags) (((val) & (flags)) == flags)
#define ARG_HAS_TYPE(i, flags) BITS_SET(dispParams->rgvarg[(i)].vt, (flags))
#define CHECK_ARG_TYPE(i, types) do {                                   \
    if (!ARG_HAS_TYPE(i, types)) {                                      \
        hippoDebugLogU("arg %d missing types %s", i, #types);           \
        return DISP_E_BADVARTYPE;                                       \
    } } while(0)
#define CHECK_ARG_COUNT(expected) do {                  \
     	if (dispParams->cArgs != 2) {                   \
			hippoDebugLogW(L"wrong number of args");    \
			return DISP_E_BADPARAMCOUNT;                \
        } } while (0)

#if 0
    // debug spew of args when trying to figure out what's up
    for (unsigned int argc = 0; argc < dispParams->cArgs; ++argc) {
#define SPAM_ARG_TYPE(i, type) if (ARG_HAS_TYPE(i, type)) hippoDebugLogU("Arg %d has type %s", (i), #type)
        SPAM_ARG_TYPE(argc, VT_UNKNOWN);
        SPAM_ARG_TYPE(argc, VT_VARIANT);
        SPAM_ARG_TYPE(argc, VT_ARRAY);
        SPAM_ARG_TYPE(argc, VT_SAFEARRAY);
        SPAM_ARG_TYPE(argc, VT_I4);
        SPAM_ARG_TYPE(argc, VT_PTR);
    }
#endif

	switch (member) {
	case DISPID_ONDATABASECHANGEDEVENT:
        hippoDebugLogW(L"database changed");
        // Two parameters, deletedObjects / changedOrAddedObjects
        // each parameter is a 2D SAFEARRAY of VARIANT with the variants
        // containing type VT_I4
        // The first dimension is of size # of objects, the second is size 4
        // the 4 items in the second dimension are the 
        // source, playlist, track, track database) IDs

        CHECK_ARG_COUNT(2);

        CHECK_ARG_TYPE(0, VT_ARRAY | VT_VARIANT);
        CHECK_ARG_TYPE(1, VT_ARRAY | VT_VARIANT);

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

            for (std::vector<ITunesObjectId>::const_iterator i = deleted.begin(); i != deleted.end(); ++i) {
                hippoDebugLogW(L"Deleted %s", i->toString().c_str());
            }

            for (std::vector<ITunesObjectId>::const_iterator i = changed.begin(); i != changed.end(); ++i) {
                hippoDebugLogW(L"Changed %s", i->toString().c_str());
            }
        }
		break;
	case DISPID_ONPLAYERPLAYEVENT:
		hippoDebugLogW(L"player play");

        CHECK_ARG_COUNT(1);

        CHECK_ARG_TYPE(0, VT_UNKNOWN | VT_PTR | VT_VARIANT);

		// FIXME it's probably better to just queue an idle that gets the 
		// current track

		{
			HippoQIPtr<IITTrack> track(dispParams->rgvarg[0].punkVal);
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
        CHECK_ARG_COUNT(1);

        CHECK_ARG_TYPE(0, VT_UNKNOWN | VT_PTR | VT_VARIANT);

        {
			HippoQIPtr<IITTrack> track(dispParams->rgvarg[0].punkVal);
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
	impl_->wrapper_ = 0;
	impl_ = 0;
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

std::vector<HippoPlaylist::Id>
HippoITunesMonitor::getPlaylists() const
{
    // FIXME
    return std::vector<HippoPlaylist::Id>();
}

HippoPtr<HippoPlaylist>
HippoITunesMonitor::getPlaylist(const HippoPlaylist::Id &id) const
{
    // FIXME
    return 0;    
}

HippoPtr<HippoPlaylist>
HippoITunesMonitor::getPrimingTracks() const
{
    // FIXME
    return 0;
}
