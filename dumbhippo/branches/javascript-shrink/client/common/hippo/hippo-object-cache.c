/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-common-internal.h"
#include "hippo-object-cache.h"
#include <string.h>

/* max cached objects to keep in memory even if they are unused */
#define MAX_STRONG_ENTRIES 32
/* how often to retry on failure */
#define RETRY_INTERVAL_SECONDS (60*2)

typedef struct {
    HippoObjectCacheLoadFunc  func;
    void                     *data;
} LoadCallback;

typedef struct {
    int               refcount;
    HippoObjectCache *cache;
    char             *url;
    GObject          *cached_obj;
    GSList           *callbacks;
    GTime             last_attempt;
    unsigned int      loading : 1;
    unsigned int      in_strong_cache : 1;
} CacheEntry;

static void      hippo_object_cache_init                (HippoObjectCache       *cache);
static void      hippo_object_cache_class_init          (HippoObjectCacheClass  *klass);

static void      hippo_object_cache_dispose             (GObject                *object);
static void      hippo_object_cache_finalize            (GObject                *object);

static void hippo_object_cache_set_property (GObject      *object,
                                             guint         prop_id,
                                             const GValue *value,
                                             GParamSpec   *pspec);
static void hippo_object_cache_get_property (GObject      *object,
                                             guint         prop_id,
                                             GValue       *value,
                                             GParamSpec   *pspec);


typedef struct {
    HippoPlatform *platform;
    
    CacheEntry *strong_cache[MAX_STRONG_ENTRIES];

    /* the CacheEntry in here are never dropped right now, just the cached_obj
     * they point to. Simplifies things a little.
     */
    GHashTable *weak_cache;
} HippoObjectCachePrivate;

#define HIPPO_OBJECT_CACHE_GET_PRIVATE(obj) (G_TYPE_INSTANCE_GET_PRIVATE ((obj), HIPPO_TYPE_OBJECT_CACHE, HippoObjectCachePrivate))

enum {
    PROP_0,
    PROP_PLATFORM
};

G_DEFINE_TYPE(HippoObjectCache, hippo_object_cache, G_TYPE_OBJECT);

static CacheEntry*
cache_entry_new(HippoObjectCache *cache,
                const char       *url)
{
    CacheEntry *entry;

    entry = g_new0(CacheEntry, 1);
    entry->refcount = 1;
    entry->cache = cache;
    HIPPO_ADD_WEAK(&entry->cache);
    entry->url = g_strdup(url);
    return entry;
}

static void
cache_entry_finalize(CacheEntry *entry)
{
    g_return_if_fail(!entry->loading);
    
    /* be sure all callbacks get an error reply if they haven't */
    HIPPO_REMOVE_WEAK(&entry->cache);
    entry->cache = NULL;
    HIPPO_REMOVE_WEAK(&entry->cached_obj);
    entry->cached_obj = NULL;

    while (entry->callbacks != NULL) {
        LoadCallback *cb = entry->callbacks->data;
        entry->callbacks = g_slist_remove(entry->callbacks, entry->callbacks->data);
        (* cb->func)(NULL, cb->data);
        g_free(cb);
    }
    
    g_free(entry->url);
    g_free(entry);
}

static void
cache_entry_unref(CacheEntry *entry)
{
    g_return_if_fail(entry->refcount > 0);
    
    entry->refcount -= 1;
    if (entry->refcount == 0) {
        cache_entry_finalize(entry);
    }
}

static void
cache_entry_ref(CacheEntry *entry)
{
    g_return_if_fail(entry->refcount > 0);

    entry->refcount += 1;
}


static void
cache_entry_invoke_callbacks(CacheEntry *entry)
{
    cache_entry_ref(entry);
    while (entry->callbacks != NULL) {
        LoadCallback *cb = entry->callbacks->data;
        entry->callbacks = g_slist_remove(entry->callbacks, entry->callbacks->data);
        (* cb->func)(entry->cached_obj, cb->data);
        g_free(cb);
    }
    cache_entry_unref(entry);
}

static GObject*
hippo_object_cache_parse(HippoObjectCache *cache,
                         const char       *url,
                         const char       *content_type,
                         GString          *content)                             
{
    GError *error;
    GObject *cached_obj;
    HippoObjectCacheClass *klass;

    klass = HIPPO_OBJECT_CACHE_GET_CLASS(cache);

    g_assert(klass->parse != NULL);

    error = NULL;
    cached_obj = (* klass->parse) (cache,
                                   url,
                                   content_type,
                                   content,
                                   &error);

    if (error != NULL) {
        g_debug("Failed to load object '%s': %s", url, error->message);
        g_error_free(error);
    }
    
    return cached_obj;
}

