/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include <mshtml.h>
#include "exdisp.h"
#include <strsafe.h>
#include <stdarg.h>
#include <ExDispid.h>
#include "HippoUI.h"
#include "HippoIE.h"
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "Guid.h"

static const TCHAR *CLASS_NAME = TEXT("HippoBubbleClass");
static const int BASE_WIDTH = 400;
static const int BASE_HEIGHT = 150;
static const UINT_PTR CHECK_MOUSE = 1;

#define NOTIMPLEMENTED assert(0); return E_NOTIMPL

HippoBubble::HippoBubble(void)
{
    refCount_ = 1;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    idle_ = FALSE;
    screenSaverRunning_ = FALSE;
    haveMouse_ = FALSE;
    effectiveIdle_ = FALSE;
    shown_ = FALSE;
    viewerSpace_ = 0;
    ie_ = NULL;

    ieCallback_ = new HippoBubbleIECallback(this);

    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoBubble, &ifaceTypeInfo_, NULL);
}

HippoBubble::~HippoBubble(void)
{
    delete ieCallback_;
}

void 
HippoBubble::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoBubble::createWindow(void)
{
    // We need to set the parent here, even though it is invisible, to keep the
    // window from showing on the taskbar. If there is no parent, then it shows
    // up even when WS_EX_APPWINDOW isn't set.
    window_ = CreateWindowEx(WS_EX_TOPMOST, CLASS_NAME, L"Hippo Notification", WS_POPUP,
                             CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                             ui_->getWindow(), NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    moveResizeWindow();

    hippoSetWindowData<HippoBubble>(window_, this);

    return true;
}

void
HippoBubble::moveResizeWindow() 
{
    int width = BASE_WIDTH;
    int height = BASE_HEIGHT + viewerSpace_;

    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    MoveWindow(window_, 
               (desktopRect.right - width), (desktopRect.bottom - height), 
               width, height, 
               TRUE);

    if (ie_) {
        RECT rect;

        rect.top = 0;
        rect.left = 0;
        rect.bottom = height;
        rect.right = width;

        ie_->resize(&rect);
    }
}

void HippoBubble::HippoBubbleIECallback::onDocumentComplete()
{
    bubble_->ui_->debugLogW(L"HippoBubble document complete");
}

void
HippoBubble::HippoBubbleIECallback::onError(WCHAR *text) 
{
    bubble_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoBubble::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    HippoBSTR srcURL;
    ui_->getAppletURL(L"notification.xml", &srcURL);
    ie_ = new HippoIE(ui_, window_, srcURL, ieCallback_, this);

    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    HippoBSTR styleURL;
    ui_->getAppletURL(L"clientstyle.xml", &styleURL);
    ie_->setXsltTransform(styleURL, L"appleturl", appletURL.m_str, NULL);
    ie_->create();
    browser_ = ie_->getBrowser();

    // Kind of a hack
    HippoBSTR serverURLStr;
    ui_->getRemoteURL(HippoBSTR(L""), &serverURLStr);
    HippoBSTR appletURLStr;
    ui_->getAppletURL(HippoBSTR(L""), &appletURLStr);
    HippoBSTR selfIdStr;
    ui_->GetLoginId(&selfIdStr);
    variant_t serverUrl(serverURLStr.m_str);
    variant_t appletUrl(appletURLStr.m_str);
    variant_t selfId(selfIdStr.m_str);
    variant_t result;
    ui_->debugLogU("dhInit being invoked");
    ie_->invokeJavascript(L"dhInit", &result, 3, &serverUrl, &appletUrl, &selfId);

    // Set the initial value of the idle state
    doSetIdle();

    return true;
}

bool
HippoBubble::create(void)
{
    if (window_ != NULL) {
        return true;
    }
    if (!registerClass()) {
        ui_->debugLogW(L"Failed to register window class");
        return false;
    }
    if (!createWindow()) {
        ui_->debugLogW(L"Failed to create window");
        return false;
    }
    if (!embedIE()) {
        ui_->debugLogW(L"Failed to embed IE");
        return false;
    }
    return true;
}

static SAFEARRAY *
hippoStrArrayToSafeArray(HippoArray<HippoBSTR> &args)
{
    // I swear the SAFEARRAY API was *designed* to be painful
    SAFEARRAYBOUND dim[1];
    dim[0].lLbound= 0;
    dim[0].cElements = args.length();
    SAFEARRAY *ret = SafeArrayCreate(VT_VARIANT, 1, dim);
    for (unsigned int i = 0; i < args.length(); i++) {
        VARIANT *data;
        VARIANT argv;
        SafeArrayAccessData(ret, (void**)&data);
        argv.vt = VT_BSTR;
        argv.bstrVal = args[i];
        VariantCopy(&(data[i]), &argv);
        SafeArrayUnaccessData(ret);
    }
    return ret;
}

static SAFEARRAY *
hippoLinkRecipientArrayToSafeArray(HippoArray<HippoLinkRecipient> &args)
{
    SAFEARRAYBOUND dim[1];
    dim[0].lLbound= 0;
    dim[0].cElements = args.length();
    SAFEARRAY *ret = SafeArrayCreate(VT_VARIANT, 1, dim);
    for (unsigned int i = 0; i < args.length(); i++) {
        VARIANT *data;

        SAFEARRAYBOUND subdim[1];
        subdim[0].lLbound = 0;
        subdim[0].cElements = 2;
        SAFEARRAY *val = SafeArrayCreate(VT_VARIANT, 1, subdim);
        variant_t vId(args[i].id);
        variant_t vName(args[i].name);
        SafeArrayAccessData(val, (void**)&data);
        VariantCopy(&(data[0]), &vId);
        VariantCopy(&(data[1]), &vName);
        SafeArrayUnaccessData(val);

        VARIANT vArray;
        vArray.vt = VT_ARRAY | VT_VARIANT;
        vArray.parray = val;
        SafeArrayAccessData(ret, (void**)&data);
        VariantCopy(&(data[i]), &vArray);
        SafeArrayUnaccessData(ret);
    }
    return ret;
}

void 
HippoBubble::setLinkNotification(HippoLinkShare &share)
{
    if (window_ == NULL) {
        ui_->debugLogW(L"Creating new window");
        if (!create()) {
            ui_->debugLogW(L"Failed to create window");
            return;
        }
    }

    variant_t senderName(share.senderName);
    variant_t senderId(share.senderId);
    variant_t senderPhotoUrl(share.senderPhotoUrl);
    variant_t postId(share.postId);
    variant_t linkTitle(share.title);
    variant_t linkURL(share.url);
    variant_t linkDescription(share.description);
    SAFEARRAY *personRecipients = hippoLinkRecipientArrayToSafeArray(share.personRecipients);
    VARIANT personRecipientsArg;
    personRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    personRecipientsArg.parray = personRecipients;
    SAFEARRAY *groupRecipients = hippoStrArrayToSafeArray(share.groupRecipients);
    VARIANT groupRecipientsArg;
    groupRecipientsArg.vt = VT_ARRAY | VT_VARIANT;
    groupRecipientsArg.parray = groupRecipients;
    SAFEARRAY *viewers = hippoLinkRecipientArrayToSafeArray(share.viewers);
    VARIANT viewersArg;
    viewersArg.vt = VT_ARRAY | VT_VARIANT;
    viewersArg.parray = viewers;
    variant_t infoArg(share.info);
    variant_t timeout(share.timeout);

    variant_t result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    // Note if you change the arguments to this function, you must change
    // notification.js (and don't forget to update the argument count here too)
    invokeJavascript(L"dhAddLinkShare", &result, 12, &senderName,
                     &senderId, &senderPhotoUrl, &postId, &linkTitle, &linkURL, &linkDescription,
                     &personRecipientsArg, &groupRecipientsArg, &viewersArg, &infoArg, &timeout);
    SafeArrayDestroy(personRecipients);
    SafeArrayDestroy(groupRecipients);

    if (!ui_->isChatWindowActive(share.postId))
        show();
    else
        ui_->debugLogW(L"chat is active for postId %s, not showing", share.postId);
}

void 
HippoBubble::addMySpaceCommentNotification(long myId, long blogId, HippoMySpaceBlogComment &comment)
{
    if (window_ == NULL) {
        ui_->debugLogW(L"Creating new window");
        if (!create()) {
            ui_->debugLogW(L"Failed to create window");
            return;
        }
    }
    variant_t vMyId(myId);
    variant_t vBlogId(blogId);
    variant_t vCommentId(comment.commentId);
    variant_t vPosterId(comment.posterId);
    variant_t vPosterName(comment.posterName.m_str);
    variant_t vPosterImgUrl(comment.posterImgUrl.m_str);
    variant_t vContent(_bstr_t(comment.content.m_str));
    ui_->debugLogW(L"Invoking dhAddMySpaceComment");
    // Note if you change the arguments to this function, you must change
    // notification.js (and don't forget to update the argument count here too)
    invokeJavascript(L"dhAddMySpaceComment", NULL, 7, &vMyId, &vBlogId, &vCommentId, &vPosterId, &vPosterName, &vPosterImgUrl, &vContent);
    show();
}

bool
HippoBubble::invokeJavascript(WCHAR *funcName, VARIANT *invokeResult, int nargs, ...)
{
    va_list args;
    va_start (args, nargs);
    HRESULT result = ie_->invokeJavascript(funcName, invokeResult, nargs, args);
    bool ret = SUCCEEDED(result);
    if (!ret)
        ui_->logError(L"failed to invoke javascript", result);
    va_end (args);
    return ret;
}

void 
HippoBubble::setIdle(bool idle)
{
    idle_ = idle;
    updateIdle();
}

void
HippoBubble::updateIdle()
{
    bool effectiveIdle = idle_ || haveMouse_;
    if (effectiveIdle_ != effectiveIdle) {
        effectiveIdle_ = effectiveIdle;
        doSetIdle();
    }
}

void
HippoBubble::doSetIdle()
{
    if (window_) {
        variant_t idleVariant(effectiveIdle_);
        variant_t result;
    
        ie_->invokeJavascript(L"dhSetIdle", &result, 1, &idleVariant);
    }
}

void 
HippoBubble::setScreenSaverRunning(bool screenSaverRunning)
{
    screenSaverRunning_ = screenSaverRunning;
    if (shown_) {
        if (!screenSaverRunning_)
            doShow();
        else
            doClose();
    }
}

bool
HippoBubble::registerClass()
{
    WNDCLASSEX wcex;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));
    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = CLASS_NAME;

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, CLASS_NAME, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoBubble::doShow(void) 
{   
    ui_->debugLogW(L"doing bubble show");
    //if (!AnimateWindow(window_, 400, AW_BLEND))
    //  ui_->logLastError(L"Failed to invoke AnimateWindow");
    if (!ShowWindow(window_, SW_SHOW))
        ui_->logLastError(L"Failed to invoke ShowWindow");
    if (!RedrawWindow(window_, NULL, NULL, RDW_UPDATENOW))
        ui_->logLastError(L"Failed to invoke RedrawWindow");
    if (!BringWindowToTop(window_))
        ui_->logLastError(L"Failed to invoke BringWindowToTop");

    SetTimer(window_, CHECK_MOUSE, 250 /* 0.25 second */, NULL);
    checkMouse();
}

