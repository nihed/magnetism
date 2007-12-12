/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include "hippo-canvas-type-builtins.h"
#include "hippo-canvas-internal.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-layout.h"
#include "hippo-canvas-container.h"
#include "hippo-canvas-style.h"

typedef struct {
    int minimum;
    int natural;
    int adjustment;
    unsigned int does_not_fit : 1;
} AdjustInfo;


static void hippo_canvas_box_init                 (HippoCanvasBox            *box);
static void hippo_canvas_box_class_init           (HippoCanvasBoxClass       *klass);
static void hippo_canvas_box_iface_init           (HippoCanvasItemIface      *klass);
static void hippo_canvas_box_iface_init_context   (HippoCanvasContextIface   *klass);
static void hippo_canvas_box_iface_init_container (HippoCanvasContainerIface *klass);
static void hippo_canvas_box_dispose              (GObject                   *object);
static void hippo_canvas_box_finalize             (GObject                   *object);




static void hippo_canvas_box_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_canvas_box_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);


/* Canvas context methods */
static PangoLayout*     hippo_canvas_box_create_layout          (HippoCanvasContext   *context);
static cairo_surface_t* hippo_canvas_box_load_image             (HippoCanvasContext   *context,
                                                                 const char           *image_name);
static guint32          hippo_canvas_box_get_color              (HippoCanvasContext   *context,
                                                                 HippoStockColor       color);
static void             hippo_canvas_box_register_widget_item   (HippoCanvasContext   *context,
                                                                 HippoCanvasItem      *item);
static void             hippo_canvas_box_unregister_widget_item (HippoCanvasContext   *context,
                                                                 HippoCanvasItem      *item);
static void             hippo_canvas_box_translate_to_widget    (HippoCanvasContext   *context,
                                                                 HippoCanvasItem      *item,
                                                                 int                  *x_p,
                                                                 int                  *y_p);
static void             hippo_canvas_box_translate_to_screen    (HippoCanvasContext   *context,
                                                                 HippoCanvasItem      *item,
                                                                 int                  *x_p,
                                                                 int                  *y_p);
static void             hippo_canvas_box_affect_color           (HippoCanvasContext   *context,
                                                                 guint32              *color_rgba_p);
static void             hippo_canvas_box_affect_font_desc       (HippoCanvasContext   *context,
                                                                 PangoFontDescription *font_desc);
static void             hippo_canvas_box_style_changed          (HippoCanvasContext   *context,
                                                                 gboolean              resize_needed);


/* Canvas container methods */
static gboolean         hippo_canvas_box_get_child_visible      (HippoCanvasContainer *container,
                                                                 HippoCanvasItem      *child);
static void             hippo_canvas_box_set_child_visible      (HippoCanvasContainer *container,
                                                                 HippoCanvasItem      *child,
                                                                 gboolean              visible);

/* Canvas item methods */
static void               hippo_canvas_box_sink                (HippoCanvasItem    *item);
static HippoCanvasContext* hippo_canvas_box_get_context        (HippoCanvasItem    *item);
static void               hippo_canvas_box_set_context         (HippoCanvasItem    *item,
                                                                HippoCanvasContext *context);
static void               hippo_canvas_box_set_parent          (HippoCanvasItem    *item,
                                                                HippoCanvasContainer *container);
static HippoCanvasContainer* hippo_canvas_box_get_parent       (HippoCanvasItem    *item);
static void               hippo_canvas_box_paint               (HippoCanvasItem    *item,
                                                                cairo_t            *cr,
                                                                HippoRectangle     *damaged_box);
static void               hippo_canvas_box_get_width_request   (HippoCanvasItem   *item,
                                                                int               *min_width_p,
                                                                int               *natural_width_p);
static void               hippo_canvas_box_get_height_request  (HippoCanvasItem   *item,
                                                                int                for_width,
                                                                int               *min_height_p,
                                                                int               *natural_height_p);
static void               hippo_canvas_box_allocate            (HippoCanvasItem    *item,
                                                                int                 width,
                                                                int                 height,
                                                                gboolean            origin_changed);
static void               hippo_canvas_box_get_allocation      (HippoCanvasItem    *item,
                                                                int                *width_p,
                                                                int                *height_p);
static gboolean           hippo_canvas_box_button_press_event  (HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static gboolean           hippo_canvas_box_button_release_event(HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static gboolean           hippo_canvas_box_motion_notify_event (HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static gboolean           hippo_canvas_box_scroll_event        (HippoCanvasItem    *item,
                                                                HippoEvent         *event);
static void               hippo_canvas_box_request_changed     (HippoCanvasItem    *item);
static gboolean           hippo_canvas_box_get_needs_request   (HippoCanvasItem    *canvas_item);
static char*              hippo_canvas_box_get_tooltip         (HippoCanvasItem    *item,
                                                                int                 x,
                                                                int                 y,
                                                                HippoRectangle     *for_area);
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

static void hippo_canvas_box_get_content_width_request  (HippoCanvasBox *box,
                                                         int            *min_width_p,
                                                         int            *natural_width_p);
static void hippo_canvas_box_get_content_height_request (HippoCanvasBox *box,
                                                         int             for_width,
                                                         int            *min_width_p,
                                                         int            *natural_width_p);
typedef struct _BoxChildQDataNode BoxChildQDataNode;
 
struct _BoxChildQDataNode {
    GQuark key;
    gpointer data;
    GDestroyNotify notify;
    BoxChildQDataNode *next;
};

typedef struct {
    HippoCanvasBoxChild public;

    guint ref_count;
    
    /* allocated x, y */
    int              x;
    int              y;
    
    /* cache of last-requested sizes */
    int              min_width;       /* -1 if invalid */
    int              natural_width;   /* always valid and >= 0 when min_width is valid */
    int              min_height;      /* -1 if invalid */
    int              natural_height;  /* always valid and >= 0 when min_height is valid */
    int              height_request_for_width; /* width the height_request is valid for */
    
    guint            requesting : 1; /* used to detect bugs */

    /* Whether the mouse is over the child */
    guint            hovering : 1;

    /* mouse button click tracking */
    guint            left_release_pending : 1;
    guint            middle_release_pending : 1;
    guint            right_release_pending : 1;

    /* Layout manager data */
    BoxChildQDataNode  *qdata;

} BoxChildPrivate;

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
    PROP_SPACING,
    PROP_COLOR,
    PROP_COLOR_CASCADE,
    PROP_COLOR_SET,
    PROP_FONT,
    PROP_FONT_DESC,
    PROP_FONT_CASCADE,
    PROP_TOOLTIP,
    PROP_DEBUG_NAME
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBox, hippo_canvas_box, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_box_iface_init);
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTEXT, hippo_canvas_box_iface_init_context);
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_CONTAINER, hippo_canvas_box_iface_init_container));

static void
hippo_canvas_box_iface_init(HippoCanvasItemIface *klass)
{
    klass->sink = hippo_canvas_box_sink;
    klass->get_context = hippo_canvas_box_get_context;
    klass->set_context = hippo_canvas_box_set_context;
    klass->set_parent = hippo_canvas_box_set_parent;
    klass->get_parent = hippo_canvas_box_get_parent;
    klass->paint = hippo_canvas_box_paint;
    klass->get_width_request = hippo_canvas_box_get_width_request;
    klass->get_height_request = hippo_canvas_box_get_height_request;
    klass->allocate = hippo_canvas_box_allocate;
    klass->get_allocation = hippo_canvas_box_get_allocation;
    klass->button_press_event = hippo_canvas_box_button_press_event;
    klass->button_release_event = hippo_canvas_box_button_release_event;
    klass->motion_notify_event = hippo_canvas_box_motion_notify_event;
    klass->scroll_event = hippo_canvas_box_scroll_event;
    klass->request_changed = hippo_canvas_box_request_changed;
    klass->get_needs_request = hippo_canvas_box_get_needs_request;
    klass->get_tooltip = hippo_canvas_box_get_tooltip;
    klass->get_pointer = hippo_canvas_box_get_pointer;
}

static void
hippo_canvas_box_iface_init_context (HippoCanvasContextIface *klass)
{
    klass->create_layout = hippo_canvas_box_create_layout;
    klass->load_image = hippo_canvas_box_load_image;
    klass->get_color = hippo_canvas_box_get_color;
    klass->register_widget_item = hippo_canvas_box_register_widget_item;
    klass->unregister_widget_item = hippo_canvas_box_unregister_widget_item;
    klass->translate_to_widget = hippo_canvas_box_translate_to_widget;
    klass->translate_to_screen = hippo_canvas_box_translate_to_screen;
    klass->affect_color = hippo_canvas_box_affect_color;
    klass->affect_font_desc = hippo_canvas_box_affect_font_desc;
    klass->style_changed = hippo_canvas_box_style_changed;
}

static void
hippo_canvas_box_iface_init_container (HippoCanvasContainerIface *klass)
{
    klass->get_child_visible = hippo_canvas_box_get_child_visible;
    klass->set_child_visible = hippo_canvas_box_set_child_visible;
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
    box->background_color_rgba = HIPPO_CANVAS_DEFAULT_BACKGROUND_COLOR;
    box->content_min_width = -1;
    box->content_min_height = -1;
    box->needs_width_request = TRUE; /* be sure we do at least one allocation */
    box->needs_height_request = TRUE;

    box->color_cascade = HIPPO_CASCADE_MODE_INHERIT;
    box->font_cascade = HIPPO_CASCADE_MODE_INHERIT;
}

