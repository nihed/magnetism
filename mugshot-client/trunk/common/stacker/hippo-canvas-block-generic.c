/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-stacker-internal.h"
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-generic.h"
#include "hippo-canvas-thumbnails.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-quipper.h"
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

static void hippo_canvas_block_generic_significant_clicked_count_changed (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_generic_stack_reason_changed (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_generic_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_generic_unexpand (HippoCanvasBlock *canvas_block);




/* Our own methods */
static void hippo_canvas_block_generic_update_visibility (HippoCanvasBlockGeneric *block_generic);
static void hippo_canvas_block_generic_update_expandable (HippoCanvasBlockGeneric *block_generic);


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
on_has_thumbnails_changed(HippoCanvasItem         *thumbnails_item,
                          GParamSpec              *pspec,
                          HippoCanvasBlockGeneric *block_generic)
{
    gboolean has_thumbnails;

    g_object_get(thumbnails_item, "has-thumbnails", &has_thumbnails, NULL);

    if (has_thumbnails != block_generic->have_thumbnails) {
        block_generic->have_thumbnails = has_thumbnails;
        hippo_canvas_block_generic_update_visibility(block_generic);
        hippo_canvas_block_generic_update_expandable(block_generic);
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
    hippo_canvas_item_set_visible(block_generic->reason_item,
                                  FALSE);
    
    block_generic->expand_tip = g_object_new(HIPPO_TYPE_CANVAS_LINK,
                                             "text", _("View thumbnails"),
                                             "xalign", HIPPO_ALIGNMENT_START,
                                             NULL);
    HIPPO_CANVAS_BOX(block_generic->expand_tip)->clickable = FALSE;
    hippo_canvas_box_append(parent_box,
                            block_generic->expand_tip, 0);
    hippo_canvas_item_set_visible(block_generic->expand_tip,
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

    block_generic->last_message_preview = g_object_new(HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW,
                                                       "actions", hippo_canvas_block_get_actions(block),
                                                       NULL);
    hippo_canvas_box_append(parent_box, block_generic->last_message_preview, 0);
    hippo_canvas_item_set_visible(block_generic->last_message_preview,
                                  FALSE); /* no messages yet */

    block_generic->thumbnails_item = g_object_new(HIPPO_TYPE_CANVAS_THUMBNAILS,
                                                  "actions", block->actions,
                                                  NULL);
    g_signal_connect(block_generic->thumbnails_item, "notify::has-thumbnails",
                     G_CALLBACK(on_has_thumbnails_changed), block_generic);
    
    hippo_canvas_box_append(HIPPO_CANVAS_BOX(parent_box),
                            block_generic->thumbnails_item, 0);

    block_generic->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                          "actions", hippo_canvas_block_get_actions(block),
                                          "padding-top", 8,
                                          NULL);
    hippo_canvas_box_append(parent_box, block_generic->quipper, 0);
    hippo_canvas_item_set_visible(block_generic->quipper,
                                  FALSE); /* not expanded */

    block_generic->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                               "actions", hippo_canvas_block_get_actions(block),
                                               NULL);
    hippo_canvas_box_append(parent_box,
                            block_generic->chat_preview, 0);
    hippo_canvas_item_set_visible(block_generic->chat_preview,
                                  FALSE); /* not expanded at first */
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
    g_object_get(G_OBJECT(canvas_block->block), "title-link", &link, NULL);

    if (link != NULL && actions != NULL) {
        hippo_actions_open_url(actions, link);
    }

    g_free(link);
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
    gboolean show_single_message;
    gboolean show_expand_tip;
    gboolean have_chat_id;

    if (canvas_block->block)
        stack_reason = hippo_block_get_stack_reason(canvas_block->block);
    else
        stack_reason = HIPPO_STACK_BLOCK_UPDATE;

    have_chat_id = FALSE;
    if (canvas_block->block)
        have_chat_id = hippo_block_get_chat_id(canvas_block->block) != NULL;

    /* Things that show only when we are expanded
     */
    hippo_canvas_item_set_visible(block_generic->quipper,
                                  canvas_block->expanded && have_chat_id);

    hippo_canvas_item_set_visible(block_generic->chat_preview,
                                  canvas_block->expanded && have_chat_id);

    hippo_canvas_item_set_visible(block_generic->thumbnails_item,
                                  canvas_block->expanded && block_generic->have_thumbnails);
    
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

    show_single_message = !canvas_block->expanded && stack_reason == HIPPO_STACK_CHAT_MESSAGE;
    show_reason = stack_reason == HIPPO_STACK_VIEWER_COUNT;
    show_description = (canvas_block->expanded || (!show_single_message && !show_reason)) && block_generic->have_description;
    show_expand_tip = !canvas_block->expanded && !show_single_message && !show_reason && !show_description && block_generic->have_thumbnails;

    hippo_canvas_item_set_visible(block_generic->description_item,
                                  show_description);
    hippo_canvas_item_set_visible(block_generic->reason_item,
                                  show_reason);
    hippo_canvas_item_set_visible(block_generic->last_message_preview,
                                  show_single_message);
    hippo_canvas_item_set_visible(block_generic->expand_tip,
                                  show_expand_tip);
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
hippo_canvas_block_generic_update_expandable(HippoCanvasBlockGeneric *block_generic)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_generic);
    
    gboolean have_chat_id;

    have_chat_id = FALSE;
    if (canvas_block->block)
        have_chat_id = hippo_block_get_chat_id(canvas_block->block) != NULL;

    canvas_block->expandable = block_generic->have_description ||
        block_generic->have_thumbnails ||
        have_chat_id;

    if (!canvas_block->expandable) {
        hippo_canvas_block_set_expanded(canvas_block, FALSE);
    }
}
    
static void
on_block_title_changed(HippoBlock *block,
                       GParamSpec *arg, /* null when first calling this */
                       void       *data)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(data);
    HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(canvas_block);
    
    char *title;
    char *link;

    title = NULL;
    link = NULL;
    g_object_get(G_OBJECT(block),
                 "title", &title,
                 "title-link", &link,
                 NULL);

    hippo_canvas_block_set_title(canvas_block, title, link, FALSE);
    g_object_set(G_OBJECT(block_generic->quipper),
                 "title", title,
                 NULL);
    
    g_free(title);
    g_free(link);
}

