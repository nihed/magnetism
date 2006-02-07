/* HippoAbstractWindow.cpp: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx.h"

#include "HippoUI.h"
#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

static const int BASE_WIDTH = 600;
static const int BASE_HEIGHT = 600;

HippoAbstractWindow::HippoAbstractWindow()
{
    animate_ = false;
    windowStyle_ = WS_OVERLAPPEDWINDOW;

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
}

void
HippoAbstractWindow::setApplication(IDispatch *application)
{
    application_ = application;
}

void 
HippoAbstractWindow::setAnimate(bool animate)
{
    animate_ = animate;
}

void 
HippoAbstractWindow::setWindowStyle(DWORD windowStyle)
{
    windowStyle_ = windowStyle;
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

bool
HippoAbstractWindow::createWindow(void)
{
    window_ = CreateWindow(className_, title_, windowStyle_,
                           CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                           NULL, NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoAbstractWindow>(window_, this);
    ui_->registerWindowMsgHook(window_, this);

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
HippoAbstractWindow::HippoAbstractWindowIECallback::onError(WCHAR *text) 
{
    abstractWindow_->ui_->debugLogW(L"HippoIE error: %s", text);
}

bool
HippoAbstractWindow::embedIE(void)
{
    RECT rect;
    GetClientRect(window_,&rect);
    ie_ = new HippoIE(ui_, window_, url_, ieCallback_, application_);
    ie_->setThreeDBorder(false);

    ie_->create();
    browser_ = ie_->getBrowser();

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

    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = windowProc;
    wcex.cbClsExtra = 0;
    wcex.cbWndExtra = 0;
    wcex.hInstance  = instance_;
    wcex.hCursor    = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = NULL;
    wcex.lpszClassName  = className_.m_str;

    if (RegisterClassEx(&wcex) == 0) {
        if (GetClassInfoEx(instance_, className_.m_str, &wcex) != 0)
            return true;
        return false;
    }
    return true;
}

void
HippoAbstractWindow::show(void) 
{   
    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND);
    else
        ShowWindow(window_, SW_SHOW);

    // Probably not really necessary
    BringWindowToTop(window_);
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
                active->OnDocWindowActivate(wParam == WA_ACTIVE);
            return true;
        }
    case WM_CLOSE:
        onClose(false);
        return true;
    case WM_SIZE:
        {
            RECT rect = { 0, 0, LOWORD(lParam), HIWORD(lParam) };
            ie_->resize(&rect);
            return true;
        }
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
