#include <config.h>
#include <hippo/hippo-basics.h>
#include <gtk/gtk.h>
#include <glib/gi18n-lib.h>
#include <stdarg.h>
#include "hippo-dbus-client.h"

static GMainLoop *loop = NULL;
static int gtk_count = 0;

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