static void
hippo_canvas_box_class_init(HippoCanvasBoxClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_box_set_property;
    object_class->get_property = hippo_canvas_box_get_property;

    object_class->dispose = hippo_canvas_box_dispose;
    object_class->finalize = hippo_canvas_box_finalize;

    klass->default_color = HIPPO_CANVAS_DEFAULT_COLOR;
    
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
    
    /**
     * HippoCanvasBox:orientation
     *
     * A box can stack items vertically #HIPPO_ORIENTATION_VERTICAL, like #GtkVBox,
     * or put them in a horizontal row, #HIPPO_ORIENTATION_HORIZONTAL, like #GtkHBox.
     *
     */
    g_object_class_install_property(object_class,
                                    PROP_ORIENTATION,
                                    g_param_spec_enum("orientation",
                                                      _("Orientation"),
                                                      _("Direction of the box"),
                                                      HIPPO_TYPE_ORIENTATION,
                                                      HIPPO_ORIENTATION_VERTICAL,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:padding-top
     *
     * Space inside the background color, on the top edge. Contrast with the
     * border, which is outside the background color.
     *
     */
    g_object_class_install_property(object_class,
                                    PROP_PADDING_TOP,
                                    g_param_spec_int("padding-top",
                                                     _("Top Padding"),
                                                     _("Padding above the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:padding-bottom
     *
     * Space inside the background color, on the bottom edge. Contrast with the
     * border, which is outside the background color.
     *
     */    
    g_object_class_install_property(object_class,
                                    PROP_PADDING_BOTTOM,
                                    g_param_spec_int("padding-bottom",
                                                     _("Bottom Padding"),
                                                     _("Padding below the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:padding-left
     *
     * Space inside the background color, on the left edge. Contrast with the
     * border, which is outside the background color.
     *
     */    
    g_object_class_install_property(object_class,
                                    PROP_PADDING_LEFT,
                                    g_param_spec_int("padding-left",
                                                     _("Left Padding"),
                                                     _("Padding to left of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:padding-right
     *
     * Space inside the background color, on the right edge. Contrast with the
     * border, which is outside the background color.
     *
     */    
    g_object_class_install_property(object_class,
                                    PROP_PADDING_RIGHT,
                                    g_param_spec_int("padding-right",
                                                     _("Right Padding"),
                                                     _("Padding to right of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:padding
     *
     * Write-only convenience property to set padding-top,
     * padding-bottom, padding-left, and padding-right all at once.
     *
     */    
    g_object_class_install_property(object_class,
                                    PROP_PADDING,
                                    g_param_spec_int("padding",
                                                     _("Padding"),
                                                     _("Set all four paddings at once"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_WRITABLE));

    
    /**
     * HippoCanvasBox:border-top
     *
     * Space outside the background color, on the top edge. Contrast with the
     * padding, which is inside the background color.     
     */
    g_object_class_install_property(object_class,
                                    PROP_BORDER_TOP,
                                    g_param_spec_int("border-top",
                                                     _("Top Border"),
                                                     _("Border above the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:border-bottom
     *
     * Space outside the background color, on the bottom edge. Contrast with the
     * padding, which is inside the background color.     
     */    
    g_object_class_install_property(object_class,
                                    PROP_BORDER_BOTTOM,
                                    g_param_spec_int("border-bottom",
                                                     _("Bottom Border"),
                                                     _("Border below the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasBox:border-left
     *
     * Space outside the background color, on the left edge. Contrast with the
     * padding, which is inside the background color.     
     */        
    g_object_class_install_property(object_class,
                                    PROP_BORDER_LEFT,
                                    g_param_spec_int("border-left",
                                                     _("Left Border"),
                                                     _("Border to left of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:border-right
     *
     * Space outside the background color, on the right edge. Contrast with the
     * padding, which is inside the background color.     
     */            
    g_object_class_install_property(object_class,
                                    PROP_BORDER_RIGHT,
                                    g_param_spec_int("border-right",
                                                     _("Right Border"),
                                                     _("Border to right of the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:border
     *
     * Write-only convenience property to set border-top,
     * border-bottom, border-left, and border-right all at once.
     *
     */        
    g_object_class_install_property(object_class,
                                    PROP_BORDER,
                                    g_param_spec_int("border",
                                                     _("Border"),
                                                     _("Set all four borders at once"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:box-width
     *
     * This forces both the natural and minimum width (for the entire
     * box, including border and padding). It should probably be
     * deprecated in favor of separate natural-width and min-width
     * properties, and potentially those properties should not include
     * the border and/or padding. Feel free to analyze and then resolve
     * the issue and send us a patch!
     *
     */            
    g_object_class_install_property(object_class,
                                    PROP_BOX_WIDTH,
                                    g_param_spec_int("box-width",
                                                     _("Box Width"),
                                                     _("Width request of the box including padding/border, or -1 to use natural width"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:box-height
     *
     * This forces both the natural and minimum height (for the entire
     * box, including border and padding). It should probably be
     * deprecated in favor of separate natural-height and min-height
     * properties, and potentially those properties should not include
     * the border and/or padding. Feel free to analyze and then resolve
     * the issue and send us a patch!
     *
     */
    g_object_class_install_property(object_class,
                                    PROP_BOX_HEIGHT,
                                    g_param_spec_int("box-height",
                                                     _("Box Height"),
                                                     _("Height request of the box including padding/border, or -1 to use natural height"),
                                                     -1,
                                                     G_MAXINT,
                                                     -1,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:xalign
     *
     * Remember that most canvas items, including text and image
     * items, derive from #HippoCanvasBox.  The alignment properties
     * determine whether the "content" of the box - which may be the
     * child items, image, text, or other main point of the canvas
     * item you're using - will be aligned start (left/top), aligned center,
     * aligned end (right/bottom), or stretched to fill the whole box.
     *
     * Alignment only matters if the box gets extra space (more space than
     * the natural size of the item).
     */
    g_object_class_install_property(object_class,
                                    PROP_XALIGN,
                                    g_param_spec_enum("xalign",
                                                      _("X Alignment"),
                                                      _("What to do with extra horizontal space"),
                                                      HIPPO_TYPE_ITEM_ALIGNMENT,
                                                      HIPPO_ALIGNMENT_FILL,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:yalign
     *
     * Remember that most canvas items, including text and image
     * items, derive from #HippoCanvasBox.  The alignment properties
     * determine whether the "content" of the box - which may be the
     * child items, image, text, or other main point of the canvas
     * item you're using - will be aligned start (left/top), aligned center,
     * aligned end (right/bottom), or stretched to fill the whole box.
     *
     * Alignment only matters if the box gets extra space (more space than
     * the natural size of the item).
     */    
    g_object_class_install_property(object_class,
                                    PROP_YALIGN,
                                    g_param_spec_enum("yalign",
                                                      _("Y Alignment"),
                                                      _("What to do with extra vertical space"),
                                                      HIPPO_TYPE_ITEM_ALIGNMENT,
                                                      HIPPO_ALIGNMENT_FILL,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:background-color
     *
     * Sets an RGBA background color (pack the color into 32-bit unsigned int, just type
     * "0xff0000ff" for example for opaque red). The background color covers the
     * padding but not the border of the box.
     */
    g_object_class_install_property(object_class,
                                    PROP_BACKGROUND_COLOR,
                                    g_param_spec_uint("background-color",
                                                      _("Background Color"),
                                                      _("32-bit RGBA background color"),
                                                      0,
                                                      G_MAXUINT,
                                                      HIPPO_CANVAS_DEFAULT_BACKGROUND_COLOR,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:border-color
     *
     * Sets an RGBA border color (pack the color into 32-bit unsigned int, just type
     * "0xff0000ff" for example for opaque red). The border color covers the
     * border of the box; you have to set the border to a nonzero size with the
     * border property.
     *
     * Frequently, the border is transparent and just used for spacing.
     */    
    g_object_class_install_property(object_class,
                                    PROP_BORDER_COLOR,
                                    g_param_spec_uint("border-color",
                                                      _("Border Color"),
                                                      _("32-bit RGBA border color"),
                                                      0,
                                                      G_MAXUINT,
                                                      HIPPO_CANVAS_DEFAULT_BACKGROUND_COLOR,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:spacing
     *
     * The spacing is a gap to leave between all child items in the box. If you want a gap
     * in only one place, try setting a transparent border on one side of the child, rather than
     * setting the spacing.
     */        
    g_object_class_install_property(object_class,
                                    PROP_SPACING,
                                    g_param_spec_int("spacing",
                                                     _("Spacing"),
                                                     _("Spacing between items in the box"),
                                                     0,
                                                     255,
                                                     0,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:color
     *
     * This property only matters if the subclass of #HippoCanvasBox you are using supports
     * it. Specifically text items use this color for the text color.
     *
     * The value is an RGBA color, i.e. a 32-bit unsigned int,
     * "0xff0000ff" for example for opaque red.
     *
     * This property is ignored if the color-set property is %FALSE.
     */
    g_object_class_install_property(object_class,
                                    PROP_COLOR,
                                    g_param_spec_uint("color",
                                                      _("Foreground Color"),
                                                      _("32-bit RGBA foreground text color"),
                                                      0,
                                                      G_MAXUINT,
                                                      HIPPO_CANVAS_DEFAULT_COLOR,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasBox:color-cascade
     *
     * If the color-set property is %FALSE, this property determines whether the item inherits
     * its foreground color property from its containing canvas item, or whether the item uses
     * a default color which may depend on the canvas item type.
     */    
    g_object_class_install_property(object_class,
                                    PROP_COLOR_CASCADE,
                                    g_param_spec_enum("color-cascade",
                                                      _("Foreground Color Cascade"),
                                                      _("Whether to use parent's color if ours is unset"),
                                                      HIPPO_TYPE_CASCADE_MODE,
                                                      HIPPO_CASCADE_MODE_INHERIT,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasBox:color-set
     *
     * Determines whether the color property is used, or whether a default color is
     * used according to the color-cascade property. This flag gets set automatically
     * if you write to the color property.
     */        
    g_object_class_install_property(object_class,
                                    PROP_COLOR_SET,
                                    g_param_spec_boolean("color-set",
                                                         _("Foreground Color Set"),
                                                         _("Whether a foreground color was set"),
                                                         FALSE,
                                                         G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasBox:font
     * 
     * The font to use as a Pango font description string. Only matters for text items, or for
     * boxes that contain text items. If a box contains text items, and the child items
     * have the font-cascade property set to #HIPPO_CASCADE_MODE_INHERIT, then the child
     * items will use the font from the containing box unless they have explicitly set
     * their own font.
     *
     * This property is just a way to set the font-desc property, using a string
     * instead of a #PangoFontDescription object.
     */
    g_object_class_install_property(object_class,
                                    PROP_FONT,
                                    g_param_spec_string("font",
                                                        _("Font"),
                                                        _("Font description as a string"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    /**
     * HippoCanvasBox:font-desc
     *
     * The font to use for text in the item and its children. Children will inherit this font
     * if their font-cascade is set to #HIPPO_CASCADE_MODE_INHERIT and they don't set a font
     * themselves.
     *
     * If the font description is %NULL then the font will be inherited or a default will be
     * used, according to the font-cascade property.
     */
    g_object_class_install_property(object_class,
                                    PROP_FONT_DESC,
                                    g_param_spec_boxed("font-desc",
                                                       _("Font Description"),
                                                       _("Font description as a PangoFontDescription object"),
                                                       PANGO_TYPE_FONT_DESCRIPTION,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:font-cascade
     *
     * If the font-desc property is %NULL, this property determines whether the item inherits
     * its font property from its containing canvas item, or whether the item uses
     * a default font.
     * 
     */    
    g_object_class_install_property(object_class,
                                    PROP_FONT_CASCADE,
                                    g_param_spec_enum("font-cascade",
                                                      _("Font Cascade"),
                                                      _("Whether to use parent's font if ours is unset"),
                                                      HIPPO_TYPE_CASCADE_MODE,
                                                      HIPPO_CASCADE_MODE_INHERIT,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    /**
     * HippoCanvasBox:tooltip
     *
     * A string to display as a tooltip on this canvas item.
     * 
     */
    g_object_class_install_property(object_class,
                                    PROP_TOOLTIP,
                                    g_param_spec_string("tooltip",
                                                        _("Tooltip"),
                                                        _("Tooltip to display on mouse hover"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    
    /**
     * HippoCanvasBox:debug-name
     *
     * If set, debug-spew detailed information about size negotiation, prefixed with this name
     */
    g_object_class_install_property(object_class,
                                    PROP_DEBUG_NAME,
                                    g_param_spec_string("debug_name",
                                                        _("Debug Name"),
                                                        _("Use this string to mark size negotiation debug spew"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_box_dispose(GObject *object)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);

    hippo_canvas_box_clear(box);
    hippo_canvas_box_set_layout(box, NULL);

    if (box->style) {
        g_object_unref(box->style);
        box->style = NULL;
    }

    hippo_canvas_item_emit_destroy(HIPPO_CANVAS_ITEM(object));
    
    G_OBJECT_CLASS(hippo_canvas_box_parent_class)->dispose(object);
}

static void
hippo_canvas_box_finalize(GObject *object)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(object);

    g_assert(!box->floating);        /* if there's still a floating ref how did we get finalized? */
    g_assert(box->children == NULL); /* should have vanished in dispose */

    g_free(box->tooltip);
    
    G_OBJECT_CLASS(hippo_canvas_box_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_box_new(void)
{
    HippoCanvasBox *box = g_object_new(HIPPO_TYPE_CANVAS_BOX, NULL);


    return HIPPO_CANVAS_ITEM(box);
}

static void
ensure_style(HippoCanvasBox *box)
{
    if (box->style == NULL) {
        box->style = g_object_new(HIPPO_TYPE_CANVAS_STYLE, NULL);
    }
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
        box->orientation = g_value_get_enum(value);
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
        box->x_align = g_value_get_enum(value);
        break;
    case PROP_YALIGN:
        box->y_align = g_value_get_enum(value);
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
    case PROP_COLOR:
        ensure_style(box);
        g_object_set_property(G_OBJECT(box->style), "color", value);
        hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                FALSE);
        need_resize = FALSE;
        break;
    case PROP_COLOR_SET:
        if (g_value_get_boolean(value) || box->style != NULL) {
            ensure_style(box);
            g_object_set_property(G_OBJECT(box->style), "color-set", value);
            hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                    FALSE);
        }
        need_resize = FALSE;
        break;
    case PROP_COLOR_CASCADE:
        {
            HippoCascadeMode new_mode = g_value_get_enum(value);
            if (new_mode != box->color_cascade) {
                box->color_cascade = new_mode;
                hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                        FALSE);
            }
        }
        need_resize = FALSE;
        break;
    case PROP_FONT:
        if (!(g_value_get_string(value) == NULL && box->style == NULL)) {
            ensure_style(box);
            g_object_set_property(G_OBJECT(box->style), "font", value);
            hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                    TRUE);
        }
        break;
    case PROP_FONT_DESC:
        if (!(g_value_get_boxed(value) == NULL && box->style == NULL)) {
            ensure_style(box);
            g_object_set_property(G_OBJECT(box->style), "font-desc", value);
            hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                    TRUE);
        }
        break;
    case PROP_FONT_CASCADE:
        {
            HippoCascadeMode new_mode = g_value_get_enum(value);
            if (new_mode != box->font_cascade) {
                box->font_cascade = new_mode;
                hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                                        TRUE);
            }
        }
        break;
    case PROP_TOOLTIP:
        {
            const char *new_tip = g_value_get_string(value);
            if (new_tip != box->tooltip) {
                gboolean changed = TRUE;
                
                if (new_tip && box->tooltip &&
                    strcmp(new_tip, box->tooltip) == 0) {
                    changed = FALSE;
                }

                if (changed) {
                    g_free(box->tooltip);
                    box->tooltip = g_strdup(new_tip);
                    hippo_canvas_item_emit_tooltip_changed(HIPPO_CANVAS_ITEM(box));
                }
            }
        }
        need_resize = FALSE;
        break;
    case PROP_DEBUG_NAME:
        {
            const char *new_name  = g_value_get_string(value);
            if (new_name != box->debug_name) {
                g_free(box->debug_name);
                box->debug_name = g_strdup(new_name);
            }
        }
        need_resize = FALSE;
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
        g_value_set_enum(value, box->orientation);
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
        g_value_set_enum(value, box->x_align);
        break;
    case PROP_YALIGN:
        g_value_set_enum(value, box->y_align);
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
    case PROP_COLOR:
        if (box->style) {
            g_object_get_property(G_OBJECT(box->style), "color", value);
        } else {
            g_value_set_uint(value, HIPPO_CANVAS_DEFAULT_BACKGROUND_COLOR);
        }
        break;
    case PROP_COLOR_CASCADE:
        g_value_set_enum(value, box->color_cascade);
        break;
    case PROP_COLOR_SET:
        if (box->style) {
            g_object_get_property(G_OBJECT(box->style), "color-set", value);
        } else {
            g_value_set_boolean(value, FALSE);
        }
        break;
    case PROP_FONT:
        if (box->style) {
            g_object_get_property(G_OBJECT(box->style), "font", value);
        } else {
            g_value_set_string(value, NULL);
        }
        break;
    case PROP_FONT_DESC:
        if (box->style) {
            g_object_get_property(G_OBJECT(box->style), "font-desc", value);
        } else {
            g_value_set_boxed(value, NULL);
        }
        break;
    case PROP_FONT_CASCADE:
        g_value_set_enum(value, box->font_cascade);
        break;
    case PROP_TOOLTIP:
        g_value_set_string(value, box->tooltip);
        break;
    case PROP_DEBUG_NAME:
        g_value_set_string(value, box->debug_name);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static BoxChildPrivate*
find_child(HippoCanvasBox  *box,
           HippoCanvasItem *item)
{
    GSList *link;

    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        if (child->item == item)
            return (BoxChildPrivate *)child;
    }
    
    return NULL;
}

static PangoLayout*
hippo_canvas_box_create_layout(HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);
    PangoLayout *layout;
    
    g_assert(box->context != NULL);
    
    /* Chain to our parent context */
    layout = hippo_canvas_context_create_layout(box->context);

    return layout;
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
    BoxChildPrivate *child;
    
    g_assert(box->context != NULL);

    child = find_child(box, item);
    g_assert(child != NULL);

    if (x_p)
        *x_p += child->x;
    if (y_p)
        *y_p += child->y;
    
    hippo_canvas_context_translate_to_widget(box->context,
                                             HIPPO_CANVAS_ITEM(box), x_p, y_p);
}
    
static void
hippo_canvas_box_translate_to_screen(HippoCanvasContext *context,
                                     HippoCanvasItem    *item,
                                     int                *x_p,
                                     int                *y_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);
    BoxChildPrivate *child;
    
    g_assert(box->context != NULL);

    child = find_child(box, item);
    g_assert(child != NULL);

    if (x_p)
        *x_p += child->x;
    if (y_p)
        *y_p += child->y;
    
    hippo_canvas_context_translate_to_screen(box->context,
                                             HIPPO_CANVAS_ITEM(box), x_p, y_p);
}

static void
hippo_canvas_box_affect_color(HippoCanvasContext *context,
                              guint32            *color_rgba_p)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);
    
    if (box->context && box->color_cascade == HIPPO_CASCADE_MODE_INHERIT)
        hippo_canvas_context_affect_color(box->context, color_rgba_p);
    
    if (box->style)
        hippo_canvas_style_affect_color(box->style, color_rgba_p);
}

static void
hippo_canvas_box_affect_font_desc(HippoCanvasContext   *context,
                                  PangoFontDescription *font_desc)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    if (box->context && box->font_cascade == HIPPO_CASCADE_MODE_INHERIT)
        hippo_canvas_context_affect_font_desc(box->context, font_desc);
    
    if (box->style)
        hippo_canvas_style_affect_font_desc(box->style, font_desc);
}

static void
hippo_canvas_box_style_changed(HippoCanvasContext   *context,
                               gboolean              resize_needed)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(context);

    if (resize_needed)
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
    else
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), 0, 0, -1, -1);
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
on_context_style_changed(HippoCanvasContext *context,
                         gboolean            resize_needed,
                         HippoCanvasBox     *box)
{    
    /* If our context's style changed, then our own style also
     * changed since we chain up to the outer context.
     */
    hippo_canvas_context_emit_style_changed(HIPPO_CANVAS_CONTEXT(box),
                                            resize_needed);
}

static HippoCanvasContext*
hippo_canvas_box_get_context(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return box->context;
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

    if (child_context) {
        box->context = context; /* set to non-NULL before sending to children */
        g_signal_connect(G_OBJECT(box->context), "style-changed",
                         G_CALLBACK(on_context_style_changed),
                         box);
    }
    
    for (link = box->children; link != NULL; link = link->next) {
        BoxChildPrivate *child = link->data;
        hippo_canvas_item_set_context(child->public.item, child_context);

        /* clear child state flags */
        child->hovering = FALSE;
        child->left_release_pending = FALSE;
        child->middle_release_pending = FALSE;
        child->middle_release_pending = FALSE; 
    }

    if (child_context == NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(box->context),
                                             G_CALLBACK(on_context_style_changed),
                                             box);
        box->context = context; /* set box context to NULL after removing it from children */
    }

    box->hovering = FALSE;
}

static void
hippo_canvas_box_set_parent (HippoCanvasItem      *item,
                             HippoCanvasContainer *container)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    /* Note that we do not ref the parent; the parent is responsible for setting
     * it back to NULL before dropping the ref to the item.
     */
    if (box->parent == container)
        return;

    box->parent = container;

    /* I believe GTK would queue resize here, but we do it in the container_add() equivalent */
}

static HippoCanvasContainer*
hippo_canvas_box_get_parent (HippoCanvasItem    *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return box->parent;
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
        /* left */
        cairo_rectangle(cr,
                        0, box->border_top,
                        box->border_left,
                        box->allocated_height - box->border_top - box->border_bottom);
        /* right */
        cairo_rectangle(cr,
                        box->allocated_width - box->border_right,
                        box->border_top,
                        box->border_right,
                        box->allocated_height - box->border_top - box->border_bottom);
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
        BoxChildPrivate *child = link->data;        

        if (!child->public.visible)
            continue;
        
        /* Fixed children have to be clipped to the box, since we
         * don't include them in the box's size request there's no
         * guarantee they are in the box
         */
        if (child->public.fixed) {
            cairo_save(cr);
            cairo_rectangle(cr, 0, 0, box->allocated_width, box->allocated_height);
            cairo_clip(cr);
        }
        
        hippo_canvas_item_process_paint(HIPPO_CANVAS_ITEM(child->public.item), cr, damaged_box,
                                        child->x, child->y);

        if (child->public.fixed) {
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
                            int             natural_content_width,
                            int             allocated_box_width,
                            int            *x_p,
                            int            *width_p)
{
    int left = box->border_left + box->padding_left;
    int right = box->border_right + box->padding_right;
    int unpadded_box_width;
    int content_width;

    g_return_if_fail(requested_content_width >= 0);

    if (natural_content_width < allocated_box_width)
        content_width = natural_content_width;
    else
        content_width = MAX(requested_content_width, allocated_box_width);

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
            *width_p = content_width;
        break;
    case HIPPO_ALIGNMENT_END:
        if (x_p)
            *x_p = allocated_box_width - right - content_width;
        if (width_p)
            *width_p = content_width;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        if (x_p)
            *x_p = left + (unpadded_box_width - content_width) / 2;
        if (width_p)
            *width_p = content_width;
        break;
    }
}

static void
get_content_area_vertical(HippoCanvasBox *box,
                          int             requested_content_height,
                          int             natural_content_height,
                          int             allocated_box_height,
                          int            *y_p,
                          int            *height_p)
{
    int top = box->border_top + box->padding_top;
    int bottom = box->border_bottom + box->padding_bottom;
    int unpadded_box_height;
    int content_height;

    g_return_if_fail(requested_content_height >= 0);
    
    if (natural_content_height < allocated_box_height)
        content_height = natural_content_height;
    else
        content_height = MAX(requested_content_height, allocated_box_height);

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
            *height_p = content_height;
        break;
    case HIPPO_ALIGNMENT_END:
        if (y_p)
            *y_p = allocated_box_height - bottom - content_height;
        if (height_p)
            *height_p = content_height;
        break;
    case HIPPO_ALIGNMENT_CENTER:
        if (y_p)
            *y_p = top + (unpadded_box_height - content_height) / 2;
        if (height_p)
            *height_p = content_height;
        break;
    }
}

static void
get_content_area(HippoCanvasBox *box,
                 int             requested_content_width,
                 int             requested_content_height,
                 int             natural_content_width,
                 int             natural_content_height,
                 int             allocated_box_width,
                 int             allocated_box_height,
                 int            *x_p,
                 int            *y_p,
                 int            *width_p,
                 int            *height_p)
{
    get_content_area_horizontal(box, requested_content_width,
                                natural_content_width,
                                allocated_box_width,
                                x_p, width_p);
    get_content_area_vertical(box, requested_content_height,
                              natural_content_height,
                              allocated_box_height,
                              y_p, height_p);
}

static gboolean
child_is_expandable(HippoCanvasBoxChild *child, AdjustInfo *adjust)
{
	return child->visible && child->expand &&
		(!child->if_fits || (adjust && !(adjust->does_not_fit)));	
}

static int
count_expandable_children(GSList     *children,
                          AdjustInfo *adjusts)
{
    int count;
    int i;
    GSList *link;
    count = 0;
    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        /* We assume here that we've prevented via g_warning
         * any floats/fixed from having expand=TRUE
         */
        if (child_is_expandable(child, adjusts ? &(adjusts[i]) : NULL))
            ++count;
        
        ++i;
    }
    return count;
}

void
hippo_canvas_box_child_get_width_request(HippoCanvasBoxChild *child,
                                         int                 *min_width_p,
                                         int                 *natural_width_p)
{
    BoxChildPrivate *private = (BoxChildPrivate *)child;

    if (child->item == NULL) {
        if (min_width_p)
            *min_width_p = 0;
        if (natural_width_p)
            *natural_width_p = 0;

        return;
    }
    
    if (private->min_width < 0) {
        if (private->requesting)
            g_warning("Somehow recursively requesting child %p", child->item);
        
        private->requesting = TRUE;
        
        hippo_canvas_item_get_width_request(child->item,
                                            &private->min_width,
                                            &private->natural_width);
        
        if (private->min_width < 0 || private->natural_width < 0)
            g_warning("child %p %s returned width request of %d and %d, at least one <0",
                      child->item,
                      g_type_name_from_instance((GTypeInstance*) child->item),
                      private->min_width, private->natural_width);
        
        if (private->natural_width < private->min_width)
            g_warning("some child says its natural width is below its min width");
        
        private->requesting = FALSE;
    }

    if (min_width_p)
        *min_width_p = private->min_width;
    if (natural_width_p)
        *natural_width_p = private->natural_width;
}

static void
hippo_canvas_box_get_content_width_request(HippoCanvasBox *box,
                                           int            *min_width_p,
                                           int            *natural_width_p)
{
    int total_min;
    int total_natural;
    int n_children_in_min;
    int n_children_in_natural;
    GSList *link;

    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        
        /* Note that we still request and allocate !visible children,
         * but we allocate them 0x0.
         */
        if (!child->in_layout)
            hippo_canvas_box_child_get_width_request(child, NULL, NULL);
    }
            
        
    if (box->layout) {
        hippo_canvas_layout_get_width_request(box->layout, min_width_p, natural_width_p);
        return;
    }

    n_children_in_min = 0;
    n_children_in_natural = 0;
    total_min = 0;
    total_natural = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        int min_width;
        int natural_width;

        if (!child->in_layout)
            continue;
        
        /* PACK_IF_FITS children are do not contribute to the min size
         * of the whole box, but do contribute to natural size, and
         * will be hidden entirely if their width request does not
         * fit.
         */
        hippo_canvas_box_child_get_width_request(child, &min_width, &natural_width);
 
        n_children_in_natural += 1;

        /* children with if fits flag won't appear at our min width if
         * we are horizontal. If we're vertical, always request enough
         * width for all if_fits children. Children with 0 min size won't
         * themselves appear but they will get spacing around them, so
         * they count in n_children_in_min.
         */
        
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
            total_min = MAX(total_min, min_width);
            n_children_in_min += 1;

            total_natural = MAX(total_natural, natural_width);
        } else {
            if (!child->if_fits) {
                total_min += min_width;
                n_children_in_min += 1;
            }
            
            total_natural += natural_width;
        }
    }

    if (box->orientation == HIPPO_ORIENTATION_HORIZONTAL && n_children_in_min > 1)
        total_min += box->spacing * (n_children_in_min - 1);

    if (box->orientation == HIPPO_ORIENTATION_HORIZONTAL && n_children_in_natural > 1)
        total_natural += box->spacing * (n_children_in_natural - 1);

    if (min_width_p)
        *min_width_p = total_min;
    if (natural_width_p)
        *natural_width_p = total_natural;
}

static int
get_adjusted_size(AdjustInfo          *adjust)
{
    return adjust->minimum + adjust->adjustment;
}

/*
 If we have an allocation larger than our request (min width), we 
 distribute the space among children as follows:
 1) for each child below natural width, bring it up to its natural width
    a) count children with a request still below their natural width
    b) find the child with the smallest needed expansion to reach natural width
       and record this needed expansion
    c) distribute among below-natural-width children the minimum of
       (all space remaining to distribute) and
       (smallest needed expansion times number of children to expand)
    d) goto a) if children below natural width remain
 2) if extra space still remains, divide it equally among each child with expand=true
 In other words, children will always grow to their natural width whether they are expand=true
 or not. Below-natural-size children always grow before expand=true children.

 Various optimizations are obviously possible here (keep track of flags for whether
 we have any expandable / any natural!=minimum, for example).

 The PACK_IF_FITS children are done in a second pass after other children,
 the if_fits flag indicates which pass this is. If if_fits=TRUE we need
 to skip if_fits children that did not fit.
 
*/
/* return TRUE if it needs to run again */
static gboolean
adjust_up_to_natural_size(GSList          *children,
                          int             *remaining_extra_space_p,
                          AdjustInfo      *adjusts,
                          gboolean         if_fits)
{
    int i;
    GSList *link;
    int smallest_increase;
    int n_needing_increase;
    int space_to_distribute;

    g_assert(*remaining_extra_space_p >= 0);

    if (*remaining_extra_space_p == 0)
        return FALSE;

    smallest_increase = G_MAXINT;
    n_needing_increase = 0;
    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (!child->fixed && child->visible &&
            ((!child->if_fits && !if_fits) ||
             (child->if_fits && if_fits && !adjusts[i].does_not_fit))) {
            int needed_increase;

            g_assert(adjusts[i].adjustment >= 0);

            /* guaranteed to be >= 0 */
            needed_increase = adjusts[i].natural - adjusts[i].minimum;
            g_assert(needed_increase >= 0);
            
            needed_increase -= adjusts[i].adjustment; /* see how much we've already increased */

            if (needed_increase > 0) {
                n_needing_increase += 1;
                smallest_increase = MIN(smallest_increase, needed_increase);
            }
        }

        ++i;
    }

    if (n_needing_increase == 0)
        return FALSE;

    g_assert(smallest_increase < G_MAXINT);

    space_to_distribute = MIN(*remaining_extra_space_p, smallest_increase * n_needing_increase);

    g_assert(space_to_distribute >= 0);
    g_assert(space_to_distribute <= *remaining_extra_space_p);
    
    *remaining_extra_space_p -= space_to_distribute;

    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (!child->fixed && child->visible &&
            ((!child->if_fits && !if_fits) ||
             (child->if_fits && if_fits && !adjusts[i].does_not_fit))) {
            int needed_increase;

            g_assert(adjusts[i].adjustment >= 0);

            /* guaranteed to be >= 0 */
            needed_increase = adjusts[i].natural - adjusts[i].minimum;
            g_assert(needed_increase >= 0);

            needed_increase -= adjusts[i].adjustment; /* see how much we've already increased */
            
            if (needed_increase > 0) {
                int extra;
                extra = (space_to_distribute / n_needing_increase);
                n_needing_increase -= 1;
                space_to_distribute -= extra;
                adjusts[i].adjustment += extra;
            }
        }

        ++i;
    }

    g_assert(n_needing_increase == 0);
    g_assert(space_to_distribute == 0);

    return TRUE;
}

static void
adjust_for_expandable(GSList        *children,
                      int           *remaining_extra_space_p,
                      AdjustInfo    *adjusts)
{
    int i;
    GSList *link;
    int expand_space;
    int expand_count;

    if (*remaining_extra_space_p == 0)
        return;
    
    expand_space = *remaining_extra_space_p;
    expand_count = count_expandable_children(children, adjusts);

    if (expand_count == 0)
        return;

    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (child_is_expandable(child, &(adjusts[i])) && !adjusts[i].does_not_fit) {
            int extra;
        	extra = (expand_space / expand_count);
        	expand_count -= 1;
        	expand_space -= extra;
        	adjusts[i].adjustment += extra;
        }
        ++i;
    }

    /* if we had anything to expand, then we will have used up all space */
    g_assert(expand_space == 0);
    g_assert(expand_count == 0);

    *remaining_extra_space_p = 0;
}

static void
adjust_if_fits_as_not_fitting(GSList          *children,
                              AdjustInfo      *adjusts)
{
    int i;
    GSList *link;

    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (child->if_fits) {
            adjusts[i].adjustment -= adjusts[i].minimum;
            adjusts[i].does_not_fit = TRUE;
        }
        ++i;
    }
}

static gboolean
adjust_one_if_fits(GSList          *children,
                   int              spacing,
                   int             *remaining_extra_space_p,
                   AdjustInfo      *adjusts)
{
    int i;
    GSList *link;
    int spacing_delta;
    gboolean visible_children = FALSE;
    
    if (*remaining_extra_space_p == 0)
        return FALSE;

    /* if there are no currently visible children, then adding a child won't
     * add another spacing
     */
    i = 0;
    for (link = children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
                
        if (child->visible && (!child->if_fits || !adjusts[i].does_not_fit)) {
            visible_children = TRUE;
            break;
        }

        i++;
    }
    
    spacing_delta = visible_children ? spacing : 0;
    
    i = 0;
    for (link = children; link != NULL; link = link->next) {
        if (adjusts[i].does_not_fit) {
            /* This child was adjusted downward, see if we can pop it visible
             * (picking the smallest instead of first if-fits child on each pass
             * might be nice, but for now it's the first that fits)
             */

            if ((adjusts[i].minimum + spacing_delta) <= *remaining_extra_space_p) {
                adjusts[i].adjustment += adjusts[i].minimum;
                
                g_assert(adjusts[i].adjustment >= 0);
                
                adjusts[i].does_not_fit = FALSE;
                *remaining_extra_space_p -= (adjusts[i].minimum + spacing_delta);

                g_assert(*remaining_extra_space_p >= 0);
                
                return TRUE;
            }
        }
        ++i;
    }

    return FALSE;
}

/* this doesn't take a CanvasBox arg because it can be run in multiple contexts
 * and it's important not to store the compute-adjusts state globally in the box,
 * so we don't pass in the box, which avoids cheating.
 */
static void
compute_adjusts(GSList          *children,
                AdjustInfo      *adjusts,
                int              spacing,
                int              alloc_request_delta)
{
    int remaining_extra_space;

    if (children == NULL)
        return;

    /* Go ahead and cram all PACK_IF_FITS children to zero width,
     * we'll expand them again if we can.
     */
    adjust_if_fits_as_not_fitting(children, adjusts);
    
    /* Make no adjustments if we got too little or just right space.
     * (FIXME handle too little space better)
     */
    if (alloc_request_delta <= 0) {
        return;
    }
    
    remaining_extra_space = alloc_request_delta;

    /* adjust non-PACK_IF_FITS up to natural size */
    while (adjust_up_to_natural_size(children,
                                     &remaining_extra_space, adjusts, FALSE))
        ;

    /* see if any PACK_IF_FITS can get their minimum size */
    while (adjust_one_if_fits(children,
                              spacing, &remaining_extra_space, adjusts))
        ;

    /* if so, then see if they can also get a natural size */
    while (adjust_up_to_natural_size(children,
                                     &remaining_extra_space, adjusts, TRUE))
        ;
    
    /* and finally we can expand to fill empty space */
    adjust_for_expandable(children, &remaining_extra_space, adjusts);

    /* remaining_extra_space need not be 0, if we had no expandable children */
}

void
hippo_canvas_box_child_get_height_request(HippoCanvasBoxChild *child,
                                          int                  for_width,
                                          int                 *min_height_p,
                                          int                 *natural_height_p)
{
    BoxChildPrivate *private = (BoxChildPrivate *)child;
    
    if (child->item == NULL) {
        if (min_height_p)
            *min_height_p = 0;
        if (natural_height_p)
            *natural_height_p = 0;

        return;
    }
    
    if (private->min_width < 0)
        g_warning("Height requesting child without width requesting first");
        
    if (private->min_height < 0 ||
        private->height_request_for_width != for_width) {
        hippo_canvas_item_get_height_request(child->item, for_width,
                                             &private->min_height, &private->natural_height);
        private->height_request_for_width = for_width;
    }

    if (min_height_p)
        *min_height_p = private->min_height;
    if (natural_height_p)
        *natural_height_p = private->natural_height;
}

static void
request_child_natural_size(HippoCanvasBoxChild *child,
                           int                 *width_p,
                           int                 *height_p)
{
    int width, height;
    
    hippo_canvas_box_child_get_width_request(child, NULL, &width);
    hippo_canvas_box_child_get_height_request(child, width, NULL, &height);

    if (width_p)
        *width_p = width;
    if (height_p)
        *height_p = height;
}

/*
 * In essence there are three separate layout managers for HippoCanvasBox:
 *
 *  - Horizontal
 *  - Vertical
 *  - Vertical with floats
 *
 * The code below implements the third case, and is used both when computing
 * height for a width and when doing the final allocation.
 *
 * The way we handle floats is similar to the CSS box model but not absolutely
 * identical. Some differences and limitations:
 *
 * - In the CSS model, an individual child in the normal flow can wrap around
 *   a float; this obviously isn't possible for us where each child occupies
 *   a rectangular area.
 * - We never put two left floats or right floats on the same line; the left
 *   all are positioned on the extreme left of the box, the right floats all
 *   are positioned on the extreme right of the box.
 * - We assume that floats all fit horizontally and that left floats and right
 *   floats don't interact with each other; a float will never be forced
 *   downwards because it doesn't fit.
 */

/* Checks that the box packing flags are consistent and returns true if the box has any
 * floating children
 */
static gboolean
box_validate_packing(HippoCanvasBox *box)
{
    gboolean has_floats = FALSE;
    gboolean has_expand = FALSE;
    gboolean has_if_fits = FALSE;
    
    GSList *l;

    for (l = box->children; l != NULL; l = l->next) {
        HippoCanvasBoxChild *child = l->data;

        if (child->float_right || child->float_left || child->clear_left || child->clear_right)
            has_floats = TRUE;
        if (child->expand)
            has_expand = TRUE;
        if (child->if_fits)
            has_if_fits = TRUE;

        if (child->expand &&
            (child->fixed || child->float_right || child->float_left ||
             child->clear_left || child->clear_right)) {
            g_warning("Child must be in 'normal flow' not floated/fixed if HIPPO_PACK_EXPAND is set");
        }
    }

    if (has_floats && box->orientation == HIPPO_ORIENTATION_HORIZONTAL)
        g_warning("Floating children can only be used in a vertical box");
    if (has_floats && has_expand)
        g_warning("Floating children cannot be used in the same box as HIPPO_PACK_EXPAND");
    if (has_floats && has_if_fits)
        g_warning("Floating children can't be used in the same box as HIPPO_PACK_IF_FITS");

    return has_floats;
}

/* Per-floated-child information that we need during the layout process
 */
typedef struct {
    HippoCanvasBoxChild *child;
    int width;
    int height;
    int y;
} HippoBoxFloat;

/* Global information during the layout process
 */
typedef struct {
    HippoCanvasBox *box;
    int for_width;
    
    int y;             /* End y-coordinate of the last normal-flow child */
    int normal_count;  /* Number of normal-flow child we've seen */

    HippoBoxFloat *left;
    int n_left;         /* number of left-floated children */
    int next_left;      /* the index of the next left-floated child in the packing order */
    int at_y_left;      /* the index of the first left-floated child that could overlap
                         * subsequent normal-flow children */
    
    HippoBoxFloat *right;
    int n_right;        /* number of right-floated children */
    int next_right;     /* the index of the next right-floated child in the packing order */
    int at_y_right;     /* the index of the first right-floated child that could overlap
                         * subsequent normal-flow children */
    
} HippoBoxFloats;

static void
init_float(HippoBoxFloat       *float_,
           HippoCanvasBoxChild *child)
{
    float_->child = child;
    request_child_natural_size(child, &float_->width, &float_->height);
}

/* Initialize the layout process when doing layout with floats.
 */
static void 
floats_start_packing(HippoBoxFloats  *floats,
                     HippoCanvasBox  *box,
                     int              for_width)
{
    GSList *l;
    int n_left = 0;
    int n_right = 0;
    int i_left, i_right;

    floats->box = box;
    floats->for_width = for_width;

    /* Count the number of floated children and allocate space for
     * per-child information
     */
    for (l = box->children; l != NULL; l = l->next) {
        HippoCanvasBoxChild *child = l->data;

        if (!child->in_layout)
            continue;

        if (child->float_left)
            n_left++;
        else if (child->float_right)
            n_right++;
    }

    floats->n_left = n_left;
    floats->left = g_new(HippoBoxFloat, n_left);
    floats->n_right = n_right;
    floats->right = g_new(HippoBoxFloat, n_right);

    /* Compute initial sizes and (vertical) positions for the floated children based on
     * the requested width and height of each child; left and right floats are positioned
     * in a solid column down each side. The only adjustment we make later is to move floats
     * down so that a float doesn't appear above a normally-flowed child that precedes it
     * in the packing order.
     */
    i_left = 0;
    i_right = 0;
    for (l = box->children; l != NULL; l = l->next) {
        HippoCanvasBoxChild *child = l->data;

        if (!child->in_layout)
            continue;

        if (child->float_left) {
            init_float(&floats->left[i_left], child);
            
            if (i_left == 0)
                floats->left[i_left].y = 0;
            else
                floats->left[i_left].y = floats->left[i_left - 1].y + floats->left[i_left - 1].height + box->spacing;
                
            i_left++;
        } else if (child->float_right) {
            init_float(&floats->right[i_right], child);
            
            if (i_right == 0)
                floats->right[i_right].y = 0;
            else
                floats->right[i_right].y = floats->right[i_right - 1].y + floats->right[i_right - 1].height + box->spacing;
                
            i_right++;
        }
    }
    
    floats->y = 0;
    floats->normal_count = 0 ;

    floats->next_left = 0;
    floats->at_y_left = 0;
    
    floats->next_right = 0;
    floats->at_y_right = 0;
}

/* Return the bottom y of the last left float processed
 */
static int
floats_get_left_end_y(HippoBoxFloats *floats)
{
    if (floats->next_left > 0)
        return floats->left[floats->next_left - 1].y + floats->left[floats->next_left - 1].height;
    else
        return 0;
}

/* Return the bottom y of the last right float processed
 */
static int
floats_get_right_end_y(HippoBoxFloats *floats)
{
    if (floats->next_right > 0)
        return floats->right[floats->next_right - 1].y + floats->right[floats->next_right - 1].height;
    else
        return 0;
}

/*
 * Do layout for a single child; if do_request is FALSE we assume that we've
 * previously done layout at the same width (from get_width_request), so we skip
 * doing size negotation with the child, and use the cached value. The content-area
 * relative allocation of the child is stored in child_allocation, if non-NULL.
 */
static void
floats_add_child(HippoBoxFloats       *floats,
                 HippoCanvasBoxChild  *child,
                 gboolean              do_request,
                 HippoRectangle       *child_allocation)
{
    HippoBoxFloat *left = floats->left;
    HippoBoxFloat *right = floats->right;
    int i;
    
    g_assert(child->in_layout);

    if (child->float_left) {
        /* If the float doesn't appear below normal normal children that precede
         * it in the packing order, then we need to move it (and all following
         * left floats) down
         */
        HippoBoxFloat *left_float = &left[floats->next_left];

        int next_normal_y = floats->y;
        if (floats->normal_count > 0)
            next_normal_y += floats->box->spacing;

        if (left_float->y < next_normal_y) {
            int move_down = next_normal_y - left_float->y;
            for (i = floats->next_left; i < floats->n_left; i++)
                left[i].y += move_down;
        }

        if (child_allocation) {
            child_allocation->x = 0;
            child_allocation->y = left_float->y;
            child_allocation->width = left_float->width;
            child_allocation->height = left_float->height;
        }
        
        floats->next_left++;
        
    } else if (child->float_right) {
        /* If the float doesn't appear below normal normal children that precede
         * it in the packing order, then we need to move it (and all following
         * left floats) down
         */
        HippoBoxFloat *right_float = &right[floats->next_right];

        int next_normal_y = floats->y;
        if (floats->normal_count > 0)
            next_normal_y += floats->box->spacing;

        if (right_float->y < next_normal_y) {
            int move_down = next_normal_y - right_float->y;
            for (i = floats->next_right; i < floats->n_right; i++)
                right[i].y += move_down;
        }
        
        if (child_allocation) {
            child_allocation->x = floats->for_width - right_float->width;
            child_allocation->y = right_float->y;
            child_allocation->width = right_float->width;
            child_allocation->height = right_float->height;
        }
        
        floats->next_right++;
        
    } else {
        int i_left = floats->at_y_left;
        int i_right = floats->at_y_right;
        int left_float_width = 0;
        int right_float_width = 0;
        gboolean one_more_pass = TRUE;

        /* Accessing natural_height here breaks the abstraction barrier between the
         * "layout manager" parts of the code and the "container" parts of the code.
         * If we were splitting out the floats layout manager into a truly separate
         * piece of code, the per-child cached negotatiated height could be stored
         * in layout-manager specific data using hippo_canvas_box_set_extra().
         */
        int tentative_height = do_request ? 1 : ((BoxChildPrivate *)child)->natural_height;

        /* Handle clear_left / clear_right. Ensure that normal-flow children appear below any
         * floats that they are specified to clear.
         */
        if (child->clear_left) {
            int float_end_y = floats_get_left_end_y(floats);
            if (float_end_y > floats->y)
                floats->y = float_end_y;
        }

        if (child->clear_right) {
            int float_end_y = floats_get_right_end_y(floats);
            if (float_end_y > floats->y)
                floats->y = float_end_y;
        }
        
        if (floats->normal_count != 0)
            floats->y += floats->box->spacing;

        /* Skip over any floats that are completely above this child; they don't affect the
         * width; we only look up to floats->next_left/right since floats after this
         * normal-flow child will appear below it.
         */
        while (i_left < floats->next_left &&
               left[i_left].y + left[i_left].height <= floats->y)
            i_left++;

        while (i_right < floats->next_right &&
               right[i_right].y + right[i_right].height <= floats->y)
            i_right++;
        
        /* We need to iterate to determine the set of floats that actually do affect
         * the width; we start off subtracting the width of only floats that overlap
         * the top line of the child (tentative_height == 1), and find the height
         * of the child at that width. If that height causes us to overlap any more
         * floats and narrow the available width, then we need to repeat, and so
         * forth.
         *
         * If we see a float that overlaps this child, then we know that floats 
         * before that float can't overlap normal children after this child,
         * so we can advance floats->at_y_left/at_y_right.
         */
        while (TRUE) {
            while (i_left < floats->next_left &&
                   left[i_left].y < floats->y + tentative_height) {
                if (left[i_left].width > left_float_width) {
                    left_float_width = left[i_left].width;
                    one_more_pass = TRUE;
                }
                floats->at_y_left = i_left;
                i_left++;
            }

            while (i_right < floats->next_right &&
                   right[i_right].y < floats->y + tentative_height) {
                if (right[i_right].width > right_float_width) {
                    right_float_width = right[i_right].width;
                    one_more_pass = TRUE;
                }
                floats->at_y_right = i_right;
                i_right++;
            }

            if (!one_more_pass)
                break;

            if (do_request) {
                int natural_height;
                
                hippo_canvas_box_child_get_height_request(child, floats->for_width - left_float_width - right_float_width, NULL, &natural_height);
                tentative_height = natural_height;
            }

            one_more_pass = FALSE;
        }

        if (child_allocation) {
            child_allocation->x = left_float_width;
            child_allocation->y = floats->y;
            child_allocation->width = floats->for_width - left_float_width - right_float_width;
            child_allocation->height = tentative_height;
        }
                                            
        floats->y += tentative_height;
        floats->normal_count++;
    }
}

/* Finish the floating layout process. Returns the minimal total height for the box
 */
static int
floats_end_packing(HippoBoxFloats *floats)
{
    int height = floats->y;
    int left_end_y = floats_get_left_end_y(floats);
    int right_end_y = floats_get_right_end_y(floats);

    if (left_end_y > height)
        height = left_end_y;
    if (right_end_y > height)
        height = right_end_y;

    g_free(floats->left);
    g_free(floats->right);

    return height;
}

static void
get_floats_height_request(HippoCanvasBox *box,
                          int             for_width,
                          int            *min_height_p,
                          int            *natural_height_p)
{
    int total_min;
    int total_natural;
    GSList *link;
    HippoBoxFloats floats;
    
    total_min = 0;
    total_natural = 0;

    floats_start_packing(&floats, box, for_width);

    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        
        if (!child->in_layout)
            continue;
        
        floats_add_child(&floats, child, TRUE, NULL);
    }
    
    total_min = total_natural = floats_end_packing(&floats);
    
    if (min_height_p)
        *min_height_p = total_min;
    if (natural_height_p)
        *natural_height_p = total_natural;
}

/* this function should look a lot like get_content_width_request() */
static void
get_vbox_height_request(HippoCanvasBox *box,
                        int             for_width,
                        int            *min_height_p,
                        int            *natural_height_p)
{
    int total_min;
    int total_natural;
    int n_children_in_min;
    int n_children_in_natural;
    GSList *link;

    total_min = 0;
    total_natural = 0;
    n_children_in_min = 0;
    n_children_in_natural = 0;

    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        int min_height;
        int natural_height;

        /* Note that we still request and allocate !visible children */
        hippo_canvas_box_child_get_height_request(child, for_width, &min_height, &natural_height);
            
        if (!child->in_layout)
            continue;
            
        n_children_in_natural += 1;
        total_natural += natural_height;
            
        if (!child->if_fits) {
            n_children_in_min += 1;
            total_min += min_height;
        }
    }

    if (n_children_in_min > 1)
        total_min += box->spacing * (n_children_in_min - 1);
    if (n_children_in_natural > 1)
        total_natural += box->spacing * (n_children_in_natural - 1);
    
    if (min_height_p)
        *min_height_p = total_min;
    if (natural_height_p)
        *natural_height_p = total_natural;
}

static AdjustInfo*
adjust_infos_new(HippoCanvasBox *box,
                 int             for_content_width)
{
    /* Rely on the zero-initialization */
    AdjustInfo *adjusts = g_new0(AdjustInfo, g_slist_length(box->children));
    GSList *link;

    int i = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (!child->in_layout) {
            adjusts[i].minimum = adjusts[i].natural = 0;
        } else if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
            hippo_canvas_box_child_get_height_request(child, for_content_width, &adjusts[i].minimum, &adjusts[i].natural);
        } else {
            hippo_canvas_box_child_get_width_request(child,  &adjusts[i].minimum, &adjusts[i].natural);
        }

        i++;
    }
    
    return adjusts;
}

static void
get_content_width_request(HippoCanvasBox *box,
                          int            *min_width_p,
                          int            *natural_width_p)
{
    if (box->content_min_width < 0)
    {
        HippoCanvasBoxClass *klass = HIPPO_CANVAS_BOX_GET_CLASS(box);
    
        (* klass->get_content_width_request)(box,
                                             &box->content_min_width, &box->content_natural_width);
    }

    if (min_width_p)
        *min_width_p = box->content_min_width;
    if (natural_width_p)
        *natural_width_p = box->content_natural_width;
}

static void
get_content_height_request(HippoCanvasBox *box,
                           int             for_width,
                           int            *min_height_p,
                           int            *natural_height_p)
{
    if (box->content_min_height < 0 ||
        box->content_height_request_for_width != for_width)
    {
        HippoCanvasBoxClass *klass = HIPPO_CANVAS_BOX_GET_CLASS(box);
    
        (* klass->get_content_height_request)(box, for_width,
                                              &box->content_min_height, &box->content_natural_height);
        
        box->content_height_request_for_width = for_width;
    }

    if (min_height_p)
        *min_height_p = box->content_min_height;
    if (natural_height_p)
        *natural_height_p = box->content_natural_height;
}

/* This function's algorithm must be kept in sync with the one in allocate() */
static void
get_hbox_height_request(HippoCanvasBox *box,
                        int             for_width,
                        int            *min_height_p,
                        int            *natural_height_p)
{
    int total_min;
    int total_natural;
    GSList *link;
    int requested_content_width;
    int natural_content_width;
    int allocated_content_width;
    AdjustInfo *width_adjusts;
    int i;
    
    total_min = 0;
    total_natural = 0;

    get_content_width_request(box, &requested_content_width, &natural_content_width);

    get_content_area_horizontal(box, requested_content_width, natural_content_width,
                                for_width, NULL, &allocated_content_width);

    width_adjusts = adjust_infos_new(box, for_width);
    compute_adjusts(box->children,
                    width_adjusts,
                    box->spacing,
                    allocated_content_width - requested_content_width);

    i = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        int min_height, natural_height;
        int req;
            
        if (!child->in_layout) {
            ++i;
            continue;
        }

        req = get_adjusted_size(&width_adjusts[i]);

        hippo_canvas_box_child_get_height_request(child, req, &min_height, &natural_height);
            
        total_min = MAX(total_min, min_height);
        total_natural = MAX(total_natural, natural_height);

        ++i;
    }

    g_free(width_adjusts);
    
    if (min_height_p)
        *min_height_p = total_min;
    if (natural_height_p)
        *natural_height_p = total_natural;
}


static void
hippo_canvas_box_get_content_height_request(HippoCanvasBox *box,
                                            int             for_width,
                                            int            *min_height_p,
                                            int            *natural_height_p)
{
    GSList *link;
    gboolean has_floats;

    /* Do fixed children, just to have their request recorded;
     * the box for_width is ignored here, fixed children just
     * always get their width request.
     *
     * For !visible children we want to request them but will
     * always allocate 0x0
     */
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;

        if (!child->in_layout)
            request_child_natural_size(child, NULL, NULL);
    }

    if (box->layout) {
        hippo_canvas_layout_get_height_request(box->layout, for_width, min_height_p, natural_height_p);
        return;
    }
 
    /* Now do the box-layout children */

    has_floats = box_validate_packing(box);
    
    if (box->orientation == HIPPO_ORIENTATION_VERTICAL && has_floats) {
        get_floats_height_request(box, for_width, min_height_p, natural_height_p);
    } else if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        get_vbox_height_request(box, for_width, min_height_p, natural_height_p);
    } else {
        get_hbox_height_request(box, for_width, min_height_p, natural_height_p);
    }
}

static void
hippo_canvas_box_get_width_request(HippoCanvasItem *item,
                                   int             *min_width_p,
                                   int             *natural_width_p)
{
    int content_min_width, content_natural_width;
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX(item);

    box->needs_width_request = FALSE;

    /* We need to call this even if just returning the box-width prop,
     * so that children can rely on getting the full request, allocate
     * cycle in order every time, and so we compute the cached requests.
     */
    get_content_width_request(box, &content_min_width, &content_natural_width);

    if (box->box_width >= 0) {
        /* FIXME it's probably a lot more useful if we had separate properties
         * to set these two.
         */
        if (min_width_p)
            *min_width_p = box->box_width;
        if (natural_width_p)
            *natural_width_p = box->box_width;
    } else {
        int outside;

        outside = box->padding_left + box->padding_right
            + box->border_left + box->border_right;

        if (min_width_p)
            *min_width_p = content_min_width + outside;
        if (natural_width_p)
            *natural_width_p = content_natural_width + outside;
    }

    if (box->debug_name != NULL && min_width_p != NULL) {
        g_debug("box %s Computed minimum width as %d", box->debug_name, *min_width_p);
    }

    if (box->debug_name != NULL && natural_width_p != NULL) {
        g_debug("box %s Computed natural width as %d", box->debug_name, *natural_width_p);
    }
}

static void
hippo_canvas_box_get_height_request(HippoCanvasItem *item,
                                    int              for_width,
                                    int             *min_height_p,
                                    int             *natural_height_p)
{
    int content_min_height, content_natural_height;
    int content_for_width;
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX(item);

    box->needs_height_request = FALSE;

    content_for_width = for_width
        - box->padding_left - box->padding_right
        - box->border_left - box->border_right;

    /* We need to call this even if just returning the box-height prop,
     * so that children can rely on getting the full request, allocate
     * cycle in order every time, and so we compute the cached requests.
     */
    get_content_height_request(box, content_for_width,
                               &content_min_height, &content_natural_height);
    
    if (box->box_height >= 0) {
        /* FIXME the property should probably be separate for these two */
        if (min_height_p)
            *min_height_p = box->box_height;
        if (natural_height_p)
            *natural_height_p = box->box_height;
    } else {
        int outside;
        outside = box->padding_top + box->padding_bottom
            + box->border_top + box->border_bottom;

        if (min_height_p)
            *min_height_p = content_min_height + outside;
        if (natural_height_p)
            *natural_height_p = content_natural_height + outside;
    }
    
    if (box->debug_name != NULL && min_height_p != NULL) {
        g_debug("box %s Computed minimum height for width=%d as %d", box->debug_name, for_width, *min_height_p);
    }

    if (box->debug_name != NULL && natural_height_p != NULL) {
        g_debug("box %s Computed natural height for width=%d as %d", box->debug_name, for_width, *natural_height_p);
    }
}

/**
 * hippo_canvas_box_align:
 * @content_width: width of the content of your item (text, image, children, whatever)
 * @content_height: height of the content
 * @x_p: output the X origin where you should put your content
 * @y_p: output the Y origin where you should put your content
 * @width_p: output the width of your content (may exceed the
 * passed-in width/height if alignment mode is #HIPPO_ALIGNMENT_FILL
 * for example)
 * @height_p: output the height of your content
 * 
 * This is a "protected" method intended for use by #HippoCanvasBox
 * subclasses (basically all items are box subclasses right now).
 * Pass in a size request, and have it converted to the right place
 * to actually draw, with padding/border removed and alignment performed.
 * In other words this sorts out the padding, border, and xalign/yalign
 * properties for you.
 */
void
hippo_canvas_box_align(HippoCanvasBox *box,
                       int             content_width,
                       int             content_height,
                       int            *x_p,
                       int            *y_p,
                       int            *width_p,
                       int            *height_p)
{
    /* What the user passes in here is the content area they are actually
     * using. For this, the distinction between the requested and natural
     * size that we need when doing layout doesn't matter, so we pass the
     * same value for both.
     */
    get_content_area(box,
                     content_width, content_height,
                     content_width, content_height,
                     box->allocated_width, box->allocated_height,
                     x_p, y_p, width_p, height_p);
}

void
hippo_canvas_box_child_allocate(HippoCanvasBoxChild *child,
                                int                  x,
                                int                  y,
                                int                  width,
                                int                  height,
                                gboolean             origin_changed)
{
    BoxChildPrivate *private = (BoxChildPrivate *)child;
    gboolean child_moved;

    if (child->item == NULL)
        return;

    child_moved = x != private->x || y != private->y;

    private->x = x;
    private->y = y;

    hippo_canvas_item_allocate(child->item,
                               width, height,
                               origin_changed || child_moved);
}

static void
layout_floats(HippoCanvasBox  *box,
              int              content_x,
              int              content_y,
              int              allocated_content_width,
              int              allocated_content_height,
              int              requested_content_width,
              int              requested_content_height,
              gboolean         origin_changed)
{
    HippoBoxFloats floats;
    GSList *link;
    
    floats_start_packing(&floats, box, allocated_content_width);
    
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        HippoRectangle child_allocation;
        
        if (!child->in_layout)
            continue;
        
        floats_add_child(&floats, child, FALSE, &child_allocation);
        
        hippo_canvas_box_child_allocate(child,
                                        content_x + child_allocation.x,
                                        content_y + child_allocation.y,
                                        child_allocation.width, child_allocation.height,
                                        origin_changed);
    }
    
    floats_end_packing(&floats);
}

static void
layout_box(HippoCanvasBox  *box,
           int              content_x,
           int              content_y,
           int              allocated_content_width,
           int              allocated_content_height,
           int              requested_content_width,
           int              requested_content_height,
           gboolean         origin_changed)
{
    int start;
    int end;
    AdjustInfo *adjusts;
    int i;
    int allocated_size, requested_size;
    GSList *link;

    if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
        allocated_size = allocated_content_height;
        requested_size = requested_content_height;            
        start = content_y;
    } else {
        allocated_size = allocated_content_width;
        requested_size = requested_content_width;
        start = content_x;
    }
    end = start + allocated_size;

    adjusts = adjust_infos_new(box, allocated_content_width);
    compute_adjusts(box->children,
                    adjusts,
                    box->spacing,
                    allocated_size - requested_size);

    i = 0;
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        int req;
        
        if (!child->in_layout) {
            ++i;
            continue;
        }

        if (box->orientation == HIPPO_ORIENTATION_VERTICAL) {
            req = get_adjusted_size(&adjusts[i]);
            hippo_canvas_box_child_allocate(child,
                                            content_x, child->end ? (end - req) : start,
                                            allocated_content_width, req,
                                            origin_changed);
                
        } else {
            req = get_adjusted_size(&adjusts[i]);
            hippo_canvas_box_child_allocate(child,
                                            child->end ? (end - req) : start, content_y,
                                            req, allocated_content_height,
                                            origin_changed);
        }

        if (req <= 0) {
            /* Child was adjusted out of existence, act like it's
             * !visible
             */
            hippo_canvas_box_child_allocate(child, 0, 0, 0, 0, origin_changed);
        }

        /* Children with req == 0 still get spacing unless they are IF_FITS.
         * The handling of spacing could use improvement (spaces should probably
         * act like items with min width 0 and natural width of spacing) but
         * it's pretty hard to get right without rearranging the code a lot.
         */
        if (!adjusts[i].does_not_fit) {
            if (child->end)
                end -= (req + box->spacing);
            else
                start += (req + box->spacing);
        }
            
        ++i;
    }
    
    g_free(adjusts);
}

static void
hippo_canvas_box_allocate(HippoCanvasItem *item,
                          int              allocated_box_width,
                          int              allocated_box_height,
                          gboolean         origin_changed)
{
    HippoCanvasBox *box;
    int requested_content_width;
    int requested_content_height;
    int natural_content_width;
    int natural_content_height;
    int allocated_content_width;
    int allocated_content_height;
    GSList *link;
    int content_x, content_y;
    gboolean has_floats;

    box = HIPPO_CANVAS_BOX(item);

    /* If we haven't emitted request-changed then we are allowed to short-circuit 
     * an unchanged allocation
     */
    if (!origin_changed && !box->needs_allocate &&
        (box->allocated_width == allocated_box_width && 
         box->allocated_height == allocated_box_height))
        return;

    box->allocated_width = allocated_box_width;
    box->allocated_height = allocated_box_height;
    box->needs_allocate = FALSE;

    get_content_width_request(box, &requested_content_width, &natural_content_width);  

    get_content_area_horizontal(box, requested_content_width, natural_content_width,
                                allocated_box_width,
                                &content_x, &allocated_content_width);
    
    get_content_height_request(box,
                               allocated_content_width,
                               &requested_content_height, &natural_content_height);

    get_content_area_vertical(box, requested_content_height, natural_content_height,
                              allocated_box_height,
                              &content_y, &allocated_content_height);
    

    if (box->debug_name != NULL) {
        g_debug("box %s allocated %dx%d  requested %dx%d lay out into %d,%d %dx%d",
                box->debug_name, box->allocated_width, box->allocated_height,
                requested_content_width, requested_content_height,
                content_x, content_y, allocated_content_width, allocated_content_height);
    }

    if (allocated_content_width == 0 || allocated_content_height == 0) {

        /* If we are !visible (0 allocation) then allocate all our
         * children to 0.  This should happen in the layout algorithms
         * below, but they are all lame about dealing with too-small
         * allocations, so they don't "just work" for this.
         */
        
        for (link = box->children; link != NULL; link = link->next) {
            HippoCanvasBoxChild *child = link->data;
            hippo_canvas_item_allocate(child->item, 0, 0, FALSE);
        }
    } else {
        /* Allocate fixed children their natural size and invisible
         * children 0x0
         */
        for (link = box->children; link != NULL; link = link->next) {
            HippoCanvasBoxChild *child = link->data;
            if (!child->visible) {
                hippo_canvas_item_allocate(child->item, 0, 0, FALSE);
            } else if (child->fixed) {
                int width, height;
                request_child_natural_size(child, &width, &height);
                hippo_canvas_item_allocate(child->item, width, height, origin_changed);
            } else {
                continue;
            }
        }

        /* Now layout the box */
        
        if (box->layout) {
            hippo_canvas_layout_allocate(box->layout,
                                         content_x, content_y,
                                         allocated_content_width, allocated_content_height,
                                         requested_content_width, requested_content_height,
                                         origin_changed);
            return;
        }

        has_floats = box_validate_packing(box);
    
        if (box->orientation == HIPPO_ORIENTATION_VERTICAL && has_floats) {
            layout_floats(box, content_x, content_y,
                          allocated_content_width, allocated_content_height,
                          requested_content_width, requested_content_height,
                          origin_changed);
        } else {
            layout_box(box, content_x, content_y,
                       allocated_content_width, allocated_content_height,
                       requested_content_width, requested_content_height,
                       origin_changed);
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

static BoxChildPrivate*
find_child_at_point(HippoCanvasBox *box,
                    int             x,
                    int             y)
{
    GSList *link;
    BoxChildPrivate *topmost;

    /* Box-layout children don't overlap each other, so we could just
     * return the first match, but for fixed children we have to
     * return the last match since the items are bottom-to-top in the list
     */
    
    topmost = NULL;
    for (link = box->children; link != NULL; link = link->next) {
        BoxChildPrivate *child = link->data;
        int width, height;

        if (!child->public.visible)
            continue;
        
        hippo_canvas_item_get_allocation(child->public.item, &width, &height);

        if (x >= child->x && y >= child->y &&
            x < (child->x + width) &&
            y < (child->y + height)) {

            topmost = child;
        }
    }

    return topmost;
}

static BoxChildPrivate*
find_hovering_child(HippoCanvasBox *box)
{
    GSList *link;
    
    for (link = box->children; link != NULL; link = link->next) {
        BoxChildPrivate *child = link->data;
        if (child->hovering)
            return child;
    }

    return NULL;
}

/* the odd thing here is that sometimes we have an enter/leave from the parent,
 * and sometimes we have just a motion from the parent, and in both cases
 * we may need to send an enter/leave to one of our children.
 */
/* FIXME we need to update the "hovering child" whenever we get a new allocation
 * (which would happen e.g. when a child becomes visible/invisible also).
 */
static gboolean
forward_motion_event(HippoCanvasBox *box,
                     HippoEvent     *event)
{
    BoxChildPrivate *child;
    BoxChildPrivate *was_hovering;
    gboolean result;

    result = FALSE; /* we only overwrite this when forwarding the current event,
                     * not with synthesized enter/leaves
                     */
    
    if (event->u.motion.detail == HIPPO_MOTION_DETAIL_ENTER ||
        event->u.motion.detail == HIPPO_MOTION_DETAIL_WITHIN)
        child = find_child_at_point(box, event->x, event->y);
    else
        child = NULL; /* leave events never have a new hover target */
    
    was_hovering = find_hovering_child(box);

    /* Do the leave event first to avoid having two current hovering children */
    
    if (was_hovering && child != was_hovering) {
        was_hovering->hovering = FALSE;
        if (event->u.motion.detail != HIPPO_MOTION_DETAIL_LEAVE)
            hippo_canvas_item_emit_motion_notify_event(was_hovering->public.item,
                                                       event->x - was_hovering->x,
                                                       event->y - was_hovering->y,
                                                       HIPPO_MOTION_DETAIL_LEAVE);
        else
            result = hippo_canvas_item_process_event(was_hovering->public.item,
                                                     event, was_hovering->x, was_hovering->y);
    }

    /* Now mark the current hovering child */
    
    if (child) {
        g_assert(event->u.motion.detail != HIPPO_MOTION_DETAIL_LEAVE);
        
        if (child != was_hovering) {
            g_assert(box->hovering);
            child->hovering = TRUE;

            if (event->u.motion.detail != HIPPO_MOTION_DETAIL_ENTER) {
                hippo_canvas_item_emit_motion_notify_event(child->public.item,
                                                           event->x - child->x,
                                                           event->y - child->y,
                                                           HIPPO_MOTION_DETAIL_ENTER);
            }
        }
        
        /* forward an enter or motion within event */
        result = hippo_canvas_item_process_event(child->public.item,
                                                 event, child->x, child->y);
        
    }    

    return result;
}

static void
set_release_pending (BoxChildPrivate *child,
                     guint          button,
                     gboolean       value)
{
    g_assert (child != NULL);

    switch (button)
      {
      case 1:
        child->left_release_pending = value;
        break;
      case 2:
        child->middle_release_pending = value;
        break;
      case 3:
        child->right_release_pending = value;
        break;
      }
}

static gboolean
is_release_pending (BoxChildPrivate *child,
                    guint          button)
{
    gboolean result = FALSE;

    g_assert (child != NULL);

    switch (button)
      {
      case 1:
        result = child->left_release_pending == TRUE;
        break;
      case 2:
        result = child->middle_release_pending == TRUE;
        break;
      case 3:
        result = child->right_release_pending == TRUE;
        break;
      }

    return result;
}

static gboolean
forward_button_release_event(HippoCanvasBox *box,
                             HippoEvent     *event)
{
    GSList *link;
    
    for (link = box->children; link != NULL; link = link->next) {
        BoxChildPrivate *child;

        child = link->data;
        if (is_release_pending (child, event->u.button.button)) {
            gboolean handled = FALSE;
            
            handled = hippo_canvas_item_process_event(child->public.item,
                                                      event, child->x, child->y);
            
            set_release_pending(child, event->u.button.button, FALSE); 
            return handled;
        }
    }

    return FALSE;
}


static gboolean
forward_event(HippoCanvasBox *box,
              HippoEvent     *event)
{
    BoxChildPrivate *child;

    if (event->type == HIPPO_EVENT_MOTION_NOTIFY) {
        /* Motion events are a bit more complicated than the others */
        return forward_motion_event(box, event);
    } else if (event->type == HIPPO_EVENT_BUTTON_RELEASE) {
        return forward_button_release_event(box, event);
    } else if (event->type == HIPPO_EVENT_BUTTON_PRESS) {
        child = find_child_at_point(box, event->x, event->y);
        
        if (child != NULL) {
            set_release_pending (child, event->u.button.button, TRUE); 
            return hippo_canvas_item_process_event(child->public.item,
                                                   event, child->x, child->y);
        } else {
            return FALSE;
        }
    } else if (event->type == HIPPO_EVENT_SCROLL) {
        child = find_child_at_point(box, event->x, event->y);
        if (child != NULL) {
            return hippo_canvas_item_process_event(child->public.item,
                                                   event, child->x, child->y);
        } else {
            return FALSE;
        }
    } else {
        return FALSE;
    } 
}

static gboolean
hippo_canvas_box_button_press_event (HippoCanvasItem *item,
                                     HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return forward_event(box, event);
}

static gboolean
hippo_canvas_box_button_release_event (HippoCanvasItem *item,
                                       HippoEvent      *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    gboolean handled;
    
    handled = forward_event (box, event);

    if (!handled && box->clickable && event->u.button.button == 1) {
        hippo_canvas_item_emit_activated(item);
        return TRUE;
    } else {
        return handled;
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
#endif
        if (box->hovering)
            g_warning("Box got enter event but was already hovering=TRUE");

        box->hovering = TRUE;
    } else if (event->u.motion.detail == HIPPO_MOTION_DETAIL_LEAVE) {
#if 0
        g_debug("motion notify LEAVE %s %p box hovering was %d",
            g_type_name_from_instance((GTypeInstance*)box), box, box->hovering);
#endif
        if (!box->hovering)
            g_warning("Box got leave event but was not hovering=TRUE");

        box->hovering = FALSE;
    } else if (event->u.motion.detail == HIPPO_MOTION_DETAIL_WITHIN) {
#if 0
        g_debug("motion notify WITHIN %s %p box hovering was %d",
            g_type_name_from_instance((GTypeInstance*)box), box, box->hovering);
#endif
        if (!box->hovering)
            g_warning("Box got motion event but never got an enter event, hovering=FALSE");
    }

    handled = forward_event(box, event);

    if (was_hovering != box->hovering) {
        g_signal_emit(G_OBJECT(box), signals[HOVERING_CHANGED], 0, box->hovering);
    }

    return handled;
}

static gboolean
hippo_canvas_box_scroll_event (HippoCanvasItem    *item,
                               HippoEvent         *event)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    gboolean handled;
    
    handled = forward_event (box, event);

    return handled;
}

static void
hippo_canvas_box_request_changed(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    box->content_min_width = -1;
    box->content_min_height = -1;
    box->needs_width_request = TRUE;
    box->needs_height_request = TRUE;
    box->needs_allocate = TRUE;
}

static gboolean
hippo_canvas_box_get_needs_request(HippoCanvasItem *item)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);

    return box->needs_width_request || box->needs_height_request;
}

static char*
hippo_canvas_box_get_tooltip(HippoCanvasItem    *item,
                             int                 x,
                             int                 y,
                             HippoRectangle     *for_area)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);    
    BoxChildPrivate *child;
    
    child = find_child_at_point(box, x, y);

    if (child != NULL) {
        char *tip;

        tip = hippo_canvas_item_get_tooltip(child->public.item,
                                            x - child->x,
                                            y - child->y,
                                            for_area);
        if (tip != NULL) {
            for_area->x += child->x;
            for_area->y += child->y;
            
            return tip;
        }
    }

    /* If no child at the point, or child did not set the tip, then
     * use our own tip if any
     */
    for_area->x = 0;
    for_area->y = 0;
    for_area->width = box->allocated_width;
    for_area->height = box->allocated_height;
    return g_strdup(box->tooltip);
}

static HippoCanvasPointer
hippo_canvas_box_get_pointer(HippoCanvasItem    *item,
                             int                 x,
                             int                 y)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);    
    BoxChildPrivate *child;
    
    child = find_child_at_point(box, x, y);

    if (child != NULL) {
        HippoCanvasPointer p;
        p = hippo_canvas_item_get_pointer(child->public.item,
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
child_destroy(HippoCanvasItem *child,
              HippoCanvasBox  *box)
{
    hippo_canvas_box_remove(box, child);
}

static void
child_request_changed(HippoCanvasItem *child,
                      HippoCanvasBox  *box)
{
    BoxChildPrivate *box_child;

    box_child = find_child(box, child);

#if 0
    g_debug("child %s %p of %s %p request changed",
            g_type_name_from_instance((GTypeInstance*) box_child->public.item),
            box_child->public.item,
            g_type_name_from_instance((GTypeInstance*) box),
            box);
#endif

    if (box_child->requesting) {
        g_warning("Child item %p of type %s changed its size request inside a size request operation",
                  box_child->public.item,
                  g_type_name_from_instance((GTypeInstance*) box_child->public.item));
    }
    
    /* invalidate cached request for this child */
    box_child->min_width = -1;    
    box_child->min_height = -1;
    box_child->height_request_for_width = -1;

    /* no-op if we already emitted since last allocate */
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
child_paint_needed(HippoCanvasItem *item,
                   const HippoRectangle *damage_box,
                   HippoCanvasBox  *box)
{
    BoxChildPrivate *child;

    /* translate to our own coordinates then emit the signal again */
    
    child = find_child(box, item);

    if (child->public.visible)
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box),
                                            damage_box->x + child->x,
                                            damage_box->y + child->y,
                                            damage_box->width, damage_box->height);
}

static void
child_tooltip_changed(HippoCanvasItem *item,
                      HippoCanvasBox  *box)
{
    hippo_canvas_item_emit_tooltip_changed(HIPPO_CANVAS_ITEM(box));
}

static void
connect_child(HippoCanvasBox  *box,
              HippoCanvasItem *child)
{
    g_signal_connect(G_OBJECT(child), "destroy",
                     G_CALLBACK(child_destroy), box);
    g_signal_connect(G_OBJECT(child), "request-changed",
                     G_CALLBACK(child_request_changed), box);
    g_signal_connect(G_OBJECT(child), "paint-needed",
                     G_CALLBACK(child_paint_needed), box);
    g_signal_connect(G_OBJECT(child), "tooltip-changed",
                     G_CALLBACK(child_tooltip_changed), box);
}

static void
disconnect_child(HippoCanvasBox  *box,
                 HippoCanvasItem *child)
{
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_destroy), box);
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_request_changed), box);
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_paint_needed), box);
    g_signal_handlers_disconnect_by_func(G_OBJECT(child),
                                         G_CALLBACK(child_tooltip_changed), box);
}

static gboolean
set_flags(HippoCanvasBoxChild *c,
          HippoPackFlags       flags)
{
    HippoPackFlags old;
    
    old = 0;
    if (c->end)
        old |= HIPPO_PACK_END;
    if (c->expand)
        old |= HIPPO_PACK_EXPAND;
    if (c->fixed)
        old |= HIPPO_PACK_FIXED;
    if (c->if_fits)
        old |= HIPPO_PACK_IF_FITS;
    if (c->float_left)
        old |= HIPPO_PACK_FLOAT_LEFT;
    if (c->float_right)
        old |= HIPPO_PACK_FLOAT_RIGHT;
    if (c->clear_left)
        old |= HIPPO_PACK_CLEAR_LEFT;
    if (c->clear_right)
        old |= HIPPO_PACK_CLEAR_RIGHT;

    if (old == flags)
        return FALSE; /* no change */

    c->expand = (flags & HIPPO_PACK_EXPAND) != 0;
    c->end = (flags & HIPPO_PACK_END) != 0;
    c->fixed = (flags & HIPPO_PACK_FIXED) != 0;
    c->if_fits = (flags & HIPPO_PACK_IF_FITS) != 0;
    c->float_left = (flags & HIPPO_PACK_FLOAT_LEFT) != 0;
    c->float_right = (flags & HIPPO_PACK_FLOAT_RIGHT) != 0;
    c->clear_left = (flags & HIPPO_PACK_CLEAR_LEFT) != 0;
    c->clear_right = (flags & HIPPO_PACK_CLEAR_RIGHT) != 0;
    
    if ((c->float_left && c->float_right) ||
        (c->float_left && c->fixed) ||
        (c->float_right && c->fixed))
        g_warning("Only one of FLOAT_LEFT, FLOAT_RIGHT, FLOAT_EXPAND can be used at once");
    
    return TRUE;
}


typedef struct {
    HippoCanvasCompareChildFunc func;
    void *data;
} ChildSortData;

static int
child_compare_func(const void *a,
                   const void *b,
                   void       *data)
{
    ChildSortData *csd = data;
    BoxChildPrivate *child_a = (BoxChildPrivate*) a;
    BoxChildPrivate *child_b = (BoxChildPrivate*) b;
    
    return (* csd->func) (child_a->public.item, child_b->public.item, csd->data);
}

void
hippo_canvas_box_sort(HippoCanvasBox              *box,
                      HippoCanvasCompareChildFunc  compare_func,
                      void                        *data)
{
    ChildSortData csd;
    csd.func = compare_func;
    csd.data = data;
    box->children = g_slist_sort_with_data(box->children, child_compare_func,
                                           &csd);
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static void
update_in_layout(BoxChildPrivate *child)
{
    child->public.in_layout = !child->public.fixed && child->public.visible;
}

static BoxChildPrivate *
child_create_from_item(HippoCanvasBox              *box,
                       HippoCanvasItem             *child,
                       HippoPackFlags               flags)
{
    BoxChildPrivate *c;

    g_object_ref(child);
    hippo_canvas_item_sink(child);
    connect_child(box, child);
    c = g_new0(BoxChildPrivate, 1);
    c->ref_count = 1;
    c->public.item = child;
    set_flags(&c->public, flags);
    c->public.visible = TRUE;
    c->min_width = -1;
    c->min_height = -1;
    c->height_request_for_width = -1;

    update_in_layout(c);

    return c;    
}

static void
child_setup(HippoCanvasBox              *box,
            HippoCanvasItem             *child)
{
    hippo_canvas_item_set_parent(child, HIPPO_CANVAS_CONTAINER(box));
    
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
hippo_canvas_box_insert_before(HippoCanvasBox              *box,
                               HippoCanvasItem             *child,
                               HippoCanvasItem             *ref_child,
                               HippoPackFlags               flags)
{
    BoxChildPrivate *c;
    BoxChildPrivate *ref_c;
    int position;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    g_return_if_fail(find_child(box, child) == NULL);
    
    ref_c = find_child(box, ref_child);
    
    g_return_if_fail(ref_c != NULL);

    c = child_create_from_item(box, child, flags);

    position = g_slist_index(box->children, ref_c);
    box->children = g_slist_insert(box->children, c, position);

    child_setup(box, child);
}

void
hippo_canvas_box_insert_after(HippoCanvasBox              *box,
                              HippoCanvasItem             *child,
                              HippoCanvasItem             *ref_child,
                              HippoPackFlags               flags)
{
    BoxChildPrivate *c;
    BoxChildPrivate *ref_c;
    int position;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    g_return_if_fail(find_child(box, child) == NULL);
    
    ref_c = find_child(box, ref_child);
    
    g_return_if_fail(ref_c != NULL);

    c = child_create_from_item(box, child, flags);

    position = g_slist_index(box->children, ref_c);
    box->children = g_slist_insert(box->children, c, ++position);

    child_setup(box, child);
}

void
hippo_canvas_box_insert_sorted(HippoCanvasBox              *box,
                               HippoCanvasItem             *child,
                               HippoPackFlags               flags,
                               HippoCanvasCompareChildFunc  compare_func,
                               void                        *data)
{
    BoxChildPrivate *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    g_return_if_fail(find_child(box, child) == NULL);

    c = child_create_from_item(box, child, flags);

    if (compare_func == NULL) {
        box->children = g_slist_append(box->children, c);
    } else {
        GSList *l;
        ChildSortData csd;
        csd.func = compare_func;
        csd.data = data;
        
        /* Could use g_slist_insert_sorted_with_data() for glib >= 2.10 */
        l = box->children;
        while (l && child_compare_func(c, l->data, &csd) > 0)
            l = l->next;
        box->children = g_slist_insert_before(box->children, l, c);
    }

    child_setup(box, child);
}

/**
 * hippo_canvas_box_prepend:
 * @box: the canvas box
 * @child: the child to prepend
 * @flags: flags for how to layout the child
 *
 * See hippo_canvas_box_append() for all the details.
 */
void
hippo_canvas_box_prepend(HippoCanvasBox  *box,
                         HippoCanvasItem *child,
                         HippoPackFlags   flags)
{
    BoxChildPrivate *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    g_return_if_fail(find_child(box, child) == NULL);

    c = child_create_from_item(box, child, flags);

    box->children = g_slist_prepend(box->children, c);

    child_setup(box, child);
}

/**
 * hippo_canvas_box_append:
 * @box: the canvas box
 * @child: the child to append
 * @flags: flags for how to layout the child
 *
 * Adds a child item to the box.  #HIPPO_PACK_END means the child
 * should be added at the end of the box while normally the child
 * would be added at the start of the box. This is just like the "pack
 * end" concept in #GtkBox. #HIPPO_PACK_FIXED means the child is not
 * included in the layout algorithm, you have to set its position
 * manually with hippo_canvas_box_set_position(). #HIPPO_PACK_IF_FITS
 * means the child is "all or nothing"; if there isn't room to give
 * all children their natural width, then the "if fits" children can
 * be dropped. Dropping some children is often nicer than having all
 * the children squished or forcing the entire box to be too big.
 *
 * If you are familiar with GTK+ widget layout, note that #GtkMisc
 * functionality (alignment and padding) are incorporated into the
 * items themselves in the form of the alignment and border/padding
 * properties. #GtkFixed functionality is part of #HippoCanvasBox
 * because children can be packed #HIPPO_PACK_FIXED. #HippoCanvasBox
 * has an orientation property, rather than two subclasses for
 * horizontal and vertical. And #HIPPO_PACK_IF_FITS is not available
 * in GTK+ because it requires the "natural size" vs. "minimum size"
 * distinction that GTK+ lacks.
 */
void
hippo_canvas_box_append(HippoCanvasBox  *box,
                        HippoCanvasItem *child,
                        HippoPackFlags   flags)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));
    
    hippo_canvas_box_insert_sorted(box, child, flags, NULL, NULL);
}

static void
remove_box_child(HippoCanvasBox *box,
                 BoxChildPrivate  *c)
{
    HippoCanvasItem *child;

    child = c->public.item;
    c->public.item = NULL;

    box->children = g_slist_remove(box->children, c);
    disconnect_child(box, child);
    hippo_canvas_item_set_context(child, NULL);
 
    g_object_unref(child);

    hippo_canvas_box_child_unref(&c->public);

    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));    
}

void
hippo_canvas_box_remove(HippoCanvasBox  *box,
                        HippoCanvasItem *child)
{
    BoxChildPrivate *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to remove a canvas item from a box it isn't in");
        return;
    }

    remove_box_child(box, c);
}

/**
 * hippo_canvas_box_remove_all:
 * @box: the canvas box
 * 
 * Removes all children from the box without destroying them. You probably want
 * hipppo_canvas_box_clear() instead unless you have a particular reason to
 * preserve the children. The explicit destroy added by hipppo_canvas_box_clear()
 * will make your application more robust against memory leaks.
 **/
void
hippo_canvas_box_remove_all(HippoCanvasBox *box)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    
    while (box->children != NULL) {
        BoxChildPrivate *child = box->children->data;
        remove_box_child(box, child);
    }
}

/**
 * hippo_canvas_box_clear:
 * @box: the canvas box
 * 
 * Removes all children from the box and calls hippo_canvas_item_destroy() on them.
 **/
void
hippo_canvas_box_clear(HippoCanvasBox *box)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    
    while (box->children != NULL) {
        BoxChildPrivate *child = box->children->data;
        HippoCanvasItem *item = child->public.item;

        g_object_ref(item);

        /* We could let this happen as a side-effect of destroying
         * the child; doing it this way results in a little less confusing
         * reentrancy.
         */
        remove_box_child(box, child);

        hippo_canvas_item_destroy(item);
        g_object_unref(item);
    }
}

void
hippo_canvas_box_move(HippoCanvasBox  *box,
                      HippoCanvasItem *child,
                      HippoGravity     gravity,
                      int              x,
                      int              y)
{
    BoxChildPrivate *c;
    int w, h;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to move a canvas item that isn't in the box");
        return;
    }

    if (!c->public.fixed) {
        g_warning("Trying to move a canvas box child that isn't fixed");
        return;
    }

    if (gravity != HIPPO_GRAVITY_NORTH_WEST) {
        int natural_width;
        int natural_height;
        
        /* Ensure the child has been requested */
        request_child_natural_size(&c->public, &natural_width, &natural_height);

        switch (gravity) {
        case HIPPO_GRAVITY_NORTH_WEST:
            break;
        case HIPPO_GRAVITY_NORTH_EAST:
            x = x - natural_width;
            break;
        case HIPPO_GRAVITY_SOUTH_WEST:
            y = y - natural_height;
            break;
        case HIPPO_GRAVITY_SOUTH_EAST:
            x = x - natural_width;
            y = y - natural_height;
            break;
        }
    }

    if (c->x != x || c->y != y) {
        /* We only repaint, don't queue a resize - fixed items don't affect the
         * size request.
         */
        hippo_canvas_item_get_allocation(child, &w, &h);
        
        if (c->public.visible)
            hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), c->x, c->y, w, h);
        
        c->x = x;
        c->y = y;
        
        if (c->public.visible)
            hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), c->x, c->y, w, h);
    }
}

void
hippo_canvas_box_set_position(HippoCanvasBox  *box,
                              HippoCanvasItem *child,
                              int              x,
                              int              y)
{
    hippo_canvas_box_move(box, child, HIPPO_GRAVITY_NORTH_WEST, x, y);
}

void
hippo_canvas_box_get_position(HippoCanvasBox  *box,
                              HippoCanvasItem *child,
                              int             *x,
                              int             *y)
{
    BoxChildPrivate *c;

    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to get the position of a canvas item that isn't in the box");
        return;
    }

    *x = c->x;
    *y = c->y;
}

static void
children_list_callback(HippoCanvasItem *item,
                       void            *data)
{
    GList **children = (GList**)data;

    *children = g_list_prepend (*children, item);
}

GList*
hippo_canvas_box_get_children(HippoCanvasBox *box)
{
    GList *children = NULL;

    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), NULL);

    hippo_canvas_box_foreach(box, children_list_callback, &children);

    /* return children in their original order */
    children = g_list_reverse(children);
    
    return children;
}

gboolean
hippo_canvas_box_is_empty(HippoCanvasBox *box)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), FALSE);

    return box->children == NULL;
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
        BoxChildPrivate *child = link->data;
        next = link->next; /* allow removal of children in the foreach */
        
        (* func) (child->public.item, data);

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
        BoxChildPrivate *child = link->data;
        
        child->public.end = !child->public.end;

        link = link->next;
    }

    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

static gboolean
hippo_canvas_box_get_child_visible (HippoCanvasContainer        *container,
                                    HippoCanvasItem             *child)
{
    BoxChildPrivate *c;
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX(container);
    
    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to get visibility on a canvas item that isn't in the box");
        return FALSE;
    }

    return c->public.visible;
}

static void
hippo_canvas_box_set_child_visible (HippoCanvasContainer        *container,
                                    HippoCanvasItem             *child,
                                    gboolean                     visible)
{
    BoxChildPrivate *c;
    HippoCanvasBox *box;

    box = HIPPO_CANVAS_BOX(container);
    
    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to set visibility on a canvas item that isn't in the box");
        return;
    }
    
    visible = visible != FALSE;
    if (visible == c->public.visible)
        return;
    
    c->public.visible = visible;

    update_in_layout(c);    

    if (c->public.fixed) {
        int w, h;
        
        /* We only repaint, don't queue a resize - fixed items don't affect the
         * size request.
         */
        hippo_canvas_item_get_allocation(child, &w, &h);
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(box), c->x, c->y, w, h);
    } else
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}

void
hippo_canvas_box_set_child_packing (HippoCanvasBox              *box,
                                    HippoCanvasItem             *child,
                                    HippoPackFlags               flags)
{
    BoxChildPrivate *c;
    
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));
    g_return_if_fail(HIPPO_IS_CANVAS_ITEM(child));

    c = find_child(box, child);

    if (c == NULL) {
        g_warning("Trying to set flags on a canvas item that isn't in the box");
        return;
    }

    if (set_flags(&c->public, flags)) {
        update_in_layout(c);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
    }
}

