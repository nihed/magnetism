/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-url-link.h"

static void      hippo_canvas_url_link_init                (HippoCanvasUrlLink       *link);
static void      hippo_canvas_url_link_class_init          (HippoCanvasUrlLinkClass  *klass);
static void      hippo_canvas_url_link_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_url_link_finalize            (GObject                *object);

static void hippo_canvas_url_link_set_property (GObject      *object,
                                                guint         prop_id,
                                                const GValue *value,
                                                GParamSpec   *pspec);
static void hippo_canvas_url_link_get_property (GObject      *object,
                                                guint         prop_id,
                                                GValue       *value,
                                                GParamSpec   *pspec);

/* Canvas item methods */
static void  hippo_canvas_url_link_activated   (HippoCanvasItem *item);
static char *hippo_canvas_url_link_get_tooltip (HippoCanvasItem *item,
                                                int              x,
                                                int              y,
                                                HippoRectangle  *for_area);

/* Our own methods */
static void hippo_canvas_url_link_set_actions (HippoCanvasUrlLink *link,
                                               HippoActions       *actions);

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_ACTIONS,
    PROP_URL
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasUrlLink, hippo_canvas_url_link, HIPPO_TYPE_CANVAS_TEXT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_url_link_iface_init));

static void
hippo_canvas_url_link_init(HippoCanvasUrlLink *link)
{
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_url_link_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
    
    item_class->activated = hippo_canvas_url_link_activated;
    item_class->get_tooltip = hippo_canvas_url_link_get_tooltip;
}

static void
hippo_canvas_url_link_class_init(HippoCanvasUrlLinkClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);

    box_class->default_color = 0x0033ffff;
    
    object_class->set_property = hippo_canvas_url_link_set_property;
    object_class->get_property = hippo_canvas_url_link_get_property;

    object_class->finalize = hippo_canvas_url_link_finalize;
    
    g_object_class_install_property(object_class,
                                    PROP_URL,
                                    g_param_spec_string("url",
                                                        _("URL"),
                                                        _("URL that the link points to"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_ACTIONS,
                                    g_param_spec_object("actions",
                                                        _("Actions"),
                                                        _("UI actions object"),
                                                        HIPPO_TYPE_ACTIONS,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE | G_PARAM_CONSTRUCT_ONLY)); 
}

static void
hippo_canvas_url_link_finalize(GObject *object)
{
    HippoCanvasUrlLink *link = HIPPO_CANVAS_URL_LINK(object);

    hippo_canvas_url_link_set_actions(link, NULL);
    g_free(link->url);

    G_OBJECT_CLASS(hippo_canvas_url_link_parent_class)->finalize(object);
}

HippoCanvasItem *
hippo_canvas_url_link_new(void)
{
    HippoCanvasUrlLink *link = g_object_new(HIPPO_TYPE_CANVAS_URL_LINK, NULL);

    return HIPPO_CANVAS_ITEM(link);
}

static void
hippo_canvas_url_link_set_url(HippoCanvasUrlLink *link,
                              const char         *url)
{

    if (url == link->url)
        return;

    if (link->url)
        g_free(link->url);
    
    link->url = g_strdup(url);

    if (link->url) {
        PangoAttrList *attrs;
        PangoAttribute *a;
        
        HIPPO_CANVAS_BOX(link)->clickable = TRUE;
        
        attrs = pango_attr_list_new();
        
        a = pango_attr_underline_new(PANGO_UNDERLINE_SINGLE);
        a->start_index = 0;
        a->end_index = G_MAXUINT;
        pango_attr_list_insert(attrs, a);
        g_object_set(link, "attributes", attrs, NULL);
        pango_attr_list_unref(attrs);
    } else {
        g_object_set(link, "attributes", NULL, NULL);
        HIPPO_CANVAS_BOX(link)->clickable = FALSE;
    }
}

static void
hippo_canvas_url_link_set_actions(HippoCanvasUrlLink *link,
                                  HippoActions       *actions)
{
    if (actions == link->actions)
        return;

    if (link->actions) {
        g_object_unref(link->actions);
        link->actions = NULL;
    }
    
    if (actions) {
        g_object_ref(actions);
        link->actions = actions;
    }

    g_object_notify(G_OBJECT(link), "actions");
}

static void
hippo_canvas_url_link_set_property(GObject         *object,
                                   guint            prop_id,
                                   const GValue    *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasUrlLink *link;

    link = HIPPO_CANVAS_URL_LINK(object);

    switch (prop_id) {
    case PROP_URL:
        hippo_canvas_url_link_set_url(link, g_value_get_string(value));
        break;
    case PROP_ACTIONS:
        {
            HippoActions *new_actions = (HippoActions*) g_value_get_object(value);
            hippo_canvas_url_link_set_actions(link, new_actions);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_url_link_get_property(GObject         *object,
                                   guint            prop_id,
                                   GValue          *value,
                                   GParamSpec      *pspec)
{
    HippoCanvasUrlLink *link;

    link = HIPPO_CANVAS_URL_LINK (object);

    switch (prop_id) {
    case PROP_URL:
        g_value_set_string(value, link->url);
        break;
    case PROP_ACTIONS:
        g_value_set_object(value, (GObject*) link->actions);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_url_link_activated(HippoCanvasItem *item)
{
    HippoCanvasUrlLink *link = HIPPO_CANVAS_URL_LINK(item);

    if (link->actions && link->url)
        hippo_actions_open_url(link->actions, link->url);
}

static char *
hippo_canvas_url_link_get_tooltip (HippoCanvasItem *item,
                                   int              x,
                                   int              y,
                                   HippoRectangle  *for_area)
{
    HippoCanvasUrlLink *link = HIPPO_CANVAS_URL_LINK(item);

    char *tooltip = item_parent_class->get_tooltip(item, x, y, for_area);
    if (tooltip) {
        return tooltip;
    } else if (link->url) {
        for_area->x = 0;
        for_area->y = 0;
        for_area->width = HIPPO_CANVAS_BOX(item)->allocated_width;
        for_area->height = HIPPO_CANVAS_BOX(item)->allocated_height;
        return g_strdup(link->url);
    } else {
        return NULL;
    }
}
