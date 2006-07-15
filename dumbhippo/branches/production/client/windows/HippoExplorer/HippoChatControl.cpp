/* HippoChatControl.cpp: ActiveX control to extend the capabilities of our web pages
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include "HippoChatControl.h"
#include <HippoURLParser.h>
#include <HippoUtilDispId.h>
#include "HippoExplorer_h.h"
#include "HippoExplorerDispID.h"
#include "HippoExplorerUtil.h"
#include "HippoUILauncher.h"
#include "Guid.h"
#include "Globals.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <wininet.h> // For InternetCrackUr

// This needs to be registered in the registry to be used; see 
// DllRegisterServer() (for self-registry during development) and Components.wxs

HippoChatControl::HippoChatControl(void)
{
    refCount_ = 1;
    safetyOptions_ = 0;
    dllRefCount++;

    memberCount_ = 0;
    participantCount_ = 0;
 
    connectionPointContainer_.setWrapper(static_cast<IObjectWithSite *>(this));

    // This could fail with out-of-memory, but there's nothing we can do in
    // the constructor. We'd need to rework ClassFactory to handle this
    connectionPointContainer_.addConnectionPoint(IID_IHippoChatRoomEvents);

    hippoLoadRegTypeInfo(LIBID_HippoExplorer, 0, 1,
                         &IID_IHippoChatRoom, &ifaceTypeInfo_,
                         &CLSID_HippoChatControl, &classTypeInfo_,
                         NULL);
}

HippoChatControl::~HippoChatControl(void)
{
    clearUI();
    clearSite();

    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoChatControl::QueryInterface(const IID &ifaceID, 
                             void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IObjectSafety)) 
        *result = static_cast<IObjectSafety *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IProvideClassInfo))
        *result = static_cast<IProvideClassInfo *>(this);
    else if (IsEqualIID(ifaceID, IID_IPersist))
        *result = static_cast<IPersist *>(this);
    else if (IsEqualIID(ifaceID, IID_IPersistPropertyBag))
        *result = static_cast<IPersistPropertyBag *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoChatRoom)) 
        *result = static_cast<IHippoChatRoom *>(this);
    else if (IsEqualIID(ifaceID, DIID_DWebBrowserEvents2)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IConnectionPointContainer)) 
        *result = static_cast<IConnectionPointContainer *>(&connectionPointContainer_);
    else {
        // hippoDebugDialog(L"QI for %x", ifaceID.Data1);

        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoChatControl)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoChatControl::SetSite(IUnknown *site)
{
    clearSite();
    clearUI();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IServiceProvider>(&site_)))
            return E_FAIL;

        site_->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &browser_);

        connectToUI();
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoChatControl::GetSite(const IID &iid, 
                    void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}


//////////////////////// IObjectSafety Methods //////////////////////

STDMETHODIMP 
HippoChatControl::GetInterfaceSafetyOptions (const IID &ifaceID, 
                                             DWORD     *supportedOptions, 
                                             DWORD     *enabledOptions)
{
    if (!supportedOptions || !enabledOptions)
        return E_INVALIDARG;

    if (IsEqualIID(ifaceID, IID_IDispatch)) {
        *supportedOptions = INTERFACESAFE_FOR_UNTRUSTED_CALLER;
        *enabledOptions = safetyOptions_ & INTERFACESAFE_FOR_UNTRUSTED_CALLER;

        return S_OK;
    } else if (IsEqualIID(ifaceID, IID_IPersist)) {
        *supportedOptions = INTERFACESAFE_FOR_UNTRUSTED_DATA;
        *enabledOptions = safetyOptions_ & INTERFACESAFE_FOR_UNTRUSTED_DATA;

        return S_OK;
    } else {
        *supportedOptions = 0;
        *enabledOptions = 0;

        return E_NOINTERFACE;
    }
}

STDMETHODIMP 
HippoChatControl::SetInterfaceSafetyOptions (const IID &ifaceID, 
                                             DWORD      optionSetMask, 
                                             DWORD      enabledOptions)
{
    if (IsEqualIID(ifaceID, IID_IDispatch)) {
        if ((optionSetMask & ~INTERFACESAFE_FOR_UNTRUSTED_CALLER) != 0)
            return E_FAIL;

        // INTERFACESAFE_FOR_UNSTRUSTED_CALLER covers use of a control
        // both for invoking methods on it and receiving events for it

        if (!isSiteSafe())
            return E_FAIL;

        safetyOptions_ = ((safetyOptions_ & ~optionSetMask) |
                          (enabledOptions & optionSetMask));

        return S_OK;
    } else if (IsEqualIID(ifaceID, IID_IPersistPropertyBag)) {
        if ((optionSetMask & ~INTERFACESAFE_FOR_UNTRUSTED_DATA) != 0)
            return E_FAIL;

        //
        // INTERFACESAFE_FOR_UNSTRUSTED_DATA covers initializing a
        // control via <param> elements; unfortunately, IE checks
        // this before setting the site, so we have no way of figuring
        // out the URL, and have to allow it even if a foreign site
        // initializes our control. But we don't do anything at 
        // initialization other than store some strings, so that should
        // be pretty safe.
        // 

        safetyOptions_ = ((safetyOptions_ & ~optionSetMask) |
                          (enabledOptions & optionSetMask));

        return S_OK;
    } else {
        return E_NOINTERFACE;
    }
}

////////////////// IProvideClassInfo implementation /////////////////

STDMETHODIMP 
HippoChatControl::GetClassInfo (ITypeInfo **typeInfo) 
{
    if (!typeInfo)
        return E_POINTER;

    if (!classTypeInfo_)
        return E_OUTOFMEMORY;

    classTypeInfo_->AddRef();
    *typeInfo = classTypeInfo_;

    return S_OK;
}

//////////////////////// IPersist implementation ///////////////////

STDMETHODIMP 
HippoChatControl::GetClassID(CLSID *classID)
{
    *classID = CLSID_HippoChatControl;

    return S_OK;
}

//////////////////////// IPersistPropertyBag implementation ///////////////////

STDMETHODIMP 
HippoChatControl::InitNew()
{
    return S_OK;
}

static bool
isHippoGuid(variant_t & value)
{
    return (value.vt == VT_BSTR && value.bstrVal != NULL && hippoVerifyGuid(value.bstrVal));
}

STDMETHODIMP 
HippoChatControl::Load(IPropertyBag *propertyBag,
                       IErrorLog    *errorLog)
{
    HRESULT hr;

    clearUI();

    variant_t userIdVariant;
    userIdVariant.vt = VT_BSTR;
    hr = propertyBag->Read(L"UserID", &userIdVariant, errorLog);
    if (FAILED(hr))
        return hr;

    if (!isHippoGuid(userIdVariant)) {
        hippoDebugDialog(L"Error setting UserID property");
        return E_FAIL;
    }

    // If Read fails get E_INVALIDARG. But we simplify by
    // assumming that any failure will leave the output
    // variant null.

    variant_t chatIdVariant((BSTR)NULL);
    propertyBag->Read(L"ChatID", &chatIdVariant, errorLog);

    // we accept "PostID" as a synonym for ChatID for back compat
    // with old html, can kill this after a while.
    variant_t postIdVariant((BSTR)NULL);
    propertyBag->Read(L"PostID", &postIdVariant, errorLog);

    // fall back to PostID if no ChatID
    if (isHippoGuid(chatIdVariant)) {
        chatId_ = chatIdVariant.bstrVal;
    } else {
        if (isHippoGuid(postIdVariant)) {
            chatId_ = postIdVariant.bstrVal;
        } else {
            hippoDebugDialog(L"Error setting ChatID property");
            return E_FAIL;
        }
    }

    userId_ = userIdVariant.bstrVal;

    connectToUI();

    return S_OK;
}

STDMETHODIMP 
HippoChatControl::Save(IPropertyBag *propertyBag,
                       BOOL          clearDirty,
                       BOOL          saveAllProperties)
{
    // This isn't needed for us, but simple enough to implement

    HRESULT hr;

    variant_t userIdVariant(userId_.m_str);
    hr = propertyBag->Write(L"UserID", &userIdVariant);
    if (FAILED(hr))
        return hr;

    variant_t chatIdVariant(chatId_.m_str);
    hr = propertyBag->Write(L"ChatID", &chatIdVariant);
    if (FAILED(hr))
        return hr;

    return S_OK;
}

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoChatControl::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoChatControl::GetTypeInfo(UINT        iTInfo,
                              LCID        lcid,
                              ITypeInfo **ppTInfo)
{
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
HippoChatControl::GetIDsOfNames (REFIID    riid,
                                 LPOLESTR *rgszNames,
                                 UINT      cNames,
                                 LCID      lcid,
                                 DISPID   *rgDispId)
 {
    if (!ifaceTypeInfo_)
         return E_OUTOFMEMORY;

    return DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
 }
        
STDMETHODIMP
HippoChatControl::Invoke (DISPID        member,
                          const IID    &iid,
                          LCID          lcid,              
                          WORD          flags,
                          DISPPARAMS   *dispParams,
                          VARIANT      *result,
                          EXCEPINFO    *excepInfo,  
                          unsigned int *argErr)
{
    // Forward chat room events on to our connected listeners
    if (member == HIPPO_DISPID_ONUSERJOIN ||
        member == HIPPO_DISPID_ONUSERLEAVE ||
        member == HIPPO_DISPID_ONMESSAGE ||
        member == HIPPO_DISPID_ONRECONNECT ||
        member == HIPPO_DISPID_ONUSERMUSICCHANGE) 
    {
        HRESULT hr;

        HippoPtr<IConnectionPoint> point;
        hr = connectionPointContainer_.FindConnectionPoint(IID_IHippoChatRoomEvents, &point);
        if (FAILED(hr))
            return hr;

        HippoPtr<IEnumConnections> e;
        hr = point->EnumConnections(&e);
        if (FAILED(hr))
            return hr;

        CONNECTDATA data;
        ULONG fetched;
        while (e->Next(1, &data, &fetched) == S_OK) {
            HippoQIPtr<IDispatch> dispatch(data.pUnk);
            if (dispatch) {
                // If we passed the result return value, then we'd have to worry about freeing 
                // it to avoid leaks when it was overwritten. Just pass in null for the
                // return value since these events don't have return values
                hr = dispatch->Invoke(member, IID_NULL, 0 /* LCID */,
                                      DISPATCH_METHOD, dispParams, 
                                      NULL /* result */, NULL /* exception */, NULL /* argError */);

                // we debug log failure but otherwise ignore it
                if (FAILED(hr))
                    hippoDebugLogW(L"Invoke of notification %d failed: %x", member, hr);
            }
        }

        return S_OK;
    }

    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HippoQIPtr<IHippoChatRoom> hippoChatControl(static_cast<IHippoChatRoom *>(this));
    HRESULT hr = DispInvoke(hippoChatControl, ifaceTypeInfo_, member, flags, 
                             dispParams, result, excepInfo, argErr);

