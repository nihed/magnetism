#include "stdafx-hippoui.h"
#include "HippoWindowWin.h"
#include "HippoAbstractWindow.h"
#include "HippoUIUtil.h"

class HippoCanvasWindow : public HippoAbstractWindow {
public:
    HippoCanvasWindow(HippoAbstractWindow *parent) 
        : width_(25), height_(25), created_(false) {

        setClassName(L"HippoCanvasWindowClass");
        setClassStyle(CS_HREDRAW | CS_VREDRAW);
        setWindowStyle(WS_CHILD);
        //setExtendedStyle(WS_EX_TOPMOST);
        setTitle(L"Canvas");
        setParent(parent);
    }

    virtual bool create();

    void setRoot(HippoCanvasItem *item);

    int getWidth() { return width_; }
    int getHeight() { return height_; }

protected:
    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

private:
    HippoGObjectPtr<HippoCanvasItem> root_;
    int width_;
    int height_;
    unsigned int created_ : 1;
};

class HippoWindowImpl : public HippoAbstractWindow {
public:
    HippoWindowImpl() 
        : width_(100), height_(100) {

        setClassName(L"HippoWindowWinClass");
        setClassStyle(CS_HREDRAW | CS_VREDRAW);
        //setWindowStyle(WS_POPUP);
        //setExtendedStyle(WS_EX_TOPMOST);
        setTitle(L"Mugshot");
        contentsWindow_ = new HippoCanvasWindow(this);
    }

    ~HippoWindowImpl() {
        delete contentsWindow_;
    }

    virtual void show(bool activate);

    void setContents(HippoCanvasItem *item);
    void setVisible(bool visible);
    void setPosition(int x, int y);
    void getSize(int *x_p, int *y_p);

    virtual bool create();

protected:
    bool processMessage(UINT   message,
                        WPARAM wParam,
                        LPARAM lParam);

private:
    HippoGObjectPtr<HippoCanvasItem> contents_;
    HippoCanvasWindow *contentsWindow_;
    int width_;
    int height_;
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
static void     hippo_window_win_set_position       (HippoWindow     *window,
                                                     int              x,
                                                     int              y);
static void     hippo_window_win_get_size           (HippoWindow     *window,
                                                     int             *width_p,
                                                     int             *height_p);
static void     hippo_window_win_set_scrollbar      (HippoWindow     *window,
                                                     HippoOrientation orientation,
                                                     gboolean         visible);
static void     hippo_window_win_set_resize_grip    (HippoWindow     *window,
                                                     HippoSide        side,
                                                     gboolean         visible);
static void     hippo_window_win_set_side_item      (HippoWindow      *window,
                                                     HippoSide         side,
                                                     HippoCanvasItem  *item);

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
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoWindowWin, hippo_window_win, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_WINDOW, hippo_window_win_iface_init));

static void
hippo_window_win_iface_init(HippoWindowClass *window_class)
{
    window_class->set_contents = hippo_window_win_set_contents;
    window_class->set_visible = hippo_window_win_set_visible;
    window_class->set_position = hippo_window_win_set_position;
    window_class->get_size = hippo_window_win_get_size;
    window_class->set_scrollbar = hippo_window_win_set_scrollbar;
    window_class->set_resize_grip = hippo_window_win_set_resize_grip;
    window_class->set_side_item = hippo_window_win_set_side_item;
}

static void
hippo_window_win_class_init(HippoWindowWinClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_window_win_set_property;
    object_class->get_property = hippo_window_win_get_property;

    object_class->finalize = hippo_window_win_finalize;
}

static void
hippo_window_win_init(HippoWindowWin *window_win)
{
    window_win->impl = new HippoWindowImpl();
}

static void
hippo_window_win_finalize(GObject *object)
{
    HippoWindowWin *win = HIPPO_WINDOW_WIN(object);

    delete win->impl;

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
hippo_window_win_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);
    window_win->impl->setPosition(x, y);
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
hippo_window_win_set_scrollbar(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    // FIXME
}

static void
hippo_window_win_set_resize_grip(HippoWindow      *window,
                                 HippoSide         side,
                                 gboolean          visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    // FIXME
}

static void
hippo_window_win_set_side_item(HippoWindow      *window,
                               HippoSide         side,
                               HippoCanvasItem  *item)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

    // FIXME
}

void
HippoWindowImpl::setContents(HippoCanvasItem *item)
{
    contents_ = item;

    // hippo_canvas_set_root(HIPPO_CANVAS(canvas), contents);
}

bool
HippoWindowImpl::create()
{
    bool result;

    // we need to create the parent window first
    result = HippoAbstractWindow::create();

    contentsWindow_->create();

    return result;
}

void
HippoWindowImpl::show(bool activate)
{
    contentsWindow_->show(false);
    HippoAbstractWindow::show(activate);
}

void
HippoWindowImpl::setVisible(bool visible)
{
    if (visible) {
        create();
        show(false);
    } else {
        hide();
    }
}

void
HippoWindowImpl::setPosition(int x, int y)
{
    moveResize(x, y, width_, height_);
}

void
HippoWindowImpl::getSize(int *width_p, int *height_p)
{
    if (width_p)
        *width_p = width_;
    if (height_p)
        *height_p = height_;
}

bool 
HippoWindowImpl::processMessage(UINT   message,
                                WPARAM wParam,
                                LPARAM lParam)
{
    switch (message) {
        case WM_PAINT:
            RECT region;
            if (GetUpdateRect(window_, &region, true)) {
                PAINTSTRUCT paint;
                HDC hdc = BeginPaint(window_, &paint);

                FillRect(hdc, &region, (HBRUSH) (COLOR_WINDOW+1));

                TextOut(hdc, 0, 0, L"Hello, Windows!", 15);

                EndPaint(window_, &paint);
            }
            return true;
        case WM_SIZE:
            width_ = LOWORD(lParam);
            height_ = HIWORD(lParam);
            return true;
    }

    return HippoAbstractWindow::processMessage(message, wParam, lParam);
}

void
HippoCanvasWindow::setRoot(HippoCanvasItem *item)
{
    root_ = item;
}

bool
HippoCanvasWindow::create()
{
    return HippoAbstractWindow::create();
}

bool 
HippoCanvasWindow::processMessage(UINT   message,
                                  WPARAM wParam,
                                  LPARAM lParam)
{
    switch (message) {
        case WM_PAINT:
            RECT region;
            if (GetUpdateRect(window_, &region, true)) {
                PAINTSTRUCT paint;
                HDC hdc = BeginPaint(window_, &paint);

                FillRect(hdc, &region, (HBRUSH) (COLOR_WINDOW+2));

                TextOut(hdc, 0, 0, L"Canvas!", 15);

                EndPaint(window_, &paint);
            }
            return true;
        case WM_SIZE:
            width_ = LOWORD(lParam);
            height_ = HIWORD(lParam);
            return true;
    }

    return HippoAbstractWindow::processMessage(message, wParam, lParam);
}
