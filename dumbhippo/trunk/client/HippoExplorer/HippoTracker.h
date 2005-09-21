/* HippoTracker.h: Browser helper object to track URL's user visits
 *
 * Copyright Red Hat, Inc. 2005
 *
 * Partially based on MSDN BandObjs sample:
 *  Copyright 1997 Microsoft Corporation.  All Rights Reserved.
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>

#include "Globals.h"

class CHippoTracker :
    public IObjectWithSite,
    public DWebBrowserEvents2
{
protected:
   DWORD m_ObjRefCount;

public:
    CHippoTracker(void);
    ~CHippoTracker(void);

   //IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, LPVOID*);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   //IObjectWithSite methods
   STDMETHOD (SetSite) (IUnknown*);
   STDMETHOD (GetSite) (REFIID, LPVOID*);

    //IDispatch methods
   STDMETHOD (GetIDsOfNames) (REFIID  riid,                  
    		              OLECHAR FAR* FAR*  rgszNames,  
			      unsigned int  cNames,          
			      LCID   lcid,                   
			      DISPID FAR*  rgDispId);
   STDMETHOD (GetTypeInfo) (unsigned int  iTInfo,         
			    LCID  lcid,                   
			    ITypeInfo FAR* FAR*  ppTInfo);
   STDMETHOD (GetTypeInfoCount) (unsigned int FAR*  pctinfo);
   STDMETHOD (Invoke) (DISPID  dispIdMember,      
		       REFIID  riid,              
		       LCID  lcid,                
		       WORD  wFlags,              
		       DISPPARAMS FAR*  pDispParams,  
		       VARIANT FAR*  pVarResult,  
		       EXCEPINFO FAR*  pExcepInfo,  
		       unsigned int FAR*  puArgErr);

private:
    IWebBrowser2 *m_pSite;
    IConnectionPoint *m_connectionPoint; // connection point for DWebBrowserEvents2
    DWORD m_connectionCookie; // cookie for DWebBrowserEvents2 connection
    ITypeInfo *m_pTInfo;
    IHippoUI *m_pHippoUI;
};
