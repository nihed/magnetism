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
        p = next_amp + 1;
    }

    /* Pre-Jan 2005 cookies may not have a host, we no longer accept that,
     * nor do we accept a mismatched host 
     */
    if (host == NULL || strcmp(required_host, host) != 0) {
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
    unsigned int count;
    char *guid;
    char *out;
    
    count = 0;
    for (p = jid; *p; p++) {
        if (*(p + 1) && *(p + 1) == '_') {
            count++;
            p++;
        }
        count++;
    }

    guid = g_new(char, count + 1);
    out = guid;
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

gboolean
hippo_parse_options(int          *argc_p,
                    char       ***argv_p,
                    HippoOptions *results)
{
    static gboolean debug = FALSE;
    static gboolean dogfood = FALSE;
    static gboolean config_flag = FALSE;
    static gboolean install_launch = FALSE;
    static gboolean replace_existing = FALSE;
    static gboolean quit_existing = FALSE;
    static gboolean initial_debug_share = FALSE;    static gboolean verbose = FALSE;    GError *error;

    static const GOptionEntry entries[] = {
        { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" },
        { "dogfood", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&dogfood, "Run against the dogfood (testing) server" },
        { "install-launch", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&install_launch, "Run appropriately at the end of the install" },
        { "replace", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&replace_existing, "Replace existing instance, if any" },
        { "quit", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&quit_existing, "Tell any existing instances to quit" },
        { "debug-share", 0, 0, G_OPTION_ARG_NONE, (gpointer)&initial_debug_share, "Show an initial dummy debug share" },
        { "verbose", 0, 0, G_OPTION_ARG_NONE, (gpointer)&verbose, "Print lots of debugging information" },
        { NULL }
    };

    GOptionContext *context = g_option_context_new("Mugshot");
    g_option_context_add_main_entries(context, entries, NULL);

    error = NULL;
    g_option_context_parse(context, argc_p, argv_p, &error);
    if (error) {
        g_printerr("%s\n", error->message);
        return FALSE;
    }
    g_option_context_free(context);

    if (debug)
        results->instance_type = HIPPO_INSTANCE_DEBUG;
    else if (dogfood)
        results->instance_type = HIPPO_INSTANCE_DOGFOOD;
    else
        results->instance_type = HIPPO_INSTANCE_NORMAL;    
    
    results->config_flag = config_flag;
    results->install_launch = install_launch;
    results->replace_existing = replace_existing;
    results->quit_existing = quit_existing;
    results->initial_debug_share = initial_debug_share;
    results->verbose = verbose;

    return TRUE;
}

void
hippo_options_free_fields(HippoOptions *options)
{
    /* Right now nothing is malloced in here so this is a no-op,
     * but if we had string args to options, etc. we'd have 
     * to free them here
     */
}

const char*
hippo_hotness_debug_string(HippoHotness hotness)
{
    switch (hotness) {
    case HIPPO_HOTNESS_COLD:
        return "COLD";
    case HIPPO_HOTNESS_COOL:
        return "COOL";
    case HIPPO_HOTNESS_WARM:
        return "WARM";
    case HIPPO_HOTNESS_GETTING_HOT:
        return "GETTING_HOT";
    case HIPPO_HOTNESS_HOT:
        return "HOT";
    case HIPPO_HOTNESS_UNKNOWN:
        return "UNKNOWN";
    }
    /* not a default case so we get a warning if we omit one from the switch */
    return "WHAT THE?";
}