void
hippo_canvas_box_set_clickable (HippoCanvasBox *box,
                                gboolean        clickable)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));

    box->clickable = clickable;
}

gboolean
hippo_canvas_box_is_clickable (HippoCanvasBox *box)
{
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), FALSE);

    return box->clickable;
}

GType
hippo_canvas_box_child_get_type (void)
{
  static GType our_type = 0;
  
  if (our_type == 0)
    our_type = g_boxed_type_register_static (("HippoCanvasBoxChild"),
					     (GBoxedCopyFunc) hippo_canvas_box_child_ref,
					     (GBoxedFreeFunc) hippo_canvas_box_child_unref);

  return our_type;
}

/**
 * hippo_canvas_box_child_ref:
 * @child: a #HippoCanvasBoxChild
 *
 * Adds a reference to a #HippoCanvasBoxChild. The child will not
 * be freed before a corresponding call to hippo_canvas_box_child_unref()
 *
 * Return value: the child passed in
 */
HippoCanvasBoxChild *
hippo_canvas_box_child_ref(HippoCanvasBoxChild *child)
{
    g_return_val_if_fail(child != NULL, NULL);

    ((BoxChildPrivate *)child)->ref_count++;

    return child;
}

/**
 * hippo_canvas_box_child_ref:
 * @child: a #HippoCanvasBoxChild
 *
 * Release a reference previously added to a #HippoCanvasBoxChild. If ther
 * are no more outstanding references to the box child (including references
 * held by the parent box), the child and associated data will be freed.
 */
