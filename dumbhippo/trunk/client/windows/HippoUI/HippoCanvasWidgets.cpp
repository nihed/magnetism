/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
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

HippoCanvas *
scrollbars_get_control(HippoCanvasScrollbars *scrollbars)
{
    HippoAbstractControl *control = HIPPO_CANVAS_CONTROL(scrollbars)->control;
    g_assert(control != NULL);
    
    return (HippoCanvas *)control;
}

HippoCanvasItem*
hippo_canvas_scrollbars_new(void)
{
    HippoCanvas *canvas;
    HippoCanvasItem *item;

    canvas = new HippoCanvas();
    canvas->setScrollbarPolicy(HIPPO_ORIENTATION_VERTICAL, HIPPO_SCROLLBAR_AUTOMATIC);
    canvas->setScrollbarPolicy(HIPPO_ORIENTATION_HORIZONTAL, HIPPO_SCROLLBAR_AUTOMATIC);

    item = HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_SCROLLBARS,
                            "control", canvas,
                            NULL));
    canvas->Release();

    return item;
}

void
hippo_canvas_scrollbars_set_root(HippoCanvasScrollbars *scrollbars,
                                 HippoCanvasItem       *item)
{
    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    scrollbars_get_control(scrollbars)->setRoot(item);    
}

void
hippo_canvas_scrollbars_set_policy (HippoCanvasScrollbars *scrollbars,
                                    HippoOrientation       orientation,
                                    HippoScrollbarPolicy   policy)
{
    HippoCanvas *control;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    scrollbars_get_control(scrollbars)->setScrollbarPolicy(orientation, policy);
}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
    return HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                            "control", NULL, /* FIXME */
                            NULL));
}
