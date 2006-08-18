/* HippoAbstractWindow.cpp: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoUI.h"
#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

HippoAbstractWindow::HippoAbstractWindow()
{
    animate_ = false;
    useParent_ = false;
    updateOnShow_ = false;
    classStyle_ = CS_HREDRAW | CS_VREDRAW;
    windowStyle_ = WS_OVERLAPPEDWINDOW;
    extendedStyle_ = 0;

    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
    ie_ = NULL;

    ieCallback_ = new HippoAbstractWindowIECallback(this);
}

HippoAbstractWindow::~HippoAbstractWindow(void)
{
    ui_->unregisterWindowMsgHook(window_);
    DestroyWindow(window_);

    delete ieCallback_;
}

void 
HippoAbstractWindow::setUI(HippoUI *ui)
{
    ui_ = ui;
    if (ui_)
        initializeUI();
}

void
HippoAbstractWindow::setApplication(IDispatch *application)
{
    application_ = application;
}

void
HippoAbstractWindow::setUseParent(bool useParent)
{
    useParent_ = useParent;
}

void 
HippoAbstractWindow::setAnimate(bool animate)
{
    animate_ = animate;
}

void 
HippoAbstractWindow::setUpdateOnShow(bool updateOnShow)
{
    updateOnShow_ = updateOnShow;
}

void 
HippoAbstractWindow::setClassStyle(UINT classStyle)
{
    classStyle_ = classStyle;
}

void 
HippoAbstractWindow::setWindowStyle(DWORD windowStyle)
{
    windowStyle_ = windowStyle;
}

void 
HippoAbstractWindow::setExtendedStyle(DWORD extendedStyle)
{
    extendedStyle_ = extendedStyle;
}

void 
HippoAbstractWindow::setURL(const HippoBSTR &url)
{
    url_ = url;   
}

void 
HippoAbstractWindow::setClassName(const HippoBSTR &className)
{
    className_ = className;
}

void 
HippoAbstractWindow::setTitle(const HippoBSTR &title)
{
    title_ = title;
}

HippoBSTR
HippoAbstractWindow::getURL()
{
    return url_;
}

void 
HippoAbstractWindow::initializeWindow()
{
}

void 
HippoAbstractWindow::initializeIE()
{
}

void 
HippoAbstractWindow::initializeBrowser()
{
}

void 
HippoAbstractWindow::initializeUI()
{
}

void 
HippoAbstractWindow::onClose(bool fromScript)
{
}

void
HippoAbstractWindow::onDocumentComplete()
{
}

bool
HippoAbstractWindow::createWindow(void)
{
    window_ = CreateWindowEx(extendedStyle_, className_, title_, windowStyle_,
                             CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                             useParent_ ? ui_->getWindow() : NULL, 
                             NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoAbstractWindow>(window_, this);
    ui_->registerWindowMsgHook(window_, this);

    initializeWindow();

    return true;
}

void 
HippoAbstractWindow::HippoAbstractWindowIECallback::onClose()
{
    abstractWindow_->onClose(true);
}

void 
HippoAbstractWindow::HippoAbstractWindowIECallback::onDocumentComplete()
{
    abstractWindow_->onDocumentComplete();
}

void 
HippoAbstractWindow::HippoAbstractWindowIECallback::launchBrowser(const HippoBSTR &url)
{
    abstractWindow_->ui_->launchBrowser(url.m_str);
}

bool
HippoAbstractWindow::HippoAbstractWindowIECallback::isOurServer(const HippoBSTR &host)
{
    HippoUStr serverHost;
    int port;
    HippoPlatform *platform;

    platform = abstractWindow_->ui_->getPlatform();
    hippo_platform_get_web_host_port(platform, &serverHost, &port);

    return host == serverHost.toBSTR();
}

HRESULT 
HippoAbstractWindow::HippoAbstractWindowIECallback::getToplevelBrowser(const IID &ifaceID, void **toplevelBrowser)
{
    return E_UNEXPECTED;
}

bool
HippoAbstractWindow::embedIE(void)
{
    ie_ = HippoIE::create(window_, getURL(), ieCallback_, application_);
    ie_->setThreeDBorder(false);
    initializeIE();
    ie_->embedBrowser();
    browser_ = ie_->getBrowser();

    initializeBrowser();

    return true;
}

bool
HippoAbstractWindow::create(void)
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

HippoIE *
HippoAbstractWindow::getIE()
{
    return ie_;
}

bool
HippoAbstractWindow::registerClass()
{
    WNDCLASSEX wcex;

    ZeroMemory(&wcex, sizeof(WNDCLASSEX));
    wcex.cbSize = sizeof(WNDCLASSEX); 

    wcex.style = classStyle_;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = className_.m_str;
    if (ui_) {
        wcex.hIcon = ui_->getBigIcon();
        wcex.hIconSm = ui_->getSmallIcon();;
    }

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, className_.m_str, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoAbstractWindow::show(BOOL activate) 
{   
    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND);
    else
        ShowWindow(window_, (activate ? SW_RESTORE : SW_SHOWNOACTIVATE));

    if (updateOnShow_) 
        RedrawWindow(window_, NULL, NULL, RDW_INVALIDATE | RDW_UPDATENOW | RDW_ALLCHILDREN);
}

void
HippoAbstractWindow::hide(void)
{
    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND | AW_HIDE);
    else
        ShowWindow(window_, SW_HIDE);
}

void 
HippoAbstractWindow::setForegroundWindow()
{
    if (window_)
        SetForegroundWindow(window_);
}

void
HippoAbstractWindow::moveResize(int x, int y, int width, int height)
{
    if (x == CW_DEFAULT || y == CW_DEFAULT) {
        RECT workArea;
        int centerX = 0;
        int centerY = 0;
     
        if (::SystemParametersInfo(SPI_GETWORKAREA, 0, &workArea, 0)) {
            centerX = (workArea.left + workArea.right - width) / 2;
            centerY = (workArea.bottom + workArea.top - height) / 2;
        }
        
        if (x == CW_DEFAULT)
            x = centerX;
        if (y == CW_DEFAULT)
            y = centerY;
    }

    MoveWindow(window_, x, y, width, height, TRUE);
}

bool
HippoAbstractWindow::hookMessage(MSG *msg)
{
    if ((msg->message >= WM_KEYFIRST && msg->message <= WM_KEYLAST))
    {
        HippoPtr<IWebBrowser> browser(ie_->getBrowser());
        HippoQIPtr<IOleInPlaceActiveObject> active(ie_->getBrowser());
        HRESULT res = active->TranslateAccelerator(msg);
        return res == S_OK;
    }
    return FALSE;
}

bool
HippoAbstractWindow::processMessage(UINT   message,
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
    case WM_CLOSE:
        onClose(false);
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
HippoAbstractWindow::windowProc(HWND   window,
                            UINT   message,
                            WPARAM wParam,
                            LPARAM lParam)
{
    HippoAbstractWindow *AbstractWindow = hippoGetWindowData<HippoAbstractWindow>(window);
    if (AbstractWindow) {
        if (AbstractWindow->processMessage(message, wParam, lParam))
            return 0;
    }

    return DefWindowProc(window, message, wParam, lParam);
}
