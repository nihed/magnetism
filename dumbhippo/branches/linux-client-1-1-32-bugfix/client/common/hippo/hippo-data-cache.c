/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-data-cache-internal.h"
#include "hippo-connection.h"
#include "hippo-group.h"
#include "hippo-block-post.h"
#include "hippo-xml-utils.h"
#include <string.h>

typedef void (* HippoChatRoomFunc) (HippoChatRoom *room,
                                    void          *data);

static void      hippo_data_cache_init                (HippoDataCache       *cache);
static void      hippo_data_cache_class_init          (HippoDataCacheClass  *klass);

static void      hippo_data_cache_finalize            (GObject              *object);

static void      hippo_data_cache_foreach_chat_room   (HippoDataCache       *cache,
                                                       gboolean              in_cache_finalize,
                                                       HippoChatRoomFunc     func,
                                                       void                 *data);

static void      hippo_data_cache_on_connect          (HippoConnection      *connection,
                                                       gboolean              connected,
                                                       void                 *data);
static void      hippo_data_cache_on_chat_room_loaded (HippoChatRoom        *chat_room,
                                                       void                 *data);

struct _HippoDataCache {
    GObject          parent;
    HippoConnection *connection;
    GHashTable      *posts;
    GSList          *active_posts;
    GHashTable      *entities;
    GHashTable      *chats;
    GHashTable      *blocks;
    HippoPerson     *cached_self;
    char            *myspace_name;
    GSList          *myspace_contacts;
    GSList          *myspace_comments;
    HippoHotness     hotness;
    unsigned int     music_sharing_enabled : 1;
    unsigned int     music_sharing_primed : 1;    
    HippoClientInfo  client_info;
};

struct _HippoDataCacheClass {
    GObjectClass parent_class;

};

enum {
    POST_ADDED,
    POST_REMOVED,
    ENTITY_ADDED,
    ENTITY_REMOVED,
    BLOCK_ADDED,
    BLOCK_REMOVED,    
    MUSIC_SHARING_CHANGED,
    HOTNESS_CHANGED,
    ACTIVE_POSTS_CHANGED,
    MYSPACE_NAME_CHANGED,
    MYSPACE_COMMENTS_CHANGED,
    MYSPACE_CONTACTS_CHANGED,
    CHAT_ROOM_LOADED,
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
    cache->chats = g_hash_table_new_full(g_str_hash, g_str_equal,
                                         g_free, (GFreeFunc) g_object_unref);
    cache->blocks = g_hash_table_new_full(g_str_hash, g_str_equal,
                                          g_free, (GFreeFunc) g_object_unref);
                                               
    /* these defaults are important to be sure we
     * do nothing until we hear otherwise
     * (and to be sure a signal is emitted if we need to
     * do something, since stuff will have changed)
     */
    cache->music_sharing_enabled = FALSE;
    cache->music_sharing_primed = TRUE;
    
    cache->hotness = HIPPO_HOTNESS_UNKNOWN;                                               
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

    signals[BLOCK_ADDED] =
        g_signal_new ("block-added",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, G_TYPE_OBJECT);

