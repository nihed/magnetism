/* HippoTracker.cpp: Browser helper object to track user visited URL
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"
#include "HippoTracker.h"
#include "Guid.h"
#include "Globals.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

// Window class for our notification window
static const TCHAR *CLASS_NAME = TEXT("HippoTrackerClass");

HippoTracker::HippoTracker(void)
{
    refCount_ = 1;
    dllRefCount++;

    hippoLoadRegTypeInfo(LIBID_SHDocVw, 1, 1,
                         &DIID_DWebBrowserEvents2, &eventsTypeInfo_, 
                         NULL);

    registered_ = false;
    dogfoodRegistered_ = false;
    debugRegistered_ = false;

    uiStartedMessage_ = RegisterWindowMessage(TEXT("HippoUIStarted"));

    createWindow();
    onUIStarted();
}

HippoTracker::~HippoTracker(void)
{
    DestroyWindow(window_);

    clearUI();

    // In case setSite(NULL) wasn't called
    clearSite();
        
    dllRefCount--;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoTracker::QueryInterface(const IID &ifaceID, 
                             void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IObjectWithSite *>(this));
    else if (IsEqualIID(ifaceID, IID_IObjectWithSite)) 
        *result = static_cast<IObjectWithSite *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, DIID_DWebBrowserEvents2))
        *result = static_cast<DWebBrowserEvents2 *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoTracker)

/////////////////// IObjectWithSite implementation ///////////////////

STDMETHODIMP 
HippoTracker::SetSite(IUnknown *site)
{
    clearSite();
    
    if (site) 
    {
        if (FAILED(site->QueryInterface<IWebBrowser2>(&site_)))
            return E_FAIL;
        
        /* We'd like to call registerBrowser() here, but it turns out IE gets
         * extremely unhappy if we make a possibly reentrant call out at this
         * point, so we wait until we get DocumentComplete()
         */
        HippoQIPtr<IConnectionPointContainer> container(site);
        if (container)
        {
            if (SUCCEEDED(container->FindConnectionPoint(DIID_DWebBrowserEvents2,
                                                         &connectionPoint_))) 
            {
                // The COM-safe downcast here is a little overkill ... 
                // we actually just need to disambiguate
                HippoQIPtr<IUnknown> unknown(static_cast<DWebBrowserEvents2 *>(this));
                connectionPoint_->Advise(unknown, &connectionCookie_);
            }
        }
    }
    
    return S_OK;
}

STDMETHODIMP 
HippoTracker::GetSite(const IID &iid, 
                      void     **result)
{
    if (!site_) {
        *result = NULL;
        return E_FAIL;
    }

    return site_->QueryInterface(iid, result);
}

//////////////////////// IDispatch implementation ///////////////////

STDMETHODIMP
HippoTracker::GetIDsOfNames (const IID   &iid,
                             OLECHAR    **names,  
                             unsigned int cNames,          
                             LCID         lcid,                   
                             DISPID *     dispID)
{
    return DispGetIDsOfNames(eventsTypeInfo_, names, cNames, dispID);
}

