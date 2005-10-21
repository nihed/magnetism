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

// We redefine these here to avoid a dependency on the HippoUI project
static const CLSID CLSID_HippoUI = {
    0xfd2d3bee, 0x477e, 0x4625, 0xb3, 0x5f, 0xbf, 0x49, 0x7f, 0xf6, 0x1, 0xd9
};

static const CLSID CLSID_HippoUI_Debug = {
    0xee8e46eb, 0xcdc7, 0x4f89, 0xa8, 0xae, 0xaf, 0x9, 0x94, 0x6c, 0x96, 0x85
};

// Window class for our notification window
static const TCHAR *CLASS_NAME = TEXT("HippoTrackerClass");

HippoTracker::HippoTracker(void)
{
    refCount_ = 1;
    dllRefCount++;

    HippoPtr<ITypeLib> typeLib;
    if (SUCCEEDED (LoadRegTypeLib(LIBID_SHDocVw, 1, 1, 0, &typeLib)))
	typeLib->GetTypeInfoOfGuid(DIID_DWebBrowserEvents2, &eventsTypeInfo_);

    registered_ = false;
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
      switch (member) {
	case DISPID_DOCUMENTCOMPLETE:
	     if (dispParams->cArgs == 2 &&
		 dispParams->rgvarg[1].vt == VT_DISPATCH &&
		 dispParams->rgvarg[0].vt == VT_BYREF | VT_VARIANT) 
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
    }


    if (!debugRegistered_ && debugUi_ && site_) {
        debugRegistered_ = true;
	HRESULT hr = debugUi_->RegisterBrowser(site_, &debugRegisterCookie_); // may reenter
	if (FAILED (hr))
	    debugRegistered_ = false;
    }
}

void
HippoTracker::unregisterBrowser()
{
    if (registered_) {
	registered_ = false;
	ui_->UnregisterBrowser(registerCookie_); // May recurse
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
	if (registered_)
    	    ui_->UpdateBrowser(registerCookie_, url, name);
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
