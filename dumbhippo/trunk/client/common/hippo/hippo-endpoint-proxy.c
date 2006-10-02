/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-endpoint-proxy.h"
#include "hippo-common-marshal.h"

static void hippo_endpoint_proxy_finalize(GObject *object);

struct _HippoEndpointProxy {
    GObject parent_instance;

    HippoDataCache *data_cache;
    guint64 id;

    GSList *visitor_rooms;
    GSList *participant_rooms;

    /* entities this endpoint cares about */
    GHashTable *entities;

    guint unregistered : 1;
};

struct _HippoEndpointProxyClass {
    GObjectClass parent_class;
};

G_DEFINE_TYPE(HippoEndpointProxy, hippo_endpoint_proxy, G_TYPE_OBJECT);

enum {
    USER_JOIN,
    USER_LEAVE,
    MESSAGE,
    ENTITY_INFO,
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
             hippo_common_marshal_VOID__OBJECT_OBJECT_BOOLEAN,
             G_TYPE_NONE, 3, G_TYPE_OBJECT, G_TYPE_OBJECT, G_TYPE_BOOLEAN);
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
    signals[ENTITY_INFO] = 
    g_signal_new("entity-info",
             G_TYPE_FROM_CLASS(object_class),
             G_SIGNAL_RUN_LAST,
             0,
             NULL, NULL,
             hippo_common_marshal_VOID__OBJECT,
             G_TYPE_NONE, 1, G_TYPE_OBJECT);

    object_class->finalize = hippo_endpoint_proxy_finalize;
}

static void
emit_entity_info(HippoEndpointProxy  *proxy,
                 HippoEntity         *entity)
{
    g_signal_emit(G_OBJECT(proxy), signals[ENTITY_INFO], 0, entity);
}

static void
on_entity_changed(HippoEntity *entity,
                  void        *data)
{
    HippoEndpointProxy *proxy;

    proxy = HIPPO_ENDPOINT_PROXY(data);

    emit_entity_info(proxy, entity);
}

static gboolean
entity_remove_foreach(void *key,
                      void *value,
                      void *data)
{
    HippoEndpointProxy *proxy;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(value);
    proxy = HIPPO_ENDPOINT_PROXY(data);

    g_signal_handlers_disconnect_by_func(G_OBJECT(entity), 
                                         G_CALLBACK(on_entity_changed),
                                         proxy);
    
    g_object_unref(entity);

    /* remove! */
    return TRUE;
}

static void
clear_entities(HippoEndpointProxy *proxy)
{
    g_hash_table_foreach_remove(proxy->entities, entity_remove_foreach, proxy);
    g_assert(g_hash_table_size(proxy->entities) == 0);
}

static void
hippo_endpoint_proxy_finalize(GObject *object)
{
    HippoEndpointProxy *proxy;

    proxy = HIPPO_ENDPOINT_PROXY(object);

    g_return_if_fail(proxy->unregistered);
    
    g_assert(proxy->visitor_rooms == NULL);
    g_assert(proxy->participant_rooms == NULL);
    g_assert(g_hash_table_size(proxy->entities) == 0);
    
    g_hash_table_destroy(proxy->entities);

    G_OBJECT_CLASS(hippo_endpoint_proxy_parent_class)->finalize(object);
}

static void
track_entity(HippoEndpointProxy *proxy,
             HippoEntity        *entity)
{
    if (g_hash_table_lookup(proxy->entities, hippo_entity_get_guid(entity)) != NULL)
        return;

    g_signal_connect(G_OBJECT(entity), "changed",
                     G_CALLBACK(on_entity_changed), proxy);
    g_object_ref(entity);
    g_hash_table_replace(proxy->entities, (char*) hippo_entity_get_guid(entity), entity);

    /* emit the entity info for the first time */
    emit_entity_info(proxy, entity);
}

static void
on_connected_changed(HippoConnection *connection,
                     gboolean         connected,
                     void            *data)
{
    HippoEndpointProxy *proxy = HIPPO_ENDPOINT_PROXY(data);

    if (connected) {
        HippoPerson *self = hippo_data_cache_get_self(proxy->data_cache);
        track_entity(proxy, HIPPO_ENTITY(self));
    } else {
        hippo_endpoint_proxy_unregister(proxy);
    }
}

