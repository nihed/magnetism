/* HippoChatControl.h: ActiveX control for jabber chatting about a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#pragma once

#include <shlobj.h>
#include <HippoUtil.h>
#include <HippoConnectionPointContainer.h>
#include <objsafe.h>
#include "HippoExplorer_h.h"

class HippoChatControl :
    public IObjectWithSite,
    public IObjectSafety,
    public IProvideClassInfo,
    public IPersistPropertyBag,
    public IHippoChatRoom
{
public:
    HippoChatControl(void);
    ~HippoChatControl(void);

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

   // IPersist
   STDMETHODIMP GetClassID(CLSID *classID);

   // IPersistPropertyBag methods
   STDMETHODIMP InitNew();
   STDMETHODIMP Load(IPropertyBag *propertyBag,
                     IErrorLog    *errorLog);
   STDMETHODIMP Save(IPropertyBag *propertyBag,
                     BOOL          clearDirty,
                     BOOL          saveAllProperties);

    // IDispatch methods
   STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
   STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);                   
   STDMETHODIMP GetTypeInfoCount (unsigned int *);
   STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                        VARIANT *, EXCEPINFO *, unsigned int *);

   // IHippoChatRoom methods
   STDMETHODIMP Join(BOOL participant);
   STDMETHODIMP Leave(BOOL participant);
   STDMETHODIMP SendMessage(BSTR text);
   STDMETHODIMP Rescan();

private:
    void clearSite();
    
    /**
     * Check that we can get the web browser and it is pointing to a location 
     * that we consider safe to use our control.
     *
     * @return true if the web browser has been set for our control and
     *   points to a safe URL.
     */
    bool isSiteSafe();

    // Helper for isSiteSafe();
    bool checkURL(BSTR url);

    void connectToUI();
    void clearUI();

    HippoBSTR chatId_;
    HippoBSTR userId_;

    HippoPtr<IHippoUI> ui_;
    HippoPtr<IHippoChatRoom> chatRoom_;
    HippoPtr<IConnectionPoint> chatRoomConnection_; // connection point for IHippoChatRoomEvents
    DWORD chatRoomCookie_;                          // cookie for IHippoChatRoomEvents

    HippoConnectionPointContainer connectionPointContainer_;

    int memberCount_;
    int participantCount_;

    DWORD refCount_;
    HippoPtr<IServiceProvider> site_;
    HippoPtr<IWebBrowser2> browser_;
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    HippoPtr<ITypeInfo> classTypeInfo_;

    DWORD safetyOptions_;
};
