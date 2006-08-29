/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-box.h"

#define DEFAULT_BACKGROUND 0xffffff00

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
static PangoLayout*     hippo_canvas_box_create_layout      (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_box_load_image         (HippoCanvasContext *context,
                                                             const char         *image_name);

/* Canvas item methods */
static void               hippo_canvas_box_sink                (HippoCanvasItem    *item);
static void               hippo_canvas_box_set_context         (HippoCanvasItem    *item,
                                                                HippoCanvasContext *context);
static void               hippo_canvas_box_paint               (HippoCanvasItem    *item,
                                                                cairo_t            *cr);
static int                hippo_canvas_box_get_width_request   (HippoCanvasItem    *item);
static int                hippo_canvas_box_get_height_request  (HippoCanvasItem    *item,
                                                                int                 for_width);
static void               hippo_canvas_box_allocate            (HippoCanvasItem    *item,
                                                                int                 width,
                                                                int                 height);
static void               hippo_canvas_box_get_allocation      (HippoCanvasItem    *item,
                                                                int                *width_p,
                                                                int                *height_p);
static gboolean           hippo_canvas_box_button_press_event  (HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static gboolean           hippo_canvas_box_motion_notify_event (HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static void               hippo_canvas_box_request_changed     (HippoCanvasItem    *item);
static gboolean           hippo_canvas_box_get_needs_resize    (HippoCanvasItem    *canvas_item);
static char*              hippo_canvas_box_get_tooltip         (HippoCanvasItem    *item,
                                                                int                 x,
                                                                int                 y);
static HippoCanvasPointer hippo_canvas_box_get_pointer         (HippoCanvasItem    *item,
                                                                int                 x,
                                                                int                 y);


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
    PROP_PADDING,
    PROP_FIXED_WIDTH,
    PROP_XALIGN,
    PROP_YALIGN,
    PROP_BACKGROUND_COLOR,
    PROP_SPACING
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBox, hippo_canvas_box, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_box_iface_init);
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT, hippo_canvas_box_iface_init_context));

static void
hippo_canvas_box_iface_init(HippoCanvasItemClass *klass)
{
    klass->sink = hippo_canvas_box_sink;
    klass->set_context = hippo_canvas_box_set_context;
    klass->paint = hippo_canvas_box_paint;
    klass->get_width_request = hippo_canvas_box_get_width_request;
    klass->get_height_request = hippo_canvas_box_get_height_request;
    klass->allocate = hippo_canvas_box_allocate;
    klass->get_allocation = hippo_canvas_box_get_allocation;
    klass->button_press_event = hippo_canvas_box_button_press_event;
    klass->motion_notify_event = hippo_canvas_box_motion_notify_event;
    klass->request_changed = hippo_canvas_box_request_changed;
    klass->get_needs_resize = hippo_canvas_box_get_needs_resize;
    klass->get_tooltip = hippo_canvas_box_get_tooltip;
    klass->get_pointer = hippo_canvas_box_get_pointer;
}

static void
hippo_canvas_box_iface_init_context (HippoCanvasContextClass *klass)
{
    klass->create_layout = hippo_canvas_box_create_layout;
    klass->load_image = hippo_canvas_box_load_image;
}

