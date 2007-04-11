/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-group.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-group-chat.h"
#include "hippo-canvas-chat-preview.h"
#include "hippo-canvas-last-message-preview.h"
#include "hippo-canvas-quipper.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-gradient.h>
#include <hippo/hippo-canvas-link.h>

static void      hippo_canvas_block_group_chat_init                (HippoCanvasBlockGroupChat       *block);
static void      hippo_canvas_block_group_chat_class_init          (HippoCanvasBlockGroupChatClass  *klass);
static void      hippo_canvas_block_group_chat_iface_init          (HippoCanvasItemIface   *item_class);
static void      hippo_canvas_block_group_chat_dispose             (GObject                *object);
static void      hippo_canvas_block_group_chat_finalize            (GObject                *object);

static void hippo_canvas_block_group_chat_set_property (GObject      *object,
                                                        guint         prop_id,
                                                        const GValue *value,
                                                        GParamSpec   *pspec);
static void hippo_canvas_block_group_chat_get_property (GObject      *object,
                                                        guint         prop_id,
                                                        GValue       *value,
                                                        GParamSpec   *pspec);

/* Canvas block methods */
static void hippo_canvas_block_group_chat_append_content_items (HippoCanvasBlock *canvas_block,
                                                                HippoCanvasBox   *parent_box);
static void hippo_canvas_block_group_chat_set_block       (HippoCanvasBlock *canvas_block,
                                                           HippoBlock       *block);

static void hippo_canvas_block_group_chat_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_group_chat_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_chat_unexpand (HippoCanvasBlock *canvas_block);

/* Our methods */
static void hippo_canvas_block_group_update_visibility(HippoCanvasBlockGroupChat *block_group_chat);

struct _HippoCanvasBlockGroupChat {
    HippoCanvasBlock canvas_block;
    HippoCanvasItem *quipper;
    HippoCanvasItem *last_message_preview;
    HippoCanvasItem *chat_preview;
};

struct _HippoCanvasBlockGroupChatClass {
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

G_DEFINE_TYPE_WITH_CODE(HippoCanvasBlockGroupChat, hippo_canvas_block_group_chat, HIPPO_TYPE_CANVAS_BLOCK,
                        G_IMPLEMENT_INTERFACE(HIPPO_TYPE_CANVAS_ITEM, hippo_canvas_block_group_chat_iface_init));

static void
hippo_canvas_block_group_chat_init(HippoCanvasBlockGroupChat *block_group_chat)
{
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(block_group_chat);

    block->required_type = HIPPO_BLOCK_TYPE_GROUP_CHAT;
}

static HippoCanvasItemIface *item_parent_class;

static void
hippo_canvas_block_group_chat_iface_init(HippoCanvasItemIface *item_class)
{
    item_parent_class = g_type_interface_peek_parent(item_class);
}

static void
hippo_canvas_block_group_chat_class_init(HippoCanvasBlockGroupChatClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
    HippoCanvasBlockClass *canvas_block_class = HIPPO_CANVAS_BLOCK_CLASS(klass);

    object_class->set_property = hippo_canvas_block_group_chat_set_property;
    object_class->get_property = hippo_canvas_block_group_chat_get_property;

    object_class->dispose = hippo_canvas_block_group_chat_dispose;
    object_class->finalize = hippo_canvas_block_group_chat_finalize;

    canvas_block_class->append_content_items = hippo_canvas_block_group_chat_append_content_items;
    canvas_block_class->set_block = hippo_canvas_block_group_chat_set_block;
    canvas_block_class->title_activated = hippo_canvas_block_group_chat_title_activated;
    canvas_block_class->expand = hippo_canvas_block_group_chat_expand;
    canvas_block_class->unexpand = hippo_canvas_block_group_chat_unexpand;
}

static void
hippo_canvas_block_group_chat_dispose(GObject *object)
{

    G_OBJECT_CLASS(hippo_canvas_block_group_chat_parent_class)->dispose(object);
}

static void
hippo_canvas_block_group_chat_finalize(GObject *object)
{
    /* HippoCanvasBlockGroupChat *block = HIPPO_CANVAS_BLOCK_GROUP_CHAT(object); */

    G_OBJECT_CLASS(hippo_canvas_block_group_chat_parent_class)->finalize(object);
}

static void
hippo_canvas_block_group_chat_set_property(GObject         *object,
                                           guint            prop_id,
                                           const GValue    *value,
                                           GParamSpec      *pspec)
{
    HippoCanvasBlockGroupChat *block_group_chat;

    block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_group_chat_get_property(GObject         *object,
                                           guint            prop_id,
                                           GValue          *value,
                                           GParamSpec      *pspec)
{
    HippoCanvasBlockGroupChat *block_group_chat;

    block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT (object);

    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_canvas_block_group_chat_append_content_items (HippoCanvasBlock *block,
                                                    HippoCanvasBox   *parent_box)
{
    HippoCanvasBlockGroupChat *block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(block);
        
    hippo_canvas_block_set_heading(block, _("Group Chat"));
    hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(block_group_chat),
                                 "New chat activity",
                                 "Click to join group chat", FALSE);

    block_group_chat->quipper = g_object_new(HIPPO_TYPE_CANVAS_QUIPPER,
                                             "actions", hippo_canvas_block_get_actions(block),
                                             NULL);
    hippo_canvas_box_append(parent_box,
                            block_group_chat->quipper, 0);
    hippo_canvas_item_set_visible(block_group_chat->quipper,
                                  FALSE); /* not expanded at first */

    block_group_chat->last_message_preview = g_object_new(HIPPO_TYPE_CANVAS_LAST_MESSAGE_PREVIEW,
                                                          "actions", hippo_canvas_block_get_actions(block),
                                                          NULL);
    hippo_canvas_box_append(parent_box,
                            block_group_chat->last_message_preview, 0);
    hippo_canvas_item_set_visible(block_group_chat->last_message_preview,
                                  TRUE); /* initially expanded */

    
    block_group_chat->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                                  "actions", hippo_canvas_block_get_actions(block),
                                                  "padding-top", 8,
                                                  NULL);
    hippo_canvas_box_append(parent_box,
                            block_group_chat->chat_preview,
                            HIPPO_PACK_CLEAR_RIGHT);
    hippo_canvas_item_set_visible(block_group_chat->chat_preview,
                                  FALSE); /* not expanded at first */

}

static void
on_group_changed(HippoBlock *block,
                 GParamSpec *arg, /* null when first calling this */
                 HippoCanvasBlock *canvas_block)
{
    HippoGroup *group;
    HippoCanvasBlockGroupChat *canvas_group_chat;

    canvas_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(canvas_block);
    g_assert(block == canvas_block->block);
    group = NULL;
    g_object_get(G_OBJECT(block), "group", &group, NULL);

    if (group == NULL) {
        g_object_set(G_OBJECT(canvas_group_chat->quipper),
                     "title", NULL,
                     NULL);
    } else {
        g_object_set(G_OBJECT(canvas_group_chat->quipper),
                     "title", hippo_entity_get_name(HIPPO_ENTITY(group)),
                     NULL);
                                     
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_group_chat),
                                      hippo_entity_get_guid(HIPPO_ENTITY(group)));

