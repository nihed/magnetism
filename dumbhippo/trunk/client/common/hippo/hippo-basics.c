/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-basics.h"

#ifdef G_OS_WIN32
#define _WIN32_WINNT 0x0400 /* for IsDebuggerPresent() */
#include <HippoStdAfx.h>
#endif

#include <string.h>
#include <stdlib.h>
#include <math.h>
#include "hippo-common-internal.h"

static void hippo_basic_self_test(void);

GQuark
hippo_error_quark(void)
{
    return g_quark_from_static_string("hippo-error-quark");
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
    p = jid;
    while (*p != '\0') {
        if (*(p + 1) && *(p + 1) == '_') {
            ++count;
            ++p;
        }
        ++count;
        ++p;
    }

    guid = g_new(char, count + 1);
    out = guid;
    p = jid;
    while (*p != '\0') {
        char c = *p;
        if (*(p + 1) && *(p + 1) == '_') {
            if (*p >= 'A' && c <= 'Z') {
                c = c + ('a' - 'A');
            }
            ++p;
        } else {
            if (*p >= 'a' && c <= 'z') {
                c = c - ('a' - 'A');
            }
        }
        *(out++) = c;
        ++p;
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
    static gboolean install_launch = FALSE;
    static gboolean replace_existing = FALSE;
    static gboolean quit_existing = FALSE;
    static gboolean initial_debug_share = FALSE;
    static gboolean verbose = FALSE;
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
        { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" },
        { "dogfood", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&dogfood, "Run against the dogfood (testing) server" },
        { "install-launch", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&install_launch, "Run appropriately at the end of the install" },
        { "replace", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&replace_existing, "Replace existing instance, if any" },
        { "quit", '\0', 0, G_OPTION_ARG_NONE, (gpointer)&quit_existing, "Tell any existing instances to quit" },
        { "debug-share", 0, 0, G_OPTION_ARG_NONE, (gpointer)&initial_debug_share, "Show an initial dummy debug share" },
        { "verbose", 0, 0, G_OPTION_ARG_NONE, (gpointer)&verbose, "Print lots of debugging information" },
        { NULL }
    };

    g_assert(hippo_print_debug_func != NULL);

    /* on Windows, the point of this is that stderr/stdout don't show up anywhere so 
     * we want to reroute; on Linux, the point is to suppress DEBUG level in some cases
     */
    g_log_set_default_handler(log_handler, NULL);

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

    /* run tests prior to doing other stuff */
    if (debug)
        hippo_basic_self_test();

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

    if (results->verbose) {
        hippo_print_debug_level = TRUE;
        hippo_override_loudmouth_log();
    }

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
    
    return TRUE;
}

void
hippo_options_free_fields(HippoOptions *options)
{
    g_strfreev(options->restart_argv);
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

/* FIXME not really clear on how uri escaping relates to 
 * character encoding, i.e. is %NN a binary byte which expands
 * to part of a utf-8 char, or is %NN the Unicode codepoint?
 * 
 * For now using the latter, it's academic anyhow since our urls
 * contain no human-readable text, so as long as we don't crash
 * it won't come up.
 */
static char*
unescape_uri(const char *uri)
{
    const char *p;
    GString *unescaped;
    
    unescaped = g_string_new(NULL);
    
    p = uri;
    while (*p) {
        if (*p == '%') {
            char hex[3] = "\0\0\0";
            long c;
            
            hex[0] = *(p + 1);
            if (hex[0] == '\0')
                goto failed;
            hex[1] = *(p + 2);
            if (hex[1] == '\0')
                goto failed;
            p += 2;
            
            if (!(g_ascii_isxdigit(hex[0]) &&
                  g_ascii_isxdigit(hex[1])))
                goto failed;
            
            c = strtol(hex, NULL, 16);

            g_assert(c < 256);
            
            /* catch encoded nul byte */
            if (c == 0) {
                goto failed;
            }
            
            /* FIXME see note above, should it be append_c ?
             * in that case we'd need to utf8_validate at the end
             * of this routine.
             */
            g_string_append_unichar(unescaped, (gunichar) c);
        } else {
            g_string_append_c(unescaped, *p);
        }
    
        ++p;
    }
    
    return g_string_free(unescaped, FALSE);
    
  failed:
    g_string_free(unescaped, TRUE);
    return NULL;    
}

/* return value indicates error; if no param left, 
 * that just means *key_p == NULL
 */
static gboolean
get_param(const char  *str, 
          const char **next_p,
          char       **key_p,
          char       **value_p,
          GError     **error)
{
    const char *equals;
    const char *end;
    gsize key_len;
    gsize value_len;
    
    *key_p = NULL;
    *value_p = NULL;
    
    if (*str == '\0')
        return TRUE; /* not an error, we're at end */
    
    equals = strchr(str, '=');
    if (equals == NULL) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("No = sign after URI parameter name '%s'"),
                    str);
        return FALSE;
    }
    key_len = equals - str;
    if (key_len == 0) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
            _("No key name before '=' in URI query string"));
        return FALSE;
    }
    
    end = strchr(equals + 1, '&');
    if (end == NULL)
        end = str + strlen(str);
    value_len = end - equals - 1;
    g_assert(value_len >= 0);
    /* value_len == 0 is valid, the value is empty string */
    
    *key_p = g_strndup(str, key_len);
    *value_p = g_strndup(equals + 1, value_len);
    
    *next_p = end;
    
    return TRUE;
}

