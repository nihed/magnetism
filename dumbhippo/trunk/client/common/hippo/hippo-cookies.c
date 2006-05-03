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
    cookie->domain = g_strdup(domain);
    cookie->port = port;
    cookie->all_hosts_match = all_hosts_match;
    cookie->path = g_strdup(path);
    cookie->secure_connection_required = secure_connection_required;
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

