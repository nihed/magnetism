/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_CHAT_ROOM_H__
#define __HIPPO_CHAT_ROOM_H__

#include <hippo/hippo-person.h>

G_BEGIN_DECLS

/*
 * CHAT ROOM LIFECYCLE OVERVIEW
 * 
 * When a chat room is created, it asks the server for the current chat state.
 * during this time, loading = TRUE. When the room is fully loaded, loading=FALSE
 * and the room emits the "loaded" signal.
 * 
 * (note, loading=FALSE when a chat room is just created, the loading flag means 
 * we've called request_details and don't have a reply yet. but we always request_details
 * right away)
 * 
 * Once a room is loaded, it's guaranteed to have known kind POST, GROUP, or BROKEN.
 * BROKEN means the server hadn't heard of the room.
 * 
 * Before a room is loaded, if the kind is known it will be set, but the kind may
 * be UNKNOWN.
 * 
 * If we disconnect and reconnect to the server, the chat room can re-enter the
 * loading=TRUE state. If it does so, the "cleared" signal will be emitted first.
 */

typedef struct _HippoChatMessage   HippoChatMessage;

#define HIPPO_TYPE_CHAT_ROOM              (hippo_chat_room_get_type ())
#define HIPPO_CHAT_ROOM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CHAT_ROOM, HippoChatRoom))
#define HIPPO_CHAT_ROOM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))
#define HIPPO_IS_CHAT_ROOM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_IS_CHAT_ROOM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_CHAT_ROOM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))

GType        	 hippo_chat_room_get_type               (void) G_GNUC_CONST;

HippoChatRoom*    hippo_chat_room_new                     (const char   *chat_id,
                                                           HippoChatKind kind);

const char*       hippo_chat_room_get_id                  (HippoChatRoom  *room);
const char*       hippo_chat_room_get_jabber_id           (HippoChatRoom  *room);
gboolean          hippo_chat_room_get_loading             (HippoChatRoom  *room);
HippoChatKind     hippo_chat_room_get_kind                (HippoChatRoom  *room);
HippoChatState    hippo_chat_room_get_user_state          (HippoChatRoom  *room,
                                                           HippoPerson    *person);
void              hippo_chat_room_set_title               (HippoChatRoom  *room,
                                                           const char     *title);
const char*       hippo_chat_room_get_title               (HippoChatRoom  *room);
HippoChatMessage* hippo_chat_room_get_last_message        (HippoChatRoom  *room);
/* need to unref each element and free the list when done with this */
GSList*           hippo_chat_room_get_users               (HippoChatRoom  *room);
int               hippo_chat_room_get_viewing_user_count  (HippoChatRoom  *room);
int               hippo_chat_room_get_chatting_user_count (HippoChatRoom  *room);
/* This is not a copy, don't need to free list or its members */
GSList*           hippo_chat_room_get_messages            (HippoChatRoom  *room);
GTime             hippo_chat_room_get_date_last_ignored   (HippoChatRoom  *room);
gboolean          hippo_chat_room_get_ignored             (HippoChatRoom  *room);

/* === Methods used by HippoConnection to keep chat room updated === */

/* Set while we are loading the chat room details with connection_request_details */
void     hippo_chat_room_set_loading             (HippoChatRoom *room,
                                                  int            generation,
                                                  gboolean       loading);
void     hippo_chat_room_set_kind                (HippoChatRoom *room,
                                                  HippoChatKind  kind);
void     hippo_chat_room_set_user_state          (HippoChatRoom *room,
                                                  HippoPerson   *person,
                                                  HippoChatState state);
void     hippo_chat_room_set_date_last_ignored   (HippoChatRoom *room,
												  GTime          date); 
void     hippo_chat_room_set_ignored             (HippoChatRoom *room,
												  gboolean       is_ignored);


/* Ownership of the message passes to the chat room, which may IMMEDIATELY FREE
 * the message if it's a dup
 */
void     hippo_chat_room_add_message             (HippoChatRoom    *room,
                                                  HippoChatMessage *message);
/* Called on reconnect, since users and messages will be resent */
void     hippo_chat_room_reconnected             (HippoConnection *connection,
                                                  HippoChatRoom   *room);

/* bump our count of desiring PARTICIPANT or VISITOR */
void            hippo_chat_room_increment_state_count (HippoChatRoom *room,
                                                       HippoChatState state);
void            hippo_chat_room_decrement_state_count (HippoChatRoom *room,
                                                       HippoChatState state);
/* based on the above counts, what state would the logged-in user like to be in */
HippoChatState  hippo_chat_room_get_desired_state     (HippoChatRoom *room);

/* === Methods on HippoChatMessage === */

HippoChatMessage* hippo_chat_message_new           (HippoPerson      *person,
                                                    const char       *text,
                                                    GTime             timestamp,
                                                    int               serial);
void              hippo_chat_message_free          (HippoChatMessage *message);
HippoChatMessage* hippo_chat_message_copy          (HippoChatMessage *message);
HippoPerson*      hippo_chat_message_get_person    (HippoChatMessage *message);
const char*       hippo_chat_message_get_text      (HippoChatMessage *message);
GTime             hippo_chat_message_get_timestamp (HippoChatMessage *message);
int               hippo_chat_message_get_serial    (HippoChatMessage *message);

G_END_DECLS

#endif /* __HIPPO_CHAT_ROOM_H__ */
