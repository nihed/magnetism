/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-common-marshal.h"
#include "hippo-canvas-base.h"
#include "hippo-actions.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-image-button.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-gradient.h>

static void      hippo_canvas_base_init                (HippoCanvasBase       *base);
static void      hippo_canvas_base_class_init          (HippoCanvasBaseClass  *klass);
static void      hippo_canvas_base_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_base_finalize            (GObject                *object);

static void hippo_canvas_base_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_base_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);
static GObject* hippo_canvas_base_constructor (GType                  type,
                                               guint                  n_construct_properties,
                                               GObjectConstructParam *construct_properties);

/* Canvas item methods */
static void     hippo_canvas_base_paint              (HippoCanvasItem *item,
                                                      cairo_t         *cr,
                                                      HippoRectangle  *damaged_box);

/* Callbacks */
static gboolean on_title_bar_button_press_event(HippoCanvasItem *title_bar,
                                                HippoEvent      *event,
                                                HippoCanvasBase *base);
static gboolean on_button_button_press_event(HippoCanvasItem *button,
                                             HippoEvent      *event);
static void on_close_activated (HippoCanvasItem *button,
                                HippoCanvasBase *base);
static void on_expand_activated  (HippoCanvasItem  *button,
                                  HippoCanvasBase *base);
static void on_hush_activated  (HippoCanvasItem  *button,
                                HippoCanvasBase *base);
static void on_filter_activated(HippoCanvasItem  *button,
                                HippoCanvasBase *base);                                
static void on_home_activated  (HippoCanvasItem  *button,
                                HippoCanvasBase *base);

struct _HippoCanvasBase {
    HippoCanvasBox box;
    gboolean notification_mode;
    HippoActions *actions;
};

struct _HippoCanvasBaseClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    TITLE_BAR_BUTTON_PRESS_EVENT,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0,
    PROP_ACTIONS,
    PROP_NOTIFICATION_MODE
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBase, hippo_canvas_base, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_base_iface_init));

static void
hippo_canvas_base_init(HippoCanvasBase *base)
{
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_base_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_base_paint;
}

static void
hippo_canvas_base_class_init(HippoCanvasBaseClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_base_set_property;
    object_class->get_property = hippo_canvas_base_get_property;
    object_class->constructor = hippo_canvas_base_constructor;

    object_class->finalize = hippo_canvas_base_finalize;

    signals[TITLE_BAR_BUTTON_PRESS_EVENT] =
        g_signal_new ("title-bar-button-press-event",
                      HIPPO_TYPE_CANVAS_ITEM,
                      G_SIGNAL_RUN_LAST,
                      0,
                      g_signal_accumulator_true_handled, NULL,
                      hippo_common_marshal_BOOLEAN__BOXED,
                      G_TYPE_BOOLEAN, 1, HIPPO_TYPE_EVENT);
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
    g_object_class_install_property(object_class,
                                    PROP_NOTIFICATION_MODE,
                                    g_param_spec_boolean("notification-mode",
                                                         _("Notification Node"),
                                                         _("True if this the base item for the notification window"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY));
}

static void
set_actions(HippoCanvasBase *base,
            HippoActions     *actions)
{
    if (actions == base->actions)
        return;
    
    if (base->actions) {
        g_object_unref(base->actions);
        base->actions = NULL;
    }

    if (actions) {
        base->actions = actions;
        g_object_ref(base->actions);
    }
    
    g_object_notify(G_OBJECT(base), "actions");
}

static void
hippo_canvas_base_finalize(GObject *object)
{
    HippoCanvasBase *base = HIPPO_CANVAS_BASE(object);

    set_actions(base, NULL);
    
    G_OBJECT_CLASS(hippo_canvas_base_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_base_new(void)
{
    HippoCanvasBase *base = g_object_new(HIPPO_TYPE_CANVAS_BASE, NULL);


    return HIPPO_CANVAS_ITEM(base);
}

static void
hippo_canvas_base_set_property(GObject         *object,
                               guint            prop_id,
                               const GValue    *value,
                               GParamSpec      *pspec)
{
    HippoCanvasBase *base;

    base = HIPPO_CANVAS_BASE(object);

    switch (prop_id) {
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            set_actions(base, new_actions);
        }
        break;
    case PROP_NOTIFICATION_MODE:
        {
            gboolean notification_mode = g_value_get_boolean(value);
            /* The property is construct-only, so we don't need to adapt the layout */
            base->notification_mode = notification_mode;
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_base_get_property(GObject         *object,
                               guint            prop_id,
                               GValue          *value,
                               GParamSpec      *pspec)
{
    HippoCanvasBase *base;

    base = HIPPO_CANVAS_BASE (object);

    switch (prop_id) {
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) base->actions);
        break;
    case PROP_NOTIFICATION_MODE:
        g_value_set_boolean(value, base->notification_mode);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
add_pipe_bar(HippoCanvasBox *box,
             HippoPackFlags  flags)
{
    HippoCanvasItem *item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                                         "image-name", "bar_pipe",
                                         "xalign", HIPPO_ALIGNMENT_END,
                                         NULL);
    hippo_canvas_box_append(box, item, flags);
}

static GObject*
hippo_canvas_base_constructor (GType                  type,
                               guint                  n_construct_properties,
                               GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_base_parent_class)->constructor(type,
                                                                                  n_construct_properties,
                                                                                  construct_properties);
    HippoCanvasBase *base = HIPPO_CANVAS_BASE(object);
    HippoCanvasItem *item;
    HippoCanvasBox *box;
    
    /* Create top bar */
    
    box = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "image-name", "bar_middle", /* tile this as background */
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(base),
                            HIPPO_CANVAS_ITEM(box), 0);

    g_signal_connect_after(G_OBJECT(box), "button-press-event", G_CALLBACK(on_title_bar_button_press_event), base);

    if (base->notification_mode) {
        item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                            "normal-image-name", "arrow",
                            "prelight-image-name", "arrow2",
                            "xalign", HIPPO_ALIGNMENT_START,
                            "tooltip", "Browse all items",
                            NULL);
        hippo_canvas_box_append(box, item, 0);
    
        g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_expand_activated), base);
        g_signal_connect(G_OBJECT(item), "button-press-event", G_CALLBACK(on_button_button_press_event), base);

        add_pipe_bar(box, 0);
    }
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "mugshotstacker",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "normal-image-name", "bar_x",
                        "prelight-image-name", "bar_x2",
                        "xalign", HIPPO_ALIGNMENT_END,
                        "tooltip", "Close",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_close_activated), base);
    g_signal_connect(G_OBJECT(item), "button-press-event", G_CALLBACK(on_button_button_press_event), base);

    add_pipe_bar(box, HIPPO_PACK_END);

    if (base->notification_mode) {
        item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                            "normal-image-name", "hush",
                            "prelight-image-name", "hush2",
                            "xalign", HIPPO_ALIGNMENT_END,
                            "tooltip", "Don't bug me for a while",
                            NULL);
        hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
        g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_hush_activated), base);
        g_signal_connect(G_OBJECT(item), "button-press-event", G_CALLBACK(on_button_button_press_event), base);

        add_pipe_bar(box, HIPPO_PACK_END);
    }
    
    if (!base->notification_mode) {
        item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                            "normal-image-name", "home",
                            "prelight-image-name", "home2",
                            "xalign", HIPPO_ALIGNMENT_END,
                            "tooltip", "Open Mugshot home page",
                            NULL);
        hippo_canvas_box_append(box, item, HIPPO_PACK_END);
        
        g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_home_activated), base);
        g_signal_connect(G_OBJECT(item), "button-press-event", G_CALLBACK(on_button_button_press_event), base);
        
        add_pipe_bar(box, HIPPO_PACK_END);
        
        item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                            "normal-image-name", "filter",
                            "prelight-image-name", "filter2",
                            "xalign", HIPPO_ALIGNMENT_END,
                            "tooltip", "Show filters",
                            NULL);
        hippo_canvas_box_append(box, item, HIPPO_PACK_END);
        g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_filter_activated), base);
        g_signal_connect(G_OBJECT(item), "button-press-event", G_CALLBACK(on_button_button_press_event), base);        
        
        add_pipe_bar(box, HIPPO_PACK_END);        
    }
        
