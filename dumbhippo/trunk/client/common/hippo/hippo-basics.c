#include "hippo-basics.h"

#include <string.h>
#include <stdlib.h>

gboolean
hippo_parse_server(const char *server,
                   char      **host,
                   int        *port)
{
    const char *p = server + strlen(server);

    if (p == server)
        return FALSE;

    *host = NULL;
    *port = -1;

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

static void
get_server(const char *server,
           const char *default_server,
           int         default_port,
           char      **host_p,
           int        *port_p)
{
    char *host = NULL;
    int port = -1;
    
    if (!hippo_parse_server(server, &host, &port)) {
        *host_p = g_strdup(default_server);
        *port_p = default_port;
        return;
    }
    
    if (port < 0)
        port = HIPPO_DEFAULT_MESSAGE_PORT;
    
    *host_p = host;
    *port_p = port;
}     

void
hippo_parse_message_server(const char *server,
                           char      **host_p,
                           int        *port_p)
{
    get_server(server, HIPPO_DEFAULT_MESSAGE_SERVER, HIPPO_DEFAULT_MESSAGE_PORT, host_p, port_p);
}

void
hippo_parse_web_server(const char *server,
                       char      **host_p,
                       int        *port_p)
{
    get_server(server, HIPPO_DEFAULT_WEB_SERVER, HIPPO_DEFAULT_WEB_PORT, host_p, port_p);
}

gboolean
hippo_parse_login_cookie(const char *cookie_value,
                         const char *required_host,
                         char      **username_p,
                         char      **password_p)
{
    char *host;
    char *username;
    char *password;
    const char *p;
    const char *end;
                
    /* If cookie is unset */
    if (cookie_value == NULL)
        return FALSE;
    
    /* Parse host=foo.example.com&name=GUID&password=LONGHEXNUMBER */    
    
    host = NULL;
    username = NULL;
    password = NULL;
    p = cookie_value;
    end = cookie_value + strlen(cookie_value);
    
    while (p < end) {
        const char *next_amp = strchr(p, '&');
        if (next_amp == NULL)
            next_amp = end;
        if (g_str_has_prefix(p, "host=")) {
            p += 5;
            if (p <= next_amp)
                host = g_strndup(p, next_amp - p);
        } else if (g_str_has_prefix(p, "name=")) {
            p += 5;
            if (p <= next_amp)
                username = g_strndup(p, next_amp - p);        
        } else if (g_str_has_prefix(p, "password=")) {
            p += 9;
            if (p <= next_amp)
                password = g_strndup(p, next_amp - p);
        } else {
            /* Ignore unknown field */
        }
        p = next_amp;
    }

    /* Pre-Jan 2005 cookies may not have a host */
    if (host != NULL && strcmp(required_host, host) != 0) {
        g_free(host);
        g_free(username);
        g_free(password);
        return FALSE;
    } else {
        g_free(host);
        *username_p = username;
        *password_p = password;
        return TRUE;   
    }
}
                        
char*
hippo_id_to_jabber_id(const char *guid)
{
    const char *p;
    GString *str = g_string_new(NULL);
    for (p = guid; *p; p++) {
        char c = *p;
        // A username in our system is alphanumeric, with case sensitivity
        // convert to lowercase only, by using _ to mark lowercase in the
        // original.
        if (c >= 'A' && c <= 'Z') {
            g_string_append_c(str, c + ('a' - 'A'));
        } else if (c >= 'a' && c <= 'z') {
            g_string_append_c(str, (char)c);
            g_string_append_c(str, '_');
        } else if (c >= '0' && c <= '9') {
            g_string_append_c(str, (char)c);
        }
    }

    return g_string_free(str, FALSE);
}

char*
hippo_id_from_jabber_id(const char *jid)
{
    const char *p;
    unsigned int count = 0;
    for (p = jid; *p; p++) {
        if (*(p + 1) && *(p + 1) == '_') {
            count++;
            p++;
        }
        count++;
    }

    char *guid = g_new(char, count + 1);
    char *out = guid;
    for (p = jid; *p; p++) {
        char c = *p;
        if (*(p + 1) && *(p + 1) == '_') {
            if (*p >= 'A' && c <= 'Z') {
                c = c + ('a' - 'A');
            }
            p++;
        } else {
            if (*p >= 'a' && c <= 'z') {
                c = c - ('a' - 'A');
            }
        }
        *(out++) = c;
    }
    *out = '\0';

    return guid;
}
