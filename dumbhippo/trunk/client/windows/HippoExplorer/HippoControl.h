/* HippoControl.h: ActiveX control to extend the capabilities of our web pages
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include <objsafe.h>
#include <dispex.h>
#include <hippo-ipc.h>
#include "HippoExplorer_h.h"

class HippoControl :
    public IObjectWithSite,
    public IObjectSafety,
    public IProvideClassInfo,
    public IDispatchEx,
    public IHippoControl,
    public HippoIpcListener
{
public:
    HippoControl(void);
    ~HippoControl(void);

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

   // IDispatchEx methods
   STDMETHODIMP GetDispID (BSTR, DWORD, DISPID *);
   STDMETHODIMP InvokeEx (DISPID, LCID, WORD, DISPPARAMS *,
                          VARIANT *, EXCEPINFO *, IServiceProvider *);
   STDMETHODIMP DeleteMemberByName (BSTR, DWORD);
   STDMETHODIMP DeleteMemberByDispID (DISPID);
   STDMETHODIMP GetMemberProperties (DISPID, DWORD, DWORD *);
   STDMETHODIMP GetMemberName (DISPID, BSTR *);
   STDMETHODIMP GetNextDispID (DWORD, DISPID, DISPID *);
   STDMETHODIMP GetNameSpaceParent (IUnknown **);

   // IHippoControl methods
   // Standard HippoControl interface shared with firefox
   STDMETHODIMP start(BSTR serverUrl);
   STDMETHODIMP stop();
       
   STDMETHODIMP isConnected(BOOL *isConnected);

   STDMETHODIMP setListener(IDispatch *listener); 

   STDMETHODIMP joinChatRoom(BSTR chatId, BOOL participant);
   STDMETHODIMP leaveChatRoom(BSTR chatId);
   STDMETHODIMP showChatWindow(BSTR chatId);
   STDMETHODIMP sendChatMessage(BSTR chatId, BSTR text);
   STDMETHODIMP sendChatMessageSentiment(BSTR chatId, BSTR text, int sentiment);

   STDMETHODIMP OpenBrowserBar();
   STDMETHODIMP CloseBrowserBar();

   // HippoIpcListener methods
   virtual void onConnect();
   virtual void onDisconnect();

   virtual void onUserJoin(HippoEndpointId endpoint, const char *chatId, const char *userId, bool participant);
   virtual void onUserLeave(HippoEndpointId endpoint, const char *chatId, const char *userId);
   virtual void onMessage(HippoEndpointId endpoint, const char *chatId, const char *userId, const char *message, int sentiment, double timestamp, long serial);

   virtual void userInfo(HippoEndpointId endpoint, const char *userId, const char *name, const char *smallPhotoUrl, const char *currentSong, const char *currentArtist, bool musicPlaying);

   static HRESULT showHideBrowserBarInternal(HippoPtr<IWebBrowser2> &browser, bool doShow);

private:
    void clearSite();
    bool siteIsTrusted();
    bool checkURL(BSTR url);
    bool getBrowser(IWebBrowser2 **browser);
    bool getToplevelBrowser(IWebBrowser2 **toplevelBrowser);
    HRESULT showHideBrowserBar(bool doShow);

    HippoConnectionPointContainer connectionPointContainer_;

    DWORD refCount_;
    HippoPtr<IServiceProvider> site_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;

    DWORD safetyOptions_;

    HippoPtr<IDispatch> listener_;

    HippoIpcController *controller_;
    HippoEndpointId endpoint_;
};
