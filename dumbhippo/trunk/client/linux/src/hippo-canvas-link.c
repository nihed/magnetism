/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-link.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_link_init                (HippoCanvasLink       *link);
static void      hippo_canvas_link_class_init          (HippoCanvasLinkClass  *klass);
static void      hippo_canvas_link_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_link_finalize            (GObject                *object);

static void hippo_canvas_link_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_link_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);


/* Canvas item methods */
static void               hippo_canvas_link_paint               (HippoCanvasItem *item,
                                                                 cairo_t         *cr);
static int                hippo_canvas_link_get_width_request   (HippoCanvasItem *item);
static int                hippo_canvas_link_get_height_request  (HippoCanvasItem *item,
                                                                 int              for_width);
static gboolean           hippo_canvas_link_button_press_event  (HippoCanvasItem *item,
                                                                 HippoEvent      *event);
static gboolean           hippo_canvas_link_motion_notify_event (HippoCanvasItem *item,
                                                                 HippoEvent      *event);
static HippoCanvasPointer hippo_canvas_link_get_pointer         (HippoCanvasItem *item,
                                                                 int              x,
                                                                 int              y);

struct _HippoCanvasLink {
    HippoCanvasText text;
};

struct _HippoCanvasLinkClass {
    HippoCanvasTextClass parent_class;

};

enum {
    ACTIVATED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};

#define DEFAULT_FOREGROUND 0x0000ffff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasLink, hippo_canvas_link, HIPPO_TYPE_CANVAS_TEXT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_link_iface_init));

static void
hippo_canvas_link_init(HippoCanvasLink *link)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(link);
    PangoAttrList *attrs;
    PangoAttribute *a;
    
    text->color_rgba = DEFAULT_FOREGROUND;

    attrs = pango_attr_list_new();

    a = pango_attr_underline_new(PANGO_UNDERLINE_SINGLE);
    a->start_index = 0;
    a->end_index = G_MAXUINT;
    pango_attr_list_insert(attrs, a);
    g_object_set(link, "attributes", attrs, NULL);
    pango_attr_list_unref(attrs);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_link_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_link_paint;
    item_class->get_width_request = hippo_canvas_link_get_width_request;
    item_class->get_height_request = hippo_canvas_link_get_height_request;
    item_class->button_press_event = hippo_canvas_link_button_press_event;
    item_class->motion_notify_event = hippo_canvas_link_motion_notify_event;
    item_class->get_pointer = hippo_canvas_link_get_pointer;
}

static void
hippo_canvas_link_class_init(HippoCanvasLinkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_link_set_property;
    object_class->get_property = hippo_canvas_link_get_property;

    object_class->finalize = hippo_canvas_link_finalize;
    
    signals[ACTIVATED] =
        g_signal_new ("activated",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
hippo_canvas_link_finalize(GObject *object)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(object); */


    G_OBJECT_CLASS(hippo_canvas_link_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_link_new(void)
{
    HippoCanvasLink *link = g_object_new(HIPPO_TYPE_CANVAS_LINK, NULL);


    return HIPPO_CANVAS_ITEM(link);
}

static void
hippo_canvas_link_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasLink *link;

    link = HIPPO_CANVAS_LINK(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_link_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasLink *link;

    link = HIPPO_CANVAS_LINK (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_link_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(item); */
    
    /* Just chain up */
    item_parent_class->paint(item, cr);
}

static int
hippo_canvas_link_get_width_request(HippoCanvasItem *item)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(item); */

    /* Just chain up */
    return item_parent_class->get_width_request(item);
}

static int
hippo_canvas_link_get_height_request(HippoCanvasItem *item,
                                      int              for_width)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(item); */

    /* Just chain up */
    return item_parent_class->get_height_request(item, for_width);
}

static gboolean
hippo_canvas_link_button_press_event(HippoCanvasItem *item,
                                     HippoEvent      *event)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(item); */

    /* see if a child wants it */
    if (item_parent_class->button_press_event(item, event))
        return TRUE;

    /* g_debug("button on link %d,%d", event->x, event->y); */
    
    g_signal_emit(item, signals[ACTIVATED], 0);
    
    return TRUE;
}

static gboolean
hippo_canvas_link_motion_notify_event(HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    /* HippoCanvasLink *link = HIPPO_CANVAS_LINK(item); */

    /* see if a child wants it */
    if (item_parent_class->motion_notify_event(item, event))
        return TRUE;
    
    /* g_debug("motion on link %d,%d", event->x, event->y); */
    
    return FALSE;
}

static HippoCanvasPointer
hippo_canvas_link_get_pointer(HippoCanvasItem *item,
                              int              x,
                              int              y)
{
    HippoCanvasPointer pointer;

    /* see if a child wants to set it */
    pointer = item_parent_class->get_pointer(item, x, y);
    if (pointer != HIPPO_CANVAS_POINTER_UNSET)
        return pointer;

    return HIPPO_CANVAS_POINTER_HAND;
}
