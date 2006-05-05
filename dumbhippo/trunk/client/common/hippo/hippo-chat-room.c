#include "hippo-chat-room.h"
#include <string.h>

static void      hippo_chat_room_init        (HippoChatRoom       *room);
static void      hippo_chat_room_class_init  (HippoChatRoomClass  *klass);
static void      hippo_chat_room_finalize    (GObject             *object);

struct _HippoChatRoom {
    GObject parent;
    char *id;
    HippoChatState state;
    char *title;
    GHashTable *viewers;
    GHashTable *chatters;
    GSList *messages; /* sorted latest -> oldest */
    guint filling : 1;
};

struct _HippoChatRoomClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoChatRoom, hippo_chat_room, G_TYPE_OBJECT);

enum {
    TITLE_CHANGED,
    STATE_CHANGED,
    USER_STATE_CHANGED,
    MESSAGE_ADDED,
    CLEARED,
    LAST_SIGNAL
};
  
static int signals[LAST_SIGNAL];  

static void
make_hash_tables(HippoChatRoom *room)
{
    room->viewers = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          g_free, (GFreeFunc) g_object_unref);
    room->chatters = g_hash_table_new_full(g_str_hash, g_str_equal,
                                           g_free, (GFreeFunc) g_object_unref);
}
                       
static void
hippo_chat_room_init(HippoChatRoom *room)
{
    room->state = HIPPO_CHAT_NONMEMBER;

    make_hash_tables(room);
}

static void
hippo_chat_room_class_init(HippoChatRoomClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  

    signals[TITLE_CHANGED] =
        g_signal_new ("title-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);

    signals[STATE_CHANGED] =
        g_signal_new ("state-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__INT,
            		  G_TYPE_NONE, 1, G_TYPE_INT);

    signals[USER_STATE_CHANGED] =
        g_signal_new ("user-state-changed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[MESSAGE_ADDED] =
        g_signal_new ("message-added",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__POINTER,
            		  G_TYPE_NONE, 1, G_TYPE_POINTER);

    signals[CLEARED] =
        g_signal_new ("cleared",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__VOID,
            		  G_TYPE_NONE, 0);

    object_class->finalize = hippo_chat_room_finalize;
}

static void
free_messages(HippoChatRoom *room)
{
    g_slist_foreach(room->messages, (GFunc) hippo_chat_message_free, NULL);
    g_slist_free(room->messages);
    room->messages = NULL;
}

static void
hippo_chat_room_finalize(GObject *object)
{
    HippoChatRoom *room = HIPPO_CHAT_ROOM(object);

    free_messages(room);
    g_hash_table_destroy(room->viewers);
    g_hash_table_destroy(room->chatters);
    g_free(room->title);
    g_free(room->id);
    
    G_OBJECT_CLASS(hippo_chat_room_parent_class)->finalize(object);
}

HippoChatRoom*
hippo_chat_room_new(const char *chat_id)
{
    HippoChatRoom *room = g_object_new(HIPPO_TYPE_CHAT_ROOM, NULL);

    room->id = g_strdup(chat_id);

    return room;
}

const char*
hippo_chat_room_get_id(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    return room->id;
}

HippoChatState
hippo_chat_room_get_state(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_NONMEMBER);
    return room->state;
}

void
hippo_chat_room_set_title(HippoChatRoom  *room,
                          const char     *title)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (title == room->title ||
        (title && room->title && strcmp(title, room->title) == 0))
        return;
        
    g_free(room->title);
    room->title = g_strdup(title);
    
    g_signal_emit(room, signals[TITLE_CHANGED], 0, room->title);
}

const char*
hippo_chat_room_get_title(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    return room->title;
}

HippoChatMessage*
hippo_chat_room_get_last_message(HippoChatRoom  *room)
{
    HippoChatMessage *message;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);

    if (room->messages == NULL)
        return NULL;
        
    message = room->messages->data;
    
    /* -1 message is the post description, which isn't technically
     * a message, it's displayed only in certain contexts.
     */
    if (hippo_chat_message_get_serial(message) == -1)
        return NULL;
    else
        return message;
}

static void
listify_foreach(void *key, void *value, void *data)
{
    GSList **list_p = data;
    *list_p = g_slist_prepend(*list_p, value);
    g_object_ref(value);
}

GSList*
hippo_chat_room_get_users(HippoChatRoom  *room)
{
    GSList *list;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    
    list = NULL;
    g_hash_table_foreach(room->chatters, listify_foreach, &list);
    g_hash_table_foreach(room->viewers, listify_foreach, &list);
    return list;
}

/*
 * FIXME this is "write through" unlike the rest of HippoDataCache etc.
 * i.e. it updates local state before hearing back from the server.
 * Kind of strange.
 */
void
hippo_chat_room_set_state(HippoChatRoom  *room,
                          HippoChatState  state)
{
    HippoChatState old_state;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (room->state == state)
        return;
    old_state = room->state;
    room->state = state;

    /* FIXME this needs to send the appropriate XMPP, 
     * see HippoIM::onChatRoomStateChange
     */
        
    g_signal_emit(room, signals[STATE_CHANGED], 0, old_state);
}

int
hippo_chat_room_get_viewing_user_count(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);
    
    return g_hash_table_size(room->viewers);
}

int
hippo_chat_room_get_chatting_user_count(HippoChatRoom  *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);
    
    return g_hash_table_size(room->chatters);
}

gboolean
hippo_chat_room_get_filling(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), FALSE);
    
    return room->filling;
}

void
hippo_chat_room_set_filling(HippoChatRoom *room,
                            gboolean       filling)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    room->filling = filling;
}

