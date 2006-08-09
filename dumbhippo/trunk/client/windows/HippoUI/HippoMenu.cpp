/* HippoMenu.cpp: popup menu for the notification icon
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "stdafx-hippoui.h"

#include <HippoUtil.h>
#include "HippoMenu.h"
#include "HippoUI.h"
#include "HippoComWrappers.h"

// These values basically don't matter, since the javascript code drives the size
static const int BASE_WIDTH = 200;
static const int BASE_HEIGHT = 250;

HippoMenu::HippoMenu(void)
{
    refCount_ = 1;
    hippoLoadTypeInfo((WCHAR *)0, &IID_IHippoMenu, &ifaceTypeInfo_, NULL);

    desiredWidth_ = BASE_WIDTH;
    desiredHeight_ = BASE_HEIGHT;
    mouseX_ = 0;
    mouseY_ = 0;

    setUseParent(true);
    setClassName(L"HippoMenuClass");
    setClassStyle(CS_HREDRAW | CS_VREDRAW | CS_DROPSHADOW);
    setWindowStyle(WS_POPUP);
    setExtendedStyle(WS_EX_TOPMOST);
    setTitle(L"Hippo Menu");
    setApplication(this);
}

HippoMenu::~HippoMenu(void)
{
    clearActivePosts(false);
}

void 
HippoMenu::popup(int mouseX, int mouseY)
{
    mouseX_ = mouseX;
    mouseY_ = mouseY;

    create();
    moveResizeWindow();
    ie_->createInvocation(L"dhMenuSetRecentCount")
        .addLong(ui_->getRecentMessageCount())
        .run();
    show();
}

void 
HippoMenu::clearActivePosts(bool notify)
{
    for (int i = (int)activePosts_.size() - 1; i >= 0; i--) {
        disconnectPost(activePosts_[i]);
        if (notify)
            invokeRemoveActivePost(i);
    }
    activePosts_.clear();
}

void 
HippoMenu::addActivePost(HippoPost *post)
{
    int i = 0;
    for (std::vector<HippoGObjectPtr<HippoPost> >::iterator iter = activePosts_.begin();
         iter != activePosts_.end();
         iter++, i++) 
    {
        if (post == *iter) {
            activePosts_.erase(iter);
            disconnectPost(*iter);
            invokeRemoveActivePost(i);
            break;
        }
    }

    activePosts_.insert(activePosts_.begin(), post);
    connectPost(post);
    invokeInsertActivePost(0, post);
}
 
void 
HippoMenu::invokeRemoveActivePost(int i)
{
    if (ie_)
        ie_->createInvocation(L"dhMenuRemoveActivePost").addLong(i).run();
}

void 
HippoMenu::invokeInsertActivePost(int i, HippoPost *post)
{
    if (ie_)
        ie_->createInvocation(L"dhMenuInsertActivePost")
            .addLong(i)
            .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))
            .run();
}

void 
HippoMenu::updatePost(HippoPost *post)
{
    if (!ie_)
        return;

    int i = 0;
    for (std::vector<HippoGObjectPtr<HippoPost> >::iterator iter = activePosts_.begin();
         iter != activePosts_.end();
         iter++, i++) 
    {
        if (post == *iter) {
            ie_->createInvocation(L"dhMenuUpdatePost")
                        .addLong(i)
                        .addDispatch(HippoPostWrapper::getWrapper(post, ui_->getDataCache()))               
                        .run();
        }
    }
}

void
HippoMenu::onActivePostsChanged()
{
    clearActivePosts(true);

    GSList *active = hippo_data_cache_get_active_posts(ui_->getDataCache());

    for (GSList *link = active; link != NULL; link = link->next) {
        HippoPost *post = HIPPO_POST(link->data);
        
        addActivePost(post);

        g_object_unref(G_OBJECT(post));
    }

    g_slist_free(active);
}

void
HippoMenu::onPostChanged(HippoPost *post)
{
    g_return_if_fail(HIPPO_IS_POST(post)); // double-check the HippoGSignal.h stuff ;-)
    updatePost(post);
}

void
HippoMenu::onMessageAdded(HippoChatMessage *message,
                          HippoPost        *post)
{
    
}

void
HippoMenu::connectPost(HippoPost *post)
{
    GConnection0<void>::named_connect(G_OBJECT(post), "hippo-menu-connection", "changed", 
            bind(slot(this, &HippoMenu::onPostChanged), post));

    HippoChatRoom *chatRoom = hippo_post_get_chat_room(post);

    g_return_if_fail(chatRoom != NULL);

    GConnection1<void,HippoChatMessage*>::named_connect(G_OBJECT(chatRoom), "hippo-menu-connection",
                "message-added", 
                bind(slot(this, &HippoMenu::onMessageAdded), post));
}

void
HippoMenu::disconnectPost(HippoPost *post)
{
    GConnection::named_disconnect(G_OBJECT(post), "hippo-menu-connection");
    
    HippoChatRoom *chatRoom = hippo_post_get_chat_room(post);
    GConnection::named_disconnect(G_OBJECT(chatRoom), "hippo-menu-connection");
}

void
HippoMenu::moveResizeWindow() 
{
    int x = mouseX_;
    int y = mouseY_;
    int width = desiredWidth_;
    int height = desiredHeight_;

    RECT desktopRect;
    HRESULT hr = SystemParametersInfo(SPI_GETWORKAREA, NULL, &desktopRect, 0);

    if (y + height > desktopRect.bottom)
        y -= height;
    if (x + width > desktopRect.right)
        x -= width;

    moveResize(x, y, width, height);
}

HippoBSTR
HippoMenu::getURL()
{
    HippoBSTR srcURL;

    ui_->getAppletURL(L"menu.xml", &srcURL);

    return srcURL;
}

void
HippoMenu::initializeWindow()
{
    assert(ui_ != NULL);

    activePostsChanged_.connect(G_OBJECT(ui_->getDataCache()), "active-posts-changed",
            slot(this, &HippoMenu::onActivePostsChanged));

    // initially load active posts
    onActivePostsChanged();

    moveResizeWindow();
}
    
void 
HippoMenu::initializeIE()
{
    HippoBSTR appletURL;
    ui_->getAppletURL(L"", &appletURL);
    HippoBSTR styleURL;
    ui_->getAppletURL(L"clientstyle.xml", &styleURL);
    ie_->setXsltTransform(styleURL, L"appleturl", appletURL.m_str, NULL);
}

void 
HippoMenu::initializeBrowser()
{
    HippoBSTR appletURL;
    ui_->getAppletURL(HippoBSTR(L""), &appletURL);

    ie_->createInvocation(L"dhInit").add(appletURL).run();

    int i = 0;
    for (std::vector<HippoGObjectPtr<HippoPost> >::iterator iter = activePosts_.begin();
         iter != activePosts_.end();
         iter++, i++) 
    {
        invokeInsertActivePost(i, *iter);
    }
}

bool 
HippoMenu::processMessage(UINT   message,
                          WPARAM wParam,
                          LPARAM lParam)
{
    if (message == WM_ACTIVATE && LOWORD(wParam) == WA_INACTIVE) {
        hide();
    }

    return HippoAbstractWindow::processMessage(message, wParam, lParam);
}

void 
HippoMenu::onClose(bool fromScript)
{
    hide();
}

// IHippoMenu

STDMETHODIMP 
HippoMenu::Exit()
{
    ui_->Quit(NULL);

    return S_OK;
}
    
STDMETHODIMP
HippoMenu::GetServerBaseUrl(BSTR *result)
{
    HippoBSTR temp;
    ui_->getRemoteURL(L"", &temp);

    temp.CopyTo(result);
    return S_OK;
}

STDMETHODIMP 
HippoMenu::Hush()
{
    return S_OK;
}

STDMETHODIMP 
HippoMenu::Resize(int width, int height)
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
HippoMenu::ShowRecent()
{
    return ui_->ShowRecent();
}

/////////////////////// IUnknown implementation ///////////////////////

STDMETHODIMP 
HippoMenu::QueryInterface(const IID &ifaceID, 
                            void   **result)
{
    if (IsEqualIID(ifaceID, IID_IUnknown))
        *result = static_cast<IUnknown *>(static_cast<IHippoMenu*>(this));
    else if (IsEqualIID(ifaceID, IID_IDispatch)) 
        *result = static_cast<IDispatch *>(this);
    else if (IsEqualIID(ifaceID, IID_IHippoMenu)) 
        *result = static_cast<IHippoMenu *>(this);
    else {
        *result = NULL;
        return E_NOINTERFACE;
    }

    this->AddRef();
    return S_OK;    
}                                             

HIPPO_DEFINE_REFCOUNTING(HippoMenu)


//////////////////////// IDispatch implementation ///////////////////

// We just delegate IDispatch to the standard Typelib-based version.

STDMETHODIMP
HippoMenu::GetTypeInfoCount(UINT *pctinfo)
{
    if (pctinfo == NULL)
        return E_INVALIDARG;

    *pctinfo = 1;

    return S_OK;
}

STDMETHODIMP 
HippoMenu::GetTypeInfo(UINT        iTInfo,
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
HippoMenu::GetIDsOfNames (REFIID    riid,
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
HippoMenu::Invoke (DISPID        member,
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
    HippoQIPtr<IHippoMenu> hippoMenu(static_cast<IHippoMenu *>(this));
    HRESULT hr = DispInvoke(hippoMenu, ifaceTypeInfo_, member, flags, 
                            dispParams, result, excepInfo, argErr);
    return hr;
}
