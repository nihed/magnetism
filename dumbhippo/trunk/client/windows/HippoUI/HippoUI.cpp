/* HippoUI.cpp: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx-hippoui.h"
#include "HippoUI.h"
#include <stdio.h>
#include <process.h>
#include <strsafe.h>
#include <exdisp.h>
#include <Windows.h>
#include <HippoUtil.h>
#include <HippoRegistrar.h>
#include <HippoURLParser.h>
#include <HippoUtil_i.c>
#include <Winsock2.h>
#include <urlmon.h>   // For CoInternetParseUrl
#include <wininet.h>  // for cookie retrieval
#include <limits>
#include "Resource.h"
#include "HippoChatManager.h"
#include "HippoCrash.h"
#include "HippoHTTP.h"
#include "HippoToolbarEdit.h"
#include "HippoRegKey.h"
#include "HippoRemoteWindow.h"
#include "HippoPreferences.h"
#include "HippoPlatformImpl.h"
#include "HippoUIUtil.h"
#include "HippoImageFactory.h"
#include <hippo/hippo-stack-manager.h>
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-widgets.h>
#include <hippo/hippo-window.h>
#include <hippo/hippo-canvas-test.h>

#include <glib.h>

// GUID definition
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include "Guid.h"
#pragma data_seg()

// Generated code for HippoUI.idl
#include "HippoUI_i.c"

static const WCHAR *HIPPO_CLIENT_SUBKEY = HIPPO_REGISTRY_KEY L"\\Client";

static const int MAX_LOADSTRING = 100;
static const TCHAR *CLASS_NAME = TEXT("HippoUIClass");

// If this long elapses since the last activity, count the user as idle (in ms)
static const int USER_IDLE_TIME = 30 * 1000;

// How often to check if the user is idle (in ms)
static const int CHECK_IDLE_TIME = 5 * 1000;

// Time between icon blinks (in ms)
static const int HOTNESS_BLINK_TIME = 150;

static void quitAndWait(IHippoUI *ui);

HippoUI::HippoUI(HippoInstanceType instanceType, bool replaceExisting, bool initialDebugShare) 
{
    HippoConnection *connection;

    refCount_ = 1;
    instanceType_ = instanceType;
    replaceExisting_ = replaceExisting;
    initialShowDebugShare_ = initialDebugShare;

    hippoLoadTypeInfo(L"HippoUtil.dll",
                      &IID_IHippoUI, &uiTypeInfo_,
                      NULL);

    platform_ = hippo_platform_impl_new(instanceType);
    g_object_unref(G_OBJECT(platform_)); // remove extra ref

    connection = hippo_connection_new(platform_);
    dataCache_ = hippo_data_cache_new(connection);

    g_object_unref(connection); // owned now by data cache
    g_object_unref(G_OBJECT(dataCache_)); // remove extra ref

    // Set up connections
    hotnessChanged_.connect(dataCache_, "hotness-changed", slot(this, &HippoUI::onHotnessChanged));
    connectedChanged_.connect(G_OBJECT(connection), "connected-changed", slot(this, &HippoUI::onConnectedChanged));
    hasAuthChanged_.connect(G_OBJECT(connection), "has-auth-changed", slot(this, &HippoUI::onHasAuthChanged));
    authFailed_.connect(G_OBJECT(connection), "auth-failed", slot(this, &HippoUI::onAuthFailed));
    authSucceeded_.connect(G_OBJECT(connection), "auth-succeeded", slot(this, &HippoUI::onAuthSucceeded));

    hippo_platform_impl_set_ui(HIPPO_PLATFORM_IMPL(platform_), this);
    notificationIcon_.setUI(this);
    upgrader_.setUI(this);
    music_.setUI(this);
    hippo_image_factory_set_ui(this);

    hotnessBlinkCount_ = 0;
    idleHotnessBlinkId_ = 0;

    chatManager_ = NULL;
    preferencesDialog_ = NULL;

    registered_ = false;

    nextBrowserCookie_ = 0;

    rememberPassword_ = FALSE;
    passwordRemembered_ = FALSE;

    smallIcon_ = NULL;
    bigIcon_ = NULL;
    trayIcon_ = NULL;
    tooltip_ = L"Initializing General Purpose Architecture";  // thanks jboss

    idle_ = FALSE;
    haveMissedBubbles_ = FALSE;
    screenSaverRunning_ = FALSE;

    currentShare_ = NULL;
    upgradeWindowCallback_ = NULL;
    upgradeWindow_ = NULL;
    signinWindow_ = NULL;
}

HippoUI::~HippoUI()
{
    if (chatManager_)
        delete chatManager_;

    DestroyIcon(smallIcon_);
    DestroyIcon(bigIcon_);
    DestroyIcon(trayIcon_);
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoUI::QueryInterface(const IID &ifaceID, 
                        void     **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(this);
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoUI)) 
        *result = static_cast<IHippoUI *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}

HIPPO_DEFINE_REFCOUNTING(HippoUI)

////////////////////////// IDispatch implementation ///////////////////////

// We just delegate IDispatch to the standard Typelib-based version.


STDMETHODIMP
HippoUI::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoUI::GetTypeInfo(UINT        iTInfo,
                     LCID        lcid,
                     ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!uiTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    uiTypeInfo_->AddRef();
    *ppTInfo = uiTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoUI::GetIDsOfNames (REFIID    riid,
                        LPOLESTR *rgszNames,
                        UINT      cNames,
                        LCID      lcid,
                        DISPID   *rgDispId)
{
    if (!uiTypeInfo_) 
        return E_OUTOFMEMORY;
    
    return  DispGetIDsOfNames(uiTypeInfo_, rgszNames, cNames, rgDispId);
}
        
STDMETHODIMP 
HippoUI::Invoke (DISPID      dispIdMember,
                 REFIID      riid,
                 LCID        lcid,
                 WORD        wFlags,
                 DISPPARAMS *pDispParams,
                 VARIANT    *pVarResult,
                 EXCEPINFO  *pExcepInfo,
                 UINT       *puArgErr)
{
    if (!uiTypeInfo_) 
        return E_OUTOFMEMORY;

    HippoQIPtr<IHippoUI> hippoUI(this);
    return DispInvoke(hippoUI, uiTypeInfo_, dispIdMember, wFlags, 
                      pDispParams, pVarResult, pExcepInfo, puArgErr);
}

//////////////////////// IHippoTracker implementation //////////////////////

STDMETHODIMP 
HippoUI::RegisterBrowser(IWebBrowser2 *browser,
                         DWORD        *cookie)
{
    HippoBrowserInfo info;

    info.browser = browser;
    *cookie = info.cookie = ++nextBrowserCookie_;

    browsers_.append(info);

    return S_OK;
}

STDMETHODIMP 
HippoUI::UnregisterBrowser(DWORD cookie)
{
    for (ULONG i = 0; i < browsers_.length(); i++) {
        if (browsers_[i].cookie == cookie) {
            browsers_.remove(i);
            return S_OK;
        }
    }

    return E_FAIL;
}

STDMETHODIMP 
HippoUI::UpdateBrowser(DWORD cookie, BSTR url, BSTR title)
{
    for (ULONG i = 0; i < browsers_.length(); i++) {
        if (browsers_[i].cookie == cookie) {
            browsers_[i].url = url;
            browsers_[i].title = title;

            // mySpace_.browserChanged(browsers_[i]);

            return S_OK;
        }
    }

    return E_FAIL;
}

int
HippoUI::doQuit(gpointer data) 
{
    HippoUI *ui = (HippoUI *)data;
    DestroyWindow(ui->window_);

    return FALSE;
}

STDMETHODIMP
HippoUI::Quit(DWORD *processId)
{
    if (processId)
        *processId = GetCurrentProcessId();

    // We need to unregister ourself as the active HippoUI implementation before
    // we return, but we need to return to the caller, not just exit immediately.

    revokeActive();
    g_idle_add(doQuit, this);

    return S_OK;
}


////////////////////////////////////////////////////////////////////////////

//HICON
//HippoUI::loadWithOverlay(int width, int height, TCHAR *icon)
//{
//    HICON icon;
//    HDC memcontext = CreateCompatibleDC(NULL);
//    ICONINFO iconInfo;
//    HBITMAP bitmap = CreateCompatibleBitmap(memcontext, width, height);
//    icon = (HICON)LoadImage(instance_, icon,
//                            IMAGE_ICON, width, height, LR_DEFAULTCOLOR);
//    GetIconInfo(icon, &iconInfo);
//    DrawIcon(memcontext, 0, 0, icon);
//
//    DestroyIcon(icon);
//    if (haveMissedBubbles_) {
//        // Draw a white rectangle
//        TRIVERTEX bounds[2]
//        GRADIENT_RECT rect;
//        bounds[0].x = width * .75;
//        bounds[0].y = height * .75;
//        bounds[0].Red = 0x0000;
//        bounds[0].Green = 0x0000;
//        bounds[0].Blue = 0x0000;
//        bounds[0].Alpha = 0x0000;
//        bounds[1].x = width;
//        bounds[1].y = height; 
//        bounds[1].Red = 0xFF00;
//        bounds[1].Green = 0xFF00;
//        bounds[1].Blue = 0xFF00;
//        bounds[1].Alpha = 0x0000;
//        rect.UpperLeft = 0;
//        rect.LowerRight = 1;
//        GradientFill(memcontext, bounds, 2, &rect, 1, GRADIENT_FILL_RECT_H);
//    }
//
//}

void
HippoUI::setIcons(void)
{
    TCHAR *icon;

    // Load the standard icons if not loaded before
    if (smallIcon_ == NULL) {
        icon = MAKEINTRESOURCE(IDI_MUGSHOT);
        smallIcon_ = (HICON)LoadImage(instance_, icon,
                                      IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
        bigIcon_ = (HICON)LoadImage(instance_, icon,
                                    IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
    }

    // And always load the notification icon
    if (!hippo_connection_get_connected(getConnection())) {
        icon = MAKEINTRESOURCE(IDI_NOTIFICATION_DISCONNECTED);
    } else if (hotnessBlinkCount_ % 2 == 1) {
        // Currently unused
        icon = MAKEINTRESOURCE(IDI_DUMBHIPPO_BLANK); // need blank/outlined icon?
    } else {
        icon = MAKEINTRESOURCE(IDI_NOTIFICATION);
    }
    if (trayIcon_ != NULL)
        DestroyIcon(trayIcon_);

    trayIcon_ = (HICON)LoadImage(instance_, icon,
                                 IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);

    tooltip_ = HippoBSTR::fromUTF8(hippo_connection_get_tooltip(getConnection()));
}

void
HippoUI::onConnectedChanged(gboolean connected)
{
    if (connected) {
        const HippoClientInfo *info;
        info = hippo_data_cache_get_client_info(getDataCache());
        upgrader_.setUpgradeInfo(info->minimum, info->current, info->download);
    }
    updateIcon();
}

void
HippoUI::updateIcon()
{
    setIcons();
    notificationIcon_.updateIcon(trayIcon_);
    notificationIcon_.updateTip(tooltip_.m_str);
}

// takes int and not HippoHotness to avoid any possible 
// issues with signal marshaling through GObject ... 
// (the callback connected to GObject is generated with the
// arg type in the GConnection)
void
HippoUI::onHotnessChanged(int oldHotnessInt)
{
    HippoHotness oldHotness = (HippoHotness) oldHotnessInt;
    HippoHotness hotness;

    hotness = hippo_data_cache_get_hotness(dataCache_);

    if (hotness == oldHotness)
        return; /* should never happen though */

    if (idleHotnessBlinkId_ > 0)
        g_source_remove(idleHotnessBlinkId_);
    idleHotnessBlinkId_ = 0;
    hotnessBlinkCount_ = 0;
    
    debugLogU("hotness changing from %d to %d", oldHotness, hotness);

    if (oldHotness != HIPPO_HOTNESS_UNKNOWN) {
        idleHotnessBlinkId_ = g_idle_add(HippoUI::idleHotnessBlink, this);
    } else {
        // If we're transitioning from UNKNOWN, don't do blink
        updateIcon();
    }
}

