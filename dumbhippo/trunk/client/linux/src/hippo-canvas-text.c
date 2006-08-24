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
                                                      HippoDrawable   *drawable);
static int      hippo_canvas_text_get_width_request  (HippoCanvasItem *item);
static int      hippo_canvas_text_get_height_request (HippoCanvasItem *item,
                                                      int              for_width);
static gboolean hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                                      HippoEvent      *event);

struct _HippoCanvasText {
    HippoCanvasBox box;
    guint32 color_rgba;
    guint32 background_color_rgba;
    char *text;
};

struct _HippoCanvasTextClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_0,
    PROP_TEXT,
    PROP_COLOR,
    PROP_BACKGROUND_COLOR
};

#define DEFAULT_FOREGROUND 0x000000ff
#define DEFAULT_BACKGROUND 0xffffffff

G_DEFINE_TYPE_WITH_CODE(HippoCanvasText, hippo_canvas_text, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_text_iface_init));

static void
hippo_canvas_text_init(HippoCanvasText *text)
{
    text->color_rgba = DEFAULT_FOREGROUND;
    text->background_color_rgba = DEFAULT_BACKGROUND;
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
                                    PROP_BACKGROUND_COLOR,
                                    g_param_spec_uint("background-color",
                                                      _("Background Color"),
                                                      _("32-bit RGBA background color"),
                                                      0,
                                                      G_MAXUINT,
                                                      DEFAULT_BACKGROUND,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_text_finalize(GObject *object)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(object);

    g_free(text->text);
    text->text = NULL;
    
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
        break;
    case PROP_COLOR:
        text->color_rgba = g_value_get_uint(value);
        break;
    case PROP_BACKGROUND_COLOR:
        text->background_color_rgba = g_value_get_uint(value);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }

    /* FIXME add a way to only trigger a redraw, not a resize */
    hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(text));
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
    case PROP_BACKGROUND_COLOR:
        g_value_set_uint(value, text->background_color_rgba);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_text_paint(HippoCanvasItem *item,
                        HippoDrawable   *drawable)
{
    HippoCanvasText *text = HIPPO_CANVAS_TEXT(item);
    cairo_t *cr;

    cr = hippo_drawable_get_cairo(drawable);

    hippo_canvas_item_push_cairo(item, cr); /* FIXME do this in container items on behalf of children */

    /* fill background */
    if ((text->background_color_rgba & 0xff) != 0) {
        hippo_cairo_set_source_rgba32(cr, text->background_color_rgba);
        cairo_paint(cr);
    }

    /* draw foreground */
    if ((text->color_rgba & 0xff) != 0 && text->text != NULL) {
        PangoLayout *layout;
        PangoFontDescription *font;
        int width, height;
        int layout_width, layout_height;
        int x, y;
        
        hippo_canvas_item_get_allocation(item, NULL, NULL, &width, &height);
        
        hippo_cairo_set_source_rgba32(cr, text->color_rgba);

        layout = pango_cairo_create_layout(cr);
        pango_layout_set_text(layout, text->text, -1);
        font = pango_font_description_from_string("Sans 12");
        pango_layout_set_font_description(layout, font);
        pango_font_description_free(font);

        /* center it */
        pango_layout_get_size(layout, &layout_width, &layout_height);
        layout_width /= PANGO_SCALE;
        layout_height /= PANGO_SCALE;
        x = (width - layout_width) / 2;
        if (x < 0)
            x = 0;
        y = (height - layout_height) / 2;
        if (y < 0)
            y = 0;
        cairo_move_to (cr, x, y);
        pango_cairo_show_layout(cr, layout);
        
        g_object_unref(layout);
    }

    hippo_canvas_item_pop_cairo(item, cr);
    
    /* Draw any children (FIXME inside pop_cairo once HippoCanvasBox::paint() is fixed
     * to automatically push/pop cairo coords)
     */
    item_parent_class->paint(item, drawable);
}

static int
hippo_canvas_text_get_width_request(HippoCanvasItem *item)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */
    int children_width;

    /* FIXME to do this we need the cairo or gdk context in order to create a layout
     * I think, so it requires some concept of "realized"? or at least an idea that
     * the platform-specific canvas widget provides an interface to items including
     * stuff like create_layout()
     */
    children_width = item_parent_class->get_width_request(item);

    return children_width;
}

static int
hippo_canvas_text_get_height_request(HippoCanvasItem *item,
                                     int              for_width)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */
    int children_height;
    
    children_height = item_parent_class->get_height_request(item, for_width);

    return children_height;
}


static gboolean
hippo_canvas_text_button_press_event (HippoCanvasItem *item,
                                      HippoEvent      *event)
{
    /* HippoCanvasText *text = HIPPO_CANVAS_TEXT(item); */

    return item_parent_class->button_press_event(item, event);
}
