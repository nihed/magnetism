/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-chat-room.h"
#include "hippo-common-internal.h"
#include "hippo-block-group-chat.h"
#include "hippo-group.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_group_chat_init                (HippoBlockGroupChat       *block_group_chat);
static void      hippo_block_group_chat_class_init          (HippoBlockGroupChatClass  *klass);

static void      hippo_block_group_chat_dispose             (GObject              *object);
static void      hippo_block_group_chat_finalize            (GObject              *object);

static gboolean  hippo_block_group_chat_update_from_xml     (HippoBlock           *block,
                                                             HippoDataCache       *cache,
                                                             LmMessageNode        *node);

static void hippo_block_group_chat_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_block_group_chat_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);

struct _HippoBlockGroupChat {
    HippoBlock       parent;
    HippoGroup      *group;
};

struct _HippoBlockGroupChatClass {
    HippoBlockClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_GROUP,
};

G_DEFINE_TYPE(HippoBlockGroupChat, hippo_block_group_chat, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_group_chat_init(HippoBlockGroupChat *block_group_chat)
{
}

static void
hippo_block_group_chat_class_init(HippoBlockGroupChatClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_group_chat_set_property;
    object_class->get_property = hippo_block_group_chat_get_property;

    object_class->dispose = hippo_block_group_chat_dispose;
    object_class->finalize = hippo_block_group_chat_finalize;

    block_class->update_from_xml = hippo_block_group_chat_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_GROUP,
                                    g_param_spec_object("group",
                                                        _("Group"),
                                                        _("Group where people are chatting"),
                                                        HIPPO_TYPE_GROUP,
                                                        G_PARAM_READABLE));
}

static void
set_group(HippoBlockGroupChat *block_group_chat,
          HippoGroup          *group)
{
#if 0
    g_debug("set_group old group %s new group %s",
            block_group_chat->group ?
            hippo_entity_get_guid(HIPPO_ENTITY(block_group_chat->group)) : "null",
            group ?
            hippo_entity_get_guid(HIPPO_ENTITY(group)) : "null");
#endif
    
    if (group == block_group_chat->group)
        return;
    
    if (block_group_chat->group) {
        g_object_unref(block_group_chat->group);
        block_group_chat->group = NULL;
    }

    if (group) {
        g_object_ref(group);
        block_group_chat->group = group;
    }

    g_object_notify(G_OBJECT(block_group_chat), "group");
}

static void
hippo_block_group_chat_dispose(GObject *object)
{
    HippoBlockGroupChat *block_group_chat = HIPPO_BLOCK_GROUP_CHAT(object);

    set_group(block_group_chat, NULL);
    
    G_OBJECT_CLASS(hippo_block_group_chat_parent_class)->dispose(object); 
}

static void
hippo_block_group_chat_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_group_chat_parent_class)->finalize(object); 
}

static void
hippo_block_group_chat_set_property(GObject         *object,
                                    guint            prop_id,
                                    const GValue    *value,
                                    GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_group_chat_get_property(GObject         *object,
                                    guint            prop_id,
                                    GValue          *value,
                                    GParamSpec      *pspec)
{
    HippoBlockGroupChat *block_group_chat = HIPPO_BLOCK_GROUP_CHAT(object);

    switch (prop_id) {
    case PROP_GROUP:
        g_value_set_object(value, G_OBJECT(block_group_chat->group));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_group_chat_update_from_xml (HippoBlock           *block,
                                        HippoDataCache       *cache,
                                        LmMessageNode        *node)
{
    HippoBlockGroupChat *block_group_chat = HIPPO_BLOCK_GROUP_CHAT(block);
    LmMessageNode *group_node;
    HippoGroup *group;
    LmMessageNode *recent_messages_node;

    if (!HIPPO_BLOCK_CLASS(hippo_block_group_chat_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "groupChat", HIPPO_SPLIT_NODE, &group_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, group_node, NULL,
                         "groupId", HIPPO_SPLIT_GROUP, &group,
                         "recentMessages", HIPPO_SPLIT_NODE, &recent_messages_node,
                         NULL))
        return FALSE;

        
    if (!hippo_block_set_recent_messages_from_xml(block, cache, recent_messages_node))
        return FALSE;
    
    hippo_block_set_chat_id(block, hippo_entity_get_guid(HIPPO_ENTITY(group)));

    set_group(block_group_chat, group);

    return TRUE;
}
