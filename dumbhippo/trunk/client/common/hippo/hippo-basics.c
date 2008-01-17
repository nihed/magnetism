/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-basics.h"

#include <errno.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>
#include "hippo-common-internal.h"

GQuark
hippo_error_quark(void)
{
    return g_quark_from_static_string("hippo-error-quark");
}

#define VALID_GUID_CHAR(c)                    \
             (((c) >= '0' && (c) <= '9') ||   \
              ((c) >= 'A' && (c) <= 'Z') ||   \
              ((c) >= 'a' && (c) <= 'z'))
#define GUID_LEN 14

/* should be robust against untrusted data, e.g. we 
 * parse urls and config files with it
 */
gboolean
hippo_parse_server(const char *server,
                   char      **host,
                   int        *port)
{
    const char *p;

    if (host)
        *host = NULL;
    if (port)
        *port = -1;

    if (!g_utf8_validate(server, -1, NULL))
        return FALSE;

    p = server + strlen(server);
    if (p == server)
        return FALSE;

    while (p > server) {
        if (*(p - 1) == ':') {
            gsize host_len;
            char *end;
            long val;

            host_len = p - server - 1;
            if (host_len == 0)
                return FALSE;
            
            if (host)
                *host = g_strndup(server, host_len);

            end = NULL;
            val = strtol(p, &end, 10);
            if (*end || end == p || val <= 0) {
                if (host) {
                    g_free(*host);
                    *host = NULL;
                }
                return FALSE;
            } else {
                if (port)
                    *port = (int) val;
            }
            break;
        }

        p--;
    }
    
    /* no ':' seen */
    if (host && *host == NULL) {
        *host = g_strdup(server);
    }
    
    return TRUE;
}

static void
get_server(const char         *server,
           HippoInstanceType   instance_type,
           HippoServerType     server_type,
           HippoServerProtocol protocol,
           char              **host_p,
           int                *port_p)
{
    char *host = NULL;
    int port = -1;
    
    if (!hippo_parse_server(server, &host, &port)) {
        const char *default_server;

        default_server = hippo_get_default_server(instance_type, server_type, protocol);

        if (!hippo_parse_server(default_server, &host, &port))
            g_assert_not_reached();
    }
    
    if (port < 0) {
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            port = 80;
            break;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            port = 5222;
            break;
        }
        g_assert(port >= 0);
    }
    
    *host_p = host;
    *port_p = port;
}     

void
hippo_parse_message_server(const char       *server,
                           HippoInstanceType instance_type,
                           HippoServerType   server_type,                         
                           char            **host_p,
                           int              *port_p)
{    
    get_server(server, instance_type, server_type, HIPPO_SERVER_PROTOCOL_MESSAGE, host_p, port_p);
}

void
hippo_parse_web_server(const char       *server,
                       HippoInstanceType instance_type,
                       HippoServerType   server_type,                         
                       char            **host_p,
                       int              *port_p)
{
    get_server(server, instance_type, server_type, HIPPO_SERVER_PROTOCOL_WEB, host_p, port_p);
}

/* this returns a value with the port in it already */
static const char *
get_debug_server(HippoServerType     server_type,
                 HippoServerProtocol protocol)
{
    const char *server = g_getenv("HIPPO_DEBUG_SERVER");
    if (server)
        return server;
    
    switch (server_type) {
    case HIPPO_SERVER_DESKTOP:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_DESKTOP_LOCAL_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_DESKTOP_LOCAL_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    case HIPPO_SERVER_STACKER:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_STACKER_LOCAL_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_STACKER_LOCAL_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    }

    g_assert_not_reached();

    return NULL;
}

static const char *
get_dogfood_server(HippoServerType     server_type,
                   HippoServerProtocol protocol)
{
    const char *server = g_getenv("HIPPO_DOGFOOD_SERVER");
    if (server)
        return server;
    
    switch (server_type) {
    case HIPPO_SERVER_DESKTOP:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_DESKTOP_DOGFOOD_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_DESKTOP_DOGFOOD_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    case HIPPO_SERVER_STACKER:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_STACKER_DOGFOOD_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_STACKER_DOGFOOD_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    }

    g_assert_not_reached();

    return NULL;
}

