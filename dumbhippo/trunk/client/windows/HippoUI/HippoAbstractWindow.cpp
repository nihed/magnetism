/* HippoAbstractWindow.cpp: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoUI.h"
#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

static const int DEFAULT_WIDTH = 600;
static const int DEFAULT_HEIGHT = 600;

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

    x_ = 0;
    y_ = 0;
    width_ = DEFAULT_WIDTH;
    height_ = DEFAULT_HEIGHT;

    defaultPositionSet_ = false;

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

// FIXME I think this may be wrong since these flags are multiple flags in one macro, and some of the component
// flags may also be in WS_POPUP
#define IS_NORMAL_TOPLEVEL(windowStyle) (((windowStyle) & (WS_OVERLAPPED | WS_OVERLAPPEDWINDOW | WS_TILED | WS_TILEDWINDOW)) != 0)

void
HippoAbstractWindow::convertClientRectToWindowRect(HippoRectangle *rect)
{
    RECT wrect;

    wrect.left = rect->x;
    wrect.top = rect->y;
    wrect.right = rect->x + rect->width;
    wrect.bottom = rect->y + rect->height;

    if (IS_NORMAL_TOPLEVEL(windowStyle_)) {
        // isn't this just when AdjustWindowRectEx would be useful? must be missing something
        g_warning("AdjustWindowRectEx doesn't like WS_OVERLAPPED according to MSDN docs");
    }

    if (windowStyle_ & (WS_HSCROLL | WS_VSCROLL)) {
        g_warning("convertClientRectToWindowRect() does not handle WS_HSCROLL/VSCROLL");
    }

    if (AdjustWindowRectEx(&wrect, windowStyle_,
        false, extendedStyle_) == 0) {
        g_warning("Failed to convert client rect to window rect");
    }

    rect->x = wrect.left;
    rect->y = wrect.top;
    rect->width = wrect.right - wrect.left;
    rect->height = wrect.bottom - wrect.top;
}

// ask Windows for current window geometry, returning client rect in parent window 
// coordinates (screen coordinates for toplevels), similar to how window geometry 
// works on Windows
void
HippoAbstractWindow::queryCurrentClientRect(HippoRectangle *rect)
{
    // this function would be slow and racy under X but I think is fine 
    // with Windows since everything is synchronous ...

    // we ask Windows for the parent HWND instead of using parent_, more robust
    // for this purpose
    HWND parent = GetAncestor(window_, GA_PARENT);
    if (parent == NULL) {
        g_warning("Window has no parent?");
        return;
    }
    RECT parentWindowArea;
    RECT parentClientArea;
    RECT clientArea;
    RECT windowArea;
    if (!GetWindowRect(parent, &parentWindowArea))
        g_warning("Failed to get parent's window rect");
    if (!GetClientRect(parent, &parentClientArea))
        g_warning("Failed to get parent's client rect");
    if (!GetWindowRect(window_, &windowArea))
        g_warning("Failed to get window rect");
    if (!GetClientRect(window_, &clientArea))
        g_warning("Failed to get client rect");

    // The window rects are always in screen coordinates, while the 
    // client rects are always relative to the window rects.
    // We want to return a child client rect relative to the parent's 
    // client rect.

    rect->x = windowArea.left + clientArea.left - (parentWindowArea.left + parentClientArea.left);
    rect->y = windowArea.top + clientArea.top - (parentWindowArea.top + parentClientArea.top);
    rect->width = clientArea.right - clientArea.left;
    rect->height = clientArea.bottom - clientArea.top;
}

bool
HippoAbstractWindow::createWindow(void)
{
    if (!defaultPositionSet_ && IS_NORMAL_TOPLEVEL(windowStyle_)) {
        RECT workArea;
        int centerX = 0;
        int centerY = 0;
     
        if (::SystemParametersInfo(SPI_GETWORKAREA, 0, &workArea, 0)) {
            centerX = (workArea.left + workArea.right - getWidth()) / 2;
            centerY = (workArea.bottom + workArea.top - getHeight()) / 2;
        }
        
        x_ = centerX;
        y_ = centerY;
    }

    HippoRectangle rect;
    getClientArea(&rect);
    convertClientRectToWindowRect(&rect);

    /* from this point on, x/y/width/height are defined to match the 
     * actual x/y/width/height we've set on window_.
     */
    
    /* Note that WM_SIZE is 
     * sent right away but we ignore it since we haven't set ourselves
     * as window data.
     */

    window_ = CreateWindowEx(extendedStyle_, className_, title_, windowStyle_,
        rect.x, rect.y, rect.width, rect.height,
        (useParent_ && ui_) ? ui_->getWindow() : (createWithParent_ ? createWithParent_->window_ : NULL), 
        NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    {
        // right now this will happen with any WS_OVERLAPPEDWINDOW since 
        // AdjustWindowRectEx doesn't work on those (or conceivably because
        // of a bug in our code) ... if it's not our bug, the simplest
        // solution is probably to just remove the warning here, though
        // it's a bit gross
        HippoRectangle actual;
        queryCurrentClientRect(&actual);
        if (!hippo_rectangle_equal(&rect, &actual)) {
            g_warning("window class %s not created with expected dimensions",
                HippoUStr(getClassName()).c_str());
            // Fix up and try to continue
            x_ = actual.x;
            y_ = actual.y;
            width_ = actual.width;
            height_ = actual.height;
        }
    }

    //EnableScrollBar(window_, SB_BOTH, ESB_DISABLE_BOTH);

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

    g_debug("SIZING: create %p %s",
        window_, HippoUStr(getClassName()).c_str());

    g_assert(window_ == NULL);

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
    
    // note that the class may be a predefined Windows control class,
    // and that RegisterClassEx does NOT fail if the class is already
    // registered

    HippoUStr uName(className_);
    if (GetClassInfoEx(instance_, className_.m_str, &wcex) != 0) {
        g_debug("Got existing window class %s", uName.c_str());
        return true;
    } else if (GetClassInfoEx(NULL, className_.m_str, &wcex) != 0) {
        g_debug("Got existing system window class %s", uName.c_str());
        return true;
    } else {
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
            HippoBSTR err;
            hippoHresultToString(GetLastError(), err);
            g_warning("Failed to register window class and failed to get existing class %s: %s", uName.c_str(),
                HippoUStr(err).c_str());
            return false;
        } else {
            g_debug("Registered new window class %s", uName.c_str());
            return true;
        }
    }
}

