/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
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
static PangoLayout*     hippo_canvas_box_create_layout          (HippoCanvasContext *context);
static cairo_surface_t* hippo_canvas_box_load_image             (HippoCanvasContext *context,
                                                                 const char         *image_name);
static guint32          hippo_canvas_box_get_color              (HippoCanvasContext *context,
                                                                 HippoStockColor     color);
static void             hippo_canvas_box_register_widget_item   (HippoCanvasContext *context,
                                                                 HippoCanvasItem    *item);
static void             hippo_canvas_box_unregister_widget_item (HippoCanvasContext *context,
                                                                 HippoCanvasItem    *item);
static void             hippo_canvas_box_translate_to_widget    (HippoCanvasContext *context,
                                                                 HippoCanvasItem    *item,
                                                                 int                *x_p,
                                                                 int                *y_p);


/* Canvas item methods */
static void               hippo_canvas_box_sink                (HippoCanvasItem    *item);
static void               hippo_canvas_box_set_context         (HippoCanvasItem    *item,
                                                                HippoCanvasContext *context);
static void               hippo_canvas_box_paint               (HippoCanvasItem    *item,
                                                                cairo_t            *cr,
                                                                HippoRectangle     *damaged_box);
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

/* Canvas box methods */
static void hippo_canvas_box_paint_background           (HippoCanvasBox *box,
                                                         cairo_t        *cr,
                                                         HippoRectangle *damaged_box);
static void hippo_canvas_box_paint_children             (HippoCanvasBox *box,
                                                         cairo_t        *cr,
                                                         HippoRectangle *damaged_box);
static int  hippo_canvas_box_get_content_width_request  (HippoCanvasBox *box);
static int  hippo_canvas_box_get_content_height_request (HippoCanvasBox *box,
                                                         int             for_width);


typedef struct {
    HippoCanvasItem *item;
    /* allocated x, y */
    int              x;
    int              y;
    /* cache of last-requested sizes */
    int              width_request;
    int              height_request;
    int              height_request_for_width;
    guint            expand : 1;
    guint            end : 1;
    guint            fixed : 1;
    guint            hovering : 1;
} HippoBoxChild;

enum {
    HOVERING_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0,
    PROP_ORIENTATION,
    PROP_PADDING_TOP,
    PROP_PADDING_BOTTOM,
    PROP_PADDING_LEFT,
    PROP_PADDING_RIGHT,
    PROP_PADDING,
    PROP_BORDER_TOP,
    PROP_BORDER_BOTTOM,
    PROP_BORDER_LEFT,
    PROP_BORDER_RIGHT,
    PROP_BORDER,
    PROP_BOX_WIDTH,
    PROP_BOX_HEIGHT,
    PROP_XALIGN,
    PROP_YALIGN,
    PROP_BACKGROUND_COLOR,
    PROP_BORDER_COLOR,
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
    klass->get_color = hippo_canvas_box_get_color;
    klass->register_widget_item = hippo_canvas_box_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_box_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_box_translate_to_widget;
}

static void
hippo_canvas_box_init(HippoCanvasBox *box)
{
    box->floating = TRUE;
    box->orientation = HIPPO_ORIENTATION_VERTICAL;
    box->x_align = HIPPO_ALIGNMENT_FILL;
    box->y_align = HIPPO_ALIGNMENT_FILL;
    box->box_width = -1;
    box->box_height = -1;
    box->background_color_rgba = DEFAULT_BACKGROUND;
    box->request_changed_since_allocate = TRUE; /* be sure we do at least one allocation */
}