void 
HippoBubble::showMissedBubbles()
{
    invokeJavascript(L"dhDisplayMissed", NULL, 0);
    show();
}

void
HippoBubble::show(void) 
{   
    // If we show the bubble when the screensaver is running, it will pop up
    // over the screensaver, so we simply remember that the bubble is logically
    // shown and actually show it when the screensaver deactivates. Our assumption
    // here is that the screensaver only activates when the user is idle, so we
    // don't have to worry about explicitely stopping paging in the bubble; 
    // paging doesn't happen when the user is idle anyways.
    shown_ = TRUE;
    if (!screenSaverRunning_)
        doShow();
}

/* We want to detect when the mouse pointer is over the bubble; this
 * is used to suspend the handling of the paging timeout; if
 * the mouse is over the bubble we keep the bubble up indefinitely
 * to let the user interact with it. As far as I know there is no
 * way to detect enters and leaves that go directly to a subwindow
 * (virtual enters/leaves in X terminology), so we check by simply
 * installing a timeout when the bubble is up.
 */
void
HippoBubble::checkMouse()
{
    POINT pt;
    RECT rect;

    GetCursorPos(&pt);
    ScreenToClient(window_, &pt);
    GetClientRect(window_, &rect);

    if (pt.x > 0 && pt.x < rect.right &&
        pt.y > 0 && pt.y < rect.bottom) 
    {
        if (!haveMouse_) {
            haveMouse_ = TRUE;
            updateIdle();
        }
    } 
    else
    {
        if (haveMouse_) {
            haveMouse_ = FALSE;
            updateIdle();
        }
    }
}