static const char *
get_production_server(HippoServerType     server_type,
                      HippoServerProtocol protocol)
{
    const char *server = g_getenv("HIPPO_PRODUCTION_SERVER");
    if (server)
        return server;
    
    switch (server_type) {
    case HIPPO_SERVER_DESKTOP:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_DESKTOP_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_DESKTOP_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    case HIPPO_SERVER_STACKER:
        switch (protocol) {
        case HIPPO_SERVER_PROTOCOL_WEB:
            return HIPPO_DEFAULT_STACKER_WEB_SERVER;
        case HIPPO_SERVER_PROTOCOL_MESSAGE:
            return HIPPO_DEFAULT_STACKER_MESSAGE_SERVER;
        }
        g_assert_not_reached();
        return NULL;
    }

    g_assert_not_reached();

    return NULL;
}

const char*
hippo_get_default_server(HippoInstanceType   instance_type,
                         HippoServerType     server_type,
                         HippoServerProtocol protocol)
{
    /* Check env variables that force a particular web/message
     * server regardless of instance
     */
    if (protocol == HIPPO_SERVER_PROTOCOL_WEB) {
        const char *web_server = g_getenv("HIPPO_WEB_SERVER");
        if (web_server)
            return web_server;
    } else {
        const char *message_server = g_getenv("HIPPO_MESSAGE_SERVER");
        if (message_server)
            return message_server;
    }

    /* Then try per-instance env variables and defaults */
    switch (instance_type) {
    case HIPPO_INSTANCE_NORMAL:
        return get_production_server(server_type, protocol);
    case HIPPO_INSTANCE_DOGFOOD:
        return get_dogfood_server(server_type, protocol);
    case HIPPO_INSTANCE_DEBUG:
        return get_debug_server(server_type, protocol);
    }

    g_assert_not_reached();

    return NULL;
}