void
hippo_canvas_box_child_unref(HippoCanvasBoxChild *child)
{
    BoxChildPrivate *private;
    
    g_return_if_fail(child != NULL);

    private = (BoxChildPrivate *)child;

    private->ref_count--;
    if (private->ref_count == 0) {
        BoxChildQDataNode *node;

        for (node = private->qdata; node; node = node->next) {
            if (node->notify)
                node->notify(node->data);
        }

        if (private->qdata)
            g_slice_free_chain(BoxChildQDataNode, private->qdata, next);
    
        g_free(private);
    }
}

/**
 * hippo_canvas_box_child_set_qdata:
 * @child: a #HippoCanvasBoxChild
 * @key: a #GQuark identifying the data to store
 * @data: the data to store
 * @notify: a function to call when the data is replaced or the box child is freed
 *
 * Associates data with a #HippoCanvasBoxChild object
 * 
 * Return value: the data previously stored, or %NULL if data was not
 *  previously stored for the given key.
 */
void
hippo_canvas_box_child_set_qdata(HippoCanvasBoxChild *child,
                                 GQuark               key,
                                 gpointer             data,
                                 GDestroyNotify       notify)
{
    BoxChildPrivate *private = (BoxChildPrivate *)child;
    BoxChildQDataNode *node;
    
    for (node = private->qdata; node; node = node->next) {
        if (node->key == key)
            break;
    }

    if (node == NULL) {
        node = g_slice_new(BoxChildQDataNode);
        node->key = key;
        node->next = private->qdata;
        private->qdata = node;
    } else {
        if (node->notify)
            node->notify(node->data);
    }
    
    node->data = data;
    node->notify = notify;
}

