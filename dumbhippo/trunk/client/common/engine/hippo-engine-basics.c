/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippo-engine-basics.h"

#ifdef G_OS_WIN32
#define _WIN32_WINNT 0x0400 /* for IsDebuggerPresent() */
#include <HippoStdAfx.h>
#endif

#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>
#include "hippo-engine-internal.h"

static void hippo_basic_self_test(void);

gboolean
hippo_parse_int32(const char *s,
                  int        *result)
{
    /*
     * We accept values of the form '\s*\d+\s+'
     */
    
    char *end;
    long v;

    while (g_ascii_isspace(*s))
        ++s;
    
    if (*s == '\0')
        return FALSE;
    
    end = NULL;
    errno = 0;
    v = strtol(s, &end, 10);

    if (errno == ERANGE)
        return FALSE;

    while (g_ascii_isspace(*end))
        end++;

    if (*end != '\0')
        return FALSE;

    *result = v;

    return TRUE;
}

gboolean
hippo_parse_int64(const char *s,
                  gint64     *result)
{
    char *end;
    guint64 v;
    gboolean had_minus = FALSE;

    /*
     * We accept values of the form '\s*\d+\s+'.  
     *
     * FC5's glib does not have g_ascii_strtoll, only strtoull, so
     * we have an extra hoop or two to jump through.
     */
    while (g_ascii_isspace(*s))
        ++s;
    
    if (*s == '\0')
        return FALSE;
    
    if (*s == '-') {
        ++s;
        had_minus = TRUE;
    }

    end = NULL;
    errno = 0;
    v = g_ascii_strtoull(s, &end, 10);

    if (errno == ERANGE)
        return FALSE;

    while (g_ascii_isspace(*end))
        end++;

    if (*end != '\0')
        return FALSE;

    if (had_minus) {
        if (v > - (guint64) G_MININT64)
            return FALSE;
        
        *result = - (gint64) v;
    } else {
        if (v > G_MAXINT64)
            return FALSE;
        
        *result = (gint64) v;
    }
        
    return TRUE;
}

gboolean
hippo_parse_double (const char *s,
                    double     *result)
{
    /*
     * We accept values of the form '\s*\d+\s+'
     */
    
    char *end;
    double v;

    while (g_ascii_isspace(*s))
        ++s;
    
    if (*s == '\0')
        return FALSE;
    
    end = NULL;
    errno = 0;
    v = g_ascii_strtod(s, &end);
    
    if (errno == ERANGE)
        return FALSE;

    while (g_ascii_isspace(*end))
        end++;

    if (*end != '\0')
        return FALSE;

    *result = v;

    return TRUE;
}

/* No end of spec-compliance here, no doubt */
gboolean
hippo_parse_http_url (const char *url,
                      gboolean   *is_https,
                      char      **host,
                      int        *port)
{
    const char *server;
    char *no_slash;
    gboolean result;
    
    if (is_https)
        *is_https = FALSE;
    if (host)
        *host = NULL;
    if (port)
        *port = -1;
    
    if (g_str_has_prefix(url, "http://")) {
        server = url + 7;
    } else if (g_str_has_prefix(url, "https://")) {
        server = url + 8;
        if (is_https)
            *is_https = TRUE;
    } else {
        return FALSE;
    }

    no_slash = NULL;
    if (g_str_has_suffix(server, "/")) {
        no_slash = g_strndup(server, strlen(server) - 1);
    }
    
    result = hippo_parse_server(no_slash ? no_slash : server, host, port);

    g_free(no_slash);

    return result;
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

static void
print_debug_func(const char *message)
{
    g_printerr("%s\n", message);
}

static HippoPrintDebugFunc hippo_print_debug_func = print_debug_func;

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
static gboolean hippo_print_xmpp_noise = FALSE;

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
           if (!hippo_print_xmpp_noise)
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

void
hippo_initialize_logging(HippoOptions *options)
{
    /* run tests prior to doing other stuff; wedging it in here is pretty crude */
    if (options->instance_type == HIPPO_INSTANCE_DEBUG)
        hippo_basic_self_test();

    /* on Windows, the point of this is that stderr/stdout don't show up anywhere so 
     * we want to reroute; on Linux, the point is to suppress DEBUG level in some cases
     */
    g_log_set_default_handler(log_handler, NULL);

    hippo_print_debug_level = options->verbose;
    hippo_print_xmpp_noise = options->verbose_xmpp;
    if (hippo_print_debug_level || hippo_print_xmpp_noise) {
        hippo_override_loudmouth_log();
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
    test_version_parsing();
}