    signals[BLOCK_REMOVED] =
        g_signal_new ("block-removed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, G_TYPE_OBJECT);
    
    signals[MUSIC_SHARING_CHANGED] =
        g_signal_new ("music-sharing-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[HOTNESS_CHANGED] =
        g_signal_new ("hotness-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__INT,
                      G_TYPE_NONE, 1, G_TYPE_INT);

    signals[ACTIVE_POSTS_CHANGED] =
        g_signal_new ("active-posts-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[MYSPACE_NAME_CHANGED] =
        g_signal_new ("myspace-name-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__STRING,
                      G_TYPE_NONE, 1, G_TYPE_STRING);

    signals[MYSPACE_COMMENTS_CHANGED] =
        g_signal_new ("myspace-comments-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[MYSPACE_CONTACTS_CHANGED] =
        g_signal_new ("myspace-contacts-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[CHAT_ROOM_LOADED] =
        g_signal_new ("chat-room-loaded",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__OBJECT,
                      G_TYPE_NONE, 1, G_TYPE_OBJECT);

    object_class->finalize = hippo_data_cache_finalize;
}

static void
disconnect_chat_room_foreach(HippoChatRoom *chat_room,
                             void          *data)
{
    HippoDataCache *cache = (HippoDataCache *)data;
    g_signal_handlers_disconnect_by_func(chat_room, (void *)hippo_data_cache_on_chat_room_loaded, cache);
}

static void
hippo_data_cache_finalize(GObject *object)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(object);

    g_debug("Finalizing data cache");

    hippo_data_cache_set_myspace_name(cache, NULL);
    hippo_data_cache_set_myspace_blog_comments(cache, NULL);
    hippo_data_cache_set_myspace_contacts(cache, NULL);

    hippo_data_cache_clear_active_posts(cache);

    hippo_data_cache_set_client_info(cache, NULL);

    /* FIXME need to emit signals for these things going away here, POST_REMOVED/ENTITY_REMOVED */

    g_hash_table_destroy(cache->blocks);
    
    g_hash_table_destroy(cache->posts);

    hippo_data_cache_foreach_chat_room(cache, TRUE, disconnect_chat_room_foreach, cache);
    g_hash_table_destroy(cache->chats);
    
    /* destroy entities after stuff pointing to entities */
    g_hash_table_destroy(cache->entities);

    if (cache->cached_self) {
        g_object_unref(cache->cached_self);
        cache->cached_self = NULL;
    }

    g_signal_handlers_disconnect_by_func(cache->connection,
                                         G_CALLBACK(hippo_data_cache_on_connect), cache);
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

    g_signal_connect(cache->connection, "connected-changed",
                     G_CALLBACK(hippo_data_cache_on_connect), cache);

    return cache;
}

HippoConnection*
hippo_data_cache_get_connection(HippoDataCache  *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    return cache->connection;
}

HippoHotness
hippo_data_cache_get_hotness(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), HIPPO_HOTNESS_COLD);
    return cache->hotness;
}

gboolean
hippo_data_cache_get_music_sharing_enabled(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), FALSE);
    return cache->music_sharing_enabled;
}

gboolean
hippo_data_cache_get_need_priming_music(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), FALSE);
    return cache->music_sharing_enabled && !cache->music_sharing_primed;
}

const char*
hippo_data_cache_get_myspace_name(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
    return cache->myspace_name;
}

void
hippo_data_cache_set_hotness(HippoDataCache  *cache,
                             HippoHotness     hotness)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    if (hotness != cache->hotness) {
        HippoHotness old = cache->hotness;
        g_debug("new hotness %s from %s", hippo_hotness_debug_string(hotness),
                hippo_hotness_debug_string(old));
        cache->hotness = hotness;
        g_signal_emit(cache, signals[HOTNESS_CHANGED], 0, old);
    }
}

static void
set_music_sharing(HippoDataCache *cache,
                  gboolean        enabled,
                  gboolean        primed)
{
    gboolean old_enabled;
    gboolean old_need_primed;

    /* note that need_primed is a function of two props (enabled and primed)
     * on the data cache
     */
    old_enabled = hippo_data_cache_get_music_sharing_enabled(cache);
    old_need_primed = hippo_data_cache_get_need_priming_music(cache);
    
    cache->music_sharing_enabled = enabled != FALSE;
    cache->music_sharing_primed = primed != FALSE;
    
    if (old_enabled != hippo_data_cache_get_music_sharing_enabled(cache) ||
        old_need_primed != hippo_data_cache_get_need_priming_music(cache)) {
        g_debug("music sharing changed enabled = %d primed = %d",
                cache->music_sharing_enabled, cache->music_sharing_primed);
        g_signal_emit(cache, signals[MUSIC_SHARING_CHANGED], 0);
    }
}