bool
HippoBubble::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    switch (message) 
    {
    case WM_TIMER:
        if (wParam == CHECK_MOUSE) {
            checkMouse();
            return true;
        } else {
            return false;
        }
    case WM_CLOSE:
        Close();
        return true;
    default:
        return false;
    }
}

LRESULT CALLBACK 
HippoBubble::windowProc(HWND   window,
                        UINT   message,
                        WPARAM wParam,
                        LPARAM lParam)
{
    HippoBubble *bubbleWindow = hippoGetWindowData<HippoBubble>(window);
    if (bubbleWindow) {
        if (bubbleWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}

// IHippoBubble

STDMETHODIMP
HippoBubble::DebugLog(BSTR str)
{
    ui_->debugLogW(L"%s", str);
    return S_OK;
}

STDMETHODIMP 
HippoBubble::DisplaySharedLink(BSTR linkId, BSTR url)
{
    ui_->displaySharedLink(linkId, url);
    return S_OK;
}

STDMETHODIMP
HippoBubble::OpenExternalURL(BSTR url)
{
    ui_->launchBrowser(url);
    return S_OK;
}

STDMETHODIMP
HippoBubble::GetServerBaseUrl(BSTR *ret)
{
    HippoBSTR temp;
    ui_->getRemoteURL(L"", &temp);

    temp.CopyTo(ret);
    return S_OK;
}

void
HippoBubble::doClose()
{
    KillTimer(window_, CHECK_MOUSE);
    if (haveMouse_) {
        haveMouse_ = FALSE;
        updateIdle();
    }

    // Don't animate if the screenSaver is starting
    if (screenSaverRunning_)
        ShowWindow(window_, SW_HIDE);
    else
        AnimateWindow(window_, 200, AW_BLEND | AW_HIDE);
}

STDMETHODIMP
HippoBubble::Close()
{
    ui_->debugLogU("closing link notification");

    shown_ = FALSE;

    if (!screenSaverRunning_)
        doClose();

    return S_OK;
}

STDMETHODIMP 
HippoBubble::SetViewerSpace(DWORD viewerSpace)
{
    if (viewerSpace != viewerSpace_) {
        viewerSpace_ = viewerSpace;
        if (window_)
            moveResizeWindow();
    }

    return S_OK;
}

STDMETHODIMP 
HippoBubble::SetHaveMissedBubbles(BOOL haveMissed)
{
    ui_->setHaveMissedBubbles(!!haveMissed);
    return S_OK;
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoBubble::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoBubble*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoBubble)) 
        *result = static_cast<IHippoBubble *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoBubble)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

    STDMETHODIMP
