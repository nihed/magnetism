/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <glib.h>
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoBubble.h"
#include "HippoIcon.h"
#include "HippoLogWindow.h"
#include "HippoPreferences.h"
#include "HippoUpgrader.h"
#include "HippoFlickr.h"
#include "HippoIM.h"
#include "HippoExternalBrowser.h"
#include "HippoRemoteWindow.h"

struct HippoBrowserInfo
{
    HippoPtr<IWebBrowser2> browser;
    HippoBSTR url;
    HippoBSTR title;
    DWORD cookie;
};

struct HippoLinkShare
{
    HippoBSTR postId;
    HippoBSTR senderId;
    HippoBSTR senderName;
    HippoBSTR url;
    HippoBSTR title;
    HippoBSTR description;
    HippoArray<HippoBSTR> personRecipients;
    HippoArray<HippoBSTR> groupRecipients;
    HippoArray<HippoBSTR> viewers;
    HippoBSTR info;
};

class HippoUI 
: public IHippoUI 
{
public:
    HippoUI(bool debug, bool replaceExisting, bool initialDebugShare);
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
    STDMETHODIMP Quit();
    STDMETHODIMP ShowRecent();
    STDMETHODIMP BeginFlickrShare(BSTR filePath);
    STDMETHODIMP ShareLink(BSTR url, BSTR title);

    bool create(HINSTANCE instance);
    void destroy();

    HippoPreferences *getPreferences();

    void showMenu(UINT buttonFlag);
    HippoExternalBrowser *launchBrowser(BSTR url);
    void displaySharedLink(BSTR postId);

    void debugLogW(const WCHAR *format, ...); // UTF-16
    void debugLogU(const char *format, ...);  // UTF-8
    void logError(const WCHAR *text, HRESULT result);
    void logLastError(const WCHAR *text);

    void onConnectionChange(bool connected);
    void onAuthFailure();
    void onAuthSuccess();
    void setClientInfo(const char *minVersion,
                       const char *currentVersion,
                       const char *downloadUrl);
    void onUpgradeReady();
    void onLinkMessage(HippoLinkShare &link);

    HRESULT getRemoteURL(BSTR appletName, BSTR *result);
    HRESULT getAppletURL(BSTR appletName, BSTR *result);

    void showAppletWindow(BSTR url, HippoPtr<IWebBrowser2> &webBrowser);

    void registerWindowMsgHook(HWND window, HippoMessageHook *hook);
    void unregisterWindowMsgHook(HWND window);
    HWND getWindow() { return window_; }

private:
    bool registerActive();
    bool registerClass();
    bool createWindow();
    void updateMenu();
    void updateIcons();

    void showSignInWindow();
    void showPreferences();
    void updateForgetPassword();

    // Register an "internal" browser instance that we don't want
    // to allow sharing of, and that we quit when the HippoUI
    // instance exits
    void addInternalBrowser(HippoExternalBrowser *browser, bool closeOnQuit);

    static int checkIdle(gpointer data);

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    void revokeActive();

    void registerStartup();
    void unregisterStartup();

    static int doQuit(gpointer data);

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
    // If true, then on startup if another instance is already running,
    // tell it to exit rather than erroring out.
    bool replaceExisting_;
    bool initialShowDebugShare_;
    bool connected_;
    // Whether we are registered as the active HippoUI object
    bool registered_; 

    DWORD refCount_;
    HINSTANCE instance_;
    HWND window_;
    HICON bigIcon_;
    HICON smallIcon_;
    HMENU menu_;
    HMENU debugMenu_;
    HWND preferencesDialog_;

    HippoBSTR currentURL_;

    HippoBubble bubble_;
    HippoPreferences preferences_;
    HippoLogWindow logWindow_;
    HippoIcon notificationIcon_;
    HippoIM im_;
    HippoFlickr *flickr_;
    HippoUpgrader upgrader_;

    HippoRemoteWindow *currentShare_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject

    HippoArray<HippoPtr<HippoExternalBrowser> > internalBrowsers_;
    HippoArray<HippoBrowserInfo> browsers_;
    DWORD nextBrowserCookie_;

    bool rememberPassword_;
    bool passwordRemembered_;

    bool idle_;
    unsigned checkIdleTimeoutId_;
};
