/* HippoTracker.cpp: Browser helper object to track URL's user visits
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 */
#include "stdafx.h"
#include "HippoTracker.h"
#include "Guid.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>

static const CLSID CLSID_HippoUI = {
    0xfd2d3bee, 0x477e, 0x4625, 0xb3, 0x5f, 0xbf, 0x49, 0x7f, 0xf6, 0x1, 0xd9
};

static void logit(const char *fmt, ...)
{
    va_list vap;
    va_start (vap, fmt);
    FILE *file = fopen("C:/Documents and Settings/Owen/Desktop/log.txt", "a");
    if (file) {
	vfprintf (file, fmt, vap);
	fclose(file);
    }
    va_end (vap);
}

CHippoTracker::CHippoTracker(void)
{
    ITypeLib *pTypeLib;
    m_pSite = NULL;
    
    logit("New\n");

    m_ObjRefCount = 1;
    g_DllRefCount++;
 
    m_pTInfo = NULL;
    if (SUCCEEDED (LoadRegTypeLib(LIBID_SHDocVw, 1, 1, 0, &pTypeLib))) {
	pTypeLib->GetTypeInfoOfGuid(DIID_DWebBrowserEvents2, &m_pTInfo);
	if (m_pTInfo) {
	    BSTR name;
	    UINT count;
	    if (SUCCEEDED (m_pTInfo->GetNames(0x66, &name, 1, &count))) {
		logit ("%ls\n", name);
	    }
	}
	pTypeLib->Release();
    }

    IUnknown *pUnk;
    m_pHippoUI = NULL;
    if (SUCCEEDED (GetActiveObject(CLSID_HippoUI, NULL, &pUnk))) {
	pUnk->QueryInterface(IID_IHippoUI, (LPVOID *)&m_pHippoUI);
	pUnk->Release();
    }
}

CHippoTracker::~CHippoTracker(void)
{
    logit("Delete\n");

    // This should have been freed in a call to SetSite(NULL), but 
    // it is defined here to be safe.
    if(m_pSite)
    {
	if (m_connectionPoint) {
	    if (m_connectionCookie) {
		logit("unadvise");
		m_connectionPoint->Unadvise(m_connectionCookie);
	    }
	    m_connectionPoint->Release();
	    m_connectionPoint = NULL;
	}

        m_pSite->Release();
        m_pSite = NULL;
    }

    if (m_pTInfo) {
	m_pTInfo->Release();
	m_pTInfo = NULL;
    }

    if (m_pHippoUI) {
	m_pHippoUI->Release();
	m_pHippoUI = NULL;
    }
    
    g_DllRefCount--;
}

/* IUnknown Implementation */

STDMETHODIMP 
CHippoTracker::QueryInterface(REFIID riid, LPVOID *ppReturn)
{
    *ppReturn = NULL;

    //IUnknown
    if(IsEqualIID(riid, IID_IUnknown))
    {
        *ppReturn = this;
    }
    
    //IObjectWithSite
    else if(IsEqualIID(riid, IID_IObjectWithSite))
    {
        *ppReturn = (IObjectWithSite*)this;
    }   

    //IDispatch
    else if(IsEqualIID(riid, IID_IDispatch))
    {
        *ppReturn = (IDispatch*)this;
    }   
    
    //DWebBrowserEvents2
    else if(IsEqualIID(riid, DIID_DWebBrowserEvents2))
    {
        *ppReturn = (DWebBrowserEvents2*)this;
    }   

    if(*ppReturn)
    {
        (*(LPUNKNOWN*)ppReturn)->AddRef();
        return S_OK;
    }
    
    return E_NOINTERFACE;
}                                             

STDMETHODIMP_(DWORD) 
CHippoTracker::AddRef()
{
    return ++m_ObjRefCount;
}


STDMETHODIMP_(DWORD) 
CHippoTracker::Release()
{
    if(--m_ObjRefCount == 0)
    {
        delete this;
        return 0;
    }
   
    return m_ObjRefCount;
}

/* IObjectWithSite implementations */

