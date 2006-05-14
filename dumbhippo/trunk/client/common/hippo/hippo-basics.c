#include "hippo-basics.h"

#ifdef G_OS_WIN32
#define _WIN32_WINNT 0x0400 /* for IsDebuggerPresent() */
#include <HippoStdAfx.h>
#endif

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

#define VALID_GUID_CHAR(c)                    \
             (((c) >= '0' && (c) <= '9') ||   \
              ((c) >= 'A' && (c) <= 'Z') ||   \
              ((c) >= 'a' && (c) <= 'z'))
#define GUID_LEN 14

/* keep in sync with below */
gboolean
hippo_verify_guid(const char *possible_guid)
{
    const char *p;
    p = possible_guid;
    while (*p) {
        if (!VALID_GUID_CHAR(*p))
              return FALSE;
            
        ++p;
    }
    if ((p - possible_guid) != GUID_LEN)
        return FALSE;
        
    return TRUE;
}

/* keep in sync with above */
gboolean
hippo_verify_guid_wide(const gunichar2 *possible_guid)
{
    const gunichar2 *p;
    
    p = possible_guid;
    while (*p) {
        if (!VALID_GUID_CHAR(*p))
              return FALSE;
            
        ++p;
    }
    if ((p - possible_guid) != (sizeof(gunichar2) * GUID_LEN))
        return FALSE;
        
    return TRUE;
}

static HippoPrintDebugFunc hippo_print_debug_func = NULL;

void
hippo_set_print_debug_func(HippoPrintDebugFunc func)
{
    hippo_print_debug_func = func;
}

/* A little cut-and-paste out of loudmouth to avoid including all 
 * of its headers here; kind of lame, sue me. It's just for debug spew.
 */
typedef enum {
    LM_LOG_LEVEL_VERBOSE = 1 << (G_LOG_LEVEL_USER_SHIFT),
    LM_LOG_LEVEL_NET     = 1 << (G_LOG_LEVEL_USER_SHIFT + 1),
    LM_LOG_LEVEL_PARSER  = 1 << (G_LOG_LEVEL_USER_SHIFT + 2),
    LM_LOG_LEVEL_ALL     = (LM_LOG_LEVEL_NET |
                LM_LOG_LEVEL_VERBOSE |
                LM_LOG_LEVEL_PARSER)
} LmLogLevelFlags;

#ifndef LM_LOG_DOMAIN
#  define LM_LOG_DOMAIN "LM"
#endif

static gboolean hippo_print_debug_level = FALSE;

static void
log_handler(const char    *log_domain,
            GLogLevelFlags log_level,
            const char    *message,
            void          *user_data)
{
    const char *prefix;
    GString *gstr;

    if (log_level & G_LOG_FLAG_RECURSION) {
        (*hippo_print_debug_func)("Mugshot: log recursed");
        return;
    }

    switch (log_level & G_LOG_LEVEL_MASK) {
        case LM_LOG_LEVEL_PARSER:
            return; /* don't care */
            break;    
        case LM_LOG_LEVEL_VERBOSE:
        case LM_LOG_LEVEL_NET:
           if (!hippo_print_debug_level)
               return;
            prefix = "LM: ";
            break;
        case G_LOG_LEVEL_DEBUG:
            if (!hippo_print_debug_level)
                return;
            prefix = "DEBUG: ";
            break;
        case G_LOG_LEVEL_WARNING:
            prefix = "WARNING: ";
            break;
        case G_LOG_LEVEL_CRITICAL:
            prefix = "CRITICAL: ";
            break;
        case G_LOG_LEVEL_ERROR:
            prefix = "ERROR: ";
            break;
        case G_LOG_LEVEL_INFO:
            prefix = "INFO: ";
            break;
        case G_LOG_LEVEL_MESSAGE:
            prefix = "MESSAGE: ";
            break;
        default:
            prefix = "";
            break;
    }

    gstr = g_string_new("Mugshot: ");
    
    g_string_append(gstr, prefix);
    g_string_append(gstr, message);

    /* no newline here, the print_debug_func is supposed to add it */
    if (gstr->str[gstr->len - 1] == '\n') {
        g_string_erase(gstr, gstr->len - 1, 1);
    }

    (*hippo_print_debug_func)(gstr->str);
    g_string_free(gstr, TRUE);

#ifdef G_OS_WIN32
    // glib will do this for us, but if we abort in our own code which has
    // debug symbols, visual studio gets less confused about the backtrace.
    // at least, that's my experience.
    if (log_level & G_LOG_FLAG_FATAL) {
        if (IsDebuggerPresent())
            G_BREAKPOINT();
        abort();
    }
#endif
}

/* Our loudmouth has a patch so this can be called before loudmouth is 
 * used, but on Linux with stock loudmouth this has to be called 
 * after loudmouth init
 */
void
hippo_override_loudmouth_log(void)
{
    g_log_set_handler(LM_LOG_DOMAIN,
                      (GLogLevelFlags) (LM_LOG_LEVEL_ALL | G_LOG_FLAG_FATAL | G_LOG_FLAG_RECURSION),
                      log_handler, NULL);
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
    static gboolean initial_debug_share = FALSE;
    static gboolean verbose = FALSE;
    static char *join_chat_id = NULL;
    GError *error;
    GOptionContext *context;
    static const GOptionEntry entries[] = {
        { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" },
        { "dogfood", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&dogfood, "Run against the dogfood (testing) server" },
        { "install-launch", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&install_launch, "Run appropriately at the end of the install" },
        { "replace", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&replace_existing, "Replace existing instance, if any" },
        { "quit", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&quit_existing, "Tell any existing instances to quit" },
        { "join-chat", '\0', 0, G_OPTION_ARG_STRING, (gpointer)&join_chat_id, "Join a chat", "CHAT_ID" },        
        { "debug-share", 0, 0, G_OPTION_ARG_NONE, (gpointer)&initial_debug_share, "Show an initial dummy debug share" },
        { "verbose", 0, 0, G_OPTION_ARG_NONE, (gpointer)&verbose, "Print lots of debugging information" },
        { NULL }
    };

    /* on Windows, the point of this is that stderr/stdout don't show up anywhere so 
     * we want to reroute; on Linux, the point is to suppress DEBUG level in some cases
     */
    g_log_set_default_handler(log_handler, NULL);
    
    context = g_option_context_new("Mugshot");
    g_option_context_add_main_entries(context, entries, NULL);

    error = NULL;
    g_option_context_parse(context, argc_p, argv_p, &error);
    if (error) {
        g_printerr("%s\n", error->message);
        return FALSE;
    }
    g_option_context_free(context);

    if (join_chat_id && !hippo_verify_guid(join_chat_id)) {
        g_printerr("Invalid chat id '%s'\n", join_chat_id);
        return FALSE;
    }

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
    results->join_chat_id = join_chat_id;

    if (results->verbose) {
        hippo_print_debug_level = TRUE;
        hippo_override_loudmouth_log();
    }

    return TRUE;
}

void
hippo_options_free_fields(HippoOptions *options)
{
    g_free(options->join_chat_id);
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
