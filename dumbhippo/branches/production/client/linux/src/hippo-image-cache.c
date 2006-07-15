#include <config.h>
#include "hippo-image-cache.h"
#include "main.h"
#include "hippo-http.h"
#include <string.h>

/* max pixbufs to keep in memory even if they are unused */
#define MAX_STRONG_ENTRIES 16
/* how often to retry on failure */
#define RETRY_INTERVAL_SECONDS (60*2)

typedef struct {
    HippoImageCacheLoadFunc  func;
    void                    *data;
} LoadCallback;

typedef struct {
    char         *url;
    GdkPixbuf    *pixbuf;
    GSList       *callbacks;
    GTime         last_attempt;
    unsigned int  loading : 1;
    /* tried to free entry with pending load, free it on load complete */
    unsigned int  free_on_load : 1;
    unsigned int  in_strong_cache : 1;
} CacheEntry;

static void      hippo_image_cache_init                (HippoImageCache       *cache);
static void      hippo_image_cache_class_init          (HippoImageCacheClass  *klass);

static void      hippo_image_cache_finalize            (GObject               *object);

struct _HippoImageCache {
    GObject parent;
    CacheEntry *strong_cache[MAX_STRONG_ENTRIES];
    
    /* the CacheEntry in here are never dropped right now, just the pixbuf
     * they point to. Simplifies things a little.
     */
    GHashTable *weak_cache;
};

struct _HippoImageCacheClass {
    GObjectClass parent_class;

};

G_DEFINE_TYPE(HippoImageCache, hippo_image_cache, G_TYPE_OBJECT);

static void
cache_entry_invoke_callbacks(CacheEntry *entry)
{
    while (entry->callbacks != NULL) {
        LoadCallback *cb = entry->callbacks->data;
        entry->callbacks = g_slist_remove(entry->callbacks, entry->callbacks->data);
        (* cb->func)(entry->pixbuf, cb->data);
        g_free(cb);
    }
}

static CacheEntry*
cache_entry_new(const char *url)
{
    CacheEntry *entry;
    
    entry = g_new0(CacheEntry, 1);
    entry->url = g_strdup(url);
    return entry;
}

static void
cache_entry_free(CacheEntry *entry)
{
    if (entry->loading) {
        /* have to wait for http get to complete since our 
         * awesome http get system has no way to cancel pending
         * gets ...
         */
        entry->free_on_load = TRUE;
        return;
    }

    /* be sure all callbacks get an error reply if they haven't */
    REMOVE_WEAK(&entry->pixbuf);
    entry->pixbuf = NULL;

    cache_entry_invoke_callbacks(entry);
    
    g_free(entry->url);
    g_free(entry);
}

static GdkPixbuf*
parse_pixbuf(const char *url,
             GString    *image_data)
{
    GdkPixbufLoader *loader;
    GError *error;
    GdkPixbuf *pixbuf;
    
    loader = gdk_pixbuf_loader_new();
    
    error = NULL;
    if (!gdk_pixbuf_loader_write(loader, (guchar*) image_data->str, image_data->len, &error))
        goto failed;

    g_assert(error == NULL);
    if (!gdk_pixbuf_loader_close(loader, &error))
        goto failed;
    g_assert(error == NULL);
        
    pixbuf = gdk_pixbuf_loader_get_pixbuf(loader);
    if (pixbuf == NULL)
        goto failed;
        
    g_object_ref(pixbuf);
    g_object_unref(loader);
    return pixbuf;

  failed:
    if (loader)
        g_object_unref(loader);
    if (error) {
        g_debug("Failed to load image '%s': %s", url, error->message);
        g_error_free(error);
    }
    return NULL;
}

static void
http_func(const char *content_type,
          GString    *content_or_error,
          void       *data)
{
    CacheEntry *entry = data;
    GTimeVal now;

    g_assert(entry->pixbuf == NULL);

    g_get_current_time(&now);

    entry->last_attempt = now.tv_sec;

    if (content_type != NULL && content_or_error->len > 0) {
        entry->pixbuf = parse_pixbuf(entry->url, content_or_error);
    }

    if (entry->pixbuf)    
        ADD_WEAK(&entry->pixbuf);
    
    /* if pixbuf is NULL we failed, otherwise we succeeded.
     * either way we invoke the callbacks.
     */
    entry->loading = FALSE;
    cache_entry_invoke_callbacks(entry);
    
    /* note that this will immediately NULL the pixbuf if there were no 
     * strong references added during the callbacks
     */
    if (entry->pixbuf)
        g_object_unref(entry->pixbuf);
    
    if (entry->free_on_load) {
        cache_entry_free(entry);
    }
}

