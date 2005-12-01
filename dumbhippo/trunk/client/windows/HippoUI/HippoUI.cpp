/* HippoUI.cpp: global singleton UI object
 *
 * Copyright Red Hat, Inc. 2005
 **/
#include "stdafx.h"
#include "HippoUI.h"
#include <stdio.h>
#include <process.h>
#include <strsafe.h>
#include <exdisp.h>
#include <HippoUtil.h>
#include <HippoRegistrar.h>
#include <HippoUtil_i.c>
#include <Winsock2.h>
#include <urlmon.h>   // For CoInternetParseUrl
#include <wininet.h>  // for cookie retrieval
#include "Resource.h"
#include "HippoHTTP.h"

#include <glib.h>

// GUID definition
#pragma data_seg(".text")
#define INITGUID
#include <initguid.h>
#include "Guid.h"
#pragma data_seg()

static const int MAX_LOADSTRING = 100;
static const TCHAR *CLASS_NAME = TEXT("HippoUIClass");

// If this long elapses since the last activity, count the user as idle (in ms)
static const int USER_IDLE_TIME = 30 * 1000;

// How often to check if the user is idle (in ms)
static const int CHECK_IDLE_TIME = 5 * 1000;

HippoUI::HippoUI(bool debug, bool replaceExisting, bool initialDebugShare) 
    : preferences_(debug)
{
    refCount_ = 1;
    debug_ = debug;
    replaceExisting_ = replaceExisting;
    initialShowDebugShare_ = initialDebugShare;

    HippoPtr<ITypeLib> typeLib;
    HRESULT hr = LoadRegTypeLib(LIBID_HippoUtil, 
                                0, 1, /* Version */
                                0,    /* LCID */
                                &typeLib);
    if (SUCCEEDED (hr))
        typeLib->GetTypeInfoOfGuid(IID_IHippoUI, &uiTypeInfo_);
    else
        hippoDebug(L"Failed to load type lib: %x\n", hr);

    notificationIcon_.setUI(this);
    bubble_.setUI(this);
    upgrader_.setUI(this);

    preferencesDialog_ = NULL;

    connected_ = false;
    registered_ = false;

    nextBrowserCookie_ = 0;

    rememberPassword_ = FALSE;
    passwordRemembered_ = FALSE;

    smallIcon_ = NULL;
    bigIcon_ = NULL;

    idle_ = FALSE;
    checkIdleTimeoutId_ = 0;
}

HippoUI::~HippoUI()
{
    DestroyIcon(smallIcon_);
    DestroyIcon(bigIcon_);
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
HippoUI::Quit()
{
    // We need to unregister ourself as the active HippoUI implementation before
    // we return, but we need to return to the caller, not just exit immediately.

    revokeActive();
    g_idle_add(doQuit, this);

    return S_OK;
}

STDMETHODIMP
HippoUI::ShowRecent()
{
    HippoBSTR recentURL;

    HippoPtr<IWebBrowser2> webBrowser;
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
                     IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser) {
        hippoDebug(L"Couldn't create web browser, we lose");
        return E_FAIL;
    }
    
    HRESULT hr = getRemoteURL(HippoBSTR(L"home"), &recentURL);
    if (!SUCCEEDED(hr))
        return hr;

    VARIANT missing;
    missing.vt = VT_EMPTY;

    webBrowser->Navigate(recentURL,
                         &missing, &missing, &missing, &missing);

    webBrowser->put_Visible(VARIANT_TRUE);

    return S_OK;
}

////////////////////////////////////////////////////////////////////////////