int
HippoUI::idleHotnessBlink(gpointer data)
{
    HippoUI *ui = (HippoUI*) data;

    ui->debugLogU("doing blink, count=%d", ui->hotnessBlinkCount_);
    if (ui->hotnessBlinkCount_ > 4) {
        ui->hotnessBlinkCount_ = 0;
        ui->idleHotnessBlinkId_ = 0;
        return FALSE;
    }
    ui->updateIcon();
    ui->hotnessBlinkCount_++;
    ui->idleHotnessBlinkId_ = g_timeout_add(HOTNESS_BLINK_TIME, HippoUI::idleHotnessBlink, ui);
    
    return FALSE;
}

static void
testStatusCallback(HINTERNET ictx, DWORD_PTR uctx, DWORD status, LPVOID statusInfo, 
                  DWORD statusLength)
{
    HippoUI *ui = (HippoUI*) uctx;
}

bool
HippoUI::timeoutShowDebugShare()
{
    hippo_data_cache_add_debug_data(dataCache_);

    return false; // remove idle
}

void 
HippoUI::groupInvite(BSTR groupId, BSTR userId)
{
    HippoUStr groupIdU(groupId);
    HippoUStr userIdU(userId);
    hippo_connection_do_invite_to_group(getConnection(), groupIdU.c_str(), userIdU.c_str());
}

bool
HippoUI::create(HINSTANCE instance)
{
    instance_ = instance;
   
    setIcons();
    oldMenu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_NOTIFY));
    debugMenu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_DEBUG));

    if (!registerClass()) {
        hippoDebugLastErr(L"Failed to register class %s", CLASS_NAME);
        return false;
    }

    if (!registerActive())
        return false;

    if (!createWindow()) {
        g_debug("Failed to createWindow()");
        revokeActive();
        return false;
    }

    notificationIcon_.setIcon(trayIcon_);
    if (!notificationIcon_.create(window_)) {
        g_debug("Failed to create notification icon");
        revokeActive();
        return false;
    }
    notificationIcon_.updateTip(tooltip_.m_str);

    logWindow_.setBigIcon(bigIcon_);
    logWindow_.setSmallIcon(smallIcon_);
    if (!logWindow_.create()) {
        g_debug("Failed to create log window");
        revokeActive();
        notificationIcon_.destroy();
        return false;
    }

#if 1
    if (hippo_platform_get_signin(platform_)) {
        if (hippo_connection_signin(getConnection()))
            showSignInWindow();
    }
#endif

    checkIdleTimeout_.add(CHECK_IDLE_TIME, slot(this, &HippoUI::timeoutCheckIdle));

    registerStartup();

#if 1
    // and very last once we're all ready, fire up the stacker
    stack_ = hippo_stack_manager_new(hippo_data_cache_get_model(dataCache_), platform_);
#endif

#if 0
    HippoWindow *window = hippo_platform_create_window(HIPPO_STACKER_PLATFORM(platform_));
    hippo_window_set_resizable(window, HIPPO_ORIENTATION_HORIZONTAL, TRUE);
    hippo_window_set_resizable(window, HIPPO_ORIENTATION_VERTICAL, TRUE);
    HippoCanvasItem *root = hippo_canvas_test_get_root();
    hippo_window_set_contents(window, root);
    hippo_window_set_visible(window, TRUE);
#endif

    if (initialShowDebugShare_) {
        showDebugShareTimeout_.add(3000, slot(this, &HippoUI::timeoutShowDebugShare));
    }

    return true;
}

void
HippoUI::destroy()
{
    hippo_stack_manager_free(stack_);

    if (currentShare_) {
        delete currentShare_;
        currentShare_ = NULL;
    }
    
    if (signinWindow_) {
        delete signinWindow_;
        signinWindow_ = NULL;
    }

    notificationIcon_.destroy();
    
    revokeActive();
}

HippoPlatform *
HippoUI::getPlatform()
{
    return platform_;
}

HippoConnection*
HippoUI::getConnection()
{
    return hippo_data_cache_get_connection(dataCache_);
}

HippoDataCache*
HippoUI::getDataCache()
{
    return dataCache_;
}

HippoPreferences*
HippoUI::getPreferences()
{
    return hippo_platform_impl_get_preferences(HIPPO_PLATFORM_IMPL(platform_));
}

