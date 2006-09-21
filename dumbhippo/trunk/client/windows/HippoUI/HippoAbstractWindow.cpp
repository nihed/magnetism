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

HIPPO_DEFINE_REFCOUNTING(HippoAbstractWindow)

HippoAbstractWindow::HippoAbstractWindow()
{
    refCount_ = 1;
    ui_ = NULL;
    animate_ = false;
    useParent_ = false;
    createWithParent_ = NULL;
    updateOnShow_ = false;
    classStyle_ = CS_HREDRAW | CS_VREDRAW;
    windowStyle_ = WS_OVERLAPPEDWINDOW;
    extendedStyle_ = 0;
    created_ = false;
    showing_ = false;
    destroyed_ = false;

    instance_ = GetModuleHandle(NULL);
    window_ = NULL;
}

HippoAbstractWindow::~HippoAbstractWindow(void)
{
    destroy();
    assert(window_ == NULL);
}

void 
HippoAbstractWindow::setUI(HippoUI *ui)
{
    ui_ = ui;
    if (ui_)
        initializeUI();
}

void
HippoAbstractWindow::setUseParent(bool useParent)
{
    useParent_ = useParent;
    if (createWithParent_)
        g_warning("can't useParent and set parent");
}

void 
HippoAbstractWindow::setCreateWithParent(HippoAbstractWindow *parent)
{
    createWithParent_ = parent;
    if (useParent_)
        g_warning("can't set parent and useParent");
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
HippoAbstractWindow::setClassName(const HippoBSTR &className)
{
    className_ = className;
}

void 
HippoAbstractWindow::setTitle(const HippoBSTR &title)
{
    title_ = title;
}

void 
HippoAbstractWindow::initializeWindow()
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

bool
HippoAbstractWindow::createWindow(void)
{
    window_ = CreateWindowEx(extendedStyle_, className_, title_, windowStyle_,
                             CW_USEDEFAULT, CW_USEDEFAULT, BASE_WIDTH, BASE_HEIGHT,
                             (useParent_ && ui_) ? ui_->getWindow() : (createWithParent_ ? createWithParent_->window_ : NULL), 
                             NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

    hippoSetWindowData<HippoAbstractWindow>(window_, this);
    ui_->registerMessageHook(window_, this);

    initializeWindow();

    return true;
}

bool
HippoAbstractWindow::create(void)
{
    if (created_)
        return true;

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

    created_ = true;

    return true;
}

void 
HippoAbstractWindow::destroy()
{
    // This method should be safe against multiple calls, since a subclass is allowed
    // to call it before our destructor runs
    if (destroyed_)
        return;

    destroyed_ = true;

    // The actual work is done in the WM_DESTROY handler, onWindowDestroyed()
    if (window_ != NULL) {
        DestroyWindow(window_);
        window_ = NULL;
    }
}

void
HippoAbstractWindow::onWindowDestroyed(void)
{
    if (window_) {
        ui_->unregisterMessageHook(window_);
        hippoSetWindowData<HippoAbstractWindow>(window_, NULL);

        window_ = NULL;
    }
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
    if (showing_)
        return;

    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND);
    else
        ShowWindow(window_, (activate ? SW_RESTORE : SW_SHOWNOACTIVATE));

    if (updateOnShow_) 
        RedrawWindow(window_, NULL, NULL, RDW_INVALIDATE | RDW_UPDATENOW | RDW_ALLCHILDREN);

    showing_= true;
}

void
HippoAbstractWindow::hide(void)
{
    if (!showing_)
        return;

    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND | AW_HIDE);
    else
        ShowWindow(window_, SW_HIDE);

    showing_ = false;
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

void
HippoAbstractWindow::move(int x, int y)
{
    moveResize(x, y, getWidth(), getHeight());
}

void
HippoAbstractWindow::resize(int width, int height)
{
    RECT area;
    if (!GetWindowRect(window_, &area)) {
        return;
    }
    moveResize(area.left, area.top, width, height);
}

// InvalidateRect queues a WM_PAINT, while UpdateRect sends WM_PAINT synchronously
void
HippoAbstractWindow::invalidate(int x, int y, int width, int height)
{
    RECT area;

    area.left = x;
    area.top = y;
    area.right = area.left + width;
    area.bottom = area.top + height;

    // false = don't clear the area
    InvalidateRect(window_, &area, false);
}

int
HippoAbstractWindow::getWidth() 
{
    if (!window_)
        return 0;

    RECT area;
    if (!GetWindowRect(window_, &area)) {
        g_warning("failed to get window rect");
        return 0;
    }
    return area.right - area.left;
}

int
HippoAbstractWindow::getHeight()
{
    if (!window_)
        return 0;

    RECT area;
    if (!GetWindowRect(window_, &area)) {
        g_warning("failed to get window rect");
        return 0;
    }
    return area.bottom - area.top;
}

bool
HippoAbstractWindow::hookMessage(MSG *msg)
{

    return false;
}

bool
HippoAbstractWindow::processMessage(UINT   message,
                                    WPARAM wParam,
                                    LPARAM lParam)
{
    switch (message) 
    {
    case WM_CLOSE:
        onClose(false);
        return true;
    case WM_DESTROY:
        onWindowDestroyed();
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
    // Our only content is the IE browser, and it erases everything itself
    // on repaint, so we tell windows not to repaint by returning 1. This
    // prevents flicker on resize.
    if (message == WM_ERASEBKGND)
        return 1;

    HippoAbstractWindow *abstractWindow = hippoGetWindowData<HippoAbstractWindow>(window);
    if (abstractWindow) {
        abstractWindow->AddRef();
        if (abstractWindow->processMessage(message, wParam, lParam)) {
            abstractWindow->Release();
            return 0;
        } else {
            abstractWindow->Release();
        }
    }

    return DefWindowProc(window, message, wParam, lParam);
}
