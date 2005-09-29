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

// We redefine this here to avoid a dependency on the HippoUI project
static const CLSID CLSID_HippoUI = {
    0xfd2d3bee, 0x477e, 0x4625, 0xb3, 0x5f, 0xbf, 0x49, 0x7f, 0xf6, 0x1, 0xd9
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

    uiStartedMessage_ = RegisterWindowMessage(TEXT("HippoUIStarted"));

    createWindow();
    onUIStarted();
}

HippoTracker::~HippoTracker(void)
{
    DestroyWindow(window_);

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
		 dispParams->rgvarg[0].vt == VT_BYREF | VT_VARIANT) {
		  if (dispParams->rgvarg[0].pvarVal->vt == VT_BSTR && ui_)
		      ui_->Log(dispParams->rgvarg[0].pvarVal->bstrVal);
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
HippoTracker::clearSite()
{
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
    HippoPtr<IUnknown> unknown;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI, NULL, &unknown)))
	unknown->QueryInterface<IHippoUI>(&ui_);
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