void
hippo_data_cache_set_music_sharing_enabled(HippoDataCache  *cache,
                                           gboolean         enabled)
{    
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    set_music_sharing(cache, enabled, cache->music_sharing_primed);
}

void
hippo_data_cache_set_music_sharing_primed(HippoDataCache  *cache,
                                          gboolean         primed)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    set_music_sharing(cache, cache->music_sharing_enabled, primed);
}                                          

void
hippo_data_cache_set_myspace_name(HippoDataCache  *cache,
                                  const char      *name)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    if (name == cache->myspace_name) /* catches both NULL, and self-assignment */
        return;
    
    if (!name || !cache->myspace_name || strcmp(name, cache->myspace_name) != 0) {
        g_free(cache->myspace_name);
        cache->myspace_name = g_strdup(name);
        
        g_debug("new myspace name '%s'", cache->myspace_name ? cache->myspace_name : "NULL");
        
        g_signal_emit(cache, signals[MYSPACE_NAME_CHANGED], 0, cache->myspace_name);
    }
}

void
hippo_data_cache_add_post(HippoDataCache *cache,
                          HippoPost      *post)
{
    g_return_if_fail(hippo_data_cache_lookup_post(cache, hippo_post_get_guid(post)) == NULL);

    g_object_ref(post);
    g_hash_table_replace(cache->posts, g_strdup(hippo_post_get_guid(post)), post);
    
    g_debug("Post %s added, emitting post-added", hippo_post_get_guid(post));

    g_object_ref(post);
    g_signal_emit(cache, signals[POST_ADDED], 0, post);
    g_object_unref(post);
}