static void
http_func(const char *content_type,
          GString    *content_or_error,
          void       *data)
{
    CacheEntry *entry = data;
    GTimeVal now;

    /* The cache entry may have cache == NULL if the cache
     * was nuked while we were loading.
     */
    
    g_assert(entry->cached_obj == NULL);

    g_get_current_time(&now);

    entry->last_attempt = now.tv_sec;
    
    if (content_type != NULL && content_or_error->len > 0 && entry->cache != NULL) {
        entry->cached_obj = hippo_object_cache_parse(entry->cache,
                                                     entry->url,
                                                     content_type,
                                                     content_or_error);
    }

    if (entry->cached_obj)
        HIPPO_ADD_WEAK(&entry->cached_obj);

    /* if cached_obj is NULL we failed, otherwise we succeeded.
     * either way we invoke the callbacks.
     */
    entry->loading = FALSE;
    cache_entry_invoke_callbacks(entry);

    /* note that this will immediately NULL the cached_obj if there were no
     * strong references added during the callbacks
     */
    if (entry->cached_obj)
        g_object_unref(entry->cached_obj);

    /* a ref was held by the http load */
    cache_entry_unref(entry);
}

static void
cache_entry_load(CacheEntry               *entry,
                 HippoObjectCacheLoadFunc  func,
                 void                     *data)
{
    LoadCallback *cb;
    
    cb = g_new0(LoadCallback, 1);
    cb->func = func;
    cb->data = data;
    entry->callbacks = g_slist_append(entry->callbacks, cb);

    if (entry->cached_obj) {
        /* already have the cached_obj, just invoke callback
         * synchronously and return
         */
        cache_entry_invoke_callbacks(entry);
    } else if (!entry->loading) {
        /* start the http get, unless we recently failed
         * or the cache is already gone
         */
        GTimeVal now;

        g_get_current_time(&now);

        if ((entry->last_attempt < now.tv_sec && /* detect clock backward */
             (entry->last_attempt + RETRY_INTERVAL_SECONDS) > now.tv_sec) ||
            entry->cache == NULL) {
            /* RETRY_INTERVAL_SECONDS have not elapsed, so we fail
             * immediately/synchronously, don't retry the http get
             */
            cache_entry_invoke_callbacks(entry);
        } else {
            /* pend the http get */

            HippoObjectCachePrivate *priv;

            g_assert(entry->cache != NULL);
            
            priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(entry->cache);
            
            entry->loading = TRUE;
            cache_entry_ref(entry); /* held by http_func */
            hippo_platform_http_request(priv->platform,
                                        entry->url, http_func, entry);
        }
    } else {
        /* nothing to do, just keep waiting for http to return */
        ;
    }
}

static void
hippo_object_cache_init(HippoObjectCache  *cache)
{
    HippoObjectCachePrivate *priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);

    priv->weak_cache = g_hash_table_new_full(g_str_hash, g_str_equal,
                                             NULL, (GFreeFunc) cache_entry_unref);
}

static void
hippo_object_cache_class_init(HippoObjectCacheClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->set_property = hippo_object_cache_set_property;
    object_class->get_property = hippo_object_cache_get_property;
    
    object_class->dispose = hippo_object_cache_dispose;
    object_class->finalize = hippo_object_cache_finalize;

    g_object_class_install_property(object_class,
                                    PROP_PLATFORM,
                                    g_param_spec_object("platform",
                                                        _("Platform"),
                                                        _("Platform object"),
                                                        /* interfaces can't go here it seems */
                                                        G_TYPE_OBJECT,
                                                        G_PARAM_READABLE | G_PARAM_WRITABLE)); 
    
    g_type_class_add_private(object_class, sizeof(HippoObjectCachePrivate));
}

static void
hippo_object_cache_dispose(GObject *object)
{
    HippoObjectCache *cache = HIPPO_OBJECT_CACHE(object);
    HippoObjectCachePrivate *priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);
    int i;

    for (i = 0; i < MAX_STRONG_ENTRIES; ++i) {
        CacheEntry *entry = priv->strong_cache[i];
        if (entry != NULL) {
            g_assert(entry->in_strong_cache);
            g_assert(entry->cached_obj != NULL); /* invariant that strong entries have a cached_obj */
            
            priv->strong_cache[i] = NULL;
            
            g_object_unref(entry->cached_obj);

            entry->cached_obj = NULL;
            entry->in_strong_cache = FALSE;
        }
    }

    G_OBJECT_CLASS(hippo_object_cache_parent_class)->dispose(object);
}

static void
hippo_object_cache_finalize(GObject *object)
{
    HippoObjectCache *cache = HIPPO_OBJECT_CACHE(object);
    HippoObjectCachePrivate *priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);

    g_object_unref(priv->platform);
    g_hash_table_destroy(priv->weak_cache);

    G_OBJECT_CLASS(hippo_object_cache_parent_class)->finalize(object);
}

