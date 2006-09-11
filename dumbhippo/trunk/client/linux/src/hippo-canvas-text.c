/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-text.h"
#include "hippo-canvas-box.h"
#include <pango/pangocairo.h>

static void      hippo_canvas_text_init                (HippoCanvasText       *text);
static void      hippo_canvas_text_class_init          (HippoCanvasTextClass  *klass);
static void      hippo_canvas_text_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_text_finalize            (GObject                *object);

static void hippo_canvas_text_set_property (GObject      *object,
                                            guint         prop_id,
                                            const GValue *value,
                                            GParamSpec   *pspec);
static void hippo_canvas_text_get_property (GObject      *object,
                                            guint         prop_id,
                                            GValue       *value,
                                            GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_text_paint              (HippoCanvasItem *item,
                                                      cairo_t         *cr);
static int      hippo_canvas_text_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_text_get_height_request (HippoCanvasItem *item,
                                                      int              for_width);
static gboolean hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                                      HippoEvent      *event);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_TEXT,
    PROP_COLOR,
    PROP_ATTRIBUTES,
    PROP_FONT,
    PROP_FONT_DESC,
    PROP_FONT_SCALE,
    PROP_SIZE_MODE
};

#define DEFAULT_FOREGROUND 0x000000ff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasText, hippo_canvas_text, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_text_iface_init));

static void
hippo_canvas_text_init(HippoCanvasText *text)
{
    text->color_rgba = DEFAULT_FOREGROUND;
    text->font_scale = 1.0;
    text->size_mode = HIPPO_CANVAS_SIZE_FULL_WIDTH;
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_text_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_text_paint;
    item_class->get_width_request = hippo_canvas_text_get_width_request;
    item_class->get_height_request = hippo_canvas_text_get_height_request;
    item_class->button_press_event = hippo_canvas_text_button_press_event;
}

