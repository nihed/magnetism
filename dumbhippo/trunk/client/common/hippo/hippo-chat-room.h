#ifndef __HIPPO_CHAT_ROOM_H__
#define __HIPPO_CHAT_ROOM_H__

#include <hippo/hippo-connection.h>

G_BEGIN_DECLS

typedef struct _HippoChatRoom      HippoChatRoom;
typedef struct _HippoChatRoomClass HippoChatRoomClass;

#define HIPPO_TYPE_CHAT_ROOM              (hippo_chat_room_get_type ())
#define HIPPO_CHAT_ROOM(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_CHAT_ROOM, HippoChatRoom))
#define HIPPO_CHAT_ROOM_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))
#define HIPPO_IS_CHAT_ROOM(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_IS_CHAT_ROOM_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_CHAT_ROOM))
#define HIPPO_CHAT_ROOM_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_CHAT_ROOM, HippoChatRoomClass))

GType        	 hippo_chat_room_get_type               (void) G_GNUC_CONST;

HippoChatRoom*  hippo_chat_room_new                     (void);

G_END_DECLS

#endif /* __HIPPO_CHAT_ROOM_H__ */