void
HippoUI::updateIcons(void)
{
    TCHAR *icon;

    if (connected_) {
        icon = MAKEINTRESOURCE(debug_ ? IDI_DUMBHIPPO_DEBUG : IDI_DUMBHIPPO);
    } else {
        icon = MAKEINTRESOURCE(debug_ ? IDI_DUMBHIPPO_DEBUG_DISCONNECTED : IDI_DUMBHIPPO_DISCONNECTED);
    }
    if (smallIcon_ != NULL)
        DestroyIcon(smallIcon_);
    if (bigIcon_ != NULL)
        DestroyIcon(bigIcon_);

    smallIcon_ = (HICON)LoadImage(instance_, icon,
                                  IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    bigIcon_ = (HICON)LoadImage(instance_, icon,
                                IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
}

void
HippoUI::onConnectionChange(bool connected)
{
    if (connected_ == connected)
        return;
    connected_ = connected;
    updateIcons();

    notificationIcon_.updateIcon(smallIcon_);
}

static void
testStatusCallback(HINTERNET ictx, DWORD_PTR uctx, DWORD status, LPVOID statusInfo, 
                  DWORD statusLength)
{
    HippoUI *ui = (HippoUI*) uctx;
}

bool
HippoUI::create(HINSTANCE instance)
{
    instance_ = instance;
   
    updateIcons();
    menu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_NOTIFY));
    debugMenu_ = LoadMenu(instance, MAKEINTRESOURCE(IDR_DEBUG));

    if (!registerClass())
        return false;

    if (!registerActive())
        return false;


    if (!createWindow()) {
        revokeActive();
        return false;
    }

    notificationIcon_.setIcon(smallIcon_);
    if (!notificationIcon_.create(window_)) {
        revokeActive();
        return false;
    }

    logWindow_.setBigIcon(bigIcon_);
    logWindow_.setSmallIcon(smallIcon_);
    if (!logWindow_.create()) {
        revokeActive();
        notificationIcon_.destroy();
        return false;
    }

    im_.setUI(this);
    if (preferences_.getSignIn()) {
        im_.signIn();
    }

    flickr_.setUI(this);

    checkIdleTimeoutId_ = g_timeout_add(CHECK_IDLE_TIME, checkIdle, this);

    if (this->initialShowDebugShare_) {
        HippoLinkShare linkshare;

        linkshare.url.setUTF8("http://flickr.com/photos/tweedie/63302017/");
        linkshare.postId.setUTF8("2");
        linkshare.title.setUTF8("funny photo");
        linkshare.senderName.setUTF8("debug user");
        linkshare.senderId.setUTF8("3");
        linkshare.description.setUTF8("debug share");
        HippoBSTR recipient(L"debug recipient");
        linkshare.personRecipients.append(recipient);
        onLinkMessage(linkshare);
    }

    return true;
}

void
HippoUI::destroy()
{
    if (checkIdleTimeoutId_)
        g_source_remove(checkIdleTimeoutId_);

    notificationIcon_.destroy();
    
    revokeActive();
}

HippoPreferences *
HippoUI::getPreferences()
{
    return &preferences_;
}

void
HippoUI::showAppletWindow(BSTR url, HippoPtr<IWebBrowser2> &webBrowser)
{
    long width = 500;
    long height = 600;
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
                     IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser)
        return;

    VARIANT missing;
    missing.vt = VT_NULL;

    webBrowser->Navigate(url,
                         &missing, &missing, &missing, &missing);
    webBrowser->put_AddressBar(VARIANT_FALSE);
    webBrowser->put_MenuBar(VARIANT_FALSE);
    webBrowser->put_StatusBar(VARIANT_FALSE);
    webBrowser->put_ToolBar(VARIANT_FALSE);
    webBrowser->put_Width(width);
    webBrowser->put_Height(height);

    RECT workArea;
    if (::SystemParametersInfo(SPI_GETWORKAREA, 0, &workArea, 0)) {
        webBrowser->put_Left((workArea.left + workArea.right - width) / 2);
        webBrowser->put_Top((workArea.bottom + workArea.top - height) / 2);
    }

    HippoPtr<IDispatch> dispDocument;   
    webBrowser->get_Document(&dispDocument);
    HippoQIPtr<IHTMLDocument2> document(dispDocument);

    if (document) {
        HippoPtr<IHTMLElement> bodyElement;
        document->get_body(&bodyElement);
        HippoQIPtr<IHTMLBodyElement> body(bodyElement);

        if (body)
            body->put_scroll(HippoBSTR(L"no"));
    }

    webBrowser->put_Visible(VARIANT_TRUE);
}

