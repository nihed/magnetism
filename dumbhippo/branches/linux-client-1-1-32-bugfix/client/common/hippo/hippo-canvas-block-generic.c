/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-generic.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-gradient.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_block_generic_init                (HippoCanvasBlockGeneric       *block);
static void      hippo_canvas_block_generic_class_init          (HippoCanvasBlockGenericClass  *klass);
static void      hippo_canvas_block_generic_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_generic_dispose             (GObject                *object);
static void      hippo_canvas_block_generic_finalize            (GObject                *object);

static void hippo_canvas_block_generic_set_property (GObject      *object,
                                                     guint         prop_id,
                                                     const GValue *value,
                                                     GParamSpec   *pspec);
static void hippo_canvas_block_generic_get_property (GObject      *object,
                                                     guint         prop_id,
                                                     GValue       *value,
                                                     GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_generic_append_content_items(HippoCanvasBlock *block,
                                                            HippoCanvasBox   *parent_box);

static void hippo_canvas_block_generic_set_block       (HippoCanvasBlock *canvas_block,
                                                        HippoBlock       *block);
static void hippo_canvas_block_generic_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_generic_clicked_count_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_generic_significant_clicked_count_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_generic_stack_reason_changed (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_generic_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_generic_unexpand (HippoCanvasBlock *canvas_block);


/* Our own methods */
static void hippo_canvas_block_generic_update_visibility    (HippoCanvasBlockGeneric *block_generic);


struct _HippoCanvasBlockGeneric {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *description_parent;
    HippoCanvasItem *description_item;
    HippoCanvasItem *reason_item;
    HippoCanvasItem *clicked_count_item;
    HippoCanvasBox *parent_box;
    HippoCanvasItem *details_box;
};

struct _HippoCanvasBlockGenericClass {
    HippoCanvasBlockClass parent_class;

};

#if 0
enum {
    NO_SIGNALS_YET,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

enum {
    PROP_0
};
#endif

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockGeneric, hippo_canvas_block_generic, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_generic_iface_init));

static void
hippo_canvas_block_generic_init(HippoCanvasBlockGeneric *block_generic)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_generic);

    block->required_type = HIPPO_BLOCK_TYPE_GENERIC;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_generic_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_generic_class_init(HippoCanvasBlockGenericClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_generic_set_property;
    object_class->get_property = hippo_canvas_block_generic_get_property;

    object_class->dispose = hippo_canvas_block_generic_dispose;
    object_class->finalize = hippo_canvas_block_generic_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_generic_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_generic_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_generic_title_activated;
    canvas_block_class->clicked_count_changed = hippo_canvas_block_generic_clicked_count_changed;
    canvas_block_class->significant_clicked_count_changed = hippo_canvas_block_generic_significant_clicked_count_changed;
    canvas_block_class->stack_reason_changed = hippo_canvas_block_generic_stack_reason_changed;
    canvas_block_class->expand = hippo_canvas_block_generic_expand;
    canvas_block_class->unexpand = hippo_canvas_block_generic_unexpand;
}

static void
hippo_canvas_block_generic_dispose(GObject *object)
{
    /* HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(object); */

    G_OBJECT_CLASS(hippo_canvas_block_generic_parent_class)->dispose(object);
}

static void
hippo_canvas_block_generic_finalize(GObject *object)
{
    /* HippoCanvasBlockGeneric *block = HIPPO_CANVAS_BLOCK_GENERIC(object); */

    G_OBJECT_CLASS(hippo_canvas_block_generic_parent_class)->finalize(object);
}