HRESULT
HippoUI::ShowRecent()
{
    hippo_stack_manager_show_browser(stack_, FALSE);    

    return S_OK;
}

HRESULT
HippoUI::BeginFlickrShare(BSTR path)
{
    // FIXME this is not used, but it's in the COM IDL so maybe
    // it's bad to just delete

    return S_OK;
}

// Show a window offering to share the given URL
HRESULT
HippoUI::ShareLink(BSTR url, BSTR title, IHippoToolbarAction *action)
{
    if (currentShare_)
        delete currentShare_;
    currentShare_ = new HippoRemoteWindow(this, L"Share Link", NULL);
    currentShare_->showShare(url, title);
    currentShare_->setForegroundWindow();
    currentShareAction_ = action;

    return S_OK;
}

HRESULT
HippoUI::ShowChatWindow(BSTR chatId)
{
    if (!chatManager_)
        chatManager_ = HippoChatManager::createInstance(this);

    chatManager_->showChatWindow(chatId);

    return S_OK;
}
    
HippoWindowState
HippoUI::getChatWindowState(BSTR chatId)
{
    if (!chatManager_)
        return HIPPO_WINDOW_STATE_CLOSED;

    return chatManager_->getChatWindowState(chatId);
}

STDMETHODIMP
HippoUI::GetLoginId(BSTR *ret)
{
    const char *selfGuidU = hippo_connection_get_self_guid(getConnection());
    if (selfGuidU) {
        HippoBSTR::fromUTF8(selfGuidU, -1).CopyTo(ret);
    } else {
        // not logged in right now
        *ret = NULL;
    }
    return S_OK;
}

STDMETHODIMP
HippoUI::DoUpgrade()
{
    upgrader_.performUpgrade();
    return S_OK;
}

STDMETHODIMP
HippoUI::ShareLinkComplete(BSTR postId, BSTR url)
{
    if (currentShareAction_ == NULL)
        return S_OK;
    HippoBSTR visitUrl;
    getRemoteURL(HippoBSTR(L"visit?post="), &visitUrl);
    visitUrl.Append(postId);
    VARIANT missing;
    missing.vt = VT_EMPTY;

    HRESULT hr = currentShareAction_->Navigate(visitUrl);
    return S_OK;
}

HippoListenerProxy *
HippoUI::findListenerById(UINT64 listenerId)
{
    for (std::vector<HippoPtr<HippoListenerProxy> >::iterator i = listeners_.begin();
         i != listeners_.end();
         i++) 
    {
        HippoListenerProxy *proxy = *i;

        if (proxy->getId() == listenerId)
            return proxy;
    }

    return NULL;
}

HippoListenerProxy *
HippoUI::findListenerByEndpoint(UINT64 endpointId)
{
    for (std::vector<HippoPtr<HippoListenerProxy> >::iterator i = listeners_.begin();
         i != listeners_.end();
         i++) 
    {
        HippoListenerProxy *proxy = *i;

        if (proxy->hasEndpoint(endpointId))
            return proxy;
    }

    return NULL;
}

STDMETHODIMP 
HippoUI::RegisterListener(IHippoUIListener *listener, UINT64 *listenerId)
{
    HippoListenerProxy *proxy = HippoListenerProxy::createInstance(dataCache_, listener);
    listeners_.push_back(proxy);
   
    *listenerId = proxy->getId();

    return S_OK;
}
 
STDMETHODIMP 
HippoUI::UnregisterListener(UINT64 listenerId)
{
    for (std::vector<HippoPtr<HippoListenerProxy> >::iterator i = listeners_.begin();
         i != listeners_.end();
         i++) 
    {
        HippoListenerProxy *proxy = *i;

        if (proxy->getId() == listenerId) {
            proxy->unregister();
            listeners_.erase(i);
            break;
        }
    }

    return S_OK;
}

STDMETHODIMP 
HippoUI::RegisterEndpoint(UINT64 listenerId, UINT64 *endpointId)
{
    HippoListenerProxy *proxy = findListenerById(listenerId);
    if (proxy)
        *endpointId = proxy->registerEndpoint();

    return S_OK;
}

STDMETHODIMP 
HippoUI::UnregisterEndpoint(UINT64 endpointId)
{
    HippoListenerProxy *proxy = findListenerByEndpoint(endpointId);
    if (proxy)
        proxy->unregisterEndpoint(endpointId);

    return S_OK;
}
    
STDMETHODIMP 
HippoUI::JoinChatRoom(UINT64 endpointId, BSTR chatId, BOOL participant)
{
    HippoListenerProxy *proxy = findListenerByEndpoint(endpointId);
    if (proxy)
        proxy->joinChatRoom(endpointId, chatId, participant);

    return S_OK;
}
 
STDMETHODIMP 
HippoUI::LeaveChatRoom(UINT64 endpointId, BSTR chatId)
{
    HippoListenerProxy *proxy = findListenerByEndpoint(endpointId);
    if (proxy)
        proxy->leaveChatRoom(endpointId, chatId);

    return S_OK;
}

STDMETHODIMP 
HippoUI::SendChatMessage(BSTR chatId, BSTR text, int sentiment)
{
    HippoUStr chatIdU(chatId);
    HippoUStr textU(text);
    HippoSentiment hippoSentiment;

    switch (sentiment) {
        case 0:
            hippoSentiment = HIPPO_SENTIMENT_INDIFFERENT;
            break;
        case 1:
            hippoSentiment = HIPPO_SENTIMENT_LOVE;
            break;
        case 2:
            hippoSentiment = HIPPO_SENTIMENT_HATE;
            break;
        default:
            return E_INVALIDARG;
    }

    HippoChatRoom *room = hippo_data_cache_ensure_chat_room(dataCache_, chatIdU.c_str(), HIPPO_CHAT_KIND_UNKNOWN);
    hippo_connection_send_chat_room_message(hippo_data_cache_get_connection(dataCache_), room, textU.c_str(), hippoSentiment);

    return S_OK;
}

STDMETHODIMP
HippoUI::GetServerName(BSTR *serverName)
{
    // We can't use the value from preferences_ directly, since we want to include
    // the port even for the default value of 80, so rebuild the host:port value
    // from the parsed version.

    char *hostU;
    int port;
    hippo_platform_get_web_host_port(platform_, &hostU, &port);

    HippoBSTR result = HippoBSTR::fromUTF8(hostU, -1);
    result.Append(L":");

    WCHAR buffer[16];
    StringCchPrintfW(buffer, sizeof(buffer) / sizeof(buffer[0]), L"%d", port);
    result.Append(buffer);

    g_free(hostU);

    result.CopyTo(serverName);

    return S_OK;
}

STDMETHODIMP 
HippoUI::LaunchBrowser(BSTR url)
{
    if (!url)
        return E_INVALIDARG;

    launchBrowser(url);

    return S_OK;
}

void
HippoUI::showSignInWindow()
{
    if (!signinWindow_) {
        signinWindow_ = new HippoRemoteWindow(this, L"Sign in to Mugshot", NULL);
    } else {
        signinWindow_->setForegroundWindow();
    }
    signinWindow_->showSignin();
}

void
HippoUI::showMenu(UINT buttonFlag)
{
    POINT pt;
    GetCursorPos(&pt);

    if (GetAsyncKeyState(VK_CONTROL)) {
        HMENU menu;
        HMENU popupMenu;

        if (buttonFlag == TPM_RIGHTBUTTON) {
            menu = debugMenu_;
        } else {
            updateMenu();
            menu = oldMenu_;
        }

        // We:
        //  - Set the foreground window to our (non-shown) window so that clicking
        //    away elsewhere works
        //  - Send the dummy event to force a context switch to our app
        // See Microsoft knowledgebase Q135788

        popupMenu = GetSubMenu(menu, 0);

        SetForegroundWindow(window_);
        TrackPopupMenu(popupMenu, buttonFlag, pt.x, pt.y, 0, window_, NULL);

        PostMessage(window_, WM_NULL, 0, 0);
    } else {
        HippoConnection *connection;

        connection = hippo_data_cache_get_connection(dataCache_);
        if (!hippo_connection_get_connected(connection)) {
            HippoPlatform *platform;
            platform = hippo_connection_get_platform(connection);
            hippo_platform_show_disconnected_window(platform, connection);
        } else {
            hippo_stack_manager_show_browser(stack_, TRUE);
        }
    }
}

