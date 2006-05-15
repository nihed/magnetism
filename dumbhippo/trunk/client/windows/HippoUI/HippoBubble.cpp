/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include <stdarg.h>
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "HippoUI.h"
#include "HippoComWrappers.h"

#include "cleangdiplus.h"

// These values basically don't matter, since the bubble picks its own
// size, but matching the values from bubble.js may save a bit on resizing
static const int BASE_WIDTH = 450;
static const int BASE_HEIGHT = 123;
static const int SWARM_HEIGHT = 180;
static const UINT_PTR CHECK_MOUSE = 1;

HippoBubble::HippoBubble(void)
{
    idle_ = FALSE;
    screenSaverRunning_ = FALSE;
    haveMouse_ = FALSE;
    effectiveIdle_ = FALSE;
    shown_ = FALSE;
    desiredWidth_ = BASE_WIDTH;
    desiredHeight_ = BASE_HEIGHT;
    layerDC_ = NULL;
    oldBitmap_ = NULL;
 
    // Notes on animation
    // 
    // * Without our usage of UpdateLayeredWindow, it would fade the window in and
    // out using AnimateWindow, but you turn on fading in then you'll probably have difficulties 
    // the second time you show the window ... it appears that the web browser control has some 
    // bugs with WM_PRINTCLIENT; the first time the contents of the window are properly
    // initialized before fade-in, but on subsequent shows they are not. A crude
    // workaround might be to reembed a new control every time, but there are probably
    // less sledgehammer methods. Even without animation there are sometimes problems
    // with reshowing the window, which is why we turn on updateOnShow(). (This may
    // no longer be necessary with our usage of UpdateLayeredWindow())
    //
    // * AnimateWindow isn't compatible with UpdateLayeredWindow() ... when you start
    // animating the window, the shape is removed. This is presumably because AnimateWindow
    // internally uses SetLayeredWindowAttributes, which is exclusive with UpdateLayeredWindow
    // So to animate a shaped window, we'd have to fade the bits in and out ourselves.
    //
    setAnimate(false);
    setUseParent(true);
    setUpdateOnShow(true);
    setWindowStyle(WS_POPUP);
    setExtendedStyle(WS_EX_TOPMOST | WS_EX_LAYERED | WS_EX_NOACTIVATE);
    setClassName(L"HippoBubbleClass");
    setTitle(L"Hippo Notification");
    setApplication(this);
}

HippoBubble::~HippoBubble(void)
{
    disconnectAllPosts();

    if (layerDC_) {
        // We have to select the original bitmap back into the DC before freeing
        // it or we'll leak the last bitmap we installed
        if (oldBitmap_)
            SelectObject(layerDC_, oldBitmap_);

        DeleteDC(layerDC_);
    }
}

ITypeInfo *
HippoBubble::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;
    if (!typeInfo)
        hippoLoadTypeInfo((WCHAR *)0, &IID_IHippoBubble, &typeInfo, NULL);

    return typeInfo;
}

void
HippoBubble::moveResizeWindow() 
{
    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    moveResize(desktopRect.right - desiredWidth_, (desktopRect.bottom - desiredHeight_) - 5,
               desiredWidth_, desiredHeight_);
}

HippoBSTR
HippoBubble::getURL()
{
    HippoBSTR srcURL;

    ui_->getAppletURL(L"notification.xml", &srcURL);

    return srcURL;
}

