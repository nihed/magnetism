/* HippoTracker.h: Browser helper object to track user visited URLs
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>
#include "HippoTrackerUpdater.h"

class HippoTracker : 
    public IObjectWithSite,
    public DWebBrowserEvents2
{
public:
    HippoTracker(void);
    ~HippoTracker(void);

   //IUnknown methods
   STDMETHODIMP QueryInterface(REFIID, LPVOID*);
   STDMETHODIMP_(DWORD) AddRef();
   STDMETHODIMP_(DWORD) Release();

   //IObjectWithSite methods
   STDMETHOD (SetSite) (IUnknown*);
   STDMETHOD (GetSite) (const IID &, void **);

    //IDispatch methods
   STDMETHOD (GetIDsOfNames) (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
   STDMETHOD (GetTypeInfo) (unsigned int, LCID, ITypeInfo **);                    
   STDMETHOD (GetTypeInfoCount) (unsigned int *);
   STDMETHOD (Invoke) (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                       VARIANT *, EXCEPINFO *, unsigned int *);

protected:
    DWORD refCount_;

private:
    void update();
    void createUpdater();
    void clearSite();
    void clearUpdater();

    HippoPtr<IWebBrowser2> site_;
    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection
    HippoPtr<ITypeInfo> eventsTypeInfo_;

    HippoTrackerUpdater *updater_;

    HippoBSTR lastUrl_;
    HippoBSTR lastName_;
};
