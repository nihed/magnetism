/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-box.h"

static void hippo_canvas_box_init               (HippoCanvasBox          *box);
static void hippo_canvas_box_class_init         (HippoCanvasBoxClass     *klass);
static void hippo_canvas_box_iface_init         (HippoCanvasItemClass    *klass);
static void hippo_canvas_box_iface_init_context (HippoCanvasContextClass *klass);
static void hippo_canvas_box_dispose            (GObject                 *object);
static void hippo_canvas_box_finalize           (GObject                 *object);


static void hippo_canvas_box_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_canvas_box_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


/* Canvas context methods */
static PangoLayout* hippo_canvas_create_layout      (HippoCanvasContext *context);

/* Canvas item methods */
static void     hippo_canvas_box_set_context        (HippoCanvasItem    *item,
                                                     HippoCanvasContext *context);
static void     hippo_canvas_box_paint              (HippoCanvasItem    *item,
                                                     cairo_t            *cr);
static int      hippo_canvas_box_get_width_request  (HippoCanvasItem    *item);
static int      hippo_canvas_box_get_height_request (HippoCanvasItem    *item,
                                                     int                 for_width);
static void     hippo_canvas_box_allocate           (HippoCanvasItem    *item,
                                                     int                 width,
                                                     int                 height);
static void     hippo_canvas_box_get_allocation     (HippoCanvasItem    *item,
                                                     int                *width_p,
                                                     int                *height_p);
static gboolean hippo_canvas_box_button_press_event (HippoCanvasItem    *item,
                                                     HippoEvent         *event);
static void     hippo_canvas_box_request_changed    (HippoCanvasItem    *item);
static gboolean hippo_canvas_box_get_needs_resize   (HippoCanvasItem    *canvas_item);



/* Our own methods */

static void hippo_canvas_box_free_children        (HippoCanvasBox *box);

typedef struct {
    HippoCanvasItem *item;
    /* allocated x, y */
    int              x;
    int              y;
    /* last-requested size */
    int              width_request;
    int              height_request;
    guint            expand : 1;
    guint            end : 1;
} HippoBoxChild;

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_ORIENTATION,
    PROP_PADDING_TOP,
    PROP_PADDING_BOTTOM,
    PROP_PADDING_LEFT,
    PROP_PADDING_RIGHT,
    PROP_FIXED_WIDTH
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBox, hippo_canvas_box, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_box_iface_init);
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT, hippo_canvas_box_iface_init_context));

static void
hippo_canvas_box_iface_init(HippoCanvasItemClass *klass)
{
    klass->set_context = hippo_canvas_box_set_context;
    klass->paint = hippo_canvas_box_paint;
    klass->get_width_request = hippo_canvas_box_get_width_request;
    klass->get_height_request = hippo_canvas_box_get_height_request;
    klass->allocate = hippo_canvas_box_allocate;
    klass->get_allocation = hippo_canvas_box_get_allocation;
    klass->button_press_event = hippo_canvas_box_button_press_event;
    klass->request_changed = hippo_canvas_box_request_changed;
    klass->get_needs_resize = hippo_canvas_box_get_needs_resize;
}

static void
hippo_canvas_box_iface_init_context (HippoCanvasContextClass *klass)
{
    klass->create_layout = hippo_canvas_create_layout;
}

static void
hippo_canvas_box_init(HippoCanvasBox *box)
{
    box->orientation = HIPPO_ORIENTATION_VERTICAL;
    box->fixed_width = -1;
}

