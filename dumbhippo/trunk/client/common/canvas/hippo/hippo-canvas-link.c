/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
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


#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

#define DEFAULT_FOREGROUND 0x0000ffff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasLink, hippo_canvas_link, HIPPO_TYPE_CANVAS_TEXT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_link_iface_init));

static void
hippo_canvas_link_init(HippoCanvasLink *link)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(link);
    PangoAttrList *attrs;
    PangoAttribute *a;
    
    HIPPO_CANVAS_BOX(link)->clickable = TRUE;
    
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
}

static void
hippo_canvas_link_class_init(HippoCanvasLinkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_link_set_property;
    object_class->get_property = hippo_canvas_link_get_property;

    object_class->finalize = hippo_canvas_link_finalize;
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

