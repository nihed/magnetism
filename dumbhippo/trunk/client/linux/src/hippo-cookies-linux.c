#include "hippo-cookies-linux.h"
#include <string.h>
#include <stdlib.h>

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
    const char *p = server + strlen(server);

    if (p == server)
        return FALSE;

    *host = NULL;
    *port = 80;

    while (p > server) {
        if (*(p - 1) == ':') {
            gsize host_len = p - server - 1;
            
            if (host_len == 0)
                return FALSE;
            
            *host = g_strndup(server, host_len);

            if (*p) {
                char *end = NULL;
                long val;

                val = strtol(p, &end, 10);
                if (*end || end == p || val <= 0) {
                    g_free(*host);
                    return FALSE;
                } else {
                    *port = (int) val;
                }
            }
            break;
        }

        p--;
    }
    
    /* no ':' seen */
    if (*host == NULL) {
        *host = g_strdup(server);
    }
    
    return TRUE;
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
           const char *name_filter)
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
            cookie = hippo_cookie_new(domain, port, all_hosts_match,
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
static GSList*
hippo_load_cookies_file(const char *filename,
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
            parse_line(&cookies, line, p, domain, port, name);
            line = p + 1;
        }
    }
    
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
hippo_load_cookies(const char *domain,
                   int         port,
                   const char *name)
{
    GHashTable *merge;
    GSList *files;
    const char *homedir;
    GSList *merged_list;
    GDir *dir;
    char *firefox_dir;
    
    files = NULL;
    homedir = g_get_home_dir();

    /* We load the epiphany cookies, and the cookies from all copies of firefox; 
     * not really clear what we "should" do, how do we know which browser is someone's 
     * "main" or "current"? I guess if any browser has our login cookie, the user's 
     * account is effectively logged in from a security standpoint...
     */

    files = g_slist_prepend(files,
        g_build_filename(homedir,
        ".gnome2/epiphany/mozilla/epiphany/cookies.txt", NULL));

    firefox_dir = g_build_filename(homedir, ".mozilla/firefox", NULL);
    dir = g_dir_open(firefox_dir, 0, NULL); /* ignore errors */
    if (dir != NULL) {
        const char *subdir;
        while ((subdir = g_dir_read_name(dir)) != NULL) {
            char *cookie_file = g_build_filename(firefox_dir, subdir, "cookies.txt", NULL);
            if (g_file_test(cookie_file, G_FILE_TEST_EXISTS))
                files = g_slist_prepend(files, cookie_file);
        }
        g_dir_close(dir);
    }
    g_free(firefox_dir);
    
    merge = g_hash_table_new((GHashFunc)hippo_cookie_hash, (GEqualFunc) hippo_cookie_equals);

    while (files != NULL) {
        char *filename = files->data;
        GSList *cookies;
        GError *error;
        
        error = NULL;
        cookies = hippo_load_cookies_file(filename, domain, port, name, &error);
        if (error != NULL) {
            g_printerr("Failed to load '%s': %s\n", filename, error->message);
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
        
        files = g_slist_remove(files, files->data);
        g_free(filename);
    }

    /* Now listify the hash table */
    merged_list = NULL;
    g_hash_table_foreach(merge, listify_foreach, &merged_list);
    g_hash_table_destroy(merge);
    return merged_list;
}

#if 1
static void
print_and_eat_cookies(GSList *cookies)
{
    int count;
    
    count = 1;
    while (cookies != NULL) {
        HippoCookie *cookie = cookies->data;

        g_print("%d '%s:%d' all_hosts=%d path='%s' secure=%d time=%lu name='%s' value='%s'\n", count,
            hippo_cookie_get_domain(cookie),
            hippo_cookie_get_port(cookie),
            hippo_cookie_get_all_hosts_match(cookie),
            hippo_cookie_get_path(cookie),
            hippo_cookie_get_secure_connection_required(cookie),
            (unsigned long) hippo_cookie_get_timestamp(cookie),
            hippo_cookie_get_name(cookie),
            hippo_cookie_get_value(cookie));
        
        count += 1;
        
        cookies = g_slist_remove(cookies, cookies->data);
        hippo_cookie_unref(cookie);
    }
}

int
main(int argc, char **argv)
{
    GSList *cookies;
    int i;
    
    cookies = hippo_load_cookies(NULL, -1, NULL);
    print_and_eat_cookies(cookies);
    
    for (i = 1; i < argc; ++i) {
        GError *error = NULL;
        cookies = hippo_load_cookies_file(argv[i], NULL, -1, NULL, &error);
        if (error != NULL) {
            g_printerr("Failed to load '%s': %s\n", argv[i], error->message);
            g_error_free(error);
        }
        
        g_print("=== %s === ", argv[i]);   
        print_and_eat_cookies(cookies);
    }
    
    return 0;
}
#endif
