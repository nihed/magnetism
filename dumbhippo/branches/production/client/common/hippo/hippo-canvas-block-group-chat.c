/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include <hippo/hippo-group.h>
#include "hippo-canvas-block.h"
#include "hippo-canvas-block-group-chat.h"
#include <hippo/hippo-canvas-box.h>
#include <hippo/hippo-canvas-image.h>
#include <hippo/hippo-canvas-text.h>
#include <hippo/hippo-canvas-gradient.h>
#include <hippo/hippo-canvas-link.h>
#include <hippo/hippo-canvas-chat-preview.h>
#include <hippo/hippo-canvas-message-preview.h>

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
static GObject* hippo_canvas_block_group_chat_constructor (GType                  type,
                                                           guint                  n_construct_properties,
                                                           GObjectConstructParam *construct_params);


/* Canvas block methods */
static void hippo_canvas_block_group_chat_set_block       (HippoCanvasBlock *canvas_block,
                                                           HippoBlock       *block);

static void hippo_canvas_block_group_chat_title_activated (HippoCanvasBlock *canvas_block);

static void hippo_canvas_block_group_chat_expand   (HippoCanvasBlock *canvas_block);
static void hippo_canvas_block_group_chat_unexpand (HippoCanvasBlock *canvas_block);

/* Our methods */
static void hippo_canvas_block_group_update_visibility(HippoCanvasBlockGroupChat *block_group_chat);

struct _HippoCanvasBlockGroupChat {
    HippoCanvasBlock canvas_block;
    HippoCanvasBox *parent_box;
    HippoCanvasItem *single_message_preview;
    HippoCanvasItem *chat_preview;

    guint have_messages : 1;
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
    object_class->constructor = hippo_canvas_block_group_chat_constructor;

    object_class->dispose = hippo_canvas_block_group_chat_dispose;
    object_class->finalize = hippo_canvas_block_group_chat_finalize;

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

static GObject*
hippo_canvas_block_group_chat_constructor (GType                  type,
                                           guint                  n_construct_properties,
                                           GObjectConstructParam *construct_properties)
{
    GObject *object = G_OBJECT_CLASS(hippo_canvas_block_group_chat_parent_class)->constructor(type,
                                                                                              n_construct_properties,
                                                                                              construct_properties);
    
    HippoCanvasBlock *block = HIPPO_CANVAS_BLOCK(object);
    HippoCanvasBlockGroupChat *block_group_chat = HIPPO_CANVAS_BLOCK_GROUP_CHAT(object);
    HippoCanvasBox *box;
        
    hippo_canvas_block_set_heading(block, _("Group Chat"));

    box = g_object_new(HIPPO_TYPE_CANVAS_BOX,
                       NULL);

    block_group_chat->parent_box = box;
    
    block_group_chat->single_message_preview = g_object_new(HIPPO_TYPE_CANVAS_MESSAGE_PREVIEW,
                                                           "actions", hippo_canvas_block_get_actions(block),
                                                           NULL);
    hippo_canvas_box_append(block_group_chat->parent_box,
                            block_group_chat->single_message_preview, 0);
    hippo_canvas_box_set_child_visible(block_group_chat->parent_box,
                                       block_group_chat->single_message_preview,
                                       TRUE); /* not expanded at first */

    
    block_group_chat->chat_preview = g_object_new(HIPPO_TYPE_CANVAS_CHAT_PREVIEW,
                                                  "actions", hippo_canvas_block_get_actions(block),
                                                  NULL);
    hippo_canvas_box_append(block_group_chat->parent_box,
                            block_group_chat->chat_preview, 0);
    hippo_canvas_box_set_child_visible(block_group_chat->parent_box,
                                       block_group_chat->chat_preview,
                                       FALSE); /* not expanded at first */

    hippo_canvas_block_set_content(block, HIPPO_CANVAS_ITEM(box));
    
    return object;
}

static void
update_chat_messages(HippoCanvasBlockGroupChat *canvas_group_chat)
{
    HippoBlock *block;
    HippoCanvasBlock *canvas_block;
    HippoChatMessage *last_message = NULL;
    GSList *messages;
    
    canvas_block = HIPPO_CANVAS_BLOCK(canvas_group_chat);
    block = canvas_block->block;
    g_assert(block != NULL);

    /* FIXME the title should probably be something like "N people chatting" or
     * "N messages" but we don't have that info yet
     */
    hippo_canvas_block_set_title(HIPPO_CANVAS_BLOCK(canvas_group_chat),
                                 "New chat activity",
                                 "Click to join group chat", FALSE);
    
    g_object_get(G_OBJECT(block), "recent-messages", &messages, NULL);
    
    if (messages)
        last_message = messages->data;

    g_object_set(G_OBJECT(canvas_group_chat->chat_preview),
                 "recent-messages", messages,
                 NULL);
    g_object_set(G_OBJECT(canvas_group_chat->single_message_preview),
                 "message", last_message,
                 NULL);

    canvas_group_chat->have_messages = last_message != NULL;

    hippo_canvas_block_group_update_visibility(canvas_group_chat);
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
        g_object_set(G_OBJECT(canvas_group_chat->chat_preview),
                     "chat-id", NULL,
                     NULL);
    } else {
        g_object_set(G_OBJECT(canvas_group_chat->chat_preview),
                     "chat-id", hippo_entity_get_guid(HIPPO_ENTITY(group)),
                     NULL);
                                     
        hippo_canvas_block_set_sender(HIPPO_CANVAS_BLOCK(canvas_group_chat),
                                      hippo_entity_get_guid(HIPPO_ENTITY(group)));

        g_object_unref(group);
    }
}

static void
on_recent_messages_changed(HippoBlock *block,
                           GParamSpec *arg, /* null when first calling this */
                           HippoCanvasBlock *canvas_block)
{
    update_chat_messages(HIPPO_CANVAS_BLOCK_GROUP_CHAT(canvas_block));
}

static void
hippo_canvas_block_group_chat_set_block(HippoCanvasBlock *canvas_block,
                                        HippoBlock       *block)
{
    /* g_debug("canvas-block-group-chat set block %p", block); */

    if (block == canvas_block->block)
        return;

    if (canvas_block->block != NULL) {
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_recent_messages_changed),
                                             canvas_block);
        g_signal_handlers_disconnect_by_func(G_OBJECT(canvas_block->block),
                                             G_CALLBACK(on_group_changed),
                                             canvas_block);        
    }
    
    /* Chain up to get the block really changed */
    HIPPO_CANVAS_BLOCK_CLASS(hippo_canvas_block_group_chat_parent_class)->set_block(canvas_block, block);

    if (canvas_block->block != NULL) {
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::recent-messages",
                         G_CALLBACK(on_recent_messages_changed),
                         canvas_block);
        g_signal_connect(G_OBJECT(canvas_block->block),
                         "notify::group",
                         G_CALLBACK(on_group_changed),
                         canvas_block);

        on_group_changed(canvas_block->block, NULL, canvas_block);
        on_recent_messages_changed(canvas_block->block, NULL, canvas_block);
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
    
    hippo_canvas_box_set_child_visible(block_group_chat->parent_box,
                                       block_group_chat->single_message_preview,
                                       !canvas_block->expanded && block_group_chat->have_messages);
    hippo_canvas_box_set_child_visible(block_group_chat->parent_box,
                                       block_group_chat->chat_preview,
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