static void
hippo_canvas_box_class_init(HippoCanvasBoxClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_box_set_property;
    object_class->get_property = hippo_canvas_box_get_property;

    object_class->dispose = hippo_canvas_box_dispose;
    object_class->finalize = hippo_canvas_box_finalize;

    klass->paint_background = hippo_canvas_box_paint_background;
    klass->paint_children = hippo_canvas_box_paint_children;
    klass->get_content_width_request = hippo_canvas_box_get_content_width_request;
    klass->get_content_height_request = hippo_canvas_box_get_content_height_request;

    signals[HOVERING_CHANGED] =
        g_signal_new ("hovering-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      G_STRUCT_OFFSET(HippoCanvasBoxClass, hovering_changed),
                      NULL, NULL,
                      g_cclosure_marshal_VOID__BOOLEAN,
                      G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
    
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
    g_object_class_install_property(object_class,
                                    PROP_BORDER_TOP,
                                    g_param_spec_int("border-top",
                                                     _("Top Border"),
                                                     _("Border above the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_BORDER_BOTTOM,
                                    g_param_spec_int("border-bottom",
                                                     _("Bottom Border"),
                                                     _("Border below the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_BORDER_LEFT,
                                    g_param_spec_int("border-left",
                                                     _("Left Border"),
                                                     _("Border to left of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_BORDER_RIGHT,
                                    g_param_spec_int("border-right",
                                                     _("Right Border"),
                                                     _("Border to right of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_BORDER,
                                    g_param_spec_int("border",
                                                     _("Border"),
                                                     _("Set all four borders at once"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_WRITABLE));
    
    g_object_class_install_property(object_class,
                                    PROP_BOX_WIDTH,
                                    g_param_spec_int("box-width",
                                                     _("Box Width"),
                                                     _("Width request of the box including padding/border, or -1 to use natural width"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_BOX_HEIGHT,
                                    g_param_spec_int("box-height",
                                                     _("Box Height"),
                                                     _("Height request of the box including padding/border, or -1 to use natural height"),
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
                                    PROP_BORDER_COLOR,
                                    g_param_spec_uint("border-color",
                                                      _("Border Color"),
                                                      _("32-bit RGBA border color"),
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

    hippo_canvas_box_remove_all(box);

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
    case PROP_BORDER_TOP:
        box->border_top = g_value_get_int(value);
        break;
    case PROP_BORDER_BOTTOM:
        box->border_bottom = g_value_get_int(value);
        break;
    case PROP_BORDER_LEFT:
        box->border_left = g_value_get_int(value);
        break;
    case PROP_BORDER_RIGHT:
        box->border_right = g_value_get_int(value);
        break;
    case PROP_BORDER:
        {
            int p = g_value_get_int(value);
            box->border_top = box->border_bottom = p;
            box->border_left = box->border_right = p;
        }
        break;        
    case PROP_BOX_WIDTH:
        box->box_width = g_value_get_int(value);
        break;
    case PROP_BOX_HEIGHT:
        box->box_height = g_value_get_int(value);
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
    case PROP_BORDER_COLOR:
        box->border_color_rgba = g_value_get_uint(value);
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
    case PROP_BORDER_TOP:
        g_value_set_int(value, box->border_top);
        break;
    case PROP_BORDER_BOTTOM:
        g_value_set_int(value, box->border_bottom);
        break;
    case PROP_BORDER_LEFT:
        g_value_set_int(value, box->border_left);
        break;
    case PROP_BORDER_RIGHT:
        g_value_set_int(value, box->border_right);
        break;        
    case PROP_BOX_WIDTH:
        g_value_set_int(value, box->box_width);
        break;
    case PROP_BOX_HEIGHT:
        g_value_set_int(value, box->box_height);
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
    case PROP_BORDER_COLOR:
        g_value_set_uint(value, box->border_color_rgba);
        break;
    case PROP_SPACING:
        g_value_set_int(value, box->spacing);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
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

static guint32
hippo_canvas_box_get_color(HippoCanvasContext *context,
                           HippoStockColor     color)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    g_assert(box->context != NULL);

    /* chain to our parent context */
    return hippo_canvas_context_get_color(box->context, color);
}

static void
hippo_canvas_box_register_widget_item(HippoCanvasContext *context,
                                      HippoCanvasItem    *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    g_assert(box->context != NULL);

    hippo_canvas_context_register_widget_item(box->context, item);
}

static void
hippo_canvas_box_unregister_widget_item (HippoCanvasContext *context,
                                         HippoCanvasItem    *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    g_assert(box->context != NULL);

    hippo_canvas_context_unregister_widget_item(box->context, item);
}
    
static void
hippo_canvas_box_translate_to_widget(HippoCanvasContext *context,
                                     HippoCanvasItem    *item,
                                     int                *x_p,
                                     int                *y_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);
    HippoBoxChild *child;
    
    g_assert(box->context != NULL);

    child = find_child(box, item);
    g_assert(child != NULL);

    if (x_p)
        *x_p += child->x;
    if (y_p)
        *y_p += child->y;
    
    hippo_canvas_context_translate_to_widget(box->context, item, x_p, y_p);
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
    
    /* Note that we do not ref the context; the parent is responsible for setting
     * it back to NULL before dropping the ref to the item.
     *
     * Also, we set box->context to non-NULL *before* setting child contexts,
     * so they see a valid context; and set it to NULL *after* setting child
     * contexts, so they see a valid context again. i.e. the context the
     * child sees is always valid.
     */

    if (context != NULL)
        child_context = HIPPO_CANVAS_CONTEXT(box);
    else
        child_context = NULL;

    if (child_context)
        box->context = context; /* set to non-NULL before sending to children */
    
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        hippo_canvas_item_set_context(child->item, child_context);
    }

    if (child_context == NULL)
        box->context = context; /* set box context to NULL after removing it from children */
}

void
hippo_canvas_box_get_background_area (HippoCanvasBox *box,
                                      HippoRectangle *area)
{
    area->x = box->border_left;
    area->y = box->border_top;
    area->width = box->allocated_width - box->border_left - box->border_right;
    area->height = box->allocated_height - box->border_top - box->border_bottom;
}

static void
hippo_canvas_box_paint_background(HippoCanvasBox *box,
                                  cairo_t        *cr,
                                  HippoRectangle *damaged_box)
{
    /* fill background, with html div type semantics - covers entire
     * item allocation, including padding but not border
     */
    if ((box->background_color_rgba & 0xff) != 0) {
        HippoRectangle area;

        hippo_canvas_box_get_background_area(box, &area);
        
        hippo_cairo_set_source_rgba32(cr, box->background_color_rgba);
        cairo_rectangle(cr,
                        area.x, area.y,
                        area.width, area.height);
        cairo_fill(cr);
    }

    /* draw the borders, in four non-overlapping rectangles */
    if ((box->border_color_rgba & 0xff) != 0) {
        hippo_cairo_set_source_rgba32(cr, box->border_color_rgba);
        /* top */
        cairo_rectangle(cr,
                        0, 0,
                        box->allocated_width,
                        box->border_top);
        cairo_fill(cr);
        /* left */
        cairo_rectangle(cr,
                        0, box->border_top,
                        box->border_left,
                        box->allocated_height - box->border_top - box->border_bottom);
        cairo_fill(cr);
        /* right */
        cairo_rectangle(cr,
                        box->allocated_width - box->border_right,
                        box->border_top,
                        box->border_right,
                        box->allocated_height - box->border_top - box->border_bottom);
        cairo_fill(cr);
        /* bottom */
        cairo_rectangle(cr,
                        0, box->allocated_height - box->border_bottom,
                        box->allocated_width,
                        box->border_bottom);
        cairo_fill(cr);
    }
}

static void
hippo_canvas_box_paint_children(HippoCanvasBox *box,
                                cairo_t        *cr,
                                HippoRectangle *damaged_box)
{
    GSList *link;

    /* FIXME there will need to be some z-order control for fixed children,
     * probably keeping them above all layout children and allowing
     * ordering within the fixed
     */
    
    /* Now draw children */
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;        

        /* Fixed children have to be clipped to the box, since we
         * don't include them in the box's size request there's no
         * guarantee they are in the box
         */
        if (child->fixed) {
            cairo_save(cr);
            cairo_rectangle(cr, 0, 0, box->allocated_width, box->allocated_height);
            cairo_clip(cr);
        }
        
        hippo_canvas_item_process_paint(HIPPO_CANVAS_ITEM(child->item), cr, damaged_box,
                                        child->x, child->y);

        if (child->fixed) {
            cairo_restore(cr);
        }
    }
}

static void
hippo_canvas_box_paint(HippoCanvasItem *item,
                       cairo_t         *cr,
                       HippoRectangle  *damaged_box)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    HippoCanvasBoxClass *klass = HIPPO_CANVAS_BOX_GET_CLASS(box);

    g_return_if_fail(box->allocated_width > 0 && box->allocated_height > 0);
    
    cairo_save(cr);
    (* klass->paint_background) (box, cr, damaged_box);
    cairo_restore(cr);
    
    if (klass->paint_below_children != NULL) {
        cairo_save(cr);
        (* klass->paint_below_children) (box, cr, damaged_box);
        cairo_restore(cr);
    }

    cairo_save(cr);
    (* klass->paint_children) (box, cr, damaged_box);
    cairo_restore(cr);
    
    if (klass->paint_above_children != NULL) {
        cairo_save(cr);
        (* klass->paint_above_children) (box, cr, damaged_box);
        cairo_restore(cr);
    }
}

/* This is intended to not rely on size request/allocation state,
 * so it can be called from request/allocation methods.
 * So for example don't use box->allocated_width in here.
 */
static void
get_content_area_horizontal(HippoCanvasBox *box,
                            int             requested_content_width,
                            int             allocated_box_width,
                            int            *x_p,
                            int            *width_p)
{
    int left = box->border_left + box->padding_left;
    int right = box->border_right + box->padding_right;
    int unpadded_box_width;

    g_return_if_fail(requested_content_width >= 0);
    
    unpadded_box_width = allocated_box_width - left - right;
    
    switch (box->x_align) {
    case HIPPO_ALIGNMENT_FILL:
        if (x_p)
            *x_p = left;
        if (width_p)
            *width_p = unpadded_box_width;
        break;
    case HIPPO_ALIGNMENT_START:
        if (x_p)
            *x_p = left;
        if (width_p)
            *width_p = requested_content_width;
        break;
    case HIPPO_ALIGNMENT_END:
        if (x_p)
            *x_p = allocated_box_width - right - requested_content_width;
        if (width_p)
            *width_p = requested_content_width;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        if (x_p)
            *x_p = left + (unpadded_box_width - requested_content_width) / 2;
        if (width_p)
            *width_p = requested_content_width;
        break;
    }
}

static void
get_content_area_vertical(HippoCanvasBox *box,
                          int             requested_content_height,
                          int             allocated_box_height,
                          int            *y_p,
                          int            *height_p)
{
    int top = box->border_top + box->padding_top;
    int bottom = box->border_bottom + box->padding_bottom;
    int unpadded_box_height;

    g_return_if_fail(requested_content_height >= 0);
    
    unpadded_box_height = allocated_box_height - top - bottom;

    switch (box->y_align) {
    case HIPPO_ALIGNMENT_FILL:
        if (y_p)
            *y_p = top;
        if (height_p)
            *height_p = unpadded_box_height;
        break;
    case HIPPO_ALIGNMENT_START:
        if (y_p)
            *y_p = top;
        if (height_p)
            *height_p = requested_content_height;
        break;
    case HIPPO_ALIGNMENT_END:
        if (y_p)
            *y_p = allocated_box_height - bottom - requested_content_height;
        if (height_p)
            *height_p = requested_content_height;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        if (y_p)
            *y_p = top + (unpadded_box_height - requested_content_height) / 2;
        if (height_p)
            *height_p = requested_content_height;
        break;
    }
}

static void
get_content_area(HippoCanvasBox *box,
                 int             requested_content_width,
                 int             requested_content_height,
                 int             allocated_box_width,
                 int             allocated_box_height,
                 int            *x_p,
                 int            *y_p,
                 int            *width_p,
                 int            *height_p)
{
    get_content_area_horizontal(box, requested_content_width,
                                allocated_box_width,
                                x_p, width_p);
    get_content_area_vertical(box, requested_content_height,
                              allocated_box_height,
                              y_p, height_p);
}

static int
count_expandable(HippoCanvasBox *box)
{
    int count;
    GSList *link;
    count = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        if (child->expand && !child->fixed)
            ++count;
    }
    return count;
}

static int
count_width_expandable_children(HippoCanvasBox *box)
{
    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        return 0;
    } else {
        return count_expandable(box);
    }
}

static int
count_height_expandable_children(HippoCanvasBox *box)
{
    if (box->orientation == HIPPO_ORIENTATION_HORIZONTAL) {
        return 0;
    } else {
        return count_expandable(box);
    }
}

static int
hippo_canvas_box_get_content_width_request(HippoCanvasBox *box)
{
    int total;
    int n_children;
    GSList *link;

    n_children = 0;
    total = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        if (child->width_request < 0)
            child->width_request = hippo_canvas_item_get_width_request(child->item);
        
        if (child->fixed)
            continue;
        
        n_children += 1;
        
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL)
            total = MAX(total, child->width_request);
        else {
            total += child->width_request;
        }
    }

    if (box->orientation == HIPPO_ORIENTATION_HORIZONTAL)
        total += box->spacing * (n_children - 1);
    
    return total;
}

static int
hippo_canvas_box_get_content_height_request(HippoCanvasBox *box,
                                            int             for_width)
{
    int total;
    GSList *link;
    int n_children;

    /* Do fixed children, just to have their request recorded;
     * the box for_width is ignored here, fixed children just
     * always get their width request
     */
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;

        if (!child->fixed)
            continue;
        
        if (child->height_request < 0 ||
            child->height_request_for_width != child->width_request) {
            child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                         child->width_request);
            child->height_request_for_width = child->width_request;
        }
    }

    /* Now do the box-layout children */
    
    n_children = 0;
    total = 0;

    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;

            if (child->fixed)
                continue;

            n_children += 1;
            
            if (child->height_request < 0 ||
                child->height_request_for_width != for_width) {
                child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                             for_width);
                child->height_request_for_width = for_width;
            }
                
            total += child->height_request;
        }

        total += box->spacing * (n_children - 1);
    } else {
        int horizontal_expand_count;
        int horizontal_expand_space;
        int requested_content_width;
        int allocated_content_width;
        HippoCanvasBoxClass *klass;

        /* Note that this algorithm must be kept in sync with the one in allocate() */

        klass = HIPPO_CANVAS_BOX_GET_CLASS(box);
        
        horizontal_expand_count = count_width_expandable_children(box);
        requested_content_width = (* klass->get_content_width_request)(box);

        get_content_area_horizontal(box, requested_content_width,
                                    for_width, NULL, &allocated_content_width);
        
        horizontal_expand_space = allocated_content_width - requested_content_width;
        if (horizontal_expand_space < 0)
            horizontal_expand_space = 0;
        
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;
            
            if (child->fixed)
                continue;
            
            n_children += 1;

            g_assert(child->width_request >= 0);
            
            req = child->width_request;
            
            if (child->expand) {
                int extra = (horizontal_expand_space / horizontal_expand_count);
                horizontal_expand_count -= 1;
                horizontal_expand_space -= extra;
                req += extra;
            }

            if (child->height_request < 0 ||
                child->height_request_for_width != req) {            
                child->height_request = hippo_canvas_item_get_height_request(child->item,
                                                                             req);
                child->height_request_for_width = req;
            }
            
            total = MAX(total, child->height_request);
        }
    }
    
    return total;
}

static int
hippo_canvas_box_get_width_request(HippoCanvasItem *item)
{
    int content_width;
    HippoCanvasBox *box;
    HippoCanvasBoxClass *klass;

    box = HIPPO_CANVAS_BOX(item);
    klass = HIPPO_CANVAS_BOX_GET_CLASS(box);

    /* We need to call this even if just returning the box-width prop,
     * so that children can rely on getting the full request, allocate
     * cycle in order every time, and so we compute the cached requests.
     */
    content_width = (* klass->get_content_width_request)(box);

    if (box->box_width >= 0) {
        return box->box_width;
    } else {
        return content_width
            + box->padding_left + box->padding_right
            + box->border_left + box->border_right;
    }
}

static int
hippo_canvas_box_get_height_request(HippoCanvasItem *item,
                                    int              for_width)
{
    int content_height;
    int content_for_width;
    HippoCanvasBox *box;
    HippoCanvasBoxClass *klass;

    box = HIPPO_CANVAS_BOX(item);
    klass = HIPPO_CANVAS_BOX_GET_CLASS(box);

    content_for_width = for_width
        - box->padding_left - box->padding_right
        - box->border_left - box->border_right;

    /* We need to call this even if just returning the box-height prop,
     * so that children can rely on getting the full request, allocate
     * cycle in order every time, and so we compute the cached requests.
     */
    content_height = (* klass->get_content_height_request)(box, content_for_width);

    if (box->box_height >= 0) {
        return box->box_height;
    } else {
        return content_height
            + box->padding_top + box->padding_bottom
            + box->border_top + box->border_bottom;
    }
}

/* Pass in a size request, and have it converted to the right place
 * to actually draw, with padding/border removed and alignment performed.
 */
void
hippo_canvas_box_align(HippoCanvasBox *box,
                       int             requested_content_width,
                       int             requested_content_height,
                       int            *x_p,
                       int            *y_p,
                       int            *width_p,
                       int            *height_p)
{
    get_content_area(box,
                     requested_content_width,
                     requested_content_height,
                     box->allocated_width, box->allocated_height,
                     x_p, y_p, width_p, height_p);
}

static void
hippo_canvas_box_allocate(HippoCanvasItem *item,
                          int              allocated_box_width,
                          int              allocated_box_height)
{
    HippoCanvasBox *box;
    HippoCanvasBoxClass *klass;
    int requested_content_width;
    int requested_content_height;
    int allocated_content_width;
    int allocated_content_height;
    int horizontal_expand_count;
    int vertical_expand_count;
    GSList *link;
    int content_x, content_y;

    box = HIPPO_CANVAS_BOX(item);
    klass = HIPPO_CANVAS_BOX_GET_CLASS(box);
    
    /* If we haven't emitted request-changed then 
     * we are allowed to short-circuit an
     * unchanged allocation
     */
    if (!box->request_changed_since_allocate && 
        (box->allocated_width == allocated_box_width && 
         box->allocated_height == allocated_box_height))
        return;

    box->allocated_width = allocated_box_width;
    box->allocated_height = allocated_box_height;
    box->request_changed_since_allocate = FALSE;
    
    horizontal_expand_count = count_width_expandable_children(box);
    vertical_expand_count = count_height_expandable_children(box);

    requested_content_width = (* klass->get_content_width_request)(box);        

    get_content_area_horizontal(box, requested_content_width,
                                allocated_box_width,
                                &content_x, &allocated_content_width);
    
    requested_content_height = (* klass->get_content_height_request)(box,
                                                                     allocated_content_width);    

    get_content_area_vertical(box, requested_content_height,
                              allocated_box_height,
                              &content_y, &allocated_content_height);
    

#if 0
    if (box->x_align == HIPPO_ALIGNMENT_START && box->y_align == HIPPO_ALIGNMENT_START) {
        g_debug("box %p allocated %dx%d  requested %dx%d lay out into %d,%d %dx%d",
                box, box->allocated_width, box->allocated_height,
                requested_content_width, requested_content_height,
                content_x, content_y, allocated_content_width, allocated_content_height);
    }
#endif

    /* Allocate fixed children whatever they requested */
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        if (!child->fixed)
            continue;
        hippo_canvas_item_allocate(child->item, child->width_request, child->height_request);
    }


    /* Now layout the box */
    
    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        int top_y;
        int bottom_y;
        int vertical_expand_space;

        vertical_expand_space = allocated_content_height - requested_content_height;
        if (vertical_expand_space < 0)
            vertical_expand_space = 0;
        
        top_y = content_y;
        bottom_y = content_y + allocated_content_height;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            if (child->fixed)
                continue;
            
            req = child->height_request;
            if (child->expand) {
                int extra = (vertical_expand_space / vertical_expand_count);
                vertical_expand_count -= 1;
                vertical_expand_space -= extra;
                req += extra;
            }
            
            child->x = content_x;
            child->y = child->end ? (bottom_y - req) : top_y;
            hippo_canvas_item_allocate(child->item,
                                       allocated_content_width,
                                       req);
            if (child->end)
                bottom_y -= (req + box->spacing);
            else
                top_y += (req + box->spacing);
        }
    } else {
        int left_x;
        int right_x;
        int horizontal_expand_space;

        horizontal_expand_space = allocated_content_width - requested_content_width;
        if (horizontal_expand_space < 0)
            horizontal_expand_space = 0;
        
        left_x = content_x;
        right_x = content_x + allocated_content_width;
        for (link = box->children; link != NULL; link = link->next) {
            HippoBoxChild *child = link->data;
            int req;

            if (child->fixed)
                continue;
            
            req = child->width_request;
            if (child->expand) {
                int extra = (horizontal_expand_space / horizontal_expand_count);
                horizontal_expand_count -= 1;
                horizontal_expand_space -= extra;
                req += extra;
            }

            child->x = child->end ? (right_x - req) : left_x;
            child->y = content_y;
            hippo_canvas_item_allocate(child->item,
                                       req,
                                       allocated_content_height);
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
    HippoBoxChild *topmost;

    /* Box-layout children don't overlap each other, so we could just
     * return the first match, but for fixed children we have to
     * return the last match since the items are bottom-to-top in the list
     */
    
    topmost = NULL;
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        int width, height;
        hippo_canvas_item_get_allocation(child->item, &width, &height);

        if (x >= child->x && y >= child->y &&
            x < (child->x + width) &&
            y < (child->y + height)) {

            topmost = child;
        }
    }

    return topmost;
}

