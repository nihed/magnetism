/* Copyright 2000-2005 The Apache Software Foundation or its licensors, as
 * applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MOD_CACHE_H
#define MOD_CACHE_H 

/*
 * Main include file for the Apache Transparent Cache
 */

#define CORE_PRIVATE

#include "apr_hooks.h"
#include "apr.h"
#include "apr_lib.h"
#include "apr_strings.h"
#include "apr_buckets.h"
#include "apr_md5.h"
#include "apr_pools.h"
#include "apr_strings.h"
#include "apr_optional.h"
#define APR_WANT_STRFUNC
#include "apr_want.h"

#include "httpd.h"
#include "http_config.h"
#include "ap_config.h"
#include "http_core.h"
#include "http_protocol.h"
#include "http_request.h"
#include "http_vhost.h"
#include "http_main.h"
#include "http_log.h"
#include "http_connection.h"
#include "util_filter.h"
#include "apr_date.h"
#include "apr_uri.h"

#ifdef HAVE_NETDB_H
#include <netdb.h>
#endif

#ifdef HAVE_SYS_SOCKET_H
#include <sys/socket.h>
#endif

#ifdef HAVE_NETINET_IN_H
#include <netinet/in.h>
#endif

#ifdef HAVE_ARPA_INET_H
#include <arpa/inet.h>
#endif

#include "apr_atomic.h"

#ifndef MAX
#define MAX(a,b)                ((a) > (b) ? (a) : (b))
#endif
#ifndef MIN
#define MIN(a,b)                ((a) < (b) ? (a) : (b))
#endif

/* default completion is 60% */
#define DEFAULT_CACHE_COMPLETION (60)
#define MSEC_ONE_DAY    ((apr_time_t)(86400*APR_USEC_PER_SEC)) /* one day, in microseconds */
#define MSEC_ONE_HR     ((apr_time_t)(3600*APR_USEC_PER_SEC))  /* one hour, in microseconds */
#define MSEC_ONE_MIN    ((apr_time_t)(60*APR_USEC_PER_SEC))    /* one minute, in microseconds */
#define MSEC_ONE_SEC    ((apr_time_t)(APR_USEC_PER_SEC))       /* one second, in microseconds */
#define DEFAULT_CACHE_MAXEXPIRE MSEC_ONE_DAY
#define DEFAULT_CACHE_EXPIRE    MSEC_ONE_HR
#define DEFAULT_CACHE_LMFACTOR  (0.1)

/* Create a set of PROXY_DECLARE(type), PROXY_DECLARE_NONSTD(type) and 
 * PROXY_DECLARE_DATA with appropriate export and import tags for the platform
 */
#if !defined(WIN32)
#define CACHE_DECLARE(type)            type
#define CACHE_DECLARE_NONSTD(type)     type
#define CACHE_DECLARE_DATA
#elif defined(CACHE_DECLARE_STATIC)
#define CACHE_DECLARE(type)            type __stdcall
#define CACHE_DECLARE_NONSTD(type)     type
#define CACHE_DECLARE_DATA
#elif defined(CACHE_DECLARE_EXPORT)
#define CACHE_DECLARE(type)            __declspec(dllexport) type __stdcall
#define CACHE_DECLARE_NONSTD(type)     __declspec(dllexport) type
#define CACHE_DECLARE_DATA             __declspec(dllexport)
#else
#define CACHE_DECLARE(type)            __declspec(dllimport) type __stdcall
#define CACHE_DECLARE_NONSTD(type)     __declspec(dllimport) type
#define CACHE_DECLARE_DATA             __declspec(dllimport)
#endif

struct cache_enable {
    const char *url;
    const char *type;
    apr_size_t urllen;
};

struct cache_disable {
    const char *url;
    apr_size_t urllen;
};