static void
hippo_canvas_box_class_init(HippoCanvasBoxClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_box_set_property;
    object_class->get_property = hippo_canvas_box_get_property;

    object_class->dispose = hippo_canvas_box_dispose;
    object_class->finalize = hippo_canvas_box_finalize;

    /* we're supposed to register the enum yada yada, but doesn't matter */
    g_object_class_install_property(object_class,
                                    PROP_ORIENTATION,
                                    g_param_spec_int("orientation",
                                                     _("Orientation"),
                                                     _("Direction of the box"),
                                                     0,
                                                     G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_PADDING_TOP,
                                    g_param_spec_int("padding-top",
                                                     _("Top Padding"),
                                                     _("Padding above the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_PADDING_BOTTOM,
                                    g_param_spec_int("padding-bottom",
                                                     _("Bottom Padding"),
                                                     _("Padding below the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_PADDING_LEFT,
                                    g_param_spec_int("padding-left",
                                                     _("Left Padding"),
                                                     _("Padding to left of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_PADDING_RIGHT,
                                    g_param_spec_int("padding-right",
                                                     _("Right Padding"),
                                                     _("Padding to right of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_FIXED_WIDTH,
                                    g_param_spec_int("fixed-width",
                                                     _("Fixed Width"),
                                                     _("Width request of the canvas item, or -1 to use natural width"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_box_dispose(GObject *object)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);

    hippo_canvas_box_free_children(box);

    G_OBJECT_CLASS(hippo_canvas_box_parent_class)->dispose(object);
}

static void
hippo_canvas_box_finalize(GObject *object)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);

    g_assert(box->children == NULL); /* should have vanished in dispose */

    G_OBJECT_CLASS(hippo_canvas_box_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_box_new(void)
{
    HippoCanvasBox *box = g_object_new(HIPPO_TYPE_CANVAS_BOX, NULL);


    return HIPPO_CANVAS_ITEM(box);
}

static void
hippo_canvas_box_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX(object);

    switch (prop_id) {
    case PROP_ORIENTATION:
        box->orientation = g_value_get_int(value);
        break;
    case PROP_PADDING_TOP:
        box->padding_top = g_value_get_int(value);
        break;
    case PROP_PADDING_BOTTOM:
        box->padding_bottom = g_value_get_int(value);
        break;
    case PROP_PADDING_LEFT:
        box->padding_left = g_value_get_int(value);
        break;
    case PROP_PADDING_RIGHT:
        box->padding_right = g_value_get_int(value);
        break;
    case PROP_FIXED_WIDTH:
        box->fixed_width = g_value_get_int(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }

    /* Right now all our properties require this */
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
hippo_canvas_box_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX (object);

    switch (prop_id) {
    case PROP_ORIENTATION:
        g_value_set_int(value, box->orientation);
        break;
    case PROP_PADDING_TOP:
        g_value_set_int(value, box->padding_top);
        break;
    case PROP_PADDING_BOTTOM:
        g_value_set_int(value, box->padding_bottom);
        break;
    case PROP_PADDING_LEFT:
        g_value_set_int(value, box->padding_left);
        break;
    case PROP_PADDING_RIGHT:
        g_value_set_int(value, box->padding_right);
        break;
    case PROP_FIXED_WIDTH:
        g_value_set_int(value, box->fixed_width);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static PangoLayout*
hippo_canvas_create_layout(HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    /* just chain to our parent */
    return hippo_canvas_context_create_layout(box->context);
}

static void
hippo_canvas_box_set_context(HippoCanvasItem    *item,
                             HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    box->context = context;
}

static void
hippo_canvas_box_paint(HippoCanvasItem *item,
                       cairo_t         *cr)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    GSList *link;

    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        hippo_canvas_item_process_paint(HIPPO_CANVAS_ITEM(child->item), cr,
                                        child->x, child->y);
    }
}

static int
hippo_canvas_box_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int total;
    GSList *link;

    total = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        child->width_request = hippo_canvas_item_get_width_request(child->item);
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL)
            total = MAX(total, child->width_request);
        else
            total += child->width_request;
    }

    total += box->padding_left;
    total += box->padding_right;

    /* This ignored "total" but we needed to be sure we called width_request
     * on all children so we have the width requests to do layout later
     * and so they can rely on the invariant that the sizing cycle always
     * happens in its entirety (width req, height req, allocate)
     */
    if (box->fixed_width >= 0)
        return box->fixed_width;
    
    return total;
}

static int
hippo_canvas_box_get_height_request(HippoCanvasItem *item,
                                    int              for_width)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int total;
    GSList *link;
    
    total = 0;

    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
            child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                         for_width);
            total += child->height_request;
        } else {
            /* FIXME this is wrong, we need to do the layout algorithm from allocate()
             * and pass in the width that algorithm would give to the child
             */
            child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                         child->width_request);
            total = MAX(total, child->height_request);
        }
    }

    total += box->padding_top;
    total += box->padding_bottom;

    return total;
}

static void
hippo_canvas_box_allocate(HippoCanvasItem *item,
                          int              width,
                          int              height)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int used;
    int expandable_count;
    int extra;
    GSList *link;

    box->allocated_width = width;
    box->allocated_height = height;
    box->request_changed_since_allocate = FALSE;

    used = 0;
    expandable_count = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        if (box->orientation == HIPPO_ORIENTATION_VERTICAL)
            used += child->height_request;
        else
            used += child->width_request;

        if (child->expand)
            expandable_count += 1;
    }

    if (expandable_count == 0) {
        extra = 0;
    } else if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        used += box->padding_top;
        used += box->padding_bottom;
        extra = (height - used) / expandable_count;
    } else {
        used += box->padding_left;
        used += box->padding_right;
        extra = (width - used) / expandable_count;
    }

    /* got less than requested, currently not handled sanely in general... */
    if (extra < 0)
        extra = 0;

    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        int top_y;
        int bottom_y;
        
        top_y = box->padding_top;
        bottom_y = height - box->padding_bottom;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            req = child->height_request;
            if (child->expand)
                req += extra;

            child->x = box->padding_left;
            child->y = child->end ? (bottom_y - req) : top_y;
            hippo_canvas_item_allocate(child->item,
                                       width, /* child->width_request, */
                                       req);
            if (child->end)
                bottom_y -= req;
            else
                top_y += req;
        }
    } else {
        int left_x;
        int right_x;

        left_x = box->padding_left;
        right_x = width - box->padding_right;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            req = child->width_request;
            if (child->expand)
                req += extra;

            child->x = child->end ? (right_x - req) : left_x;
            child->y = box->padding_top;
            hippo_canvas_item_allocate(child->item,
                                       req,
                                       height); /* child->height_request); */
            if (child->end)
                right_x -= req;
            else
                left_x += req;
        }
    }
}