/* assumes whole URI was unescaped, but not params ... 
 * though we don't unescape params right now, not sure 
 * if "double escape" is the norm for params? anyway 
 * for now we never use non-ascii params anyhow.
 */
static gboolean
parse_params(const char          *str,
             HippoUriActionData  *data,
             GError             **error)
{
    const char *p;
    char *key;
    char *value;

    key = NULL;
    value = NULL;
    
    /* init defaults (assume already zero'd) */
    switch (data->action) {
    case HIPPO_URI_ACTION_JOIN_CHAT:
        data->u.join_chat.kind = HIPPO_CHAT_KIND_UNKNOWN;
        break;
    case HIPPO_URI_ACTION_BROKEN:
        break;    
    }
    
    p = str;
    if (*p) {
        /* param string can be empty, but otherwise it has to start with '?' */
        if (*p != '?') {
            g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                        _("Parameter string does not start with '?' in URI"));
            goto failed;
        } else {
            ++p;
        }
    }
    
    if (!get_param(p, &p, &key, &value, error))
        goto failed;
    while (key != NULL) {
        /* we ignore unknown params */
        switch (data->action) {
        case HIPPO_URI_ACTION_JOIN_CHAT:
            if (strcmp(key, "id") == 0) {
                if (!hippo_verify_guid(value)) {
                    g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                        _("In joinChat?id=, '%s' is not a valid chat ID"), value);
                    goto failed;
                }
                data->u.join_chat.chat_id = value;
                value = NULL; /* steal it */
            } else if (strcmp(key, "kind") == 0) {
                HippoChatKind kind;
                kind = hippo_parse_chat_kind(value);
                if (kind == HIPPO_CHAT_KIND_BROKEN) {
                    g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                        _("Don't know how to join chat kind '%s'"), value);
                    goto failed;
                }
                data->u.join_chat.kind = kind;
            }
            break;
        case HIPPO_URI_ACTION_BROKEN:
            break;
        }

        g_free(key);
        g_free(value);
        key = NULL;
        value = NULL;
        if (!get_param(p, &p, &key, &value, error))
            goto failed;    
    }

    g_assert(key == NULL);
    g_assert(value == NULL);

    /* Check we have everything */
    switch (data->action) {
    case HIPPO_URI_ACTION_JOIN_CHAT:
        if (data->u.join_chat.chat_id == NULL) {
            g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                _("joinChat URI missing id= parameter"));
            goto failed;
        }
        break;
    case HIPPO_URI_ACTION_BROKEN:
        break;    
    }
    
    g_assert(error == NULL || *error == NULL);
    return TRUE;
    
  failed:
    g_assert(error == NULL || *error != NULL);
    
    g_free(key);
    g_free(value);
    /* caller has to free "data" */
    return FALSE;
}

/*
 * Parses our URI scheme. Not sure we'll stick to this long term, but 
 * it allows using a protocol handler instead of a Firefox extension or 
 * ActiveX control.
 * 
 * The scheme is defined as follows:
 * 
 *   mugshot://host:port/actionName?key1=value&key2=value
 * 
 * where encoding of the url and parameters are as for an http URL.
 * Our parsing isn't very clever right now, though.
 * 
 */
