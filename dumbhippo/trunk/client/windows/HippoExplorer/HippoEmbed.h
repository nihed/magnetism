/* HippoEmbed.h: Browser helper object to track user visited URLs
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include <objsafe.h>
#include "HippoExplorer_h.h"

class HippoEmbed :
    public IObjectWithSite,
    public IObjectSafety,
    public IProvideClassInfo,
    public IHippoEmbed
{
public:
    HippoEmbed(void);
    ~HippoEmbed(void);

   // IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, LPVOID*);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   // IObjectWithSite methods
   STDMETHODIMP SetSite (IUnknown*);
   STDMETHODIMP GetSite (const IID &, void **);

   // IObjectSafety methods
   STDMETHODIMP GetInterfaceSafetyOptions (const IID &, DWORD *, DWORD *);
   STDMETHODIMP SetInterfaceSafetyOptions (const IID &, DWORD , DWORD);

   // IProvideClassInfo methods
   STDMETHODIMP GetClassInfo (ITypeInfo **);

    // IDispatch methods
   STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
   STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);                   
   STDMETHODIMP GetTypeInfoCount (unsigned int *);
   STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                        VARIANT *, EXCEPINFO *, unsigned int *);

   // IHippoEmbed methods
   STDMETHODIMP DisplayMessage (BSTR message);
   STDMETHODIMP DebugDump (IDispatch *element);
   STDMETHODIMP CloseWindow (void);

private:
    void clearSite();
    void onDocumentComplete(IDispatch *dispatch, BSTR url);

    HippoConnectionPointContainer connectionPointContainer_;

    DWORD refCount_;
    HippoPtr<IServiceProvider> site_;
    HippoPtr<IWebBrowser2> browser_;
    HippoPtr<IWebBrowser2> toplevelBrowser_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;

    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection

    DWORD safetyOptions_;
};