bool
HippoUI::crackUrl(BSTR url, URL_COMPONENTS *components)
{
    ZeroMemory(components, sizeof(*components));
    components->dwStructSize = sizeof(*components);

    // The case where lpszHostName is NULL and dwHostNameLength is non-0 means
    // to return pointers into the passed in URL along with lengths. The 
    // specific non-zero value is irrelevant
    components->dwHostNameLength = 1;
    components->dwUserNameLength = 1;
    components->dwPasswordLength = 1;
    components->dwUrlPathLength = 1;
    components->dwExtraInfoLength = 1;

    if (!InternetCrackUrl(url, 0, 0, components))
        return false;
    return true;
}

static void
getWebHostPort(HippoPlatform *platform,
               BSTR          *hostReturn,
               unsigned int  *portReturn)
{
    char *host;
    int port;
    hippo_platform_get_web_host_port(platform, &host, &port);
    HippoBSTR hostBSTR = HippoBSTR::fromUTF8(host, -1);
    g_free(host);
    hostBSTR.CopyTo(hostReturn);
    *portReturn = (unsigned int) port;
}

bool
HippoUI::isFramedPost(BSTR url, BSTR postId)
{
    URL_COMPONENTS components;
    if (!crackUrl(url, &components))
        return false;

    if (components.nScheme != INTERNET_SCHEME_HTTP && components.nScheme != INTERNET_SCHEME_HTTPS)
        return false;
    
    HippoBSTR host;
    unsigned int port;
    getWebHostPort(platform_, &host, &port);

    if (components.dwHostNameLength != host.Length() ||
        wcsncmp(components.lpszHostName, host.m_str, components.dwHostNameLength) != 0 ||
        port != components.nPort)
        return false;

    HippoBSTR expectedExtra(L"?post=");
    expectedExtra.Append(postId);
    if (components.dwUrlPathLength == wcslen(L"/visit") &&
        wcsncmp(components.lpszUrlPath, L"/visit", components.dwUrlPathLength) == 0 &&
        wcsncmp(components.lpszExtraInfo, expectedExtra.m_str, expectedExtra.Length()) == 0) 
    {
        return true;
    }
    return false;
}

bool
HippoUI::isSiteURL(BSTR url)
{
    URL_COMPONENTS components;
    if (!crackUrl(url, &components))
        return false;

    if (components.nScheme != INTERNET_SCHEME_HTTP && components.nScheme != INTERNET_SCHEME_HTTPS)
        return false;

    HippoBSTR host;
    unsigned int port;
    getWebHostPort(platform_, &host, &port);

    if (components.dwHostNameLength != host.Length() ||
        wcsncmp(components.lpszHostName, host.m_str, components.dwHostNameLength) != 0 ||
        port != components.nPort)
        return false;

    // If we're just framing a page, don't count it as a browser pointing to out site. 
    // (For bonus points we'd figure out what was being visited, and check if *that*
    // was on our site, but it's better to just handle that on the server and unframe.)
    if (components.dwUrlPathLength == wcslen(L"/visit") &&
        wcsncmp(components.lpszUrlPath, L"/visit", components.dwUrlPathLength) == 0)
        return false;

    return true;
}

bool
HippoUI::isNoFrameURL(BSTR url)
{
    URL_COMPONENTS components;

    if (!crackUrl(url, &components))
        return false;

    if (components.nScheme != INTERNET_SCHEME_HTTP && components.nScheme != INTERNET_SCHEME_HTTPS)
        return false;

    HippoBSTR host;
    unsigned int port;
    getWebHostPort(platform_, &host, &port);
    if (components.dwHostNameLength != host.Length() ||
        wcsncmp(components.lpszHostName, host, components.dwHostNameLength) != 0 ||
        port != components.nPort)
        return false;

    // Currently the only page we don't frame is /account
    static const WCHAR *noFramePages[] = {
        L"/account",
    };

    static const WCHAR *noFramePagePrefixes[] = {
        L"/group?",
    };

    for (int i = 0; i < sizeof(noFramePages) / sizeof(noFramePages[0]); i++) {
        const WCHAR *page = noFramePages[i];

        if (components.dwUrlPathLength == wcslen(page) &&
            wcsncmp(components.lpszUrlPath, page, components.dwUrlPathLength) == 0)
            return true;
    }

    for (int i = 0; i < sizeof(noFramePagePrefixes) / sizeof(noFramePagePrefixes[0]); i++) {
        const WCHAR *page = noFramePagePrefixes[i];

        if (wcsncmp(components.lpszUrlPath, page, wcslen(page)) == 0)
            return true;
    }

    return false;
}

bool 
HippoUI::needOldIELaunch()
{
    // IE6 and older have an entirely broken default behavior for opening a new URL ... 
    // a random browser is selected and repurposed for the new URL. So, we detect
    // the condition that:
    // 
    //  - The installed version of IE is IE 6 or older
    //  - The user's default browser is IE
    // 
    // and create an IE window directly in that case rather than going through
    // ShellExecute.

    bool haveOldIE = false;

    HippoRegKey versionKey(HKEY_LOCAL_MACHINE, L"SOFTWARE\\Microsoft\\Internet Explorer", false);
    HippoBSTR version;
    if (versionKey.loadString(L"Version", &version)) {
        if ((version[0] == '4' || version[0] == '5' || version[0] == '6') && version[1] == '.')
            haveOldIE = true;
    }

    if (!haveOldIE)
        return false;

    bool launchViaIE = false;

    HippoRegKey commandKey(HKEY_CLASSES_ROOT, L"HTTP\\shell\\open\\command", false);
    HippoBSTR command;
    if (commandKey.loadString(NULL, &command)) {
        if (wcsstr(command.m_str, L"iexplore.exe") != 0 ||
            wcsstr(command.m_str, L"IEXPLORE.EXE") != 0)
        {
            launchViaIE = true;
        }
    }

    if (!launchViaIE)
        return false;

    return true;
}

void 
HippoUI::launchNewBrowserOldIE(BSTR url)
{
    HippoPtr<IWebBrowser2> ie;

    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
                     IID_IWebBrowser2, (void **)&ie);
    HippoBSTR urlStr(url);
    VARIANT missing;
    missing.vt = VT_EMPTY;
    ie->Navigate(urlStr, &missing, &missing, &missing, &missing);
    ie->put_Visible(VARIANT_TRUE);
}

void 
HippoUI::launchNewBrowserGeneric(BSTR url)
{
    ShellExecute(NULL, L"open", url, NULL, NULL, SW_SHOWNORMAL);
}

void
HippoUI::launchBrowser(BSTR url)
{
    // If the URL points directly to our site, try to find another IE window
    // visiting a part of our site and reuse that web browser; this avoids
    // getting a big pile of windows as the user keeps on using the notification
    // icon. We don't currently have this tracking facility if the windows are
    // being opened via Firefox, so we just ignore the issue. Firefox tabs
    // will keep things a bit neater in any case.
    if (isSiteURL(url)) {
        for (ULONG i = 0; i < browsers_.length(); i++) {
            if (isSiteURL(browsers_[i].url)) {
                IWebBrowser2 *browser = browsers_[i].browser;
    
                VARIANT missing;
                missing.vt = VT_EMPTY;
                if (FAILED(browser->Navigate(url, &missing, &missing, &missing, &missing)))
                    continue;

                long windowLong;
                if (SUCCEEDED(browser->get_HWND(&windowLong))) {
                    HWND window = (HWND)(size_t)windowLong; // Suppress a warning

                    // If the window is minimized, we want to restore it, but we
                    // have to be careful not to restore a maximized window, which
                    // ShowWindow(window, SW_RESTORE) will also do.
                    WINDOWPLACEMENT windowPlacement;
                    windowPlacement.length = sizeof(WINDOWPLACEMENT);
                    if (GetWindowPlacement(window, &windowPlacement) && 
                        windowPlacement.showCmd != SW_SHOWMAXIMIZED)
                        ShowWindow(window, SW_RESTORE);

                    SetForegroundWindow(window);
                }
            }
        }
    }

    if (needOldIELaunch())
        launchNewBrowserOldIE(url);
    else
        launchNewBrowserGeneric(url);
}

