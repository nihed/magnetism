/* HippoEmbed.cpp: ActiveX control to extend the capabilities of our web pages
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoexplorer.h"

#include "HippoEmbed.h"
#include "HippoExplorer_h.h"
#include "HippoExplorerDispID.h"
#include "HippoExplorerUtil.h"
#include "HippoUILauncher.h"
#include <HippoRegKey.h>
#include <HippoURLParser.h>
#include "Guid.h"
#include "Globals.h"
#include <comutil.h>
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

// This needs to be registered in the registry to be used; see 
// DllRegisterServer() (for self-registry during development) and Components.wxs

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
    AllowSetForegroundWindow(ASFW_ANY);
    ui->ShowChatWindow(postId_);
    delete launcher;
    delete this;
}
 
void 
ShowChatLaunchListener::onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason)
{
    hippoDebugDialog(L"%s", reason);
    delete launcher;
    delete this;
}

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

    HRESULT hr = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);

    if (cNames == 1)
        hippoDebugLogW(L"%ls = %d (%x)", rgszNames[0], hr, rgDispId[0]);

    return hr;
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
        // In theory, we should restrict setting the foreground to the HippoUI process, 
        // the chance of some other app getting in the way is small
        AllowSetForegroundWindow(ASFW_ANY);
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

STDMETHODIMP 
HippoEmbed::OpenBrowserBar()
{
    return showHideBrowserBar(true);
}

STDMETHODIMP 
HippoEmbed::CloseBrowserBar()
{
    return showHideBrowserBar(false);
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
                hippoDebugDialog(L"Invoke failed %x", hr);
        }
    }
}

bool
HippoEmbed::checkURL(BSTR url)
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

HRESULT 
HippoEmbed::showHideBrowserBar(bool doShow)
{
    if (!toplevelBrowser_)
        return E_FAIL;

    variant_t classId(L"{174D2323-9AF2-4257-B8BD-849865E4F1AE}"); // CLSID_HippoExplorerBar
    variant_t show(doShow);
    variant_t size;

    // We want to change our browser bar for this window, but we don't want the change
    // to stick in the registry, so we save the value of the relevant registry key
    // and restore it afterwards.

    BYTE *oldRegistryData = NULL;
    DWORD oldRegistryLength = 0;

    {
        HippoRegKey key(HKEY_CURRENT_USER, L"Software\\Microsoft\\Internet Explorer\\Toolbar\\WebBrowser", false);
        key.loadBinary(L"ITBarLayout", &oldRegistryData, &oldRegistryLength);
    }

    HRESULT hr = toplevelBrowser_->ShowBrowserBar(&classId, &show, &size);

    {
        HippoRegKey key(HKEY_CURRENT_USER, L"Software\\Microsoft\\Internet Explorer\\Toolbar\\WebBrowser", true);
        key.saveBinary(L"ITBarLayout", oldRegistryData, oldRegistryLength);
    }

    return hr;
}
