/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-window.h"
#include <hippo/hippo-common-marshal.h>

static void     hippo_window_base_init (void                  *klass);

enum {
    LAST_SIGNAL
};
/* static int signals[LAST_SIGNAL]; */

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
        /* create signals in here */

        initialized = TRUE;
    }
}

void
hippo_window_set_contents(HippoWindow     *window,
                          HippoCanvasItem *item)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));
    g_return_if_fail(item == NULL || HIPPO_IS_CANVAS_ITEM(item));

    return HIPPO_WINDOW_GET_CLASS(window)->set_contents(window, item);
}

void
hippo_window_set_visible(HippoWindow     *window,
                         gboolean         visible)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    return HIPPO_WINDOW_GET_CLASS(window)->set_visible(window, visible);
}

void
hippo_window_set_position(HippoWindow     *window,
                          int              x,
                          int              y)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    return HIPPO_WINDOW_GET_CLASS(window)->set_position(window, x, y);
}

void
hippo_window_get_size(HippoWindow     *window,
                      int             *width_p,
                      int             *height_p)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    return HIPPO_WINDOW_GET_CLASS(window)->get_size(window, width_p, height_p);
}

void
hippo_window_set_scrollbar(HippoWindow      *window,
                           HippoOrientation  orientation,
                           gboolean          visible)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    return HIPPO_WINDOW_GET_CLASS(window)->set_scrollbar(window, orientation, visible);
}

void
hippo_window_set_resize_grip(HippoWindow      *window,
                             HippoSide         side,
                             gboolean          visible)
{
    g_return_if_fail(HIPPO_IS_WINDOW(window));

    return HIPPO_WINDOW_GET_CLASS(window)->set_resize_grip(window, side, visible);
}
