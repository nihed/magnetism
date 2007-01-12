/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-block-music-chat.h"
#include "hippo-block-abstract-person.h"
#include "hippo-chat-room.h"
#include "hippo-person.h"
#include "hippo-xml-utils.h"
#include <string.h>

static void      hippo_block_music_chat_init                (HippoBlockMusicChat       *block_music_chat);
static void      hippo_block_music_chat_class_init          (HippoBlockMusicChatClass  *klass);

static void      hippo_block_music_chat_dispose             (GObject              *object);
static void      hippo_block_music_chat_finalize            (GObject              *object);

static gboolean  hippo_block_music_chat_update_from_xml     (HippoBlock           *block,
                                                               HippoDataCache       *cache,
                                                               LmMessageNode        *node);

static void hippo_block_music_chat_set_property (GObject      *object,
                                                   guint         prop_id,
                                                   const GValue *value,
                                                   GParamSpec   *pspec);
static void hippo_block_music_chat_get_property (GObject      *object,
                                                   guint         prop_id,
                                                   GValue       *value,
                                                   GParamSpec   *pspec);

struct _HippoBlockMusicChat {
    HippoBlockAbstractPerson            parent;

    HippoTrack *track;
    GSList *recent_messages;
};

struct _HippoBlockMusicChatClass {
    HippoBlockAbstractPersonClass parent_class;
};

#if 0
enum {
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];
#endif

enum {
    PROP_0,
    PROP_TRACK,
    PROP_RECENT_MESSAGES
};

G_DEFINE_TYPE(HippoBlockMusicChat, hippo_block_music_chat, HIPPO_TYPE_BLOCK_ABSTRACT_PERSON);
                       
static void
hippo_block_music_chat_init(HippoBlockMusicChat *block_music_chat)
{
}

static void
hippo_block_music_chat_class_init(HippoBlockMusicChatClass *klass)
{
    HippoBlockClass *block_class = HIPPO_BLOCK_CLASS(klass);
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    object_class->set_property = hippo_block_music_chat_set_property;
    object_class->get_property = hippo_block_music_chat_get_property;

    object_class->dispose = hippo_block_music_chat_dispose;
    object_class->finalize = hippo_block_music_chat_finalize;

    block_class->update_from_xml = hippo_block_music_chat_update_from_xml;
    
    g_object_class_install_property(object_class,
                                    PROP_TRACK,
                                    g_param_spec_object("track",
                                                         _("Track"),
                                                         _("Track the chat is about"),
                                                        HIPPO_TYPE_TRACK,
                                                        G_PARAM_READABLE));
    g_object_class_install_property(object_class,
                                    PROP_RECENT_MESSAGES,
                                    g_param_spec_pointer("recent-messages",
                                                         _("Recent Messages"),
                                                         _("Recent Messages in the Group's Chat Room"),
                                                         G_PARAM_READABLE));
}

static void
set_recent_messages(HippoBlockMusicChat *block_music_chat,
                    GSList              *recent_messages)
{
    GSList *l;
    
    g_slist_foreach(block_music_chat->recent_messages, (GFunc)hippo_chat_message_free, NULL);
    g_slist_free(block_music_chat->recent_messages);
    block_music_chat->recent_messages = NULL;

    for (l = recent_messages; l; l = l->next) {
        block_music_chat->recent_messages = g_slist_prepend(block_music_chat->recent_messages,
                                                            hippo_chat_message_copy(l->data));
    }

    block_music_chat->recent_messages = g_slist_reverse(block_music_chat->recent_messages);

    g_object_notify(G_OBJECT(block_music_chat), "recent-messages");
}

static void
set_track(HippoBlockMusicChat *block_music_chat,
          HippoTrack          *track)
{
    if (block_music_chat->track == track)
        return;

    if (block_music_chat->track) {
        g_object_unref(block_music_chat->track);
        block_music_chat->track = NULL;
    }

    block_music_chat->track = track;
    
    if (block_music_chat->track) {
        g_object_ref(block_music_chat->track);
    }
    
    g_object_notify(G_OBJECT(block_music_chat), "track");
}

static void
hippo_block_music_chat_dispose(GObject *object)
{
    HippoBlockMusicChat *block_music_chat = HIPPO_BLOCK_MUSIC_CHAT(object);

    set_track(block_music_chat, NULL);
    set_recent_messages(block_music_chat, NULL);
    
    G_OBJECT_CLASS(hippo_block_music_chat_parent_class)->dispose(object); 
}

static void
hippo_block_music_chat_finalize(GObject *object)
{
    G_OBJECT_CLASS(hippo_block_music_chat_parent_class)->finalize(object); 
}

static void
hippo_block_music_chat_set_property(GObject         *object,
                                      guint            prop_id,
                                      const GValue    *value,
                                      GParamSpec      *pspec)
{
    switch (prop_id) {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_block_music_chat_get_property(GObject         *object,
                                      guint            prop_id,
                                      GValue          *value,
                                      GParamSpec      *pspec)
{
    HippoBlockMusicChat *block_music_chat = HIPPO_BLOCK_MUSIC_CHAT(object);

    switch (prop_id) {
    case PROP_TRACK:
        g_value_set_object(value, block_music_chat->track);
        break;
    case PROP_RECENT_MESSAGES:
        g_value_set_pointer(value, block_music_chat->recent_messages);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static gboolean
hippo_block_music_chat_update_from_xml (HippoBlock           *block,
                                        HippoDataCache       *cache,
                                        LmMessageNode        *node)
{
    HippoBlockMusicChat *block_music_chat = HIPPO_BLOCK_MUSIC_CHAT(block);
    LmMessageNode *music_node;
    LmMessageNode *track_node;
    LmMessageNode *recent_messages_node = NULL;
    LmMessageNode *subchild;
    HippoPerson *user;
    HippoTrack *track;
    GSList *recent_messages = NULL;

    if (!HIPPO_BLOCK_CLASS(hippo_block_music_chat_parent_class)->update_from_xml(block, cache, node))
        return FALSE;

    if (!hippo_xml_split(cache, node, NULL,
                         "musicChat", HIPPO_SPLIT_NODE, &music_node,
                         NULL))
        return FALSE;

    if (!hippo_xml_split(cache, music_node, NULL,
                         "userId", HIPPO_SPLIT_PERSON, &user,
                         "recentMessages", HIPPO_SPLIT_NODE, &recent_messages_node,
                         "track", HIPPO_SPLIT_NODE, &track_node,
                         NULL))
        return FALSE;

    hippo_block_abstract_person_set_user(HIPPO_BLOCK_ABSTRACT_PERSON(block), user);

    track = hippo_track_new_from_xml(cache, track_node);
    if (!track)
        return FALSE;
    
    set_track(block_music_chat, track);
    g_object_unref(track);

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
    
    set_recent_messages(block_music_chat, recent_messages);

    g_slist_foreach(recent_messages, (GFunc)hippo_chat_message_free, NULL);
    g_slist_free(recent_messages);

    return TRUE;
}
