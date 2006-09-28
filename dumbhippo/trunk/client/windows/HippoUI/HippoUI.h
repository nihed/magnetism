/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <glib.h>
#include <hippo/hippo-common.h>
#include <HippoUtil.h>
#include <HippoArray.h>
#include <HippoMessageHook.h>
#include "HippoIcon.h"
#include "HippoListenerProxy.h"
#include "HippoLogWindow.h"
#include "HippoUpgrader.h"
#include "HippoExternalBrowser.h"
#include "HippoRemoteWindow.h"
#include "HippoMusic.h"
#include "HippoUIUtil.h"
#include "HippoGSignal.h"

#include <vector>

class HippoChatManager;
class HippoPreferences;

struct HippoBrowserInfo
{
    // circumvent constness
    IWebBrowser2* getBrowser() const {
        const IWebBrowser2 *b = browser;
        return const_cast<IWebBrowser2*>(b);
    }

    HippoPtr<IWebBrowser2> browser;
    HippoBSTR url;
    HippoBSTR title;
    DWORD cookie;
};

class HippoUI 
: public IHippoUI 
{
public:
    HippoUI(HippoInstanceType instanceType, bool replaceExisting, bool initialDebugShare);
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
    STDMETHODIMP Quit(DWORD *processId);
    STDMETHODIMP ShowRecent();
    STDMETHODIMP BeginFlickrShare(BSTR path);
    STDMETHODIMP ShareLink(BSTR url, BSTR title);
    STDMETHODIMP ShowChatWindow(BSTR postId);
    STDMETHODIMP GetLoginId(BSTR *result);
    STDMETHODIMP GetChatRoom(BSTR postId, IHippoChatRoom **result);
    STDMETHODIMP DoUpgrade();
    STDMETHODIMP ShareLinkComplete(BSTR postId, BSTR url);

    STDMETHODIMP RegisterListener(IHippoUIListener *listener, UINT64 *listenerId);
    STDMETHODIMP UnregisterListener(UINT64 listenerId);
    STDMETHODIMP RegisterEndpoint(UINT64 listenerId, UINT64 *endpointId);
    STDMETHODIMP UnregisterEndpoint(UINT64 endpointId);
    STDMETHODIMP JoinChatRoom(UINT64 endpointId, BSTR chatId, BOOL participant);
    STDMETHODIMP LeaveChatRoom(UINT64 endpointId, BSTR chatId);
    STDMETHODIMP SendChatMessage(BSTR chatId, BSTR text);
    STDMETHODIMP GetServerName(BSTR *serverName);
    STDMETHODIMP LaunchBrowser(BSTR url);

    bool create(HINSTANCE instance);
    void destroy();

    HippoPlatform *getPlatform();
    HippoPreferences *getPreferences();
    HippoDataCache *getDataCache();
    HippoConnection *getConnection();

    void showMenu(UINT buttonFlag);
    HippoExternalBrowser *launchBrowser(BSTR url);
    void displaySharedLink(BSTR postId, BSTR url);

    void ignorePost(BSTR postId);
    void ignoreEntity(BSTR entityId);
    void ignoreChat(BSTR chatId);
    void groupInvite(BSTR groupId, BSTR userId);

    void debugLogW(const WCHAR *format, ...); // UTF-16
    void debugLogU(const char *format, ...);  // UTF-8
    void logErrorU(const char *format, ...); // UTF-8
    void logHresult(const WCHAR *text, HRESULT result);
    void logLastHresult(const WCHAR *text);

    void onUpgradeReady();

    int getRecentMessageCount();

    bool isGroupChatActive(HippoEntity *entity);
    bool isShareActive(HippoPost *post);

    void getRemoteURL(BSTR appletName, BSTR *result) throw (std::bad_alloc, HResultException);
    void getAppletPath(BSTR filename, BSTR *result) throw (std::bad_alloc, HResultException);
    void getAppletURL(BSTR appletName, BSTR *result) throw (std::bad_alloc, HResultException);

    void showAppletWindow(BSTR url, HippoPtr<IWebBrowser2> &webBrowser);

    void getImagePath(BSTR filename, BSTR *result) throw (std::bad_alloc, HResultException);

    void registerMessageHook(HWND window, HippoMessageHook *hook);
    void unregisterMessageHook(HWND window);
    HWND getWindow() { return window_; }
    HICON getSmallIcon() { return smallIcon_; }
    HICON getBigIcon() { return bigIcon_; }

private:
    class HippoUIUpgradeWindowCallback : public HippoIEWindowCallback
    {
    public:
        HippoUIUpgradeWindowCallback(HippoUI *ui) { ui_ = ui; }
        virtual void onDocumentComplete();
    private:
        HippoUI *ui_;
    };

    HippoBSTR getBasePath() throw (std::bad_alloc, HResultException);

    bool registerActive();
    bool registerClass();
    bool createWindow();
    void updateMenu();
    void setIcons();
    void updateIcon();

    void showSignInWindow();
    void showPreferences();

    // Register an "internal" browser instance that we don't want
    // to allow sharing of, and that we quit when the HippoUI
    // instance exits
    void addInternalBrowser(HippoExternalBrowser *browser, bool closeOnQuit);

    bool crackUrl(BSTR url, URL_COMPONENTS *components);
    // Check if an URL points to our site (and not to /visit)
    bool isSiteURL(BSTR url);
    bool isFramedPost(BSTR url, BSTR postId);

    // Check if an URL points to /account, or another page that we
    // want to avoid framing
    bool isNoFrameURL(BSTR url);

    static int idleHotnessBlink(gpointer data);

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

    //// Idles and timeouts

    bool timeoutShowDebugShare();
    bool timeoutCheckIdle();

    //// Signal handlers 

    void onHotnessChanged(int oldHotness);
    void onConnectedChanged(gboolean connected);
    void onHasAuthChanged();
    void onAuthFailed();
    void onAuthSucceeded();

    HippoListenerProxy *findListenerById(UINT64 listenerId);
    HippoListenerProxy *findListenerByEndpoint(UINT64 endpointId);

private:
    // If true, this is a debug instance, acts as a separate global
    // singleton and has a separate registry namespace
    HippoInstanceType instanceType_;
    // If true, then on startup if another instance is already running,
    // tell it to exit rather than erroring out.
    bool replaceExisting_;
    bool initialShowDebugShare_;

    // Whether we are registered as the active HippoUI object
    bool registered_; 

    DWORD refCount_;
    HINSTANCE instance_;
    HWND window_;
    HICON bigIcon_;
    HICON smallIcon_;
    HICON trayIcon_;
    HippoBSTR tooltip_;
    HMENU oldMenu_;
    HMENU debugMenu_;
    HWND preferencesDialog_;

    HippoBSTR currentURL_;

    HippoGObjectPtr<HippoPlatform> platform_;
    HippoGObjectPtr<HippoDataCache> dataCache_;

    GConnection1<void,int> hotnessChanged_;
    GConnection1<void,gboolean> connectedChanged_;
    GConnection0<void> authFailed_;
    GConnection0<void> authSucceeded_;
    GConnection0<void> hasAuthChanged_;

    GTimeout showDebugShareTimeout_;
    GTimeout checkIdleTimeout_;

    HippoLogWindow logWindow_;
    HippoIcon notificationIcon_;
    HippoUpgrader upgrader_;
    HippoMusic music_;
    HippoChatManager *chatManager_;

    HippoRemoteWindow *currentShare_;
    HippoRemoteWindow *upgradeWindow_;
    HippoRemoteWindow *signinWindow_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject

    HippoUIUpgradeWindowCallback *upgradeWindowCallback_;

    HippoArray<HippoPtr<HippoExternalBrowser> > internalBrowsers_;
    HippoArray<HippoBrowserInfo> browsers_;
    std::vector<HippoPtr<HippoListenerProxy> > listeners_;

    DWORD nextBrowserCookie_;

    bool rememberPassword_;
    bool passwordRemembered_;

    int idleHotnessBlinkId_;
    int hotnessBlinkCount_;

    bool idle_; // is the user idle
    bool haveMissedBubbles_; // has the user not seen bubbles
    bool screenSaverRunning_; // is the screen saver running
};
