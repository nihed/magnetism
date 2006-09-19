#include "stdafx-hippoui.h"
#include "HippoWindowWin.h"
#include "HippoAbstractWindow.h"
#include "HippoUIUtil.h"

class HippoWindowImpl : public HippoAbstractWindow {
public:
    HippoWindowImpl() 
        : width_(100), height_(100), created_(false) {

        setClassName(L"HippoWindowWinClass");
        setClassStyle(CS_HREDRAW | CS_VREDRAW);
        //setWindowStyle(WS_POPUP);
        //setExtendedStyle(WS_EX_TOPMOST);
        setTitle(L"Mugshot");
    }
    
    void setContents(HippoCanvasItem *item);
    void setVisible(bool visible);
    void setPosition(int x, int y);
    void getSize(int *x_p, int *y_p);

private:
    HippoGObjectPtr<HippoCanvasItem> contents_;
    int width_;
    int height_;
    unsigned int created_;
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

void
HippoWindowImpl::setVisible(bool visible)
{
    if (visible) {
        if (!created_) {
            create();
            created_ = 1;
        }
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