static void
cache_entry_load(CacheEntry              *entry,
                 HippoImageCacheLoadFunc  func,
                 void                    *data)
{
    LoadCallback *cb;
    
    cb = g_new0(LoadCallback, 1);
    cb->func = func;
    cb->data = data;
    entry->callbacks = g_slist_append(entry->callbacks, cb);

    if (entry->pixbuf) {
        /* already have the pixbuf, just invoke callback
         * synchronoously and return 
         */
        cache_entry_invoke_callbacks(entry);
    } else if (!entry->loading) {
        /* start the http get, unless we recently failed */
        GTimeVal now;

        g_get_current_time(&now);
        
        if (entry->last_attempt < now.tv_sec && /* detect clock backward */
            (entry->last_attempt + RETRY_INTERVAL_SECONDS) > now.tv_sec) {
            /* RETRY_INTERVAL_SECONDS have not elapsed, so we fail 
             * immediately/synchronously, don't retry the http get
             */
            cache_entry_invoke_callbacks(entry);
        } else {
            /* pend the http get */
            entry->loading = TRUE;
            hippo_http_get(entry->url, http_func, entry);
        }
    } else {
        /* nothing to do, just keep waiting for http to return */
        ;
    }
}

static void
hippo_image_cache_init(HippoImageCache  *cache)
{
    cache->weak_cache = g_hash_table_new_full(g_str_hash, g_str_equal,
                            NULL, (GFreeFunc) cache_entry_free);
}

static void
hippo_image_cache_class_init(HippoImageCacheClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_image_cache_finalize;
}

HippoImageCache*
hippo_image_cache_new(void)
{
    HippoImageCache *cache;

    cache = g_object_new(HIPPO_TYPE_IMAGE_CACHE,
                         NULL);
    
    return cache;
}

static void
hippo_image_cache_finalize(GObject *object)
{
    HippoImageCache *cache = HIPPO_IMAGE_CACHE(object);
    int i;
    
    for (i = 0; i < MAX_STRONG_ENTRIES; ++i) {
        CacheEntry *entry = cache->strong_cache[i];
        if (entry != NULL) {
            g_assert(entry->in_strong_cache);
            g_assert(entry->pixbuf != NULL); /* invariant that strong entries have a pixbuf */
            
            cache->strong_cache[i] = NULL;
            
            g_object_unref(entry->pixbuf);
            
            entry->pixbuf = NULL;
            entry->in_strong_cache = FALSE;
        }
    }
    
    g_hash_table_destroy(cache->weak_cache);
    
    G_OBJECT_CLASS(hippo_image_cache_parent_class)->finalize(object);
}

typedef struct
{
    HippoImageCache *cache;
    CacheEntry      *entry;
} StrongRefData;

static void
strong_ref_on_load(GdkPixbuf *pixbuf,
                   void      *data)
{
    StrongRefData *s = data;
    if (s->cache == NULL) {
        /* cache got nuked with http request pending */    
        g_free(s);
        return;
    }

    g_assert(s->entry->pixbuf == pixbuf);
    
    /* replace a strong ref with this new one if we have a pixbuf to 
     * strongly ref
     */
    if (pixbuf && !s->entry->in_strong_cache) {
        int i;
        CacheEntry *old_strong;
            
        /* look for empty strong slot */
        for (i = 0; i < MAX_STRONG_ENTRIES; ++i) {
            if (s->cache->strong_cache[i] == NULL)
                break;
        }    
        if (i == MAX_STRONG_ENTRIES) {
            /* replace random item */
            i = g_random_int_range(0, MAX_STRONG_ENTRIES);
            g_assert(i >= 0);
            g_assert(i < MAX_STRONG_ENTRIES);
        }
        
        /* old_strong can't be this new entry since old_strong had a pixbuf thus 
         * no outstanding http request, but this code should work anyway if 
         * we replace ourselves since we ref first and unref second
         */
        old_strong = s->cache->strong_cache[i];
        
        g_debug("Replacing old strong-ref '%s' with new strong-ref '%s' entry %d", 
                old_strong ? old_strong->url : "none", s->entry->url, i);

        g_assert(s->entry->pixbuf != NULL);        
        g_object_ref(s->entry->pixbuf);
        s->entry->in_strong_cache = TRUE;
        s->cache->strong_cache[i] = s->entry;
        if (old_strong) {
            g_assert(old_strong->pixbuf);       /* invariant that strong entries have a pixbuf */
            g_assert(old_strong->in_strong_cache);
            old_strong->in_strong_cache = FALSE;
            g_object_unref(old_strong->pixbuf); /* old_strong->pixbuf may be set to NULL here */
        }
    }

    REMOVE_WEAK(&s->cache);    
    g_free(s);
}

void
hippo_image_cache_load(HippoImageCache          *cache,
                       const char               *url,
                       HippoImageCacheLoadFunc   func,
                       void                     *data)
{
    CacheEntry *entry;
    
    entry = g_hash_table_lookup(cache->weak_cache, url);
    if (entry == NULL) {
        entry = cache_entry_new(url);
        g_hash_table_replace(cache->weak_cache, entry->url, entry);
    }
    g_assert(entry != NULL);
    
    /* don't check entry->in_strong_cache here, so each load attempt 
     * has its own chance to end up in the strong cache
     */
    if (entry->pixbuf == NULL &&
        !entry->loading) {
        /* this will be newly-loaded so add our strong ref callback
         * which will add a strong ref on the pixbuf if the http 
         * request returns successfully.
         */
        StrongRefData *s;
        s = g_new0(StrongRefData, 1);
        s->cache = cache;
        s->entry = entry;
        ADD_WEAK(&s->cache);
        cache_entry_load(entry, strong_ref_on_load, s);
    }
    cache_entry_load(entry, func, data);
}