void
HippoBubble::initializeUI()
{
    postAdded_.connect(G_OBJECT(ui_->getDataCache()), "post-added",
        slot(this, &HippoBubble::onPostAdded));
    chatRoomLoaded_.connect(G_OBJECT(ui_->getDataCache()), "chat-room-loaded",
        slot(this, &HippoBubble::onChatRoomLoaded));
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

bool 
HippoBubble::processMessage(UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    if (message == WM_TIMER && wParam == CHECK_MOUSE) {
        checkMouse();
        return true;
    }

    return HippoAbstractWindow::processMessage(message, wParam, lParam);
}

void 
HippoBubble::setLinkNotification(bool isRedisplay, HippoPost *share)
{
    // watch future changes to this post
    connectPost(share);

    if (!create())
        return;

    variant_t result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    // Note if you change the arguments to this function, you must change notification.js
    ie_->createInvocation(L"dhAddLinkShare")
        .addBool(isRedisplay)
        .addDispatch(HippoPostWrapper::getWrapper(share, ui_->getDataCache()))
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
    if (ui_->isShareActive(share)) {
        ui_->debugLogU("chat is active for postId %s, not showing", hippo_post_get_guid(share));
        return;
    }
    setShown();
}

void 
HippoBubble::addMySpaceCommentNotification(long myId, long blogId, const HippoMySpaceCommentData &comment)
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
    show(FALSE);

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
HippoBubble::onUserJoined(HippoPerson *person,
                          HippoPost   *post)
{
    if (ui_->isShareActive(post)) // Already have a window open
        return;

    if (!ie_) {
        if (!create())
            return;
    }

    ie_->createInvocation(L"dhViewerJoined")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .run();

    setShown();
}

void 
HippoBubble::onMessageAdded(HippoChatMessage *message,
                            HippoPost        *post)
{
    if (ui_->isShareActive(post)) // Already have a window open
        return;

    if (!ie_) {
        if (!create())
            return;
    }

    ie_->createInvocation(L"dhChatRoomMessage")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .run();

    setShown();
}

void 
HippoBubble::onPostChanged(HippoPost *post)
{
    if (!ie_)
        return;

    ie_->createInvocation(L"dhUpdatePost")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .run();
}

void
HippoBubble::onPostAdded(HippoPost *post)
{
    if (hippo_post_get_new(post)) {
        hippoDebugLogU("New post %s needs bubbling up",
            hippo_post_get_guid(post));
        hippo_post_set_new(post, FALSE);
        setLinkNotification(false, post);
    } else {
        hippoDebugLogU("Post %s is not new, not bubbling up for now",
            hippo_post_get_guid(post));
    }
}

void
HippoBubble::onChatRoomLoaded(HippoChatRoom *room)
{
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_POST) {
        HippoDataCache *cache = ui_->getDataCache();
        HippoPost *post = hippo_data_cache_lookup_post(ui_->getDataCache(), hippo_chat_room_get_id(room));

        GConnection1<void,HippoPerson*>::named_connect(G_OBJECT(room), "hippo-bubble-user-joined",
            "user-joined", bind(slot(this, &HippoBubble::onUserJoined), post));
        GConnection1<void,HippoChatMessage*>::named_connect(G_OBJECT(room), "hippo-bubble-message-added",
            "message-added", bind(slot(this, &HippoBubble::onMessageAdded), post));
    }
}

void
HippoBubble::connectPost(HippoPost *post)
{
    hippoDebugLogU("Bubble connecting post %s",
            hippo_post_get_guid(post));

    std::set<HippoPost*>::iterator i = connectedPosts_.find(post);
    if (i != connectedPosts_.end()) {
        hippoDebugLogU("Post was already connected");
        return;
    }

    GConnection0<void>::named_connect(G_OBJECT(post), "hippo-bubble-changed",
        "changed", bind(slot(this, &HippoBubble::onPostChanged), post));

    g_object_ref(post);
    connectedPosts_.insert(post);
}

void
HippoBubble::disconnectPost(HippoPost *post)
{
    hippoDebugLogU("Bubble disconnecting post %s",
            hippo_post_get_guid(post));

    std::set<HippoPost*>::iterator i = connectedPosts_.find(post);
    if (i != connectedPosts_.end()) {
        GConnection::named_disconnect(G_OBJECT(post), "hippo-bubble-changed");

        HippoChatRoom *room = hippo_post_get_chat_room(post);
        GConnection::named_disconnect(G_OBJECT(room), "hippo-bubble-user-joined");
        GConnection::named_disconnect(G_OBJECT(room), "hippo-bubble-message-added");
        
        connectedPosts_.erase(i);
        g_object_unref(post);
    } else {
        hippoDebugLogU("Post wasn't connected anyhow");
    }
}

