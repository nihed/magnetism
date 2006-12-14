/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoCanvasWidgets.cpp: Windows implementation of hippo/hippo-canvas-widgets.h
 *
 * Copyright Red Hat, Inc. 2006
 **/
#include "stdafx-hippoui.h"
#include <hippo/hippo-canvas-widgets.h>
#include "HippoCanvasControl.h"
#include "HippoCanvas.h"
#include "HippoEntryControl.h"

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

/*************************************************************************/

class HippoCanvasEntryListener : HippoEntryControlListener {
public:
    HippoCanvasEntryListener(HippoCanvasItem *item, HippoEntryControl *control) {
        item_ = item;
        control_ = control;
    }

    virtual void onTextChanged();
    virtual bool onKeyPress(HippoKey key, gunichar character);

private:
    HippoCanvasItem *item_;
    HippoEntryControl *control_;
}

struct _HippoCanvasEntry {
    HippoCanvasControl parent;

    HippoCanvasEntryListener *listener;
};

struct _HippoCanvasEntryClass { 
    HippoCanvasControlClass parent; 
};

G_DEFINE_TYPE(HippoCanvasEntry, hippo_canvas_entry, HIPPO_TYPE_CANVAS_CONTROL)

static void 
hippo_canvas_entry_init(HippoCanvasEntry *canvas_entry) {
}

HippoEntryControl *
entry_get_control(HippoCanvasEntry *canvas_entry)
{
    HippoAbstractControl *control = HIPPO_CANVAS_CONTROL(canvas_entry)->control;
    g_assert(control != NULL);
    
    return (HippoEntryControl *)control;
}

static void
hippo_canvas_entry_dispose(GObject *object)
{
    HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object);
    HippoEntryControl *entry = entry_get_control(canvas_entry);

    if (entry)
        entry->setListener(null);

    G_OBJECT_CLASS(hippo_canvas_entry_parent_class)->dispose(object);
}

static void
hippo_canvas_entry_set_property(GObject        *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY(object);
    HippoEntryControl *entry = entry_get_control(canvas_entry);

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
        HippoBSTR str = HippoBSTR.fromUTF8(g_value_get_string(value));
        entry->setText(str);
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
    HippoCanvasEntry *canvas_entry = HIPPO_CANVAS_ENTRY (object);
    HippoEntryControl *entry = entry_get_control(canvas_entry);

    switch (prop_id) {
    case ENTRY_PROP_TEXT:
        HippoUStr ustr(entry->getText());
        g_value_set_string(value, ustr.c_str);
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
    HippoEntryControl *entry = new HippoEntryControl();
    HippoCanvasEntry *item = g_object_new(HIPPO_TYPE_CANVAS_ENTRY,
                                          "control", entry,
                                          NULL);

    item->listener = new HippoCanvasEntryListener(HIPPO_CANVAS_ITEM(item), entry);
    entry->setListener(item->listener);

    return HIPPO_CANVAS_ITEM(item);
}

void 
HippoCanvasEntryListener::onTextChanged() {
    g_object_notify(G_OBJECT(item_), "text");
}

bool 
HippoCanvasEntryListener::onKeyPress(HippoKey key, gunichar character) {
    return hippo_canvas_item_emit_key_press_event(item_, key, character) != FALSE;
}