HippoBubble::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoBubble::GetTypeInfo(UINT        iTInfo,
                         LCID        lcid,
                         ITypeInfo **ppTInfo)
{
    if (ppTInfo == NULL)
        return E_INVALIDARG;
    if (!ifaceTypeInfo_)
        return E_OUTOFMEMORY;
    if (iTInfo != 0)
        return DISP_E_BADINDEX;

    ifaceTypeInfo_->AddRef();
    *ppTInfo = ifaceTypeInfo_;

    return S_OK;
}
        
STDMETHODIMP 
HippoBubble::GetIDsOfNames (REFIID    riid,
                            LPOLESTR *rgszNames,
                            UINT      cNames,
                            LCID      lcid,
                            DISPID   *rgDispId)
{
    HRESULT ret;
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    
    ret = DispGetIDsOfNames(ifaceTypeInfo_, rgszNames, cNames, rgDispId);
    return ret;
}
        
STDMETHODIMP
HippoBubble::Invoke (DISPID        member,
                     const IID    &iid,
                     LCID          lcid,              
                     WORD          flags,
                     DISPPARAMS   *dispParams,
                     VARIANT      *result,
                     EXCEPINFO    *excepInfo,  
                     unsigned int *argErr)
{
    if (!ifaceTypeInfo_) 
        return E_OUTOFMEMORY;
    HippoQIPtr<IHippoBubble> hippoBubble(static_cast<IHippoBubble *>(this));
    HRESULT hr = DispInvoke(hippoBubble, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