void
hippo_data_cache_add_entity(HippoDataCache *cache,
                            HippoEntity    *entity)
{
    HippoChatRoom* chat;
    
    g_return_if_fail(hippo_data_cache_lookup_entity(cache, hippo_entity_get_guid(entity)) == NULL);
    
    g_object_ref(entity);
    g_hash_table_replace(cache->entities, g_strdup(hippo_entity_get_guid(entity)), entity);
    g_debug("Entity %s of type %d added, emitting entity-added", 
            hippo_entity_get_guid(entity), hippo_entity_get_entity_type(entity));
    
    if (HIPPO_IS_GROUP(entity)) {
        // we do not want to create a chat room for every group that the client learns about,
        // but we should look up if we already have the chat room, and then associate it with
        // the group, because we should only set the chat room to be fully loaded once it has 
        // an associated entity or post
        chat = hippo_data_cache_lookup_chat_room(cache, hippo_entity_get_guid(entity), NULL);
        
        if (chat) {
            hippo_group_set_chat_room(HIPPO_GROUP(entity), chat);
        }
    }

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

void
hippo_data_cache_add_block(HippoDataCache *cache,
                           HippoBlock     *block)
{
    g_return_if_fail(hippo_data_cache_lookup_block(cache, hippo_block_get_guid(block)) == NULL);

    g_object_ref(block);

    g_hash_table_replace(cache->blocks, g_strdup(hippo_block_get_guid(block)), block);
    
    g_debug("Block %s added, emitting block-added", hippo_block_get_guid(block));
    g_signal_emit(cache, signals[BLOCK_ADDED], 0, block);
}

static gboolean
update_block_from_xml(HippoDataCache *cache,
                      const char     *id,
                      LmMessageNode  *node)
{
    const char *type_str;
    const char *generic_types;
    HippoBlockType type;
    HippoBlock *block;
    gboolean existing;
    gboolean success;

    generic_types = NULL;
    if (!hippo_xml_split(cache, node, NULL,
                         "type", HIPPO_SPLIT_STRING, &type_str,
                         "genericTypes", HIPPO_SPLIT_STRING | HIPPO_SPLIT_OPTIONAL, &generic_types,
                         NULL)) {
        g_debug("no type attribute on block node");
        return FALSE;
    }

    type = hippo_block_type_from_attributes(type_str, generic_types);
    if (type == HIPPO_BLOCK_TYPE_UNKNOWN) {
        g_debug("Ignoring block of unknown type '%s'", type_str);
        return FALSE; // Ignore silently
    }

    block = hippo_data_cache_lookup_block(cache, id);
    if (block) {
        if (hippo_block_get_block_type(block) != type) {
            g_warning("New <block/> node for '%s' has type '%d, doesn't match '%d'\n",
                      id, type, hippo_block_get_block_type(block));
            return FALSE;
        }
        existing = TRUE;
    } else {
        block = hippo_block_new(id, type);
        existing = FALSE;
    }

    success = hippo_block_update_from_xml(block, cache, node);

    if (success) {
        hippo_connection_update_last_blocks_timestamp(cache->connection,
                                                      hippo_block_get_timestamp(block));
    }
    if (!existing) {
        if (success)
            hippo_data_cache_add_block(cache, block);
        g_object_unref(block);
    }

    return success;
}

static gboolean
update_entity_from_xml(HippoDataCache *cache,
                      const char      *id,
                       LmMessageNode  *node,
                       HippoEntityType type)
{
    HippoEntity *entity;
    gboolean existing;
    gboolean success;

    entity = hippo_data_cache_lookup_entity(cache, id);
    if (entity) {
        if (type != hippo_entity_get_entity_type(entity)) {
            g_warning("Type of entity received in update is different from the original entity type!");
            return FALSE;
        }
        existing = TRUE;
    } else {
        entity = hippo_entity_new(type, id);
        existing = FALSE;
    }

    success = hippo_entity_update_from_xml(entity, cache, node);

    if (!existing) {
        if (success)
            hippo_data_cache_add_entity(cache, entity);
        g_object_unref(entity);
    }

    return success;
}

static gboolean
update_post_from_xml(HippoDataCache *cache,
                     const char     *id,
                     LmMessageNode  *node)
{
    HippoPost *post;
    gboolean existing;
    gboolean success;

    post = hippo_data_cache_lookup_post(cache, id);
    if (post) {
        existing = TRUE;
    } else {
        post = hippo_post_new(id);
        existing = FALSE;
    }

    success = hippo_post_update_from_xml(post, cache, node);

    if (!existing) {
        if (success)
            hippo_data_cache_add_post(cache, post);
        g_object_unref(post);
    }

    return success;
}

gboolean
hippo_data_cache_update_from_xml (HippoDataCache *cache,
                                  LmMessageNode  *node)
{
    const char *name = node->name;
    const char *id;

    if (!hippo_xml_split(cache, node, NULL,
                         "id", HIPPO_SPLIT_GUID, &id,
                         NULL)) {
        g_debug("no id=guid attribute on node <%s>", node->name);
        return FALSE;
    }

    if (strcmp(name, "block") == 0) {
        return update_block_from_xml(cache, id, node);
    } else if (strcmp(name, "feed") == 0) {
        return update_entity_from_xml(cache, id, node, HIPPO_ENTITY_FEED);
    } else if (strcmp(name, "group") == 0) {
        return update_entity_from_xml(cache, id, node, HIPPO_ENTITY_GROUP);
    } else if (strcmp(name, "post") == 0) {
        return update_post_from_xml(cache, id, node);
    } else if (strcmp(name, "resource") == 0) {
        return update_entity_from_xml(cache, id, node, HIPPO_ENTITY_RESOURCE);
    } else if (strcmp(name, "user") == 0) {
        return update_entity_from_xml(cache, id, node, HIPPO_ENTITY_PERSON);
    }

    g_debug("Unknown element '%s' in XML object stream\n", name);
    return FALSE;
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

HippoBlock*
hippo_data_cache_lookup_block(HippoDataCache  *cache,
                              const char      *guid)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
   
    return g_hash_table_lookup(cache->blocks, guid);
}

HippoPerson*
hippo_data_cache_get_self(HippoDataCache   *cache)
{
    const char *self_id;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    self_id = hippo_connection_get_self_guid(cache->connection);
    if (self_id == NULL)
        return NULL; /* means we aren't connected */

    return HIPPO_PERSON(hippo_data_cache_ensure_bare_entity(cache, HIPPO_ENTITY_PERSON, self_id));
}

typedef struct {
    GTimeVal now;
    GSList *list;
    int count;
    gboolean build_list;
} ListRecentPostsData;

static void
list_recent_posts(void *key, void *value, void *data)
{   
    ListRecentPostsData *lrpd = data;
    HippoPost *post = HIPPO_POST(value);
    GTime postDate = hippo_post_get_date(post);
    
    if ((postDate + (60 * 60 * 24)) > lrpd->now.tv_sec) {
        if (lrpd->build_list) {
            lrpd->list = g_slist_prepend(lrpd->list, post);
            g_object_ref(post);
        }
        lrpd->count += 1;
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

static void
walk_recent_posts_internal(HippoDataCache      *cache,
                           ListRecentPostsData *lrpd_p,
                           gboolean             build_list)
{
    ListRecentPostsData lrpd;

    g_get_current_time(&lrpd.now);
    lrpd.list = NULL;
    lrpd.count = 0;
    lrpd.build_list = build_list;

    /* ref's each post */
    g_hash_table_foreach(cache->posts, list_recent_posts, &lrpd);
    
    if (build_list)
        lrpd.list = g_slist_sort(lrpd.list, post_date_compare);
    else
        g_assert(lrpd.list == NULL);

    *lrpd_p = lrpd;
}

GSList*
hippo_data_cache_get_recent_posts(HippoDataCache  *cache)
{
    ListRecentPostsData lrpd;
    
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    walk_recent_posts_internal(cache, &lrpd, TRUE);

    return lrpd.list;
}

int
hippo_data_cache_get_recent_posts_count(HippoDataCache  *cache)
{
    ListRecentPostsData lrpd;
    
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), 0);

    walk_recent_posts_internal(cache, &lrpd, FALSE);

    g_assert(lrpd.list == NULL);

    return lrpd.count;
}

GSList*
hippo_data_cache_get_active_posts(HippoDataCache  *cache)
{
    GSList *copy;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
   
    copy = g_slist_copy(cache->active_posts);
    g_slist_foreach(copy, (GFunc) g_object_ref, NULL);
    return copy;    
}

static void
list_all_posts_foreach(void *key, void *value, void *data)
{   
    GSList **result = data;
    HippoPost *post = HIPPO_POST(value);

    g_object_ref(post);
    *result = g_slist_prepend(*result, post);
}

GSList*
hippo_data_cache_get_all_posts(HippoDataCache  *cache)
{
    GSList *result = NULL;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
   
    /* ref's each post */
    g_hash_table_foreach(cache->posts, list_all_posts_foreach, &result);

    return result;    
}

static gboolean
clear_active_posts_no_signal(HippoDataCache *cache)
{
    GSList *tmp;

    if (cache->active_posts == NULL)
        return FALSE; /* don't emit changed signal */
    
    tmp = cache->active_posts;
    cache->active_posts = NULL;
    g_slist_foreach(tmp, (GFunc) g_object_unref, NULL);
    g_slist_free(tmp);
    
    return TRUE;
}

void
hippo_data_cache_clear_active_posts(HippoDataCache  *cache)
{    
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    
    if (clear_active_posts_no_signal(cache))
        g_signal_emit(cache, signals[ACTIVE_POSTS_CHANGED], 0);
}


static gboolean
add_active_posts_no_signal(HippoDataCache  *cache,
                           GSList          *posts)
{                           
    GSList *link;
    GSList *copy;
    gboolean added_anything;

    added_anything = FALSE;

    /* Make a backward copy so we can prepend each post 
     * and end up keeping their order
     */
    copy = g_slist_copy(posts);
    copy = g_slist_reverse(copy);

    /* This is a terrible algorithm but the lists are supposed
     * to be very short so it should be OK
     */
    for (link = copy; link != NULL; link = link->next) {
        HippoPost *post = link->data;
        if (g_slist_find(cache->active_posts, post)) {
            ; /* nothing, already in there */
        } else {
            cache->active_posts = g_slist_prepend(cache->active_posts, post);
            g_object_ref(post);
            added_anything = TRUE;
        }
    }
    
    g_slist_free(copy);

    return added_anything;
}

void
hippo_data_cache_add_active_posts(HippoDataCache  *cache,
                                  GSList          *posts)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    if (add_active_posts_no_signal(cache, posts)) {
        g_signal_emit(cache, signals[ACTIVE_POSTS_CHANGED], 0);
    }
}

