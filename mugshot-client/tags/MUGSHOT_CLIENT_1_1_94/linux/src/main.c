/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include "main.h"
#include <hippo/hippo-group.h>
#include "hippo-dbus-client.h"
#include "hippo-dbus-stacker.h"
#include "hippo-idle.h"
#include "hippo-ui.h"
#include <ddm/ddm-data-model-dbus.h>

static const char *hippo_version_file = NULL;

struct HippoStackerApp {
    GMainLoop *loop;
    DDMDataModel *model;
    char *stacker_server;
    HippoDBusStacker *dbus;
    HippoUI *ui;
    HippoIdleMonitor *idle_monitor;
    char **restart_argv;
    int restart_argc;
    GTime installed_version_timestamp;
    guint check_installed_timeout;
    int check_installed_fast_count;
    guint check_installed_timeout_fast : 1;
};

static void hippo_stacker_app_start_check_installed_timeout(HippoStackerApp *app,
                                                            gboolean         fast);

void
hippo_stacker_app_quit(HippoStackerApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

HippoStackerPlatform *
hippo_stacker_app_get_stacker_platform (HippoStackerApp *app)
{
    return hippo_ui_get_stacker_platform(app->ui);
}

DDMDataModel*
hippo_stacker_app_get_data_model (HippoStackerApp *app)
{
    return app->model;
}

HippoStackManager*
hippo_stacker_app_get_stack (HippoStackerApp *app)
{
    g_return_val_if_fail(app != NULL, NULL);
    g_return_val_if_fail(app->ui != NULL, NULL);

    return hippo_ui_get_stack_manager(app->ui);
}

static void
hippo_stacker_app_restart(HippoStackerApp *app)
{
    GError *error;
    
    g_debug("Restarting");
    
    /* we don't quit, the restart_argv has --replace in it so 
     * if this succeeds we should end up quitting
     */
    error = NULL;
    if (!g_spawn_async(NULL, app->restart_argv, NULL,
                       G_SPAWN_SEARCH_PATH,
                       NULL, NULL, NULL,
                       &error)) {
        GtkWidget *dialog;
        
        dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("Couldn't launch the new Mugshot!"));
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        
        gtk_widget_show(dialog);
        
        g_debug("Failed to restart: %s", error->message);
        g_error_free(error);
    }
}

static void
hippo_stacker_app_check_installed(HippoStackerApp *app)
{
    struct stat sb;
    GTime current_mtime;
    char *contents;
    gsize len;
    GError *error;
    char *version_str;

    if (stat(hippo_version_file, &sb) != 0) {
        /* don't warn here; we already warned on startup. */
        g_debug("failed to stat version file: %s", g_strerror(errno));
        return;
    }
    
    current_mtime = sb.st_mtime;
    
    /* this check is essential to keep the dialog from reopening
     * all the time if you say "don't restart"
     */
    
    if (current_mtime == app->installed_version_timestamp)
        return; /* nothing new */
    
    app->installed_version_timestamp = current_mtime;
    
    error = NULL;
    if (!g_file_get_contents(hippo_version_file, &contents, &len, &error)) {
        /* should not warn repeatedly, because of the mtime tracking above */
        g_warning("Failed to open %s: %s", hippo_version_file, error->message);
        g_error_free(error);
        return;
    }
    
    version_str = g_strdup(g_strstrip(contents));
    g_free(contents);
    
    if (hippo_compare_versions(VERSION, version_str) < 0) {
        /* Since our chat windows run in the Firefox process, we can just restart
         * and not bother the user with a prompt (as in the commented out code
         * below.)
         */
        hippo_stacker_app_restart(app);
    }
    
    g_free(version_str);
}

/* The idea of "fast" is that we think the user might be 
 * installing an upgrade, and we want to notice as soon 
 * as they finish; the idea of "slow" is that maybe 
 * an admin or yum cron job installed an update, and we 
 * want to notify any logged-in users after a while
 */
/* 5 seconds */
#define CHECK_INSTALLED_FAST_TIMEOUT (5000)
/* 1/2 hour */
#define CHECK_INSTALLED_SLOW_TIMEOUT (30 * 60 * 1000)
 
static gboolean
on_check_installed_timeout(HippoStackerApp *app)
{    
    hippo_stacker_app_check_installed(app);

    if (app->check_installed_timeout_fast) {
        app->check_installed_fast_count += 1;

        /* if we run 1 slow timeout's worth of fast timeouts, then switch
         * back to slow
         */
        if (app->check_installed_fast_count >
            (CHECK_INSTALLED_SLOW_TIMEOUT / CHECK_INSTALLED_FAST_TIMEOUT)) {
            g_debug("switching back to slow check installed timeout");
            hippo_stacker_app_start_check_installed_timeout(app, FALSE);
        }
    }
    
    return TRUE;
}