gboolean
hippo_parse_uri(const char         *uri,
                HippoUriActionData *data,
                GError            **error)
{
    char *unescaped;
    const char *p;
    const char *slash;
    char *s;
    
    /* Remember "uri" is untrusted data. */

    g_return_val_if_fail(error == NULL || *error == NULL, FALSE);
    g_return_val_if_fail(uri != NULL, FALSE);
    g_return_val_if_fail(data != NULL, FALSE);    
    
    memset(data, '\0', sizeof(*data));
    data->action = HIPPO_URI_ACTION_BROKEN;
    
    if (!g_utf8_validate(uri, -1, NULL)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
            _("URI contains invalid UTF-8"));
        return FALSE;
    }
    
    /* FIXME I'm not 100% clear on how full-url escaping works with param escaping; 
     * i.e. if you escape an entire url, are the params normally "double escaped"?
     * Also, currently this allows escape sequences in the uri scheme and so forth,
     * which is probably screwy. All needs fixing up...
     */
    
    unescaped = unescape_uri(uri);
    if (unescaped == NULL) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
            _("URI contains invalid hex escape sequence (should be '%%7F' for example)"));
        return FALSE;
    }
    
    p = unescaped;
    
    if (*p == '\0') {
        /* this would also come up as "bad uri scheme" but this error 
         * message is nicer
         */
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("URI is an empty string"));
        goto failed;
    }
    
    if (strncmp(p, HIPPO_URI_SCHEME, HIPPO_URI_SCHEME_LEN) != 0) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("URI does not have the scheme '%s:'"),
                    HIPPO_URI_SCHEME); 
        goto failed;
    }
    p += HIPPO_URI_SCHEME_LEN;

    if (strncmp(p, "://", 3) != 0) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("URI does not have '://' after '%s' scheme name"),
                    HIPPO_URI_SCHEME); 
        goto failed;
    }
    p += 3;
        
    slash = strchr(p, '/');
    if (slash == NULL) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("No '/' character after hostname in URI"));
        goto failed;
    } else if (slash == p) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("No hostname in URI"));
        goto failed;
    }
    
    s = g_strndup(p, slash - p);
    if (!hippo_parse_server(s, NULL, NULL)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                    _("Failed to parse hostname:port in URI"));
        g_free(s);
        goto failed;
    }
    if (data)
        data->server = s;
    else
        g_free(s);

    p = slash + 1;
    
    if (strncmp(p, "joinChat", strlen("joinChat")) == 0) {
        data->action = HIPPO_URI_ACTION_JOIN_CHAT;
        p += strlen("joinChat");
    } else {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
                _("Did not recognize requested action in '%s' URI: '%s'"),
                HIPPO_URI_SCHEME, p);
        goto failed;
    }
    
    if (!parse_params(p, data, error)) {
        goto failed;
    }
    
    g_free(unescaped);
    return TRUE;
    
  failed:
    g_assert(error == NULL || *error != NULL);
    
    g_free(unescaped);
    
    hippo_uri_action_data_free_fields(data);
    
    memset(data, '\0', sizeof(*data));    
    data->action = HIPPO_URI_ACTION_BROKEN;
    return FALSE;
}

void
hippo_uri_action_data_free_fields(HippoUriActionData *data)
{
    switch (data->action) {
    case HIPPO_URI_ACTION_JOIN_CHAT:
        g_free(data->u.join_chat.chat_id);
        break;
    case HIPPO_URI_ACTION_BROKEN:
        break;
    }
    g_free(data->server);
    data->action = HIPPO_URI_ACTION_BROKEN;
}

HippoChatKind
hippo_parse_chat_kind(const char *str)
{
    if (strcmp(str, "post") == 0)
        return HIPPO_CHAT_KIND_POST;
    else if (strcmp(str, "group") == 0)
        return HIPPO_CHAT_KIND_GROUP;
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
    case HIPPO_CHAT_KIND_UNKNOWN:
        return "unknown";
    case HIPPO_CHAT_KIND_BROKEN:
        return "broken";
    }
    
    g_warning("Invalid HippoChatKind value %d", kind);
    return NULL;
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