static void
hippo_canvas_box_init(HippoCanvasBox *box)
{
    box->floating = TRUE;
    box->orientation = HIPPO_ORIENTATION_VERTICAL;
    box->x_align = HIPPO_ALIGNMENT_FILL;
    box->y_align = HIPPO_ALIGNMENT_FILL;
    box->fixed_width = -1;
    box->background_color_rgba = DEFAULT_BACKGROUND;
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
                                    PROP_PADDING,
                                    g_param_spec_int("padding",
                                                     _("Padding"),
                                                     _("Set all four paddings at once"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_WRITABLE));

    /* FIXME this is mis-named, since it's really a request/minimum width, not
     * a fixed width - i.e. can get wider
     */
    g_object_class_install_property(object_class,
                                    PROP_FIXED_WIDTH,
                                    g_param_spec_int("fixed-width",
                                                     _("Fixed Width"),
                                                     _("Width request of the canvas item, or -1 to use natural width"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_XALIGN,
                                    g_param_spec_int("xalign",
                                                     _("X Alignment"),
                                                     _("What to do with extra horizontal space"),
                                                     0,
                                                     G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_YALIGN,
                                    g_param_spec_int("yalign",
                                                     _("Y Alignment"),
                                                     _("What to do with extra vertical space"),
                                                     0,
                                                     G_MAXINT,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_BACKGROUND_COLOR,
                                    g_param_spec_uint("background-color",
                                                      _("Background Color"),
                                                      _("32-bit RGBA background color"),
                                                      0,
                                                      G_MAXUINT,
                                                      DEFAULT_BACKGROUND,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_SPACING,
                                    g_param_spec_int("spacing",
                                                     _("Spacing"),
                                                     _("Spacing between items in the box"),
                                                     0,
                                                     255,
                                                     0,
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

    g_assert(!box->floating);        /* if there's still a floating ref how did we get finalized? */
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
    gboolean need_resize;
    
    box = HIPPO_CANVAS_BOX(object);

    need_resize = TRUE; /* for most of them it's true */
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
    case PROP_PADDING:
        {
            int p = g_value_get_int(value);
            box->padding_top = box->padding_bottom = p;
            box->padding_left = box->padding_right = p;
        }
        break;
    case PROP_FIXED_WIDTH:
        box->fixed_width = g_value_get_int(value);
        break;
    case PROP_XALIGN:
        box->x_align = g_value_get_int(value);
        break;
    case PROP_YALIGN:
        box->y_align = g_value_get_int(value);
        break;
    case PROP_BACKGROUND_COLOR:
        box->background_color_rgba = g_value_get_uint(value);
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), 0, 0, -1, -1);
        need_resize = FALSE;
        break;
    case PROP_SPACING:
        box->spacing = g_value_get_int(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }

    if (need_resize)
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
    case PROP_XALIGN:
        g_value_set_int(value, box->x_align);
        break;
    case PROP_YALIGN:
        g_value_set_int(value, box->y_align);
        break;
    case PROP_BACKGROUND_COLOR:
        g_value_set_uint(value, box->background_color_rgba);
        break;
    case PROP_SPACING:
        g_value_set_int(value, box->spacing);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static PangoLayout*
hippo_canvas_box_create_layout(HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    g_assert(box->context != NULL);
    
    /* just chain to our parent context */
    return hippo_canvas_context_create_layout(box->context);
}

static cairo_surface_t*
hippo_canvas_box_load_image(HippoCanvasContext *context,
                            const char         *image_name)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    g_assert(box->context != NULL);
    
    /* just chain to our parent context */
    return hippo_canvas_context_load_image(box->context, image_name);
}

static void
hippo_canvas_box_sink(HippoCanvasItem    *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    if (box->floating) {
        box->floating = FALSE;
        g_object_unref(box);
    }
}

static void
hippo_canvas_box_set_context(HippoCanvasItem    *item,
                             HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    GSList *link;
    HippoCanvasContext *child_context;
    
    /* this shortcut most importantly catches NULL == NULL */
    if (box->context == context)
        return;
    
    /* Note that we do not ref this; the parent is responsible for setting
     * it back to NULL before dropping the ref to the item
     */
    box->context = context;

    if (box->context != NULL)
        child_context = HIPPO_CANVAS_CONTEXT(box);
    else
        child_context = NULL;
    
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        hippo_canvas_item_set_context(child->item, child_context);
    }
}

static void
hippo_canvas_box_paint(HippoCanvasItem *item,
                       cairo_t         *cr)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    GSList *link;

    /* fill background, with html div type semantics - covers entire
     * item allocation, including padding
     */
    if ((box->background_color_rgba & 0xff) != 0) {
        hippo_cairo_set_source_rgba32(cr, box->background_color_rgba);
        cairo_rectangle(cr, 0, 0, box->allocated_width, box->allocated_height);
        cairo_fill(cr);
    }

    /* Now draw children */
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        hippo_canvas_item_process_paint(HIPPO_CANVAS_ITEM(child->item), cr,
                                        child->x, child->y);
    }
}

static int
hippo_canvas_box_get_width_request_internal(HippoCanvasItem *item,
                                            int             *expandable_count_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int total;
    int n_children;
    GSList *link;

    if (expandable_count_p)
        *expandable_count_p = 0;

    n_children = 0;
    total = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        n_children += 1;
        
        child->width_request = hippo_canvas_item_get_width_request(child->item);
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL)
            total = MAX(total, child->width_request);
        else {
            total += child->width_request;

            if (child->expand && expandable_count_p)
                *expandable_count_p += 1;
        }
    }

    if (box->orientation == HIPPO_ORIENTATION_HORIZONTAL)
        total += box->spacing * (n_children - 1);

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
hippo_canvas_box_get_width_request(HippoCanvasItem *item)
{
    return hippo_canvas_box_get_width_request_internal(item, NULL);
}

static int
hippo_canvas_box_get_height_request_internal(HippoCanvasItem *item,
                                             int              for_width,
                                             int             *expandable_count_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int total;
    GSList *link;
    int n_children;
    
    /* note that we get the number expandable in HEIGHT so only for orientation vertical */
    if (expandable_count_p)
        *expandable_count_p = 0;

    n_children = 0;
    total = 0;

    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        n_children += 1;
        
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
            child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                         for_width);
            total += child->height_request;

            if (child->expand && expandable_count_p)
                *expandable_count_p += 1;
        } else {
            /* FIXME this is wrong, we need to do the layout algorithm from allocate()
             * and pass in the width that algorithm would give to the child
             */
            child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                         child->width_request);
            total = MAX(total, child->height_request);
        }
    }

    if (box->orientation == HIPPO_ORIENTATION_VERTICAL)
        total += box->spacing * (n_children - 1);
    
    total += box->padding_top;
    total += box->padding_bottom;
    
    return total;
}