void
HippoBubble::disconnectAllPosts()
{
    std::set<HippoPost*>::iterator i;
    while ((i = connectedPosts_.begin()) != connectedPosts_.end()) {
        disconnectPost(*i);
    }
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

    // See comments in the constructor for why we'd want to only fade out, not in 
    // and why we aren't turning on fading at all at the moment
    // if (!screenSaverRunning_)
    //    setAnimate(true);
    hide();
    // if (!screenSaverRunning_)
    //    setAnimate(false);
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
HippoBubble::Resize(int width, int height)
{
    if (width != desiredWidth_ || height != desiredHeight_) {
        desiredWidth_ = width;
        desiredHeight_ = height;
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

STDMETHODIMP 
HippoBubble::UpdateDisplay()
{
    bool firstTime = layerDC_ == NULL;

    if (firstTime)
        layerDC_ = CreateCompatibleDC(NULL);

    int width = desiredWidth_;
    int height = desiredHeight_;

    BITMAPINFO bitmapInfo;

    ZeroMemory(&bitmapInfo, sizeof(BITMAPINFO));
    bitmapInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bitmapInfo.bmiHeader.biWidth = width;
    bitmapInfo.bmiHeader.biHeight = height;
    bitmapInfo.bmiHeader.biSizeImage = 4 * width * height;
    bitmapInfo.bmiHeader.biPlanes = 1;
    bitmapInfo.bmiHeader.biBitCount = 32;
    bitmapInfo.bmiHeader.biCompression = BI_RGB;

    void *bits;
    HBITMAP bitmap = CreateDIBSection(layerDC_, &bitmapInfo, DIB_RGB_COLORS, &bits, NULL, 0);

    HBITMAP lastBitmap = (HBITMAP)SelectObject(layerDC_, bitmap);
    if (!lastBitmap) {
        hippoDebugLastErr(L"Can't select bitmap into DC");
        return E_FAIL;
    }

    if (firstTime)
        oldBitmap_ = lastBitmap;

    HRESULT hr;

    IWebBrowser2 *browser = ie_->getBrowser();
    HippoPtr<IDispatch> dispatch;
    hr = browser->get_Document(&dispatch);
    if (FAILED(hr))
        return hr;

    // Using IViewObject works better than IHTMLElementRender; IHTMLElementRender
    // doesn't handle, for example, alpha-blended PNGs, probably because it 
    // is intended for printing, where alpha-blending wouldn't work
    HippoQIPtr<IViewObject> viewObject = dispatch;
    if (!viewObject)
        return E_FAIL;

    RECTL bounds = { 0, 0, width, height };
    hr = viewObject->Draw(DVASPECT_CONTENT, 1, NULL, NULL, NULL, layerDC_, &bounds, NULL, NULL, 0);
    if (FAILED(hr))
        return hr;

    hippoDebugLogU("redrawing layered window, height=%d width=%d", height, width);

    // This makes the entire image default to opaque
    unsigned char *p = (unsigned char *)bits;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            p[3] = 0xff;
            p += 4;
        }
    }

    p = (unsigned char *)bits;

    // Black means transparent - go over top right and bottom right corners
    int radius = 3;
    for (int i = 0; i < radius; i++) {
        for (int j = 0; j < radius; j++) {
            int x = (width-i);
            int y = (height-j);
            unsigned char *pos = (p + (width*y + x)*4);
            if (pos[0] == 0 && pos[1] == 0 && pos[2] == 0)
                pos[3] = 0;
        }
    }
    radius = 20;
    for (int i = 0; i < radius; i++) {
        for (int j = 0; j < radius; j++) {
            int x = (width-i);
            int y = j;
            unsigned char *pos = (p + (width*y + x)*4);
            if (pos[0] == 0 && pos[1] == 0 && pos[2] == 0)
                pos[3] = 0;
        }
    }

    HippoBSTR maskPath;
    ui_->getAppletPath(height == BASE_HEIGHT ? L"bubbleNavMask.png" : L"bubbleNavMaskSwarm.png", &maskPath);
    Gdiplus::Bitmap navMask(maskPath.m_str, false);
    Gdiplus::Status st = navMask.GetLastStatus();

    UINT navWidth = navMask.GetWidth();
    UINT navHeight = navMask.GetHeight();
    for (UINT i = 0; i < navWidth; i++) {
        for (UINT j = 0; j < navHeight; j++) {
            Gdiplus::Color pixel;
            // GDI+ bitmap coordinates differ from GDI bitmaps
            UINT y = (navHeight - j) - 1;
            navMask.GetPixel(i, y, &pixel);
            // We use blue as a mask
            UINT alpha = pixel.GetAlpha();
            unsigned char *pos = (p + (width*j + i)*4);
            if (pixel.GetBlue() != 255) {
                pos[0] = pixel.GetBlue();
                pos[1] = pixel.GetGreen();
                pos[2] = pixel.GetRed();
                pos[3] = pixel.GetAlpha();
            } else if (alpha == 0) {
                pos[0] = pos[1] = pos[2] = pos[3] = 0;
            }
        }
    }

    POINT srcPoint = { 0, 0 };
    SIZE size = { width, height };
    
    BLENDFUNCTION blend;
    blend.BlendOp = AC_SRC_OVER;
    blend.BlendFlags = 0;
    blend.SourceConstantAlpha = 0xff;
    blend.AlphaFormat = AC_SRC_ALPHA;

    // If AnimateWindow() was used to fade the window out, we'd need to force the WS_EX_LAYERED back into 
    // the extended attributes. But that's incompatible with the shape anyways so there isn't much point
    // in using it
    // SetWindowLong(window_, GWL_EXSTYLE, extendedStyle_);
    if (!UpdateLayeredWindow(window_, NULL, NULL, &size, layerDC_, &srcPoint, NULL, &blend, ULW_ALPHA)) {
        hippoDebugLogW(L"Can't update layered window");
        // Ignore the failure, if we return an error, the user will get a Javascript dialog
    }

    // The bitmap will be kept referenced by layerDC_, we can drop our reference now
    DeleteObject(bitmap);

    return S_OK;
}
