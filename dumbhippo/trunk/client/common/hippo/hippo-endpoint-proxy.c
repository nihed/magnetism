#include "hippo-endpoint-proxy.h"
#include <hippo/hippo-common-marshal.h>

static void hippo_endpoint_proxy_finalize(GObject *object);

struct _HippoEndpointProxy {
    GObject parent_instance;

    HippoDataCache *data_cache;
    guint64 id;

    GSList *visitor_rooms;
    GSList *participant_rooms;
};

struct _HippoEndpointProxyClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoEndpointProxy, hippo_endpoint_proxy, G_TYPE_OBJECT);

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
hippo_endpoint_proxy_init(HippoEndpointProxy *proxy)
{
    proxy->id = ++last_id;
}

static void
hippo_endpoint_proxy_class_init(HippoEndpointProxyClass *klass)
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

    object_class->finalize = hippo_endpoint_proxy_finalize;
}

static void
hippo_endpoint_proxy_finalize(GObject *object)
{
}

HippoEndpointProxy *
hippo_endpoint_proxy_new (HippoDataCache *data_cache)
{
    HippoEndpointProxy *proxy;
    
    g_return_val_if_fail (HIPPO_IS_DATA_CACHE(data_cache), NULL);
    
    proxy = g_object_new(HIPPO_TYPE_ENDPOINT_PROXY, NULL);

    /* No strong reference, avoid refcount cycles */
    proxy->data_cache = data_cache;

    return proxy;
}

guint64
hippo_endpoint_proxy_get_id (HippoEndpointProxy *proxy)
{
    return proxy->id;
}

void
hippo_endpoint_proxy_connect (HippoEndpointProxy *proxy)
{
}

static void
on_room_user_state_changed(HippoChatRoom      *room,
			   HippoPerson        *person,
			   HippoEndpointProxy *proxy)
{
    HippoChatState new_state = hippo_chat_room_get_user_state(room, person);

    if (new_state == HIPPO_CHAT_STATE_PARTICIPANT) {
	g_signal_emit(proxy, signals[USER_JOIN], 0, room, person);
    } else {
	g_signal_emit(proxy, signals[USER_LEAVE], 0, room, person);
    }
}

static void
on_room_message_added(HippoChatRoom      *room,
		      HippoChatMessage   *message,
                      HippoEndpointProxy *proxy)
{
    g_signal_emit(proxy, signals[MESSAGE], 0, room, message);
}

static void
on_room_cleared(HippoChatRoom      *room,
		HippoEndpointProxy *proxy)
{
    g_signal_emit(proxy, signals[RECONNECT], 0, room);
}

static void
add_to_room_list(HippoEndpointProxy *proxy,
		 GSList            **list,
		 HippoChatState      state,
		 HippoChatRoom      *room)
{
    *list = g_slist_append(*list, room);
    hippo_chat_room_increment_state_count(room, state);

    g_signal_connect(room, "user-state-changed",
		     G_CALLBACK(on_room_user_state_changed), proxy);
    g_signal_connect(room, "message-added",
		     G_CALLBACK(on_room_message_added), proxy);
    g_signal_connect(room, "cleared",
		     G_CALLBACK(on_room_cleared), proxy);
}

static void
remove_from_room_list(HippoEndpointProxy *proxy,
		      GSList            **list,
		      HippoChatState      state,
                      HippoChatRoom      *room)
{
    if (g_slist_find(*list, room)) {
	*list = g_slist_remove(*list, room);
	hippo_chat_room_decrement_state_count(room, state);

	g_signal_handlers_disconnect_by_func(room, (void *)on_room_user_state_changed, proxy);
	g_signal_handlers_disconnect_by_func(room, (void *)on_room_message_added, proxy);
	g_signal_handlers_disconnect_by_func(room, (void *)on_room_cleared, proxy);
    }
}

void
hippo_endpoint_proxy_disconnect (HippoEndpointProxy *proxy)
{
    g_return_if_fail (HIPPO_IS_ENDPOINT_PROXY(proxy));

    while (proxy->visitor_rooms) {
	remove_from_room_list(proxy, &proxy->visitor_rooms,
			      HIPPO_CHAT_STATE_VISITOR, proxy->visitor_rooms->data);
    }
    while (proxy->participant_rooms) {
	remove_from_room_list(proxy, &proxy->participant_rooms,
			      HIPPO_CHAT_STATE_PARTICIPANT, proxy->participant_rooms->data);
    }
}

void    
hippo_endpoint_proxy_join_chat_room (HippoEndpointProxy *proxy,
				     const char         *chat_id,
				     HippoChatState      state)
{
    HippoChatRoom *room;
    
    g_return_if_fail (HIPPO_IS_ENDPOINT_PROXY(proxy));
    g_return_if_fail (state == HIPPO_CHAT_STATE_VISITOR || state == HIPPO_CHAT_STATE_PARTICIPANT);

    hippo_endpoint_proxy_leave_chat_room(proxy, chat_id);

    room = hippo_data_cache_ensure_chat_room(proxy->data_cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);

    if (state == HIPPO_CHAT_STATE_VISITOR) {
	add_to_room_list(proxy, &proxy->visitor_rooms, HIPPO_CHAT_STATE_VISITOR, room);
    } else if (state == HIPPO_CHAT_STATE_PARTICIPANT) {
	add_to_room_list(proxy, &proxy->participant_rooms, HIPPO_CHAT_STATE_PARTICIPANT, room);
    }
}

void
hippo_endpoint_proxy_leave_chat_room (HippoEndpointProxy *proxy,
				      const char         *chat_id)
{
    HippoChatRoom *room;
    
    g_return_if_fail (HIPPO_IS_ENDPOINT_PROXY(proxy));

    room = hippo_data_cache_lookup_chat_room(proxy->data_cache, chat_id, NULL);
    if (!room)
	return;

    remove_from_room_list(proxy, &proxy->visitor_rooms, HIPPO_CHAT_STATE_VISITOR, room);
    remove_from_room_list(proxy, &proxy->participant_rooms, HIPPO_CHAT_STATE_PARTICIPANT, room);
}