        g_object_unref(group);
    }
}

static void
hippo_canvas_block_group_chat_set_block(HippoCanvasBlock *canvas_block,
                                        HippoBlock       *block)
{
    HippoCanvasBlockGroupChat *block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(canvas_block);
    
    /* g_debug("canvas-block-group-chat set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_group_changed),
                                             canvas_block);        
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_chat_parent_class)->set_block(canvas_block, block);

    g_object_set(block_group_chat->quipper,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_group_chat->last_message_preview,
                 "block", canvas_block->block,
                 NULL);
    g_object_set(block_group_chat->chat_preview,
                 "block", canvas_block->block,
                 NULL);
    
    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::group",
                         G_CALLBACK(on_group_changed),
                         canvas_block);

        on_group_changed(canvas_block->block, NULL, canvas_block);
    }
}

static void
hippo_canvas_block_group_chat_title_activated(HippoCanvasBlock *canvas_block)
{
    HippoActions *actions;
    HippoGroup *group;

    if (canvas_block->block == NULL)
        return;

    actions = hippo_canvas_block_get_actions(canvas_block);

    group = NULL;
    g_object_get(G_OBJECT(canvas_block->block),
                 "group", &group,
                 NULL);

    if (group == NULL)
        return;

    hippo_actions_join_chat_id(actions, hippo_entity_get_guid(HIPPO_ENTITY(group)));

    g_object_unref(group);
}

static void
hippo_canvas_block_group_update_visibility(HippoCanvasBlockGroupChat *block_group_chat)
{
    HippoCanvasBlock *canvas_block = HIPPO_CANVAS_BLOCK(block_group_chat);
    
    hippo_canvas_item_set_visible(block_group_chat->last_message_preview,
                                  !canvas_block->expanded);
    hippo_canvas_item_set_visible(block_group_chat->chat_preview,
                                  canvas_block->expanded);
    hippo_canvas_item_set_visible(block_group_chat->quipper,
                                  canvas_block->expanded);
}

static void
hippo_canvas_block_group_chat_expand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupChat *block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_chat_parent_class)->expand(canvas_block);

    hippo_canvas_block_group_update_visibility(block_group_chat);
}

static void
hippo_canvas_block_group_chat_unexpand(HippoCanvasBlock *canvas_block)
{
    HippoCanvasBlockGroupChat *block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(canvas_block);

    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_chat_parent_class)->unexpand(canvas_block);

    hippo_canvas_block_group_update_visibility(block_group_chat);
}