HippoEndpointProxy *
hippo_endpoint_proxy_new (HippoDataCache *data_cache)
{
    HippoEndpointProxy *proxy;
    
    g_return_val_if_fail (HIPPO_IS_DATA_CACHE(data_cache), NULL);
    
    proxy = g_object_new(HIPPO_TYPE_ENDPOINT_PROXY, NULL);

    /* No strong reference, avoid refcount cycles */
    proxy->data_cache = data_cache;

    proxy->entities = g_hash_table_new(g_str_hash, g_str_equal);

    g_signal_connect(G_OBJECT(hippo_data_cache_get_connection(data_cache)),
                     "connected-changed",
                     G_CALLBACK(on_connected_changed), proxy);
    
    return proxy;
}

guint64
hippo_endpoint_proxy_get_id (HippoEndpointProxy *proxy)
{
    return proxy->id;
}

static void
on_room_user_state_changed(HippoChatRoom      *room,
               HippoPerson        *person,
               HippoEndpointProxy *proxy)
{
    HippoChatState new_state = hippo_chat_room_get_user_state(room, person);

    track_entity(proxy, HIPPO_ENTITY(person));
    
    if (new_state != HIPPO_CHAT_STATE_NONMEMBER) {
    g_signal_emit(proxy, signals[USER_JOIN], 0, room, person, new_state == HIPPO_CHAT_STATE_PARTICIPANT);
    } else {
    g_signal_emit(proxy, signals[USER_LEAVE], 0, room, person);
    }
}

static void
on_room_message_added(HippoChatRoom      *room,
              HippoChatMessage   *message,
                      HippoEndpointProxy *proxy)
{
    HippoPerson *person;
    person = hippo_chat_message_get_person(message);
    track_entity(proxy, HIPPO_ENTITY(person));
    g_signal_emit(proxy, signals[MESSAGE], 0, room, message);
}

static void
add_to_room_list(HippoEndpointProxy *proxy,
         GSList            **list,
         HippoChatState      state,
         HippoChatRoom      *room)
{
    gboolean already_there_once = g_slist_find(*list, room) != NULL;
    
    *list = g_slist_append(*list, room);
    hippo_connection_join_chat_room(hippo_data_cache_get_connection(proxy->data_cache), room, state);

    g_signal_connect(room, "user-state-changed",
             G_CALLBACK(on_room_user_state_changed), proxy);
    g_signal_connect(room, "message-added",
             G_CALLBACK(on_room_message_added), proxy);

    if (!already_there_once) {
        /* Emit the whole current state of the chat room */
        GSList *users;
        GSList *messages;
        GSList *l;
        
        users = hippo_chat_room_get_users(room);
        for (l = users; l != NULL; l = l->next) {
            HippoPerson *person = HIPPO_PERSON(l->data);
            on_room_user_state_changed(room, person, proxy);
            g_object_unref(person);
        }
        g_slist_free(users);

        // It's unfriendly to send the messages in reverse chronological order,
        // as they are stored in room->messages, so reverse them before replaying
        messages = hippo_chat_room_get_messages(room);
        messages = g_slist_reverse(g_slist_copy(messages));
        
        for (l = messages; l != NULL; l = l->next) {
            on_room_message_added(room, l->data, proxy);
        }
        
        g_slist_free(messages);
    }
}

static void
remove_from_room_list(HippoEndpointProxy *proxy,
              GSList            **list,
              HippoChatState      state,
                      HippoChatRoom      *room)
{
    if (g_slist_find(*list, room)) {
    *list = g_slist_remove(*list, room);
    hippo_connection_leave_chat_room(hippo_data_cache_get_connection(proxy->data_cache), room, state);

    g_signal_handlers_disconnect_by_func(room, (void *)on_room_user_state_changed, proxy);
    g_signal_handlers_disconnect_by_func(room, (void *)on_room_message_added, proxy);
    }
}

void
hippo_endpoint_proxy_unregister (HippoEndpointProxy *proxy)
{
    g_return_if_fail (HIPPO_IS_ENDPOINT_PROXY(proxy));

    if (proxy->unregistered)
        return;
    
    while (proxy->visitor_rooms) {
    remove_from_room_list(proxy, &proxy->visitor_rooms,
                  HIPPO_CHAT_STATE_VISITOR, proxy->visitor_rooms->data);
    }
    while (proxy->participant_rooms) {
    remove_from_room_list(proxy, &proxy->participant_rooms,
                  HIPPO_CHAT_STATE_PARTICIPANT, proxy->participant_rooms->data);
    }
    
    clear_entities(proxy);

    g_signal_handlers_disconnect_by_func(G_OBJECT(hippo_data_cache_get_connection(proxy->data_cache)), 
                                         G_CALLBACK(on_connected_changed),
                                         proxy);

    proxy->unregistered = TRUE;
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
