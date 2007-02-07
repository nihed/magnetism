/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-common-marshal.h"
#include "hippo-canvas-filter-area.h"
#include "hippo-actions.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-image-button.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-gradient.h>

static void      hippo_canvas_filter_area_init                (HippoCanvasFilterArea       *base);
static void      hippo_canvas_filter_area_class_init          (HippoCanvasFilterAreaClass  *klass);
static void      hippo_canvas_filter_area_iface_init          (HippoCanvasItemIface        *item_class);
static void      hippo_canvas_filter_area_finalize            (GObject                     *object);

static void hippo_canvas_filter_area_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_filter_area_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);
static GObject* hippo_canvas_filter_area_constructor (GType                  type,
                                               guint                  n_construct_properties,
                                               GObjectConstructParam *construct_properties);

/* Canvas item methods */
static void     hippo_canvas_filter_area_paint              (HippoCanvasItem *item,
                                                             cairo_t         *cr,
                                                             HippoRectangle  *damaged_box);

static void
on_nofeed_activated(HippoCanvasItem        *button,
                    HippoCanvasFilterArea  *area);
static void
on_noselfsource_activated(HippoCanvasItem        *button,
                          HippoCanvasFilterArea  *area);

struct _HippoCanvasFilterArea {
    HippoCanvasBox box;
    HippoActions *actions;
    HippoCanvasItem *nofeed_item;
    HippoCanvasItem *noselfsource_item;    
    gboolean nofeed_checked;
    gboolean noselfsource_checked;
};

struct _HippoCanvasFilterAreaClass {
    HippoCanvasBoxClass parent_class;
};

enum {
    LAST_SIGNAL
};

// static int signals[LAST_SIGNAL];

enum {
    PROP_0,
    PROP_ACTIONS,
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasFilterArea, hippo_canvas_filter_area, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_filter_area_iface_init));

static void
hippo_canvas_filter_area_init(HippoCanvasFilterArea *base)
{
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_filter_area_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_filter_area_paint;
}

static void
hippo_canvas_filter_area_class_init(HippoCanvasFilterAreaClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_filter_area_set_property;
    object_class->get_property = hippo_canvas_filter_area_get_property;
    object_class->constructor = hippo_canvas_filter_area_constructor;

    object_class->finalize = hippo_canvas_filter_area_finalize;

    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
}

static void
set_actions(HippoCanvasFilterArea *base,
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
hippo_canvas_filter_area_finalize(GObject *object)
{
    HippoCanvasFilterArea *base = HIPPO_CANVAS_FILTER_AREA(object);

    set_actions(base, NULL);
    
    G_OBJECT_CLASS(hippo_canvas_filter_area_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_filter_area_new(void)
{
    return HIPPO_CANVAS_ITEM(g_object_new(HIPPO_TYPE_CANVAS_FILTER_AREA, NULL));
}

static void
hippo_canvas_filter_area_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    HippoCanvasFilterArea *base;

    base = HIPPO_CANVAS_FILTER_AREA(object);

    switch (prop_id) {
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            set_actions(base, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_filter_area_get_property(GObject         *object,
                               guint            prop_id,
                               GValue          *value,
                               GParamSpec      *pspec)
{
    HippoCanvasFilterArea *base;

    base = HIPPO_CANVAS_FILTER_AREA (object);

    switch (prop_id) {
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) base->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static GObject*
hippo_canvas_filter_area_constructor (GType                  type,
                                      guint                  n_construct_properties,
                                      GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_filter_area_parent_class)->constructor(type,
                                                                                         n_construct_properties,
                                                                                         construct_properties);
    HippoCanvasFilterArea *area = HIPPO_CANVAS_FILTER_AREA(object);
    HippoCanvasItem *item;    
    HippoCanvasBox *box;
    
    box = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(area),
                            HIPPO_CANVAS_ITEM(box), 0);    

    box->background_color_rgba = 0xE4D0EDFF;
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "Hide feeds: ",
                        "padding-left", 4,
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);    
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "normal-image-name", "checkbox",
                        "xalign", HIPPO_ALIGNMENT_START,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        "tooltip", "Whether or not to display group feed items",
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_nofeed_activated), area);
    area->nofeed_item = item;
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "Hide my items: ",
                        "padding-left", 40,
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);    
    
    item = g_object_new(HIPPO_TYPE_CANVAS_IMAGE_BUTTON,
                        "normal-image-name", "checkbox",
                        "xalign", HIPPO_ALIGNMENT_START,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        "tooltip", "Whether or not to show items from me",
                        NULL);
    hippo_canvas_box_append(box, item, 0);
    g_signal_connect(G_OBJECT(item), "activated", G_CALLBACK(on_noselfsource_activated), area);
    area->noselfsource_item = item;    
   
    return object;
}

static void
hippo_canvas_filter_area_paint(HippoCanvasItem *item,
                        cairo_t         *cr,
                        HippoRectangle  *damaged_box)
{
    /* HippoCanvasFilterArea *base = HIPPO_CANVAS_FILTER_AREA(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr, damaged_box);
}

void
hippo_canvas_filter_area_set_nofeed_active(HippoCanvasFilterArea *area,
                                           gboolean               active)
{
    area->nofeed_checked = active;
    g_object_set(G_OBJECT(area->nofeed_item), "normal-image-name", 
                 area->nofeed_checked ? "checked_checkbox" : "checkbox", NULL);    
}

void
hippo_canvas_filter_area_set_noselfsource_active(HippoCanvasFilterArea *area,
                                                 gboolean               active)
{
   area->noselfsource_checked = active;
   g_object_set(G_OBJECT(area->noselfsource_item), "normal-image-name", 
                area->noselfsource_checked ? "checked_checkbox" : "checkbox", NULL);   
}

static void
on_nofeed_activated(HippoCanvasItem        *button,
                    HippoCanvasFilterArea  *area)
{
    hippo_canvas_filter_area_set_nofeed_active(area, !area->nofeed_checked);
    if (area->actions)
        hippo_actions_toggle_nofeed(area->actions);
}

static void
on_noselfsource_activated(HippoCanvasItem        *button,
                          HippoCanvasFilterArea  *area)
{
    hippo_canvas_filter_area_set_noselfsource_active(area, !area->noselfsource_checked);
    if (area->actions)
        hippo_actions_toggle_noselfsource(area->actions);
}
