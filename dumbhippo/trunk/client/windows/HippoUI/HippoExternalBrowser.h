/* HippoExternalBrowser.h: Launch a normal web browser and gain limited control over it.
 * Right now this is an out of process IE instance, but in theory we could launch
 * Firefox or something too if it's installed.
 * Contrast with HippoIE/HippoIEWindow which are in-process, guaranteed to be IE, 
 * don't look like normal web browsers, and don't implement security.
 *
 * Copyright Red Hat, Inc. 2005
 */

#pragma once

#include "stdafx.h"
#include <mshtml.h>
#include <HippoUtil.h>

class HippoExternalBrowser;

/* Besides being a convenience wrapper around all the IDispatch/connection point/etc. insanity,
 * this class is intended to represent the subset of events which we should expect to get
 * from reasonable browsers.  No idea if e.g. Firefox on Windows actually lets you get this stuff
 * though.
 */
class HippoExternalBrowserEvents
{
public:
    virtual void onNavigate(HippoExternalBrowser *browser, BSTR url) {}
    virtual void onDocumentComplete(HippoExternalBrowser *browser) = 0;
    virtual void onQuit(HippoExternalBrowser *browser) = 0;
};

class HippoExternalBrowser :
        public DWebBrowserEvents2 // <- IDispatch
{
public:
    HippoExternalBrowser(const WCHAR *url, bool quitOnDelete, HippoExternalBrowserEvents *events);
    ~HippoExternalBrowser(void);

    void quit();
    void navigate(WCHAR * url);
    void injectBrowserBar();

   // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
    STDMETHOD (GetIDsOfNames) (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHOD (GetTypeInfo) (unsigned int, LCID, ITypeInfo **);                    
    STDMETHOD (GetTypeInfoCount) (unsigned int *);
    STDMETHOD (Invoke) (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                        VARIANT *, EXCEPINFO *, unsigned int *);

private:
    void disconnect();

    bool quitOnDelete_;
    HippoExternalBrowserEvents *events_;
    DWORD refCount_;

    // IE specific things
    HippoPtr<IWebBrowser2> ie_;
    HippoPtr<IConnectionPoint> connectionPoint_; // connection point for DWebBrowserEvents2
    DWORD connectionCookie_; // cookie for DWebBrowserEvents2 connection
    HippoPtr<ITypeInfo> eventsTypeInfo_;
};
