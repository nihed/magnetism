/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <hippo/hippo-common-internal.h>
#include "hippo-chat-room.h"
#include "hippo-connection.h"
#include "hippo-xml-utils.h"
#include <string.h>


/* === CONSTANTS === */

/* 2 hours chat room ignore timeout, in seconds; make this a parameter that can be set */
/* if want different ignore timeouts or want ignore to remain in effect indefinitely */
static const int CHAT_ROOM_IGNORE_TIMEOUT = 2*60*60; 

static void      hippo_chat_room_init        (HippoChatRoom       *room);
static void      hippo_chat_room_class_init  (HippoChatRoomClass  *klass);
static void      hippo_chat_room_finalize    (GObject             *object);

struct _HippoChatRoom {
    GObject parent;
    char *id;
    char *jabber_id;
    HippoChatKind kind;
    char *title;
    GHashTable *viewers;
    GHashTable *chatters;
    GSList *messages; /* sorted latest -> oldest */

    /* FIXME These two counts are preserved across connect/disconnect
     * which is wrong if we change login ID. However, it can't be fixed
     * here due to the "refcount" behavior; the UI code or whatever that 
     * does the joins/leaves affecting the count will have to know
     * to leave the chatroom on login ID change.
     */

    /* number of times we want to be a participant */
    int participant_count;
    /* number of times we want to be a viewer */
    int visitor_count;

    int generation;
    int loading_generation;
    guint loading : 1;
    /* date this chat room was last ignored; ignore for posts (including post-related chat rooms) */
    /* is handled separately */
    GTime date_last_ignored;
};

struct _HippoChatRoomClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoChatRoom, hippo_chat_room, G_TYPE_OBJECT);

enum {
    TITLE_CHANGED,
    USER_STATE_CHANGED,
    /* JOINED is emitted for a subset of STATE_CHANGED where the state went
     * from nonmember to either viewer or participant, and the chat room
     * isn't in the "loading" state. This means we may want to specially
     * indicate the new member of the chat to users.
     */
    USER_JOINED,
    MESSAGE_ADDED,
    LOADED,
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

    signals[USER_STATE_CHANGED] =
        g_signal_new ("user-state-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, HIPPO_TYPE_PERSON);

    signals[USER_JOINED] =
        g_signal_new ("user-joined",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, HIPPO_TYPE_PERSON);

    signals[MESSAGE_ADDED] =
        g_signal_new ("message-added",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__POINTER,
                      G_TYPE_NONE, 1, G_TYPE_POINTER);

    signals[LOADED] =
        g_signal_new ("loaded",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

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
    g_free(room->jabber_id);
    g_free(room->id);
    
    G_OBJECT_CLASS(hippo_chat_room_parent_class)->finalize(object);
}

HippoChatRoom*
hippo_chat_room_new(const char   *chat_id,
                    HippoChatKind kind)
{
    HippoChatRoom *room = g_object_new(HIPPO_TYPE_CHAT_ROOM, NULL);

    room->id = g_strdup(chat_id);
    room->kind = kind;

    room->generation = -1;
    room->loading_generation = -1;

    hippo_chat_room_set_title(room, NULL);

    return room;
}

const char*
hippo_chat_room_get_id(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    return room->id;
}

const char*
hippo_chat_room_get_jabber_id(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);
    if (room->jabber_id == NULL) {
        char *chat_jid;
        
        chat_jid = hippo_id_to_jabber_id(hippo_chat_room_get_id(room));
        room->jabber_id = g_strconcat(chat_jid, "@" HIPPO_ROOMS_JID_DOMAIN, NULL);
        g_free(chat_jid);
    }
    return room->jabber_id;
}

HippoChatKind
hippo_chat_room_get_kind(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_KIND_UNKNOWN);
    return room->kind;
}

GTime            
hippo_chat_room_get_date_last_ignored(HippoChatRoom   *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);
    return room->date_last_ignored;
}

gboolean         
hippo_chat_room_get_ignored(HippoChatRoom  *room)
{
    GTimeVal timeval;

    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), FALSE);

    // date_last_ignored being 0 means that the chat room was never ignored 
    // or that the chat room was explicitly unignored; we want to check for 
    // it explicitly here, rather than let this special meaning of 0 be lost
    // in the check below
    if (room->date_last_ignored == 0)
        return FALSE;

    g_get_current_time(&timeval);
    if (room->date_last_ignored + CHAT_ROOM_IGNORE_TIMEOUT < timeval.tv_sec)
        return FALSE;

    return TRUE;
}

void
hippo_chat_room_set_title(HippoChatRoom  *room,
                          const char     *title)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (title == NULL) {
        title = _("Loading...");
    }
    
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

GSList*
hippo_chat_room_get_messages(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), NULL);

    return room->messages;
}