static void
hippo_canvas_box_get_allocation(HippoCanvasItem *item,
                                int              *width_p,
                                int              *height_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    if (width_p)
        *width_p = box->allocated_width;
    if (height_p)
        *height_p = box->allocated_height;
}

static gboolean
hippo_canvas_box_button_press_event (HippoCanvasItem *item,
                                     HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    GSList *link;

    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        int width, height;
        hippo_canvas_item_get_allocation(child->item, &width, &height);

        if (event->x >= child->x && event->y >= child->y &&
            event->x < (child->x + width) &&
            event->y < (child->y + height)) {
            return hippo_canvas_item_process_event(HIPPO_CANVAS_ITEM(child->item),
                                                   event, child->x, child->y);
        }
    }

    return FALSE;
}

static void
hippo_canvas_box_request_changed(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    box->request_changed_since_allocate = TRUE;
}

static gboolean
hippo_canvas_box_get_needs_resize(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return box->request_changed_since_allocate;
}

static void
child_request_changed(HippoCanvasItem *child,
                      HippoCanvasBox  *box)
{
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
connect_child(HippoCanvasBox  *box,
              HippoCanvasItem *child)
{
    g_signal_connect(G_OBJECT(child), "request-changed",
                     G_CALLBACK(child_request_changed), box);
}

static void
disconnect_child(HippoCanvasBox  *box,
                 HippoCanvasItem *child)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_request_changed), box);
}

static HippoBoxChild*
find_child(HippoCanvasBox  *box,
           HippoCanvasItem *item)
{
    GSList *link;

    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        if (child->item == item)
            return child;
    }
    return NULL;
}

void
hippo_canvas_box_append(HippoCanvasBox  *box,
                        HippoCanvasItem *child,
                        HippoPackFlags   flags)
{
    HippoBoxChild *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    g_return_if_fail(find_child(box, child) == NULL);

    g_object_ref(child);
    connect_child(box, child);
    c = g_new0(HippoBoxChild, 1);
    c->item = child;
    c->expand = (flags & HIPPO_PACK_EXPAND) != 0;
    c->end = (flags & HIPPO_PACK_END) != 0;
    box->children = g_slist_append(box->children, c);
    hippo_canvas_item_set_context(child, HIPPO_CANVAS_CONTEXT(box));
    
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

void
hippo_canvas_box_remove(HippoCanvasBox  *box,
                        HippoCanvasItem *child)
{
    HippoBoxChild *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to remove a canvas item from a box it isn't in");
        return;
    }
    g_assert(c->item == child);

    box->children = g_slist_remove(box->children, c);
    disconnect_child(box, child);
    hippo_canvas_item_set_context(child, NULL);
    g_object_unref(child);
    g_free(c);

    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
hippo_canvas_box_free_children(HippoCanvasBox *box)
{
    while (box->children != NULL) {
        HippoBoxChild *child = box->children->data;
        hippo_canvas_box_remove(box, child->item);
    }
}

HippoCanvasContext*
hippo_canvas_box_get_context(HippoCanvasBox *box)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), NULL);

    return box->context;
}

int
hippo_canvas_box_get_fixed_width (HippoCanvasBox *box)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), 0);

    return box->fixed_width;
}