#if 0
    hippoDebugDialog(L"Invoke: %#x - result %#x\n", member, hr);
#endif
    
    return hr;
}

//////////////////////// IHippoChatRoom Methods ////////////////////////
    
STDMETHODIMP 
HippoChatControl::Join(BOOL participant)
{
    // We multiplex all calls to Join() either as participant or guest
    // into a single membership in the system copy of the chatroom, as
    // participant if we have any participant calls to Join(), otherwise
    // as a guest.

    memberCount_++;
    if (participant)
        participantCount_++;

    hippoDebugLogW(L"HippoChatControl::Join memberCount = %d participantCount = %d",
                   memberCount_, participantCount_);

    if (chatRoom_) {
        if (memberCount_ == 1) {
            chatRoom_->Join(participant);
        } else if (participant && participantCount_ == 1) {
            // Was a guest, rejoin as a participant, 
            chatRoom_->Join(TRUE);
            chatRoom_->Leave(FALSE);
        }
    }

    return S_OK;
}

STDMETHODIMP 
HippoChatControl::Leave(BOOL participant)
{
    if (memberCount_ == 0 || (participant && participantCount_ == 0))
        return E_INVALIDARG;

    memberCount_--;
    if (participant)
        participantCount_--;

    hippoDebugLogW(L"HippoChatControl::Leave memberCount = %d participantCount = %d",
                   memberCount_, participantCount_);

    if (chatRoom_) {
        if (memberCount_ == 0) {
            chatRoom_->Leave(participant);
        } else if (participant && participantCount_ == 0) {
            // Was a participant, rejoin as a guest
            chatRoom_->Join(FALSE);
            chatRoom_->Leave(TRUE);
        }
    }

    return S_OK;
}