// Show a window when the user clicks on a shared link
void 
HippoUI::displaySharedLink(BSTR postId, BSTR url)
{
    HippoBSTR targetURL;

    // The initial share from the man of /account is very confusing if framed
    if (isNoFrameURL(url)) {
        targetURL = url;
    } else {
        getRemoteURL(HippoBSTR(L"visit?post="), &targetURL);
        targetURL.Append(postId);
    }

    launchBrowser(targetURL);
}

void
HippoUI::debugLogW(const WCHAR *format, ...)
{
    WCHAR buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), format, vap);
    va_end (vap);

    hippoDebugLogW(L"%ls", buf);
}

void
HippoUI::debugLogU(const char *format, ...)
{
    va_list vap;
    va_start (vap, format);
    char *str = g_strdup_vprintf(format, vap);
    va_end (vap);

    HippoBSTR strW;
    strW.setUTF8(str);
    if (strW) 
        hippoDebugLogW(L"%ls", strW.m_str);
    
    g_free(str);
}

void
HippoUI::logErrorU(const char *fmt, ...)
{
    WCHAR buf[1024];
    HippoBSTR fmtUtf16;
    fmtUtf16.setUTF8(fmt);
    va_list vap;
    va_start(vap, fmt);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), fmtUtf16.m_str, vap);
    va_end(vap);
    HippoBSTR msg;
    msg.Append(L"ERROR: ");
    msg.Append(buf);
    hippoDebugLogW(L"%s", msg.m_str);
}

void 
HippoUI::logHresult(const WCHAR *text, HRESULT result)
{
    HippoBSTR errstr;
    hippoHresultToString(result, errstr);
    debugLogW(L"%s: %s", text, errstr.m_str);
}

void
HippoUI::logLastHresult(const WCHAR *text)
{
    logHresult(text, GetLastError());
}

void 
HippoUI::onAuthFailed()
{
    showSignInWindow();
}


void
HippoUI::onAuthSucceeded()
{
    // currently does nothing
}

void
HippoUI::HippoUIUpgradeWindowCallback::onDocumentComplete()
{
    ui_->upgradeWindow_->show();
    ui_->upgradeWindow_->setForegroundWindow();
}

void 
HippoUI::onUpgradeReady()
{
    if (upgradeWindow_)
        delete upgradeWindow_;
    if (!upgradeWindowCallback_) 
        upgradeWindowCallback_ = new HippoUIUpgradeWindowCallback(this);
    upgradeWindow_ = new HippoRemoteWindow(this, L"New version of Mugshot!", upgradeWindowCallback_);
    HippoBSTR url;
    getRemoteURL(HippoBSTR(L"upgrade"), &url);
    upgradeWindow_->navigate(url);
}

// Tries to register as the singleton HippoUI, returns true on success
bool 
HippoUI::registerActive()
{
    int retryCount = 2; // No infinite loops
RETRY_REGISTER:
    IHippoUI *pHippoUI;

    QueryInterface(IID_IHippoUI, (LPVOID *)&pHippoUI);
    HRESULT hr = RegisterActiveObject(pHippoUI, 
                                      *(getPreferences()->getInstanceClassId()),
                                      ACTIVEOBJECT_STRONG, &registerHandle_);
    pHippoUI->Release();

    if (FAILED(hr)) {
        MessageBox(NULL, TEXT("Error registering Mugshot"), NULL, MB_OK);
        return false;
    } else if (hr == MK_S_MONIKERALREADYREGISTERED) {
        // Duplicates are actually succesfully registered, so we have to remove
        // ourself before exiting, or alternatively, before telling the old copy to
        // remove itself then retrying.
        registered_ = true;
        revokeActive();

        // If we were launched with the --replace flag, then if an existing instance
        // was already running, we tell it to exit, and run ourself. Otherwise, we
        // tell the old copy to show recently shared URLs.

        HippoPtr<IUnknown> unknown;
        HippoPtr<IHippoUI> oldUI;
        if (SUCCEEDED (GetActiveObject(*(getPreferences()->getInstanceClassId()), NULL, &unknown)))
            unknown->QueryInterface<IHippoUI>(&oldUI);

        if (replaceExisting_) {
            if (retryCount-- > 0) {
                if (oldUI)
                    quitAndWait(oldUI);

                goto RETRY_REGISTER;
            }
        } else {
            if (oldUI)
                oldUI->ShowRecent();
        }

        g_debug("Another copy already running, will exit");

        return false;
    }

    registered_ = true;
    
    // There might already be explorer windows open, so broadcast a message
    // that causes HippoTracker to recheck the active object table
    UINT uiStartedMessage = RegisterWindowMessage(TEXT("HippoUIStarted"));
    SendNotifyMessage(HWND_BROADCAST, uiStartedMessage, 0, 0);

    return true;
}

// Removes previous registration via registerActive()
void
HippoUI::revokeActive()
{
    if (registered_) {
        RevokeActiveObject(registerHandle_, NULL);

        registered_ = false;
    }
}

// We register ourself as a startup program each time we run; if we are already registered
// we'll just write over the old copy
void 
HippoUI::registerStartup()
{
    if (instanceType_ == HIPPO_INSTANCE_NORMAL) {
        WCHAR commandLine[MAX_PATH];
        GetModuleFileName(instance_, commandLine, sizeof(commandLine) / sizeof(commandLine[0]));
        HippoRegistrar registrar(NULL);
        registrar.registerStartupProgram(L"Mugshot", commandLine);
    }
}

// We unregister as a startup program when the user selects Exit explicitly
void
HippoUI::unregisterStartup()
{
    if (instanceType_ == HIPPO_INSTANCE_NORMAL) {
        HippoRegistrar registrar(NULL);
        registrar.unregisterStartupProgram(L"Mugshot");
    }
}

bool
HippoUI::registerClass()
{
    WNDCLASSEX wcex;

    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style          = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc    = windowProc;
    wcex.cbClsExtra     = 0;
    wcex.cbWndExtra     = 0;
    wcex.hInstance      = instance_;
    wcex.hIcon          = bigIcon_;
    wcex.hCursor        = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;
    wcex.hIconSm        = smallIcon_;

    return RegisterClassEx(&wcex) != 0;
}

bool
HippoUI::createWindow(void)
{
    WCHAR title[MAX_LOADSTRING];
    LoadString(instance_, IDS_APP_TITLE, title, MAX_LOADSTRING);

    window_ = CreateWindow(CLASS_NAME, title, WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, 0, CW_USEDEFAULT, 0, NULL, NULL, instance_, NULL);
    
    if (!window_)
        return false;

    hippoSetWindowData<HippoUI>(window_, this);

    return true;
}

void 
HippoUI::showPreferences()
{
    if (!preferencesDialog_) {
        preferencesDialog_ = CreateDialogParam(instance_, MAKEINTRESOURCE(IDD_PREFERENCES),
                                               window_, preferencesProc, (::LONG_PTR)this);
        if (!preferencesDialog_)
            return;

        SendDlgItemMessage(preferencesDialog_, IDC_LOGOICON, STM_SETICON, (WPARAM)bigIcon_, 0);

        HippoBSTR messageServer;
        getPreferences()->getMessageServer(&messageServer);
        SetDlgItemText(preferencesDialog_, IDC_MESSAGE_SERVER, messageServer);

        HippoBSTR webServer;
        getPreferences()->getWebServer(&webServer);
        SetDlgItemText(preferencesDialog_, IDC_WEB_SERVER, webServer);
    }
    
    onHasAuthChanged(); // update the password button sensitivity
    ShowWindow(preferencesDialog_, SW_SHOW);
}

void 
HippoUI::onHasAuthChanged()
{
    if (!preferencesDialog_)
        return;

    HWND forgetPassButton = GetDlgItem(preferencesDialog_, IDC_FORGETPASSWORD);
    if (forgetPassButton)
        EnableWindow(forgetPassButton, hippo_connection_get_has_auth(getConnection()));
}