static int
hippo_canvas_box_get_height_request(HippoCanvasItem *item,
                                    int              for_width)
{
    return hippo_canvas_box_get_height_request_internal(item, for_width, NULL);
}

/* Pass in a size request, and have it converted to the right place
 * to actually draw, with padding removed and alignment performed
 */
void
hippo_canvas_box_align(HippoCanvasBox *box,
                       int            *x_p,
                       int            *y_p,
                       int            *width_p,
                       int            *height_p)
{
    int unpadded_width = box->allocated_width - box->padding_left - box->padding_right;
    int unpadded_height = box->allocated_height - box->padding_top - box->padding_bottom;
    
    /* If we got a too-small allocation go ahead and clamp our
     * usable area down to it
     */    
    if (*width_p > unpadded_width)
        *width_p = unpadded_width;
    if (*height_p > unpadded_height)
        *height_p = unpadded_height;

    /* -1 for w/h means to use the allocated size of the item */
    if (*width_p < 0)
        *width_p = unpadded_width;
    if (*height_p < 0)
        *height_p = unpadded_height;
    
    switch (box->x_align) {
    case HIPPO_ALIGNMENT_FILL:
        *x_p = box->padding_left;
        *width_p = unpadded_width;
        break;
    case HIPPO_ALIGNMENT_START:
        *x_p = box->padding_left;
        break;
    case HIPPO_ALIGNMENT_END:
        *x_p = box->allocated_width - box->padding_right - *width_p;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        *x_p = box->padding_left + (unpadded_width - *width_p) / 2;
        break;
    }

    switch (box->y_align) {
    case HIPPO_ALIGNMENT_FILL:
        *y_p = box->padding_top;
        *height_p = unpadded_height;
        break;
    case HIPPO_ALIGNMENT_START:
        *y_p = box->padding_top;
        break;
    case HIPPO_ALIGNMENT_END:
        *y_p = box->allocated_height - box->padding_bottom - *height_p;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        *y_p = box->padding_top + (unpadded_height - *height_p) / 2;
        break;
    }
}

