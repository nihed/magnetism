/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoIcon.h"
#include <loudmouth/loudmouth.h>

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
    STDMETHODIMP RegisterBrowser(IWebBrowser2 *, DWORD *);
    STDMETHODIMP UnregisterBrowser(DWORD);
    STDMETHODIMP UpdateBrowser(DWORD, BSTR, BSTR);

    bool create(HINSTANCE instance);
    void destroy();

    void showMenu(UINT buttonFlag);
    void showURL(BSTR url);
    void showShareWindow(BSTR url);

private:
    bool registerActive();
    bool registerClass();
    bool createWindow();
    void updateMenu();
    void onPasswordDialogLogin(const WCHAR *username,
	                       const WCHAR *password,
			       bool         rememberPassword);

    void showPreferences();
    void updateForgetPassword();

    bool processMessage(UINT   message,
	                WPARAM wParam,
			LPARAM lParam);

    void revokeActive();
    void loadUserInfo();
    void saveUserInfo();

    HRESULT getAppletURL(BSTR filename, BSTR *url);

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
    static void onConnectionOpen (LmConnection *connection,
				  gboolean      success,
				  gpointer      userData);
    static void onConnectionAuthenticate (LmConnection *connection,
				          gboolean      success,
				          gpointer      userData);

    static LmHandlerResult onMessage (LmMessageHandler *handler,
				      LmConnection     *connection,
				      LmMessage        *message,
				      gpointer          userData);


private:
    DWORD refCount_;
    HINSTANCE instance_;
    HWND window_;
    HICON bigIcon_;
    HICON smallIcon_;
    HMENU menu_;
    HWND preferencesDialog_;

    HippoBSTR currentURL_;

    HippoIcon notificationIcon_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject

    LmConnection *lmConnection_;

    HippoArray<HippoBrowserInfo> browsers_;
    DWORD nextBrowserCookie_;

    char *username_; // UTF-8
    char *password_; // UTF-8
    bool rememberPassword_;
    bool passwordRemembered_;
};