static void
hippo_stacker_app_start_check_installed_timeout(HippoStackerApp *app,
                                                gboolean         fast)
{
    fast = fast != FALSE;
    
    if (app->check_installed_timeout != 0 && 
        app->check_installed_timeout_fast == fast) {
        return;   
    }
    
    if (app->check_installed_timeout != 0) {
        g_source_remove(app->check_installed_timeout);
    } 
    
    app->check_installed_fast_count = 0;
    app->check_installed_timeout_fast = fast;
    app->check_installed_timeout =
        g_timeout_add(fast ? CHECK_INSTALLED_FAST_TIMEOUT : CHECK_INSTALLED_SLOW_TIMEOUT,
            (GSourceFunc) on_check_installed_timeout,
            app);
}

void
hippo_stacker_app_show_about(HippoStackerApp *app)
{
    hippo_ui_show_about(app->ui);
}

void
hippo_stacker_app_show_home(HippoStackerApp *app)
{
    ddm_data_model_update(app->model,
                          "online-desktop:/p/system#openUrl", NULL,
                          "url", "/",
                          NULL);
}

void
hippo_stacker_app_join_chat(HippoStackerApp *app,
                            const char      *chat_id)
{
    GError *error = NULL;

    if (!hippo_dbus_open_chat_blocking(app->stacker_server, HIPPO_CHAT_KIND_UNKNOWN, chat_id, &error)) {
        g_warning("Failed to open chat window: %s", error->message);
        g_error_free(error);
    }
}

HippoWindowState
hippo_stacker_app_get_chat_state (HippoStackerApp *app,
                                  const char      *chat_id)
{
    HippoWindowState state;
    GError *error = NULL;
     
    if (!hippo_dbus_get_chat_window_state_blocking(app->stacker_server, chat_id, &state, &error)) {
        g_warning("Failed to get chat state: %s", error->message);
        g_error_free(error);
        state = HIPPO_WINDOW_STATE_HIDDEN;
    }

    return state;
}

void
hippo_stacker_app_get_screen_info(HippoStackerApp  *app,
                                  HippoRectangle   *monitor_rect_p,
                                  HippoRectangle   *tray_icon_rect_p,
                                  HippoOrientation *tray_icon_orientation_p)
{
    hippo_ui_get_screen_info(app->ui, monitor_rect_p, tray_icon_rect_p, tray_icon_orientation_p);
}

gboolean
hippo_stacker_app_get_pointer_position (HippoStackerApp *app,
                                        int             *x_p,
                                        int             *y_p)
{
    return hippo_ui_get_pointer_position(app->ui, x_p, y_p);
}

static void
on_dbus_disconnected(HippoDBusStacker *dbus,
                     HippoStackerApp  *app)
{
    hippo_stacker_app_quit(app);
}

static void
on_idle_changed(gboolean  idle,
                void      *data)
{
    HippoStackerApp *app = data;

    hippo_ui_set_idle(app->ui, idle);
}

static HippoStackerApp*
hippo_stacker_app_new(HippoInstanceType  instance_type,
                      const char        *stacker_server,
                      HippoDBusStacker  *dbus,
                      char             **restart_argv,
                      int                restart_argc)
{
    HippoStackerApp *app = g_new0(HippoStackerApp, 1);

    app->restart_argv = g_strdupv(restart_argv);
    app->restart_argc = restart_argc;

    app->stacker_server = g_strdup(stacker_server);

    app->loop = g_main_loop_new(NULL, FALSE);
    app->dbus = dbus;
    g_object_ref(app->dbus);

    g_signal_connect(G_OBJECT(app->dbus), "disconnected",
                     G_CALLBACK(on_dbus_disconnected), app);

    app->idle_monitor = hippo_idle_add(gdk_display_get_default(), on_idle_changed, app);

    /* We use ddm_data_model_get_default() when possible because the name for that
     * has a D-BUS server file and will activate the data-model-engine, while a
     * server-specific name won't activate.
     */
    if (strcmp(stacker_server, HIPPO_DEFAULT_STACKER_WEB_SERVER) == 0) {
        app->model = ddm_data_model_get_default();
    } else {
        char *bus_name = hippo_dbus_full_bus_name(stacker_server);
        app->model = ddm_data_model_get_for_bus_name(bus_name);
        g_free(bus_name);
    }

    app->ui = hippo_ui_new(app->model);
    hippo_ui_show(app->ui);

    /* initially be sure we are the latest installed, though it's 
     * tough to imagine this happening outside of testing 
     * in a local source tree (which is why it's here...)
     */
    hippo_stacker_app_check_installed(app);
    
    /* start slow timeout to look for new installed versions */
    hippo_stacker_app_start_check_installed_timeout(app, FALSE);
 
    return app;
}

