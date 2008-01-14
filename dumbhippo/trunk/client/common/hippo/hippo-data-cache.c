/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-data-cache-internal.h"
#include <ddm/ddm.h>
#include "hippo-connection.h"
#include "hippo-title-pattern.h"
#include "hippo-xml-utils.h"
#include "hippo-data-model-backend.h"
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

struct _HippoDataCache {
    GObject          parent;
    HippoConnection *connection;
    GHashTable      *chats;
    unsigned int     music_sharing_enabled : 1;
    unsigned int     music_sharing_primed : 1;
    unsigned int     application_usage_enabled : 1;
    HippoClientInfo  client_info;
    GSList          *title_patterns;
    DDMDataModel    *model;
    HippoPerson     *self; /* Cached so get_self() doesn't ref */
};

struct _HippoDataCacheClass {
    GObjectClass parent_class;

};

enum {
    APPLICATION_USAGE_CHANGED,
    MUSIC_SHARING_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(HippoDataCache, hippo_data_cache, G_TYPE_OBJECT);

static void
hippo_data_cache_init(HippoDataCache *cache)
{
    cache->chats = g_hash_table_new_full(g_str_hash, g_str_equal,
                                         g_free, (GFreeFunc) g_object_unref);
                                               
    /* these defaults are important to be sure we
     * do nothing until we hear otherwise
     * (and to be sure a signal is emitted if we need to
     * do something, since stuff will have changed)
     */
    cache->music_sharing_enabled = FALSE;
    cache->music_sharing_primed = TRUE;
}

static void
hippo_data_cache_class_init(HippoDataCacheClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);  
          
    signals[APPLICATION_USAGE_CHANGED] =
        g_signal_new ("application-usage-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    signals[MUSIC_SHARING_CHANGED] =
        g_signal_new ("music-sharing-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);

    object_class->finalize = hippo_data_cache_finalize;
}

static void
hippo_data_cache_finalize(GObject *object)
{
    HippoDataCache *cache = HIPPO_DATA_CACHE(object);

    g_debug("Finalizing data cache");
    
    hippo_data_cache_set_title_patterns(cache, NULL);

    hippo_data_cache_set_client_info(cache, NULL);

    g_hash_table_destroy(cache->chats);
    
    if (cache->self) {
        g_object_unref(cache->self);
        cache->self = NULL;
    }

    g_object_unref (cache->model);
    
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

    cache->model = ddm_data_model_new_with_backend(hippo_data_model_get_backend(),
                                                   cache, NULL);

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

gboolean
hippo_data_cache_get_application_usage_enabled(HippoDataCache *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), FALSE);
    return cache->application_usage_enabled;
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
hippo_data_cache_set_application_usage_enabled (HippoDataCache *cache,
                                                gboolean        enabled)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));

    enabled = enabled != FALSE;

    if (enabled != cache->application_usage_enabled) {
        cache->application_usage_enabled = enabled;
        g_signal_emit(cache, signals[APPLICATION_USAGE_CHANGED], 0);
    }
}

HippoPerson*
hippo_data_cache_get_self(HippoDataCache   *cache)
{
    if (cache->self == NULL) {
        DDMDataResource *self_resource = ddm_data_model_get_self_resource(cache->model);
        if (self_resource)
            cache->self = hippo_person_get_for_resource(self_resource);
        else
            return NULL;
    }

    return cache->self;
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
        room = hippo_chat_room_new(chat_id, kind);

        /* We don't try to find out the contents of the chat room immediately, since
         * we don't get updates on a chatroom until we join it; we just wait until
         * we first join it (which will usually be immediately after this.)
         */

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
    HippoPerson *self = hippo_data_cache_get_self(cache);

    if (self != NULL) {
        /* be sure we know we aren't in the room */
        hippo_chat_room_set_user_state(room, self, HIPPO_CHAT_STATE_NONMEMBER);

        /* Now clear, rejoin, and reload the chat room */
        hippo_connection_rejoin_chat_room(cache->connection, room);
    }
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

        if (cache->self) {
            g_object_unref(cache->self);
            cache->self = NULL;
        }

        /* This only matters the second time we authenticate. 
         * For each chat room we were previously in, rejoin it.
         */
        hippo_data_cache_foreach_chat_room(cache, FALSE, update_chat_room_func, cache);
        
        hippo_connection_request_prefs(connection);
        hippo_connection_request_title_patterns(connection);
    } else {
        /* Clear stuff, so we get "changed" signals both on disconnect 
         * and again on reconnect, and so we don't have stale data on
         * reconnect. FIXME more stuff needs to be cleared here, e.g. 
         * all the users probably... or if there's a 
         * connection-spanning cache, it needs to be thought through.
         */
        hippo_data_cache_set_client_info(cache, NULL);
    }
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
    zero_str(&cache->client_info.ddm_protocol_version);    

    if (info) {
        cache->client_info.current = g_strdup(info->current);
        cache->client_info.minimum = g_strdup(info->minimum);
        cache->client_info.download = g_strdup(info->download);
        cache->client_info.ddm_protocol_version = g_strdup(info->ddm_protocol_version);        
    }
}


void
hippo_data_cache_set_title_patterns(HippoDataCache *cache,
                                    GSList         *title_patterns)
{
    g_return_if_fail(HIPPO_IS_DATA_CACHE(cache));
        
    g_slist_foreach(cache->title_patterns, (GFunc)hippo_title_pattern_free, NULL);
    g_slist_free(cache->title_patterns);
    
    cache->title_patterns = title_patterns;
}

const char *
hippo_data_cache_match_application_title(HippoDataCache *cache,
                                         const char     *title)
{
    GSList *l;

    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    for (l = cache->title_patterns; l; l = l->next) {
        if (hippo_title_pattern_matches(l->data, title))
            return hippo_title_pattern_get_app_id(l->data);
    }

    return NULL;
}
    
DDMDataModel *
hippo_data_cache_get_model(HippoDataCache   *cache)
{
    g_return_val_if_fail(HIPPO_IS_DATA_CACHE(cache), NULL);

    return cache->model;
}
