/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <gtk/gtk.h>
#include <glib/gi18n-lib.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-client.h"

typedef enum {
    HIPPO_URI_ACTION_BROKEN,
    HIPPO_URI_ACTION_JOIN_CHAT
} HippoUriAction;

typedef struct {
    HippoUriAction action;
    char *server;
    union {
        struct {
            char         *chat_id;
            HippoChatKind kind;
        } join_chat;
    } u;
} HippoUriActionData;

static void hippo_uri_action_data_free_fields(HippoUriActionData *data);

static GMainLoop *loop = NULL;
static int gtk_count = 0;

#define HIPPO_URI_SCHEME     "mugshot"
#define HIPPO_URI_SCHEME_LEN 7


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
static gboolean
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

static void
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

static gboolean
enter_gtk(void)
{
    int argc;
    char **argv;
    
    argc = 1;
    argv = g_new0(char*, 1);
    argv[0] = g_get_prgname();
    argv[1] = NULL;
    
    gtk_count += 1;
    if (gtk_count == 1) {
        if (!gtk_init_check(&argc, &argv)) {
            gtk_count -= 1;
            g_free(argv);
            return FALSE;
        }
        loop = g_main_loop_new(NULL, FALSE);
    }
    g_free(argv);
    return TRUE;
}

static void
leave_gtk(void)
{
    g_return_if_fail(gtk_count > 0);
    gtk_count -= 1;
    if (gtk_count == 0) {
        g_main_loop_quit(loop);
    }
}

static void
on_destroy_dialog(GtkWidget *dialog)
{
    leave_gtk();
}

static void error_dialog(GError *error, const char *format, ...) G_GNUC_PRINTF(2, 3);

static void
error_dialog(GError *error, const char *format, ...)
{
    va_list args;
    char *s;
    GtkWidget *dialog;
    
    va_start(args, format);
    s = g_strdup_vprintf(format, args);
    va_end(args);
    
    if (!enter_gtk()) {
        g_printerr(_("Failed to open dialog, can't initialize X display"));
        g_printerr("%s\n", s);
        g_free(s);
        if (error)
            g_printerr("%s\n", error->message);
        return;
    }
    
    dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                    GTK_BUTTONS_CLOSE, "%s", s);
    g_free(s);
    
    if (error) {
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
    }
  
    g_signal_connect(G_OBJECT(dialog), "response", G_CALLBACK(gtk_widget_destroy), NULL);
    g_signal_connect(G_OBJECT(dialog), "destroy", G_CALLBACK(on_destroy_dialog), NULL);
    
    gtk_widget_show(dialog);
}

int
main(int argc, char **argv)
{
    GError *error;
    GOptionContext *context;
    int i;
    static const char **uris = NULL;
    static const GOptionEntry entries[] = {
        /* { "debug", 'd', 0, G_OPTION_ARG_NONE, (gpointer)&debug, "Run in debug mode" }, */
        { G_OPTION_REMAINING, '\0', 0, G_OPTION_ARG_STRING_ARRAY, (gpointer)&uris, NULL },
        { NULL }
    };

    /* please translate only "Link Handler" not "Mugshot" since "Mugshot" is a proper name */
    g_set_application_name(_("Mugshot Link Handler"));

    context = g_option_context_new("URI1 URI2 URI3 ...");
    g_option_context_add_main_entries(context, entries, NULL);

    error = NULL;
    g_option_context_parse(context, &argc, &argv, &error);

    /* Be careful to avoid too much dialog spam... one per URI at most */

    if (error) {
        error_dialog(error, _("Incorrect options provided to %s"), g_get_application_name());
        g_error_free(error);
    } else if (uris == NULL || uris[0] == NULL) {
        error_dialog(NULL, _("No link was provided to %s"),
                     g_get_application_name());
    } else {    
        for (i = 0; uris[i] != NULL; ++i) {
            HippoUriActionData data;

            /* be sure to keep only 1 error dialog per URI */
            error = NULL;
            if (!hippo_parse_uri(uris[i], &data, &error)) {
                error_dialog(error, _("Couldn't understand the link '%s'"), uris[i]);
                g_error_free(error);
            } else if (data.action == HIPPO_URI_ACTION_JOIN_CHAT) {
                error = NULL;
                if (!hippo_dbus_open_chat_blocking(data.server,
                        data.u.join_chat.kind, data.u.join_chat.chat_id,
                        &error)) {
                    error_dialog(error, _("Could not open chat - is Mugshot running?"));
                }       
            } else {
                error_dialog(NULL, _("This version of Mugshot can't open this link"));
            }
            
            hippo_uri_action_data_free_fields(&data);
        }
    }
    
    /* frees uris */
    g_option_context_free(context);

    if (loop != NULL) {
        /* we have UI to display ... when all dialogs close we'll quit the main loop */
        g_main_loop_run(loop);
        g_main_loop_unref(loop);
        loop = NULL;
    }

    return 0;
}

#if 0
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
#endif