static HippoBoxChild*
find_hovering_child(HippoCanvasBox *box)
{
    GSList *link;
    
    for (link = box->children; link != NULL; link = link->next) {
        HippoBoxChild *child = link->data;
        if (child->hovering)
            return child;
    }

    return NULL;
}

/* the odd thing here is that sometimes we have an enter/leave from the parent,
 * and sometimes we have just a motion from the parent, and in both cases
 * we may need to send an enter/leave to one of our children
 */
static gboolean
forward_motion_event(HippoCanvasBox *box,
                     HippoEvent     *event)
{
    HippoBoxChild *child;
    HippoBoxChild *was_hovering;
    gboolean result;
    
    if (event->u.motion.detail == HIPPO_MOTION_DETAIL_ENTER ||
        event->u.motion.detail == HIPPO_MOTION_DETAIL_WITHIN)
        child = find_child_at_point(box, event->x, event->y);
    else
        child = NULL; /* leave events never have a new hover target */
    
    was_hovering = find_hovering_child(box);

    /* If our parent context/item gave us an enter or leave, be sure
     * we record it
     */
    if (child) {
        g_assert(box->hovering);
        child->hovering = TRUE;
    }

    if (was_hovering && child != was_hovering) {
        was_hovering->hovering = FALSE;
    }
    
    /* need to generate an enter event if we aren't already processing one
     * and the child is about to get a motion event
     */
    if (child && child != was_hovering &&
        event->u.motion.detail != HIPPO_MOTION_DETAIL_ENTER) {
        hippo_canvas_item_emit_motion_notify_event(child->item,
                                                   event->x - child->x,
                                                   event->y - child->y,
                                                   HIPPO_MOTION_DETAIL_ENTER);
    }

    result = FALSE; /* we only overwrite this when forwarding the current event,
                     * not with synthesized enter/leaves
                     */
    
    /* Remember child is always null for leave event.
     */
    if (child) {
        g_assert(event->u.motion.detail != HIPPO_MOTION_DETAIL_LEAVE);
        
        /* forward an enter or motion within event */
        result = hippo_canvas_item_process_event(child->item,
                                                 event, child->x, child->y);
    }

    /* Now be sure the old hover child gets a leave event, if it is not the
     * current hover child.
     */
    if (was_hovering && was_hovering != child) {
        if (event->u.motion.detail != HIPPO_MOTION_DETAIL_LEAVE)
            hippo_canvas_item_emit_motion_notify_event(was_hovering->item,
                                                       event->x - was_hovering->x,
                                                       event->y - was_hovering->y,
                                                       HIPPO_MOTION_DETAIL_LEAVE);
        else
            result = hippo_canvas_item_process_event(was_hovering->item,
                                                     event, was_hovering->x, was_hovering->y);
    }

    return result;
}