static void
hippo_object_cache_set_property(GObject         *object,
                                guint            prop_id,
                                const GValue    *value,
                                GParamSpec      *pspec)
{
    HippoObjectCache *cache;
    HippoObjectCachePrivate *priv;

    cache = HIPPO_OBJECT_CACHE (object);
    priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);

    switch (prop_id) {
    case PROP_PLATFORM:
        {
            HippoPlatform *new_platform =
                (HippoPlatform*) g_value_get_object(value);
            if (new_platform != priv->platform) {
                if (priv->platform)
                    g_object_unref(priv->platform);

                if (new_platform)
                    g_object_ref(new_platform);

                priv->platform = new_platform;
            }
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void
hippo_object_cache_get_property(GObject         *object,
                                guint            prop_id,
                                GValue          *value,
                                GParamSpec      *pspec)
{
    HippoObjectCache *cache;
    HippoObjectCachePrivate *priv;

    cache = HIPPO_OBJECT_CACHE (object);
    priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);

    switch (prop_id) {
    case PROP_PLATFORM:
        g_value_set_object(value, (GObject*) priv->platform);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

HippoObjectCache*
hippo_object_cache_new(HippoPlatform *platform)
{    
    HippoObjectCache *cache;

    cache = g_object_new(HIPPO_TYPE_OBJECT_CACHE,
                         "platform", platform,
                         NULL);
    
    return cache;
}

static void
strong_ref_on_load(GObject   *cached_obj,
                   void      *data)
{
    HippoObjectCachePrivate *priv;
    CacheEntry *entry = data;

    
    /* g_debug("strong_ref_on_load cache=%p obj=%p in_strong_cache=%d",
       entry->cache, entry->cached_obj, entry->in_strong_cache); */
    
    if (entry->cache == NULL) {
        /* cache got nuked with http request pending */
        cache_entry_unref(entry);
        return;
    }

    priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(entry->cache);

    g_assert(entry->cached_obj == cached_obj);
    
    /* replace a strong ref with this new one if we have a cached_obj to
     * strongly ref
     */
    if (cached_obj && !entry->in_strong_cache) {
        int i;
        CacheEntry *old_strong;

        /* look for empty strong slot */
        for (i = 0; i < MAX_STRONG_ENTRIES; ++i) {
            if (priv->strong_cache[i] == NULL)
                break;
        }
        if (i == MAX_STRONG_ENTRIES) {
            /* replace random item */
            i = g_random_int_range(0, MAX_STRONG_ENTRIES);
            g_assert(i >= 0);
            g_assert(i < MAX_STRONG_ENTRIES);
        }

        /* old_strong can't be this new entry since old_strong had a cached_obj thus
         * no outstanding http request, but this code should work anyway if
         * we replace ourselves since we ref first and unref second
         */
        old_strong = priv->strong_cache[i];

        g_debug("Replacing old strong-ref '%s' with new strong-ref '%s' entry %d",
                old_strong ? old_strong->url : "none", entry->url, i);

        g_assert(entry->cached_obj != NULL);
        g_object_ref(entry->cached_obj);
        entry->in_strong_cache = TRUE;
        priv->strong_cache[i] = entry;
        if (old_strong) {
            g_assert(old_strong->cached_obj);       /* invariant that strong entries have a cached_obj */
            g_assert(old_strong->in_strong_cache);
            old_strong->in_strong_cache = FALSE;
            g_object_unref(old_strong->cached_obj); /* old_strong->cached_obj may be set to NULL here */
        }

        if (entry->cache)
            hippo_object_cache_debug_dump(entry->cache);
    }

    cache_entry_unref(entry);
}

void
hippo_object_cache_load(HippoObjectCache           *cache,
                        const char                 *url,
                        HippoObjectCacheLoadFunc    func,
                        void                       *data)
{
    HippoObjectCachePrivate *priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);
    CacheEntry *entry;

    entry = g_hash_table_lookup(priv->weak_cache, url);
    if (entry == NULL) {
        entry = cache_entry_new(cache, url);
        g_hash_table_replace(priv->weak_cache, entry->url, entry);
    }
    g_assert(entry != NULL);

    /* hippo_object_cache_debug_dump(cache); */
    
    /* don't check entry->in_strong_cache here, so each load attempt
     * has its own chance to end up in the strong cache
     */
    if (entry->cached_obj == NULL &&
        !entry->loading) {
        /* this will be newly-loaded so add our strong ref callback
         * which will add a strong ref on the cached_obj if the http
         * request returns successfully.
         */
        cache_entry_ref(entry); /* ref held by strong_ref_on_load */
        cache_entry_load(entry, strong_ref_on_load, entry);
    }
    cache_entry_load(entry, func, data);
}

static void
dump_foreach(void *key, void *value, void *data)
{
    CacheEntry *entry = value;

    g_debug("%s '%s' obj=%p %s",
            entry->in_strong_cache ? "STRONG" : "WEAK  ",
            entry->url,
            entry->cached_obj,
            entry->cached_obj ?
            g_type_name_from_instance((GTypeInstance*) entry->cached_obj) : "");
}

void
hippo_object_cache_debug_dump (HippoObjectCache *cache)
{
    HippoObjectCachePrivate *priv = HIPPO_OBJECT_CACHE_GET_PRIVATE(cache);
    
    g_hash_table_foreach(priv->weak_cache,
                         dump_foreach,
                         NULL);
}