bool
HippoUI::timeoutCheckIdle() 
{
    LASTINPUTINFO lastInput;
    DWORD currentTime;

    /* GetLastInputInfo is only available on Windows 2000 and newer. To handle
     * detection of user idle on older systems, the best approach seems to be
     * to use SetWindowsHookEx() with WH_MOUSE and WH_KEYBOARD to create 
     * global hooks. In addition to the (slight) performance impact that would
     * cause, it's a little bit of a pain to program: the hook needs to be
     * in a DLL (presumably, for us, HippoUtil.dll) and probably needs to 
     * run in its own thread so that it gets called and returns as fast as
     * possible even if we are busy doing something else.
     */
    ZeroMemory(&lastInput, sizeof(LASTINPUTINFO));
    lastInput.cbSize = sizeof(LASTINPUTINFO);
    GetLastInputInfo(&lastInput);

    currentTime = GetTickCount();

    if (currentTime - lastInput.dwTime > USER_IDLE_TIME) {
        if (!idle_) {
            idle_ = true;
            // FIXME hippo_stack_manager_set_idle
            // bubble_.setIdle(true);
        }
    } else {
        if (idle_) {
            idle_ = false;
            // FIXME hippo_stack_manager_set_idle
            // bubble_.setIdle(false);
        }
    }

    /* Getting notification on screen saver starts/stops without polling also would 
     * require a global hook. (For SC_SCREENSAVE) We actually don't need notification 
     * when the screensaver starts, but we do need it when it is deactivated so we
     * know to pop up our bubble at that point if we have one queued.
     */
    BOOL screenSaverRunning;
    SystemParametersInfo(SPI_GETSCREENSAVERRUNNING, 0, (void *)&screenSaverRunning, 0);
    if (!screenSaverRunning_ != !screenSaverRunning) {
        screenSaverRunning_ = screenSaverRunning != FALSE;
        // FIXME would want to forward this to stack manager
        // bubble_.setScreenSaverRunning(screenSaverRunning_);
    }

    return true;
}

static bool
urlIsLocal(const WCHAR *url)
{
    WCHAR schemaBuf[64];
    DWORD schemaSize;

    if (CoInternetParseUrl(url, PARSE_SCHEMA, 0,
                           schemaBuf, sizeof(schemaBuf) / sizeof(schemaBuf[0]), 
                           &schemaSize, 0) != S_OK)
        return false;

    return wcscmp(schemaBuf, L"file") == 0;
}

void
HippoUI::updateMenu()
{
    HMENU popupMenu = GetSubMenu(oldMenu_, 0);

    // Delete previous dynamic menuitems
    while (TRUE) {
        int id = GetMenuItemID(popupMenu, 0);
        if (id >= IDM_SHARE0 && id <= IDM_SIGN_OUT)
            RemoveMenu(popupMenu, 0, MF_BYPOSITION);
        else
            break;
    }

    // Now insert new ones for the current URLs
    UINT pos = 0;
    for (ULONG i = 0; i < browsers_.length() && i < 10; i++) {
        MENUITEMINFO info;
        WCHAR menubuf[64];

        if (!browsers_[i].title)
            continue;
    
        if (urlIsLocal(browsers_[i].url))
            continue;

        StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Share "));
        StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, browsers_[i].title);
        StringCchCat(menubuf, sizeof(menubuf) / sizeof(TCHAR) - 5, TEXT("..."));
        StringCchCopy(menubuf + sizeof(menubuf) / sizeof(TCHAR) - 6, 6, TEXT("[...]"));

        memset((void *)&info, 0, sizeof(MENUITEMINFO));
        info.cbSize = sizeof(MENUITEMINFO);

        info.fMask = MIIM_ID | MIIM_DATA | MIIM_STRING;
        info.fType = MFT_STRING;
        info.wID = IDM_SHARE0 + i;
        info.dwTypeData = menubuf;
            
        InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }

    // Insert a separator if necessary
    if (pos != 0) {
        MENUITEMINFO info;
    
        memset((void *)&info, 0, sizeof(MENUITEMINFO));
        info.cbSize = sizeof(MENUITEMINFO);

        info.fMask = MIIM_ID | MIIM_FTYPE;
        info.fType = MFT_SEPARATOR;
        info.wID = IDM_SHARESEPARATOR;

        InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }

    // Insert the sign in / sign out menu item
    {
        MENUITEMINFO info;
        WCHAR menubuf[64];

        memset((void *)&info, 0, sizeof(MENUITEMINFO));
        info.cbSize = sizeof(MENUITEMINFO);

        info.fMask = MIIM_ID | MIIM_DATA | MIIM_STRING;
        info.fType = MFT_STRING;
        info.wID = IDM_SIGN_IN;

        HippoState state = hippo_connection_get_state(getConnection());
        if (state == HIPPO_STATE_SIGNED_OUT || state == HIPPO_STATE_SIGN_IN_WAIT) {
            info.wID = IDM_SIGN_IN;
            StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign In..."));
        } else {
            info.wID = IDM_SIGN_OUT;
            StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign Out"));
        }

        info.dwTypeData = menubuf;
            
        InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }

    EnableMenuItem(popupMenu, IDM_MISSED, haveMissedBubbles_ ? MF_ENABLED : MF_GRAYED);
}

HippoBSTR
HippoUI::getBasePath() throw (std::bad_alloc, HResultException)
{
    // XXX can theoretically truncate if we have a \?\\foo\bar\...
    // path which isn't limited to the short Windows MAX_PATH
    // Could use dynamic allocation here
    WCHAR baseBuf[MAX_PATH];

    if (!GetModuleFileName(instance_, baseBuf, sizeof(baseBuf) / sizeof(baseBuf[0])))
        throw HResultException(GetLastError());

    size_t i;
    for (i = wcslen(baseBuf); i > 0; i--)
        if (baseBuf[i - 1] == '\\')
            break;

    if (i == 0)  // No \ in path?
        throw HResultException(E_FAIL);

    return HippoBSTR((UINT)i, baseBuf);
}

void
HippoUI::getImagePath(BSTR filename, BSTR *result) throw (std::bad_alloc, HResultException)
{
    assert(*result == NULL);

    HippoBSTR path(getBasePath());
    
    path.Append(L"images\\");

    path.Append(filename);
    *result = ::SysAllocStringLen(path.m_str, path.Length());
}

// Get the URL of a file on the web server
void
HippoUI::getRemoteURL(BSTR  appletName, 
                      BSTR *result)
{
    HippoBSTR webServer;
    HippoBSTR url(L"http://");

    getPreferences()->getWebServer(&webServer);

    url.Append(webServer);

    url.Append(L"/");

    url.Append(appletName);

    url.CopyTo(result);
}

bool
HippoUI::processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    int wmId, wmEvent;

    // Messages sent from the notification icon or the taskbar
    if (notificationIcon_.processMessage(message, wParam, lParam))
        return true;

    switch (message) 
    {
    case WM_COMMAND:
        wmId    = LOWORD(wParam); 
        wmEvent = HIWORD(wParam);
        if (wmId >= IDM_SHARE0 && wmId <= IDM_SHARE9) {
            UINT i = wmId - IDM_SHARE0;
            if (i < browsers_.length() && browsers_[i].url)
                ShareLink(browsers_[i].url, browsers_[i].title, NULL);
            return true;
        }

        switch (wmId)
        {
        case IDM_SIGN_IN:
            if (hippo_connection_signin(getConnection()))
                showSignInWindow();
            return true;
        case IDM_SIGN_OUT:
            hippo_connection_signout(getConnection());
            return true;
        case IDM_RECENT:
            ShowRecent();
            return true;
        case IDM_PREFERENCES:
            showPreferences();
            return true;
        case IDM_DEBUGLOG:
            logWindow_.show();
            return true;
        case IDM_EXIT:
            unregisterStartup();
            DestroyWindow(window_);
            return true;
        }
        break;
    case WM_DESTROY:
        PostQuitMessage(0);
        return true;
    }

    return false;
}