static gboolean
forward_event(HippoCanvasBox *box,
              HippoEvent     *event)
{
    HippoBoxChild *child;

    if (event->type == HIPPO_EVENT_MOTION_NOTIFY) {
        /* Motion events are a bit more complicated than the others */
        return forward_motion_event(box, event);
    } else {
        child = find_child_at_point(box, event->x, event->y);
        
        if (child != NULL) {
            return hippo_canvas_item_process_event(child->item,
                                                   event, child->x, child->y);
        } else {
            return FALSE;
        }
    }
}

static gboolean
hippo_canvas_box_button_press_event (HippoCanvasItem *item,
                                     HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    if (forward_event(box, event)) {
        return TRUE;
    } else if (box->clickable && event->u.button.button == 1) {
        /* FIXME we should really do this on release iff the same
         * item got the press, but this requires emulating "passive
         * grabs" for canvas items and right now just not important
         * enough to bother with
         */
        hippo_canvas_item_emit_activated(item);
        return TRUE;
    } else {
        return FALSE;
    }
}

static gboolean
hippo_canvas_box_motion_notify_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    gboolean was_hovering;
    gboolean handled;

    was_hovering = box->hovering;
    
    /* FIXME the warnings here need fixing; right now we aren't handling
     * e.g. unmap I think, maybe some larger problems too
     */
    
    if (event->u.motion.detail == HIPPO_MOTION_DETAIL_ENTER) {
#if 0
        g_debug("motion notify ENTER %s %p box hovering was %d",
            g_type_name_from_instance((GTypeInstance*)box), box, box->hovering);
        if (box->hovering)
            g_warning("Box got enter event but was already hovering=TRUE");
#endif

        box->hovering = TRUE;
    } else if (event->u.motion.detail == HIPPO_MOTION_DETAIL_LEAVE) {
#if 0
        g_debug("motion notify LEAVE %s %p box hovering was %d",
            g_type_name_from_instance((GTypeInstance*)box), box, box->hovering);
        if (!box->hovering)
            g_warning("Box got leave event but was not hovering=TRUE");
#endif

        box->hovering = FALSE;
    } else if (event->u.motion.detail == HIPPO_MOTION_DETAIL_WITHIN) {
#if 0
        g_debug("motion notify WITHIN %s %p box hovering was %d",
            g_type_name_from_instance((GTypeInstance*)box), box, box->hovering);
        if (!box->hovering)
            g_warning("Box got motion event but never got an enter event, hovering=FALSE");
#endif
        /* Fix it up, why not - we assert in forward_motion_event that it's right */
        box->hovering = TRUE;
    }

    handled = forward_event(box, event);

    if (was_hovering != box->hovering) {
        g_signal_emit(G_OBJECT(box), signals[HOVERING_CHANGED], 0, box->hovering);
    }

    return handled;
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

    if (child != NULL) {
        HippoCanvasPointer p;
        p = hippo_canvas_item_get_pointer(child->item,
                                          x - child->x,
                                          y - child->y);
        if (p != HIPPO_CANVAS_POINTER_UNSET)
            return p;
    }

    if (box->clickable)
        return HIPPO_CANVAS_POINTER_HAND;
    else
        return HIPPO_CANVAS_POINTER_UNSET;
}