gboolean
hippo_parse_options(int          *argc_p,
                    char       ***argv_p,
                    HippoOptions *results)
{
    static gboolean debug = FALSE;
    static gboolean dogfood = FALSE;
    static gboolean install_launch = FALSE;
    static gboolean replace_existing = FALSE;
    static gboolean quit_existing = FALSE;
    static gboolean initial_debug_share = FALSE;
    static gboolean verbose = FALSE;
    static gboolean verbose_xmpp = FALSE;
    static gboolean debug_updates = FALSE;
    static gboolean no_show_window = FALSE;
    static char *crash_dump = NULL;
    char *argv0;
    GError *error;
    GOptionContext *context;
    /* something to consider when adding entries is that the process is a singleton 
     * (well, singleton per server instance).
     * So you really shouldn't add something unless it can (logically speaking) 
     * be forwarded to an existing instance.
     * On Linux, consider mugshot-uri-handler instead, on Windows consider a COM method instead.
     */
    static const GOptionEntry entries[] = {
        { "crash-dump", '\0', 0, G_OPTION_ARG_STRING, (gpointer)&crash_dump, "Report a crash using the specified crash dump" },
        { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" },
        { "dogfood", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&dogfood, "Run against the dogfood (testing) server" },
        { "install-launch", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&install_launch, "Run appropriately at the end of the install" },
        { "replace", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&replace_existing, "Replace existing instance, if any" },
        { "quit", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&quit_existing, "Tell any existing instances to quit" },
        { "debug-share", 0, 0, G_OPTION_ARG_NONE, (gpointer)&initial_debug_share, "Show an initial dummy debug share" },
        { "debug-updates", 0, 0, G_OPTION_ARG_NONE, (gpointer)&debug_updates, "Show debugging animation for display updates" },
        { "verbose", 0, 0, G_OPTION_ARG_NONE, (gpointer)&verbose, "Print lots of debugging information" },
        { "verbose-xmpp", 0, 0, G_OPTION_ARG_NONE, (gpointer)&verbose_xmpp, "Print lots of debugging information about raw XMPP traffic" },
        { "no-show-window", 0, 0, G_OPTION_ARG_NONE, (gpointer)&no_show_window, "If already running, don't ask the existing one to open a window" },
        { NULL }
    };

    /* save this away before the option parser might get it */
    argv0 = g_strdup((*argv_p)[0]);
    
    /* the argument here is a little odd, look at where it goes in --help output
     * before changing it
     */
    context = g_option_context_new("");
    g_option_context_add_main_entries(context, entries, NULL);

    error = NULL;
    g_option_context_parse(context, argc_p, argv_p, &error);
    if (error) {
        g_free(argv0);
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
    
    results->install_launch = install_launch;
    results->replace_existing = replace_existing;
    results->quit_existing = quit_existing;
    results->initial_debug_share = initial_debug_share;
    results->verbose = verbose;
    results->verbose_xmpp = verbose_xmpp;
    results->debug_updates = debug_updates;
    results->crash_dump = g_strdup(crash_dump);
    results->show_window = ! no_show_window;
    
    /* build an argv suitable for exec'ing in order to restart this 
     * instance of the process ... used when the user chooses "restart"
     * so --replace is added.
     * Allocate space for all our command line options, plus argv[0], plus NULL term
     */
    results->restart_argv = g_new0(char*, G_N_ELEMENTS(entries) + 2);
    results->restart_argv[0] = argv0; /* pass ownership of argv0 */
    results->restart_argc = 1;
    results->restart_argv[results->restart_argc] = g_strdup("--replace");
    results->restart_argc += 1;
    if (results->instance_type == HIPPO_INSTANCE_DEBUG) {
        results->restart_argv[results->restart_argc] = g_strdup("--debug");
        results->restart_argc += 1;
    } else if (results->instance_type == HIPPO_INSTANCE_DOGFOOD) {
        results->restart_argv[results->restart_argc] = g_strdup("--dogfood");
        results->restart_argc += 1;
    }
    if (results->verbose) {
        results->restart_argv[results->restart_argc] = g_strdup("--verbose");
        results->restart_argc += 1;
    }
    if (results->verbose_xmpp) {
        results->restart_argv[results->restart_argc] = g_strdup("--verbose-xmpp");
        results->restart_argc += 1;
    }

    /* Always use --no-show-window when restarting since whether to show is a property
     * of the context we were launched in, not a persistent property of the app
     */
    results->restart_argv[results->restart_argc] = g_strdup("--no-show-window");
    results->restart_argc += 1;
    
    return TRUE;
}

void
hippo_options_free_fields(HippoOptions *options)
{
    g_free(options->crash_dump);
    g_strfreev(options->restart_argv);
}


/* Parse a positive integer, return false if parsing fails */
static gboolean
parse_positive_int(const char *str, 
                   gsize       len,
                   int        *result_p)
{
    char *end;
    unsigned long val;
    
    if (len == 0)
        return FALSE;
    if (str[0] < '0' || str[0] > '9') // don't support white space, +, -
        return FALSE;

    end = NULL;
    val = strtoul(str, &end, 10);
    if (end != (str + len))
        return FALSE;
    if (val > G_MAXINT)
        return FALSE;
    if (result_p)
        *result_p = (int)val;

    return TRUE;
}

/* parse a major.minor.micro triplet. Returns false and sets
 * major, minor, micro to 0 if parsing fails. Micro is allowed
 * to be implicitly 0 if missing.
 */
static gboolean
parse_version(const char *version,
              int        *major_p,
              int        *minor_p,
              int        *micro_p)
{
    const char *first_dot;
    const char *second_dot;
    const char *end;
    const char *minor_end;
    int major, minor, micro;
    
    major = 0;
    minor = 0;
    micro = 0;
    
    end = version + strlen(version);
    
    first_dot = strchr(version, '.');
    
    if (first_dot == NULL)
        goto failed;
    
    second_dot = strchr(first_dot + 1, '.');
    if (second_dot)
        minor_end = second_dot;
    else
        minor_end = end;

    if (!parse_positive_int(version, first_dot - version, &major))
        goto failed;

    if (!parse_positive_int(first_dot + 1, minor_end - (first_dot + 1), &minor))
        goto failed;
    
    if (second_dot) {
        if (!parse_positive_int(second_dot + 1, end - (second_dot + 1), &micro))
            goto failed;
    }

    if (major_p)
        *major_p = major;
    if (minor_p)
        *minor_p = minor;        
    if (micro_p)
        *micro_p = micro;

    return TRUE;
    
  failed:
    if (major_p)
        *major_p = 0;
    if (minor_p)
        *minor_p = 0;
    if (micro_p)
        *micro_p = 0;
    return FALSE;
}

/* compare to major.minor.micro version strings. Unparseable
 * strings are treated the same as 0.0.0. Missing micro is 
 * treated as micro 0, missing major/minor treated as unparseable.
 */
int
hippo_compare_versions(const char *version_a,
                       const char *version_b)
{
    int major_a, minor_a, micro_a;
    int major_b, minor_b, micro_b;

    parse_version(version_a, &major_a, &minor_a, &micro_a);
    parse_version(version_b, &major_b, &minor_b, &micro_b);

    if (major_a < major_b)
        return -1;
    else if (major_a > major_b)
        return 1;
    else if (minor_a < minor_b)
        return -1;
    else if (minor_a > minor_b)
        return 1;
    else if (micro_a < micro_b)
        return -1;
    else if (micro_a > micro_b)
        return 1;
    else
        return 0;
}

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

HippoChatKind
hippo_parse_chat_kind(const char *str)
{
    if (strcmp(str, "post") == 0)
        return HIPPO_CHAT_KIND_POST;
    else if (strcmp(str, "group") == 0)
        return HIPPO_CHAT_KIND_GROUP;
    else if (strcmp(str, "music") == 0)
        return HIPPO_CHAT_KIND_MUSIC;
    else if (strcmp(str, "block") == 0)
        return HIPPO_CHAT_KIND_BLOCK;
    else if (strcmp(str, "unknown") == 0)
        return HIPPO_CHAT_KIND_UNKNOWN;
    else
        return HIPPO_CHAT_KIND_BROKEN;
}

const char*
hippo_chat_kind_as_string(HippoChatKind kind)
{
    switch (kind) {
    case HIPPO_CHAT_KIND_POST:
        return "post";
    case HIPPO_CHAT_KIND_GROUP:
        return "group";
    case HIPPO_CHAT_KIND_MUSIC:
        return "music";
    case HIPPO_CHAT_KIND_BLOCK:
        return "block";
    case HIPPO_CHAT_KIND_UNKNOWN:
        return "unknown";
    case HIPPO_CHAT_KIND_BROKEN:
        return "broken";
    }
    
    g_warning("Invalid HippoChatKind value %d", kind);
    return NULL;
}

gboolean 
hippo_parse_sentiment(const char     *str,
                      HippoSentiment *sentiment)
{
    if (strcmp(str, "INDIFFERENT") == 0) {
        *sentiment = HIPPO_SENTIMENT_INDIFFERENT;
        return TRUE;
    } else if (strcmp(str, "LOVE") == 0) {
        *sentiment = HIPPO_SENTIMENT_LOVE;
        return TRUE;
    } else if (strcmp(str, "HATE") == 0) {
        *sentiment = HIPPO_SENTIMENT_HATE;
        return TRUE;
    }

    return FALSE;
}

const char *
hippo_sentiment_as_string(HippoSentiment sentiment)
{
    switch (sentiment) {
    case HIPPO_SENTIMENT_INDIFFERENT:
        return "INDIFFERENT";
    case HIPPO_SENTIMENT_LOVE:
        return "LOVE";
    case HIPPO_SENTIMENT_HATE:
        return "HATE";
    }

    g_warning("Invalid HippoSentiment value %d", sentiment);
    return NULL;
}

/* rint doesn't exist on Windows */
static double 
hippo_rint(double n)
{
    double ci, fl;
    ci = ceil(n);
    fl = floor(n);
    return (((ci-n) >= (n-fl)) ? fl :ci);
}

/* improvements to this should probably go in the javascript version too */
char*
hippo_format_time_ago(GTime now,
                      GTime then)
{
    GTime delta = now - then;
    double delta_hours;
    double delta_weeks;
    double delta_years;

    if (then <= 0)
        return g_strdup("");
    
    if (delta < 0)
        return g_strdup("the future");

    if (delta < 120)
        return g_strdup("a minute ago");

    if (delta < 60*60) {
        int delta_minutes = delta / 60;
        if (delta_minutes > 5)
            delta_minutes = delta_minutes - (delta_minutes % 5);
        return g_strdup_printf("%d minutes ago", delta_minutes);
    }

    delta_hours = delta / (60.0 * 60.0);

    if (delta_hours < 1.55)
        return g_strdup("1 hr. ago");

    if (delta_hours < 24) {
        return g_strdup_printf("%.0f hrs. ago", hippo_rint(delta_hours));
    }

    if (delta_hours < 48) {
        return g_strdup("Yesterday");
    }
    
    if (delta_hours < 24*15) {
        return g_strdup_printf("%.0f days ago", hippo_rint(delta_hours / 24));
    }

    delta_weeks = delta_hours / (24.0 * 7.0);

    if (delta_weeks < 6) {
        return g_strdup_printf("%.0f weeks ago", hippo_rint(delta_weeks));
    }

    if (delta_weeks < 50) {
        return g_strdup_printf("%.0f months ago", hippo_rint(delta_weeks / 4));
    }

    delta_years = delta_weeks / 52;

    if (delta_years < 1.55)
        return g_strdup_printf("1 year ago");

    return g_strdup_printf("%.0f years ago", hippo_rint(delta_years));
}


char*
hippo_size_photo_url(const char *base_url,
                     int         size)
{
    if (strchr(base_url, '?') != 0)
        return g_strdup_printf("%s&size=%d", base_url, size);
    else
        return g_strdup_printf("%s?size=%d", base_url, size);
}

gint64
hippo_current_time_ms(void)
{
    GTimeVal now;

    g_get_current_time(&now);
    return (gint64)now.tv_sec * 1000 + now.tv_usec / 1000;
}

gboolean
hippo_membership_status_from_string(const char            *s,
                                    HippoMembershipStatus *result)
{
    static const struct { const char *name; HippoMembershipStatus status; } statuses[] = {
        { "NONMEMBER", HIPPO_MEMBERSHIP_STATUS_NONMEMBER },
        { "INVITED_TO_FOLLOW", HIPPO_MEMBERSHIP_STATUS_INVITED_TO_FOLLOW },
        { "FOLLOWER", HIPPO_MEMBERSHIP_STATUS_FOLLOWER },
        { "REMOVED", HIPPO_MEMBERSHIP_STATUS_REMOVED },
        { "INVITED", HIPPO_MEMBERSHIP_STATUS_INVITED },
        { "ACTIVE", HIPPO_MEMBERSHIP_STATUS_ACTIVE }
    };
    unsigned int i;
    for (i = 0; i < G_N_ELEMENTS(statuses); ++i) {
        if (strcmp(s, statuses[i].name) == 0) {
            *result = statuses[i].status;
            return TRUE;
        }
    }
    g_warning("Unknown membership status '%s'", s);
    return FALSE;
}
