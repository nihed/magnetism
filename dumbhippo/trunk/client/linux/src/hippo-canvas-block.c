/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#if 1
#include <config.h>
#include <glib/gi18n-lib.h>
#include "hippo-canvas.h"
#else
#include "hippo-common-internal.h"
#endif
#include "hippo-canvas-block.h"
#include "hippo-canvas-box.h"
#include "hippo-canvas-image.h"
#include "hippo-canvas-text.h"
#include "hippo-canvas-link.h"

static void      hippo_canvas_block_init                (HippoCanvasBlock       *block);
static void      hippo_canvas_block_class_init          (HippoCanvasBlockClass  *klass);
static void      hippo_canvas_block_iface_init          (HippoCanvasItemClass   *item_class);
static void      hippo_canvas_block_dispose             (GObject                *object);
static void      hippo_canvas_block_finalize            (GObject                *object);

static void hippo_canvas_block_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_canvas_block_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);


/* Canvas item methods */
static void     hippo_canvas_block_paint              (HippoCanvasItem *item,
                                                       cairo_t         *cr);

struct _HippoCanvasBlock {
    HippoCanvasBox box;
    HippoBlock *block;
    HippoCanvasItem *age_item;
};

struct _HippoCanvasBlockClass {
    HippoCanvasBoxClass parent_class;

};

enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

/* static int signals[LAST_SIGNAL]; */

enum {
    PROP_BLOCK,
    PROP_0
};

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlock, hippo_canvas_block, HIPPO_TYPE_CANVAS_BOX,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_iface_init));

static void
hippo_canvas_block_init(HippoCanvasBlock *block)
{
    HippoCanvasItem *item;
    HippoCanvasBox *box;
    HippoCanvasBox *left_column;
    HippoCanvasBox *right_column;


    HIPPO_CANVAS_BOX(block)->background_color_rgba = 0xffffffff;
    HIPPO_CANVAS_BOX(block)->padding_left = 4;
    HIPPO_CANVAS_BOX(block)->padding_right = 4;
    HIPPO_CANVAS_BOX(block)->padding_top = 4;
    HIPPO_CANVAS_BOX(block)->padding_bottom = 4;
    
    /* Create top bar */

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block),
                            HIPPO_CANVAS_ITEM(box), 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "Web Swarm",
                        "xalign", HIPPO_ALIGNMENT_START,
                        NULL);
    hippo_canvas_box_append(box, item, 0);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "[\\/]",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "[X]",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    /* Create left and right columns */

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block),
                            HIPPO_CANVAS_ITEM(box), HIPPO_PACK_EXPAND);

    left_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                               "orientation", HIPPO_ORIENTATION_VERTICAL,
                               "xalign", HIPPO_ALIGNMENT_FILL,
                               "yalign", HIPPO_ALIGNMENT_START,
                               NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(left_column), HIPPO_PACK_EXPAND);
    
    right_column = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                "orientation", HIPPO_ORIENTATION_VERTICAL,
                                "xalign", HIPPO_ALIGNMENT_END,                                
                                "yalign", HIPPO_ALIGNMENT_START,
                                "padding-left", 8,
                               NULL);
    hippo_canvas_box_append(box, HIPPO_CANVAS_ITEM(right_column), HIPPO_PACK_END);


    /* Fill in left column */
    
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                        "xalign", HIPPO_ALIGNMENT_START,
                        "yalign", HIPPO_ALIGNMENT_START,
                        "font", "Bold",
                        "text", "Recycling medical devices raises concerns",
                        NULL);
    hippo_canvas_box_append(left_column, item, 0);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "size-mode", HIPPO_CANVAS_SIZE_WRAP_WORD,
                        "xalign", HIPPO_ALIGNMENT_START,
                        "yalign", HIPPO_ALIGNMENT_START,
                        "font-scale", PANGO_SCALE_SMALL,
                        "text",
                        "Federal regulators say reprocessing is safe, but original "
                        "device manufacturers say they can't guarantee recycled "
                        "products will work correctly - and that they are wrongly blamed "
                        "for problems when they break.",
                        NULL);
    hippo_canvas_box_append(left_column, item, HIPPO_PACK_EXPAND);

    
    /* Fill in right column */


    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_END,
                       "yalign", HIPPO_ALIGNMENT_START,
                       NULL);
    hippo_canvas_box_append(right_column, HIPPO_CANVAS_ITEM(box), 0);


    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "text", "[photo]",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);
    
    item = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                        "font-scale", PANGO_SCALE_SMALL,
                        "font", "Italic",
                        "text", "LizardBoy",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "font-scale", PANGO_SCALE_SMALL,
                        "font", "Italic",
                        "text", "from ",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                       "xalign", HIPPO_ALIGNMENT_END,
                       "yalign", HIPPO_ALIGNMENT_START,
                       NULL);
    hippo_canvas_box_append(right_column, HIPPO_CANVAS_ITEM(box), 0);


    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "font-scale", PANGO_SCALE_SMALL,
                        "text", "56 views",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                        "font-scale", PANGO_SCALE_SMALL,
                        "text", " | ",
                        NULL);
    hippo_canvas_box_append(box, item, HIPPO_PACK_END);

    
    block->age_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                   "font-scale", PANGO_SCALE_SMALL,
                                   NULL);
    hippo_canvas_box_append(box, block->age_item, HIPPO_PACK_END);
}