void
hippo_data_cache_set_active_posts(HippoDataCache  *cache,
                                  GSList          *posts)
{
    gboolean cleared;
    gboolean added;
    
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    cleared = clear_active_posts_no_signal(cache);
    added = add_active_posts_no_signal(cache, posts);
    if (cleared || added) {
        g_signal_emit(cache, signals[ACTIVE_POSTS_CHANGED], 0);
    }
}

HippoChatRoom*
hippo_data_cache_lookup_chat_room(HippoDataCache  *cache,
                                  const char      *chat_id,
                                  HippoChatKind   *kind_p)
{
    HippoChatRoom *room;

    if (kind_p)
        *kind_p = -1; /* more reliably detect bugs */

    room = g_hash_table_lookup(cache->chats, chat_id);
    if (room != NULL) {
        if (kind_p)
            *kind_p = hippo_chat_room_get_kind(room);
        return room;
    }
    
    return room;
}

HippoChatRoom*
hippo_data_cache_ensure_chat_room(HippoDataCache  *cache,
                                  const char      *chat_id,
                                  HippoChatKind    kind)
{
    HippoChatRoom *room;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
    g_return_val_if_fail(chat_id != NULL, NULL);
    g_return_val_if_fail(hippo_verify_guid(chat_id), NULL);
    
    room = g_hash_table_lookup(cache->chats, chat_id);
    if (room == NULL) {
        HippoPost *post = NULL;
        HippoEntity *group = NULL;

        if (kind == HIPPO_CHAT_KIND_POST || kind == HIPPO_CHAT_KIND_UNKNOWN) {
            post = hippo_data_cache_lookup_post(cache, chat_id);
            
            if (post != NULL && kind == HIPPO_CHAT_KIND_UNKNOWN) {
                kind = HIPPO_CHAT_KIND_POST;
            }
        }
        
        if (kind == HIPPO_CHAT_KIND_GROUP || kind == HIPPO_CHAT_KIND_UNKNOWN) {
            group = hippo_data_cache_lookup_entity(cache, chat_id);
            
            if (group != NULL && !HIPPO_IS_GROUP(group))
                group = NULL;
            
            if (group != NULL && kind == HIPPO_CHAT_KIND_UNKNOWN) {
                kind = HIPPO_CHAT_KIND_GROUP;
            }
        }
        
        room = hippo_chat_room_new(chat_id, kind);

        if (post)
            hippo_post_set_chat_room(post, room);

        if (group) {
            hippo_group_set_chat_room(HIPPO_GROUP(group), room);
        }

        g_signal_connect(room, "loaded",
                         G_CALLBACK(hippo_data_cache_on_chat_room_loaded), cache);
            
        hippo_connection_request_chat_room_details(cache->connection, room);
        /* hand our refcount to the chats hashtable */
        g_hash_table_replace(cache->chats, g_strdup(chat_id), room);
    }
    return room;
}

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
foreach_chat_room_func(void *key, void *value, void *data)
{
    HippoChatRoom *room = HIPPO_CHAT_ROOM(value);
    ChatRoomClosure *closure = data;
    apply_closure(room, closure);
}

