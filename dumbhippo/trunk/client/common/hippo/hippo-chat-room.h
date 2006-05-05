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

typedef struct _HippoChatRoom      HippoChatRoom;
typedef struct _HippoChatRoomClass HippoChatRoomClass;

#define HIPPO_TYPE_CHAT_ROOM              (hippo_chat_room_get_type ())
#define HIPPO_CHAT_ROOM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CHAT_ROOM, HippoChatRoom))
#define HIPPO_CHAT_ROOM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))
#define HIPPO_IS_CHAT_ROOM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_IS_CHAT_ROOM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_CHAT_ROOM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))

GType        	 hippo_chat_room_get_type               (void) G_GNUC_CONST;

HippoChatRoom*   hippo_chat_room_new                    (const char *chat_id);

const char*       hippo_chat_room_get_id                  (HippoChatRoom  *room);
HippoChatState    hippo_chat_room_get_state               (HippoChatRoom  *room);
void              hippo_chat_room_set_title               (HippoChatRoom  *room,
                                                           const char     *title);
const char*       hippo_chat_room_get_title               (HippoChatRoom  *room);
HippoChatMessage* hippo_chat_room_get_last_message        (HippoChatRoom  *room);
/* need to unref each element and free the list when done with this */
GSList*           hippo_chat_room_get_users               (HippoChatRoom  *room);
void              hippo_chat_room_set_state               (HippoChatRoom  *room,
                                                           HippoChatState  state);
int               hippo_chat_room_get_viewing_user_count  (HippoChatRoom  *room);
int               hippo_chat_room_get_chatting_user_count (HippoChatRoom  *room);

/* === Methods used by HippoConnection to keep chat room updated === */

/* Set while we are using a <details/> IQ to fill a chatroom we aren't part of */
gboolean hippo_chat_room_get_filling             (HippoChatRoom *room);
void     hippo_chat_room_set_filling             (HippoChatRoom *room,
                                                  gboolean       filling);
void     hippo_chat_room_set_user_state          (HippoChatRoom *room,
                                                  HippoPerson   *person,
                                                  HippoChatState state);
void     hippo_chat_room_add_message             (HippoChatRoom *room,
                                                  HippoPerson   *sender,
                                                  const char    *text,
                                                  GTime          timestamp,
                                                  int            serial);
/* Called on reconnect, since users and messages will be resent */
void     hippo_chat_room_clear                   (HippoChatRoom *room);

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