HippoChatState
hippo_chat_room_get_user_state(HippoChatRoom *room,
                               HippoPerson   *person)
{
    HippoChatState state;
    const char *guid;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_NONMEMBER);
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));
    
    if (g_hash_table_lookup(room->viewers, guid) != NULL)
        state = HIPPO_CHAT_VISITOR;
    else if (g_hash_table_lookup(room->chatters, guid) != NULL)
        state = HIPPO_CHAT_PARTICIPANT;
    else
        state = HIPPO_CHAT_NONMEMBER;
        
    return state;
}

void
hippo_chat_room_set_user_state(HippoChatRoom *room,
                               HippoPerson   *person,
                               HippoChatState state)
{
    HippoChatState old_state;
    const char *guid;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    old_state = hippo_chat_room_get_user_state(room, person);
    
    if (old_state == state)
        return;
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));

    /* so we can remove from our hash tables and then 
     * still safely emit the signal 
     */
    g_object_ref(person);
    
    switch (old_state) {
    case HIPPO_CHAT_VISITOR:
        g_hash_table_remove(room->viewers, guid);
        break; 
    case HIPPO_CHAT_PARTICIPANT:
        g_hash_table_remove(room->viewers, guid);
        break;
    case HIPPO_CHAT_NONMEMBER:
        /* no old entry to remove */        
        break;
    }
    
    switch(state) {
    case HIPPO_CHAT_VISITOR:
        g_hash_table_replace(room->viewers, g_strdup(guid), person);
        break; 
    case HIPPO_CHAT_PARTICIPANT:
        g_hash_table_replace(room->chatters, g_strdup(guid), person);    
        break;
    case HIPPO_CHAT_NONMEMBER:
        /* no new entry to insert */
        break;
    }

    g_signal_emit(room, signals[USER_STATE_CHANGED], 0, person);
    
    g_object_unref(person);
}

void
hippo_chat_room_add_message(HippoChatRoom *room,
                            HippoPerson   *sender,
                            const char    *text,
                            GTime          timestamp,
                            int            serial)
{
    GSList *link;
    GSList *prev;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    /* highest serial is earliest in the list */
    prev = NULL;
    for (link = room->messages; link != NULL; link = link->next) {
        HippoChatMessage *old = link->data;
        int old_serial = hippo_chat_message_get_serial(old);
        
        if (old_serial == serial) {
            return; /* nothing to do, we already have this message */
        } else if (old_serial < serial) {
            HippoChatMessage *message;
            message = hippo_chat_message_new(sender, text, timestamp, serial);
            if (prev) {
                g_assert(prev->next == link);
                prev->next = g_slist_prepend(prev->next, message);
            } else {
                g_assert(link == room->messages);
                room->messages = g_slist_prepend(room->messages, message);
            }
            
            g_signal_emit(room, signals[MESSAGE_ADDED], 0, message);
            
            break;
        }
        prev = link;
    }
}

/* Called on reconnect, since users and messages will be resent */
void
hippo_chat_room_clear(HippoChatRoom *room)
{
    GHashTable *old_viewers;
    GHashTable *old_chatters;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    /* Save old tables to hold refs while
     * we emit the "cleared" signal so 
     * api users can drop their usage of the users 
     */

    old_viewers = room->viewers;
    room->viewers = NULL;
    old_chatters = room->chatters;
    room->chatters = NULL;
    
    make_hash_tables(room);
    
    free_messages(room);
    
    g_signal_emit(room, signals[CLEARED], 0);
    
    g_hash_table_destroy(old_viewers);
    g_hash_table_destroy(old_chatters);        
}

/* === HippoChatMessage === */

/* This is a "value object", has to be copied instead of ref'd */

struct _HippoChatMessage {
    int magic;
    HippoPerson *person;
    char *text;
    GTime timestamp;
    int serial;
};

/* a little paranoia to help with our C++ skillz */
#define HIPPO_CHAT_MESSAGE_MAGIC 0x1234cafe
#define HIPPO_IS_CHAT_MESSAGE(message)(((HippoChatMessage*)message)->magic == HIPPO_CHAT_MESSAGE_MAGIC)

HippoChatMessage*
hippo_chat_message_new(HippoPerson *person,
                       const char  *text,
                       GTime        timestamp,
                       int          serial)
{
    HippoChatMessage *message;
    
    message = g_new0(HippoChatMessage, 1);
    message->magic = HIPPO_CHAT_MESSAGE_MAGIC;
    message->person = person;
    g_object_ref(message->person);
    message->text = g_strdup(text);
    message->timestamp = timestamp;
    message->serial = serial;

    return message;
}

void
hippo_chat_message_free(HippoChatMessage *message)
{
    g_return_if_fail(HIPPO_IS_CHAT_MESSAGE(message));
    
    message->magic = 0xdeadbeef;
    g_object_unref(message->person);
    g_free(message->text);
    g_free(message);
}

HippoChatMessage*
hippo_chat_message_copy(HippoChatMessage *message)
{
    HippoChatMessage *copy;
 
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);
    
    copy = g_new(HippoChatMessage, 1);
    *copy = *message;
    g_object_ref(copy->person);
    copy->text = g_strdup(message->text);
    return copy;
}

HippoPerson*
hippo_chat_message_get_person(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);

    return message->person;
}

const char*
hippo_chat_message_get_text(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), NULL);

    return message->text;
}

GTime
hippo_chat_message_get_timestamp(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), 0);

    return message->timestamp;
}

int
hippo_chat_message_get_serial(HippoChatMessage *message)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_MESSAGE(message), 0);

    return message->serial;
}
