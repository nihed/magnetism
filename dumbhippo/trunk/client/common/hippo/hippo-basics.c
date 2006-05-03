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
