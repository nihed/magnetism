/* HippoControl.cpp: ActiveX control to extend the capabilities of our web pages
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoexplorer.h"

#include "HippoControl.h"
#include "HippoExplorer_h.h"
#include "HippoExplorerDispID.h"
#include "HippoExplorerUtil.h"
#include "HippoUILauncher.h"
#include <HippoInvocation.h>
#include <HippoRegKey.h>
#include <HippoURLParser.h>
#include "Guid.h"
#include "Globals.h"
#include <comutil.h>
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include <hippo-com-ipc-locator.h>
#include <dispex.h>

// This needs to be registered in the registry to be used; see 
// DllRegisterServer() (for self-registry during development) and Components.wxs

class HippoControlLaunchListener : public HippoUILaunchListener {
public:
    HippoControlLaunchListener(BSTR postId);
    
    void onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui);
    void onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason);

private:
    HippoBSTR postId_;
};

HippoControlLaunchListener::HippoControlLaunchListener(BSTR postId)
{
    postId_ = postId;
}

void 
HippoControlLaunchListener::onLaunchSuccess(HippoUILauncher *launcher, IHippoUI *ui)
{
    AllowSetForegroundWindow(ASFW_ANY);
    ui->ShowChatWindow(postId_);
    delete launcher;
    delete this;
}
 
void 
HippoControlLaunchListener::onLaunchFailure(HippoUILauncher *launcher, const WCHAR *reason)
{
    delete launcher;
    delete this;
}

HippoControl::HippoControl(void)
{
    refCount_ = 1;
    safetyOptions_ = 0;
    dllRefCount++;

    controller_ = NULL;
    endpoint_ = 0;
 
    hippoLoadRegTypeInfo(LIBID_HippoExplorer, 0, 1,
                         &IID_IHippoControl, &ifaceTypeInfo_,
                         &CLSID_HippoControl, &classTypeInfo_,
                         NULL);
}

HippoControl::~HippoControl(void)
{
    hippoDebugLogW(L"Finalizing HippoControl");

    clearSite();

    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoControl::QueryInterface(const IID &ifaceID, 
                               void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
//    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
//        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IObjectSafety)) 
        *result = static_cast<IObjectSafety *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(static_cast<IDispatchEx *>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatchEx)) 
        *result = static_cast<IDispatchEx *>(this);
    else if (IsEqualIID(ifaceID, IID_IProvideClassInfo))
        *result = static_cast<IProvideClassInfo *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoControl)) 
        *result = static_cast<IHippoControl *>(this);
    else {
        // hippoDebug(L"QI for %x", ifaceID.Data1);

        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

STDMETHODIMP_(DWORD)
HippoControl::AddRef() 
{
    refCount_++;

    return refCount_;
}

STDMETHODIMP_(DWORD)
HippoControl::Release() 
{
    refCount_--;
    if (refCount_ == 0) {
        delete this;
        return 0;
    } else {
        return refCount_;
    }
}

// HIPPO_DEFINE_REFCOUNTING(HippoControl)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoControl::SetSite(IUnknown *site)
{
    clearSite();

    if (site) 
    {
        if (FAILED(site->QueryInterface<IServiceProvider>(&site_)))
            return E_FAIL;
    }

    return S_OK;
}

STDMETHODIMP 
HippoControl::GetSite(const IID &iid, 
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
HippoControl::GetInterfaceSafetyOptions (const IID &ifaceID, 
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
HippoControl::SetInterfaceSafetyOptions (const IID &ifaceID, 
                                         DWORD      optionSetMask, 
                                         DWORD      enabledOptions)
{
    if (IsEqualIID(ifaceID, IID_IDispatch)) {
        if ((optionSetMask & ~INTERFACESAFE_FOR_UNTRUSTED_CALLER) != 0)
            return E_FAIL;

        // INTERFACESAFE_FOR_UNSTRUSTED_CALLER covers use of a control
        // both for invoking methods on it and receiving events for it

        // For extra safety, we only want to permit our control to be used
        // from *.mugshot.org and *.dumbhippo.com, but when our control
        // is created via new ActiveXObject, SetSite() is called *after*
        // SetInterfaceSafetyOptions(), so we can't check the caller here,
        // but have to do it in individual methods.

        safetyOptions_ = ((safetyOptions_ & ~optionSetMask) |
                          (enabledOptions & optionSetMask));

        return S_OK;
    } else {
        return E_NOINTERFACE;
    }
}

////////////////// IProvideClassInfo implementation /////////////////

STDMETHODIMP 
HippoControl::GetClassInfo (ITypeInfo **typeInfo) 
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
HippoControl::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoControl::GetTypeInfo(UINT        iTInfo,
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
HippoControl::GetIDsOfNames (REFIID    riid,
                             LPOLESTR *rgszNames,
                             UINT      cNames,
                             LCID      lcid,
                             DISPID   *rgDispId)
 {
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HRESULT hr = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);

    return hr;
 }
        
STDMETHODIMP
HippoControl::Invoke (DISPID        member,
                      const IID    &iid,
                      LCID          lcid,              
                      WORD          flags,
                      DISPPARAMS   *dispParams,
                      VARIANT      *result,
                      EXCEPINFO    *excepInfo,  
                      unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HippoQIPtr<IHippoControl> hippoControl(static_cast<IHippoControl *>(this));
    HRESULT hr = DispInvoke(hippoControl, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);

    return hr;
}

//////////////////////// IDispatchEx implementation ///////////////////

STDMETHODIMP 
HippoControl::GetDispID (BSTR    name, 
                         DWORD   options, 
                         DISPID *member)
{
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    if (!member)
        return E_INVALIDARG;

    return ifaceTypeInfo_->GetIDsOfNames(&name, 1, member);
}

STDMETHODIMP 
HippoControl::InvokeEx (DISPID            member,
                        LCID              lcid,
                        WORD              flags,
                        DISPPARAMS       *dispParams,
                        VARIANT          *result,
                        EXCEPINFO        *excepInfo,  
                        IServiceProvider *serviceProvider)
{
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    HippoPtr<IServiceProvider> oldSite = site_;
    site_ = serviceProvider;

    HippoQIPtr<IHippoControl> hippoControl(static_cast<IHippoControl *>(this));
    HRESULT hr = DispInvoke(hippoControl, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, NULL);

    site_ = oldSite;

    return hr;
}
   
STDMETHODIMP
HippoControl::DeleteMemberByName (BSTR name, DWORD options)
{
    return E_NOTIMPL;
}

STDMETHODIMP
HippoControl::DeleteMemberByDispID (DISPID member)
{
    return E_NOTIMPL;
}

STDMETHODIMP
HippoControl::GetMemberProperties (DISPID member, DWORD fetch, DWORD *properties)
{
    return E_NOTIMPL;
}

STDMETHODIMP 
HippoControl::GetMemberName (DISPID member, BSTR *name)
{
    if (!ifaceTypeInfo_) 
         return E_OUTOFMEMORY;

    if (!name)
        return E_INVALIDARG;

    UINT count;
    HRESULT hr = ifaceTypeInfo_->GetNames(member, name, 1, &count);

    if (FAILED(hr))
        return hr;

    return count > 0 ? S_OK : DISP_E_UNKNOWNNAME;
}

STDMETHODIMP
HippoControl::GetNextDispID (DWORD which, DISPID member, DISPID *nextMember)
{
    return E_NOTIMPL;
}

STDMETHODIMP
HippoControl::GetNameSpaceParent (IUnknown **parentNamespace)
{
    return E_NOTIMPL;
}

//////////////////////// IHippoControl Methods ////////////////////////

STDMETHODIMP 
HippoControl::start(BSTR serverUrl)
{
    hippoDebugLogW(L"start %ls", serverUrl);

    // As a little extra security, we only allow operation from *.mugshot.org
    // and *.dumbhippo.com. Because of the check below against cross-site
    // operation this isn't very important, but the browser bar doesn't work
    // outside of these domains anyways, so we might as well be conservative.
    if (!siteIsTrusted())
        return E_ACCESSDENIED;

    // Check to make sure that serverUrl points to the same site as the page
    // loading the control

    HippoURLParser serverUrlParser(serverUrl);

    HippoBSTR browserUrl;
    HippoPtr<IWebBrowser2> browser;
    if (getBrowser(&browser))
        browser->get_LocationURL(&browserUrl);

    if (!browserUrl)
        return E_ACCESSDENIED;

    HippoURLParser browserUrlParser(browserUrl);

    if (browserUrlParser.getScheme() != serverUrlParser.getScheme())
        return E_ACCESSDENIED;
    if (browserUrlParser.getPort() != serverUrlParser.getPort())
        return E_ACCESSDENIED;
    if (!browserUrlParser.getHostName() || !serverUrlParser.getHostName() ||
        wcscmp(browserUrlParser.getHostName(), serverUrlParser.getHostName()) != 0)
        return E_ACCESSDENIED;

    // Look up the controller for the site as host:port
    
    HippoBSTR serverName(serverUrlParser.getHostName());
    serverName.Append(L":");

    WCHAR buffer[16];
    StringCchPrintfW(buffer, sizeof(buffer)/sizeof(buffer[0]), L"%d", serverUrlParser.getPort());

    serverName.Append(buffer);

    HippoUStr serverNameU(serverName);

    hippoDebugLogW(L"In start, thread is %d", GetCurrentThreadId());
    controller_ = HippoComIpcLocator::getInstance()->getController(serverNameU.c_str());
    controller_->addListener(this);

    endpoint_ = controller_->registerEndpoint(this);
    if (endpoint_ != 0)
        hippoDebugLogW(L"HippoControl: Succesfully registered endpoint at startup");

    return S_OK;
}
       
STDMETHODIMP 
HippoControl::stop()
{
    hippoDebugLogW(L"stop");

    if (controller_) {
        if (endpoint_ != 0) {
            controller_->unregisterEndpoint(endpoint_);
            endpoint_ = 0;
        }

        controller_->removeListener(this);
        HippoComIpcLocator::getInstance()->releaseController(controller_);
        controller_ = NULL;
    }
 
    return S_OK;
}

STDMETHODIMP 
HippoControl::isConnected(BOOL *isConnected)
{
    *isConnected = endpoint_ != 0;

    return S_OK;
}

STDMETHODIMP 
HippoControl::setListener(IDispatch *listener)
{
    listener_ = listener;

    return S_OK;
}

STDMETHODIMP 
HippoControl::setWindow(IDispatch *window)
{
    return S_OK;
}

STDMETHODIMP 
HippoControl::joinChatRoom(BSTR chatId, BOOL participant)
{
    hippoDebugLogW(L"joinChatRoom %ls %d", chatId, participant);

    if (!chatId || !hippoVerifyGuid(chatId))
        return E_INVALIDARG;

    HippoUStr chatIdU(chatId);

    if (controller_ && endpoint_)
        controller_->joinChatRoom(endpoint_, chatIdU.c_str(), participant ? true : false);

    return S_OK;
}

STDMETHODIMP 
HippoControl::leaveChatRoom(BSTR chatId)
{
    if (!chatId || !hippoVerifyGuid(chatId))
        return E_INVALIDARG;

    HippoUStr chatIdU(chatId);

    if (controller_ && endpoint_)
        controller_->leaveChatRoom(endpoint_, chatIdU.c_str());

    return S_OK;
}

STDMETHODIMP 
HippoControl::showChatWindow(BSTR chatId)
{
    if (!chatId || !hippoVerifyGuid(chatId))
        return E_INVALIDARG;

    HippoUStr chatIdU(chatId);

    if (controller_ && endpoint_)
        controller_->showChatWindow(chatIdU.c_str());

    return S_OK;
}
   
STDMETHODIMP 
HippoControl::sendChatMessage(BSTR chatId, BSTR text)
{
    return sendChatMessageSentiment(chatId, text, 0); // 0 == INDIFFERENT
}

STDMETHODIMP 
HippoControl::sendChatMessageSentiment(BSTR chatId, BSTR text, int sentiment)
{
    if (!chatId || !hippoVerifyGuid(chatId))
        return E_INVALIDARG;
    if (!text)
        return E_INVALIDARG;
    if (sentiment < 0 || sentiment > 2)
        return E_INVALIDARG;

    HippoUStr chatIdU(chatId);
    HippoUStr textU(text);

    if (controller_ && endpoint_)
        controller_->sendChatMessage(chatIdU.c_str(), textU.c_str(), sentiment);

    return S_OK;
}

STDMETHODIMP 
HippoControl::OpenBrowserBar()
{
    // Checking that we are connected here isn't a good idea, since that would mean
    // that the browser bar doesn't show up if the XMPP connection is missing. Instead
    // we check that we are getting called from *.mugshot.org or *.dumbhippo.com.
    // Note that HippoExplorerBar will only *work* for those domains anyways, because
    // of how it works via observing the browser URL.

    if (!siteIsTrusted())
        return E_ACCESSDENIED;

    return showHideBrowserBar(true);
}

STDMETHODIMP 
HippoControl::CloseBrowserBar()
{
    return showHideBrowserBar(true);
}

/////////////////////////////////////////////////////////////////////

void
HippoControl::onConnect()
{
    hippoDebugLogW(L"HippoControl::onConnect");

    if (endpoint_ == 0) {
        hippoDebugLogW(L"Registering endpoint (%p)", controller_);
        endpoint_ = controller_->registerEndpoint(this);
        if (endpoint_ != 0)
            hippoDebugLogW(L"HippoControl: Succesfully registered endpoint onConnect");
        if (endpoint_ && listener_) {
            HippoInvocation invocation(listener_, L"onConnect");
            invocation.run();
        }
    }
}

void 
HippoControl::onDisconnect()
{
    hippoDebugLogW(L"HippoControl::onDisconnect");

    if (endpoint_ != 0) {
        endpoint_ = 0;

        if (listener_) {
            HippoInvocation invocation(listener_, L"onDisconnect");
            invocation.run();
        }
    }
}

void 
HippoControl::onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant)
{
    hippoDebugLogU("HippoControl::onUserJoin(%s,%s,%d)", chatId, userId, participant);

    if (!listener_)
        return;

    HippoInvocation invocation(listener_, L"onUserJoin");

    invocation.add(HippoBSTR::fromUTF8(chatId, -1));
    invocation.add(HippoBSTR::fromUTF8(userId, -1));
    invocation.addBool(participant);

    invocation.run();
}

void 
HippoControl::onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId)
{
    hippoDebugLogU("HippoControl::onUserLeave(%s,%s,%d)", chatId, userId);

    if (!listener_)
        return;

    HippoInvocation invocation(listener_, L"onUserLeave");

    invocation.add(HippoBSTR::fromUTF8(chatId, -1));
    invocation.add(HippoBSTR::fromUTF8(userId, -1));

    invocation.run();
}

void 
HippoControl::onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment, double timestamp, long serial)
{
    hippoDebugLogU("HippoControl::onMessage(%s,%s,%s,%d,%f,%ld)", chatId, userId, message, sentiment, timestamp, serial);

    if (!listener_)
        return;

    HippoInvocation invocation(listener_, L"onMessage");

    invocation.add(HippoBSTR::fromUTF8(chatId, -1));
    invocation.add(HippoBSTR::fromUTF8(userId, -1));
    invocation.add(HippoBSTR::fromUTF8(message, -1));
    invocation.addDouble(timestamp);
    invocation.addLong(serial);
    invocation.addLong(sentiment);

    HRESULT hr = invocation.run();
    if (FAILED(hr))
        hippoDebugLogW(L"Invoking onMessage failed %x", hr);
}

void 
HippoControl::userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying)
{
    hippoDebugLogU("HippoControl::userInfo(%s,%s,%s,%s,%s,%d)", userId, name, smallPhotoUrl, currentSong, currentArtist, musicPlaying);

    if (!listener_)
        return;

    HippoInvocation invocation(listener_, L"userInfo");

    invocation.add(HippoBSTR::fromUTF8(userId, -1));
    invocation.add(HippoBSTR::fromUTF8(name, -1));
    invocation.add(HippoBSTR::fromUTF8(smallPhotoUrl, -1));
    invocation.add(HippoBSTR::fromUTF8(currentArtist, -1));
    invocation.add(HippoBSTR::fromUTF8(currentSong, -1));
    invocation.addBool(musicPlaying);

    invocation.run();
}

/////////////////////////////////////////////////////////////////////

void
HippoControl::clearSite()
{
    stop();
    site_ = NULL;
}

bool
HippoControl::siteIsTrusted()
{
    HippoBSTR url;
    HippoPtr<IWebBrowser2> browser;
    if (getBrowser(&browser))
        browser->get_LocationURL(&url);

    return url && checkURL(url);
}

bool
HippoControl::checkURL(BSTR url)
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
HippoControl::showHideBrowserBar(bool doShow) 
{
    HippoPtr<IWebBrowser2> toplevelBrowser;
    if (!getToplevelBrowser(&toplevelBrowser))
        return E_FAIL;
    return HippoControl::showHideBrowserBarInternal(toplevelBrowser, doShow);
}

HRESULT 
HippoControl::showHideBrowserBarInternal(HippoPtr<IWebBrowser2> &toplevelBrowser, bool doShow)
{
    variant_t classId(L"{174D2323-9AF2-4257-B8BD-849865E4F1AE}"); // CLSID_HippoExplorerBar
    variant_t show(doShow);
    variant_t size;

    // We want to change our browser bar for this window, but we don't want the change
    // to stick in the registry, so we save the value of the relevant registry key
    // and restore it afterwards.

    bool haveIE6Value;
    BYTE *oldRegistryData = NULL;
    DWORD oldRegistryLength = 0;
    bool haveIE7Value;
    BYTE *oldRegistryData7 = NULL;
    DWORD oldRegistryLength7 = 0;

    {
        HippoRegKey key(HKEY_CURRENT_USER, L"Software\\Microsoft\\Internet Explorer\\Toolbar\\WebBrowser", false);
        haveIE6Value = key.loadBinary(L"ITBarLayout", &oldRegistryData, &oldRegistryLength);
        haveIE7Value = key.loadBinary(L"ITBar7Layout", &oldRegistryData7, &oldRegistryLength7);
    }

    HRESULT hr = toplevelBrowser->ShowBrowserBar(&classId, &show, &size);

    {
        HippoRegKey key(HKEY_CURRENT_USER, L"Software\\Microsoft\\Internet Explorer\\Toolbar\\WebBrowser", true);
        if (haveIE6Value)
            key.saveBinary(L"ITBarLayout", oldRegistryData, oldRegistryLength);
        if (haveIE7Value)
            key.saveBinary(L"ITBar7Layout", oldRegistryData7, oldRegistryLength7);
    }

    return hr;
}


bool
HippoControl::getBrowser(IWebBrowser2 **browser)
{
    if (!site_)
        return false;

    HRESULT hr = site_->QueryService<IWebBrowser2>(SID_SWebBrowserApp, browser);
    return SUCCEEDED(hr);
}

bool
HippoControl::getToplevelBrowser(IWebBrowser2 **browser)
{
    if (!site_)
        return false;

    HippoPtr<IServiceProvider> toplevel;
    site_->QueryService<IServiceProvider>(SID_STopLevelBrowser, &toplevel);
    if (!toplevel)
        return false;
    
    HRESULT hr = toplevel->QueryService<IWebBrowser2>(SID_SWebBrowserApp, browser);
    return SUCCEEDED(hr);
}
