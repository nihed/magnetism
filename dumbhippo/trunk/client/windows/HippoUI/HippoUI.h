/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include "HippoIcon.h"

class HippoUI 
    : public IHippoUI 
{
public:
    HippoUI();
    ~HippoUI();

    //IUnknown methods
    STDMETHODIMP QueryInterface(REFIID, LPVOID*);
    STDMETHODIMP_(DWORD) AddRef();
    STDMETHODIMP_(DWORD) Release();

    //IDispatch methods
    STDMETHODIMP GetTypeInfoCount(UINT *);
    STDMETHODIMP GetTypeInfo(UINT, LCID, ITypeInfo **);
    STDMETHODIMP GetIDsOfNames(REFIID, LPOLESTR *, UINT, LCID, DISPID *);
    STDMETHODIMP Invoke(DISPID, REFIID, LCID, WORD, DISPPARAMS *, VARIANT *, EXCEPINFO *, UINT *);

    //IHippoUI methods
    STDMETHODIMP Log(BSTR message);

    bool create(HINSTANCE instance);
    void destroy();

    void showURL(BSTR url);
    void showShareWindow(BSTR url);

private:
    bool registerActive();
    bool registerClass();
    bool createWindow();

    bool processMessage(UINT   message,
	                WPARAM wParam,
			LPARAM lParam);

    void revokeActive();

    HRESULT getAppletURL(BSTR filename, BSTR *url);

    static LRESULT CALLBACK windowProc(HWND   window,
    		                       UINT   message,
		                       WPARAM wParam,
		                       LPARAM lParam);

private:
    DWORD refCount_;
    HINSTANCE instance_;
    HWND window_;
    HICON bigIcon_;
    HICON smallIcon_;

    HippoBSTR currentURL_;

    HippoIcon notificationIcon_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject
};