static void
hippo_data_cache_foreach_chat_room (HippoDataCache    *cache,
                                    gboolean           in_cache_finalize,
                                    HippoChatRoomFunc  func,
                                    void              *data)
{
    ChatRoomClosure closure;
    
    closure.func = func;
    closure.data = data;
    
    if (!in_cache_finalize)
        g_object_ref(cache);
    
    g_hash_table_foreach(cache->chats, foreach_chat_room_func, &closure);
    
    if (!in_cache_finalize)
        g_object_unref(cache);
}                                                                       

static void
update_chat_room_func(HippoChatRoom *room,
                      void          *data)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(data);
    
    /* be sure we know we aren't in the room */
    hippo_chat_room_set_user_state(room, cache->cached_self, HIPPO_CHAT_STATE_NONMEMBER);

    /* Now clear, rejoin, and reload the chat room */
    hippo_connection_rejoin_chat_room(cache->connection, room);
}

/* FIXME this is kind of broken; really we need to blow up the whole cache
 * anytime we reconnect, because it might be a different server, or the 
 * database might have been modified, or whatever. guids may not even be 
 * valid anymore.
 */
static void
hippo_data_cache_on_connect(HippoConnection      *connection,
                            gboolean              connected,
                            void                 *data)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(data);

    if (connected) {
        const char *self_id;
        
        self_id = hippo_connection_get_self_guid(cache->connection);
        
        /* This should not be NULL, since we authenticated. It can be 
         * NULL if called when not authenticated.
         */
        g_assert(self_id != NULL);

        /* note that cached_self may change to a different person each time we auth */
        cache->cached_self = HIPPO_PERSON(hippo_data_cache_ensure_bare_entity(cache, HIPPO_ENTITY_PERSON, self_id));
        g_object_ref(cache->cached_self);

        /* This only matters the second time we authenticate. 
         * For each chat room we were previously in, rejoin it.
         */
        hippo_data_cache_foreach_chat_room(cache, FALSE, update_chat_room_func, cache);

        hippo_connection_request_prefs(connection);
        hippo_connection_request_blocks(connection, 0);
    } else {
        /* Clear stuff, so we get "changed" signals both on disconnect 
         * and again on reconnect, and so we don't have stale data on
         * reconnect. FIXME more stuff needs to be cleared here, e.g. 
         * all the users and posts probably... or if there's a 
         * connection-spanning cache, it needs to be thought through.
         */
        hippo_data_cache_set_myspace_name(cache, NULL);
        hippo_data_cache_set_myspace_blog_comments(cache, NULL);
        hippo_data_cache_set_myspace_contacts(cache, NULL);
        hippo_data_cache_set_client_info(cache, NULL);
    }
}

