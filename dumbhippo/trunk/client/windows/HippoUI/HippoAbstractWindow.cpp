/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoAbstractWindow.cpp: Base class for toplevel windows that embed a web browser control
 *
 * Copyright Red Hat, Inc. 2005
 */
#include "stdafx-hippoui.h"

#include "HippoUI.h"
#include <HippoUtil.h>
#include "HippoAbstractWindow.h"

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
    windowStyle_ = 0;
    setWindowStyle(WS_OVERLAPPEDWINDOW); // setWindowStyle does some magic
    extendedStyle_ = 0;
    created_ = false;
    showing_ = false;
    destroyed_ = false;

    x_ = 0;
    y_ = 0;
    width_ = 0;
    height_ = 0;

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
    // essentially our whole setup assumes the clipchildren/clipsiblings so just add them here
    windowStyle_ = windowStyle | WS_CLIPCHILDREN | WS_CLIPSIBLINGS;
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

/* wrapper that takes a HippoRectangle */
static void
hippoAdjustWindowRectEx(HippoRectangle *rect,
                        DWORD dwStyle,
                        BOOL bMenu,
                        DWORD dwExStyle)
{
    RECT wrect;

#if 0
    if (dwStyle & (WS_OVERLAPPED | WS_OVERLAPPEDWINDOW | WS_TILED | WS_TILEDWINDOW)) {
        // MSDN docs on AdjustWindowRectEx say WS_OVERLAPPED isn't allowed but they don't 
        // say why or what to do instead or what happens if you use it. It seems to work,
        // more or less.
    }
#endif

    if (dwStyle & (WS_HSCROLL | WS_VSCROLL)) {
        g_warning("AdjustWindowRectEx does not handle WS_HSCROLL/VSCROLL, have to get the system metric for scrollbar size and add it in");
    }

    wrect.left = rect->x;
    wrect.top = rect->y;
    wrect.right = rect->x + rect->width;
    wrect.bottom = rect->y + rect->height;

    if (AdjustWindowRectEx(&wrect, dwStyle, bMenu, dwExStyle) == 0) {
        g_warning("Failed to convert client rect to window rect");
    }

    hippo_rectangle_from_rect(rect, &wrect);
}

void
HippoAbstractWindow::convertClientRectToWindowRect(HippoRectangle *rect)
{
    HippoRectangle adjusted = *rect;
    hippoAdjustWindowRectEx(&adjusted, windowStyle_, false, extendedStyle_);

#if 0
    g_debug("SIZING: adjusted client rect %d,%d %dx%d to window rect %d,%d %dx%d",
        rect->x, rect->y, rect->width, rect->height,
        adjusted.x, adjusted.y, adjusted.width, adjusted.height);
#endif

    *rect = adjusted;
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
#if 0
    /* These functions are no good because GetClientRect
     * returns the rect in client coords (i.e. the origin
     * is always 0,0)
     */
    if (!GetWindowRect(parent, &parentWindowArea))
        g_warning("Failed to get parent's window rect");
    if (!GetClientRect(parent, &parentClientArea))
        g_warning("Failed to get parent's client rect");
    if (!GetWindowRect(window_, &windowArea))
        g_warning("Failed to get window rect");
    if (!GetClientRect(window_, &clientArea))
        g_warning("Failed to get client rect");
#endif

    WINDOWINFO parentInfo;
    WINDOWINFO childInfo;

    parentInfo.cbSize = sizeof(parentInfo);
    childInfo.cbSize = sizeof(childInfo);

    if (!GetWindowInfo(parent, &parentInfo))
        g_warning("Failed to get parent window info");

    if (!GetWindowInfo(window_, &childInfo))
        g_warning("Failed to get child window info");

    parentWindowArea = parentInfo.rcWindow;
    parentClientArea = parentInfo.rcClient;
    windowArea = childInfo.rcWindow;
    clientArea = childInfo.rcClient;

#if 0
    g_debug("SIZING: queried current parent window %d,%d %dx%d client %d,%d %dx%d child window %d,%d %dx%d client %d,%d %dx%d",
        parentWindowArea.left,
        parentWindowArea.top,
        parentWindowArea.right - parentWindowArea.left,
        parentWindowArea.bottom - parentWindowArea.top,
        parentClientArea.left,
        parentClientArea.top,
        parentClientArea.right - parentClientArea.left,
        parentClientArea.bottom - parentClientArea.top,
        windowArea.left,
        windowArea.top,
        windowArea.right - windowArea.left,
        windowArea.bottom - windowArea.top,
        clientArea.left,
        clientArea.top,
        clientArea.right - clientArea.left,
        clientArea.bottom - clientArea.top);
#endif

    // We want to return a child client rect relative to the parent's 
    // client rect. The WINDOWINFO rects are all in screen coordinates.

    rect->x = clientArea.left - parentClientArea.left;
    rect->y = clientArea.top - parentClientArea.top;
    rect->width = clientArea.right - clientArea.left;
    rect->height = clientArea.bottom - clientArea.top;
}