static void
hippo_canvas_block_generic_set_property(GObject         *object,
                                        guint            prop_id,
                                        const GValue    *value,
                                        GParamSpec      *pspec)
{
    HippoCanvasBlockGeneric *block_generic;

    block_generic = HIPPO_CANVAS_BLOCK_GENERIC(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_generic_get_property(GObject         *object,
                                        guint            prop_id,
                                        GValue          *value,
                                        GParamSpec      *pspec)
{
    HippoCanvasBlockGeneric *block_generic;

    block_generic = HIPPO_CANVAS_BLOCK_GENERIC (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_generic_append_content_items(HippoCanvasBlock *block,
                                                HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(block);

    block_generic->reason_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                              "xalign", HIPPO_ALIGNMENT_START,
                                              "text", NULL,
                                              "font", "Italic 11px",
                                              NULL);
    hippo_canvas_box_append(parent_box, block_generic->reason_item, 0);
    hippo_canvas_box_set_child_visible(parent_box,
                                       block_generic->reason_item,
                                       FALSE);

    block_generic->description_parent = parent_box;
    block_generic->description_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                   "size-mode", HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                                                   "xalign", HIPPO_ALIGNMENT_START,
                                                   "yalign", HIPPO_ALIGNMENT_START,
                                                   "text", NULL,
                                                   "border-top", 4,
                                                   "border-bottom", 4,
                                                   NULL);
    hippo_canvas_box_append(parent_box, block_generic->description_item, 0);

    block_generic->details_box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                                              "orientation", HIPPO_ORIENTATION_HORIZONTAL,
                                              "color", HIPPO_CANVAS_BLOCK_GRAY_TEXT_COLOR,
                                              NULL);
    hippo_canvas_box_append(parent_box, block_generic->details_box, HIPPO_PACK_CLEAR_RIGHT);
    block_generic->parent_box = parent_box;
    hippo_canvas_box_set_child_visible(block_generic->parent_box,
                                       block_generic->details_box,
                                       FALSE); /* not expanded at first */

    block_generic->clicked_count_item = g_object_new(HIPPO_TYPE_CANVAS_TEXT,
                                                     "text", NULL,
                                                     NULL);
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(block_generic->details_box), block_generic->clicked_count_item, 0);
}

static void
hippo_canvas_block_generic_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    char *link;

    if (canvas_block->block == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    link = NULL;
    g_object_get(G_OBJECT(canvas_block->block), "link", &link, NULL);

    if (link != NULL && actions != NULL) {
        hippo_actions_open_url(actions, link);
    }

    g_free(link);
}

static void
hippo_canvas_block_generic_clicked_count_changed (HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGeneric *canvas_block_generic = HIPPO_CANVAS_BLOCK_GENERIC(canvas_block);
    char *s;
    int count;

    count = hippo_block_get_clicked_count(canvas_block->block);

    if (count > 0) {
        s = g_strdup_printf(_("%d views"), count);
    } else {
        s = NULL;
    }

    g_object_set(G_OBJECT(canvas_block_generic->clicked_count_item),
                 "text", s,
                 NULL);
    g_free(s);
}

static void
block_generic_update_reason_item(HippoCanvasBlockGeneric *canvas_block_generic)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(canvas_block_generic);
    HippoStackReason stack_reason = hippo_block_get_stack_reason(canvas_block->block);

    switch (stack_reason) {
    case HIPPO_STACK_VIEWER_COUNT:
        {
            int significant_count = hippo_block_get_significant_clicked_count(canvas_block->block);
            char *s;
            if (significant_count == 1)
                s = g_strdup(_("1 person has now viewed this."));
            else
                s = g_strdup_printf(_("%d people have now viewed this."), significant_count);
            g_object_set(G_OBJECT(canvas_block_generic->reason_item),
                         "text", s,
                         NULL);
            g_free(s);
            break;
        }
    default:
        break;
    }

    hippo_canvas_block_generic_update_visibility(canvas_block_generic);
}

static void
hippo_canvas_block_generic_significant_clicked_count_changed(HippoCanvasBlock *canvas_block)
{
    block_generic_update_reason_item(HIPPO_CANVAS_BLOCK_GENERIC(canvas_block));
}

static void
hippo_canvas_block_generic_stack_reason_changed(HippoCanvasBlock *canvas_block)
{
    block_generic_update_reason_item(HIPPO_CANVAS_BLOCK_GENERIC(canvas_block));
}