static void      
hippo_data_cache_on_chat_room_loaded (HippoChatRoom *chat_room,
                                      void          *data)
{
    HippoDataCache *cache = data;

    g_signal_emit(cache, signals[CHAT_ROOM_LOADED], 0, chat_room);
}

GSList*
hippo_data_cache_get_myspace_blog_comments(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
    return cache->myspace_comments;
}

GSList*
hippo_data_cache_get_myspace_contacts(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);
    return cache->myspace_contacts;
}

void
hippo_data_cache_set_myspace_blog_comments(HippoDataCache  *cache,
                                           GSList          *comments)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
    
    g_slist_foreach(cache->myspace_comments, (GFunc) hippo_myspace_blog_comment_free, NULL);
    g_slist_free(cache->myspace_comments);
    cache->myspace_comments = g_slist_copy(comments);
    
    g_signal_emit(cache, signals[MYSPACE_COMMENTS_CHANGED], 0);
}
                                           
void
hippo_data_cache_set_myspace_contacts(HippoDataCache  *cache,
                                      GSList          *contacts)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));

    g_slist_foreach(cache->myspace_contacts, (GFunc) hippo_myspace_contact_free, NULL);
    g_slist_free(cache->myspace_contacts);
    cache->myspace_contacts = g_slist_copy(contacts);
    
    g_signal_emit(cache, signals[MYSPACE_CONTACTS_CHANGED], 0);
}                                      

