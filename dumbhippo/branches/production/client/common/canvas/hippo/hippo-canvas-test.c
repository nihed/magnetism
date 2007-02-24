/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include "hippo-canvas-test.h"
#include "hippo-canvas-internal.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-image.h>

typedef struct {
    int width;
    int height;
    guint32 color;
    HippoPackFlags flags;
    HippoItemAlignment alignment;
} BoxAttrs;

static BoxAttrs single_start[] = { { 40, 80, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_end[] = { { 100, 60, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_start[] = { { 50, 90, 0x0000ffff, 0, HIPPO_ALIGNMENT_FILL },
                                   { 50, 90, 0xff000099, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs double_end[] = { { 45, 55, 0x00ff00ff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
                                 { 45, 55, 0x00ff0077, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs single_expand_end[] = { { 100, 60, 0x0000ffff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL }, { 0, 0, 0, 0 } };
static BoxAttrs everything[] = {
    { 120, 50, 0x00ccccff, 0, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND | HIPPO_PACK_END, HIPPO_ALIGNMENT_FILL },
    { 0, 0, 0, 0 }
};
static BoxAttrs alignments[] = {
    { 120, 50, 0x00ffcccc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_FILL },
    { 120, 50, 0x00ccffcc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_START },
    { 120, 50, 0x00cffffc, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_CENTER },
    { 120, 50, 0x00ccccff, HIPPO_PACK_EXPAND, HIPPO_ALIGNMENT_END },
    { 0, 0, 0, 0 }
};

static BoxAttrs* box_rows[] = { single_start, /* double_start,*/ single_end, /* double_end, */
                                single_expand, everything, alignments };

static HippoCanvasItem*
create_row(BoxAttrs *boxes)
{
    HippoCanvasItem *row;
    int i;
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);

    for (i = 0; boxes[i].width > 0; ++i) {
        BoxAttrs *attrs = &boxes[i];
        HippoCanvasItem *shape;
        HippoCanvasItem *label;
        const char *flags_text;
        const char *align_text;
        char *s;
        
        shape = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                             /* "width", attrs->width,
                                "height", attrs->height, */
                             /* "color", attrs->color, */
                             "background-color", 0xffffffff,
                             "xalign", attrs->alignment,
                             NULL);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), shape, attrs->flags);
        
        if (attrs->flags == (HIPPO_PACK_END | HIPPO_PACK_EXPAND))
            flags_text = "END | EXPAND";
        else if (attrs->flags == HIPPO_PACK_END)
            flags_text = "END";
        else if (attrs->flags == HIPPO_PACK_EXPAND)
            flags_text = "EXPAND";
        else
            flags_text = "0";

        switch (attrs->alignment) {
        case HIPPO_ALIGNMENT_FILL:
            align_text = "FILL";
            break;
        case HIPPO_ALIGNMENT_START:
            align_text = "START";
            break;
        case HIPPO_ALIGNMENT_END:
            align_text = "END";
            break;
        case HIPPO_ALIGNMENT_CENTER:
            align_text = "CENTER";
            break;
        default:
            align_text = NULL;
            break;
        }

        s = g_strdup_printf("%s, %s", flags_text, align_text);
        label = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                             "text", s,
                             "background-color", 0x0000ff00,
                             NULL);
        g_free(s);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape), label, HIPPO_PACK_EXPAND);
    }
    return row;
}

static void
change_text_on_hovering(HippoCanvasItem *item,
                        gboolean hovering,
                        void *data)
{
    g_object_set(G_OBJECT(item), "text",
                 hovering ? "Hovering!" : "... not hovering ... looooooooooooooooooooooooooong",
                 NULL);
}

static HippoCanvasItem*
create_test_box_layout_root(void)
{
    HippoCanvasItem *box;
    HippoCanvasItem *text;
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "border", 4,
                       "border-color", 0x000000ff,
                       "padding", 10,
                       "background-color", 0xffffffff,
                       "spacing", 10,
                       NULL);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "text", "expand=0 ellipsize=1",
                        "background-color", 0xffaaaaff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "text", "expand=1 ellipsize=1",
                        "background-color", 0xaaffaaff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_EXPAND);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "expand=0 ellipsize=0",
                        "background-color", 0xaaaaffff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "expand=1 ellipsize=0",
                        "background-color", 0xaaffffff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_EXPAND);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "text", "short ex=0 el=1",
                        "background-color", 0xffffaaff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "expand=1 if_fits=1",
                        "background-color", 0xaaffaaff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_EXPAND | HIPPO_PACK_IF_FITS);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "expand=0 if_fits=1",
                        "background-color", 0xffaaffaa,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_IF_FITS);
    
    return box;
}