/* static information about the local cache */
typedef struct {
    apr_array_header_t *cacheenable;	/* URLs to cache */
    apr_array_header_t *cachedisable;	/* URLs not to cache */
    apr_array_header_t *hippo_always;	/* URLs to cache even when authorized */
    apr_time_t maxex;			/* Maximum time to keep cached files in msecs */
    int maxex_set;
    apr_time_t defex;           /* default time to keep cached file in msecs */
    int defex_set;
    double factor;              /* factor for estimating expires date */
    int factor_set;
    int complete;               /* Force cache completion after this point */
    int complete_set;
    /** ignore the last-modified header when deciding to cache this request */
    int no_last_mod_ignore_set;
    int no_last_mod_ignore; 
    /** ignore client's requests for uncached responses */
    int ignorecachecontrol;
    int ignorecachecontrol_set;
    /** store the headers that should not be stored in the cache */
    apr_array_header_t *ignore_headers;
    /* flag if CacheIgnoreHeader has been set */
    #define CACHE_IGNORE_HEADERS_SET   1
    #define CACHE_IGNORE_HEADERS_UNSET 0
    int ignore_headers_set;
    const char *hippo_server_name;
    apr_size_t hippo_server_name_len;
    /** cache requests with query strings even when there is no Expires: */
    int hippo_ignore_no_expires_set;
    int hippo_ignore_no_expires;
} cache_server_conf;

/* cache info information */
typedef struct cache_info cache_info;
struct cache_info {
    int status;
    char *content_type;
    char *etag;
    char *lastmods;         /* last modified of cache entity */
    char *filename;   
    apr_time_t date;
    apr_time_t lastmod;
    char lastmod_str[APR_RFC822_DATE_LEN];
    apr_time_t expire;
    apr_time_t request_time;
    apr_time_t response_time;
    apr_size_t len;
    apr_time_t ims;    /*  If-Modified_Since header value    */
    apr_time_t ius;    /*  If-UnModified_Since header value    */
    const char *im;         /* If-Match header value */
    const char *inm;         /* If-None-Match header value */
};

/* cache handle information */

/* XXX TODO On the next structure change/MMN bump, 
 * count must become an apr_off_t, representing
 * the potential size of disk cached objects.
 * Then dig for
 * "XXX Bad Temporary Cast - see cache_object_t notes" 
 */
typedef struct cache_object cache_object_t;
struct cache_object {
    char *key;
    cache_object_t *next;
    cache_info info;
    void *vobj;         /* Opaque portion (specific to the cache implementation) of the cache object */
    apr_size_t count;   /* Number of body bytes written to the cache so far */
    int complete;
    apr_atomic_t refcount;
    apr_size_t cleanup;
};

typedef struct cache_handle cache_handle_t;

#define CACHE_PROVIDER_GROUP "cache"

typedef struct {
    int (*remove_entity) (cache_handle_t *h);
    apr_status_t (*store_headers)(cache_handle_t *h, request_rec *r, cache_info *i);
    apr_status_t (*store_body)(cache_handle_t *h, request_rec *r, apr_bucket_brigade *b);
    apr_status_t (*recall_headers) (cache_handle_t *h, request_rec *r);
    apr_status_t (*recall_body) (cache_handle_t *h, apr_pool_t *p, apr_bucket_brigade *bb); 
    int (*create_entity) (cache_handle_t *h, request_rec *r,
                           const char *urlkey, apr_off_t len);
    int (*open_entity) (cache_handle_t *h, request_rec *r,
                           const char *urlkey);
    int (*remove_url) (const char *urlkey);
} cache_provider;

/* A linked-list of authn providers. */
typedef struct cache_provider_list cache_provider_list;

struct cache_provider_list {
    const char *provider_name;
    const cache_provider *provider;
    cache_provider_list *next;
};

struct cache_handle {
    cache_object_t *cache_obj;
    apr_table_t *req_hdrs;        /* cached request headers */
    apr_table_t *resp_hdrs;       /* cached response headers */
    apr_table_t *resp_err_hdrs;   /* cached response err headers */
    const char *content_type;     /* cached content type */
    int status;                   /* cached status */
};

/* per request cache information */
typedef struct {
    cache_provider_list *providers;         /* possible cache providers */
    const cache_provider *provider;         /* current cache provider */
    const char *provider_name;              /* current cache provider name */
    int fresh;				/* is the entitey fresh? */
    cache_handle_t *handle;		/* current cache handle */
    cache_handle_t *stale_handle;	/* stale cache handle */
    apr_table_t *stale_headers;		/* original request headers. */
    int in_checked;			/* CACHE_SAVE must cache the entity */
    int block_response;			/* CACHE_SAVE must block response. */
    apr_bucket_brigade *saved_brigade;  /* copy of partial response */
    apr_off_t saved_size;               /* length of saved_brigade */
    apr_time_t exp;                     /* expiration */
    apr_time_t lastmod;                 /* last-modified time */
    cache_info *info;                   /* current cache info */
} cache_request_rec;


