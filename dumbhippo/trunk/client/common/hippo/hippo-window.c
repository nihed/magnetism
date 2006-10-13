/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-window.h"
#include "hippo-common-marshal.h"

static void     hippo_window_base_init (void                  *klass);

enum {
    MINIMIZE,
    LAST_SIGNAL
};
static int signals[LAST_SIGNAL];

GType
hippo_window_get_type(void)
{
    static GType type = 0;
    if (type == 0) {
        static const GTypeInfo info =
            {
                sizeof(HippoWindowClass),
                hippo_window_base_init,
                NULL /* base_finalize */
            };
        type = g_type_register_static(G_TYPE_INTERFACE, "HippoWindow",
                                      &info, 0);
    }

    return type;
}

static void
hippo_window_base_init(void *klass)
{
    static gboolean initialized = FALSE;

    if (!initialized) {
        signals[MINIMIZE] =
            g_signal_new ("minimize",
                          HIPPO_TYPE_WINDOW,
                          G_SIGNAL_RUN_LAST,
                          0,
                          NULL, NULL,
                          g_cclosure_marshal_VOID__VOID,
                          G_TYPE_NONE, 0);

        g_object_interface_install_property(klass,
                                            g_param_spec_boolean("app-window",
                                                                 _("App Window"),
                                                                 _("Whether the window should appear in the tasklist"),
                                                                 TRUE,
                                                                 G_PARAM_READABLE | G_PARAM_WRITABLE));
        g_object_interface_install_property(klass,
                                            g_param_spec_boolean("active",
                                                                 _("Active"),
                                                                 _("Whether the window is the currently active window"),
                                                                 FALSE,
                                                                 G_PARAM_READABLE));
        g_object_interface_install_property(klass,
                                            g_param_spec_boolean("onscreen",
                                                                 _("Onscreen"),
                                                                 _("Whether any portion of the window is visible to the user"),
                                                                 FALSE,
                                                                 G_PARAM_READABLE));

        initialized = TRUE;
    }
}

void
hippo_window_set_contents(HippoWindow     *window,
                          HippoCanvasItem *item)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));
    g_return_if_fail(item == NULL || HIPPO_IS_CANVAS_ITEM(item));

    HIPPO_WINDOW_GET_CLASS(window)->set_contents(window, item);
}

void
hippo_window_set_visible(HippoWindow     *window,
                         gboolean         visible)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_visible(window, visible);
}

void
hippo_window_hide_to_icon(HippoWindow     *window,
                          HippoRectangle  *icon_rect)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->hide_to_icon(window, icon_rect);
}

void
hippo_window_set_position(HippoWindow     *window,
                          int              x,
                          int              y)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_position(window, x, y);
}

void
hippo_window_set_size(HippoWindow     *window,
                      int              width,
                      int              height)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_size(window, width, height);
}

void
hippo_window_get_position(HippoWindow     *window,
                          int             *x_p,
                          int             *y_p)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->get_position(window, x_p, y_p);
}

void
hippo_window_get_size(HippoWindow     *window,
                      int             *width_p,
                      int             *height_p)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->get_size(window, width_p, height_p);
}

void
hippo_window_set_resizable(HippoWindow      *window,
                           HippoOrientation  orientation,
                           gboolean          value)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->set_resizable(window, orientation, value);
}

void
hippo_window_begin_move_drag (HippoWindow      *window,
                              HippoEvent       *event)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->begin_move_drag(window, event);
}

void
hippo_window_begin_resize_drag (HippoWindow      *window,
                                HippoSide         side,
                                HippoEvent       *event)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->begin_resize_drag(window, side, event);
}

void
hippo_window_present (HippoWindow *window)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    HIPPO_WINDOW_GET_CLASS(window)->present(window);
}
