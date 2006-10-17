/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "stdafx-hippoui.h"
#include "HippoWindowWin.h"
#include "HippoAbstractControl.h"
#include "HippoUIUtil.h"
#include "HippoUI.h"
#include "HippoCanvas.h"
#include "HippoGSignal.h"

class HippoWindowImpl : public HippoAbstractControl {
public:
    HippoWindowImpl(HippoWindowWin *wrapper) {
        wrapper_ = wrapper;
        setClassName(L"HippoWindow");
        setClassStyle(CS_HREDRAW | CS_VREDRAW);
        setAppWindow(true);
        setResizeGravity(HIPPO_GRAVITY_NORTH_WEST);
        setTitle(L"Mugshot");
        setResizable(HIPPO_ORIENTATION_VERTICAL, false);
        setResizable(HIPPO_ORIENTATION_HORIZONTAL, false);
        contentsControl_ = new HippoCanvas();
        contentsControl_->Release(); // remove extra ref
        contentsControl_->setParent(this);
    }

    void setContents(HippoCanvasItem *item);
    void setVisible(bool visible);
    void hideToIcon(HippoRectangle *iconRect);
    void setAppWindow(bool appWindow);
    bool getAppWindow() { return appWindow_; }
    void getPosition(int *x_p, int *y_p);
    void getSize(int *width_p, int *height_p);
    void setResizeGravity(HippoGravity gravity);
    HippoGravity getResizeGravity() { return resizeGravity_; }
    bool getActive();
    bool getOnscreen();
    void beginMove();
    void beginResize(HippoSide side);
    void present();

    virtual void sizeAllocate(int width, int height);

protected:
    
    virtual void onSizeAllocated();
    virtual void onRequestChanged();
    
    virtual int getWidthRequestImpl();
    virtual int getHeightRequestImpl(int forWidth);

    virtual void createChildren();
    virtual void showChildren();

    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

    virtual void destroy();

private:
    HippoWindowWin *wrapper_;
    GIdle resizeIdle_;
    bool appWindow_;
    HippoGravity resizeGravity_;

    bool idleResize();
    bool windowVisibleAtPoint(POINT point);

    HippoGObjectPtr<HippoCanvasItem> contents_;
    HippoPtr<HippoCanvas> contentsControl_;
};

static void      hippo_window_win_init                (HippoWindowWin       *window_win);
static void      hippo_window_win_class_init          (HippoWindowWinClass  *klass);
static void      hippo_window_win_iface_init          (HippoWindowClass     *window_class);
static void      hippo_window_win_finalize            (GObject              *object);

static void hippo_window_win_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_window_win_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


/* Window methods */
static void     hippo_window_win_set_contents       (HippoWindow     *window,
                                                     HippoCanvasItem *item);
static void     hippo_window_win_set_visible        (HippoWindow     *window,
                                                     gboolean         visible);
static void     hippo_window_win_hide_to_icon       (HippoWindow     *window,
                                                     HippoRectangle  *icon_rect);
static void     hippo_window_win_set_position       (HippoWindow     *window,
                                                     int              x,
                                                     int              y);
static void     hippo_window_win_set_size           (HippoWindow     *window,
                                                     int              width,
                                                     int              height);
static void     hippo_window_win_get_position       (HippoWindow     *window,
                                                     int             *x,
                                                     int             *y);
static void     hippo_window_win_get_size           (HippoWindow     *window,
                                                     int             *width_p,
                                                     int             *height_p);
static void     hippo_window_win_set_resizable      (HippoWindow     *window,
                                                     HippoOrientation orientation,
                                                     gboolean         value);
static void     hippo_window_win_begin_move_drag    (HippoWindow     *window,
                                                     HippoEvent      *event);
static void     hippo_window_win_begin_resize_drag  (HippoWindow     *window,
                                                     HippoSide        side,
                                                     HippoEvent      *event);
static void     hippo_window_win_present            (HippoWindow     *window);

/* internal stuff */

struct _HippoWindowWin {
    GObject object;
    HippoWindowImpl *impl;
};

struct _HippoWindowWinClass {
    GObjectClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_APP_WINDOW,
    PROP_RESIZE_GRAVITY,
    PROP_ACTIVE,
    PROP_ONSCREEN
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowWin, hippo_window_win, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_win_iface_init));

