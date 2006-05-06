#include "hippo-data-cache.h"

static void      hippo_data_cache_init                (HippoDataCache       *cache);
static void      hippo_data_cache_class_init          (HippoDataCacheClass  *klass);

static void      hippo_data_cache_finalize            (GObject              *object);

static void      hippo_data_cache_on_authenticate     (HippoConnection      *connection,
                                                       void                 *data);

struct _HippoDataCache {
    GObject          parent;
    HippoConnection *connection;
    GHashTable      *posts;
    GHashTable      *entities;
    GHashTable      *group_chats;
    HippoPerson     *cached_self;
};

struct _HippoDataCacheClass {
    GObjectClass parent_class;

};

enum {
    POST_ADDED,
    POST_REMOVED,
    ENTITY_ADDED,
    ENTITY_REMOVED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(HippoDataCache, hippo_data_cache, G_TYPE_OBJECT);
                       
static void
hippo_data_cache_init(HippoDataCache *cache)
{
    cache->posts = g_hash_table_new_full(g_str_hash, g_str_equal,
                                         g_free, (GFreeFunc) g_object_unref);
    cache->entities = g_hash_table_new_full(g_str_hash, g_str_equal,
                                            g_free, (GFreeFunc) g_object_unref);                                            
    cache->group_chats = g_hash_table_new_full(g_str_hash, g_str_equal,
                                               g_free, (GFreeFunc) g_object_unref);                                         
}

static void
hippo_data_cache_class_init(HippoDataCacheClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    signals[POST_ADDED] =
        g_signal_new ("post-added",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[POST_REMOVED] =
        g_signal_new ("post-removed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[ENTITY_ADDED] =
        g_signal_new ("entity-added",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[ENTITY_REMOVED] =
        g_signal_new ("entity-removed",
            		  G_TYPE_FROM_CLASS (object_class),
            		  G_SIGNAL_RUN_LAST,
            		  0,
            		  NULL, NULL,
            		  g_cclosure_marshal_VOID__OBJECT,
            		  G_TYPE_NONE, 1, G_TYPE_OBJECT);

    object_class->finalize = hippo_data_cache_finalize;
}

static void
hippo_data_cache_finalize(GObject *object)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(object);

    /* FIXME need to emit signals for these things going away here, POST_REMOVED/ENTITY_REMOVED */
    g_hash_table_destroy(cache->posts);
    g_hash_table_destroy(cache->group_chats);
    
    /* destroy entities after stuff pointing to entities */
    g_hash_table_destroy(cache->entities);

    if (cache->cached_self) {
        g_object_unref(cache->cached_self);
        cache->cached_self = NULL;
    }

    g_signal_handlers_disconnect_by_func(cache->connection,
                                 G_CALLBACK(hippo_data_cache_on_authenticate), cache);
    hippo_connection_set_cache(cache->connection, NULL);
    g_object_unref(cache->connection);
    
    G_OBJECT_CLASS(hippo_data_cache_parent_class)->finalize(object); 
}

HippoDataCache*
hippo_data_cache_new(HippoConnection *connection)
{
    HippoDataCache *cache = g_object_new(HIPPO_TYPE_DATA_CACHE, NULL);

    cache->connection = connection;
    g_object_ref(cache->connection);
    hippo_connection_set_cache(cache->connection, cache);

    g_signal_connect(cache->connection, "auth-success",
                     G_CALLBACK(hippo_data_cache_on_authenticate), cache);

    return cache;
}

void
hippo_data_cache_add_post(HippoDataCache *cache,
                          HippoPost      *post)
{
    g_return_if_fail(hippo_data_cache_lookup_post(cache, hippo_post_get_guid(post)) == NULL);

    g_object_ref(post);
    g_hash_table_replace(cache->posts, g_strdup(hippo_post_get_guid(post)), post);
    g_signal_emit(cache, signals[POST_ADDED], 0, post);
}

void
hippo_data_cache_add_entity(HippoDataCache *cache,
                            HippoEntity    *entity)
{
    g_return_if_fail(hippo_data_cache_lookup_entity(cache, hippo_entity_get_guid(entity)) == NULL);

    g_object_ref(entity);
    g_hash_table_replace(cache->entities, g_strdup(hippo_entity_get_guid(entity)), entity);
    g_signal_emit(cache, signals[ENTITY_ADDED], 0, entity);    
}

HippoEntity*
hippo_data_cache_ensure_bare_entity(HippoDataCache *cache,
                                    HippoEntityType type,
                                    const char     *guid)
{
    HippoEntity* entity;
    
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
    g_return_val_if_fail(guid != NULL, NULL);
    
    entity = hippo_data_cache_lookup_entity(cache, guid);
    if (entity == NULL) {
        entity = hippo_entity_new(type, guid);
        hippo_data_cache_add_entity(cache, entity);
        g_object_unref(entity);
    }
    return entity;
}                                    

HippoPost*
hippo_data_cache_lookup_post(HippoDataCache  *cache, 
                             const char      *guid)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    return g_hash_table_lookup(cache->posts, guid);
}

HippoEntity*
hippo_data_cache_lookup_entity(HippoDataCache  *cache,
                               const char      *guid)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
   
    return g_hash_table_lookup(cache->entities, guid);
}

typedef struct {
    GTimeVal now;
    GSList *list;
} ListRecentPostsData;

static void
list_recent_posts(void *key, void *value, void *data)
{   
    ListRecentPostsData *lrpd = data;
    HippoPost *post = HIPPO_POST(value);
    GTime postDate = hippo_post_get_date(post);
    
    if ((postDate + (60 * 60 * 24)) > lrpd->now.tv_sec) {
        lrpd->list = g_slist_prepend(lrpd->list, post);
        g_object_ref(post);
    }
}

static int
post_date_compare(const void *a, const void *b)
{
    GTime aTime = hippo_post_get_date(HIPPO_POST(a));
    GTime bTime = hippo_post_get_date(HIPPO_POST(b));
    if (aTime > bTime)
        return 1;
    else if (aTime < bTime)
        return -1;
    else
        return 0; 
}

GSList*
hippo_data_cache_get_recent_posts(HippoDataCache  *cache)
{
    ListRecentPostsData lrpd;
    
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    g_get_current_time(&lrpd.now);
    lrpd.list = NULL;

    /* ref's each post */
    g_hash_table_foreach(cache->posts, list_recent_posts, &lrpd);
    
    lrpd.list = g_slist_sort(lrpd.list, post_date_compare);
    return lrpd.list;
}

HippoChatRoom*
hippo_data_cache_lookup_chat_room(HippoDataCache  *cache,
                                  const char      *chat_id,
                                  HippoChatKind   *kind_p)
{
    HippoChatRoom *room;
    HippoPost *post;

    *kind_p = -1; /* more reliably detect bugs */

    post = hippo_data_cache_lookup_post(cache, chat_id);
    if (post)
        room = hippo_post_get_chat_room(post);
    else
        room = NULL;
        
    if (room != NULL) {
        *kind_p = HIPPO_CHAT_POST;
        return room;
    }
    
    room = g_hash_table_lookup(cache->group_chats, chat_id);
    if (room != NULL) {
        *kind_p = HIPPO_CHAT_GROUP;
        return room;
    }
    
    return room;
}                                  

HippoChatRoom*
hippo_data_cache_ensure_chat_room(HippoDataCache  *cache,
                                  const char      *chat_id,
                                  HippoChatKind    kind)
{
    if (kind == HIPPO_CHAT_POST) {
        HippoPost *post;
        HippoChatRoom *room;
        gboolean created_post;
        
        post = hippo_data_cache_lookup_post(cache, chat_id);
        if (post) {
            /* can return NULL, right now only when a post is first 
             * created since we quickly ask for the chat room on all posts
             */
            room = hippo_post_get_chat_room(post);
            if (room)
                g_object_ref(room);
        } else {
            room = NULL;
        }

        if (room == NULL) /* no post, or post has no room yet */
            room = hippo_chat_room_new(chat_id, kind);
            
        g_assert(room != NULL);    
            
        if (post == NULL) {
            post = hippo_post_new(chat_id);
            created_post = TRUE;
        } else {
            created_post = FALSE;
        }
        
        /* no-op if the room is already set on the post */
        hippo_post_set_chat_room(post, room);
        
        g_assert(post != NULL);
        g_assert(hippo_post_get_chat_room(post) == room);
        
        if (created_post)
            hippo_data_cache_add_post(cache, post);

        g_object_unref(room); /* post still holds a ref so it won't be finalized */
        return room;
    } else if (kind == HIPPO_CHAT_GROUP) {
        HippoChatRoom *room;
        
        room = g_hash_table_lookup(cache->group_chats, chat_id);
        if (room == NULL) {
            room = hippo_chat_room_new(chat_id, HIPPO_CHAT_GROUP);
            g_hash_table_replace(cache->group_chats, g_strdup(chat_id), room);
            g_object_unref(room); /* hash table still holds ref */
        }
        return room;
    } else {
        g_assert_not_reached();
        return NULL;
    }
}                              

typedef void (* HippoChatRoomFunc) (HippoChatRoom *room,
                                    void          *data);

typedef struct {
    HippoChatRoomFunc  func;
    void              *data;
} ChatRoomClosure;

static void
apply_closure(HippoChatRoom   *room,
              ChatRoomClosure *closure)
{
    g_object_ref(room);

    (closure->func)(room, closure->data);

    g_object_unref(room);
}              

static void
foreach_post_chat_room_func(void *key, void *value, void *data)
{
    HippoPost *post = HIPPO_POST(value);
    ChatRoomClosure *closure = data;
    HippoChatRoom *room;
    
    room = hippo_post_get_chat_room(post);
    if (room) {
        apply_closure(room, closure);
    }
}

static void
foreach_group_chat_room_func(void *key, void *value, void *data)
{
    HippoChatRoom *room = HIPPO_CHAT_ROOM(value);
    ChatRoomClosure *closure = data;
    apply_closure(room, closure);
}

static void
hippo_data_cache_foreach_chat_room (HippoDataCache    *cache,
                                    HippoChatRoomFunc  func,
                                    void              *data)
{
    ChatRoomClosure closure;
    
    closure.func = func;
    closure.data = data;
    
    g_object_ref(cache);
    
    g_hash_table_foreach(cache->posts, foreach_post_chat_room_func, &closure);
    g_hash_table_foreach(cache->group_chats, foreach_group_chat_room_func, &closure);
    
    g_object_unref(cache);
}                                                                       

static void
update_chat_room_func(HippoChatRoom *room,
                      void          *data)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(data);
    HippoChatState state;