gboolean
hippo_chat_room_get_loading(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), FALSE);

    if (room->generation > room->loading_generation)
        return FALSE; /* haven't called set_loading yet in this generation */
    
    return room->loading;
}

void
hippo_chat_room_set_loading(HippoChatRoom *room,
                            int            generation,
                            gboolean       loading)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    g_debug("Setting loading=%d generation %d on room %s (%p) old loading %d old generation %d",
            loading, generation, room->id, room, room->loading, room->loading_generation);
    
    if (room->loading_generation != generation) {
        room->loading_generation = generation;
        /* the old pending chat details isn't going to show */
        room->loading = FALSE;
    }
    
    loading = loading != FALSE;
    
    if (room->loading == loading)
        return;
    
    room->loading = loading;
    
    if (!room->loading) {
        g_signal_emit(G_OBJECT(room), signals[LOADED], 0);
    }
}

void
hippo_chat_room_set_kind(HippoChatRoom *room,
                         HippoChatKind  kind)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    g_return_if_fail(room->loading);
    
    if (room->kind == kind)
        return;
    
    /* Kind can go UNKNOWN->anything and anything->BROKEN but not 
     * GROUP->POST or GROUP->UNKNOWN or anything like that 
     */
    if (room->kind != HIPPO_CHAT_KIND_UNKNOWN) {
        if (kind != HIPPO_CHAT_KIND_BROKEN) {
            g_warning("Can't change chat room kind after it's already been set");
            return;
        }
    }
    
    room->kind = kind;
}

HippoChatState
hippo_chat_room_get_user_state(HippoChatRoom *room,
                               HippoPerson   *person)
{
    HippoChatState state;
    const char *guid;
    
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), HIPPO_CHAT_STATE_NONMEMBER);
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));
    
    if (g_hash_table_lookup(room->viewers, guid) != NULL)
        state = HIPPO_CHAT_STATE_VISITOR;
    else if (g_hash_table_lookup(room->chatters, guid) != NULL)
        state = HIPPO_CHAT_STATE_PARTICIPANT;
    else
        state = HIPPO_CHAT_STATE_NONMEMBER;
        
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
    
    g_debug("Updating chat state room '%s' person '%s/%s' old state %d state %d",
            room->id, hippo_entity_get_guid(HIPPO_ENTITY(person)),
            hippo_entity_get_name(HIPPO_ENTITY(person)) ? 
            hippo_entity_get_name(HIPPO_ENTITY(person)) : "NULL",
            old_state, state);
    
    if (old_state == state)
        return;
    
    guid = hippo_entity_get_guid(HIPPO_ENTITY(person));

    /* so we can remove from our hash tables and then 
     * still safely emit the signal 
     */
    g_object_ref(person);
    
    switch (old_state) {
    case HIPPO_CHAT_STATE_VISITOR:
        g_hash_table_remove(room->viewers, guid);
        break; 
    case HIPPO_CHAT_STATE_PARTICIPANT:
        g_hash_table_remove(room->chatters, guid);
        break;
    case HIPPO_CHAT_STATE_NONMEMBER:
        /* no old entry to remove */        
        break;
    }
    
    switch(state) {
    case HIPPO_CHAT_STATE_VISITOR:
        g_object_ref(person); /* extra ref for the hash */
        g_hash_table_replace(room->viewers, g_strdup(guid), person);
        break; 
    case HIPPO_CHAT_STATE_PARTICIPANT:
        g_object_ref(person); /* extra ref for the hash */    
        g_hash_table_replace(room->chatters, g_strdup(guid), person);    
        break;
    case HIPPO_CHAT_STATE_NONMEMBER:
        /* no new entry to insert */
        break;
    }

    g_signal_emit(room, signals[USER_STATE_CHANGED], 0, person);

    if (!room->loading && old_state == HIPPO_CHAT_STATE_NONMEMBER) {
        g_signal_emit(room, signals[USER_JOINED], 0, person);
    }

    g_object_unref(person);
}

void
hippo_chat_room_add_message(HippoChatRoom    *room,
                            HippoChatMessage *message)
{
    GSList *link;
    GSList *prev;
    int serial;    
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    serial = hippo_chat_message_get_serial(message);

    /* highest serial is earliest in the list */
    prev = NULL;
    for (link = room->messages; link != NULL; link = link->next) {
        HippoChatMessage *old = link->data;
        int old_serial = hippo_chat_message_get_serial(old);

        if (old_serial == serial) {
            /* We already have this message */
            hippo_chat_message_free(message);
            return;
        } else if (old_serial < serial) {
            if (prev) {
                g_assert(prev->next == link);
                prev->next = g_slist_prepend(prev->next, message);
            } else {
                g_assert(link == room->messages);
                room->messages = g_slist_prepend(room->messages, message);
            }

            g_signal_emit(room, signals[MESSAGE_ADDED], 0, message);

            return;
        }
        
        prev = link;
    }

    /* We were lower than all existing serials, or the existing list was empty */
    room->messages = g_slist_append(room->messages, message);
    g_signal_emit(room, signals[MESSAGE_ADDED], 0, message);
}