STDMETHODIMP CHippoTracker::SetSite(IUnknown* punkSite)
{
    if (punkSite)
	logit("SetSite - with site (%d)", m_ObjRefCount);
    else
	logit("SetSite - without site (%d)", m_ObjRefCount);

    // If a site is being held, release it.
    if(m_pSite)
    {
	if (m_connectionPoint) {
	    if (m_connectionCookie)
		m_connectionPoint->Unadvise(m_connectionCookie);
	    m_connectionPoint->Release();
	    m_connectionPoint = NULL;
	}

        m_pSite->Release();
        m_pSite = NULL;
    }
    
    // If punkSite is not NULL, a new site is being set.
    if(punkSite)
    {
	IConnectionPointContainer *pContainer;

	//Get and keep the IInputObjectSite pointer.
        if (SUCCEEDED(punkSite->QueryInterface(IID_IConnectionPointContainer,
                                               (LPVOID*)&pContainer)))
        {
	    logit("Found connectionPointContainer");
	    if (SUCCEEDED(pContainer->FindConnectionPoint(DIID_DWebBrowserEvents2,
		                                          &m_connectionPoint))) 
	    {
		logit("Found connectionPoint");
		IUnknown *pUnk;
		this->QueryInterface(IID_IUnknown, (LPVOID *)&pUnk);
		HRESULT hr = m_connectionPoint->Advise(pUnk, &m_connectionCookie);
		if (SUCCEEDED (hr)) {
		    logit("Advised");
		} else {
		    logit("Advice failed: %#x", hr);
		}
		pUnk->Release();
	    }
	    else
	    {
		m_connectionPoint = NULL;
	    }

	    pContainer->Release();
        }
	else
	{
	    logit("No connectionPoint");
	}

        //Get and keep the IInputObjectSite pointer.
        if(SUCCEEDED(punkSite->QueryInterface(IID_IWebBrowser2,
                                              (LPVOID*)&m_pSite)))
        {
	    logit("(%d)\n", m_ObjRefCount);
            return S_OK;
        }
       
        return E_FAIL;
    }

    logit("(%d)\n", m_ObjRefCount);
    
    return S_OK;
}

STDMETHODIMP CHippoTracker::GetSite(REFIID riid, LPVOID *ppvReturn)
{
    *ppvReturn = NULL;
    
    if(m_pSite)
        return m_pSite->QueryInterface(riid, ppvReturn);
    
    return E_FAIL;
}

//IDispatch methods
STDMETHODIMP
CHippoTracker::GetIDsOfNames (REFIID  riid,                  
    		              OLECHAR FAR* FAR*  rgszNames,  
			      unsigned int  cNames,          
			      LCID   lcid,                   
			      DISPID FAR*  rgDispId)
{
    return DispGetIDsOfNames(m_pTInfo, rgszNames, cNames, rgDispId);
}

STDMETHODIMP
CHippoTracker::GetTypeInfo (unsigned int  iTInfo,         
			    LCID  lcid,                   
			    ITypeInfo FAR* FAR*  ppTInfo)
{
   logit("Invoke");
   if (ppTInfo == NULL)
      return E_INVALIDARG;
   *ppTInfo = NULL;

   if(iTInfo != 0)
      return DISP_E_BADINDEX;

   m_pTInfo->AddRef();
   *ppTInfo = m_pTInfo;

   return S_OK;
}

 STDMETHODIMP 
 CHippoTracker::GetTypeInfoCount (unsigned int FAR*  pctinfo)
 {
    if (pctinfo == NULL)
      return E_INVALIDARG;

   *pctinfo = 1;
   return S_OK;

 }
  
 STDMETHODIMP
 CHippoTracker::Invoke (DISPID  dispIdMember,      
		        REFIID  riid,              
		        LCID  lcid,                
		        WORD  wFlags,              
		        DISPPARAMS FAR*  pDispParams,  
		        VARIANT FAR*  pVarResult,  
		        EXCEPINFO FAR*  pExcepInfo,  
		        unsigned int FAR*  puArgErr)
 {
     if (m_pTInfo) {
	 BSTR name;
	 UINT count;
	 if (SUCCEEDED (m_pTInfo->GetNames(dispIdMember, &name, 1, &count)))
	     logit ("%ls\n", name);
     }
 
     switch (dispIdMember) {
	case DISPID_DOCUMENTCOMPLETE:
	     if (pDispParams->cArgs == 2 &&
		 pDispParams->rgvarg[1].vt == VT_DISPATCH &&
		 pDispParams->rgvarg[0].vt == VT_BYREF | VT_VARIANT) {
		  if (pDispParams->rgvarg[0].pvarVal->vt == VT_BSTR)
		     logit("Got DocumentComplete: %ls\n", pDispParams->rgvarg[0].pvarVal->bstrVal);
		  if (m_pHippoUI) 
		      m_pHippoUI->Log(pDispParams->rgvarg[0].pvarVal->bstrVal);
		  return S_OK;
	     } else {
		 return DISP_E_BADVARTYPE; // Or DISP_E_BADPARAMCOUNT
	     }
	     break;
	 default:
	     return DISP_E_MEMBERNOTFOUND; // Or S_OK
     }
 }