static HippoCanvasItemClass *item_parent_class;

static void
hippo_canvas_block_iface_init(HippoCanvasItemClass *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);

    item_class->paint = hippo_canvas_block_paint;
}

static void
hippo_canvas_block_class_init(HippoCanvasBlockClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->set_property = hippo_canvas_block_set_property;
    object_class->get_property = hippo_canvas_block_get_property;

    object_class->dispose = hippo_canvas_block_dispose;
    object_class->finalize = hippo_canvas_block_finalize;

    g_object_class_install_property(object_class,
                                    PROP_BLOCK,
                                    g_param_spec_object("block",
                                                        _("Block"),
                                                        _("Block to display"),
                                                        HIPPO_TYPE_BLOCK,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE));        
}

static void
on_block_changed(HippoBlock *block,
                 void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    GTimeVal tv;
    char *when;

    if (block == NULL) /* should be impossible */
        return;
    
    g_get_current_time(&tv);

    when = hippo_format_time_ago(tv.tv_sec,
                                 (int) (hippo_block_get_timestamp(block) / 1000));

    g_object_set(G_OBJECT(canvas_block->age_item),
                 "text", when,
                 NULL);

    g_free(when);

    /* FIXME update all the other info */
}

static void
set_block(HippoCanvasBlock *canvas_block,
          HippoBlock       *new_block)
{
    if (new_block != canvas_block->block) {
        if (new_block) {
            g_object_ref(new_block);
            g_signal_connect(G_OBJECT(new_block), "changed",
                             G_CALLBACK(on_block_changed), canvas_block);
        }
        if (canvas_block->block) {
            g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                                 G_CALLBACK(on_block_changed), canvas_block);
            g_object_unref(canvas_block->block);
        }
        canvas_block->block = new_block;

        on_block_changed(new_block, canvas_block);
        
        g_object_notify(G_OBJECT(canvas_block), "block");
        hippo_canvas_item_emit_request_changed(HIPPO_CANVAS_ITEM(canvas_block));
    }
}

static void
hippo_canvas_block_dispose(GObject *object)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);

    set_block(block, NULL);

    block->age_item = NULL;
    
    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->dispose(object);
}

static void
hippo_canvas_block_finalize(GObject *object)
{
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object); */

    G_OBJECT_CLASS(hippo_canvas_block_parent_class)->finalize(object);
}

HippoCanvasItem*
hippo_canvas_block_new(void)
{
    HippoCanvasBlock *block = g_object_new(HIPPO_TYPE_CANVAS_BLOCK, NULL);


    return HIPPO_CANVAS_ITEM(block);
}

static void
hippo_canvas_block_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK(object);

    switch (prop_id) {
    case PROP_BLOCK:
        {
            HippoBlock *new_block = (HippoBlock*) g_value_get_object(value);
            set_block(block, new_block);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoCanvasBlock *block;

    block = HIPPO_CANVAS_BLOCK (object);

    switch (prop_id) {
    case PROP_BLOCK:
        g_value_set_object(value, G_OBJECT(block->block));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_paint(HippoCanvasItem *item,
                         cairo_t         *cr)
{
    /* HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(item); */

    /* Draw the background and any children */
    item_parent_class->paint(item, cr);
}

void
hippo_canvas_block_set_block(HippoCanvasBlock *canvas_block,
                             HippoBlock       *block)
{
    g_object_set(G_OBJECT(canvas_block), "block", block, NULL);
}
