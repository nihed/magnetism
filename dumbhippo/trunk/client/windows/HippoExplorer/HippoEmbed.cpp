/* HippoEmbed.cpp: Browser helper object to track user visited URL
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include "HippoEmbed.h"
#include "HippoExplorer_h.h"
#include "HippoDispID.h"
#include "Guid.h"
#include "Globals.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

HippoEmbed::HippoEmbed(void)
{
    refCount_ = 1;
    safetyOptions_ = 0;
    dllRefCount++;
 
    connectionPointContainer_.setWrapper(static_cast<IObjectWithSite *>(this));

    // This could fail with out-of-memory, but there's nothing we can do in
    // the constructor. We'd need to rework ClassFactory to handle this
    connectionPointContainer_.addConnectionPoint(IID_IHippoEmbedEvents);

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoExplorer, 
				0, 1, /* Version */
				0,    /* LCID */
				&typeLib);
    if (SUCCEEDED (hr)) {
	typeLib->GetTypeInfoOfGuid(IID_IHippoEmbed, &ifaceTypeInfo_);
	typeLib->GetTypeInfoOfGuid(CLSID_HippoEmbed, &classTypeInfo_);
    } else
	hippoDebug(L"Failed to load type lib: %x\n", hr);
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
			   LCID	     lcid,
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
	      dispParams->rgvarg[0].vt == VT_BYREF | VT_VARIANT) 
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
HippoEmbed::DisplayMessage (BSTR message)
{
    MessageBox(NULL, message, NULL, MB_OK);

    return S_OK;
}

static void
formatHTML(MSHTML::IHTMLElement *element,
	   WCHAR *format, 
	   ...)
{
    WCHAR buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), format, vap);
    va_end (vap);

    element->put_innerHTML(buf);
}

STDMETHODIMP 
HippoEmbed::DebugDump (IDispatch *dispatch)
{
    if (!dispatch)
	return E_FAIL;
    HippoPtr<MSHTML::IHTMLElement> element;
    HRESULT hr = dispatch->QueryInterface<MSHTML::IHTMLElement>(&element);
    if (!SUCCEEDED(hr))
	return hr;
 
    HippoBSTR browserURL, toplevelURL;
    if (browser_)
	browser_->get_LocationURL(&browserURL);
    if (toplevelBrowser_)
	toplevelBrowser_->get_LocationURL(&toplevelURL);

    formatHTML(element, L"Toplevel URL: %ls<br>Browser URL: %ls\n",
	toplevelURL ? toplevelURL : L"<null>", 
	browserURL ? browserURL : L"<null>");

    return S_OK;
}

STDMETHODIMP
HippoEmbed::CloseWindow()
{
    if (browser_)
	browser_->Quit();

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
//	HippoQIPtr<IHippoEmbedEvents> events(data.pUnk);
//	if (events)
//	    events->LocationChanged(url);
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