static void
hippo_canvas_box_allocate(HippoCanvasItem *item,
                          int              full_width,
                          int              full_height)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int requested_width;
    int requested_height;
    int horizontal_expand_count;
    int vertical_expand_count;
    GSList *link;
    int x, y, width, height;
    int horizontal_expand_space;
    int vertical_expand_space;

    box->allocated_width = full_width;
    box->allocated_height = full_height;
    box->request_changed_since_allocate = FALSE;

    requested_width = hippo_canvas_box_get_width_request_internal(item, &horizontal_expand_count);
    requested_height = hippo_canvas_box_get_height_request_internal(item, full_width,
                                                                    &vertical_expand_count);
    
    x = 0;
    y = 0;
    width = requested_width;
    height = requested_height;
    /* This gets us the box we want to lay out into, with padding already removed */
    hippo_canvas_box_align(box, &x, &y, &width, &height);

    /* width/height are larger than request if ALIGNMENT_FILL and
     * the same size as request otherwise, naturally making this
     * expand stuff work out.
     */
    horizontal_expand_space = width - requested_width;
    if (horizontal_expand_space < 0)
        horizontal_expand_space = 0;
    vertical_expand_space = height - requested_height;
    if (vertical_expand_space < 0)
        vertical_expand_space = 0;
    
    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        int top_y;
        int bottom_y;
        
        top_y = y;
        bottom_y = y + height;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            req = child->height_request;
            if (child->expand) {
                int extra = (vertical_expand_space / vertical_expand_count);
                vertical_expand_count -= 1;
                vertical_expand_space -= extra;
                req += extra;
            }
            
            child->x = x;
            child->y = child->end ? (bottom_y - req) : top_y;
            hippo_canvas_item_allocate(child->item,
                                       width,
                                       req);
            if (child->end)
                bottom_y -= (req + box->spacing);
            else
                top_y += (req + box->spacing);
        }
    } else {
        int left_x;
        int right_x;

        left_x = x;
        right_x = x + width;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            req = child->width_request;
            if (child->expand) {
                int extra = (horizontal_expand_space / horizontal_expand_count);
                horizontal_expand_count -= 1;
                horizontal_expand_space -= extra;
                req += extra;
            }

            child->x = child->end ? (right_x - req) : left_x;
            child->y = y;
            hippo_canvas_item_allocate(child->item,
                                       req,
                                       height);
            if (child->end)
                right_x -= (req + box->spacing);
            else
                left_x += (req + box->spacing);
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

static HippoBoxChild*
find_child_at_point(HippoCanvasBox *box,
                    int             x,
                    int             y)
{
    GSList *link;
    
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        int width, height;
        hippo_canvas_item_get_allocation(child->item, &width, &height);

        if (x >= child->x && y >= child->y &&
            x < (child->x + width) &&
            y < (child->y + height)) {

            return child;
        }
    }

    return NULL;
}

static gboolean
forward_event(HippoCanvasBox *box,
              HippoEvent     *event)
{
    HippoBoxChild *child;

    child = find_child_at_point(box, event->x, event->y);

    if (child != NULL)
        return hippo_canvas_item_process_event(HIPPO_CANVAS_ITEM(child->item),
                                               event, child->x, child->y);
    else
        return FALSE;
}

static gboolean
hippo_canvas_box_button_press_event (HippoCanvasItem *item,
                                     HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return forward_event(box, event);
}

static gboolean
hippo_canvas_box_motion_notify_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return forward_event(box, event);
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

static char*
hippo_canvas_box_get_tooltip(HippoCanvasItem    *item,
                             int                 x,
                             int                 y)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);    
    HippoBoxChild *child;

    child = find_child_at_point(box, x, y);

    if (child != NULL)
        return hippo_canvas_item_get_tooltip(child->item,
                                             x - child->x,
                                             y - child->y);
    else
        return NULL;
}

static HippoCanvasPointer
hippo_canvas_box_get_pointer(HippoCanvasItem    *item,
                             int                 x,
                             int                 y)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);    
    HippoBoxChild *child;

    child = find_child_at_point(box, x, y);

    if (child != NULL)
        return hippo_canvas_item_get_pointer(child->item,
                                             x - child->x,
                                             y - child->y);
    else
        return HIPPO_CANVAS_POINTER_UNSET;
}

static void
child_request_changed(HippoCanvasItem *child,
                      HippoCanvasBox  *box)
{
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
child_paint_needed(HippoCanvasItem *item,
                   int              x,
                   int              y,
                   int              width,
                   int              height,
                   HippoCanvasBox  *box)
{
    HippoBoxChild *child;

    /* translate to our own coordinates then emit the signal again */
    
    child = find_child(box, item);
    
    hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box),
                                        x + child->x,
                                        y + child->y,
                                        width, height);
}

static void
connect_child(HippoCanvasBox  *box,
              HippoCanvasItem *child)
{
    g_signal_connect(G_OBJECT(child), "request-changed",
                     G_CALLBACK(child_request_changed), box);
    g_signal_connect(G_OBJECT(child), "paint-needed",
                     G_CALLBACK(child_paint_needed), box);
}

static void
disconnect_child(HippoCanvasBox  *box,
                 HippoCanvasItem *child)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_request_changed), box);
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_paint_needed), box);    
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
    hippo_canvas_item_sink(child);
    connect_child(box, child);
    c = g_new0(HippoBoxChild, 1);
    c->item = child;
    c->expand = (flags & HIPPO_PACK_EXPAND) != 0;
    c->end = (flags & HIPPO_PACK_END) != 0;
    box->children = g_slist_append(box->children, c);

    if (box->context != NULL)
        hippo_canvas_item_set_context(child, HIPPO_CANVAS_CONTEXT(box));
    else
        hippo_canvas_item_set_context(child, NULL);
    
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
