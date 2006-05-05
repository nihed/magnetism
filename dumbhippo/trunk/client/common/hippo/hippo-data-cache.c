#include "hippo-data-cache.h"

static void      hippo_data_cache_init                (HippoDataCache       *cache);
static void      hippo_data_cache_class_init          (HippoDataCacheClass  *klass);

static void      hippo_data_cache_finalize            (GObject              *object);

struct _HippoDataCache {
    GObject parent;
    HippoConnection *connection;
    GHashTable      *posts;
    GHashTable      *entities;
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
    g_hash_table_destroy(cache->entities);

    g_object_unref(cache->connection);

    G_OBJECT_CLASS(hippo_data_cache_parent_class)->finalize(object); 
}

HippoDataCache*
hippo_data_cache_new(HippoConnection *connection)
{
    HippoDataCache *cache = g_object_new(HIPPO_TYPE_DATA_CACHE, NULL);

    cache->connection = connection;
    g_object_ref(cache->connection);

    return cache;
}

/* static for now... I think the public function should maybe be ensure_post 
 * or something that both creates and adds? need to look at callers to see.
 */
static void
hippo_data_cache_add_post(HippoDataCache *cache,
                          HippoPost      *post)
{
    g_return_if_fail(hippo_data_cache_lookup_post(cache, hippo_post_get_guid(post)) == NULL);

    g_hash_table_replace(cache->posts, g_strdup(hippo_post_get_guid(post)), post);
    g_signal_emit(cache, signals[POST_ADDED], 0, post);
}

static void
hippo_data_cache_add_entity(HippoDataCache *cache,
                            HippoEntity    *entity)
{
    g_return_if_fail(hippo_data_cache_lookup_entity(cache, hippo_entity_get_guid(entity)) == NULL);

    g_hash_table_replace(cache->entities, g_strdup(hippo_entity_get_guid(entity)), entity);
    g_signal_emit(cache, signals[ENTITY_ADDED], 0, entity);    
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
