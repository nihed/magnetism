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

#define HIPPO_DEFINE_CONTROL_ITEM_CUSTOM_INIT(lower, Camel)                             \
    struct _HippoCanvas##Camel { HippoCanvasControl parent; };                          \
    struct _HippoCanvas##Camel##Class { HippoCanvasControlClass parent; };              \
    static void hippo_canvas_##lower##_class_init(HippoCanvas##Camel##Class *lower) {}  \
    G_DEFINE_TYPE(HippoCanvas##Camel, hippo_canvas_##lower, HIPPO_TYPE_CANVAS_CONTROL)


HIPPO_DEFINE_CONTROL_ITEM(button, Button);
HIPPO_DEFINE_CONTROL_ITEM(scrollbars, Scrollbars);
HIPPO_DEFINE_CONTROL_ITEM_CUSTOM_INIT(entry, Entry);

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
    HippoCanvas *control;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    control = NULL;
    g_object_get(G_OBJECT(scrollbars), "control", &control,
                 NULL);
    g_assert(control != NULL);
    control->setRoot(item);
}

void
hippo_canvas_scrollbars_set_policy (HippoCanvasScrollbars *scrollbars,
                                    HippoOrientation       orientation,
                                    HippoScrollbarPolicy   policy)
{
    HippoCanvas *control;

    g_return_if_fail(HIPPO_IS_CANVAS_SCROLLBARS(scrollbars));
    
    control = NULL;
    g_object_get(G_OBJECT(scrollbars),
                 "control", &control,
                 NULL);
    g_assert(control != NULL);
    control->setScrollbarPolicy(orientation, policy);
}

static void
hippo_canvas_entry_dispose(GObject *object)
{
    HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object);
    GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget;

    if (entry) {
        g_signal_handlers_disconnect_by_func(entry, (void *)on_canvas_entry_changed, canvas_entry);
        g_signal_handlers_disconnect_by_func(entry, (void *)on_canvas_entry_key_press_event, canvas_entry);
    }

    G_OBJECT_CLASS(hippo_canvas_entry_parent_class)->dispose(object);
}

static void
hippo_canvas_entry_set_property(GObject        *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    /* HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY(object); */
    /* GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget; */

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
#if 0
        gtk_entry_set_text(GTK_ENTRY(entry), g_value_get_string(value));
#endif
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entry_get_property(GObject        *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    /* HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object); */
    /* GtkWidget *entry = HIPPO_CANVAS_WIDGET(object)->widget; */

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
#if 0
        g_value_set_string(value, gtk_entry_get_text(GTK_ENTRY(entry)));
#endif
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_entry_class_init(HippoCanvasEntryClass *class)
{
    GObjectClass *object_class = G_OBJECT_CLASS(class);

    object_class->dispose = hippo_canvas_entry_dispose;
    object_class->set_property = hippo_canvas_entry_set_property;
    object_class->get_property = hippo_canvas_entry_get_property;
    
    g_object_class_install_property(object_class,
                                    ENTRY_PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text in the entry"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

HippoCanvasItem*
hippo_canvas_entry_new(void)
{
#if 0
    GtkWidget *entry;
    HippoCanvasItem *item;
#endif
    
    return HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                            "control", NULL, /* FIXME */
                            NULL));

#if 0    
    g_signal_connect(entry, "changed",
                     G_CALLBACK(on_canvas_entry_changed), item);
    g_signal_connect(entry, "key-press-event",
                     G_CALLBACK(on_canvas_entry_key_press_event), item);
#endif

    return item;
}
