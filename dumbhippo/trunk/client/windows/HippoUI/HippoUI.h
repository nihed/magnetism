/* HippoUI.h: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#pragma once

#include <glib.h>
#include <HippoUtil.h>
#include <HippoArray.h>
#include "HippoBubble.h"
#include "HippoChatWindow.h"
#include "HippoIcon.h"
#include "HippoLogWindow.h"
#include "HippoDataCache.h"
#include "HippoMenu.h"
#include "HippoPreferences.h"
#include "HippoUpgrader.h"
#include "HippoFlickr.h"
#include "HippoIM.h"
#include "HippoExternalBrowser.h"
#include "HippoMenu.h"
#include "HippoRemoteWindow.h"
#include "HippoMusic.h"
#include "HippoMySpace.h"

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
    STDMETHODIMP Quit();
    STDMETHODIMP ShowMissed();
    STDMETHODIMP ShowRecent();
    STDMETHODIMP BeginFlickrShare(BSTR filePath);
    STDMETHODIMP ShareLink(BSTR url, BSTR title);
    STDMETHODIMP ShowChatWindow(BSTR postId);
    STDMETHODIMP GetLoginId(BSTR *result);
    STDMETHODIMP GetChatRoom(BSTR postId, IHippoChatRoom **result);

    bool create(HINSTANCE instance);
    void destroy();

    HippoPreferences *getPreferences();

    void showMenu(UINT buttonFlag);
    HippoExternalBrowser *launchBrowser(BSTR url);
    void displaySharedLink(BSTR postId, BSTR url);

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
    void setMySpaceName(const char *name);
    void getSeenMySpaceComments();
    void getMySpaceContacts();
    void onUpgradeReady();
    void addEntity(HippoEntity &entity);
    void onLinkMessage(HippoPost &link);

    void getEntity(BSTR id, HippoEntity *entity);

    void setHaveMissedBubbles(bool haveMissed);

    int getRecentMessageCount();

    bool isShareActive(BSTR postId);
    void onChatWindowClosed(HippoChatWindow *chatWindow);

    void getRemoteURL(BSTR appletName, BSTR *result) throw (std::bad_alloc, HResultException);
    void getAppletURL(BSTR appletName, BSTR *result) throw (std::bad_alloc, HResultException);

    void showAppletWindow(BSTR url, HippoPtr<IWebBrowser2> &webBrowser);

    void registerWindowMsgHook(HWND window, HippoMessageHook *hook);
    void unregisterWindowMsgHook(HWND window);
    HWND getWindow() { return window_; }
    HICON getSmallIcon() { return smallIcon_; }
    HICON getBigIcon() { return bigIcon_; }

    void onCurrentTrackChanged(bool haveTrack, const HippoTrackInfo & newTrack);

    void onNewMySpaceComment(long myId, long blogId, HippoMySpaceBlogComment &comment, bool doDisplay);
    void setSeenMySpaceComments(HippoArray<HippoMySpaceBlogComment*> *comments);
    void setMySpaceContacts(HippoArray<HippoMySpaceContact *> &contacts);
    void onCreatingMySpaceContactPost(HippoMySpaceContact *contact);
    void onReceivingMySpaceContactPost();

    void clearActivePosts();
    void addActivePost(const HippoPost &post);

    bool getPost(const HippoBSTR postId, HippoPost *post);
    void updatePost(const HippoPost &post);

    typedef enum {
        UNKNOWN,
        COLD,
        COOL,
        WARM,
        GETTING_HOT,
        HOT
    } Hotness;

    void setHotness(BSTR hotness);

    bool getNeedPrimingTracks();
    void providePrimingTracks(HippoPlaylist *playlist);
    void setMusicSharingEnabled(bool enabled) { 
        music_.setEnabled(enabled);
    }

private:
    bool registerActive();
    bool registerClass();
    bool createWindow();
    void updateMenu();
    void setIcons();
    void updateIcon();

    void showSignInWindow();
    void showPreferences();
    void updateForgetPassword();

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

    static int checkIdle(gpointer data);

    static int idleHotnessBlink(gpointer data);

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    void revokeActive();

    void registerStartup();
    void unregisterStartup();

    static int doQuit(gpointer data);
    static gboolean idleCreateMySpace(gpointer data);

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
    HippoInstanceType instanceType_;
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
    HICON trayIcon_;
    HippoBSTR tooltip_;
    HMENU oldMenu_;
    HMENU debugMenu_;
    HWND preferencesDialog_;

    HippoBSTR currentURL_;

    HippoBubble bubble_;
    HippoMenu menu_;
    HippoPreferences preferences_;
    HippoLogWindow logWindow_;
    HippoIcon notificationIcon_;
    HippoIM im_;
    HippoFlickr *flickr_;
    HippoUpgrader upgrader_;
    HippoMusic music_;
    HippoMySpace *mySpace_;
    HippoDataCache dataCache_;

    HippoRemoteWindow *currentShare_;
    HippoRemoteWindow *signinWindow_;

    HippoPtr<ITypeInfo> uiTypeInfo_;  // Type information blob for IHippoUI, used for IDispatch
    ULONG registerHandle_;            // Handle from RegisterActiveObject

    HippoArray<HippoPtr<HippoExternalBrowser> > internalBrowsers_;
    HippoArray<HippoBrowserInfo> browsers_;
    HippoArray<HippoChatWindow *> chatWindows_;

    DWORD nextBrowserCookie_;

    bool rememberPassword_;
    bool passwordRemembered_;

    int idleHotnessBlinkId_;
    int hotnessBlinkCount_;

    bool idle_; // is the user idle
    bool haveMissedBubbles_; // has the user not seen bubbles
    HippoUI::Hotness hotness_;
    bool screenSaverRunning_; // is the screen saver running
    unsigned checkIdleTimeoutId_;
};
