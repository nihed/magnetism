/* HippoCanvasWidgets.cpp: Windows implementation of hippo/hippo-canvas-widgets.h
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <hippo/hippo-canvas-widgets.h>
#include "HippoCanvasControl.h"
#include "HippoCanvas.h"

#define HIPPO_DEFINE_CONTROL_ITEM(lower, Camel)                                         \
    struct _HippoCanvas##Camel { HippoCanvasControl parent; };                          \
    struct _HippoCanvas##Camel##Class { HippoCanvasControlClass parent; };              \
    static void hippo_canvas_##lower##_init(HippoCanvas##Camel *lower) {}               \
    static void hippo_canvas_##lower##_class_init(HippoCanvas##Camel##Class *lower) {}  \
    G_DEFINE_TYPE(HippoCanvas##Camel, hippo_canvas_##lower, HIPPO_TYPE_CANVAS_CONTROL)


HIPPO_DEFINE_CONTROL_ITEM(button, Button);
HIPPO_DEFINE_CONTROL_ITEM(scrollbars, Scrollbars);
HIPPO_DEFINE_CONTROL_ITEM(entry, Entry);

HippoCanvasItem*
hippo_canvas_button_new(void)
{
    return HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_BUTTON,
                                          "control", NULL, /* FIXME */
                                          NULL));
}

HippoCanvasItem*
hippo_canvas_scrollbars_new(void)
{
    HippoCanvas *canvas;
    HippoCanvasItem *item;

    canvas = new HippoCanvas();
    canvas->setScrollable(true);

    // FIXME actually have scrollbars around or as part of the canvas control

    item = HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_SCROLLBARS,
                            "control", canvas,
                             // FIXME debug-only, so we can see if the window is in the wrong spot
                            "background-color", 0xff0000ff,
                            NULL));
    canvas->Release();

    return item;
}

void
hippo_canvas_scrollbars_set_root(HippoCanvasScrollbars *scrollbars,
                                 HippoCanvasItem       *item)
{
    HippoCanvas *control;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    control = NULL;
    g_object_get(G_OBJECT(scrollbars), "control", &control,
                 NULL);
    g_assert(control != NULL);
    control->setRoot(item);
}

void
hippo_canvas_scrollbars_set_enabled (HippoCanvasScrollbars *scrollbars,
                                     HippoOrientation       orientation,
                                     gboolean               value)
{
    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
    return HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                            "control", NULL, /* FIXME */
                            NULL));
}
