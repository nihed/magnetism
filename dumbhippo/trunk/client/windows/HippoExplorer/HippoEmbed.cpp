/* HippoEmbed.cpp: ActiveX control to extend the capabilities of our web pages
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include "HippoEmbed.h"
#include "HippoExplorer_h.h"
#include "HippoDispID.h"
#include "HippoUILauncher.h"
#include "Guid.h"
#include "Globals.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <wininet.h> // For InternetCrackUr

class ShowChatLaunchListener : public HippoUILaunchListener {
public:
    ShowChatLaunchListener(BSTR postId);
    
    void onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui);
    void onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason);

private:
    HippoBSTR postId_;
};

ShowChatLaunchListener::ShowChatLaunchListener(BSTR postId)
{
    postId_ = postId;
}

void 
ShowChatLaunchListener::onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui)
{
    ui->ShowChatWindow(postId_);
    delete launcher;
    delete this;
}
 
void 
ShowChatLaunchListener::onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason)
{
    hippoDebug(L"%s", reason);
    delete launcher;
    delete this;
}

// This needs to be registered in the registry to be used; see 
// DllRegisterServer() (for self-registry during development) and Components.wxs
//
// Both need to be reversed when we start using this.

// "SUFFIX" by itself or "<foo>.SUFFIX" will be allowed. We might want to consider 
// changing things so that the control can only be used from *exactly* the web
// server specified in the preferences. (You'd have to check for either the
// normal or debug server.
static const WCHAR ALLOWED_HOST_SUFFIX[] = L"dumbhippo.com";

HippoEmbed::HippoEmbed(void)
{
    refCount_ = 1;
    safetyOptions_ = 0;
    dllRefCount++;
 
    connectionPointContainer_.setWrapper(static_cast<IObjectWithSite *>(this));

    // This could fail with out-of-memory, but there's nothing we can do in
    // the constructor. We'd need to rework ClassFactory to handle this
    connectionPointContainer_.addConnectionPoint(IID_IHippoEmbedEvents);

    hippoLoadRegTypeInfo(LIBID_HippoExplorer, 0, 1,
                         &IID_IHippoEmbed, &ifaceTypeInfo_,
                         &CLSID_HippoEmbed, &classTypeInfo_,
                         NULL);
}

HippoEmbed::~HippoEmbed(void)
{
    clearSite();

    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoEmbed::QueryInterface(const IID &ifaceID, 
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
    else if (IsEqualIID(ifaceID, IID_IHippoEmbed)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, DIID_DWebBrowserEvents2)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IConnectionPointContainer)) 
        *result = static_cast<IConnectionPointContainer *>(&connectionPointContainer_);
    else {
        // hippoDebug(L"QI for %x", ifaceID.Data1);

        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoEmbed)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoEmbed::SetSite(IUnknown *site)
{
    clearSite();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IServiceProvider>(&site_)))
            return E_FAIL;

        site_->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &browser_);

        HippoPtr<IServiceProvider> toplevel;
        site_->QueryService<IServiceProvider>(SID_STopLevelBrowser, &toplevel);
        if (toplevel)
            toplevel->QueryService<IWebBrowser2>(SID_SWebBrowserApp, &toplevelBrowser_);

        if (toplevelBrowser_) {
            HippoQIPtr<IConnectionPointContainer> container(toplevelBrowser_);
            if (container) {
                if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2,
                                                             &connectionPoint_))) 
                    connectionPoint_->Advise(static_cast<IObjectWithSite *>(this), // Disambiguate
                                             &connectionCookie_);
            }
        }
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoEmbed::GetSite(const IID &iid, 
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
HippoEmbed::GetInterfaceSafetyOptions (const IID &ifaceID, 
                                       DWORD     *supportedOptions, 
                                       DWORD     *enabledOptions)
{
    if (!supportedOptions || !enabledOptions)
        return E_INVALIDARG;

    if (IsEqualIID(ifaceID, IID_IDispatch)) {
        *supportedOptions = INTERFACESAFE_FOR_UNTRUSTED_CALLER;
        *enabledOptions = safetyOptions_;

        return S_OK;
    } else {
        *supportedOptions = 0;
        *enabledOptions = 0;

        return E_NOINTERFACE;
    }
}

STDMETHODIMP 
HippoEmbed::SetInterfaceSafetyOptions (const IID &ifaceID, 
                                       DWORD      optionSetMask, 
                                       DWORD      enabledOptions)
{
    if (IsEqualIID(ifaceID, IID_IDispatch)) {
        if ((optionSetMask & ~INTERFACESAFE_FOR_UNTRUSTED_CALLER) != 0)
            return E_FAIL;

        // INTERFACESAFE_FOR_UNSTRUSTED_CALLER covers use of a control
        // both for invoking methods on it and receiving events for it

        HippoBSTR url;
        if (browser_)
            browser_->get_LocationURL(&url);

        if (!url || !checkURL(url))
            return E_FAIL;

        safetyOptions_ = ((safetyOptions_ & ~optionSetMask) |
                          (enabledOptions & optionSetMask));

        return S_OK;
    } else {
        return E_NOINTERFACE;
    }
}

////////////////// IProvideClassInfo implementation /////////////////

STDMETHODIMP 
HippoEmbed::GetClassInfo (ITypeInfo **typeInfo) 
{
    if (!typeInfo)
        return E_POINTER;

    if (!classTypeInfo_)
        return E_OUTOFMEMORY;

    classTypeInfo_->AddRef();
    *typeInfo = classTypeInfo_;

    return S_OK;
}

//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoEmbed::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoEmbed::GetTypeInfo(UINT        iTInfo,
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
HippoEmbed::GetIDsOfNames (REFIID    riid,
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
HippoEmbed::Invoke (DISPID        member,
                    const IID    &iid,
                    LCID          lcid,              
                    WORD          flags,
                    DISPPARAMS   *dispParams,
                    VARIANT      *result,
                    EXCEPINFO    *excepInfo,  
                    unsigned int *argErr)
{
    if (member == DISPID_DOCUMENTCOMPLETE) {
         if (dispParams->cArgs == 2 &&
             dispParams->rgvarg[1].vt == VT_DISPATCH &&
              dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT))
         {
             if (dispParams->rgvarg[0].pvarVal->vt == VT_BSTR)
                 onDocumentComplete(dispParams->rgvarg[1].pdispVal,
                                    dispParams->rgvarg[0].pvarVal->bstrVal);
             return S_OK;
         } 
         else
             return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
    }

    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HippoQIPtr<IHippoEmbed> hippoEmbed(static_cast<IHippoEmbed *>(this));
    HRESULT hr = DispInvoke(hippoEmbed, ifaceTypeInfo_, member, flags, 
                             dispParams, result, excepInfo, argErr);

#if 0
    hippoDebug(L"Invoke: %#x - result %#x\n", member, hr);
#endif
    
    return hr;
}

//////////////////////// IHippoEmbed Methods ////////////////////////

STDMETHODIMP
HippoEmbed::CloseWindow()
{
    if (browser_)
        return browser_->Quit();
    else
        return E_FAIL;
}

STDMETHODIMP
HippoEmbed::ShowChatWindow(BSTR userId, BSTR postId)
{
    HippoUILauncher *launcher = new HippoUILauncher();
    HippoPtr<IHippoUI> ui;
    if (SUCCEEDED(launcher->getUI(&ui, userId))) {
        ui->ShowChatWindow(postId);
        delete launcher;
    } else {
        ShowChatLaunchListener *listener = new ShowChatLaunchListener(postId);
        launcher->setListener(listener);

        if (FAILED(launcher->launchUI())) {
            delete launcher;
            delete listener;
        }
    }

    return S_OK;
}

/////////////////////////////////////////////////////////////////////

 void
HippoEmbed::clearSite()
{
    site_ = NULL;

    if (connectionPoint_) {
        if (connectionCookie_) {
            connectionPoint_->Unadvise(connectionCookie_);
            connectionCookie_ = 0;
        }
           
        connectionPoint_ = NULL;
    }
}

void 
HippoEmbed::onDocumentComplete(IDispatch *dispatch, 
                               BSTR       url)
{
    // Only catch DocumentComplete() events that refer to the toplevel frame
    // ignore changes for subframes (<frame> or <iframe>)
    HippoQIPtr<IWebBrowser2> eventBrowser(dispatch);
    if (dispatch != toplevelBrowser_)
        return;

    HippoPtr<IConnectionPoint> point;
    if (FAILED(connectionPointContainer_.FindConnectionPoint(IID_IHippoEmbedEvents, &point)))
        return;

    HippoPtr<IEnumConnections> e;
    if (FAILED(point->EnumConnections(&e)))
        return;

    CONNECTDATA data;
    ULONG fetched;
    while (e->Next(1, &data, &fetched) == S_OK) {
//      HippoQIPtr<IHippoEmbedEvents> events(data.pUnk);
//      if (events)
//          events->LocationChanged(url);
        HippoQIPtr<IDispatch> dispatch(data.pUnk);
        if (dispatch) {
            DISPPARAMS dispParams;
            VARIANTARG arg;

            arg.vt = VT_BSTR;
            arg.bstrVal = url;

            dispParams.rgvarg = &arg;
            dispParams.cArgs = 1;
            dispParams.rgvarg[0];
            dispParams.cNamedArgs = 0;
            dispParams.rgdispidNamedArgs = NULL;

            HRESULT hr = dispatch->Invoke(HIPPO_DISPID_LOCATIONCHANGED, IID_IHippoEmbedEvents, 0 /* LCID */,
                                          DISPATCH_METHOD, &dispParams, 
                                          NULL /* result */, NULL /* exception */, NULL /* argError */);
            if (!SUCCEEDED(hr))
                hippoDebug(L"Invoke failed %x", hr);
        }
    }
}