LRESULT CALLBACK 
HippoUI::windowProc(HWND   window,
                    UINT   message,
                    WPARAM wParam,
                    LPARAM lParam)
{
    HippoUI *ui = hippoGetWindowData<HippoUI>(window);
    if (ui) {
        if (ui->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

INT_PTR CALLBACK 
HippoUI::preferencesProc(HWND   dialog,
                         UINT   message,
                         WPARAM wParam,
                         LPARAM lParam)
{
    if (message == WM_INITDIALOG) {
        HippoUI *ui = (HippoUI *)lParam;
        hippoSetWindowData<HippoUI>(dialog, ui);

        return TRUE;
    }

    HippoUI *ui = hippoGetWindowData<HippoUI>(dialog);
    if (!ui)
        return FALSE;

    switch (message) {
    case WM_COMMAND:
        switch (LOWORD(wParam)) {
        case IDC_FORGETPASSWORD:
            hippo_connection_forget_auth(ui->getConnection());
            return TRUE;
        case IDOK:
        {
            WCHAR messageServer[128];
            messageServer[0] = '\0';
            GetDlgItemText(dialog, IDC_MESSAGE_SERVER, 
                           messageServer, sizeof(messageServer) / sizeof(messageServer[0]));
            HippoUStr messageServerU(messageServer);
            hippo_platform_set_message_server(ui->platform_, messageServerU.c_str());

            WCHAR webServer[128];
            webServer[0] = '\0';
            GetDlgItemText(dialog, IDC_WEB_SERVER, 
                           webServer, sizeof(webServer) / sizeof(webServer[0]));
            HippoUStr webServerU(webServer);
            hippo_platform_set_web_server(ui->platform_, webServerU.c_str());

            EndDialog(dialog, TRUE);
        }
            
        return TRUE;
        case IDCANCEL:
            EndDialog(dialog, FALSE);
            return TRUE;
        }
    }

    return FALSE;
}

/* Finds all IE and Explorer windows on the system, and closes any pointing
 * to http://*.dumbhippo.com/download or http://*.mugshot.org/download. This 
 * is meant for the initial install when the user has installed our software from
 * /download and we don't want to leave the /download page there in an internet 
 * explorer window without instrumentation.
 */
static void
closeDownload()
{
    HippoPtr<IShellWindows> shellWindows;
    HRESULT hr = CoCreateInstance(CLSID_ShellWindows, NULL, CLSCTX_ALL, IID_IShellWindows, (void **)&shellWindows);
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't create CLSID_ShellWindows: %x", hr);
        return;
    }

    LONG count;
    shellWindows->get_Count(&count);
    for (LONG i = 0; i < count; i++) {
        HippoPtr<IDispatch> dispatch;
        VARIANT item;
        item.vt = VT_I4;
        item.intVal = i;
        hr = shellWindows->Item(item, &dispatch);
        if (SUCCEEDED(hr)) {
            HippoQIPtr<IWebBrowser2> browser(dispatch);

            if (!browser)
                continue;

            HippoBSTR browserURL;
            browser->get_LocationURL(&browserURL);
            if (!browserURL)
                continue;

            HippoURLParser parser(browserURL);
            if (!parser.ok())
                continue;

            if (parser.getScheme() != INTERNET_SCHEME_HTTP)
                continue;

            HippoBSTR urlPath = parser.getUrlPath();
            if (!(urlPath.m_str && wcscmp(urlPath.m_str, L"/download") == 0))
                continue;

            HippoBSTR hostName = parser.getHostName();
            if (!(hostName &&
                  (wcscmp(hostName.m_str, L"dumbhippo.com") == 0 || 
                   hostName.endsWith(L".dumbhippo.com") ||
                   wcscmp(hostName.m_str, L"mugshot.org") == 0 || 
                   hostName.endsWith(L".mugshot.org"))))
                continue;

            browser->Quit();
        }
    }

}

HippoMessageHookList *messageHooks = NULL;

void 
HippoUI::registerMessageHook(HWND window, HippoMessageHook *hook)
{
    if (!messageHooks)
        messageHooks = new HippoMessageHookList();
    
    messageHooks->registerMessageHook(window, hook);
}

void 
HippoUI::unregisterMessageHook(HWND window)
{
    if (messageHooks)
        messageHooks->unregisterMessageHook(window);
}

/* Define a custom main loop source for integrating the Glib main loop with Win32
 * message handling; this isn't very generalized, since we hardcode the handling
 * of a FALSE return from GetMessage() to call g_main_loop_quit() on a particular
 * loop. If we were being more general, we'd probably want a Win32SourceQuitFunc.
 */
struct Win32Source {
    GSource source;
    GPollFD pollFD;
    int result;
    GMainLoop *loop;
};

static gboolean 
win32SourcePrepare(GSource *source,
                   int     *timeout)
{
    MSG msg;

    *timeout = -1;

    return PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);
}

static gboolean
win32SourceCheck(GSource *source)
{
    MSG msg;

    return PeekMessage(&msg, NULL, 0, 0, PM_NOREMOVE);
}

static gboolean
win32SourceDispatch(GSource     *source,
                    GSourceFunc  callback,
                    gpointer     userData)
{
    MSG msg;

    // Don't use GetMessage() here, since the event we saw in check() could
    // have been stolen out from under us in the meantime, causing a hang
    if (!PeekMessage(&msg, NULL, 0, 0, PM_REMOVE))
        return TRUE;

    if (msg.message == WM_QUIT) {
        Win32Source *win32Source = (Win32Source *)(source);

        win32Source->result = (int)msg.wParam;

        g_main_context_remove_poll (NULL, &win32Source->pollFD);
        g_main_loop_quit(win32Source->loop);
        return FALSE;
    }

    if (messageHooks && messageHooks->processMessage(&msg))
        return TRUE;

    try {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    } catch (...) {
        hippoDebugLogW(L"*** Unhandled exception trapped in dispatch");
    }

    return TRUE;
}

static void
win32SourceFinalize(GSource *source)
{
}

static const GSourceFuncs win32SourceFuncs = {
    win32SourcePrepare,
    win32SourceCheck,
    win32SourceDispatch,
    win32SourceFinalize
};

static GSource *
win32SourceNew(GMainLoop *loop)
{
    GSource *source = g_source_new((GSourceFuncs *)&win32SourceFuncs, sizeof(Win32Source));
    Win32Source *win32Source = (Win32Source *)source;

    win32Source->pollFD.fd = G_WIN32_MSG_HANDLE;
    win32Source->pollFD.events = G_IO_IN;
    win32Source->result = 0;
    win32Source->loop = loop;

    g_main_context_add_poll(NULL, &win32Source->pollFD, G_PRIORITY_DEFAULT);

    return source;
}

static bool
initializeWinSock(void)
{
    WSADATA wsData;

    // We can support WinSock 2.2
    int result = WSAStartup(MAKEWORD(2,2), &wsData);
    // Fail to initialize if the system doesn't at least of WinSock 2.0
    // Both of these versions are pretty much arbitrary. No testing across
    // a range of versions has been done.
    if (result || LOBYTE(wsData.wVersion) < 2) {
        if (!result)
            WSACleanup();
        MessageBox(NULL, L"Couldn't initialize WinSock", NULL, MB_OK);
        return false;
    }

    return true;
}

static void
installLaunch(HINSTANCE instance)
{
    WCHAR fileBuf[MAX_PATH];
    if (!GetModuleFileName(instance, fileBuf, sizeof(fileBuf) / sizeof(fileBuf[0])))
        return;

    _wspawnl(_P_NOWAIT, fileBuf, L"HippoUI", L"--replace", NULL);
}

static void
quitAndWait(IHippoUI *ui)
{
    DWORD processId;
    HRESULT hr = ui->Quit(&processId); 
    if (FAILED(hr)) {
        hippoDebugLogW(L"Couldn't signal old process to quit");
        return;
    }

    HANDLE process = OpenProcess(SYNCHRONIZE, FALSE, processId);
    if (!process) {
        hippoDebugLogW(L"Couldn't open old process to wait for it to quit, might have exited already");
        return;
    }

    if (WaitForSingleObject(process, 30000) != WAIT_OBJECT_0) {
        hippoDebugLogW(L"Waiting for old process to quit timed out or failed");
        return;
    }

    hippoDebugLogW(L"Succesfully waited for old process to exit");
}

static void
quitExisting(HippoInstanceType instanceType)
{
    HippoPtr<IUnknown> unknown;
    HippoPtr<IHippoUI> oldUI;
    if (SUCCEEDED (GetActiveObject(*HippoPreferences::getInstanceClassId(instanceType), NULL, &unknown)))
       unknown->QueryInterface<IHippoUI>(&oldUI);

    if (oldUI)
        quitAndWait(oldUI);

    // Dereferencing the HippoUI at this point triggers a warning from the runtime since
    // it can't talk to the HippoUI object which is gone, but it seems to be safe
}

static void
editToolbar()
{
    // ensureToolbarButton() should be called when IE isn't running, since it edits
    // registry entries behind IE's back. This is normally the case the first time
    // that the client is run after an install. If it isn't the case (say, another user 
    // installed DumbHippo on the system), then we'll hope for the best. Testi
    // indicates that things should work OK for new IE windows opened later, though
    // existing IE windows will, of course, not be affected.
    //
    // After the first time, it doesn't matter if IE is running or not, since we
    // remember that we've already added (or didn't have to add) the button
    // and don't try again.

    HippoToolbarEdit edit;
    edit.ensureToolbarButton();
}

// In order for our Firefox extension to be found by Firefox, we need to install
// a key pointing to it under HKEY_LOCAL_MACHINE or HKEY_CURRENT_USER. Normally,
// this key is added and removed by the installer, but in the case where the
// user is also running debug instances of Mugshot, we want to get that key
// pointing to the right place, so we check on startup, and if the key is missing
// or mis-pointed we rewrite it under HKEY_CURRENT_USER.
static void
registerFirefoxComponent()
{
    HMODULE module;
    WCHAR buf[MAX_PATH];
    
    module = GetModuleHandle(NULL);
    DWORD len = GetModuleFileName(module, buf, MAX_PATH);
    if (len == 0 || len == MAX_PATH) // Failure or truncated
        return;

    WCHAR *lastSlash = wcsrchr(buf, '\\');
    if (lastSlash == 0)
        return;

    HippoBSTR extensionDir(lastSlash - buf, buf);
    extensionDir.Append(L"\\firefox");

    HippoBSTR userValue;
    HippoRegKey userKey(HKEY_CURRENT_USER, L"SOFTWARE\\Mozilla\\Firefox\\Extensions", FALSE);
    if (userKey.loadString(L"firefox@mugshot.org", &userValue)) {
        if (userValue == extensionDir)
            return;
    }

    HippoBSTR machineValue;
    HippoRegKey machineKey(HKEY_LOCAL_MACHINE, L"SOFTWARE\\Mozilla\\Firefox\\Extensions", FALSE);
    if (machineKey.loadString(L"firefox@mugshot.org", &machineValue)) {
        if (machineValue == extensionDir)
            return;
    }

    HippoRegKey saveKey(HKEY_CURRENT_USER, L"SOFTWARE\\Mozilla\\Firefox\\Extensions", TRUE);
    saveKey.saveString(L"firefox@mugshot.org", extensionDir.m_str);
}

static void
migrateCookie()
{
    bool migratedDumbHippoCookie = false;

    HippoRegKey loadKey(HKEY_CURRENT_USER, HIPPO_CLIENT_SUBKEY, false);
    loadKey.loadBool(L"MigratedDumbHippoCookie", &migratedDumbHippoCookie);

    if (migratedDumbHippoCookie)
        return;

    hippo_platform_impl_windows_migrate_cookie("dumbhippo.com", "mugshot.org");

    HippoRegKey saveKey(HKEY_CURRENT_USER, HIPPO_CLIENT_SUBKEY, true);
    saveKey.saveBool(L"MigratedDumbHippoCookie", true);
}

static void
test_alloc_failure_behavior()
{
#if 0
    // using just max() without / 2 will trigger some funky case 
    // in the visual studio debug malloc
    size_t maxSize = std::numeric_limits<size_t>::max() / 2;
    try {
        void *t = new char[maxSize];
        hippoDebugLogW(L"allocated t is %p", t);
        delete t; // in case it succeeded, but may be 0...
    } catch (std::bad_alloc &e) {
        hippoDebugLogU("exception is %s", e.what());
    } catch (...) {
        hippoDebugLogW(L"caught generic exception on alloc failure");
    }
#endif
}

static void
print_debug_func(const char *message)
{
    hippoDebugLogU("%s", message);
}

int APIENTRY 
WinMain(HINSTANCE hInstance,
        HINSTANCE hPrevInstance,
        LPSTR     lpCmdLine,
        int       nCmdShow)
{
    HippoUI *ui;
    GMainLoop *loop;
    GSource *source;
    int result;
    int argc;
    char **argv;
    HippoOptions options;

    //_putenv("G_DEBUG=fatal_warnings");

    g_thread_init(NULL);
    g_type_init();

    char *command_line = GetCommandLineA();
    GError *error = NULL;

    if (!g_shell_parse_argv(command_line, &argc, &argv, &error)) {
        g_printerr("%s\n", error->message);
        return 1;
    }

    hippo_set_print_debug_func(print_debug_func);

    // FIXME there's a problem here in that if hippo_parse_options removes
    // parsed options (not sure if it does but it should) then we'd leak those elements
    // of argv. Need to copy argv first for purposes of freeing it, or something.
    if (!hippo_parse_options(&argc, &argv, &options)) {
        return 1;
    }

    g_strfreev(argv);

	// See if we were run because a previous instance crashed; if so, offer the user
	// the option of uploading the crash dump to us.
	if (options.crash_dump) {
		HippoBSTR dump = HippoBSTR::fromUTF8(options.crash_dump);
        bool keepRunning = hippoCrashReport(options.instance_type, options.crash_dump);
		if (!keepRunning)
			return 0;
	}

	// OK, now we can install the crash handler without risk of circular spawning
    hippoCrashInit(options.instance_type);

    // If run as --install-launch, we rerun ourselves asynchronously, then immediately exit
    if (options.install_launch) {
        CoInitialize(NULL);
        closeDownload();
        CoUninitialize();
        installLaunch(hInstance);
        return 0;
    }

    if (options.quit_existing) {
        CoInitialize(NULL);
        quitExisting(HIPPO_INSTANCE_NORMAL);
        quitExisting(HIPPO_INSTANCE_DEBUG);
        quitExisting(HIPPO_INSTANCE_DOGFOOD);
        CoUninitialize();
        return 0;
    }

    int ccMajor = 0, ccMinor = 0;
    if (hippoGetDllVersion(L"comctl32.dll", &ccMajor, &ccMinor))
        hippoDebugLogW(L"Common controls dll %d.%d", ccMajor, ccMinor);
    else
        hippoDebugLogW(L"Failed to get common controls version");

    if (ccMajor < 6) {
        hippoDebugDialog(L"Mugshot requires Windows XP.\n(Common controls version %d.%d is too old.)", ccMajor, ccMinor);
        return 1;
    }

    // Initialize OLE and COM; We need to use this rather than CoInitialize
    // in order to get cut-and-paste and drag-and-drop to work
    OleInitialize(NULL);

    // Initialize common control window classes; needed to use e.g. 
    // scrollbar and tooltips
    INITCOMMONCONTROLSEX initControls;
    initControls.dwSize = sizeof(initControls);
    initControls.dwICC = ICC_STANDARD_CLASSES | ICC_TAB_CLASSES;

    // We used to check the return value of InitCommonControlsEx, but we
    // had a report of failure, http://bugzilla.mugshot.org/show_bug.cgi?id=1196,
    // and since there is no documentation as to why it might fails, we're
    // better off just ignoring the return code. Nobody checks the result.
    InitCommonControlsEx(&initControls);

    if (!initializeWinSock())
        return 0;

    editToolbar();
    registerFirefoxComponent();
    migrateCookie();

    ui = new HippoUI(options.instance_type, options.replace_existing, options.initial_debug_share);
    if (!ui->create(hInstance)) {
        g_debug("Failed to create UI");
        return 0;
    }

    loop = g_main_loop_new(NULL, FALSE);

    source = win32SourceNew(loop);
    g_source_attach(source, NULL);

    test_alloc_failure_behavior();

    hippo_options_free_fields(&options);

    g_main_loop_run(loop);

    result = ((Win32Source *)source)->result;
    g_source_unref(source);

    g_main_loop_unref(loop);

    ui->destroy();
    ui->Release();

    WSACleanup();
    OleUninitialize();

    return result;
}
