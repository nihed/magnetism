#include "main.h"
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"
#include "hippo-chat-window.h"

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
    GHashTable *chat_windows;
};

void
hippo_app_quit(HippoApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

void
hippo_app_show_about(HippoApp *app)
{
    if (app->about_dialog == NULL) {
        app->about_dialog = g_object_new(GTK_TYPE_ABOUT_DIALOG,
            "name", "Mugshot",
            "version", VERSION,
            "copyright", "Copyright 2006 Red Hat, Inc. and others",
            "website", "http://mugshot.org",
            NULL);
        g_signal_connect(app->about_dialog, "destroy",
            G_CALLBACK(gtk_widget_destroyed), &app->about_dialog);
    }
    
    gtk_window_present(GTK_WINDOW(app->about_dialog));
}

/* use_login_browser uses the browser we've logged in to 
 * the site with; if it's FALSE, we would want to use 
 * the user's default browser instead, which will almost 
 * always be the same presumably.
 * 
 * The idea is that links that go to our site need to 
 * use our login browser, other links should use the
 * user's browser.
 * 
 * Right now only use_login_browser is implemented anyhow
 * though ;-)
 */
void
hippo_app_open_url(HippoApp   *app,
                   gboolean    use_login_browser,
                   const char *url)
{
    HippoBrowserKind browser;
    char *command;
    char *quoted;
    GError *error;
    
    g_debug("Opening url '%s'", url);
    
    browser = hippo_connection_get_auth_browser(app->connection);
    
    quoted = g_shell_quote(url);
    
    switch (browser) {
    case HIPPO_BROWSER_EPIPHANY:
        command = g_strdup_printf("epiphany %s", quoted);
        break;
    case HIPPO_BROWSER_FIREFOX:
    default:
        command = g_strdup_printf("firefox %s", quoted);    
        break;
    }
  
    error = NULL;
    if (!g_spawn_command_line_async(command, &error)) {
        GtkWidget *dialog;
        
        dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("Couldn't start your web browser!"));
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        
        gtk_widget_show(dialog);
        
        g_debug("Failed to launch browser: %s", error->message);
        g_error_free(error);
    }
    
    g_free(command);
    g_free(quoted);
}

static char*
make_absolute_url(HippoApp   *app,
                  const char *relative)
{
    char *server;
    char *url;
    g_return_val_if_fail(*relative == '/', NULL);
    server = hippo_platform_get_web_server(app->platform);
    url = g_strdup_printf("http://%s%s", server, relative);
    g_free(server);
    return url;
}

void
hippo_app_show_home(HippoApp *app)
{
    char *url;
    url = make_absolute_url(app, "/home");
    hippo_app_open_url(app, TRUE, url);
    g_free(url);
}

void
hippo_app_visit_post(HippoApp   *app,
                     HippoPost  *post)
{
    char *url;
    char *relative;
    relative = g_strdup_printf("/visit?post=%s", hippo_post_get_guid(post));
    url = make_absolute_url(app, relative);
    hippo_app_open_url(app, TRUE, url);
    g_free(relative);
    g_free(url);
}

static void
on_chat_window_destroy(HippoChatWindow *window,
                       HippoApp        *app)
{
    HippoChatRoom *room;
    
    room = hippo_chat_window_get_room(window);
    g_hash_table_remove(app->chat_windows,
                        hippo_chat_room_get_id(room));
}

void
hippo_app_join_chat(HippoApp   *app,
                    const char *chat_id)
{
    HippoChatWindow *window;

    window = g_hash_table_lookup(app->chat_windows, chat_id);
    if (window == NULL) {
        HippoChatRoom *room;

        room = hippo_data_cache_ensure_chat_room(app->cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);
        window = hippo_chat_window_new(app->cache, room);
        g_hash_table_replace(app->chat_windows, g_strdup(chat_id), window);
        g_signal_connect(window, "destroy", G_CALLBACK(on_chat_window_destroy), app);
    }
    
    gtk_window_present(GTK_WINDOW(window));   
}

static HippoApp*
hippo_app_new(HippoInstanceType instance_type)
{
    HippoApp *app = g_new0(HippoApp, 1);

    app->platform = hippo_platform_impl_new(instance_type);

    app->loop = g_main_loop_new(NULL, FALSE);

    app->connection = hippo_connection_new(app->platform);
    g_object_unref(app->platform); /* let connection keep it alive */
    app->cache = hippo_data_cache_new(app->connection);
    g_object_unref(app->connection); /* let the data cache keep it alive */
    app->icon = hippo_status_icon_new(app->cache);
    
    app->chat_windows = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);
    
    return app;
}

static void
hippo_app_free(HippoApp *app)
{
    g_hash_table_destroy(app->chat_windows);
    app->chat_windows = NULL;

    if (app->about_dialog)
        gtk_object_destroy(GTK_OBJECT(app->about_dialog));
    g_object_unref(app->icon);
    g_object_unref(app->cache);
    g_main_loop_unref(app->loop);
    g_free(app);
}

/* 
 * Singleton HippoApp and main()
 */

static HippoApp *the_app;

HippoApp*
hippo_get_app(void)
{
    return the_app;
}

static void
print_debug_func(const char *message)
{
    g_printerr("%s\n", message);
}

int
main(int argc, char **argv)
{
    HippoOptions options;
     
    hippo_set_print_debug_func(print_debug_func);
     
    g_thread_init(NULL);
    gtk_init(&argc, &argv);

    if (!hippo_parse_options(&argc, &argv, &options))
        return 1;

    the_app = hippo_app_new(options.instance_type);
    
    if (hippo_connection_signin(the_app->connection))
        g_debug("Waiting for user to sign in");
    else
        g_debug("Found login cookie");
    
    gtk_status_icon_set_visible(GTK_STATUS_ICON(the_app->icon), TRUE);
    
    if (options.join_chat_id) {
        hippo_app_join_chat(the_app, options.join_chat_id);
    }
    
    hippo_options_free_fields(&options);
    
    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    hippo_app_free(the_app);

    return 0;
}