/* cache_util.c */
/* do a HTTP/1.1 age calculation */
CACHE_DECLARE(apr_time_t) ap_cache_current_age(cache_info *info, const apr_time_t age_value,
                                               apr_time_t now);

/**
 * Check the freshness of the cache object per RFC2616 section 13.2 (Expiration Model)
 * @param h cache_handle_t
 * @param r request_rec
 * @return 0 ==> cache object is stale, 1 ==> cache object is fresh
 */
CACHE_DECLARE(int) ap_cache_check_freshness(cache_handle_t *h, request_rec *r);
CACHE_DECLARE(apr_time_t) ap_cache_hex2usec(const char *x);
CACHE_DECLARE(void) ap_cache_usec2hex(apr_time_t j, char *y);
CACHE_DECLARE(char *) generate_name(apr_pool_t *p, int dirlevels, 
                                    int dirlength, 
                                    const char *name);
CACHE_DECLARE(int) ap_cache_request_is_conditional(apr_table_t *table);
CACHE_DECLARE(cache_provider_list *)ap_cache_get_providers(request_rec *r, cache_server_conf *conf, const char *url);
CACHE_DECLARE(int) ap_cache_liststr(apr_pool_t *p, const char *list,
                                    const char *key, char **val);
CACHE_DECLARE(const char *)ap_cache_tokstr(apr_pool_t *p, const char *list, const char **str);

/* Create a new table consisting of those elements from a request_rec's
 * headers_out that are allowed to be stored in a cache
 */
CACHE_DECLARE(apr_table_t *)ap_cache_cacheable_hdrs_out(apr_pool_t *pool,
                                                        apr_table_t *t,
                                                        server_rec *s);

/**
 * cache_storage.c
 */
int cache_remove_url(request_rec *r, char *url);
int cache_create_entity(request_rec *r, char *url, apr_off_t size);
int cache_select_url(request_rec *r, char *url);
apr_status_t cache_generate_key_default( request_rec *r, apr_pool_t*p, char**key );
/**
 * create a key for the cache based on the request record
 * this is the 'default' version, which can be overridden by a default function
 */
const char* cache_create_key( request_rec*r );

/*
apr_status_t cache_store_entity_headers(cache_handle_t *h, request_rec *r, cache_info *info);
apr_status_t cache_store_entity_body(cache_handle_t *h, request_rec *r, apr_bucket_brigade *bb);

apr_status_t cache_recall_entity_headers(cache_handle_t *h, request_rec *r);
apr_status_t cache_recall_entity_body(cache_handle_t *h, apr_pool_t *p, apr_bucket_brigade *bb);
*/

/**
 * hippo_cookie.c
 */
int hippo_cache_check(request_rec *r, cache_server_conf *conf, const char *url);

/* hooks */

/* Create a set of CACHE_DECLARE(type), CACHE_DECLARE_NONSTD(type) and 
 * CACHE_DECLARE_DATA with appropriate export and import tags for the platform
 */
#if !defined(WIN32)
#define CACHE_DECLARE(type)            type
#define CACHE_DECLARE_NONSTD(type)     type
#define CACHE_DECLARE_DATA
#elif defined(CACHE_DECLARE_STATIC)
#define CACHE_DECLARE(type)            type __stdcall
#define CACHE_DECLARE_NONSTD(type)     type
#define CACHE_DECLARE_DATA
#elif defined(CACHE_DECLARE_EXPORT)
#define CACHE_DECLARE(type)            __declspec(dllexport) type __stdcall
#define CACHE_DECLARE_NONSTD(type)     __declspec(dllexport) type
#define CACHE_DECLARE_DATA             __declspec(dllexport)
#else
#define CACHE_DECLARE(type)            __declspec(dllimport) type __stdcall
#define CACHE_DECLARE_NONSTD(type)     __declspec(dllimport) type
#define CACHE_DECLARE_DATA             __declspec(dllimport)
#endif

APR_DECLARE_OPTIONAL_FN(apr_status_t, 
                        ap_cache_generate_key, 
                        (request_rec *r, apr_pool_t*p, char**key ));


#endif /*MOD_CACHE_H*/
