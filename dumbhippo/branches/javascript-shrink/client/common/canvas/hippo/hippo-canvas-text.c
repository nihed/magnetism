/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-type-builtins.h"
#include "hippo-canvas-internal.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-box.h"
#include <pango/pangocairo.h>
#include <stdlib.h>
#include <string.h>

static void      hippo_canvas_text_init                (HippoCanvasText       *text);
static void      hippo_canvas_text_class_init          (HippoCanvasTextClass  *klass);
static void      hippo_canvas_text_iface_init          (HippoCanvasItemIface   *item_class);
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
static gboolean hippo_canvas_text_button_press_event (HippoCanvasItem    *item,
                                                      HippoEvent         *event);
static void     hippo_canvas_text_set_context        (HippoCanvasItem    *item,
                                                      HippoCanvasContext *context);

/* Box methods */
static void hippo_canvas_text_paint_below_children       (HippoCanvasBox *box,
                                                          cairo_t        *cr,
                                                          HippoRectangle *damaged_box);
static int  hippo_canvas_text_get_content_width_request  (HippoCanvasBox *box);
static int  hippo_canvas_text_get_content_natural_width  (HippoCanvasBox *box);
static int  hippo_canvas_text_get_content_height_request (HippoCanvasBox *box,
                                                          int             for_width);

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_TEXT,
    PROP_ATTRIBUTES,
    PROP_FONT_SCALE,
    PROP_SIZE_MODE
};

#define DEFAULT_FOREGROUND 0x000000ff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasText, hippo_canvas_text, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_text_iface_init));

static void
hippo_canvas_text_init(HippoCanvasText *text)
{
    text->font_scale = 1.0;
    text->size_mode = HIPPO_CANVAS_SIZE_FULL_WIDTH;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_text_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->button_press_event = hippo_canvas_text_button_press_event;

    item_class->set_context = hippo_canvas_text_set_context;
}

static void
hippo_canvas_text_class_init(HippoCanvasTextClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBoxClass *box_class = HIPPO_CANVAS_BOX_CLASS(klass);
    
    object_class->set_property = hippo_canvas_text_set_property;
    object_class->get_property = hippo_canvas_text_get_property;

    object_class->finalize = hippo_canvas_text_finalize;

    box_class->paint_below_children = hippo_canvas_text_paint_below_children;
    box_class->get_content_width_request = hippo_canvas_text_get_content_width_request;
    box_class->get_content_natural_width = hippo_canvas_text_get_content_natural_width;
    box_class->get_content_height_request = hippo_canvas_text_get_content_height_request;
    
    g_object_class_install_property(object_class,
                                    PROP_TEXT,
                                    g_param_spec_string("text",
                                                        _("Text"),
                                                        _("Text to display"),
                                                        NULL,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_ATTRIBUTES,
                                    g_param_spec_boxed ("attributes",
                                                        _("Attributes"),
                                                        _("A list of style attributes to apply to the text"),
                                                        PANGO_TYPE_ATTR_LIST,
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
                                    g_param_spec_enum("size-mode",
                                                      _("Size mode"),
                                                      _("Mode for size request and allocation"),
                                                      HIPPO_TYPE_CANVAS_SIZE_MODE,
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
        {
            const char *new_text;
            new_text = g_value_get_string(value);
            if (!(new_text == text->text ||
                  (new_text && text->text && strcmp(new_text, text->text) == 0))) {
                g_free(text->text);
                text->text = g_strdup(new_text);
                hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
            }
        }
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
    case PROP_FONT_SCALE:
        text->font_scale = g_value_get_double(value);
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
        break;
    case PROP_SIZE_MODE:
        text->size_mode = g_value_get_enum(value);
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
    case PROP_ATTRIBUTES:
        g_value_set_boxed(value, text->attributes);
        break;
    case PROP_FONT_SCALE:
        g_value_set_double(value, text->font_scale);
        break;
    case PROP_SIZE_MODE:
        g_value_set_enum(value, text->size_mode);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_text_set_context(HippoCanvasItem    *item,
                              HippoCanvasContext *context)
{
    HippoCanvasBox *box = HIPPO_CANVAS_BOX(item);
    gboolean changed;
    
    changed = context != box->context;
    
    item_parent_class->set_context(item, context);

    /* we can't create a layout until we have a context,
     * so we have to queue a size change when the context
     * is set.
     */
    if (changed)
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(item));
}

static char*
remove_newlines(const char *text)
{
    char *s;
    char *p;

    s = g_strdup(text);

    for (p = s; *p != '\0'; ++p) {
        if (*p == '\n' || *p == '\r')
            *p = ' ';
    }

    return s;
}

static PangoLayout*
create_layout(HippoCanvasText *text,
              int              allocation_width)
{
    HippoCanvasContext *context;
    PangoLayout *layout;

    /* Note that our context is *ourselves* not box->context i.e. we want
     * our own style, etc. to affect what we render. Our context methods
     * will chain up as needed.
     */
    context = HIPPO_CANVAS_CONTEXT(text);

    g_return_val_if_fail(context != NULL, NULL);
    
    layout = hippo_canvas_context_create_layout(context);

    {
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

        hippo_canvas_context_affect_font_desc(context,
                                              composite);
        
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

            /* For now if we say ellipsize end, we always just want one line.
             * Maybe this should be an orthogonal property?
             */
            if (text->size_mode == HIPPO_CANVAS_SIZE_ELLIPSIZE_END) {
                pango_layout_set_single_paragraph_mode(layout, TRUE);

                /* Pango's line separator character in this case is ugly, so we
                 * fix it. Not a very efficient approach, but oh well.
                 */
                if (text->text != NULL) {
                    char *new_text = remove_newlines(text->text);
                    /* avoid making the layout recompute everything
                     * if we didn't have newlines anyhow
                     */
                    if (strcmp(text->text, new_text) != 0) {
                        pango_layout_set_text(layout, new_text, -1);
                    }
                    g_free(new_text);
                }
            }
        }
    }
    
    return layout;
}

static void
hippo_canvas_text_paint_below_children(HippoCanvasBox  *box,
                                       cairo_t         *cr,
                                       HippoRectangle  *damaged_box)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(box);
    guint32 color_rgba;
    HippoCanvasContext *context;

    /* note, we want to use _ourselves_ as context so we get our own style */
    context = HIPPO_CANVAS_CONTEXT(box);

    color_rgba = HIPPO_CANVAS_BOX_GET_CLASS(box)->default_color;
    hippo_canvas_context_affect_color(context, &color_rgba);
    
    if ((color_rgba & 0xff) != 0 && text->text != NULL) {
        PangoLayout *layout;
        int layout_width, layout_height;
        int x, y, w, h;
        int allocation_width, allocation_height;
        
        hippo_canvas_item_get_allocation(HIPPO_CANVAS_ITEM(box),
                                         &allocation_width, &allocation_height);
        
        layout = create_layout(text, allocation_width);
        pango_layout_get_size(layout, &layout_width, &layout_height);
        layout_width /= PANGO_SCALE;
        layout_height /= PANGO_SCALE;
        
        hippo_canvas_box_align(box,
                               layout_width, layout_height,
                               &x, &y, &w, &h);

        /* we can't really "fill" so we fall back to center if we seem to be
         * in fill mode
         */
        if (w > layout_width) {
            x += (w - layout_width) / 2;
        }
        if (h > layout_height) {
            y += (h - layout_height) / 2;
        }
        
        /* Clipping is needed since the layout size could exceed our
         * allocation if we got a too-small allocation.
         * FIXME It would be better to ellipsize or something instead, though.
         */
        cairo_save(cr);
        cairo_rectangle(cr, 0, 0, allocation_width, allocation_height);
        cairo_clip(cr);

        cairo_move_to (cr, x, y);
        hippo_cairo_set_source_rgba32(cr, color_rgba);
        pango_cairo_show_layout(cr, layout);
        cairo_restore(cr);
        
        g_object_unref(layout);
    }
}

static int
hippo_canvas_text_get_content_width_request(HippoCanvasBox *box)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(box);
    int children_width;
    int layout_width;
    
    children_width = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_text_parent_class)->get_content_width_request(box);

    if (text->size_mode != HIPPO_CANVAS_SIZE_FULL_WIDTH) {
        layout_width = 0;
    } else {
        if (box->context != NULL) {
            PangoLayout *layout = create_layout(text, -1);
            pango_layout_get_size(layout, &layout_width, NULL);
            layout_width /= PANGO_SCALE;
            g_object_unref(layout);
        } else {
            layout_width = 0;
        }
    }

    return MAX(children_width, layout_width);
}