static HippoCanvasItem*
create_floated_box_layout_root(void)
{
    HippoCanvasBox *box;
    HippoCanvasItem *text;

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_VERTICAL,
                       "border", 4,
                       "border-color", 0x000000ff,
                       "padding", 10,
                       "background-color", 0xffffffff,
                       "spacing", 5,
                       NULL);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "LEFT\nFLOAT",
                        "background-color", 0xaa0000ff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_LEFT);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "WIDER LEFT\nFLOAT",
                        "background-color", 0xaa0000ff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_LEFT);

    text = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "background-color", 0x00aa00ff,
                        "border-color", 0x000000ff,
                        "box-width", 5,
                        "box-height", 5,                        
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_RIGHT);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "RIGHT",
                        "background-color", 0x00aa00ff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_RIGHT);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "WIDER RIGHT",
                        "background-color", 0x00aa00ff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_RIGHT);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "This is a normally flowed item",
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "background-color", 0x4444ffff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "Two\nLines",
                        "background-color", 0x4444ffff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                           "This item is set to clear both left and right floats, so it "
                           "will appear beneath them. It also has a lot of text in it "
                           "to force it to wrap",
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "background-color", 0x4444ffff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_CLEAR_BOTH);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "After everything so below",
                        "background-color", 0x00aa00ff,
                        "border-color", 0x000000ff,
                        "border", 1,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, HIPPO_PACK_FLOAT_RIGHT);
    
    return HIPPO_CANVAS_ITEM(box);
}

static void
on_item_activated(HippoCanvasItem *item)
{
    g_object_set(G_OBJECT(item), "text", "Foo", NULL);
}

static HippoCanvasItem*
create_test_click_release_root(void)
{
    HippoCanvasItem *box;
    HippoCanvasItem *text;
    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "border", 4,
                       "border-color", 0x000000ff,
                       "padding", 10,
                       "background-color", 0xffffffff,
                       "spacing", 10,
                       NULL);

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "text", "Click Me!",
                        "background-color", 0xffaaaaff,                        
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    g_signal_connect(G_OBJECT(text), "activated", G_CALLBACK(on_item_activated), NULL);
    
    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "text", "Click Me!",
                        "background-color", 0x00aaffff,
                        NULL);
    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(box), text, 0);

    g_signal_connect(G_OBJECT(text), "activated", G_CALLBACK(on_item_activated), NULL);

    return box;
}

HippoCanvasItem*
hippo_canvas_test_get_root(void)
{
    HippoCanvasItem *root;
    HippoCanvasItem *shape2;
    HippoCanvasItem *text;
#if 0
    HippoCanvasItem *image;
    HippoCanvasItem *row;
    HippoCanvasItem *shape1;
    int i;

    root = g_object_new(HIPPO_TYPE_CANVAS_STACK,
                        "box-width", 400,
                        "spacing", 8,
                        NULL);

    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
    row = g_object_new(HIPPO_TYPE_CANVAS_BLOCK,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            row, 0);
    
#if 0
#if 1
    shape1 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0xaeaeaeff,
                          "padding", 20,
                          NULL);

    shape2 = g_object_new(HIPPO_TYPE_CANVAS_SHAPE,
                          "width", 50, "height", 30,
                          "color", 0x00ff00ff,
                          "padding", 10,
                          NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape1, 0);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root),
                            shape2, 0);