/**
 * hippo_canvas_box_child_get_qdata:
 * @child: a #HippoCanvasBoxChild
 * @key: a #GQuark identifying the data to retrieve
 *
 * Retrieves data previously stored with hippo_canvas_box_child_set_qdata()
 * 
 * Return value: the data previously stored, or %NULL if data was not
 *  previously stored for the given key.
 */
gpointer
hippo_canvas_box_child_get_qdata(HippoCanvasBoxChild *child,
                                 GQuark               key)
{
    BoxChildPrivate *private = (BoxChildPrivate *)child;
    BoxChildQDataNode *node;

    for (node = private->qdata; node; node = node->next) {
        if (node->key == key)
            return node->data;
    }

    return NULL;
}

/**
 * hippo_canvas_box_find_box_child(HippoCanvasBox *box)
 * @box: a #HippoCanvasBox
 * @item: the canvas item to find the child object for
 *
 * Locates the #HippoCanvasBoxChild object for an item that is a child of the box.
 *
 * Return value: The #HippoCanvasBoxChild for the child item, or %NULL if the
 *  item is not a child of the box. No reference is added to the returned
 *  item.
 */
HippoCanvasBoxChild *
hippo_canvas_box_find_box_child(HippoCanvasBox  *box,
                                HippoCanvasItem *item)
{
    BoxChildPrivate *private;
    
    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), NULL);

    private = find_child(box, item);
    if (private)
        return &private->public;
    else
        return NULL;
}

