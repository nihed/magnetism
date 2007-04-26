/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib.h>
#include <glib/gi18n.h>
#include <string.h>
#include <stdlib.h>

static gboolean
parse_mailto(const char *mailto_url,
             char      **address_p,
             char      **subject_p,
             GError    **error)
{
    const char *address_end;
    int scheme_len = strlen("mailto:");
    
    if (!g_str_has_prefix(mailto_url, "mailto:")) {
        /* overload G_MARKUP_ERROR due to laziness */
        g_set_error(error, G_MARKUP_ERROR, G_MARKUP_ERROR_PARSE,
                    _("mailto: url does not start with 'mailto:'"));
        return FALSE;
    }

    address_end = strchr(mailto_url, '?');
    if (address_end == NULL)
        address_end = mailto_url + strlen(mailto_url);

    *address_p = g_strndup(mailto_url + scheme_len, (address_end - mailto_url) + scheme_len);

    /* FIXME parse the subject */
    
    return TRUE;
}

static gboolean
handle_gmail(const char  *mailto_url,
             GError     **error)
{
    char *address = NULL;
    char *subject = NULL;
    char *exec;
    GString *str;
    
    if (!parse_mailto(mailto_url, &address, &subject, error))
        return FALSE;

    str = g_string_new("https://mail.google.com/mail?view=cm&tf=0&to=");

    /* FIXME escape the address */
    g_string_append(str, address);

    if (subject != NULL) {
        g_string_append_printf(str, "&su=%s", subject);
    }

    /* other GMail url params are "cc" for CC and "body" for body */
    
    g_free(address);
    g_free(subject);

    exec = g_strdup_printf("gnome-open %s", str->str);

    g_string_free(str, FALSE);

    if (!g_spawn_command_line_async(exec, error)) {
        g_free(exec);
        return FALSE;
    }

    g_free(exec);
    
    return TRUE;
}

typedef gboolean (* HandlerFunc) (const char *, GError **);

static struct { const char *app; HandlerFunc func; } handlers[] = {
    { "gmail", handle_gmail },
    { NULL, NULL }
};

static GOptionEntry entries[] = {
    { NULL }
};

static void
usage(void)
{
    int i;
    
    g_printerr(_("Usage: mailto-handler [APP] [MAILTO-URL]\n"));
    g_printerr(_("Available apps: "));
    for (i = 0; handlers[i].app != NULL; ++i) {
        g_printerr("%s ", handlers[i].app);
    }
    g_printerr("\n");
    exit(1);
}

/* FIXME errors need to go in a dialog */
int
main(int argc, char **argv)
{
    GError *error = NULL;
    GOptionContext *context;
    const char *app;
    const char *mailto_url;
    int i;
    
    context = g_option_context_new(_("[APP] [MAILTO-URL] - launch apps to handle mailto: URLs"));
    g_option_context_add_main_entries(context, entries, GETTEXT_PACKAGE);
    g_option_context_parse(context, &argc, &argv, &error);
    
    if (error != NULL) {
        g_printerr("%s\n", error->message);
        g_error_free(error);
        exit(1);
    }

    g_option_context_free(context);

    if (argc != 3) {
        usage();
    }
    
    app = argv[1];
    mailto_url = argv[2];

    error = NULL;
    for (i = 0; handlers[i].app != NULL; ++i) {
        if (strcmp(handlers[i].app, app) == 0) {
            (* handlers[i].func) (mailto_url, &error);
            break;
        }
    }

    if (handlers[i].app == NULL) {
        usage();
    }

    if (error != NULL) {
        g_printerr("%s\n", error->message);
        g_error_free(error);
        exit(1);
    }
    
    return 0;
}
