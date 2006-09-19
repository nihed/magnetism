#include "stdafx-hippoui.h"
#include "HippoWindowWin.h"

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
    HippoCanvasItem *contents;
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

}

static void
hippo_window_win_finalize(GObject *object)
{
    /* HippoWindowWin *win = HIPPO_WINDOW_WIN(object); */


    G_OBJECT_CLASS(hippo_window_win_parent_class)->finalize(object);
}

HippoWindow*
hippo_window_win_new(void)
{
    HippoWindowWin *win;

    win = HIPPO_WINDOW_WIN(g_object_new(HIPPO_TYPE_WINDOW_WIN,
                           NULL));

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

    if (item == window_win->contents)
        return;

    if (window_win->contents) {
        g_object_unref(window_win->contents);
        window_win->contents = NULL;
    }

    if (item) {
        g_object_ref(item);
        window_win->contents = item;
    }

    // hippo_canvas_set_root(HIPPO_CANVAS(window_win->canvas), window_win->contents);
}

static void
hippo_window_win_set_visible(HippoWindow     *window,
                             gboolean         visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);


}


static void
hippo_window_win_set_position(HippoWindow     *window,
                              int              x,
                              int              y)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);


}

static void
hippo_window_win_get_size(HippoWindow     *window,
                          int             *width_p,
                          int             *height_p)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);


}

static void
hippo_window_win_set_scrollbar(HippoWindow      *window,
                               HippoOrientation  orientation,
                               gboolean          visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

}

static void
hippo_window_win_set_resize_grip(HippoWindow      *window,
                                 HippoSide         side,
                                 gboolean          visible)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);

}

static void
hippo_window_win_set_side_item(HippoWindow      *window,
                               HippoSide         side,
                               HippoCanvasItem  *item)
{
    HippoWindowWin *window_win = HIPPO_WINDOW_WIN(window);


}