STDMETHODIMP 
HippoChatControl::SendMessage(BSTR text)
{
    if (chatRoom_)
        return chatRoom_->SendMessage(text);

    return S_OK;
}

STDMETHODIMP 
HippoChatControl::Rescan()
{
    if (chatRoom_)
        chatRoom_->Rescan();

    return S_OK;
}

/////////////////////////////////////////////////////////////////////

 void
HippoChatControl::clearSite()
{
    site_ = NULL;
}

bool
HippoChatControl::isSiteSafe()
{
    HippoBSTR url;
    if (browser_)
        browser_->get_LocationURL(&url);

    return url && checkURL(url);
}

bool
HippoChatControl::checkURL(BSTR url)
{
    HippoURLParser parser(url);

    if (!parser.ok())
        return false;

    INTERNET_SCHEME scheme = parser.getScheme();
    if (scheme != INTERNET_SCHEME_HTTP && scheme != INTERNET_SCHEME_HTTPS)
        return false;

    if (!hippoIsOurServer(parser.getHostName()))
        return false;

    return true;
}

void
HippoChatControl::connectToUI()
{
    HippoUILauncher launcher;

    if (chatRoom_)
        return;

    if (!chatId_ || !userId_ || !site_)
        return;

    // Double check the URL for the page at this point; the control was initialized 
    // before we could do so, and this function may do things like cause DumbHippo
    // to be launched. Nothing should be dangerous, per se, but there may be some
    // DOS ability.
    if (!isSiteSafe())
        return;

    if (FAILED(launcher.getUI(&ui_, userId_))) {
        hippoDebugDialog(L"Couldn't find UI");
        return;
    }

    if (FAILED(ui_->GetChatRoom(chatId_, &chatRoom_))) {
        hippoDebugDialog(L"Couldn't get chat room");
        return;
    }

    if (memberCount_)
        chatRoom_->Join(participantCount_ > 0);

    HippoQIPtr<IConnectionPointContainer> container(chatRoom_);
    if (container) {
        if (SUCCEEDED(container->FindConnectionPoint(IID_IHippoChatRoomEvents,
                                                     &chatRoomConnection_)))
        {
            chatRoomConnection_->Advise(static_cast<IObjectWithSite *>(this), // Disambiguate
                                        &chatRoomCookie_);
        }
        
    }
}

void
HippoChatControl::clearUI()
{
    if (chatRoomConnection_ != NULL) {
        if (chatRoomCookie_) {
            chatRoomConnection_->Unadvise(chatRoomCookie_);
            chatRoomConnection_ = 0;
        }
           
        chatRoomConnection_ = NULL;
    }

    if (chatRoom_) {
        if (memberCount_)
            chatRoom_->Leave(participantCount_ > 0);
        chatRoom_ = NULL;
    }
    
    if (ui_)
        ui_ = NULL;
}
