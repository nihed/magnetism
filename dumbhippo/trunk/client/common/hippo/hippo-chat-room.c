#include "hippo-chat-room.h"

static void      hippo_chat_room_init                (HippoChatRoom       *room);
static void      hippo_chat_room_class_init          (HippoChatRoomClass  *klass);


struct _HippoChatRoom {
    GObject parent;
};

struct _HippoChatRoomClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoChatRoom, hippo_chat_room, G_TYPE_OBJECT);
                       
static void
hippo_chat_room_init(HippoChatRoom *room)
{

}

static void
hippo_chat_room_class_init(HippoChatRoomClass *klass)
{


}

HippoChatRoom*
hippo_chat_room_new(void)
{
    HippoChatRoom *room = g_object_new(HIPPO_TYPE_CHAT_ROOM, NULL);

    return room;
}