/* Called on reconnect, since users and messages will be resent */
void
hippo_chat_room_reconnected(HippoConnection *connection,
                            HippoChatRoom   *room)
{
    GHashTable *old_viewers;
    GHashTable *old_chatters;
    int new_generation;
    
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));

    new_generation = hippo_connection_get_generation(connection);

    g_debug("Clearing chat room old generation %d new %d",
            room->generation, new_generation);

    if (room->generation == new_generation) {
        g_debug("No disconnect has happened, no need to clear");
        return;
    }
    
    room->generation = new_generation;

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

    hippo_connection_request_chat_room_details(connection, room);
    
    g_signal_emit(room, signals[CLEARED], 0);
    
    g_hash_table_destroy(old_viewers);
    g_hash_table_destroy(old_chatters);        
}

void
hippo_chat_room_increment_state_count(HippoChatRoom *room,
                                      HippoChatState state)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    g_return_if_fail(state != HIPPO_CHAT_STATE_NONMEMBER);

    if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
        room->participant_count += 1;
        g_debug("participant_count for %s incremented to %d visitor_count %d",
                room->id, room->participant_count, room->visitor_count);
    } else if (state == HIPPO_CHAT_STATE_VISITOR) {
        room->visitor_count += 1;
        g_debug("visitor_count for %s incremented to %d participant_count %d",
                room->id, room->visitor_count, room->participant_count);
    }
}

void
hippo_chat_room_decrement_state_count(HippoChatRoom *room,
                                      HippoChatState state)
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    g_return_if_fail(state != HIPPO_CHAT_STATE_NONMEMBER);
    
    if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
        g_return_if_fail(room->participant_count >= 0);
        
        room->participant_count -= 1;
        g_debug("participant_count for %s decremented to %d visitor_count %d",
                room->id, room->participant_count, room->visitor_count);
    } else if (state == HIPPO_CHAT_STATE_VISITOR) {
        g_return_if_fail(room->visitor_count >= 0);

        room->visitor_count -= 1;
        g_debug("visitor_count for %s decremented to %d participant_count %d",
                room->id, room->visitor_count, room->participant_count);
    }
}

HippoChatState
hippo_chat_room_get_desired_state(HippoChatRoom *room)
{
    g_return_val_if_fail(HIPPO_IS_CHAT_ROOM(room), 0);

    if (room->participant_count > 0)
        return HIPPO_CHAT_STATE_PARTICIPANT;
    else if (room->visitor_count > 0)
        return HIPPO_CHAT_STATE_VISITOR;
    else
        return HIPPO_CHAT_STATE_NONMEMBER;
}

/* === HippoChatMessage === */

/* This is a "value object", has to be copied instead of ref'd */

struct _HippoChatMessage {
    guint32 magic;
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

    g_return_val_if_fail(text != NULL, NULL);
    
    message = g_new0(HippoChatMessage, 1);
    message->magic = HIPPO_CHAT_MESSAGE_MAGIC;
    message->person = person;
    g_object_ref(message->person);
    message->text = g_strdup(text);
    message->timestamp = timestamp;
    message->serial = serial;

    return message;
}

HippoChatMessage *
hippo_chat_message_new_from_xml(HippoDataCache *cache,
                                LmMessageNode  *node)
{
    gint64 serial;
    gint64 timestamp;
    HippoPerson *sender;
    const char *text;
    
    if (!hippo_xml_split(cache, node, NULL,
                         "serial", HIPPO_SPLIT_INT64, &serial,
                         "timestamp", HIPPO_SPLIT_TIME_MS, &timestamp,
                         "sender", HIPPO_SPLIT_PERSON, &sender,
                         "text", HIPPO_SPLIT_STRING | HIPPO_SPLIT_ELEMENT, &text,
                         NULL))
        return NULL;

    return hippo_chat_message_new(sender, text, timestamp / 1000, serial);
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

void             
hippo_chat_room_set_date_last_ignored(HippoChatRoom *room,
                                      GTime          date) 
{
    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    if (room->date_last_ignored != date) {
        room->date_last_ignored = date;
    }
}

void             
hippo_chat_room_set_ignored(HippoChatRoom    *room,
                            gboolean          is_ignored)
{
    GTimeVal timeval;

    g_return_if_fail(HIPPO_IS_CHAT_ROOM(room));
    
    if (is_ignored) {
        g_get_current_time(&timeval);
        room->date_last_ignored = timeval.tv_sec; 
    } else {
        room->date_last_ignored = 0;
    }
}