#endif
    
    for (i = 0; i < (int) G_N_ELEMENTS(box_rows); ++i) {
        row = create_row(box_rows[i]);
        hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, 0);
    }

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Click here",
                        "background-color", 0x990000ff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);

    row = g_object_new(HIPPO_TYPE_CANVAS_BOX, "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "spacing", 5, NULL);    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), row, HIPPO_PACK_EXPAND);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "chaticon",
                         "xalign", HIPPO_ALIGNMENT_START,
                         "yalign", HIPPO_ALIGNMENT_END,
                         "background-color", 0xffff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, 0);

    image = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                         "image-name", "ignoreicon",
                         "xalign", HIPPO_ALIGNMENT_FILL,
                         "yalign", HIPPO_ALIGNMENT_FILL,
                         "background-color", 0x00ff00ff,
                         NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(row), image, HIPPO_PACK_EXPAND);

    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "text",
                        "This is some long text that may help in testing resize behavior. It goes "
                        "on for a while, so don't get impatient. More and more and  more text. "
                        "Text text text. Lorem ipsum! Text! This is the story of text.",
                        "background-color", 0x0000ff00,
                        "yalign", HIPPO_ALIGNMENT_END,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_EXPAND);
#endif
#endif

    root = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                        "orientation", HIPPO_ORIENTATION_VERTICAL,
                        "border", 15,
                        "border-color", 0xff0000ff,
                        "padding", 25,
                        "background-color", 0x00ff00ff,
                        NULL);

#if 0
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                        "Click here",
                        "color", 0xffffffff,
                        "background-color", 0x0000ffff,
                        NULL);
#else
    text = g_object_new(HIPPO_TYPE_CANVAS_IMAGE,
                        "image-name", "nophoto",
                        "background-color", 0x0000ffff,
                        "xalign", HIPPO_ALIGNMENT_CENTER,
                        "yalign", HIPPO_ALIGNMENT_CENTER,
                        NULL);
#endif
#if 0
    {
        GtkWidget *widget = gtk_label_new("FOOO! GtkLabel");
        gtk_widget_show(widget);
        shape1 = g_object_new(HIPPO_TYPE_CANVAS_WIDGET,
                            "widget", widget,
                            "background-color", 0x0000ffff,
                            "xalign", HIPPO_ALIGNMENT_CENTER,
                            "yalign", HIPPO_ALIGNMENT_CENTER,
                            NULL);
    }
                        
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape1, HIPPO_PACK_EXPAND);

    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Fixed link inside label item",
                        "background-color", 0xffffffff,
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape1), text, HIPPO_PACK_FIXED);

    hippo_canvas_box_move(HIPPO_CANVAS_BOX(shape1), text, 50, 50);

#endif
#if 0
    /* Item that changes on hover */
    
    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text",
                        "Text item packed end",
                        "color", 0xffffffff,
                        "background-color", 0x0000ffff,
                        NULL);
    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_END);
    
    g_signal_connect(G_OBJECT(text), "hovering-changed",
                     G_CALLBACK(change_text_on_hovering), NULL);
#endif

#if 0    
    /* Fixed items */
    
    text = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "text",
                        "Fixed position link item",
                        "background-color", 0xffffffff,
                        NULL);

    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), text, HIPPO_PACK_FIXED);

    hippo_canvas_box_move(HIPPO_CANVAS_BOX(root), text,
                          HIPPO_GRAVITY_NORTH_WEST,
                          150, 150);
#endif

#if 0
    /* For get_natural_width testing */

    shape2 = create_test_box_layout_root();
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape2,
                            HIPPO_PACK_END | HIPPO_PACK_EXPAND);
#endif

#if 0
    /* For float testing */

    shape2 = create_floated_box_layout_root();
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape2,
                            HIPPO_PACK_EXPAND);
#endif

    shape2 = create_test_click_release_root();
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape2,
                            HIPPO_PACK_EXPAND);    
    
#if 0
    /* A box with nothing expandable in it */

    shape2 = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                          "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                          NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(root), shape2, HIPPO_PACK_END);

    text = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "No expand/ellipse",
                        "background-color", 0xaaaaaaff,
                        NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(shape2), text, 0);
#endif

    return root;
}