static void
hippo_canvas_text_class_init(HippoCanvasTextClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_text_set_property;
    object_class->get_property = hippo_canvas_text_get_property;

    object_class->finalize = hippo_canvas_text_finalize;

    g_object_class_install_property(object_class,
                                    PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text to display"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_COLOR,
                                    g_param_spec_uint("color",
                                                      _("Foreground Color"),
                                                      _("32-bit RGBA foreground color"),
                                                      0,
                                                      G_MAXUINT,
                                                      DEFAULT_FOREGROUND,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_ATTRIBUTES,
                                    g_param_spec_boxed ("attributes",
                                                        _("Attributes"),
                                                        _("A list of style attributes to apply to the text"),
                                                        PANGO_TYPE_ATTR_LIST,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_FONT,
                                    g_param_spec_string("font",
                                                        _("Font"),
                                                        _("Font description as a string"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_FONT_DESC,
                                    g_param_spec_boxed ("font-desc",
                                                        _("Font Description"),
                                                        _("Font description as a PangoFontDescription object"),
                                                        PANGO_TYPE_FONT_DESCRIPTION,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));
    g_object_class_install_property(object_class,
                                    PROP_FONT_SCALE,
                                    g_param_spec_double("font-scale",
                                                        _("Font scale"),
                                                        _("Scale factor for fonts"),
                                                        0.0,
                                                        100.0,
                                                        1.0,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_SIZE_MODE,
                                    g_param_spec_int("size-mode",
                                                     _("Size mode"),
                                                     _("Mode for size request and allocation"),
                                                     0,
                                                     10,
                                                     HIPPO_CANVAS_SIZE_FULL_WIDTH,
                                                     G_PARAM_READABLE | G_PARAM_WRITABLE));    
}

static void
hippo_canvas_text_finalize(GObject *object)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(object);

    g_free(text->text);
    text->text = NULL;

    if (text->attributes) {
        pango_attr_list_unref(text->attributes);
        text->attributes = NULL;
    }

    if (text->font_desc) {
        pango_font_description_free(text->font_desc);
        text->font_desc = NULL;
    }
    
    G_OBJECT_CLASS(hippo_canvas_text_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_text_new(void)
{
    HippoCanvasText *text = g_object_new(HIPPO_TYPE_CANVAS_TEXT, NULL);

    
    
    return HIPPO_CANVAS_ITEM(text);
}

static void
hippo_canvas_text_set_property(GObject         *object,
                               guint            prop_id,
                               const GValue    *value,
                               GParamSpec      *pspec)
{
    HippoCanvasText *text;

    text = HIPPO_CANVAS_TEXT(object);

    switch (prop_id) {
    case PROP_TEXT:
        g_free(text->text);
        text->text = g_value_dup_string(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        break;
    case PROP_COLOR:
        text->color_rgba = g_value_get_uint(value);
        hippo_canvas_item_emit_paint_needed(HIPPO_CANVAS_ITEM(text), 0, 0, -1, -1);
        break;
    case PROP_ATTRIBUTES:
        {
            PangoAttrList *attrs = g_value_get_boxed(value);
            if (attrs)
                pango_attr_list_ref(attrs);
            if (text->attributes)
                pango_attr_list_unref(text->attributes);            
            text->attributes = attrs;
            hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        }
        break;
    case PROP_FONT:
        {
            const char *s;
            PangoFontDescription *desc;
            s = g_value_get_string(value);
            if (s != NULL) {
                desc = pango_font_description_from_string(s);
                if (desc == NULL) {
                    g_warning("Failed to parse font description string '%s'", s);
                } else {
                    if ((pango_font_description_get_set_fields(desc) & PANGO_FONT_MASK_SIZE) != 0 &&
                        pango_font_description_get_size(desc) <= 0) {
                        g_warning("font size set to 0, not going to work well");
                    }
                }
            } else {
                desc = NULL;
            }
            /* this handles whether to queue repaint/resize */
            g_object_set(object, "font-desc", desc, NULL);
            if (desc)
                pango_font_description_free(desc);
        }
        break;
    case PROP_FONT_DESC:
        {
            PangoFontDescription *desc = g_value_get_boxed(value);

            if (!(desc == NULL && text->font_desc == NULL)) {
                if (text->font_desc) {
                    pango_font_description_free(text->font_desc);
                    text->font_desc = NULL;
                }
                if (desc != NULL) {
                    text->font_desc = pango_font_description_copy(desc);
                }
                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
            }
        }
        break;
    case PROP_FONT_SCALE:
        text->font_scale = g_value_get_double(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        break;
    case PROP_SIZE_MODE:
        text->size_mode = g_value_get_int(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_text_get_property(GObject         *object,
                               guint            prop_id,
                               GValue          *value,
                               GParamSpec      *pspec)
{
    HippoCanvasText *text;

    text = HIPPO_CANVAS_TEXT (object);

    switch (prop_id) {
    case PROP_TEXT:
        g_value_set_string(value, text->text);
        break;
    case PROP_COLOR:
        g_value_set_uint(value, text->color_rgba);
        break;
    case PROP_ATTRIBUTES:
        g_value_set_boxed(value, text->attributes);
        break;        
    case PROP_FONT:
        {
            char *s;
            if (text->font_desc)
                s = pango_font_description_to_string(text->font_desc);
            else
                s = NULL;
            g_value_take_string(value, s);
        }
        break;
    case PROP_FONT_DESC:
        g_value_set_boxed(value, text->font_desc);
        break;
    case PROP_FONT_SCALE:
        g_value_set_double(value, text->font_scale);
        break;
    case PROP_SIZE_MODE:
        g_value_set_int(value, text->size_mode);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static PangoLayout*
create_layout(HippoCanvasText *text,
              int              allocation_width)
{
    HippoCanvasContext *context;
    PangoLayout *layout;
    
    context = hippo_canvas_box_get_context(HIPPO_CANVAS_BOX(text));
    
    layout = hippo_canvas_context_create_layout(context);
    
    if (text->font_desc) {
        const PangoFontDescription *old;
        PangoFontDescription *composite;

        composite = pango_font_description_new();
        
        old = pango_layout_get_font_description(layout);
        /* if no font desc is set on the layout, the layout uses the one
         * from the context, so emulate that here.
         */
        if (old == NULL)
            old = pango_context_get_font_description(pango_layout_get_context(layout));
        
        if (old != NULL)
            pango_font_description_merge(composite, old, TRUE);
        if (text->font_desc != NULL)
            pango_font_description_merge(composite, text->font_desc, TRUE);
        
        pango_layout_set_font_description(layout, composite);
        
        pango_font_description_free(composite);
    }
    
    {
        PangoAttrList *attrs;
        
        if (text->attributes)
            attrs = pango_attr_list_copy(text->attributes);
        else
            attrs = pango_attr_list_new();

        if (ABS(1.0 - text->font_scale) > .000001) {
            PangoAttribute *attr = pango_attr_scale_new(text->font_scale);
            attr->start_index = 0;
            attr->end_index = G_MAXUINT;
            pango_attr_list_insert(attrs, attr);
        }

        pango_layout_set_attributes(layout, attrs);
        pango_attr_list_unref(attrs);
    }
    
    if (text->text != NULL) {
        pango_layout_set_text(layout, text->text, -1);
    }

    if (allocation_width >= 0) {
        int layout_width, layout_height;
        pango_layout_get_size(layout, &layout_width, &layout_height);
        layout_width /= PANGO_SCALE;
        layout_height /= PANGO_SCALE;
        
        /* Force layout smaller if required, but we don't want to make
         * the layout _wider_ because it breaks alignment, so only do
         * this if required.
         */
        if (layout_width > allocation_width) {
            pango_layout_set_width(layout, allocation_width * PANGO_SCALE);

            /* If we set ellipsize, then it overrides wrapping. If we get
             * too-small allocation for HIPPO_CANVAS_SIZE_FULL_WIDTH, then
             * we want to ellipsize instead of wrapping.
             */
            if (text->size_mode == HIPPO_CANVAS_SIZE_WRAP_WORD) {
                pango_layout_set_ellipsize(layout, PANGO_ELLIPSIZE_NONE);
            } else {
                pango_layout_set_ellipsize(layout, PANGO_ELLIPSIZE_END);
            }
        }
    }
    
    return layout;
}

static void
hippo_canvas_text_paint(HippoCanvasItem *item,
                        cairo_t         *cr)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    int allocation_width, allocation_height;
    
    /* Draw the background and any child items */
    item_parent_class->paint(item, cr);

    /* draw text on top */
    hippo_canvas_item_get_allocation(item, &allocation_width, &allocation_height);
    
    if ((text->color_rgba & 0xff) != 0 && text->text != NULL &&
        allocation_width > 0 && allocation_height > 0) {
        PangoLayout *layout;
        int layout_width, layout_height;
        int x, y, w, h;
        
        layout = create_layout(text, allocation_width);
        pango_layout_get_size(layout, &layout_width, &layout_height);
        layout_width /= PANGO_SCALE;
        layout_height /= PANGO_SCALE;

        x = 0;
        y = 0;
        w = layout_width;
        h = layout_height;
        
        hippo_canvas_box_align(HIPPO_CANVAS_BOX(item), &x, &y, &w, &h);

        /* we can't really "fill" so we fall back to center if we seem to be
         * in fill mode
         */
        if (w > layout_width) {
            x += (w - layout_width) / 2;
        }
        if (h > layout_height) {
            y += (h - layout_height) / 2;
        }
        
        /* Clipping is needed since we have no idea how high the layout is.
         * FIXME It would be better to ellipsize or something instead, though.
         */
        cairo_save(cr);
        cairo_rectangle(cr, 0, 0, allocation_width, allocation_height);
        cairo_clip(cr);

        cairo_move_to (cr, x, y);
        hippo_cairo_set_source_rgba32(cr, text->color_rgba);
        pango_cairo_show_layout(cr, layout);
        cairo_restore(cr);
        
        g_object_unref(layout);
    }
}

static int
hippo_canvas_text_get_width_request(HippoCanvasItem *item)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_width;
    int layout_width;
    
    children_width = item_parent_class->get_width_request(item);

    if (hippo_canvas_box_get_fixed_width(HIPPO_CANVAS_BOX(item)) < 0) {
        if (text->size_mode != HIPPO_CANVAS_SIZE_FULL_WIDTH) {
            layout_width = 0;
        } else {
            PangoLayout *layout = create_layout(text, -1);
            pango_layout_get_size(layout, &layout_width, NULL);
            layout_width /= PANGO_SCALE;
        }
    } else {
        /* keep the fixed width the box will have returned */
        layout_width = children_width;
    }

    return MAX(children_width, layout_width + box->padding_left + box->padding_right);
}

static int
hippo_canvas_text_get_height_request(HippoCanvasItem *item,
                                     int              for_width)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    int children_height;
    PangoLayout *layout;
    int layout_height;
    
    children_height = item_parent_class->get_height_request(item, for_width);

    if (for_width > 0) {
        layout = create_layout(text, for_width);
        pango_layout_get_size(layout, NULL, &layout_height);
        layout_height /= PANGO_SCALE;
    } else {
        layout_height = 0;
    }
    
    return MAX(layout_height + box->padding_top + box->padding_bottom, children_height);
}

static gboolean
hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */

    /* see if a child wants it */
    return item_parent_class->button_press_event(item, event);
}
