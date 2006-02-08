/* HippoMenu.h: popup menu for the notification icon
 *
 * Copyright Red Hat, Inc. 2006
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

class HippoMenu :
    public IHippoMenu,
    public IDispatch,
    public HippoAbstractWindow
{
public:
    HippoMenu();
    ~HippoMenu();

    void popup(int mouseX, int mouseY);

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

    // IHippoMenu
    STDMETHODIMP Exit();
    STDMETHODIMP GetServerBaseUrl(BSTR *result);
    STDMETHODIMP Hush();

protected:
    virtual HippoBSTR getURL();
    virtual void initializeWindow();
    virtual void initializeIE();

    virtual bool processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam);

    virtual void onClose(bool fromScript);

private:
    HippoPtr<ITypeInfo> ifaceTypeInfo_;
    DWORD refCount_;

    int mouseX_;
    int mouseY_;

    void moveResizeWindow(void);
};
