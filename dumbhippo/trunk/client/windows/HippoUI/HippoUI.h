/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoIcon.h"
#include "HippoLogWindow.h"
#include "HippoPreferences.h"
#include "HippoIM.h"

struct HippoBrowserInfo
{
    HippoPtr<IWebBrowser2> browser;
    HippoBSTR url;
    HippoBSTR title;
    DWORD cookie;
};

class HippoUI 
    : public IHippoUI 
{
public:
    HippoUI(bool debug);
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
    STDMETHODIMP RegisterBrowser(IWebBrowser2 *, DWORD *);
    STDMETHODIMP UnregisterBrowser(DWORD);
    STDMETHODIMP UpdateBrowser(DWORD, BSTR, BSTR);

    bool create(HINSTANCE instance);
    void destroy();

    HippoPreferences *getPreferences();

    void showMenu(UINT buttonFlag);
    void showURL(BSTR url);
    void showShareWindow(BSTR url);

    void debugLogW(const WCHAR *format, ...); // UTF-16
    void debugLogU(const char *format, ...);  // UTF-8

	void onConnectionChange(bool connected);
    void onAuthFailure();
    void onAuthSuccess();
    void onLinkMessage(const WCHAR *senderName,
					const WCHAR *url,
	               const WCHAR *title,
		       const WCHAR *description);

private:
    bool registerActive();
    bool registerClass();
    bool createWindow();
    void updateMenu();
	void updateIcons();

    void showSignInWindow();
    void showPreferences();
    void updateForgetPassword();

    bool processMessage(UINT   message,
	                WPARAM wParam,
			LPARAM lParam);

    void revokeActive();

    HRESULT getAppletURL(BSTR appletName, BSTR *result);
    void showAppletWindow(BSTR url);

    static LRESULT CALLBACK windowProc(HWND   window,
    		                       UINT   message,
		                       WPARAM wParam,
		                       LPARAM lParam);
    static INT_PTR CALLBACK loginProc(HWND   window,
    		                      UINT   message,
		                      WPARAM wParam,
		                      LPARAM lParam);
    static INT_PTR CALLBACK preferencesProc(HWND   window,
    		                            UINT   message,
		                            WPARAM wParam,
		                            LPARAM lParam);


private:
    // If true, this is a debug instance, acts as a separate global
    // singleton and has a separate registry namespace
    bool debug_;
	bool connected_;

    DWORD refCount_;
    HINSTANCE instance_;
    HWND window_;
    HICON bigIcon_;
    HICON smallIcon_;
    HMENU menu_;
    HWND preferencesDialog_;

    HippoBSTR currentURL_;

    HippoPreferences preferences_;
    HippoLogWindow logWindow_;
    HippoIcon notificationIcon_;
    HippoIM im_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject

    HippoArray<HippoBrowserInfo> browsers_;
    DWORD nextBrowserCookie_;

    bool rememberPassword_;
    bool passwordRemembered_;
};