STDMETHODIMP
HippoTracker::GetTypeInfo (unsigned int infoIndex,  
                           LCID         lcid,                  
                           ITypeInfo  **ppTInfo)
{
   if (ppTInfo == NULL)
      return E_INVALIDARG;

   *ppTInfo = NULL;

   if (infoIndex != 0)
      return DISP_E_BADINDEX;

   eventsTypeInfo_->AddRef();
   *ppTInfo = eventsTypeInfo_;

   return S_OK;
}

 STDMETHODIMP 
 HippoTracker::GetTypeInfoCount (unsigned int *pcTInfo)
 {
    if (pcTInfo == NULL)
      return E_INVALIDARG;

    *pcTInfo = 1;

    return S_OK;
 }
  
 STDMETHODIMP
 HippoTracker::Invoke (DISPID        member,
                       const IID    &iid,
                       LCID          lcid,              
                       WORD          flags,
                       DISPPARAMS   *dispParams,
                       VARIANT      *result,
                       EXCEPINFO    *excepInfo,  
                       unsigned int *argErr)
 {
      /* There are various events that occur as the user navigates to a
       * new page. While DocumentComplete is most authoritative, it
       * can take a very long time to occur - it won't happen until
       * all images in the page have been loaded, and so forth. On
       * the other hand, BeginNavigate2 is too early; the navigation
       * could be blocked, handled in a new window, and so forth.
       * What we do is look for DocumentComplete, but also check for
       * a change (for title *and* URL) when we get a TitleChange 
       * notification. The assumption is that the title will change
       * briefly even if we are navigating to a page with the same
       * title. If it doesn't, then we'll eventually get DocumentComplete
       * anyways.
       */
      switch (member) {
        case DISPID_DOCUMENTCOMPLETE:
             if (dispParams->cArgs == 2 &&
                 dispParams->rgvarg[1].vt == VT_DISPATCH &&
                 dispParams->rgvarg[0].vt == (VT_BYREF | VT_VARIANT))
             {
                 registerBrowser();
                 updateBrowser();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
        case DISPID_TITLECHANGE:
             if (dispParams->cArgs == 1 &&
                 dispParams->rgvarg[0].vt == VT_BSTR)
             {
                 registerBrowser();
                 updateBrowser();

                 return S_OK;
             } else {
                 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
             }
             break;
         default:
             return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
}

/////////////////////////////////////////////////////////////////////

void
HippoTracker::registerBrowser()
{
    if (!registered_ && ui_ && site_) {
        registered_ = true;
        HRESULT hr = ui_->RegisterBrowser(site_, &registerCookie_); // may reenter
        if (FAILED (hr))
            registered_ = false;
        else {
            lastUrl_ = NULL;
            lastName_ = NULL;
        }
    }


    if (!dogfoodRegistered_ && dogfoodUi_ && site_) {
        dogfoodRegistered_ = true;
        HRESULT hr = dogfoodUi_->RegisterBrowser(site_, &dogfoodRegisterCookie_); // may reenter
        if (FAILED (hr))
            dogfoodRegistered_ = false;
        else {
            lastUrl_ = NULL;
            lastName_ = NULL;
        }
    }

    if (!debugRegistered_ && debugUi_ && site_) {
        debugRegistered_ = true;
        HRESULT hr = debugUi_->RegisterBrowser(site_, &debugRegisterCookie_); // may reenter
        if (FAILED (hr))
            debugRegistered_ = false;
        else {
            lastUrl_ = NULL;
            lastName_ = NULL;
        }
    }
}

void
HippoTracker::unregisterBrowser()
{
    if (registered_) {
        registered_ = false;
        ui_->UnregisterBrowser(registerCookie_); // May recurse
    }

    if (dogfoodRegistered_) {
        dogfoodRegistered_ = false;
        dogfoodUi_->UnregisterBrowser(dogfoodRegisterCookie_); // May recurse
    }

    if (debugRegistered_) {
        debugRegistered_ = false;
        debugUi_->UnregisterBrowser(debugRegisterCookie_); // May recurse
    }
}

void
HippoTracker::updateBrowser()
{
    HippoBSTR url;
    HippoBSTR name;

    if (site_ &&
        SUCCEEDED(site_->get_LocationURL(&url)) &&
        SUCCEEDED(site_->get_LocationName(&name)) &&
        url && ((WCHAR *)url)[0] && name && ((WCHAR *)name)[0]) 
    {
        if (lastUrl_ && lastName_ &&
            wcscmp(lastUrl_, url) == 0 &&
            wcscmp(lastName_, name) == 0)
            return;

        lastUrl_ = url;
        lastName_ = name;

        if (registered_)
            ui_->UpdateBrowser(registerCookie_, url, name);
        if (dogfoodRegistered_)
            dogfoodUi_->UpdateBrowser(dogfoodRegisterCookie_, url, name);
        if (debugRegistered_)
            debugUi_->UpdateBrowser(debugRegisterCookie_, url, name);
    }
}

void
HippoTracker::clearSite()
{
    unregisterBrowser();

    if (site_)
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
HippoTracker::clearUI()
{
    unregisterBrowser();
 
    if (ui_)
        ui_ = NULL;
    if (dogfoodUi_)
        dogfoodUi_ = NULL;
    if (debugUi_)
        debugUi_ = NULL;

}

bool 
HippoTracker::registerWindowClass()
{
    WNDCLASS windowClass;

    if (GetClassInfo(dllInstance, CLASS_NAME, &windowClass))
        return true;  // Already registered

    windowClass.style = 0;
    windowClass.lpfnWndProc = windowProc;
    windowClass.cbClsExtra = 0;
    windowClass.cbWndExtra = 0;
    windowClass.hInstance = dllInstance;
    windowClass.hIcon = NULL;
    windowClass.hCursor = NULL;
    windowClass.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    windowClass.lpszMenuName = NULL;
    windowClass.lpszClassName = CLASS_NAME;    

    return RegisterClass(&windowClass) != 0;
}

bool
HippoTracker::createWindow()
{
    if (!registerWindowClass())
        return false;

    window_ = CreateWindow(CLASS_NAME, 
                           NULL, // No title
                           0,    // Window style doesn't matter
                           0, 0, 10, 10,
                           NULL, // No parent
                           NULL, // No menu
                           dllInstance,
                           NULL); // lpParam
    if (!window_)
        return false;

    hippoSetWindowData<HippoTracker>(window_, this);

    return true;
}

void
HippoTracker::onUIStarted(void)
{
    clearUI();

    HippoPtr<IUnknown> unknown;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&ui_);

    unknown = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI_Dogfood, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&dogfoodUi_);

    unknown = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI_Debug, NULL, &unknown)))
        unknown->QueryInterface<IHippoUI>(&debugUi_);

    registerBrowser();
    updateBrowser();
}

LRESULT CALLBACK 
HippoTracker::windowProc(HWND   window,
                         UINT   message,
                         WPARAM wParam,
                         LPARAM lParam)
{
    HippoTracker *tracker = hippoGetWindowData<HippoTracker>(window);
    if (tracker) {
        if (message == tracker->uiStartedMessage_) {
            tracker->onUIStarted();
            return 0;
        }
    }

    return DefWindowProc(window, message, wParam, lParam);
}
