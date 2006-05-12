#ifndef __HIPPO_CHAT_ROOM_H__
#define __HIPPO_CHAT_ROOM_H__

#include <hippo/hippo-person.h>

G_BEGIN_DECLS

typedef struct _HippoChatMessage   HippoChatMessage;

typedef enum {
    HIPPO_CHAT_NONMEMBER,
    HIPPO_CHAT_VISITOR,
    HIPPO_CHAT_PARTICIPANT
} HippoChatState;

typedef enum {
    HIPPO_CHAT_POST,
    HIPPO_CHAT_GROUP
} HippoChatKind;

typedef struct _HippoChatRoom      HippoChatRoom;
typedef struct _HippoChatRoomClass HippoChatRoomClass;

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

/* === Methods used by HippoConnection to keep chat room updated === */

/* Set while we are using a <details/> IQ to fill a chatroom we aren't part of */
gboolean hippo_chat_room_get_filling             (HippoChatRoom *room);
void     hippo_chat_room_set_filling             (HippoChatRoom *room,
                                                  gboolean       filling);
void     hippo_chat_room_set_user_state          (HippoChatRoom *room,
                                                  HippoPerson   *person,
                                                  HippoChatState state);
/* Ownership of the message passes to the chat room, which may IMMEDIATELY FREE
 * the message if it's a dup
 */
void     hippo_chat_room_add_message             (HippoChatRoom    *room,
                                                  HippoChatMessage *message);
/* Called on reconnect, since users and messages will be resent */
void     hippo_chat_room_clear                   (HippoChatRoom *room);

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