void
HippoAbstractWindow::show(BOOL activate) 
{
    if (showing_)
        return;

    g_debug("SIZING: show %p %s",
        window_, HippoUStr(getClassName()).c_str());

    if (!create())
        return;

    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND);
    else
        ShowWindow(window_, (activate ? SW_RESTORE : SW_SHOWNOACTIVATE));

    if (updateOnShow_) 
        RedrawWindow(window_, NULL, NULL, RDW_INVALIDATE | RDW_UPDATENOW | RDW_ALLCHILDREN);

    showing_= true;

    g_debug("SIZING: visible=%d for %p %s",
        IsWindowVisible(window_),
        window_, HippoUStr(getClassName()).c_str());
}

void
HippoAbstractWindow::hide(void)
{
    if (!showing_)
        return;

    g_debug("SIZING: hide %p %s",
        window_, HippoUStr(getClassName()).c_str());

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
HippoAbstractWindow::setDefaultSize(int width, int height)
{
    g_return_if_fail(!created_);

    width_ = width;
    height_ = height;
}

void
HippoAbstractWindow::setDefaultPosition(int x, int y)
{
    g_return_if_fail(!created_);

    x_ = x;
    y_ = y;
    defaultPositionSet_ = true;
}

void
HippoAbstractWindow::setPosition(int x, int y)
{
    if (created_) {
        // we rely on this generating a WM_MOVE which ends up
        // causing a sizeAllocate with the new position for 
        // HippoAbstractControl
        moveResizeWindow(x, y, getWidth(), getHeight());
    } else {
        setDefaultPosition(x, y);
    }
}

void
HippoAbstractWindow::moveResizeWindow(int x, int y, int width, int height)
{
    if (window_) {
        x_ = x;
        y_ = y;
        width_ = width;
        height_ = height;
        HippoRectangle rect;
        getClientArea(&rect);
        convertClientRectToWindowRect(&rect);
        g_debug("SIZING: MoveWindow to %d,%d %dx%d (window rect %d,%d %dx%d)",
            x, y, width, height, rect.x, rect.y, rect.width, rect.height);
        MoveWindow(window_, rect.x, rect.y, rect.width, rect.height, TRUE);
    } else {
        g_warning("no-op moveResizeWindow on not-created window");
    }
}

void
HippoAbstractWindow::onMoveResizeMessage(const HippoRectangle *newClientArea)
{
    // just a callback, should be a no-op here in base class
}

// InvalidateRect queues a WM_PAINT, while UpdateRect sends WM_PAINT synchronously
void
HippoAbstractWindow::invalidate(int x, int y, int width, int height)
{
    if (!window_)
        return;

    RECT area;

    area.left = x;
    area.top = y;
    area.right = area.left + width;
    area.bottom = area.top + height;

    g_debug("SIZING: invalidating %d,%d %dx%d on %p %s",
        x, y, width, height,
        window_, HippoUStr(getClassName()).c_str());

    // false = don't clear the area
    InvalidateRect(window_, &area, false);
}

void
HippoAbstractWindow::getClientArea(HippoRectangle *rect)
{
    rect->x = x_;
    rect->y = y_;
    rect->width = width_;
    rect->height = height_;
}

int
HippoAbstractWindow::getX()
{
    return x_;
}

int
HippoAbstractWindow::getY()
{
    return y_;
}

int
HippoAbstractWindow::getWidth() 
{
    return width_;
}

int
HippoAbstractWindow::getHeight()
{
    return height_;
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
    case WM_SIZE:
    case WM_MOVE:
        {
            HippoRectangle newRect;
            queryCurrentClientRect(&newRect);
            onMoveResizeMessage(&newRect);
        }
        return false; // for e.g. standard Windows controls we want to run DefWindowProc() ?
    default:
        return false;
    }
}

