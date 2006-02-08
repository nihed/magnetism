/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include <stdarg.h>
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "HippoUI.h"

static const int BASE_WIDTH = 400;
static const int BASE_HEIGHT = 150;
static const UINT_PTR CHECK_MOUSE = 1;

HippoBubble::HippoBubble(void)
{
    refCount_ = 1;
    idle_ = FALSE;
    screenSaverRunning_ = FALSE;
    haveMouse_ = FALSE;
    effectiveIdle_ = FALSE;
    shown_ = FALSE;
    viewerSpace_ = 0;

    setWindowStyle(WS_POPUP);
    setExtendedStyle(WS_EX_TOPMOST);
    setClassName(L"HippoBubbleClass");
    setTitle(L"Hippo Notification");
    setApplication(this);

    hippoLoadTypeInfo(L"HippoUtil.dll", &IID_IHippoBubble, &ifaceTypeInfo_, NULL);
}

HippoBubble::~HippoBubble(void)
{
}

void
HippoBubble::moveResizeWindow() 
{
    int width = BASE_WIDTH;
    int height = BASE_HEIGHT + viewerSpace_;

    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    moveResize(desktopRect.right - width, (desktopRect.bottom - height),
               width, height);
}

HippoBSTR
HippoBubble::getURL()
{
    HippoBSTR srcURL;

    ui_->getAppletURL(L"notification.xml", &srcURL);

    return srcURL;
}

void
HippoBubble::initializeWindow()
{
    moveResizeWindow();
}

void 
HippoBubble::initializeIE()
{
    ie_->setThreeDBorder(true);

    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    HippoBSTR styleURL;
    ui_->getAppletURL(L"clientstyle.xml", &styleURL);
    ie_->setXsltTransform(styleURL, L"appleturl", appletURL.m_str, NULL);
}

void 
HippoBubble::initializeBrowser()
{
    // Kind of a hack
    HippoBSTR serverURL;
    ui_->getRemoteURL(HippoBSTR(L""), &serverURL);
    HippoBSTR appletURL;
    ui_->getAppletURL(HippoBSTR(L""), &appletURL);
    HippoBSTR selfID;
    ui_->GetLoginId(&selfID);

    ui_->debugLogU("Invoking dhInit");
    ie_->createInvocation(L"dhInit")
        .add(serverURL)
        .add(appletURL)
        .add(selfID)
        .run();

    // Set the initial value of the idle state
    doSetIdle();
}
    
void 
HippoBubble::onClose(bool fromScript)
{
    Close();
}

void 
HippoBubble::setLinkNotification(HippoLinkShare &share)
{
    if (!create())
        return;

    std::vector<HippoBSTR> personRecipients;
    for (unsigned long i = 0; i < share.personRecipients.length(); i++) {
        personRecipients.push_back(share.personRecipients[i].id);
        personRecipients.push_back(share.personRecipients[i].name);
    }

    std::vector<HippoBSTR> groupRecipients;
    for (unsigned long i = 0; i < share.groupRecipients.length(); i++) {
        groupRecipients.push_back(share.groupRecipients[i]);
    }

    std::vector<HippoBSTR> viewers;
    for (unsigned long i = 0; i < share.viewers.length(); i++) {
        viewers.push_back(share.viewers[i].id);
        viewers.push_back(share.viewers[i].name);
    }

    variant_t result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    // Note if you change the arguments to this function, you must change notification.js
    ie_->createInvocation(L"dhAddLinkShare")
        .add(share.senderName)
        .add(share.senderId)
        .add(share.senderPhotoUrl)
        .add(share.postId)
        .add(share.title)
        .add(share.url)
        .add(share.description)
        .addStringVector(personRecipients)
        .addStringVector(groupRecipients)
        .addStringVector(viewers)
        .add(share.info)
        .addLong(share.timeout)
        .getResult(&result);

    if (result.vt != VT_BOOL) {
        ui_->debugLogU("dhAddLinkShare returned invalid type");
        return;
    }
    // Only show the bubble both if dhAddLinkShare says we should,
    // and the share isn't active elsewhere (e.g. in a browser frame)
    if (!result.boolVal) {
        ui_->debugLogU("dhAddLinkShare returned false");
        return;
    }
    if (ui_->isShareActive(share.postId)) {
        ui_->debugLogW(L"chat is active for postId %s, not showing", share.postId);
        return;
    }
    setShown();
}

void 
HippoBubble::addMySpaceCommentNotification(long myId, long blogId, HippoMySpaceBlogComment &comment)
{
    if (!create())
        return;

    ui_->debugLogW(L"Invoking dhAddMySpaceComment");

    // Note if you change the arguments to this function, you must change
    // notification.js
    ie_->createInvocation(L"dhAddMySpaceComment")
        .addLong(myId)
        .addLong(blogId)
        .addLong(comment.commentId)
        .addLong(comment.posterId)
        .add(comment.posterName)
        .add(comment.posterImgUrl)
        .add(comment.content)
        .run();

    setShown();
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
        ie_->createInvocation(L"dhSetIdle")
            .addBool(effectiveIdle_)
            .run();
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

void
HippoBubble::doShow(void) 
{   
    show();

    SetTimer(window_, CHECK_MOUSE, 250 /* 0.25 second */, NULL);
    checkMouse();
}

void 
HippoBubble::showMissedBubbles()
{
    ie_->createInvocation(L"dhDisplayMissed").run();
    setShown();
}

void
HippoBubble::setShown(void) 
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
    if (!screenSaverRunning_)
        setAnimate(true);
    hide();
    if (screenSaverRunning_)
        setAnimate(false);
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