static void
child_request_changed(HippoCanvasItem *child,
                      HippoCanvasBox  *box)
{
    HippoBoxChild *box_child;

    box_child = find_child(box, child);

    /* invalidate cached request for this child */
    box_child->width_request = -1;
    box_child->height_request = -1;
    box_child->height_request_for_width = -1;

    /* no-op if we already emitted since last allocate */
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
child_paint_needed(HippoCanvasItem *item,
                   const HippoRectangle *damage_box,
                   HippoCanvasBox  *box)
{
    HippoBoxChild *child;

    /* translate to our own coordinates then emit the signal again */
    
    child = find_child(box, item);
    
    hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box),
                                        damage_box->x + child->x,
                                        damage_box->y + child->y,
                                        damage_box->width, damage_box->height);
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
    c->fixed = (flags & HIPPO_PACK_FIXED) != 0;
    c->width_request = -1;
    c->height_request = -1;
    c->height_request_for_width = -1;
    box->children = g_slist_append(box->children, c);

    if (box->context != NULL)
        hippo_canvas_item_set_context(child, HIPPO_CANVAS_CONTEXT(box));
    else
        hippo_canvas_item_set_context(child, NULL);

    /* fixed items don't change the box's request, but they do
     * need to be request/allocated themselves, so we need to
     * do this even for fixed items
     */
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

