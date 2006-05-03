#include "hippo-cookies.h"

struct HippoCookie {
    int refcount;
    char *domain;
    int         port;
    gboolean    all_hosts_match;
    char *path;
    gboolean    secure_connection_required;
    GTime       timestamp;
    char  *name;
    char  *value;
};

HippoCookie*
hippo_cookie_new(const char *domain,
                 int         port,
                 gboolean    all_hosts_match,
                 const char *path,
                 gboolean    secure_connection_required,
                 GTime       timestamp,
                 const char *name,
                 const char *value)
{
    HippoCookie *cookie = g_new0(HippoCookie, 1);
    
    g_return_val_if_fail(domain != NULL, NULL);
    g_return_val_if_fail(port > 0, NULL);
    g_return_val_if_fail(path != NULL, NULL);
    g_return_val_if_fail(name != NULL, NULL);
    /* value can be NULL */
    
    cookie->domain = g_strdup(domain);
    cookie->port = port;
    cookie->all_hosts_match = all_hosts_match != FALSE;
    cookie->path = g_strdup(path);
    cookie->secure_connection_required = secure_connection_required != FALSE;
    cookie->timestamp = timestamp;
    cookie->name = g_strdup(name);
    cookie->value = g_strdup(value);
    
    cookie->refcount = 1;
    
    return cookie;
}

void
hippo_cookie_ref    (HippoCookie *cookie)
{
    cookie->refcount += 1;
}

void
hippo_cookie_unref  (HippoCookie *cookie)
{
    g_return_if_fail(cookie != NULL);
    g_return_if_fail(cookie->refcount > 0);
    
    cookie->refcount -= 1;
    if (cookie->refcount == 0) {
        g_free(cookie->domain);
        g_free(cookie->path);
        g_free(cookie->name);
        g_free(cookie->value);
        g_free(cookie);
    }
}

gboolean
hippo_cookie_equals (HippoCookie *first,
                     HippoCookie *second)
{
    return
        strcmp(first->domain, second->domain) == 0 &&
        first->port == second->port &&
        first->all_hosts_match == second->all_hosts_match &&
        strcmp(first->path, second->path) == 0 &&
        first->secure_connection_required == second->secure_connection_required &&
        strcmp(first->name, second->name) == 0;
}
                     
guint
hippo_cookie_hash (HippoCookie *cookie)
{
    guint hash;
    
    /* very scientific approach */
    hash = g_str_hash(cookie->domain);
    hash += cookie->port * 37;
    hash += g_str_hash(cookie->path) * 37;
    hash += g_str_hash(cookie->name) * 37;
    return hash;
}

const char*
hippo_cookie_get_domain(HippoCookie *cookie)
{
    return cookie->domain;
}

int
hippo_cookie_get_port(HippoCookie *cookie)
{
    return cookie->port;
}

gboolean
hippo_cookie_get_all_hosts_match(HippoCookie *cookie)
{
    return cookie->all_hosts_match;
}

const char*
hippo_cookie_get_path(HippoCookie *cookie)
{
    return cookie->path;
}

gboolean
hippo_cookie_get_secure_connection_required(HippoCookie *cookie)
{
    return cookie->secure_connection_required;
}

GTime
hippo_cookie_get_timestamp(HippoCookie *cookie)
{
    return cookie->timestamp;
}

const char*
hippo_cookie_get_name(HippoCookie *cookie)
{
    return cookie->name;
}

const char*
hippo_cookie_get_value(HippoCookie *cookie)
{
    return cookie->value;
}