static int
hippo_canvas_text_get_content_natural_width (HippoCanvasBox *box)
{
    HippoCanvasText *text;
    HippoCanvasBoxClass *box_class;
    int children_width;
    int layout_width;

    text = HIPPO_CANVAS_TEXT(box);
    box_class = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_text_parent_class);
    
    children_width = box_class->get_content_natural_width(box);

    if (text->size_mode == HIPPO_CANVAS_SIZE_FULL_WIDTH && children_width < 0) {
        /* natural width is same as request */
        layout_width = -1;
    } else {
        /* request will have been 0, compute the real natural width here. */
        /* FIXME if the children_width isn't -1 we recompute here what we've
         * already computed in get_width_request
         */
        if (box->context != NULL) {
            PangoLayout *layout = create_layout(text, -1);
            pango_layout_get_size(layout, &layout_width, NULL);
            layout_width /= PANGO_SCALE;
            g_object_unref(layout);
        } else {
            layout_width = 0;
        }
    }

    if (children_width < 0 && layout_width < 0) {
        return -1;
    } else {
        g_assert(layout_width >= 0);
        if (children_width < 0) {
            /* FIXME We have to re-request the children which is
             * potentially expensive, maybe should rethink
             * something. Text items rarely have children anyway
             * though so it doesn't make any difference right now.
             */
            children_width = box_class->get_content_width_request(box);
        }
        
        g_assert(children_width >= 0 && layout_width >= 0);
        return MAX(children_width, layout_width);
    }
}

static int
hippo_canvas_text_get_content_height_request(HippoCanvasBox  *box,
                                             int              for_width)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(box);
    int children_height;
    PangoLayout *layout;
    int layout_height;

    children_height = HIPPO_CANVAS_BOX_CLASS(hippo_canvas_text_parent_class)->get_content_height_request(box,
                                                                                                         for_width);

    if (for_width > 0) {
        if (box->context != NULL) {
            layout = create_layout(text, for_width);
            pango_layout_get_size(layout, NULL, &layout_height);
            layout_height /= PANGO_SCALE;
            g_object_unref(layout);
        } else {
            layout_height = 0;
        }
    } else {
        layout_height = 0;
    }
    
    return MAX(layout_height, children_height);
}

static gboolean
hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */

    /* see if a child wants it */
    return item_parent_class->button_press_event(item, event);
}