void
hippo_canvas_box_remove_all(HippoCanvasBox *box)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    
    while (box->children != NULL) {
        HippoBoxChild *child = box->children->data;
        hippo_canvas_box_remove(box, child->item);
    }
}

void
hippo_canvas_box_move(HippoCanvasBox  *box,
                      HippoCanvasItem *child,
                      int              x,
                      int              y)
{
    HippoBoxChild *c;
    int w, h;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to move a canvas item that isn't in the box");
        return;
    }
    g_assert(c->item == child);


    if (!c->fixed) {
        g_warning("Trying to move a canvas box child that isn't fixed");
        return;
    }

    /* We only repaint, don't queue a resize - fixed items don't affect the
     * size request.
     */
    hippo_canvas_item_get_allocation(child, &w, &h);
    
    hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), c->x, c->y, w, h);
    c->x = x;
    c->y = y;
    hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), c->x, c->y, w, h);
}

HippoCanvasContext*
hippo_canvas_box_get_context(HippoCanvasBox *box)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), NULL);

    return box->context;
}

void
hippo_canvas_box_foreach(HippoCanvasBox  *box,
                         HippoCanvasForeachChildFunc func,
                         void            *data)
{
    GSList *link;
    GSList *next;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    
    link = box->children;
    while (link != NULL) {
        HippoBoxChild *child = link->data;
        next = link->next; /* allow removal of children in the foreach */
        
        (* func) (child->item, data);

        link = next;
    }
}

/* reverse children's order and toggle all the start/end flags */
void
hippo_canvas_box_reverse(HippoCanvasBox  *box)
{
    GSList *link;
    
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));

    if (box->children == NULL)
        return;
    
    /* we don't want this, because if we toggle the "end" flag 
     * that already reverses the order
     */
    /* box->children = g_slist_reverse(box->children); */

    link = box->children;
    while (link != NULL) {
        HippoBoxChild *child = link->data;
        
        child->end = !child->end;

        link = link->next;
    }

    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}
