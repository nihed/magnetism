#include "hippo-cookies.h"
#include "hippo-basics.h"

#include <string.h>
#include <stdlib.h>

struct HippoCookie {
    int refcount;
    HippoBrowserKind origin_browser;
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
hippo_cookie_new(HippoBrowserKind origin_browser,
                 const char      *domain,
                 int              port,
                 gboolean         all_hosts_match,
                 const char      *path,
                 gboolean         secure_connection_required,
                 GTime            timestamp,
                 const char      *name,
                 const char      *value)
{
    HippoCookie *cookie = g_new0(HippoCookie, 1);
    
    g_return_val_if_fail(domain != NULL, NULL);
    g_return_val_if_fail(port > 0, NULL);
    g_return_val_if_fail(path != NULL, NULL);
    g_return_val_if_fail(name != NULL, NULL);
    /* value can be NULL */
    
    cookie->origin_browser = origin_browser;
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
        first->origin_browser == second->origin_browser &&
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
    hash = g_direct_hash(GINT_TO_POINTER(cookie->origin_browser));
    hash += g_str_hash(cookie->domain) * 37;
    hash += cookie->port * 37;
    hash += g_str_hash(cookie->path) * 37;
    hash += g_str_hash(cookie->name) * 37;
    return hash;
}

HippoBrowserKind
hippo_cookie_get_origin_browser(HippoCookie *cookie)
{
    return cookie->origin_browser;
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



/* === cookies.txt parser === */


/* cookies.txt format description, taken from wget source code 
   (but our implementation is original):

       DOMAIN DOMAIN-FLAG PATH SECURE-FLAG TIMESTAMP ATTR-NAME ATTR-VALUE

     DOMAIN      -- cookie domain, optionally followed by :PORT
     DOMAIN-FLAG -- whether all hosts in the domain match
     PATH        -- cookie path
     SECURE-FLAG -- whether cookie requires secure connection
     TIMESTAMP   -- expiry timestamp, number of seconds since epoch
     ATTR-NAME   -- name of the cookie attribute
     ATTR-VALUE  -- value of the cookie attribute (empty if absent)

   The fields are separated by TABs.  All fields are mandatory, except
   for ATTR-VALUE.  The `-FLAG' fields are boolean, their legal values
   being "TRUE" and "FALSE'.  Empty lines, lines consisting of
   whitespace only, and comment lines (beginning with # optionally
   preceded by whitespace) are ignored.

   Example line from cookies.txt (split in two lines for readability):

       .google.com	TRUE	/	FALSE	2147368447	\
       PREF	ID=34bb47565bbcd47b:LD=en:NR=20:TM=985172580:LM=985739012

*/

/* Note the cookie name and value aren't allowed to have semicolon, comma, or whitespace, 
 * so there's no escaping or anything involved here. No encoding is defined for what the
 * server wants to store so we should not do any interpretation.
 */

enum {
    DOMAIN,
    DOMAIN_FLAG,
    PATH,
    SECURE_FLAG,
    TIMESTAMP,
    ATTR_NAME,
    ATTR_VALUE,
    N_FIELDS
};

typedef struct {
    int   which;
    char *text;
} Field;

static gboolean
is_empty_line(const char *line,
              const char *end)
{
    const char *p;
    
    for (p = line; p != end; ++p) {
        if (g_ascii_isspace(*p)) {
            ; /* nothing, continue */
        } else if (*p == '#') {
            return TRUE; /* first non-white char is # */
        } else {
            return FALSE; /* non-white non-comment char is first */
        }
    }
    return TRUE; /* only saw whitespace */
}

static gboolean
parse_domain(const char *server,
             char      **host,
             int        *port)
{
    /* In theory this function isn't quite right (since cookie domain could e.g. 
     * start with a period, and prefs domain could not) but we don't validate much
     * right now anyway
     */            
    if (hippo_parse_server(server, host, port)) {
        if (*port < 0)
            *port = 80;
        return TRUE;
    } else {
        return FALSE;
    }
}

static gboolean
parse_bool(const char *text,
           gboolean   *val)
{
    if (strcmp(text, "TRUE") == 0) {
        *val = TRUE;
        return TRUE;
    } else if (strcmp(text, "FALSE") == 0) {
        *val = FALSE;
        return TRUE;
    } else {
        return FALSE;
    }
}

static gboolean
parse_time(const char *text,
           GTime      *timestamp)
{
    unsigned long val;
    char *end = NULL;

    val = strtoul(text, &end, 10);
    if (*end || end == text || val == 0) {
        return FALSE;
    } else {
        *timestamp = val;
        return TRUE;
    }
}

static void
parse_line(GSList    **cookies_p,
           const char *line,
           const char *end,
           const char *domain_filter,
           int         port_filter,
           const char *name_filter,
           HippoBrowserKind browser)
{
    const char *p;
    const char *start;
    int field;
    Field fields[N_FIELDS];
    
    // see if it's an empty or comment line
    if (is_empty_line(line, end))
        return;
        
    for (field = 0; field < N_FIELDS; ++field) {
        fields[field].which = field;
        fields[field].text = NULL;
    }
    
    start = line;
    field = 0;
    for (p = line; p <= end; ++p) {
        g_assert(p >= start);
        if (*p == '\t' || p == end) {
            
            if (field >= N_FIELDS) {
                // too many fields on this line, give up
                goto out;
            }
            
            fields[field].text = g_strndup(start, p - start);
            
            start = p + 1;
            ++field;
        }
    }
    
    /* ATTR_VALUE is optional, the other fields are not */
    for (field = 0; field < N_FIELDS; ++field) {
        if (field != ATTR_VALUE && fields[field].text == NULL)
            goto out;
    }    
    
    {
        char *domain;
        int port;
        gboolean all_hosts_match;
        gboolean secure_connection_required;
        GTime timestamp;

        if (!parse_bool(fields[DOMAIN_FLAG].text, &all_hosts_match))
            goto out;
            
        if (!parse_bool(fields[SECURE_FLAG].text, &secure_connection_required))
            goto out;
            
        if (!parse_time(fields[TIMESTAMP].text, &timestamp))
            goto out;

        if (!parse_domain(fields[DOMAIN].text, &domain, &port))
            goto out;
        
        if ((domain_filter == NULL || strcmp(domain_filter, domain) == 0) &&
            (port_filter < 0 || port_filter == port) &&
            (name_filter == NULL || strcmp(name_filter, fields[ATTR_NAME].text) == 0)) {
            HippoCookie *cookie;
            cookie = hippo_cookie_new(browser,
                                      domain, port, all_hosts_match,
                                      fields[PATH].text,
                                      secure_connection_required, timestamp,
                                      fields[ATTR_NAME].text,
                                      fields[ATTR_VALUE].text);
            *cookies_p = g_slist_prepend(*cookies_p, cookie);
        }
                                  
        g_free(domain);
    }
                        
  out:
    for (field = 0; field < N_FIELDS; ++field) {
        g_free(fields[field].text);
    }    
}

/* NULL domain, NULL name, -1 port act as "wildcard" for this function */
GSList*
hippo_load_cookies_file(HippoBrowserKind browser,
                        const char *filename,
                        const char *domain,
                        int         port,
                        const char *name,
                        GError    **error)
{
    char *contents = NULL;
    gsize length = 0;
    const char *p;
    const char *end;
    const char *line;
    GSList *cookies = NULL;
    
    if (!g_file_get_contents(filename, &contents, &length, error)) {
        return NULL;
    }
    
    end = contents + length + 1; /* end is AFTER the nul term so we can parse with no ending newline */
    line = contents;
    for (p = contents; p != end; ++p) {
        g_assert(p >= line);
        /* \r\n comes out as an extra empty line and gets ignored */
        if (*p == '\r' || *p == '\n' || *p == '\0') {
            parse_line(&cookies, line, p, domain, port, name, browser);
            line = p + 1;
        }
    }
    
    g_free(contents);
    
    /* we've been prepending, so reverse */
    return g_slist_reverse(cookies);
}

static void
listify_foreach(void *key, void *value, void *data)
{
    GSList **list_p = data;
    *list_p = g_slist_prepend(*list_p, value);
    hippo_cookie_ref(value);
}

GSList*
hippo_load_cookies_files(const HippoCookiesFile *files,
                         int         n_files,
                         const char *domain,
                         int         port,
                         const char *name)
{
    GHashTable *merge;
    GSList *merged_list;
    int i;
    
    merge = g_hash_table_new((GHashFunc)hippo_cookie_hash, (GEqualFunc) hippo_cookie_equals);

    for (i = 0; i < n_files; ++i) {
        char *filename = files[i].filename;
        HippoBrowserKind browser = files[i].browser;
        GSList *cookies;
        GError *error;
        
        error = NULL;
        cookies = hippo_load_cookies_file(browser, filename, domain, port, name, &error);
        if (error != NULL) {
            /* g_printerr("Failed to load '%s': %s\n", filename, error->message); */
            g_error_free(error); 
        }
        
        while (cookies != NULL) {
            HippoCookie *cookie = cookies->data;
    
            HippoCookie *old = g_hash_table_lookup(merge, cookie);
            if (old != NULL) {
                /* Save the cookie that expires latest */
                if (hippo_cookie_get_timestamp(old) < hippo_cookie_get_timestamp(cookie)) {
                    /* replace the old one */
                    g_hash_table_replace(merge, cookie, cookie);
                } else {
                    /* keep old one, delete this one */
                    hippo_cookie_unref(cookie);
                }
            } else {
                g_hash_table_replace(merge, cookie, cookie);
            }

            cookies = g_slist_remove(cookies, cookies->data);        
        }
    }

    /* Now listify the hash table */
    merged_list = NULL;
    g_hash_table_foreach(merge, listify_foreach, &merged_list);
    g_hash_table_destroy(merge);
    return merged_list;
}                         
