/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block.h"
#include "hippo-block-post.h"
#include "hippo-post.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_post_init                (HippoBlockPost       *block_post);
static void      hippo_block_post_class_init          (HippoBlockPostClass  *klass);

static void      hippo_block_post_dispose             (GObject              *object);
static void      hippo_block_post_finalize            (GObject              *object);

static gboolean  hippo_block_post_update_from_xml     (HippoBlock           *block,
                                                       HippoDataCache       *cache,
                                                       LmMessageNode        *node,
                                                       guint64               server_timestamp);

static void hippo_block_post_set_property (GObject      *object,
                                           guint         prop_id,
                                           const GValue *value,
                                           GParamSpec   *pspec);
static void hippo_block_post_get_property (GObject      *object,
                                           guint         prop_id,
                                           GValue       *value,
                                           GParamSpec   *pspec);

struct _HippoBlockPost {
    HippoBlock       parent;
    HippoPost       *post;

    GSList *recent_messages;
};

struct _HippoBlockPostClass {
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
    PROP_POST,
    PROP_RECENT_MESSAGES
};

G_DEFINE_TYPE(HippoBlockPost, hippo_block_post, HIPPO_TYPE_BLOCK);
                       
static void
hippo_block_post_init(HippoBlockPost *block_post)
{
}

static void
hippo_block_post_class_init(HippoBlockPostClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_post_set_property;
    object_class->get_property = hippo_block_post_get_property;

    object_class->dispose = hippo_block_post_dispose;
    object_class->finalize = hippo_block_post_finalize;

    block_class->update_from_xml = hippo_block_post_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_POST,
                                    g_param_spec_object("post",
                                                        _("Post"),
                                                        _("Post displayed in the block"),
                                                        HIPPO_TYPE_POST,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_RECENT_MESSAGES,
                                    g_param_spec_pointer("recent-messages",
                                                         _("Recent Messages"),
                                                         _("Recent Messages in the Post's Chat Room"),
                                                         G_PARAM_READABLE));
}

static void
set_post(HippoBlockPost *block_post,
         HippoPost      *post)
{
#if 0
    g_debug("set_post old post %s new post %s",
            block_post->post ?
            hippo_post_get_guid(block_post->post) : "null",
            post ?
            hippo_post_get_guid(post) : "null");
#endif
    
    if (post == block_post->post)
        return;
    
    if (block_post->post) {
        g_object_unref(block_post->post);
        block_post->post = NULL;
    }

    if (post) {
        g_object_ref(post);
        block_post->post = post;
    }

    g_object_notify(G_OBJECT(block_post), "post");
}

static void
set_recent_messages(HippoBlockPost  *block_post,
                    GSList          *recent_messages)
{
    GSList *l;
    
    g_slist_foreach(block_post->recent_messages, (GFunc)hippo_chat_message_free, NULL);
    g_slist_free(block_post->recent_messages);

    for (l = recent_messages; l; l = l->next) {
        block_post->recent_messages = g_slist_prepend(block_post->recent_messages, hippo_chat_message_copy(l->data));
    }

    block_post->recent_messages = g_slist_reverse(block_post->recent_messages);

    g_object_notify(G_OBJECT(block_post), "recent-messages");
}

static void
hippo_block_post_dispose(GObject *object)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    set_post(block_post, NULL);
    set_recent_messages(block_post, NULL);
    
    G_OBJECT_CLASS(hippo_block_post_parent_class)->dispose(object); 
}

static void
hippo_block_post_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_post_parent_class)->finalize(object); 
}

static void
hippo_block_post_set_property(GObject         *object,
                              guint            prop_id,
                              const GValue    *value,
                              GParamSpec      *pspec)
{
    G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
}

static void
hippo_block_post_get_property(GObject         *object,
                              guint            prop_id,
                              GValue          *value,
                              GParamSpec      *pspec)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(object);

    switch (prop_id) {
    case PROP_POST:
        g_value_set_object(value, G_OBJECT(block_post->post));
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_post_update_from_xml (HippoBlock           *block,
                                  HippoDataCache       *cache,
                                  LmMessageNode        *node,
                                  guint64               server_timestamp)
{
    HippoBlockPost *block_post = HIPPO_BLOCK_POST(block);
    LmMessageNode *post_node;
    HippoPost *post;
    LmMessageNode *recent_messages_node = NULL;
    LmMessageNode *subchild;
    GSList *recent_messages = NULL;

    if (!hippo_xml_split(cache, node, NULL,
                         "post", HIPPO_SPLIT_NODE, &post_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, post_node, NULL,
                         "postId", HIPPO_SPLIT_POST, &post,
                         "recentMessages", HIPPO_SPLIT_NODE, &recent_messages_node,
                         NULL))
        return FALSE;

        
    for (subchild = recent_messages_node->children; subchild; subchild = subchild->next) {
        HippoChatMessage *chat_message;
        
        if (!strcmp(subchild->name, "message") == 0)
            continue;
        
        chat_message = hippo_chat_message_new_from_xml(cache, subchild);
        if (!chat_message)
            continue;
        
        recent_messages = g_slist_prepend(recent_messages, chat_message);
    }
    
    recent_messages = g_slist_reverse(recent_messages);
    
    set_post(block_post, post);
    set_recent_messages(block_post, recent_messages);

    g_slist_foreach(recent_messages, (GFunc)hippo_chat_message_free, NULL);
    g_slist_free(recent_messages);

    return TRUE;
}