static void
hippo_window_win_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_win_set_contents;
    window_class->set_visible = hippo_window_win_set_visible;
    window_class->hide_to_icon = hippo_window_win_hide_to_icon;
    window_class->set_position = hippo_window_win_set_position;
    window_class->set_size = hippo_window_win_set_size;
    window_class->get_position = hippo_window_win_get_position;
    window_class->get_size = hippo_window_win_get_size;
    window_class->set_resizable = hippo_window_win_set_resizable;
    window_class->begin_move_drag = hippo_window_win_begin_move_drag;
    window_class->begin_resize_drag = hippo_window_win_begin_resize_drag;
    window_class->present = hippo_window_win_present;
}

static void
hippo_window_win_class_init(HippoWindowWinClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_win_set_property;
    object_class->get_property = hippo_window_win_get_property;

    g_object_class_override_property(object_class, PROP_APP_WINDOW, "app-window");
    g_object_class_override_property(object_class, PROP_RESIZE_GRAVITY, "resize-gravity");

    // Note that we only provide getters for these, and not notification when
    // they change. We're not currently using notification in the mugshot client.
    //
    // Notification for "active" should be straightforward by watching the right
    // messages. It's less clear how to do notification for "onscreen" - you might
    // have to poll for it periodically, which would be expensive.
        
    g_object_class_override_property(object_class, PROP_ACTIVE, "active");
    g_object_class_override_property(object_class, PROP_ONSCREEN, "onscreen");

    object_class->finalize = hippo_window_win_finalize;
}

static void
hippo_window_win_init(HippoWindowWin *window_win)
{
    window_win->impl = new HippoWindowImpl(window_win);
}

static void
hippo_window_win_finalize(GObject *object)
{
    HippoWindowWin *win = HIPPO_WINDOW_WIN(object);

    win->impl->Release();

    G_OBJECT_CLASS(hippo_window_win_parent_class)->finalize(object);
}

HippoWindow*
hippo_window_win_new(HippoUI *ui)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN(g_object_new(HIPPO_TYPE_WINDOW_WIN,
                           NULL));
    win->impl->setUI(ui);

    return HIPPO_WINDOW(win);
}

static void
hippo_window_win_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN(object);

    switch (prop_id) {
    case PROP_APP_WINDOW:
        win->impl->setAppWindow(g_value_get_boolean(value) != FALSE);
        break;
    case PROP_RESIZE_GRAVITY:
        win->impl->setResizeGravity((HippoGravity)g_value_get_int(value));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_win_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN (object);

    switch (prop_id) {
    case PROP_APP_WINDOW:
        g_value_set_boolean(value, win->impl->getAppWindow() ? TRUE : FALSE);
        break;
    case PROP_RESIZE_GRAVITY:
        g_value_set_int(value, (int)win->impl->getResizeGravity());
        break;
    case PROP_ACTIVE:
        g_value_set_boolean(value, win->impl->getActive() ? TRUE : FALSE);
        break;
    case PROP_ONSCREEN:
        g_value_set_boolean(value, win->impl->getOnscreen() ? TRUE : FALSE);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_window_win_set_contents(HippoWindow     *window,
                              HippoCanvasItem *item)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setContents(item);
}

static void
hippo_window_win_set_visible(HippoWindow     *window,
                             gboolean         visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setVisible(visible != FALSE);
}

static void
hippo_window_win_hide_to_icon(HippoWindow     *window,
                              HippoRectangle  *icon_rect)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->hideToIcon(icon_rect);
}

static void
hippo_window_win_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setPosition(x, y);
}

static void
hippo_window_win_set_size(HippoWindow     *window,
                          int              width,
                          int              height)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setSize(width, height);
}

static void
hippo_window_win_get_position(HippoWindow     *window,
                              int             *x_p,
                              int             *y_p)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->getPosition(x_p, y_p);
}

static void
hippo_window_win_get_size(HippoWindow     *window,
                          int             *width_p,
                          int             *height_p)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->getSize(width_p, height_p);
}

static void
hippo_window_win_set_resizable(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          value)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    window_win->impl->setResizable(orientation, value != FALSE);
}

static void
hippo_window_win_begin_move_drag(HippoWindow      *window,
                                 HippoEvent       *event)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    
    // the coordinates in the HippoEvent are on any random canvas item,
    // so not useful.
    window_win->impl->beginMove();
}

