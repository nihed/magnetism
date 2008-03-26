/* HippoChatWindow.cpp: Window displaying a chat room for a post
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoChatWindow.h"
#include "HippoDispatchableObject.h"
#include "HippoIE.h"
#include "HippoChatManager.h"
#include "HippoUIUtil.h"
#include "resource.h"

static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

static const WCHAR *CLASS_NAME = L"HippoChatWindowClass";
static const WCHAR *TITLE = L"Mugshot Quips and Comments";

class HippoChatWindowImpl : 
    public HippoChatWindow,
    public HippoIECallback,
    public HippoDispatchableObject<IHippoChatWindow, HippoChatWindowImpl>,
    public HippoMessageHook
{
public:
    HippoChatWindowImpl(HippoChatManager *manager);
    ~HippoChatWindowImpl();

    static ITypeInfo *getTypeInfo();

    void setChatId(BSTR chatId);
    BSTR getChatId();
    HippoWindowState getWindowState();

    bool create();
    void setForegroundWindow();

    // IHippoChatWindow methods
    STDMETHODIMP DemandAttention();

    // HippoIECallback methods
    void onDocumentComplete();
    void onClose();
    void launchBrowser(const HippoBSTR &url);
    bool isOurServer(const HippoBSTR &host);
    HRESULT getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser);

    HIPPO_DECLARE_REFCOUNTING;

    void doClose();

private:
    HippoChatManager *manager_;
    HippoPtr<HippoIE> ie_;
    HWND window_;

    HINSTANCE instance_;

    DWORD refCount_;
    HippoBSTR chatId_;
    HippoBSTR url_;

    bool embedIE(void);
    bool createWindow(void);
    void onWindowDestroyed();
    bool registerClass();

    bool hookMessage(MSG *msg);
    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);
    static LRESULT CALLBACK windowProc(HWND   window,
                                       UINT   message,
                                       WPARAM wParam,
                                       LPARAM lParam);

    bool getServerHostPort(BSTR *host, int *port);
};

class HippoChatWindowCloseTask : public HippoThreadTask
{
public:
    HippoChatWindowCloseTask(HippoChatWindowImpl *impl) : impl_(impl) {}
    void call();
    void cancel();
private:
    HippoPtr<HippoChatWindowImpl> impl_;
};

void
HippoChatWindowCloseTask::call()
{
    impl_->doClose();

    delete this;
}

void
HippoChatWindowCloseTask::cancel()
{
    delete this;
}

HippoChatWindow *
HippoChatWindow::createInstance(HippoChatManager *manager)
{
    return new HippoChatWindowImpl(manager);
}

HippoChatWindowImpl::HippoChatWindowImpl(HippoChatManager *manager)
{
    manager_ = manager;
    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    ie_ = NULL;
    refCount_ = 1;
}

HippoChatWindowImpl::~HippoChatWindowImpl(void)
{
    hippoDebugLogW(L"Finalizing HippoChatWindow");
    assert(window_ == NULL);
    assert(ie_ == NULL);
}

ITypeInfo *
HippoChatWindowImpl::getTypeInfo()
{
    static HippoPtr<ITypeInfo> typeInfo;
    if (!typeInfo)
        hippoLoadTypeInfo((WCHAR *)0, &IID_IHippoChatWindow, &typeInfo, NULL);

    return typeInfo;
}

STDMETHODIMP_(DWORD)
HippoChatWindowImpl::AddRef() 
{
    refCount_++;

    return refCount_;
}

STDMETHODIMP_(DWORD)
HippoChatWindowImpl::Release() 
{
    refCount_--;
    if (refCount_ == 0) {
        delete this;
        return 0;
    } else {
        return refCount_;
    }
}

//HIPPO_DEFINE_REFCOUNTING(HippoChatWindowImpl)

void 
HippoChatWindowImpl::setChatId(BSTR chatId)
{
    chatId_ = chatId;

    HippoBSTR serverHost;
    int serverPort;

    getServerHostPort(&serverHost, &serverPort);

    HippoBSTR url(L"http://");
    url.Append(serverHost.m_str);
    if (serverPort != 80) {
        url.Append(':');
        WCHAR buffer[16];
        StringCchPrintfW(buffer, sizeof(buffer) / sizeof(buffer[0]), L"%d", serverPort);
        url.Append(buffer);
    }
    url.Append(L"/chatwindow?chatId=");
    url.Append(chatId_);

    url_ = url;
}

BSTR
HippoChatWindowImpl::getChatId()
{
    return chatId_.m_str;
}

HippoWindowState 
HippoChatWindowImpl::getWindowState()
{
    if (!window_)
        return HIPPO_WINDOW_STATE_CLOSED;
    else if (!hippoWindowIsOnscreen(window_))
        return HIPPO_WINDOW_STATE_HIDDEN;
    else if (!hippoWindowIsActive(window_))
        return HIPPO_WINDOW_STATE_ONSCREEN;
    else
        return HIPPO_WINDOW_STATE_ACTIVE;
}

// IHippoChatWindow

bool
HippoChatWindowImpl::createWindow(void)
{
    window_ = CreateWindow(CLASS_NAME, TITLE, WS_OVERLAPPEDWINDOW,
                           CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                           NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoChatWindowImpl>(window_, this);
    manager_->registerMessageHook(window_, this);

    return true;
}

void
HippoChatWindowImpl::onWindowDestroyed(void)
{
    if (window_) {
        manager_->unregisterMessageHook(window_);
        hippoSetWindowData<HippoChatWindowImpl>(window_, NULL);

        window_ = NULL;
    }
    if (ie_) {
        ie_->shutdown();
        ie_ = NULL;
        manager_->onChatWindowClosed(this);
    }

    // At this point, we've closed the web browser control, and the
    // script engine has been shut down. However, there's still JScript
    // garbage hanging around created when the browser was shut down
    // that won't be cleaned up until either the thread is destroyed
    // or another garbage collection runs. Since we share one thread
    // for all HippoChatWindow, we need to force a GC at the next 
    // opportunity.
    //
    // Now that we explicitly drop references to our control
    // in an 'onunload' handler, this is less important than it was
    // originally since the collection of the left-over garbage has no
    // user visible manifestation, but we'll leave it in for now. 
    // If it causes problems, it can be removed.
    manager_->gcWhenIdle();
}

void
HippoChatWindowImpl::onClose()
{
    // It screws up the internal Internet Explorer state to destroy
    // the web browser control from the IWebBrowserEvents task that
    // asks whether it is OK to destroy the web browser control, so
    // we queue closing asynchronously.
    //
    // (The specific issue is that even though we cancel the close,
    // if you close the browser at the same time, the browser shows
    // a dialog asking them if they want to close (the already closed!)
    // browser!)

    manager_->getExecutor()->doAsync(new HippoChatWindowCloseTask(this));
}

void 
HippoChatWindowImpl::onDocumentComplete()
{
}

void 
HippoChatWindowImpl::launchBrowser(const HippoBSTR &url)
{
    manager_->getThreadUI()->LaunchBrowser(url.m_str);
}

bool
HippoChatWindowImpl::getServerHostPort(BSTR *host, int *port)
{
    HippoBSTR serverName;

    HRESULT hr = manager_->getThreadUI()->GetServerName(&serverName);
    if (FAILED(hr))
        return false;

    WCHAR *colon = wcschr(serverName, ':');
    if (colon == NULL)
        return false;

    WCHAR *end;
    *port = (int)wcstol(colon + 1, &end, 0);
    if (*end != '\0')
        return false;

    HippoBSTR serverHost((int)(colon - serverName.m_str), serverName.m_str);
    serverHost.CopyTo(host);

    return true;
}

bool
HippoChatWindowImpl::isOurServer(const HippoBSTR &host)
{
    HippoBSTR serverHost;
    int serverPort;

    if (!getServerHostPort(&serverHost, &serverPort))
        return false;

    return host == serverHost;
}

HRESULT 
HippoChatWindowImpl::getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser)
{
    return E_UNEXPECTED;
}

bool
HippoChatWindowImpl::embedIE(void)
{
    ie_ = HippoIE::create(window_, url_, this, this);
    ie_->Release();

    ie_->setThreeDBorder(false);
    ie_->embedBrowser();

    return true;
}

bool
HippoChatWindowImpl::create(void)
{
    if (window_ != NULL) {
        return true;
    }
    if (!registerClass()) {
        hippoDebugLogW(L"Failed to register window class");
        return false;
    }
    if (!createWindow()) {
        hippoDebugLogW(L"Failed to create window");
        return false;
    }
    if (!embedIE()) {
        hippoDebugLogW(L"Failed to embed IE");
        return false;
    }

    ShowWindow(window_, SW_RESTORE);

    return true;
}

bool
HippoChatWindowImpl::registerClass()
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

    WCHAR *icon = MAKEINTRESOURCE(IDI_MUGSHOT);
    wcex.hIcon = (HICON)LoadImage(instance_, icon, IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR | LR_SHARED);
    wcex.hIconSm = (HICON)LoadImage(instance_, icon, IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR | LR_SHARED);

    if (RegisterClassEx(&wcex) == 0 && 
        GetClassInfoEx(instance_, CLASS_NAME, &wcex) == 0)
        return false;

    return true;
}

void 
HippoChatWindowImpl::setForegroundWindow()
{
    if (window_)
        SetForegroundWindow(window_);
}

STDMETHODIMP
HippoChatWindowImpl::DemandAttention()
{
#if 0
    // We disable attention demanding for now, since we show the
    // notification window when the chat window isn't visible

    FLASHWINFO flashInfo;
    flashInfo.cbSize = sizeof(flashInfo);
    flashInfo.hwnd = window_;
    flashInfo.dwTimeout = 0;
    flashInfo.dwFlags = FLASHW_TIMERNOFG | FLASHW_TRAY;
    flashInfo.uCount = 5;
    FlashWindowEx(&flashInfo);
#endif
    return S_OK;
}

void 
HippoChatWindowImpl::doClose()
{
    if (window_)
        DestroyWindow(window_);
}

bool
HippoChatWindowImpl::hookMessage(MSG *msg)
{
    if (ie_ && (msg->message >= WM_KEYFIRST && msg->message <= WM_KEYLAST))
    {
        HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
        HRESULT res = active->TranslateAccelerator(msg);
        return res == S_OK;
    }
    return FALSE;
}

bool
HippoChatWindowImpl::processMessage(UINT   message,
                                    WPARAM wParam,
                                    LPARAM lParam)
{
    switch (message) 
    {
    case WM_ACTIVATE:
        {
            // It's not completely clear that this is necessary
            HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
            if (active)
                active->OnFrameWindowActivate(LOWORD(wParam) != WA_INACTIVE);
            return true;
        }
    case WM_DESTROY:
        onWindowDestroyed();
        return true;
    case WM_SIZE:
        if (ie_) {
            RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
            ie_->resize(&rect);
        }
        return true;
    default:
        return false; 
    }
}

LRESULT CALLBACK 
HippoChatWindowImpl::windowProc(HWND   window,
                                UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    // Our only content is the IE browser, and it erases everything itself
    // on repaint, so we tell windows not to repaint by returning 1. This
    // prevents flicker on resize.
    if (message == WM_ERASEBKGND)
        return 1;

    HippoPtr<HippoChatWindowImpl> chatWindow = hippoGetWindowData<HippoChatWindowImpl>(window);
    if (chatWindow) {
        bool result = chatWindow->processMessage(message, wParam, lParam);
        if (result)
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}