static void
on_block_description_changed(HippoBlock *block,
                             GParamSpec *arg, /* null when first calling this */
                             void       *data)
{
    HippoCanvasBlockGeneric *block_generic;
    char *description;

    block_generic = HIPPO_CANVAS_BLOCK_GENERIC(data);
    
    description = NULL;
    g_object_get(G_OBJECT(block), "description", &description, NULL);

    g_object_set(G_OBJECT(block_generic->description_item),
                 "text", description,
                 NULL);

    g_free(description);

    block_generic->have_description = description != NULL;
    
    hippo_canvas_block_generic_update_expandable(block_generic);
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

    hippo_canvas_block_set_sender(canvas_block, source);
    
    if (source)
        g_object_unref(source);
}

static void
on_block_chat_id_changed(HippoBlock *block,
                         GParamSpec *arg, /* null when first calling this */
                         void       *data)
{
    HippoCanvasBlockGeneric *canvas_block_generic = HIPPO_CANVAS_BLOCK_GENERIC(data);
    
    hippo_canvas_block_generic_update_visibility(canvas_block_generic);
    hippo_canvas_block_generic_update_expandable(canvas_block_generic);
}

static void
hippo_canvas_block_generic_set_block(HippoCanvasBlock *canvas_block,
                                     HippoBlock       *block)
{
    HippoCanvasBlockGeneric *block_generic = HIPPO_CANVAS_BLOCK_GENERIC(canvas_block);
    
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
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_block_chat_id_changed),
                                             canvas_block);
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_generic_parent_class)->set_block(canvas_block, block);

    g_object_set(block_generic->quipper,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_generic->last_message_preview,
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    g_object_set(block_generic->chat_preview,
                 "block", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);

    g_object_set(block_generic->thumbnails_item,
                 "resource", canvas_block->block ? hippo_block_get_resource(canvas_block->block) : NULL,
                 NULL);
    
    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title",
                         G_CALLBACK(on_block_title_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::title-link",
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
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::chat-id",
                         G_CALLBACK(on_block_chat_id_changed),
                         canvas_block);
        
        on_block_title_changed(canvas_block->block, NULL, canvas_block);
        on_block_description_changed(canvas_block->block, NULL, canvas_block);
        on_block_source_changed(canvas_block->block, NULL, canvas_block);
        on_block_chat_id_changed(canvas_block->block, NULL, canvas_block);
    }
}
