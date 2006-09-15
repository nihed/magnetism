/* HippoBubble.cpp: Display notifications
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include <stdarg.h>
#include <HippoUtil.h>
#include "HippoBubble.h"
#include "HippoUI.h"
#include "HippoComWrappers.h"

#include "cleangdiplus.h"

// A simple image buffer class; the data it stores is in 32-bpp premultipled
// ARGB. GDI+ is used for PNG loading; after loading the data, we free the 
// Gdiplus::Bitmap structures and work solely with the pixel data  to keep things 
// simple. (We need to go straight to the pixel data anyways to do the combining 
// we want to do, and combining GDI+ with direct pixel access is tricky.)

static const UINT EMPTY_BUFFER[] = { 0 };

class HippoImageBuffer {
public:
    HippoImageBuffer();
    ~HippoImageBuffer();

    void load(HippoUI *ui, const WCHAR *path);

    int getWidth()  { return data_.Width; }
    int getHeight() { return data_.Height; }
    UINT *getPixels() { return buffer_ ;}

    // Combine the image onto the given bitmap data, with the upper left at x/y
    void combine(BITMAPINFO *bitmapInfo, void *bits, int x, int y);
    // Combine the image onto the given bitmap data, tiling it over the given rectangle
    void tile(BITMAPINFO *bitmapInfo, void *bits, int x0, int y0, int x1, int y1);

private:
    void initFromBitmap(Gdiplus::Bitmap *bitmap);
 
    Gdiplus::BitmapData data_;
    UINT *buffer_;
};

HippoImageBuffer::HippoImageBuffer()
{
    buffer_ = (UINT *)EMPTY_BUFFER;
    data_.Width = 1;
    data_.Height = 1;
    data_.Stride = 4;
    data_.PixelFormat = PixelFormat32bppPARGB;
    data_.Scan0 = (BYTE *)buffer_;
}

HippoImageBuffer::~HippoImageBuffer()
 {
    if (buffer_ != EMPTY_BUFFER)
        delete[] buffer_;
}

void
HippoImageBuffer::load(HippoUI *ui, const WCHAR *path)
{
    HippoBSTR filename;
    ui->getAppletPath(HippoBSTR(path), &filename);
    
    // If loading fails, we leave the original data, a 1x1 empty image, in place
    Gdiplus::Bitmap result(filename.m_str, false);
    if (result.GetLastStatus() == Gdiplus::Ok) {
        initFromBitmap(&result);
    } else {
        hippoDebugLogW(L"Failed to load image %ls, status = %d", filename, (int)result.GetLastStatus());
    }
}

void 
HippoImageBuffer::initFromBitmap(Gdiplus::Bitmap *bitmap)
{
    if (buffer_ != EMPTY_BUFFER)
        delete[] buffer_;

    buffer_ = new UINT[bitmap->GetWidth() * bitmap->GetHeight()];
    data_.Width = bitmap->GetWidth();
    data_.Height = bitmap->GetHeight();
    data_.Stride = 4 * bitmap->GetWidth();
    data_.Scan0 = (BYTE *)buffer_;

    Gdiplus::Rect rect(0, 0, data_.Width, data_.Height);

    
    bitmap->LockBits(&rect, (UINT)Gdiplus::ImageLockModeRead | (UINT)Gdiplus::ImageLockModeUserInputBuf, PixelFormat32bppPARGB, &data_);
    bitmap->UnlockBits(&data_);
}

void 
HippoImageBuffer::combine(BITMAPINFO *bitmapInfo, void *bits, int x, int y)
{
    tile(bitmapInfo, bits, x, y, x + getWidth(), y + getHeight());
}

void 
HippoImageBuffer::tile(BITMAPINFO *bitmapInfo, void *bits, int x0, int y0, int x1, int y1)
{
    // The way we combine images is that where we have alpha == 0xff in the source
    // image, we leave the destination image untouched, otherwise we replace the
    // destination pixel with the source pixel; basically we are "fixing up" the
    // image we drew with IE, which can't handle partial alpha on the background
    // of a web page, by replacing just the partially alpha pixels

    int destWidth = bitmapInfo->bmiHeader.biWidth;
    int destHeight = - bitmapInfo->bmiHeader.biHeight;

    if (x0 < 0)
        x0 = 0;
    if (y0 < 0)
        y0 = 0;
    if (x1 > destWidth)
        x1 = destWidth;
    if (y1 > destHeight)
        y1 = destHeight;

    const UINT *srcRow = buffer_;
    UINT *destRow = (UINT *)bits + y0 * destWidth + x0;

    int j = 0;
    for (int y = y0; y < y1; y++, j++) {
        if (j == getHeight()) {
            j = 0;
            srcRow = getPixels();
        }

        const UINT *src = srcRow;
        UINT *dest = destRow;

        int i = 0;
        for (int x = x0; x < x1; x++, i++) {
            if (i == getWidth()) {
                i = 0;
                src = srcRow;
            }

            UINT srcPixel = *src;
            if ((srcPixel & 0xFF000000) != 0xFF000000)
                *dest = srcPixel;

            src++;
            dest++;
        }

        srcRow += getWidth();
        destRow += destWidth;
    }
}

// This class tracks the images we combine with the web-browser's image to
// produce the bubble with a drop shadow

class HippoBubbleImages {
public:
    HippoBubbleImages(HippoUI *ui);
    
    // Combine the drop-shadow onto the given bitmap data
    void combineImages(BITMAPINFO *bitmapInfo, void *bits);

private:
    HippoImageBuffer cornerTR_;
    HippoImageBuffer cornerTL_;
    HippoImageBuffer cornerBR_;
    HippoImageBuffer cornerBL_;
    HippoImageBuffer edgeT_;
    HippoImageBuffer edgeB_;
    HippoImageBuffer edgeL_;
    HippoImageBuffer edgeR_;
};

HippoBubbleImages::HippoBubbleImages(HippoUI *ui)
{
    cornerTR_.load(ui, L"wbubcnr_tr.png");
    cornerTL_.load(ui, L"wbubcnr_tl.png");
    cornerBR_.load(ui, L"wbubcnr_br.png");
    cornerBL_.load(ui, L"wbubcnr_bl.png");
    edgeL_.load(ui, L"bubedge_l.png");
    edgeR_.load(ui, L"bubedge_r.png");
    edgeT_.load(ui, L"bubedge_t.png");
    edgeB_.load(ui, L"bubedge_b.png");
}

void 
HippoBubbleImages::combineImages(BITMAPINFO *bitmapInfo, void *bits)
{
    int destWidth = bitmapInfo->bmiHeader.biWidth;
    int destHeight = - bitmapInfo->bmiHeader.biHeight;

    cornerTL_.combine(bitmapInfo, bits, 0, 0);
    cornerTR_.combine(bitmapInfo, bits, destWidth - cornerTR_.getWidth(), 0);
    cornerBL_.combine(bitmapInfo, bits, 0, destHeight - cornerBL_.getHeight());
    cornerBR_.combine(bitmapInfo, bits, destWidth - cornerBR_.getWidth(), destHeight - cornerBR_.getHeight());

    edgeT_.tile(bitmapInfo, bits, cornerTL_.getWidth(), 0, destWidth - cornerTR_.getWidth(), edgeT_.getHeight());
    edgeB_.tile(bitmapInfo, bits, cornerBL_.getWidth(), destHeight - edgeB_.getHeight(), destWidth - cornerBR_.getWidth(), destHeight);
    edgeL_.tile(bitmapInfo, bits, 0, cornerTL_.getHeight(), edgeL_.getWidth(), destHeight - cornerBL_.getHeight());
    edgeR_.tile(bitmapInfo, bits, destWidth - edgeR_.getWidth(), cornerTR_.getHeight(), destWidth, destHeight - cornerBR_.getHeight());
}

// These values basically don't matter, since the bubble picks its own
// size, but matching the values from bubble.js may save a bit on resizing
static const int BASE_WIDTH = 450;
static const int BASE_HEIGHT = 123;
static const int BLARM_HEIGHT = 180;
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
    images_ = NULL;
 
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

    if (images_)
        delete images_;
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

    moveResize(desktopRect.right - desiredWidth_, desktopRect.bottom - desiredHeight_,
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
    groupMembershipChanged_.connect(G_OBJECT(ui_->getConnection()), "group-membership-changed",
        slot(this, &HippoBubble::onGroupMembershipChanged));
    postActivity_.connect(G_OBJECT(ui_->getConnection()), "post-activity",
        slot(this, &HippoBubble::onPostActivity));
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

    return HippoAbstractIEWindow::processMessage(message, wParam, lParam);
}

void 
HippoBubble::setLinkNotification(HippoPost *share)
{
    // watch future changes to this post
    connectPost(share);

    if (!create())
        return;

    variant_t result;
    ui_->debugLogW(L"Invoking dhAddLinkShare");
    // Note if you change the arguments to this function, you must change notification.js
    ie_->createInvocation(L"dhAddLinkShare")
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
    // ie_->createInvocation(L"dhDisplayMissed").run();
    setShown();
}

void 
HippoBubble::onUserJoinedGroupChatRoom(HippoPerson *person,
                                       HippoEntity *entity)
{
    if (hippo_entity_get_entity_type(entity) != HIPPO_ENTITY_GROUP) {
        g_warning("HippoBubble::onUserJoinedGroupChatRoom was called with an entity that is not a group");
        return;
    }

    if (ui_->isGroupChatActive(entity)) // Already have a window open
        return;

    if (!ie_) {
        if (!create())
            return;
    }

    variant_t result;
    ie_->createInvocation(L"dhGroupViewerJoined")
        .addDispatch(HippoEntityWrapper::getWrapper(entity))
        .addBool(false)
        .getResult(&result);
    if (result.vt != VT_BOOL || !result.boolVal) {
        return;
    }
    setShown();
}

void 
HippoBubble::onGroupChatRoomMessageAdded(HippoChatMessage *message,
                                         HippoEntity      *entity)
{
    if (hippo_entity_get_entity_type(entity) != HIPPO_ENTITY_GROUP) {
        g_warning("HippoBubble::onGroupChatRoomMessageAdded was called with an entity that is not a group");
        return;
    }

    if (ui_->isGroupChatActive(entity)) // Already have a window open
        return;

    if (!ie_) {
        if (!create())
            return;
    }
    HippoChatRoom *room = hippo_entity_get_chat_room(entity);
    gboolean isReloading = hippo_chat_room_get_loading(room);

    variant_t result;
    ie_->createInvocation(L"dhGroupChatRoomMessage")
        .addDispatch(HippoEntityWrapper::getWrapper(entity))
        .addBool(isReloading ? true : false)
        .getResult(&result);
    if (result.vt != VT_BOOL || !result.boolVal) {
        return;
    }

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

    variant_t result;
    ie_->createInvocation(L"dhViewerJoined")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .add(false)
        .getResult(&result);

    if (result.vt != VT_BOOL || !result.boolVal) {
        return;
    }

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

    HippoChatRoom *room = hippo_post_get_chat_room(post);
    gboolean isReloading = hippo_chat_room_get_loading(room);

    variant_t result;
    ie_->createInvocation(L"dhChatRoomMessage")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .addBool(isReloading ? true : false)
        .getResult(&result);

    if (result.vt != VT_BOOL) {
        ui_->debugLogU("dhChatRoomMessage returned invalid type");
        return;
    }
    if (!result.boolVal) {
        ui_->debugLogU("dhChatRoomMessage returned false");
        return;
    }

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
        setLinkNotification(post);
    } else {
        hippoDebugLogU("Post %s is not new, not bubbling up for now",
            hippo_post_get_guid(post));
    }
}

void 
HippoBubble::onGroupMembershipChanged(HippoEntity *group, HippoEntity *user, const char *membershipStatus)
{
    if (hippo_entity_get_entity_type(group) != HIPPO_ENTITY_GROUP) {
        g_warning("HippoBubble::onGroupMembershipChanged was called with an entity that is not a group");
        return;
    }

    if (!ie_) {
        if (!create())
            return;
    }

    HippoBSTR status;
    status.setUTF8(membershipStatus);

    variant_t result;
    ie_->createInvocation(L"dhGroupMembershipChanged")
        .addDispatch(HippoEntityWrapper::getWrapper(group))
        .addDispatch(HippoEntityWrapper::getWrapper(user))
        .add(status)
        .getResult(&result);
    if (result.vt != VT_BOOL || !result.boolVal) {
        return;
    }

    setShown();
}

void
HippoBubble::onPostActivity(HippoPost *post)
{
    if (!create())
        return;

    // TODO: we don't presently check for isPostActive() here because
    // the total viewer count is not yet displayed in the framer.  Once
    // we do implement that, we should suppress a bubble if the post is
    // active.

    variant_t result;
    ui_->debugLogW(L"Invoking dhPostActivity");
    // Note if you change the arguments to this function, you must change notification.js
    ie_->createInvocation(L"dhPostActivity")
        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
        .getResult(&result);
    if (result.vt != VT_BOOL || !result.boolVal) {
        return;
    }
    setShown();
}

void
HippoBubble::onChatRoomLoaded(HippoChatRoom *room)
{
    if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_POST) {
        HippoPost *post = hippo_data_cache_lookup_post(ui_->getDataCache(), hippo_chat_room_get_id(room));

        g_assert(post != NULL);

        GConnection1<void,HippoPerson*>::named_connect(G_OBJECT(room), "hippo-bubble-user-joined",
            "user-joined", bind(slot(this, &HippoBubble::onUserJoined), post));
        GConnection1<void,HippoChatMessage*>::named_connect(G_OBJECT(room), "hippo-bubble-message-added",
            "message-added", bind(slot(this, &HippoBubble::onMessageAdded), post));
    } else if (hippo_chat_room_get_kind(room) == HIPPO_CHAT_KIND_GROUP) {
        HippoEntity *entity = hippo_data_cache_lookup_entity(ui_->getDataCache(), hippo_chat_room_get_id(room));

        g_assert(entity != NULL);

        GConnection1<void,HippoPerson*>::named_connect(G_OBJECT(room), "hippo-bubble-user-joined-group-chat-room",
            "user-joined", bind(slot(this, &HippoBubble::onUserJoinedGroupChatRoom), entity));
        GConnection1<void,HippoChatMessage*>::named_connect(G_OBJECT(room), "hippo-bubble-group-chat-room-message-added",
            "message-added", bind(slot(this, &HippoBubble::onGroupChatRoomMessageAdded), entity));
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
    if (!images_)
        images_ = new HippoBubbleImages(ui_);

    bool firstTime = layerDC_ == NULL;

    if (firstTime)
        layerDC_ = CreateCompatibleDC(NULL);

    int width = desiredWidth_;
    int height = desiredHeight_;
    BITMAPINFO bitmapInfo;

    ZeroMemory(&bitmapInfo, sizeof(BITMAPINFO));
    bitmapInfo.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bitmapInfo.bmiHeader.biWidth = width;
    bitmapInfo.bmiHeader.biHeight = - height; // Negative, so that we are top-down
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

    // This makes the entire image default to opaque
    unsigned char *p = (unsigned char *)bits;
    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            p[3] = 0xff;
            p += 4;
        }
    }

    POINT srcPoint = { 0, 0 };
    SIZE size = { width, height };
    
    BLENDFUNCTION blend;
    blend.BlendOp = AC_SRC_OVER;
    blend.BlendFlags = 0;
    blend.SourceConstantAlpha = 0xff;
    blend.AlphaFormat = AC_SRC_ALPHA;

    images_->combineImages(&bitmapInfo, bits);

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

HRESULT
HippoBubble::ShowChatWindow(BSTR postId) 
{
    return ui_->ShowChatWindow(postId);
}

HRESULT
HippoBubble::IgnorePost(BSTR postId)
{
    ui_->ignorePost(postId);
    return S_OK;
}

HRESULT
HippoBubble::IgnoreEntity(BSTR entityId)
{
    ui_->ignoreEntity(entityId);
    return S_OK;
}

HRESULT
HippoBubble::IgnoreChat(BSTR chatId)
{
    ui_->ignoreChat(chatId);
    return S_OK;
}

HRESULT 
HippoBubble::DoGroupInvite(BSTR groupId, BSTR userId)
{
    ui_->groupInvite(groupId, userId);
    return S_OK;
}