/**
 * hippo_canvas_box_get_layout_children()
 * @box: a #HippoCanvasBox
 * 
 * Return the list of children that a layout manager should manage. As compared
 * to hippo_canvas_box_get_children() this list omits widgets that are
 * not visible, positioned at a fixed position, or not geometry managed for
 * some other reason. It also returns a list of #HippoCanvasBoxChild rather
 * than a list of #HippoCanvasItem.
 *
 * If you are implementing a layout manager that iterates over the box's child list
 * directly (to save creating the list, say), you should check the 'in_layout'
 * property of each child before laying it out.
 *
 * Return value: a list of HippoCanvasBoxChild. The list should be freed with
 *   g_list_free(), but the members don't have to be freed. (No reference count
 *   is added to them.)
 */
GList *
hippo_canvas_box_get_layout_children (HippoCanvasBox *box)
{
    GList *result;
    GSList *link;

    g_return_val_if_fail(HIPPO_IS_CANVAS_BOX(box), NULL);

    result = NULL;
    for (link = box->children; link != NULL; link = link->next) {
        HippoCanvasBoxChild *child = link->data;
        if (child->in_layout)
            result = g_list_prepend(result, child);
    }

    return g_list_reverse(result);
}

/**
 * hippo_canvas_box_set_layout:
 * @box: a #HippoCanvasBox
 * @layout: the layout manager to set on the box or %NULL to use the
 *   default box layout manager.
 * 
 * Sets the layout manager to use for the box. A layout manager provides
 * an alternate method of laying out the children of a box. Once you set
 * a layout manager for the box, you will typically want to add children
 * to the box using the methods of the layout manager, rather than the
 * methods of the box, so that you can specify packing options that are
 * specific to the layout manager.
 */
void
hippo_canvas_box_set_layout(HippoCanvasBox    *box,
                            HippoCanvasLayout *layout)
{
    g_return_if_fail(HIPPO_IS_CANVAS_BOX(box));

    if (box->layout) {
        hippo_canvas_layout_set_box(box->layout, NULL);
        g_object_unref(box->layout);
        box->layout = NULL;
    }

    box->layout = layout;

    if (box->layout) {
        g_object_ref(box->layout);
        hippo_canvas_layout_set_box(box->layout, box);
    }

    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(box));
}
