#include "hippo-ipc-source.h"
#include <hippo/hippo-common-marshal.h>

static void hippo_ipc_source_finalize(GObject *object);

struct _HippoIpcSource {
    GObject parent_instance;

    HippoDataCache *data_cache;
    guint64 id;

    GSList *visitor_rooms;
    GSList *participant_rooms;
};

struct _HippoIpcSourceClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoIpcSource, hippo_ipc_source, G_TYPE_OBJECT);

enum {
    USER_JOIN,
    USER_LEAVE,
    MESSAGE,
    RECONNECT,
    USER_INFO,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

static guint64 last_id;

static void
hippo_ipc_source_init(HippoIpcSource *source)
{
    source->id = ++last_id;
}

static void
hippo_ipc_source_class_init(HippoIpcSourceClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);
  
    signals[USER_JOIN] =
	g_signal_new("user-join",
		     G_TYPE_FROM_CLASS(object_class),
		     G_SIGNAL_RUN_LAST,
		     0,
		     NULL, NULL,
		     hippo_common_marshal_VOID__OBJECT_OBJECT,
		     G_TYPE_NONE, 2, G_TYPE_OBJECT, G_TYPE_OBJECT);
    signals[USER_LEAVE] = 
	g_signal_new("user-leave",
		     G_TYPE_FROM_CLASS(object_class),
		     G_SIGNAL_RUN_LAST,
		     0,
		     NULL, NULL,
		     hippo_common_marshal_VOID__OBJECT_OBJECT,
		     G_TYPE_NONE, 2, G_TYPE_OBJECT, G_TYPE_OBJECT);
    signals[MESSAGE] = 
	g_signal_new("message",
		     G_TYPE_FROM_CLASS(object_class),
		     G_SIGNAL_RUN_LAST,
		     0,
		     NULL, NULL,
		     hippo_common_marshal_VOID__OBJECT_POINTER,
		     G_TYPE_NONE, 2, G_TYPE_OBJECT, G_TYPE_POINTER);
    signals[RECONNECT] = 
	g_signal_new("reconnect",
		     G_TYPE_FROM_CLASS(object_class),
		     G_SIGNAL_RUN_LAST,
		     0,
		     NULL, NULL,
		     hippo_common_marshal_VOID__OBJECT,
		     G_TYPE_NONE, 1, G_TYPE_OBJECT);
    signals[USER_INFO] = 
	g_signal_new("user-info",
		     G_TYPE_FROM_CLASS(object_class),
		     G_SIGNAL_RUN_LAST,
		     0,
		     NULL, NULL,
		     hippo_common_marshal_VOID__OBJECT,
		     G_TYPE_NONE, 1, G_TYPE_OBJECT);

    object_class->finalize = hippo_ipc_source_finalize;
}

static void
hippo_ipc_source_finalize(GObject *object)
{
}

HippoIpcSource *
hippo_ipc_source_new (HippoDataCache *data_cache)
{
    HippoIpcSource *source;
    
    g_return_val_if_fail (HIPPO_IS_DATA_CACHE(data_cache), NULL);
    
    source = g_object_new(HIPPO_TYPE_IPC_SOURCE, NULL);

    /* No strong reference, avoid refcount cycles */
    source->data_cache = data_cache;

    return source;
}

guint64
hippo_ipc_source_get_id (HippoIpcSource *source)
{
    return source->id;
}

void
hippo_ipc_source_connect (HippoIpcSource *source)
{
}

static void
on_room_user_state_changed(HippoChatRoom  *room,
			   HippoPerson    *person,
			   HippoIpcSource *source)
{
    HippoChatState new_state = hippo_chat_room_get_user_state(room, person);

    if (new_state == HIPPO_CHAT_STATE_PARTICIPANT) {
	g_signal_emit(source, signals[USER_JOIN], 0, room, person);
    } else {
	g_signal_emit(source, signals[USER_LEAVE], 0, room, person);
    }
}

static void
on_room_message_added(HippoChatRoom    *room,
		      HippoChatMessage *message,
                      HippoIpcSource   *source)
{
    g_signal_emit(source, signals[MESSAGE], 0, room, message);
}

static void
on_room_cleared(HippoChatRoom  *room,
		HippoIpcSource *source)
{
    g_signal_emit(source, signals[RECONNECT], 0, room);
}

static void
add_to_room_list(HippoIpcSource *source,
		 GSList        **list,
		 HippoChatState  state,
		 HippoChatRoom  *room)
{
    *list = g_slist_append(*list, room);
    hippo_chat_room_increment_state_count(room, state);

    g_signal_connect(room, "user-state-changed",
		     G_CALLBACK(on_room_user_state_changed), source);
    g_signal_connect(room, "message-added",
		     G_CALLBACK(on_room_message_added), source);
    g_signal_connect(room, "cleared",
		     G_CALLBACK(on_room_cleared), source);
}

static void
remove_from_room_list(HippoIpcSource *source,
		      GSList        **list,
		      HippoChatState  state,
                      HippoChatRoom  *room)
{
    if (g_slist_find(*list, room)) {
	*list = g_slist_remove(*list, room);
	hippo_chat_room_decrement_state_count(room, state);

	g_signal_handlers_disconnect_by_func(room, (void *)on_room_user_state_changed, source);
	g_signal_handlers_disconnect_by_func(room, (void *)on_room_message_added, source);
	g_signal_handlers_disconnect_by_func(room, (void *)on_room_cleared, source);
    }
}

void
hippo_ipc_source_disconnect (HippoIpcSource *source)
{
    g_return_if_fail (HIPPO_IS_IPC_SOURCE(source));

    while (source->visitor_rooms) {
	remove_from_room_list(source, &source->visitor_rooms,
			      HIPPO_CHAT_STATE_VISITOR, source->visitor_rooms->data);
    }
    while (source->participant_rooms) {
	remove_from_room_list(source, &source->participant_rooms,
			      HIPPO_CHAT_STATE_PARTICIPANT, source->participant_rooms->data);
    }
}

void    
hippo_ipc_source_join_chat_room (HippoIpcSource *source,
 		   	         const char     *chat_id,
			         HippoChatState  state)
{
    HippoChatRoom *room;
    
    g_return_if_fail (HIPPO_IS_IPC_SOURCE(source));
    g_return_if_fail (state == HIPPO_CHAT_STATE_VISITOR || state == HIPPO_CHAT_STATE_PARTICIPANT);

    hippo_ipc_source_leave_chat_room(source, chat_id);

    room = hippo_data_cache_ensure_chat_room(source->data_cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);

    if (state == HIPPO_CHAT_STATE_VISITOR) {
	add_to_room_list(source, &source->visitor_rooms, HIPPO_CHAT_STATE_VISITOR, room);
    } else if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
	add_to_room_list(source, &source->participant_rooms, HIPPO_CHAT_STATE_PARTICIPANT, room);
    }
}

void
hippo_ipc_source_leave_chat_room (HippoIpcSource *source,
				  const char     *chat_id)
{
    HippoChatRoom *room;
    
    g_return_if_fail (HIPPO_IS_IPC_SOURCE(source));

    room = hippo_data_cache_lookup_chat_room(source->data_cache, chat_id, NULL);
    if (!room)
	return;

    remove_from_room_list(source, &source->visitor_rooms, HIPPO_CHAT_STATE_VISITOR, room);
    remove_from_room_list(source, &source->participant_rooms, HIPPO_CHAT_STATE_PARTICIPANT, room);
}