static void
debugPrintMessage(HippoAbstractWindow *abstractWindow,
                  HWND   window,
                  UINT   message,
                  WPARAM wParam,
                  LPARAM lParam)
{
    HippoUStr name;
    if (abstractWindow)
        name.setUTF16(abstractWindow->getClassName().m_str);
    else
        name.setUTF16(L"<unknown>");

    switch (message) {
    case WM_SIZE:
        g_debug("SIZING: WM_SIZE on %p %s", window, name.c_str());
        break;
    case WM_PAINT:
        g_debug("SIZING: WM_PAINT on %p %s", window, name.c_str());
        break;
    case WM_ERASEBKGND:
        g_debug("SIZING: WM_ERASEBKGND on %p %s", window, name.c_str());
        break;
    case WM_MOVE:
        g_debug("SIZING: WM_MOVE on %p %s", window, name.c_str());
        break;
    }
}

LRESULT CALLBACK 
HippoAbstractWindow::windowProc(HWND   window,
                                UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    HippoAbstractWindow *abstractWindow = hippoGetWindowData<HippoAbstractWindow>(window);

    debugPrintMessage(abstractWindow, window, message, wParam, lParam);

    // Our only content is the IE browser or canvas, which erase/repaint on their 
    // own, so we return 1 to keep windows from erasing and causing flicker
    bool runDefault = true;
    LRESULT result = 0;
    if (abstractWindow) {
        switch (message) {
            case WM_ERASEBKGND:
                result = 1;
                runDefault = false;
                break;
            default:
                abstractWindow->AddRef();
                runDefault = abstractWindow->processMessage(message, wParam, lParam);
                abstractWindow->Release();
                break;
        }
    }

    if (runDefault)
        result = DefWindowProc(window, message, wParam, lParam);

    //g_debug("          message %d runDefault %d result %d", message, runDefault, result);

    return result;
}