#if 0    
    /* Create "find" area */
    box = g_object_new(HIPPO_TYPE_CANVAS_GRADIENT,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "start-color", 0xb47accff,
                       "end-color", 0xa35abfff,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(base),
                            HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "find",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "search_x",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);
#endif

    return object;
}

static void
hippo_canvas_base_paint(HippoCanvasItem *item,
                        cairo_t         *cr,
                        HippoRectangle  *damaged_box)
{
    /* HippoCanvasBase *base = HIPPO_CANVAS_BASE(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr, damaged_box);
}

static gboolean
on_title_bar_button_press_event(HippoCanvasItem *title_bar,
                                HippoEvent      *event,
                                HippoCanvasBase *base)
{
    gboolean handled = FALSE;

    /* The item-relative coordinates here are with respect to the child, which is bogus, but
     * we don't need the item-realitve event coordinates for now, so we won't bother translating,
     */
    g_signal_emit(base, signals[TITLE_BAR_BUTTON_PRESS_EVENT], 0, event, &handled);

    return handled;
}

/* This is a hack for the title bar buttons to prevent button presses from leaking
 * through them to the title bar and triggering a move. The correct fix is probably
 * for HippoCanvasImageButton, Link, etc, to *always* block button press event
 * propagation. Or maybe HippoCanvasBox can do it when set to be clickable.
 */
static gboolean 
on_button_button_press_event(HippoCanvasItem *button,
                             HippoEvent      *event)
{
    return TRUE;
}

static void
on_close_activated (HippoCanvasItem *button,
                    HippoCanvasBase *base)
{
    if (base->actions) {
        if (base->notification_mode)
            hippo_actions_close_notification(base->actions);
        else
            hippo_actions_close_browser(base->actions);
    }
}

static void
on_expand_activated(HippoCanvasItem  *button,
                  HippoCanvasBase  *base)
{
    if (base->actions)
        hippo_actions_expand_notification(base->actions);
}

static void
on_hush_activated(HippoCanvasItem  *button,
                  HippoCanvasBase  *base)
{
    if (base->actions)
        hippo_actions_hush_notification(base->actions);
}

static void
on_filter_activated(HippoCanvasItem  *button,
                    HippoCanvasBase  *base)
{
    if (base->actions)
        hippo_actions_toggle_filter(base->actions);
}

static void
on_home_activated(HippoCanvasItem  *button,
                  HippoCanvasBase  *base)
{
    if (base->actions)
        hippo_actions_open_home_page(base->actions);
}
