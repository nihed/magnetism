/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-link.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-box.h"

static void      hippo_canvas_link_init                (HippoCanvasLink       *link);
static void      hippo_canvas_link_class_init          (HippoCanvasLinkClass  *klass);
static void      hippo_canvas_link_iface_init          (HippoCanvasItemIface   *item_class);
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
#endif

enum {
    PROP_0,
    PROP_VISITED
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasLink, hippo_canvas_link, HIPPO_TYPE_CANVAS_TEXT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_link_iface_init));

static void
hippo_canvas_link_init(HippoCanvasLink *link)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(link); */
    
    HIPPO_CANVAS_BOX(link)->clickable = TRUE;

    link->base_attrs = NULL;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_link_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_link_class_init(HippoCanvasLinkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    box_class->default_color = 0x0033ffff;
    
    object_class->set_property = hippo_canvas_link_set_property;
    object_class->get_property = hippo_canvas_link_get_property;

    object_class->finalize = hippo_canvas_link_finalize;


    g_object_class_install_property(object_class,
                                    PROP_VISITED,
                                    g_param_spec_boolean("visited",
                                                        _("Visited"),
                                                        _("Whether or not link was visited"),
                                                        FALSE,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_link_finalize(GObject *object)
{
    HippoCanvasLink *link = HIPPO_CANVAS_LINK(object);

    if (link->base_attrs)
        pango_attr_list_unref(link->base_attrs);

    G_OBJECT_CLASS(hippo_canvas_link_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_link_new(void)
{
    HippoCanvasLink *link = g_object_new(HIPPO_TYPE_CANVAS_LINK, NULL);

    return HIPPO_CANVAS_ITEM(link);
}

static void
sync_attributes(HippoCanvasLink *link)
{
    PangoAttribute *a;

    if (link->base_attrs)
        pango_attr_list_unref(link->base_attrs);
    link->base_attrs = pango_attr_list_new();

    a = pango_attr_underline_new(PANGO_UNDERLINE_SINGLE);
    a->start_index = 0;
    a->end_index = G_MAXUINT;
    pango_attr_list_insert(link->base_attrs, a);

    if (link->visited) {
        a = pango_attr_foreground_new(0x6666, 0x6666, 0x6666);
        a->start_index = 0;
        a->end_index = G_MAXUINT;
        pango_attr_list_insert(link->base_attrs, a);
    }
    g_object_set(link, "attributes", link->base_attrs, NULL);
}

static void
hippo_canvas_link_set_visited(HippoCanvasLink    *link,
                              gboolean            visited)
{
    link->visited = visited;
    sync_attributes(link);
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
    case PROP_VISITED:
        hippo_canvas_link_set_visited(link, g_value_get_boolean(value));
        break;
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
    case PROP_VISITED:
        g_value_set_boolean(value, link->visited);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