/* improvements to this should probably go in the javascript version too */
char*
hippo_format_time_ago(GTime now,
                      GTime then)
{
    GTime delta = now - then;
    double delta_hours;
    double delta_weeks;
    double delta_years;
    
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

static const char*
hippo_uri_valid_tests[] = { 
    /* both chat kinds */
    "mugshot://example.com/joinChat?id=zL4BQF0ZfgV39V&kind=post",
    "mugshot://example.com/joinChat?id=zL4BQF0ZfgV39V&kind=group",
    /* escape the "z" in the guid */
    "mugshot://example.com/joinChat?id=%7AL4BQF0ZfgV39V&kind=group",
    /* missing chat kind, assumes UNKNOWN */
    "mugshot://example.com/joinChat?id=zL4BQF0ZfgV39V",
    /* with port */
    "mugshot://example.com:8080/joinChat?id=zL4BQF0ZfgV39V&kind=post",
    /* 1 char long host (ok, not a valid hostname, but we don't validate that) */
    "mugshot://e/joinChat?id=zL4BQF0ZfgV39V&kind=post" 
};

static const char*
hippo_uri_invalid_tests[] = { 
    /* empty string */
    "",
    /* not our kind of URI */
    "http://example.com/",
    /* unknown action */
    "mugshot://example.com/unknownAction",
    /* missing action */
    "mugshot://example.com/",    
    /* 1 char long action */
    "mugshot://example.com/a",
    /* missing chat id */
    "mugshot://example.com/joinChat",
    /* invalid GUID (too short) */
    "mugshot://example.com/joinChat?id=4BQF0ZfgV39V&kind=group",
    /* bad escaping (invalid hex chars) */
    "mugshot://example.com:8080/%NNjoinChat?id=zL4BQF0ZfgV39V&kind=post",
    /* bad escaping (escaped nul) */
    "mugshot://example.com:8080/%00joinChat?id=zL4BQF0ZfgV39V&kind=post",
    /* empty port string */
    "mugshot://example.com:/joinChat?id=zL4BQF0ZfgV39V&kind=post"
};

static void
test_uri_parsing(void)
{
    unsigned int i;
    HippoUriActionData data;
    GError *error;
    
    for (i = 0; i < G_N_ELEMENTS(hippo_uri_valid_tests); ++i) {
        const char *uri = hippo_uri_valid_tests[i];
        error = NULL;
        if (!hippo_parse_uri(uri, &data, &error)) {
            g_assert(error != NULL);
            g_error("Failed to parse valid test uri '%s': %s",
                    uri, error->message);
        }
        g_assert(error == NULL);
        hippo_uri_action_data_free_fields(&data);
    }

    for (i = 0; i < G_N_ELEMENTS(hippo_uri_invalid_tests); ++i) {
        const char *uri = hippo_uri_invalid_tests[i];
        error = NULL;
        if (hippo_parse_uri(uri, &data, &error)) {
            g_error("Successfully parsed invalid test uri '%s'", uri);
        }
        g_assert(error != NULL);
        /* g_printerr("Error: %s\n", error->message); */
        g_error_free(error);
        /* should not have to free data on failure */
    }
}

static void
test_version_parsing(void)
{
#define CMP hippo_compare_versions
    g_assert(CMP("0.0.0", "0.0.0") == 0);

    g_assert(CMP("1.0.0", "1.0.0") == 0);    
    g_assert(CMP("1.0.0", "0.0.0") > 0);
    g_assert(CMP("0.0.0", "1.0.0") < 0);

    g_assert(CMP("0.1.0", "0.1.0") == 0);
    g_assert(CMP("0.1.0", "0.0.0") > 0);
    g_assert(CMP("0.0.0", "0.1.0") < 0);

    g_assert(CMP("0.0.1", "0.0.1") == 0);
    g_assert(CMP("0.0.1", "0.0.0") > 0);
    g_assert(CMP("0.0.0", "0.0.1") < 0);

    g_assert(CMP("1.1.0", "1.1") == 0);
    g_assert(CMP("1.1.1", "1.1") > 0);
    g_assert(CMP("1.1.0", "1.1.1") < 0);
}

static void
hippo_basic_self_test(void)
{
    test_uri_parsing();
    test_version_parsing();
}