const HippoClientInfo*
hippo_data_cache_get_client_info(HippoDataCache   *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    return &cache->client_info;
}

static void
zero_str(char **s_p)
{
    g_free(*s_p);
    *s_p = NULL;
}

void
hippo_data_cache_set_client_info(HippoDataCache        *cache,
                                 const HippoClientInfo *info)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));

    zero_str(&cache->client_info.current);
    zero_str(&cache->client_info.minimum);
    zero_str(&cache->client_info.download);

    if (info) {
        cache->client_info.current = g_strdup(info->current);
        cache->client_info.minimum = g_strdup(info->minimum);
        cache->client_info.download = g_strdup(info->download);
    }
}

static HippoEntity *
add_debug_person(HippoDataCache *cache, const char *guid, const char *name)
{
    HippoEntity *entity = hippo_entity_new(HIPPO_ENTITY_PERSON, guid);
    hippo_entity_set_name(entity, name);
    hippo_entity_set_photo_url(entity, "/files/headshots/60/default");

    hippo_data_cache_add_entity(cache, entity);

    return entity;
}

void
hippo_data_cache_add_debug_data(HippoDataCache *cache)
{
    HippoEntity *person1, *person2, *person3, *person4;
    GSList *recipients = NULL;
    HippoPost *linkshare1, *linkshare2;

    person1 = add_debug_person(cache, "55a1fbae7f2807", "Colin");
    person2 = add_debug_person(cache, "25a1fbae7f2807", "Owen Taylor");
    person3 = add_debug_person(cache, "35a1fbae7f2807", "Colin Walters");
    person4 = add_debug_person(cache, "a5a1fbae7f2807", "Bryan Clark");

    recipients = g_slist_append(recipients, person1);

    linkshare1 = hippo_post_new("42424242424242");
    hippo_post_set_url(linkshare1, "http://www.gnome.org");
    hippo_post_set_title(linkshare1, "Here is the title make this long enough so that it will wrap and cause problems");
    hippo_post_set_sender(linkshare1, hippo_entity_get_guid(person1));
    hippo_post_set_description(linkshare1, 
                               "The body of the message. Again we want a lot of text here so that "
                               "we can see wrapping and all sorts of fun things like that which will "
                               "cause differences from what we would have if we had a short title without "
                               "the kind of excessive length that you see here.");
    hippo_post_set_recipients(linkshare1, recipients);
    hippo_post_set_public(linkshare1, TRUE);
    hippo_post_set_timeout(linkshare1, 0);
    hippo_post_set_new(linkshare1, TRUE);

    hippo_data_cache_add_post(cache, linkshare1);

    linkshare2 = hippo_post_new("43434343434343");
    hippo_post_set_url(linkshare2, "http://flickr.com/photos/tweedie/63302017/");
    hippo_post_set_title(linkshare2, "funny photo");
    hippo_post_set_sender(linkshare2, hippo_entity_get_guid(person1));
    hippo_post_set_description(linkshare2, "Wow, this photo is funny");
    hippo_post_set_recipients(linkshare2, recipients);
    hippo_post_set_public(linkshare2, FALSE);
    hippo_post_set_timeout(linkshare2, 0);
    hippo_post_set_new(linkshare2, TRUE);

    hippo_post_set_info(linkshare2, 
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        "<postInfo>"
                        "    <flickr>"
                        "        <photos>"
                        "             <photo>"
                        "                  <photoUrl>/files/postinfo/0eacc4088d8fc92edb2a9299e15acae6efa710f1</photoUrl>"
                        "                  <photoId>73029609</photoId>"
                        "             </photo>"
                        "        </photos>"
                        "    </flickr>"
                        "</postInfo>");
    
    hippo_data_cache_add_post(cache, linkshare2);
}