static void
hippo_stacker_app_free(HippoStackerApp *app)
{
    hippo_idle_free(app->idle_monitor);

    if (app->check_installed_timeout != 0)
        g_source_remove(app->check_installed_timeout);

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_disconnected), app);
    
    hippo_ui_free(app->ui);
    
    g_object_unref(app->dbus);
    g_main_loop_unref(app->loop);
    
    g_strfreev(app->restart_argv);

    g_free(app->stacker_server);
    
    g_free(app);
}

gboolean
hippo_stacker_app_is_uninstalled()
{
    const char *envvar = g_getenv("HIPPO_UNINSTALLED");
    if (envvar != NULL && strcmp(envvar, "") != 0)
        return TRUE;
    else
        return FALSE;
}

/* 
 * Singleton HippoStackerApp and main()
 */

static HippoStackerApp *the_app;

HippoStackerApp*
hippo_get_stacker_app(void)
{
    return the_app;
}

static gboolean print_debug_level = FALSE;

static void
log_handler(const char    *log_domain,
            GLogLevelFlags log_level,
            const char    *message,
            void          *user_data)
{
    const char *prefix;
    GString *gstr;

    if (log_level & G_LOG_FLAG_RECURSION) {
        g_printerr("Mugshot: log recursed\n");
        return;
    }

    switch (log_level & G_LOG_LEVEL_MASK) {
        case G_LOG_LEVEL_DEBUG:
            if (!print_debug_level)
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

    g_printerr("%s\n", gstr->str);
    g_string_free(gstr, TRUE);
}

int
main(int argc, char **argv)
{
    HippoOptions options;
    HippoDBusStacker *dbus;
    const char *stacker_server;
    GError *error;
     
    g_thread_init(NULL);
    
    /* not marked for translation, it's a trademark */
    g_set_application_name("Mugshot");
    
    gtk_init(&argc, &argv);
    gtk_window_set_default_icon_name("mugshot");

#if 0
    {
        hippo_canvas_open_test_window();
        gtk_main();
        return 0;
    }
#endif
    
    if (!hippo_parse_options(&argc, &argv, &options))
        return 1;

    g_log_set_default_handler(log_handler, NULL);
    print_debug_level = options.verbose;
    
    if (options.debug_updates)
        gdk_window_set_debug_updates(TRUE);
    
    if (hippo_stacker_app_is_uninstalled()) {
        gtk_icon_theme_append_search_path(gtk_icon_theme_get_default(),
                                          ABSOLUTE_TOP_SRCDIR "/icons");
    }
    
    stacker_server = hippo_get_default_server(options.instance_type,
                                              HIPPO_SERVER_STACKER,
                                              HIPPO_SERVER_PROTOCOL_WEB);

    if (g_file_test(VERSION_FILE, G_FILE_TEST_EXISTS)) {
        hippo_version_file = VERSION_FILE;
    } else if (options.instance_type == HIPPO_INSTANCE_DEBUG &&
               g_file_test("./version", G_FILE_TEST_EXISTS)) {
        hippo_version_file = "./version";
    }

    if (hippo_version_file == NULL) {
        g_warning("Version file %s is missing", VERSION_FILE);
        /* we still want to keep looking for it later, but 
         * we only want to warn one time (above)
         */
        hippo_version_file = VERSION_FILE;
    }

    error = NULL;
    dbus = hippo_dbus_stacker_try_to_acquire(stacker_server,
                                             (options.quit_existing || options.replace_existing),
                                             &error);
    if (dbus == NULL) {
        g_assert(error != NULL);

        if (g_error_matches(error, HIPPO_ERROR, HIPPO_ERROR_ALREADY_RUNNING)) {
            g_debug("Failed to get D-BUS names: %s\n", error->message);
            g_error_free(error);
            error = NULL;

            if (options.show_window) {
                if (!hippo_dbus_show_browser_blocking(stacker_server, &error)) {
                    g_printerr(_("Can't talk to existing instance: %s\n"), error->message);
                    g_error_free(error);
                    return 1;
                }
            }

            return 0;

        } else {
            g_printerr(_("Can't connect to session message bus: %s\n"), error->message);
            g_error_free(error);
            return 1;
        }
    } else {
        g_assert(error == NULL);
    }
    
    if (options.quit_existing) {
        /* we kicked off the other guy, now just exit ourselves */
        return 1;
    }

    /* Now let HippoStackerApp take over the logic, since we know we 
     * want to exist as a running app
     */
    the_app = hippo_stacker_app_new(options.instance_type, stacker_server, dbus,
                                    options.restart_argv, options.restart_argc);

    /* get rid of all this, the app has taken over */
    g_object_unref(dbus);
    dbus = NULL;
    
    hippo_options_free_fields(&options);

    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    hippo_stacker_app_free(the_app);

    return 0;
}