    /* We must get our previous state before clearing the chat room of users */
    state = hippo_chat_room_get_user_state(room, cache->cached_self);
    
    /* Now re-enter the room with the same state we had before */
    hippo_chat_room_clear(room);
    if (state != HIPPO_CHAT_NONMEMBER) {
        hippo_connection_send_chat_room_enter(cache->connection, room, state);
    }
}

/* FIXME this is kind of broken; really we need to blow up the whole cache
 * anytime we reconnect, because it might be a different server, or the 
 * database might have been modified, or whatever. guids may not even be 
 * valid anymore.
 */
static void
hippo_data_cache_on_authenticate(HippoConnection      *connection,
                                 void                 *data)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(data);
    const char *self_id;
    
    self_id = hippo_connection_get_self_guid(cache->connection);
    
    /* This should not be NULL, since we authenticated. It can be 
     * NULL if called when not authenticated.
     */
    g_assert(self_id != NULL);

    /* note that cached_self may change to a different person each time we auth */
    cache->cached_self = HIPPO_PERSON(hippo_data_cache_ensure_bare_entity(cache, HIPPO_ENTITY_PERSON, self_id));

    /* This only matters the second time we authenticate. 
     * For each chat room we were previously in, rejoin it.
     */
     hippo_data_cache_foreach_chat_room(cache, update_chat_room_func, cache);
}
