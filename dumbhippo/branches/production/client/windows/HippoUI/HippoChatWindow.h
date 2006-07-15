/* HippoChatWindow.h: Window displaying a ChatWindow for a post
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include "HippoAbstractWindow.h"

class HippoChatWindow : 
    public IHippoChatWindow,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoChatWindow();
    ~HippoChatWindow();

    void setChatId(BSTR chatId);
    BSTR getChatId();

    // IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    // IDispatch methods
    STDMETHODIMP GetIDsOfNames (const IID &, OLECHAR **, unsigned int, LCID, DISPID *);
    STDMETHODIMP GetTypeInfo (unsigned int, LCID, ITypeInfo **);           
    STDMETHODIMP GetTypeInfoCount (unsigned int *);
    STDMETHODIMP Invoke (DISPID, const IID &, LCID, WORD, DISPPARAMS *, 
                         VARIANT *, EXCEPINFO *, unsigned int *);

    // IHippoChatWindow methods
    STDMETHODIMP DemandAttention();

protected:
    void onClose(bool fromScript);

private:
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    DWORD refCount_;
    HippoBSTR chatId_;
};
