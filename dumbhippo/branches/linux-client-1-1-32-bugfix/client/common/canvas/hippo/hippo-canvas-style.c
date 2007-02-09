/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-canvas-internal.h"
#include "hippo-canvas-type-builtins.h"
#include "hippo-canvas-style.h"
#include <stdlib.h>
#include <string.h>

static void hippo_canvas_style_init               (HippoCanvasStyle          *style);
static void hippo_canvas_style_class_init         (HippoCanvasStyleClass     *klass);
static void hippo_canvas_style_dispose            (GObject                 *object);
static void hippo_canvas_style_finalize           (GObject                 *object);


static void hippo_canvas_style_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_style_get_property (GObject      *object,
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
    PROP_FONT,
    PROP_FONT_DESC,
    PROP_COLOR,
    PROP_COLOR_SET
};

struct _HippoCanvasStyle {
    GObject parent;

    guint32 color_rgba; 
    PangoFontDescription *font_desc;
    
    guint color_set : 1;
};

struct _HippoCanvasStyleClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoCanvasStyle, hippo_canvas_style, G_TYPE_OBJECT)

static void
hippo_canvas_style_init(HippoCanvasStyle *style)
{
    style->color_rgba = HIPPO_CANVAS_DEFAULT_COLOR;
}

static void
hippo_canvas_style_class_init(HippoCanvasStyleClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_style_set_property;
    object_class->get_property = hippo_canvas_style_get_property;

    object_class->dispose = hippo_canvas_style_dispose;
    object_class->finalize = hippo_canvas_style_finalize;


    g_object_class_install_property(object_class,
                                    PROP_COLOR,
                                    g_param_spec_uint("color",
                                                      _("Foreground Color"),
                                                      _("32-bit RGBA foreground color"),
                                                      0,
                                                      G_MAXUINT,
                                                      HIPPO_CANVAS_DEFAULT_COLOR,
                                                      G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(object_class,
                                    PROP_COLOR_SET,
                                    g_param_spec_boolean("color-set",
                                                         _("Foreground Color Set"),
                                                         _("Whether a foreground color was set"),
                                                         FALSE,
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
                                    g_param_spec_boxed("font-desc",
                                                       _("Font Description"),
                                                       _("Font description as a PangoFontDescription object"),
                                                       PANGO_TYPE_FONT_DESCRIPTION,
                                                       G_PARAM_READABLE | G_PARAM_WRITABLE));
}

static void
hippo_canvas_style_dispose(GObject *object)
{
    /* HippoCanvasStyle *style = HIPPO_CANVAS_STYLE(object); */

    G_OBJECT_CLASS(hippo_canvas_style_parent_class)->dispose(object);
}

static void
hippo_canvas_style_finalize(GObject *object)
{
    HippoCanvasStyle *style = HIPPO_CANVAS_STYLE(object);

    if (style->font_desc) {
        pango_font_description_free(style->font_desc);
        style->font_desc = NULL;
    }

    G_OBJECT_CLASS(hippo_canvas_style_parent_class)->finalize(object);
}

static int
parse_int32(const char *s)
{
    char *end;
    long v;

    end = NULL;
    v = strtol(s, &end, 10);

    if (end == NULL) {
        g_warning("Failed to parse '%s' as 32-bit integer", s);
        return 0;
    }

    return v;
}

/* Latest pango supports "NNpx" sizes, but FC5 Pango (1.12) does not */
static int
parse_absolute_size_hack(const char *s)
{
    const char *p;
    const char *number;

    p = strstr(s, "px");
    if (p == NULL)
        return -1;

    number = p;
    --number;
    while (number > s) {
        if (!g_ascii_isdigit(*number)) {
            ++number;
            break;
        }
        --number;
    }

    return parse_int32(number);
}

static void
hippo_canvas_style_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasStyle *style;

    style = HIPPO_CANVAS_STYLE(object);

    switch (prop_id) {
    case PROP_COLOR:
        style->color_rgba = g_value_get_uint(value);
        if (style->color_set != TRUE) {
            style->color_set = TRUE;
            g_object_notify(G_OBJECT(style), "color-set");
        }
        break;
    case PROP_COLOR_SET:
        style->color_set = g_value_get_boolean(value);
        break;
    case PROP_FONT:
        {
            const char *s;
            PangoFontDescription *desc;
            int absolute;
            s = g_value_get_string(value);
            if (s != NULL) {
                char *no_px = NULL;
                absolute = parse_absolute_size_hack(s);
                if (absolute >= 0) {
                    // get the "px" out of the string
                    GString *no_px_g = g_string_new(NULL);
                    const char *p;
                    p = strstr(s, "px");
                    g_assert(p != NULL);
                    g_string_append_len(no_px_g, s, p - s);
                    g_string_append_len(no_px_g, p + 2, strlen(p + 2));
                    no_px = g_string_free(no_px_g, FALSE);
                }
                desc = pango_font_description_from_string(no_px);
                g_free(no_px);
                if (desc == NULL) {
                    g_warning("Failed to parse font description string '%s'", s);
                } else {
                    if (absolute >= 0) {
                        pango_font_description_set_absolute_size(desc, absolute * PANGO_SCALE);
                    }

                    if ((pango_font_description_get_set_fields(desc) & PANGO_FONT_MASK_SIZE) != 0 &&
                        pango_font_description_get_size(desc) <= 0) {
                        g_warning("font size set to 0, not going to work well");
                    }
                }
            } else {
                desc = NULL;
            }

            
            g_object_set(object, "font-desc", desc, NULL);
            if (desc)
                pango_font_description_free(desc);
        }
        break;
    case PROP_FONT_DESC:
        {
            PangoFontDescription *desc = g_value_get_boxed(value);

            if (!(desc == NULL && style->font_desc == NULL)) {
                if (style->font_desc) {
                    pango_font_description_free(style->font_desc);
                    style->font_desc = NULL;
                }
                if (desc != NULL) {
                    style->font_desc = pango_font_description_copy(desc);
                }
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_style_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasStyle *style;

    style = HIPPO_CANVAS_STYLE (object);

    switch (prop_id) {
    case PROP_COLOR:
        g_value_set_uint(value, style->color_rgba);
        break;
    case PROP_COLOR_SET:
        g_value_set_boolean(value, style->color_set);
        break;
    case PROP_FONT:
        {
            char *s;
            if (style->font_desc)
                s = pango_font_description_to_string(style->font_desc);
            else
                s = NULL;
            g_value_take_string(value, s);
        }
        break;
    case PROP_FONT_DESC:
        g_value_set_boxed(value, style->font_desc);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

void
hippo_canvas_style_affect_color(HippoCanvasStyle     *style,
                                guint32              *color_rgba_p)
{
    if (!style->color_set)
        return;

    *color_rgba_p = style->color_rgba;
}

void
hippo_canvas_style_affect_font_desc(HippoCanvasStyle     *style,
                                    PangoFontDescription *font_desc)
{
    if (style->font_desc == NULL)
        return;

    pango_font_description_merge(font_desc,
                                 style->font_desc,
                                 TRUE); /* TRUE = overwrite anything in the target */
}