bool
HippoAbstractWindow::createWindow(void)
{
    if (!defaultPositionSet_ && (windowStyle_ & WS_OVERLAPPEDWINDOW)) {
        RECT workArea;
        int centerX = 0;
        int centerY = 0;
     
        if (::SystemParametersInfo(SPI_GETWORKAREA, 0, &workArea, 0)) {
            centerX = (workArea.left + workArea.right - getWidth()) / 2;
            centerY = (workArea.bottom + workArea.top - getHeight()) / 2;
        }
        
#if 0
        g_debug("SIZING: work area %d,%d %dx%d centering window at %d,%d",
            workArea.left, workArea.top, workArea.right - workArea.left, 
            workArea.bottom - workArea.top,
            centerX, centerY);
#endif

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
#if 0
    g_debug("SIZING: creating window at window rect %d,%d %dx%d client rect %d,%d %dx%d",
        rect.x, rect.y, rect.width, rect.height,
        x_, y_, width_, height_);
#endif

#if 0
    g_debug("Create window style %x WS_OVERLAPPEDWINDOW=%x WS_VISIBLE=%x WS_DISABLED=%x WS_CHILD=%x WS_POPUP=%x WS_CAPTION=%x WS_SYSMENU=%x WS_MINIMIZEBOX=%x",
        windowStyle_,
        windowStyle_ & WS_OVERLAPPEDWINDOW, windowStyle_ & WS_VISIBLE,
        windowStyle_ & WS_DISABLED, windowStyle_ & WS_CHILD, windowStyle_ & WS_POPUP,
        windowStyle_ & WS_CAPTION,
        windowStyle_ & WS_SYSMENU,
        windowStyle_ & WS_MINIMIZEBOX);
#endif

    window_ = CreateWindowEx(extendedStyle_, className_, title_, windowStyle_,
        rect.x, rect.y, rect.width, rect.height,
        (useParent_ && ui_) ? ui_->getWindow() : (createWithParent_ ? createWithParent_->window_ : NULL), 
        NULL, instance_, NULL);
    if (!window_) {
        hippoDebugLastErr(L"Couldn't create window!");
        return false;
    }

    {
        // Sanity check
        HippoRectangle actual;
        HippoRectangle believed;
        getClientArea(&believed);
        queryCurrentClientRect(&actual);
        if (!hippo_rectangle_equal(&believed, &actual)) {
            g_warning("window class %s not created with expected dimensions, actual %d,%d %dx%d believed %d,%d %dx%d",
                HippoUStr(getClassName()).c_str(),
                actual.x, actual.y, actual.width, actual.height,
                believed.x, believed.y, believed.width, believed.height);
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

#if 0
    g_debug("SIZING: create %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

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

#if 0
    g_debug("SIZING: show %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

    if (!create())
        return;

    if (animate_)
        AnimateWindow(window_, 400, AW_BLEND);
    else
        ShowWindow(window_, (activate ? SW_RESTORE : SW_SHOWNOACTIVATE));

    if (updateOnShow_) 
        RedrawWindow(window_, NULL, NULL, RDW_INVALIDATE | RDW_UPDATENOW | RDW_ALLCHILDREN);

    showing_= true;

#if 0
    g_debug("SIZING: visible=%d for %p %s",
        IsWindowVisible(window_),
        window_, HippoUStr(getClassName()).c_str());
#endif
}

void
HippoAbstractWindow::hide(void)
{
    if (!showing_)
        return;

#if 0
    g_debug("SIZING: hide %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

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
HippoAbstractWindow::setSize(int width, int height)
{
    if (created_) {
        // We rely on the same code path as a user resize in this case
        
        SetWindowPos(window_,
                     HWND_TOP, // insertAfter: ignored
                     0, 0, // x, y: ignored
                     width, height,
                     SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOOWNERZORDER);
    } else {
        width_ = width;
        height_ = height;
    }
}

void
HippoAbstractWindow::moveResizeWindow(int x, int y, int width, int height)
{
    x_ = x;
    y_ = y;
    width_ = width;
    height_ = height;

    if (window_) {
        HippoRectangle rect;
        getClientArea(&rect);
        convertClientRectToWindowRect(&rect);
#if 0
        g_debug("SIZING: MoveWindow to %d,%d %dx%d (window rect %d,%d %dx%d)",
            x, y, width, height, rect.x, rect.y, rect.width, rect.height);
#endif
        MoveWindow(window_, rect.x, rect.y, rect.width, rect.height, TRUE);
    } else {
        g_debug("SIZING: MoveWindow to %d,%d %dx%d prior to window create",
            x, y, width, height);
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

#if 0
    g_debug("SIZING: invalidating %d,%d %dx%d on %p %s",
        x, y, width, height,
        window_, HippoUStr(getClassName()).c_str());
#endif

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

    return false;
}

static void
debugPrintMessage(HippoAbstractWindow *abstractWindow,
                  HWND   window,
                  UINT   message,
                  WPARAM wParam,
                  LPARAM lParam)
{
#if 0
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
    case WM_GETMINMAXINFO:
        {
            MINMAXINFO *mmi = (MINMAXINFO*) lParam;
            g_debug("SIZING: MINMAXINFO defaults maximized %d,%d %dx%d, min track size %dx%d",
                mmi->ptMaxPosition.x, mmi->ptMaxPosition.y,
                mmi->ptMaxSize.x, mmi->ptMaxSize.y, 
                mmi->ptMinTrackSize.x, mmi->ptMinTrackSize.y);
        }
        break;
    case WM_CREATE:
        g_debug("WM_CREATE");
        break;
    case WM_NCCREATE:
        g_debug("WM_NCCREATE");
        break;
    case WM_NCCALCSIZE:
        g_debug("WM_NCCALCSIZE");
        break;
    case WM_NCDESTROY:
        g_debug("WM_NCDESTROY");
        break;
    case WM_DESTROY:
        g_debug("WM_DESTROY");
        break;
    case WM_LBUTTONDOWN:
        g_debug("WM_LBUTTONDOWN");
        break;
    case WM_MOUSEMOVE:
        //g_debug("WM_MOUSEMOVE");
        break;
    case WM_NCMOUSEMOVE:
        //g_debug("WM_NCMOUSEMOVE");
        break;
    case WM_NCHITTEST:
        g_debug("WM_NCHITTEST");
        break;
    case WM_SETCURSOR:
        g_debug("WM_SETCURSOR");
        break;
    case WM_MOUSELEAVE:
        g_debug("WM_MOUSELEAVE");
        break;
    case WM_NCMOUSELEAVE:
        g_debug("WM_NCMOUSELEAVE");
        break;
    case WM_PARENTNOTIFY:
        g_debug("WM_PARENTNOTIFY");
        break;
    default:
        g_debug("unknown window message 0x%X (look in winuser.h - go to definition of any WM_* in visual studio)", message);
        break;
    }
#endif
}

LRESULT CALLBACK 
HippoAbstractWindow::windowProc(HWND   window,
                                UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    HippoAbstractWindow *abstractWindow = hippoGetWindowData<HippoAbstractWindow>(window);

    debugPrintMessage(abstractWindow, window, message, wParam, lParam);

    bool runDefault = true;
    LRESULT result = 0;
    if (abstractWindow) {
        switch (message) {
            case WM_ERASEBKGND:
                // Our only content is the IE browser or canvas, which erase/repaint on their 
                // own, so we return 1 to keep windows from erasing and causing flicker
                result = 1;
                runDefault = false;
                break;
            default:
                abstractWindow->AddRef();
                runDefault = ! abstractWindow->processMessage(message, wParam, lParam);
                abstractWindow->Release();
                break;
        }
    } else {
        // If there's no abstractWindow but we were called, this must be one 
        // of our window classes (not a system class) and we should be inside
        // CreateWindowEx.
        switch (message) {
            case WM_GETMINMAXINFO:
                {
                    runDefault = false;
                    // If we don't do this, Windows will impose a min size
                    // on windows with decorations
                    // (quite possibly we should accept that min size, but 
                    // for debugging purposes it's confusing)
                    MINMAXINFO *mmi = (MINMAXINFO*) lParam;
                    mmi->ptMinTrackSize.x = 0;
                    mmi->ptMinTrackSize.y = 0;
                }
                break;
        }
    }

    if (runDefault)
        result = DefWindowProc(window, message, wParam, lParam);

    //g_debug("          message %d runDefault %d result %d", message, runDefault, result);

    return result;
}