static void
hippo_window_win_begin_resize_drag(HippoWindow      *window,
                                   HippoSide         side,
                                   HippoEvent       *event)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    
    // the coordinates in the HippoEvent are on any random canvas item,
    // so not useful.
    window_win->impl->beginResize(side);
}

static void
hippo_window_win_present(HippoWindow *window)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    window_win->impl->present();
}

void
HippoWindowImpl::destroy()
{
    contentsControl_ = NULL;
    HippoAbstractControl::destroy();
}

void
HippoWindowImpl::setContents(HippoCanvasItem *item)
{
    contents_ = item;
    contentsControl_->setRoot(item);
}

void
HippoWindowImpl::createChildren()
{
    contentsControl_->create();
}

void
HippoWindowImpl::showChildren()
{
    contentsControl_->show(false);
}

void
HippoWindowImpl::onRequestChanged()
{
#if 0
    g_debug("SIZING: adding resize idle %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

    // the resizeIdle_ only kicks in if there's no WM_PAINT pending - 
    // otherwise WM_PAINT arrives before the idle fires, and it will 
    // call ensureRequestAndAllocation() which makes the resize idle 
    // a no-op when it later runs after all the painting is over.
    resizeIdle_.add(slot(this, &HippoWindowImpl::idleResize), (G_PRIORITY_DEFAULT_IDLE + G_PRIORITY_LOW) / 2);
}

bool
HippoWindowImpl::idleResize()
{
#if 0
    g_debug("SIZING: idleResize %p %s",
        window_, HippoUStr(getClassName()).c_str());
#endif

    // this may be a no-op but will check the flag for whether we 
    // need to do anything

    ensureRequestAndAllocation();

    resizeIdle_.remove();

    return false; // remove idle
}

void
HippoWindowImpl::setVisible(bool visible)
{
    if (visible) {
        show(true);
    } else {
        hide();
    }
}

void
HippoWindowImpl::hideToIcon(HippoRectangle *iconRect)
{
    RECT fromRect;
    RECT toRect;

    GetWindowRect(window_, &fromRect);

    hide();
    
    toRect.left = iconRect->x;
    toRect.right = iconRect->x + iconRect->width;
    toRect.top = iconRect->y;
    toRect.bottom = iconRect->y + iconRect->height;

    DrawAnimatedRects(window_, IDANI_CAPTION, &fromRect, &toRect);
}

void
HippoWindowImpl::setAppWindow(bool appWindow)
{
    appWindow_ = appWindow;

    setWindowStyle(appWindow ? WS_POPUP | WS_MINIMIZEBOX :  WS_POPUP);
    setExtendedStyle(appWindow ? WS_EX_APPWINDOW : WS_EX_TOPMOST | WS_EX_NOACTIVATE);
}

void
HippoWindowImpl::getPosition(int *x_p, int *y_p)
{
    // force realization
    ensureRequestAndAllocation();

    RECT rect;
    GetWindowRect(window_, &rect);

    if (x_p)
        *x_p = rect.left;
    if (y_p)
        *y_p = rect.top;
}

void
HippoWindowImpl::getSize(int *width_p, int *height_p)
{
    // get a real size (forces realization)
    ensureRequestAndAllocation();

    if (width_p)
        *width_p = getWidth();
    if (height_p)
        *height_p = getHeight();
}

void
HippoWindowImpl::setResizeGravity(HippoGravity resizeGravity)
{
    resizeGravity_ = resizeGravity;
}

bool
HippoWindowImpl::getActive()
{
    if (!isCreated())
        return FALSE;

    // The active window isn't actually what we are interested in here, since what we
    // want to know for the Mugshot browser window is whether when the user clicks on
    // the mugshot icon we should raise the window or instead hide it. And when the
    // usre clicks on the mugshot icon, the panel is the active window. So, what
    // we check here is if our window is the top window that isn't WS_EX_TOPMOST.

    HWND top = GetTopWindow(NULL);
    while (top) {
        if (top == window_)
            return TRUE;

        WINDOWINFO windowInfo;
        memset(&windowInfo, 0, sizeof(windowInfo));
        windowInfo.cbSize = sizeof(windowInfo);
        if (!GetWindowInfo(top, &windowInfo))
            return FALSE;

        if ((windowInfo.dwExStyle & WS_EX_TOPMOST) == 0)
            return FALSE;

        top = GetNextWindow(top, GW_HWNDNEXT);
    }

    return FALSE;
}

bool
HippoWindowImpl::windowVisibleAtPoint(POINT point)
{
    HWND atPoint = WindowFromPoint(point);
    if (!atPoint)
        return FALSE;

    // The root here isn't the X root window (the entire screen), but the toplevel window
    HWND root = GetAncestor(atPoint, GA_ROOT);

    return root == window_;
}

bool
HippoWindowImpl::getOnscreen()
{
    if (!isCreated())
        return FALSE;
    
    // We consider a window to be partially visible (and hence "onscreen") if
    // any of its four corners are visible

    RECT rect;

    if (!GetWindowRect(window_, &rect))
        return FALSE;

    POINT point;

    point.x = rect.left;
    point.y = rect.top;
    if (windowVisibleAtPoint(point))
        return true;

    point.x = rect.right - 1;
    if (windowVisibleAtPoint(point))
        return true;
    
    point.y = rect.bottom - 1;
    if (windowVisibleAtPoint(point))
        return true;
     
    point.x = rect.left;
    if (windowVisibleAtPoint(point))
        return true;

    return FALSE;
}

void
HippoWindowImpl::beginMove()
{
    DefWindowProc(window_, WM_NCLBUTTONDOWN, HTCAPTION, GetMessagePos());
}

void
HippoWindowImpl::beginResize(HippoSide side)
{
    // Simulate a button click on the window frame.
    // There's an official WM_SYSCOMMAND to start a resize, but it's the 
    // keyboard resize as if you chose it from the window menu.

    WPARAM wParam;
    switch (side) {
        case HIPPO_SIDE_LEFT:
            wParam = HTLEFT;
            break;
        case HIPPO_SIDE_TOP:
            wParam = HTTOP;
            break;
        case HIPPO_SIDE_RIGHT:
            wParam = HTRIGHT;
            break;
        case HIPPO_SIDE_BOTTOM:
            wParam = HTBOTTOM;
            break;
        default:
            g_warning("bad window side");
            return;
    }

    g_debug("Starting resize of window %p", window_);

    // this is slightly bogus, since there may not be a message for 
    // GetMessagePos() to get the coords from, but I think it will work
    // for when we use it. Otherwise we probably need to add HippoWindow coords
    // to HippoEvent. It seems a bit like these coords aren't used anyway - 
    // I had them totally wrong at one point and things still worked.
    DefWindowProc(window_, WM_NCLBUTTONDOWN, wParam, GetMessagePos());
}

void
HippoWindowImpl::present()
{
    ShowWindow(window_, SW_RESTORE);
    // Apparently ShowWindow only activates a window when it is not shown. If it is
    // already showing, you need to activate it explicitly
    //
    // FIXME: The window still doesn't always end up as the active window
    SetForegroundWindow(window_);
}

void 
HippoWindowImpl::sizeAllocate(int width, int height)
{
    int x = getX();
    int y = getY();
    int oldWidth = getWidth();
    int oldHeight = getHeight();

    switch (resizeGravity_) {
    case HIPPO_GRAVITY_NORTH_WEST:
        break;
    case HIPPO_GRAVITY_NORTH_EAST:
        x -= width - oldWidth;
        break;
    case HIPPO_GRAVITY_SOUTH_EAST:
        x -= width - oldWidth;
        y -= height - oldHeight;
        break;
    case HIPPO_GRAVITY_SOUTH_WEST:
        y -= height - oldHeight;
        break;
    }

    HippoAbstractControl::sizeAllocate(x, y, width, height);
}

int
HippoWindowImpl::getWidthRequestImpl()
{
    return contentsControl_->getWidthRequest();
}

int
HippoWindowImpl::getHeightRequestImpl(int forWidth)
{
    return contentsControl_->getHeightRequest(forWidth);
}

void
HippoWindowImpl::onSizeAllocated()
{
    contentsControl_->sizeAllocate(getWidth(), getHeight());
}

bool
HippoWindowImpl::processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    if (HippoAbstractControl::processMessage(message, wParam, lParam)) {
        return true;
    }

    switch (message) {
        case WM_SETFOCUS:
            // forward focus on to our child window.
            // DefWindowProc for WM_ACTIVATE should call SetFocus on us.
            contentsControl_->setFocus();
            return true;
        case WM_SYSCOMMAND:
            if (wParam == SC_MINIMIZE) {
                g_signal_emit_by_name(wrapper_, "minimize");
                return true;
            }
            return false;
    }

    return false;
}