bool
HippoEmbed::checkURL(BSTR url)
{
    URL_COMPONENTS components;
    ZeroMemory(&components, sizeof(components));
    components.dwStructSize = sizeof(components);

    // The case where lpszHostName is NULL and dwHostNameLength is non-0 means
    // to return pointers into the passed in URL along with lengths. The 
    // specific non-zero value is irrelevant
    components.dwHostNameLength = 1;
    components.dwUserNameLength = 1;
    components.dwPasswordLength = 1;
    components.dwUrlPathLength = 1;
    components.dwExtraInfoLength = 1;

    if (!InternetCrackUrl(url, 0, 0, &components))
        return false;

    if (components.nScheme != INTERNET_SCHEME_HTTP && components.nScheme != INTERNET_SCHEME_HTTPS)
        return false;

    HippoBSTR foo(components.dwHostNameLength, components.lpszHostName);

    size_t allowedHostLength = wcslen(ALLOWED_HOST_SUFFIX);
    if (components.dwHostNameLength < allowedHostLength)
        return false;

    // check for "SUFFIX" or "<foo>.SUFFIX"
    if (wcsncmp(components.lpszHostName + components.dwHostNameLength - allowedHostLength,
                ALLOWED_HOST_SUFFIX,
                allowedHostLength) != 0)
        return false;
    if (components.dwHostNameLength > allowedHostLength && 
        *(components.lpszHostName + components.dwHostNameLength - allowedHostLength - 1) != '.')
        return false;

    return true;
}