static void
hippo_canvas_block_generic_update_visibility(HippoCanvasBlockGeneric *block_generic)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_generic);
    HippoStackReason stack_reason;
    gboolean show_description;
    gboolean show_reason;

    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    /* the details box shows iff. we are expanded
     */
    hippo_canvas_box_set_child_visible(block_generic->parent_box,
                                       block_generic->details_box,
                                       canvas_block->expanded);

    /* The description is always visible when expanded, otherwise we sometimes
     * show a single line summary
     */
    g_object_set(G_OBJECT(block_generic->description_item),
                 "size-mode", canvas_block->expanded ? HIPPO_CANVAS_SIZE_WRAP_WORD : HIPPO_CANVAS_SIZE_ELLIPSIZE_END,
                 NULL);

    /* When the block is expanded, we want to give the description the full width,
     * bumping it down below the sender information on the right if necessary
     */
    hippo_canvas_box_set_child_packing (block_generic->description_parent,
                                        block_generic->description_item,
                                        canvas_block->expanded ? HIPPO_PACK_CLEAR_RIGHT : 0);

    show_reason = stack_reason == HIPPO_STACK_VIEWER_COUNT;
    show_description = canvas_block->expanded || !show_reason;

    hippo_canvas_box_set_child_visible(block_generic->parent_box,
                                       block_generic->description_item,
                                       show_description);
    hippo_canvas_box_set_child_visible(block_generic->parent_box,
                                       block_generic->reason_item,
                                       show_reason);
}

static void
hippo_canvas_block_generic_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_generic_parent_class)->expand(canvas_block);

    hippo_canvas_block_generic_update_visibility(block_generic);
}

static void
hippo_canvas_block_generic_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_generic_parent_class)->unexpand(canvas_block);

    hippo_canvas_block_generic_update_visibility(block_generic);
}

static void
on_block_title_changed(HippoBlock *block,
                       GParamSpec *arg, /* null when first calling this */
                       void       *data)
{
    HippoCanvasBlock *canvas_block;
    char *title;
    char *link;

    canvas_block = HIPPO_CANVAS_BLOCK(data);
    
    title = NULL;
    link = NULL;
    g_object_get(G_OBJECT(block),
                 "title", &title,
                 "link", &link,
                 NULL);

    hippo_canvas_block_set_title(canvas_block, title, link, FALSE);
    
    g_free(title);
    g_free(link);
}

static void
on_block_description_changed(HippoBlock *block,
                             GParamSpec *arg, /* null when first calling this */
                             void       *data)
{
    HippoCanvasBlock *canvas_block;
    HippoCanvasBlockGeneric *block_generic;
    char *description;

    canvas_block = HIPPO_CANVAS_BLOCK(data);
    block_generic = HIPPO_CANVAS_BLOCK_GENERIC(data);
    
    description = NULL;
    g_object_get(G_OBJECT(block), "description", &description, NULL);

    if (description == NULL) {
        hippo_canvas_block_set_expanded(canvas_block, FALSE);
    }
    canvas_block->expandable = description != NULL;

    g_object_set(G_OBJECT(block_generic->description_item),
                 "text", description,
                 NULL);

    g_free(description);
}

static void
on_block_source_changed(HippoBlock *block,
                        GParamSpec *arg, /* null when first calling this */
                        void       *data)
{
    HippoCanvasBlock *canvas_block;
    HippoEntity *source;

    canvas_block = HIPPO_CANVAS_BLOCK(data);
    
    source = NULL;
    g_object_get(G_OBJECT(block), "source", &source, NULL);

    hippo_canvas_block_set_sender(canvas_block,
                                  source ? hippo_entity_get_guid(source) : NULL);
    
    if (source)
        g_object_unref(source);
}

static void
hippo_canvas_block_generic_set_block(HippoCanvasBlock *canvas_block,
                                     HippoBlock       *block)
{
    
    if (block == canvas_block->block)
        return;
    
    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_title_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_description_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_source_changed),
                                             canvas_block);
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_generic_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title",
                         G_CALLBACK(on_block_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::description",
                         G_CALLBACK(on_block_description_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::source",
                         G_CALLBACK(on_block_source_changed),
                         canvas_block);

        on_block_title_changed(canvas_block->block, NULL, canvas_block);
        on_block_description_changed(canvas_block->block, NULL, canvas_block);
        on_block_source_changed(canvas_block->block, NULL, canvas_block);
    }
}