// Show a window offering to share the given URL
void 
HippoUI::showShareWindow(BSTR title, BSTR url)
{
    HippoBSTR shareURL;

    if (!SUCCEEDED (getRemoteURL(HippoBSTR(L"sharelink"), &shareURL)))
        return;

    if (!SUCCEEDED (shareURL.Append(L"?next=close&url=")))
        return;

    wchar_t encoded[1024] = {0}; 
    DWORD len = sizeof(encoded)/sizeof(encoded[0]);

    if (!SUCCEEDED (UrlEscape(url, encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
        return;
    if (!SUCCEEDED (shareURL.Append(encoded)))
        return;

    if (!SUCCEEDED (shareURL.Append(L"&title=")))
        return;

    encoded[0] = 0;
    len = sizeof(encoded)/sizeof(encoded[0]);
    if (!SUCCEEDED (UrlEscape(title, encoded, &len, URL_ESCAPE_UNSAFE | URL_ESCAPE_SEGMENT_ONLY)))
        return;
    if (!SUCCEEDED (shareURL.Append(encoded)))
        return;

    debugLogW(L"sharing URL %s", shareURL);
    HippoPtr<IWebBrowser2> webBrowser;
    showAppletWindow(shareURL, webBrowser);
}

STDMETHODIMP 
HippoUI::BeginFlickrShare(BSTR filePath)
{
    debugLogW(L"sharing photo path %s", filePath);
    flickr_.uploadPhoto(filePath);
    return S_OK;
}

void
HippoUI::showSignInWindow()
{
    HippoBSTR signInURL;
    
    if (!SUCCEEDED (getRemoteURL(HippoBSTR(L"signin?next=close"), &signInURL)))
        return;

    HippoPtr<IWebBrowser2> webBrowser;
    showAppletWindow(signInURL, webBrowser);
}

void
HippoUI::showMenu(UINT buttonFlag)
{
    POINT pt;
    HMENU menu;
    HMENU popupMenu;

    if (buttonFlag == TPM_RIGHTBUTTON && GetAsyncKeyState(VK_CONTROL)) {
        menu = debugMenu_;
    } else {
        updateMenu();
        menu = menu_;
    }

    // We:
    //  - Set the foreground window to our (non-shown) window so that clicking
    //    away elsewhere works
    //  - Send the dummy event to force a context switch to our app
    // See Microsoft knowledgebase Q135788

    GetCursorPos(&pt);
    popupMenu = GetSubMenu(menu, 0);

    SetForegroundWindow(window_);
    TrackPopupMenu(popupMenu, buttonFlag, pt.x, pt.y, 0, window_, NULL);

    PostMessage(window_, WM_NULL, 0, 0);
}

void
HippoUI::launchBrowser(BSTR url, HippoPtr<IWebBrowser2> &webBrowser)
{
    CoCreateInstance(CLSID_InternetExplorer, NULL, CLSCTX_SERVER,
                     IID_IWebBrowser2, (void **)&webBrowser);

    if (!webBrowser) {
        hippoDebug(L"Couldn't create web browser, we lose");
        return;
    }

    debugLogW(L"launching browser for %s", url);

    VARIANT missing;
    missing.vt = VT_EMPTY;

    webBrowser->Navigate(url,
                         &missing, &missing, &missing, &missing);

    webBrowser->put_Visible(VARIANT_TRUE);
}

// Show a window when the user clicks on a shared link
void 
HippoUI::displaySharedLink(BSTR postId)
{
    HippoPtr<IWebBrowser2> webBrowser;
    HippoBSTR targetURL;

    if (!SUCCEEDED (getRemoteURL(HippoBSTR(L"frameset?postId="), &targetURL)))
        return;
    targetURL.Append(postId);

    launchBrowser(targetURL, webBrowser);
    /* Something like the following should activate a explorer bar,
     * (see Q255920) but doesn't seem to work.
     */
#if 0
    HippoBSTR barIDString(L"{A65AC703-C186-4e93-9022-AF8B92C726C8}");
    VARIANT barID;
    barID.vt = VT_BSTR;
    barID.bstrVal = barIDString;
 
    VARIANT show;
    barID.vt = VT_BOOL;
    barID.boolVal = VARIANT_TRUE;

    hr = webBrowser->ShowBrowserBar(&barID, &show, 0);
    if (!SUCCEEDED (hr)) 
        hippoDebug(L"Couldn't show browser bar: %X", hr);
#endif

    WCHAR *postIdW = postId;
    char *postIdU = g_utf16_to_utf8(postIdW, -1, NULL, NULL, NULL);
    debugLogW(L"notifying post clicked: %s", postId);
    im_.notifyPostClickedU(postIdU);
    g_free (postIdU);
}

void
HippoUI::debugLogW(const WCHAR *format, ...)
{
    WCHAR buf[1024];
    va_list vap;
    va_start (vap, format);
    StringCchVPrintfW(buf, sizeof(buf) / sizeof(buf[0]), format, vap);
    va_end (vap);

    logWindow_.logString(buf);
}

void
HippoUI::debugLogU(const char *format, ...)
{
    va_list vap;
    va_start (vap, format);
    char *str = g_strdup_vprintf(format, vap);
    va_end (vap);

    WCHAR *strW = g_utf8_to_utf16(str, -1, NULL, NULL, NULL);
    if (strW) 
        logWindow_.logString(strW);
    
    g_free(str);
    g_free(strW);
}

void 
HippoUI::logError(const WCHAR *text, HRESULT result)
{
    HippoBSTR errstr;
    hippoHresultToString(result, errstr);
    debugLogW(L"%s: %s", text, errstr.m_str);
}

void
HippoUI::logLastError(const WCHAR *text)
{
    logError(text, GetLastError());
}

void 
HippoUI::onAuthFailure()
{
    updateForgetPassword();
    //showSignInWindow();
}

void
HippoUI::onAuthSuccess()
{
    updateForgetPassword();
}

void 
HippoUI::setClientInfo(const char *minVersion,
                       const char *currentVersion,
                       const char *downloadUrl)
{
    upgrader_.setUpgradeInfo(minVersion, currentVersion, downloadUrl);
}

void 
HippoUI::onUpgradeReady()
{
    if (MessageBox(NULL, 
                   L"A new version of the DumbHippo client has been downloaded. Install now?",
                   L"DumbHippo upgrade ready", 
                   MB_OKCANCEL | MB_DEFBUTTON1) == IDOK)
        upgrader_.performUpgrade();
}

void 
HippoUI::onLinkMessage(HippoLinkShare &linkshare)
{
    bubble_.setLinkNotification(linkshare);
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
                                      debug_ ? CLSID_HippoUI_Debug : CLSID_HippoUI,
                                      ACTIVEOBJECT_STRONG, &registerHandle_);
    pHippoUI->Release();

    if (FAILED(hr)) {
        MessageBox(NULL, TEXT("Error registering Dumb Hippo"), NULL, MB_OK);
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
        if (SUCCEEDED (GetActiveObject(debug_ ? CLSID_HippoUI_Debug : CLSID_HippoUI, NULL, &unknown)))
            unknown->QueryInterface<IHippoUI>(&oldUI);

        if (replaceExisting_) {
            if (retryCount-- > 0) {
                if (oldUI)
                    oldUI->Quit();

                goto RETRY_REGISTER;
            }
        } else {
            if (oldUI)
                oldUI->ShowRecent();
        }

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
    if (!debug_) {
        WCHAR commandLine[MAX_PATH];
        GetModuleFileName(instance_, commandLine, sizeof(commandLine) / sizeof(commandLine[0]));
        HippoRegistrar registrar(NULL);
        registrar.registerStartupProgram(L"DumbHippo", commandLine);
    }
}

// We unregister as a startup program when the user selects Exit explicitely
void
HippoUI::unregisterStartup()
{
    if (!debug_) {
        HippoRegistrar registrar(NULL);
        registrar.unregisterStartupProgram(L"DumbHippo");
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

        HippoBSTR messageServer;
        if (SUCCEEDED (preferences_.getMessageServer(&messageServer)))
            SetDlgItemText(preferencesDialog_, IDC_MESSAGE_SERVER, messageServer);

        HippoBSTR webServer;
        if (SUCCEEDED (preferences_.getWebServer(&webServer)))
            SetDlgItemText(preferencesDialog_, IDC_WEB_SERVER, webServer);
    }
    
    updateForgetPassword();
    ShowWindow(preferencesDialog_, SW_SHOW);
}

void 
HippoUI::updateForgetPassword()
{
    if (!preferencesDialog_)
        return;

    HWND forgetPassButton = GetDlgItem(preferencesDialog_, IDC_FORGETPASSWORD);
    if (forgetPassButton)
        EnableWindow(forgetPassButton, im_.hasAuth());
}

gboolean
HippoUI::checkIdle(gpointer data) 
{
    HippoUI *ui = (HippoUI *)data;
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
        if (!ui->idle_) {
            ui->idle_ = TRUE;
            ui->bubble_.setIdle(TRUE);
        }
    } else {
        if (ui->idle_) {
            ui->idle_ = FALSE;
            ui->bubble_.setIdle(FALSE);
        }
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
    HMENU popupMenu = GetSubMenu(menu_, 0);

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

        HippoIM::State state = im_.getState();
        if (state == HippoIM::SIGNED_OUT || state == HippoIM::SIGN_IN_WAIT) {
            info.wID = IDM_SIGN_IN;
            StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign In..."));
        } else {
            info.wID = IDM_SIGN_OUT;
            StringCchCopy(menubuf, sizeof(menubuf) / sizeof(TCHAR), TEXT("Sign Out"));
        }

        info.dwTypeData = menubuf;
            
        InsertMenuItem(popupMenu, pos++, TRUE, &info);
    }
}

// Find the pathname for a local HTML file, based on the location of the .exe
// We could alternatively use res: URIs and embed the HTML files in the
// executable, but this is probably more flexible
HRESULT
HippoUI::getAppletURL(BSTR  filename, 
                      BSTR *url)
{
    HRESULT hr;

    // XXX can theoretically truncate if we have a \?\\foo\bar\...
    // path which isn't limited to the short Windows MAX_PATH
    // Could use dynamic allocation here
    WCHAR baseBuf[MAX_PATH];

    if (!GetModuleFileName(instance_, baseBuf, sizeof(baseBuf) / sizeof(baseBuf[0])))
        return E_FAIL;

    for (size_t i = wcslen(baseBuf); i > 0; i--)
        if (baseBuf[i - 1] == '\\')
            break;

    if (i == 0)  // No \ in path?
        return E_FAIL;

    HippoBSTR path((UINT)i, baseBuf);
    hr = path.Append(L"applets\\");
    if (!SUCCEEDED (hr))
        return hr;

    hr = path.Append(filename);
    if (!SUCCEEDED (hr))
        return hr;

    WCHAR urlBuf[INTERNET_MAX_URL_LENGTH];
    DWORD urlLength = INTERNET_MAX_URL_LENGTH;
    hr = UrlCreateFromPath(path, urlBuf, &urlLength, NULL);
    if (!SUCCEEDED (hr))
        return hr;

    *url = SysAllocString(urlBuf);
    return *url ? S_OK : E_OUTOFMEMORY;
}

// Get the URL of a file on the web server
HRESULT
HippoUI::getRemoteURL(BSTR  appletName, 
                      BSTR *result)
{
    HRESULT hr;
    HippoBSTR webServer;
    HippoBSTR url(L"http://");

    if (!url)
        return E_OUTOFMEMORY;

    hr = preferences_.getWebServer(&webServer);
    if (FAILED (hr))
        return hr;

    hr = url.Append(webServer);
    if (FAILED (hr))
        return hr;

    hr = url.Append(L"/");
    if (FAILED (hr))
        return hr;

    hr = url.Append(appletName);
    if (FAILED (hr))
        return hr;

    return url.CopyTo(result);
}

bool
HippoUI::processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    int wmId, wmEvent;

    // Messages sent from the notification icon
    if (message == notificationIcon_.getMessage())
    {
        notificationIcon_.processMessage(wParam, lParam);
        return true;
    }

    switch (message) 
    {
    case WM_COMMAND:
        wmId    = LOWORD(wParam); 
        wmEvent = HIWORD(wParam);
        if (wmId >= IDM_SHARE0 && wmId <= IDM_SHARE9) {
            UINT i = wmId - IDM_SHARE0;
            if (i < browsers_.length() && browsers_[i].url)
                showShareWindow(browsers_[i].title, browsers_[i].url);
            return true;
        }

        switch (wmId)
        {
        case IDM_SIGN_IN:
            if (im_.signIn())
                showSignInWindow();
            return true;
        case IDM_SIGN_OUT:
            im_.signOut();
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
            ui->im_.forgetAuth();
            ui->updateForgetPassword();
            return TRUE;
        case IDOK:
        {
            WCHAR messageServer[128];
            messageServer[0] = '\0';
            GetDlgItemText(dialog, IDC_MESSAGE_SERVER, 
                           messageServer, sizeof(messageServer) / sizeof(messageServer[0]));
            ui->preferences_.setMessageServer(HippoBSTR(messageServer));

            WCHAR webServer[128];
            webServer[0] = '\0';
            GetDlgItemText(dialog, IDC_WEB_SERVER, 
                           webServer, sizeof(webServer) / sizeof(webServer[0]));
            ui->preferences_.setWebServer(HippoBSTR(webServer));

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

/* Finds all IE and Explorer windows on the system. Needs some refinement
 * to distinguish the two.
 */
#if 0
static void
findExplorerWindows()
{
    HippoPtr<IShellWindows> shellWindows;
    HRESULT hr = CoCreateInstance(CLSID_ShellWindows, NULL, CLSCTX_ALL, IID_IShellWindows, (void **)&shellWindows);
    if (FAILED(hr)) {
        hippoDebug(L"Couldn't create: %x", hr);
        return;
    }

    LONG count;
    shellWindows->get_Count(&count);
    hippoDebug(L"%d", count);
    for (LONG i = 0; i < count; i++) {
        HippoPtr<IDispatch> dispatch;
        VARIANT item;
        item.vt = VT_I4;
        item.intVal = i;
        hr = shellWindows->Item(item, &dispatch);
        if (SUCCEEDED(hr)) {
            HippoQIPtr<IWebBrowser2> browser(dispatch);

            if (browser) {
                HippoBSTR browserURL;
                browser->get_LocationURL(&browserURL);

                if (browserURL)
                    hippoDebug(L"URL: %ls\n", (WCHAR *)browserURL);
            }
        }
    }

}
#endif

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

    if (!GetMessage(&msg, NULL, 0, 0)) {
        Win32Source *win32Source = (Win32Source *)(source);

        win32Source->result = (int)msg.wParam;

        g_main_context_remove_poll (NULL, &win32Source->pollFD);
        g_main_loop_quit(win32Source->loop);
        return FALSE;
    }

    TranslateMessage(&msg);
    DispatchMessage(&msg);

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

GSource *
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

    _wspawnl(_P_NOWAIT, fileBuf, L"DumbHippo", L"--replace", NULL);
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

    static gboolean debug = FALSE;
    static gboolean configFlag = FALSE;
    static gboolean doInstallLaunch = NULL;
    static gboolean replaceExisting = NULL;
    static gboolean initialDebugShare = FALSE;

    char *command_line = GetCommandLineA();
    GError *error = NULL;

    if (!g_shell_parse_argv(command_line, &argc, &argv, &error)) {
        g_printerr("%s\n", error->message);
        return 1;
    }

    static const GOptionEntry entries[] = {
        { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" },
        { "install-launch", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&doInstallLaunch, "Run appropriately at the end of the install" },
        { "replace", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&replaceExisting, "Replace existing instance, if any" },
        { "debug-share", 0, 0, G_OPTION_ARG_NONE, (gpointer)&initialDebugShare, "Show an initial dummy debug share" },
        { NULL }
    };

    g_thread_init(NULL);

    GOptionContext *context = g_option_context_new("The dumbhippo.com notification icon");
    g_option_context_add_main_entries(context, entries, NULL);

    g_option_context_parse(context, &argc, &argv, &error);
    if (error) {
        g_printerr("%s\n", error->message);
        return 1;
    }

    // If run as --install-launch, we rerun ourselves asynchronously, then immediately exit
    if (doInstallLaunch) {
        installLaunch(hInstance);
        return 0;
    }

    // Initialize COM
    CoInitialize(NULL);

    if (!initializeWinSock())
        return 0;

    ui = new HippoUI(debug != FALSE,
                     replaceExisting != FALSE, initialDebugShare != FALSE);
    if (!ui->create(hInstance))
        return 0;

    loop = g_main_loop_new(NULL, FALSE);

    source = win32SourceNew(loop);
    g_source_attach(source, NULL);

    g_main_loop_run(loop);

    result = ((Win32Source *)source)->result;
    g_source_unref(source);

    ui->destroy();
    ui->Release();

    WSACleanup();
    CoUninitialize();

    return result;
}
