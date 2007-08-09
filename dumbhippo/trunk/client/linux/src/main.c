/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include "main.h"
#include <hippo/hippo-group.h>
#include "hippo-platform-impl.h"
#include "hippo-dbus-server.h"
#include "hippo-dbus-client.h"
#include "hippo-idle.h"
#include "hippo-ui.h"

static const char *hippo_version_file = NULL;

/* How often we upload application data to the server */
#define UPLOAD_APPLICATIONS_TIMEOUT_SEC 3600 /* One hour */

/* How often we upload application data to the server in burst mode */
#define UPLOAD_APPLICATIONS_BURST_TIMEOUT_SEC 30

/* Length of the initial application burst */
#define UPLOAD_APPLICATIONS_BURST_LENGTH_SEC 3600 /* One hour */

/* The period of time our collected information about application extends over */
#define UPLOAD_APPLICATIONS_PERIOD_SEC 24 * 3600  /* One day */

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoDBus *dbus;
    HippoUI *ui;
    HippoIdleMonitor *idle_monitor;
    char **restart_argv;
    int restart_argc;
    /* see join_chat() comment */
    const char *creating_chat_id;
    /* upgrade available, go to /upgrade ? */
    GtkWidget *upgrade_dialog;
    /* new version installed, should we restart? */
    GtkWidget *installed_dialog;
    GTime installed_version_timestamp;
    guint check_installed_timeout;
    int check_installed_fast_count;
    guint check_installed_timeout_fast : 1;
    guint upload_applications_timeout;
    int upload_applications_burst_count;
};

static void hippo_app_start_check_installed_timeout(HippoApp *app,
                                                    gboolean  fast);

void
hippo_app_quit(HippoApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

HippoDataCache *
hippo_app_get_data_cache (HippoApp *app)
{
    return app->cache;
}

HippoDBus*
hippo_app_get_dbus (HippoApp *app)
{
    return app->dbus;
}

void
hippo_app_set_show_stacker (HippoApp *app,
                            gboolean  value)
{
    if (value && app->ui == NULL) {
        app->ui = hippo_ui_new(app->cache, app->dbus);
        hippo_ui_show(app->ui);
    } else if (!value && app->ui != NULL) {
        hippo_ui_free(app->ui);
        app->ui = NULL;
    }
}

HippoStackManager*
hippo_app_get_stack (HippoApp *app)
{
    g_return_val_if_fail(app != NULL, NULL);
    g_return_val_if_fail(app->ui != NULL, NULL);

    return hippo_ui_get_stack_manager(app->ui);
}

static void
hippo_app_restart(HippoApp *app)
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

void
hippo_app_show_about(HippoApp *app)
{
    hippo_ui_show_about(app->ui);
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
    hippo_platform_open_url(app->platform,
                            use_login_browser ?
                            hippo_connection_get_auth_browser(app->connection) :
                            HIPPO_BROWSER_UNKNOWN,
                            url);
}

void
hippo_app_show_home(HippoApp *app)
{
    hippo_connection_open_maybe_relative_url(app->connection, "/");
}

void
hippo_app_join_chat(HippoApp   *app,
                    const char *chat_id)
{
    hippo_ui_join_chat(app->ui, chat_id);
}

HippoWindowState
hippo_app_get_chat_state (HippoApp   *app,
                          const char *chat_id)
{
    return hippo_ui_get_chat_state(app->ui, chat_id);
}

void
hippo_app_load_photo(HippoApp                *app,
                     HippoEntity             *entity,
                     HippoPixbufCacheLoadFunc func,
                     void                    *data)
{
    hippo_ui_load_photo(app->ui, entity, func, data);
}

void
hippo_app_get_screen_info(HippoApp         *app,
                          HippoRectangle   *monitor_rect_p,
                          HippoRectangle   *tray_icon_rect_p,
                          HippoOrientation *tray_icon_orientation_p)
{
    hippo_ui_get_screen_info(app->ui, monitor_rect_p, tray_icon_rect_p, tray_icon_orientation_p);
}

gboolean
hippo_app_get_pointer_position (HippoApp *app,
                                int      *x_p,
                                int      *y_p)
{
    return hippo_ui_get_pointer_position(app->ui, x_p, y_p);
}

#if 0
static void
on_new_installed_response(GtkWidget *dialog,
                          int        response_id,
                          HippoApp  *app)
{
    gtk_object_destroy(GTK_OBJECT(dialog));
    
    if (response_id == GTK_RESPONSE_ACCEPT) {
        hippo_app_restart(app);
    } else if (response_id == GTK_RESPONSE_REJECT ||
               response_id == GTK_RESPONSE_DELETE_EVENT) {
        ; /* nothing to do */
    }
}
#endif

static void
hippo_app_check_installed(HippoApp *app)
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
        hippo_app_restart(app);
#if 0
        gboolean too_old;
        
        g_debug("Our version %s is older than installed '%s'", VERSION, version_str);
    
        too_old = hippo_connection_get_too_old(app->connection);
    
        if (app->installed_dialog == NULL) {
                app->installed_dialog = 
                    gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_INFO,
                        GTK_BUTTONS_NONE,
                        _("A newer version of Mugshot has been installed."));
                gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(app->installed_dialog),
                        _("You can restart Mugshot now to start using the new stuff."));                        
                if (too_old) {
                    /* don't give a "later" option; people can still click the close button
                     * and get in a "useless client running" state, but nobody sane will 
                     * do it and it's not catastrophic.
                     */
                    gtk_dialog_add_buttons(GTK_DIALOG(app->installed_dialog),
                            _("Restart Mugshot"), GTK_RESPONSE_ACCEPT,
                            NULL);                
                } else {
                    gtk_dialog_add_buttons(GTK_DIALOG(app->installed_dialog),
                            _("Later"), GTK_RESPONSE_REJECT,
                            _("Restart Mugshot"), GTK_RESPONSE_ACCEPT,
                            NULL);
                }
                gtk_dialog_set_default_response(GTK_DIALOG(app->installed_dialog),
                                                GTK_RESPONSE_ACCEPT);
                g_signal_connect(G_OBJECT(app->installed_dialog), "response", 
                        G_CALLBACK(on_new_installed_response), app);
            
            g_signal_connect(app->installed_dialog, "destroy",
                G_CALLBACK(gtk_widget_destroyed), &app->installed_dialog);
        }
        
        gtk_window_present(GTK_WINDOW(app->installed_dialog));
#endif        
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
on_check_installed_timeout(HippoApp *app)
{    
    hippo_app_check_installed(app);

    if (app->check_installed_timeout_fast) {
        app->check_installed_fast_count += 1;

        /* if we run 1 slow timeout's worth of fast timeouts, then switch
         * back to slow
         */
        if (app->check_installed_fast_count >
            (CHECK_INSTALLED_SLOW_TIMEOUT / CHECK_INSTALLED_FAST_TIMEOUT)) {
            g_debug("switching back to slow check installed timeout");
            hippo_app_start_check_installed_timeout(app, FALSE);
        }
    }
    
    return TRUE;
}

static void
hippo_app_start_check_installed_timeout(HippoApp *app,
                                        gboolean  fast)
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

static void
open_upgrade_page(HippoApp *app)
{
    char *s;
    
    s = hippo_connection_make_absolute_url(app->connection,
                                           "/upgrade?platform=linux");
    hippo_app_open_url(app, TRUE, s);
    g_free(s);
    
    /* switch to the fast timeout, so we see any new version 
     * that shows up.
     */
    hippo_app_start_check_installed_timeout(app, TRUE);
}

static void
on_too_old_response(GtkWidget *dialog,
                    int        response_id,
                    HippoApp  *app)
{
    gtk_object_destroy(GTK_OBJECT(dialog));
    
    if (response_id == GTK_RESPONSE_ACCEPT) {
        open_upgrade_page(app);
    } else if (response_id == GTK_RESPONSE_REJECT ||
               response_id == GTK_RESPONSE_DELETE_EVENT) {
        hippo_app_quit(app);
    }
}

static void
on_upgrade_response(GtkWidget *dialog,
                    int        response_id,
                    HippoApp  *app)
{
    gtk_object_destroy(GTK_OBJECT(dialog));
    
    if (response_id == GTK_RESPONSE_ACCEPT) {
        open_upgrade_page(app);
    }

    /* the REJECT response just means "go away until next connect" 
     * so nothing to do. Same for DELETE_EVENT
     */
}

static void
on_client_info_available(HippoConnection *connection,
                         HippoApp        *app)
{
    gboolean too_old;
    gboolean upgrade_available;
    
    too_old = hippo_connection_get_too_old(connection);
    upgrade_available = hippo_connection_get_upgrade_available(connection);

    if (!(too_old || upgrade_available))
        return; /* nothing to do */

    /* don't show upgrade stuff it we already aren't running 
     * the latest locally-installed version. Instead just 
     * offer to restart.
     */
    hippo_app_check_installed(app);
    if (app->installed_dialog)
        return;

    /* display dialog (we act like we might try to do so twice, but 
     * afaik this code runs only once right now)
     */

    if (app->upgrade_dialog == NULL) {
        if (too_old) {
            app->upgrade_dialog = 
                gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_INFO,
                    GTK_BUTTONS_NONE,
                    _("Your Mugshot software is too old to work with the web site."));
            gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(app->upgrade_dialog),
                    _("You must upgrade to continue using Mugshot."));
            gtk_dialog_add_buttons(GTK_DIALOG(app->upgrade_dialog),
                    _("Exit Mugshot"), GTK_RESPONSE_REJECT,
                    _("Open Download Page"), GTK_RESPONSE_ACCEPT,
                    NULL);
            g_signal_connect(G_OBJECT(app->upgrade_dialog), "response", 
                    G_CALLBACK(on_too_old_response), app);
        } else {
            app->upgrade_dialog = 
                gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_INFO,
                    GTK_BUTTONS_NONE,
                    _("A Mugshot upgrade is available."));
            gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(app->upgrade_dialog),
                    _("You can upgrade now or later. We'll remind you each time you connect to Mugshot."));
            gtk_dialog_add_buttons(GTK_DIALOG(app->upgrade_dialog),
                    _("Later"), GTK_RESPONSE_REJECT,
                    _("Open Download Page"), GTK_RESPONSE_ACCEPT,
                    NULL);
            g_signal_connect(G_OBJECT(app->upgrade_dialog), "response", 
                    G_CALLBACK(on_upgrade_response), app); 
        }
        
        gtk_dialog_set_default_response(GTK_DIALOG(app->upgrade_dialog),
                                GTK_RESPONSE_ACCEPT);
        
        g_signal_connect(app->upgrade_dialog, "destroy",
            G_CALLBACK(gtk_widget_destroyed), &app->upgrade_dialog);
    }
    
    gtk_window_present(GTK_WINDOW(app->upgrade_dialog));
}

static gboolean
on_upload_applications_timeout(gpointer data)
{
    HippoApp *app = (HippoApp *)data;

    if (app->upload_applications_burst_count > 0) {
        app->upload_applications_burst_count--;
        
        if (app->upload_applications_burst_count == 0) {
            g_source_remove(app->upload_applications_timeout);
            g_timeout_add(UPLOAD_APPLICATIONS_TIMEOUT_SEC * 1000,
                          on_upload_applications_timeout,
                          app);
        }
    }

    if (hippo_data_cache_get_application_usage_enabled(app->cache)) {
        GSList *app_ids;
        GSList *wm_classes;

        hippo_idle_get_active_applications(app->idle_monitor,
                                           UPLOAD_APPLICATIONS_PERIOD_SEC,
                                           &app_ids, &wm_classes);

        if (app_ids || wm_classes) {
            hippo_connection_send_active_applications(app->connection,
                                                      UPLOAD_APPLICATIONS_PERIOD_SEC,
                                                      app_ids,
                                                      wm_classes);
            
            g_slist_foreach(app_ids, (GFunc)g_free, NULL);
            g_slist_free(app_ids);
            g_slist_foreach(wm_classes, (GFunc)g_free, NULL);
            g_slist_free(wm_classes);
        }
    }

    /* There is no change notification when the set of title patterns change,
     * so we periodically re-request them to stay up-to-date (but don't do
     * this when burst uploading)
     */
    if (app->upload_applications_burst_count == 0)
        hippo_connection_request_title_patterns(app->connection);
    
    return TRUE;
}

static void
on_dbus_song_changed(HippoDBus *dbus,
                     HippoSong *song,
                     HippoApp  *app)
{
    if (!hippo_data_cache_get_music_sharing_enabled(app->cache))
        return;

    if (song)
        hippo_connection_notify_music_changed(app->connection, TRUE, song);
}

static void
on_dbus_disconnected(HippoDBus *dbus,
                     HippoApp  *app)
{
    hippo_app_quit(app);
}

static void
on_idle_changed(gboolean  idle,
                void      *data)
{
    HippoApp *app = data;

    hippo_ui_set_idle(app->ui, idle);
}

static void
on_initial_application_burst(HippoConnection *connection,
                             void            *data)
{
    HippoApp *app = data;

    app->upload_applications_burst_count = UPLOAD_APPLICATIONS_BURST_LENGTH_SEC / UPLOAD_APPLICATIONS_BURST_TIMEOUT_SEC;
        
    g_source_remove(app->upload_applications_timeout);
    g_timeout_add(UPLOAD_APPLICATIONS_BURST_TIMEOUT_SEC * 1000,
                  on_upload_applications_timeout,
                  app);
}

static void
on_connected_changed(HippoConnection *connection,
                     gboolean         connected,
                     void            *data)
{
    HippoApp *app = data;

    hippo_dbus_notify_xmpp_connected(app->dbus, connected);
}

static void
on_auth_failed(HippoConnection *connection,
               void            *data)
{
    /* Ignore this - we display as a disconnected icon */
}

static void
on_has_auth_changed(HippoConnection *connection,
                    void            *data)
{
    HippoApp *app = data;

    hippo_dbus_notify_auth_changed(app->dbus);
}

static void
on_contacts_loaded(HippoConnection *connection,
                   void            *data)
{
    HippoApp *app = data;

    hippo_dbus_notify_contacts_loaded(app->dbus);
}

static void
on_pref_changed(HippoConnection *connection,
                const char      *key,
                gboolean         value,
                void            *data)
{
    HippoApp *app = data;

    hippo_dbus_notify_pref_changed(app->dbus, key, value);
}

static void
on_whereim_changed(HippoConnection            *connection,
                   HippoExternalAccount       *acct,
                   void                       *data)
{
    HippoApp *app = data;
    hippo_dbus_notify_whereim_changed(app->dbus, app->connection, acct);
}

static void
on_external_iq_return(HippoConnection      *connection,
                      guint                 id,
                      const char           *content,
                      void                 *data)
{
    HippoApp *app = data;
    
    hippo_dbus_notify_external_iq_return(app->dbus, id, content);
}

static void
on_entity_changed(HippoEntity *entity, HippoApp *app)
{
	hippo_dbus_notify_entity_changed(app->dbus, entity);	
}

static void
on_entity_added(HippoDataCache *cache, HippoEntity *entity, HippoApp *app)
{
    g_signal_connect(G_OBJECT(entity), "changed",
                     G_CALLBACK(on_entity_changed), app);	
}

static HippoApp*
hippo_app_new(HippoInstanceType  instance_type,
              HippoPlatform     *platform,
              HippoDBus         *dbus,
              char             **restart_argv,
              int                restart_argc)
{
    HippoApp *app = g_new0(HippoApp, 1);

    app->restart_argv = g_strdupv(restart_argv);
    app->restart_argc = restart_argc;

    app->platform = platform;
    g_object_ref(app->platform);

    app->loop = g_main_loop_new(NULL, FALSE);
    app->dbus = dbus;
    g_object_ref(app->dbus);

    g_signal_connect(G_OBJECT(app->dbus), "disconnected",
                     G_CALLBACK(on_dbus_disconnected), app);
    g_signal_connect(G_OBJECT(app->dbus), "song-changed",
                     G_CALLBACK(on_dbus_song_changed), app);

    app->connection = hippo_connection_new(app->platform);
    g_object_unref(app->platform); /* let connection keep it alive */    
    app->cache = hippo_data_cache_new(app->connection);
    g_object_unref(app->connection); /* let the data cache keep it alive */

    /*** If you add handlers here, disconnect them in hipp_app_free() ***/
    
    g_signal_connect(G_OBJECT(app->cache), "entity-added",
                     G_CALLBACK(on_entity_added), app);
    
    g_signal_connect(G_OBJECT(app->connection), "client-info-available", 
                     G_CALLBACK(on_client_info_available), app);
    g_signal_connect(G_OBJECT(app->connection), "auth-failed",
                     G_CALLBACK(on_auth_failed), app);                     
    g_signal_connect(G_OBJECT(app->connection), "has-auth-changed",
                     G_CALLBACK(on_has_auth_changed), app);                     
    g_signal_connect(G_OBJECT(app->connection), "connected-changed",
                     G_CALLBACK(on_connected_changed), app);
    g_signal_connect(G_OBJECT(app->connection), "initial-application-burst",
                     G_CALLBACK(on_initial_application_burst), app);
    g_signal_connect(G_OBJECT(app->connection), "contacts-loaded", 
                     G_CALLBACK(on_contacts_loaded), app);
    g_signal_connect(G_OBJECT(app->connection), "pref-changed", 
                     G_CALLBACK(on_pref_changed), app);   
                     
    /* Hook up D-BUS reflectors */
    g_signal_connect(G_OBJECT(app->connection), "whereim-changed",
                     G_CALLBACK(on_whereim_changed), app);      
    g_signal_connect(G_OBJECT(app->connection), "external-iq-return",
                     G_CALLBACK(on_external_iq_return), app);
    
    /* initially be sure we are the latest installed, though it's 
     * tough to imagine this happening outside of testing 
     * in a local source tree (which is why it's here...)
     */
    hippo_app_check_installed(app);
    
    /* start slow timeout to look for new installed versions */
    hippo_app_start_check_installed_timeout(app, FALSE);
 
    app->idle_monitor = hippo_idle_add(gdk_display_get_default(), app->cache, on_idle_changed, app);

    app->upload_applications_timeout = g_timeout_add(UPLOAD_APPLICATIONS_TIMEOUT_SEC * 1000,
                                                     on_upload_applications_timeout,
                                                     app);
    
    return app;
}

static void
hippo_app_free(HippoApp *app)
{
    g_source_remove(app->upload_applications_timeout);
    
    hippo_idle_free(app->idle_monitor);

    if (app->check_installed_timeout != 0)
        g_source_remove(app->check_installed_timeout);

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_disconnected), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_song_changed), app);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->cache),
                                         G_CALLBACK(on_entity_added), app);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_client_info_available), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_auth_failed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_has_auth_changed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_connected_changed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_initial_application_burst), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_contacts_loaded), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_whereim_changed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_external_iq_return), app);

    if (app->upgrade_dialog)
        gtk_object_destroy(GTK_OBJECT(app->upgrade_dialog));
    if (app->installed_dialog)
        gtk_object_destroy(GTK_OBJECT(app->installed_dialog));

    hippo_ui_free(app->ui);
    
    g_object_unref(app->cache);
    g_object_unref(app->dbus);
    g_main_loop_unref(app->loop);
    
    g_strfreev(app->restart_argv);
    
    g_free(app);
}

static gboolean
show_debug_share_timeout(void *data)
{
    HippoApp *app = data;
    
    g_debug("Adding debug share data");
    
    hippo_data_cache_add_debug_data(app->cache);
    /* remove timeout */
    return FALSE;
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
    HippoDBus *dbus;
    HippoPlatform *platform;
    char *server;
    GError *error;
     
    hippo_set_print_debug_func(print_debug_func);
     
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

    if (options.debug_updates)
        gdk_window_set_debug_updates(TRUE);
    
    if (options.instance_type == HIPPO_INSTANCE_DEBUG) {
        gtk_icon_theme_append_search_path(gtk_icon_theme_get_default(),
                                          ABSOLUTE_TOP_SRCDIR "/icons");
    }
    
    platform = hippo_platform_impl_new(options.instance_type);
    server = hippo_platform_get_web_server(platform, HIPPO_SERVER_STACKER_WEB);

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
    dbus = hippo_dbus_try_to_acquire(server,
                                     (options.quit_existing || options.replace_existing),
                                     &error);
    if (dbus == NULL) {
        g_assert(error != NULL);

        if (g_error_matches(error, HIPPO_ERROR, HIPPO_ERROR_ALREADY_RUNNING)) {
            g_debug("%s\n", error->message);
            g_error_free(error);
            error = NULL;

            if (options.show_window) {
                if (!hippo_dbus_show_browser_blocking(server, &error)) {
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

    /* Now let HippoApp take over the logic, since we know we 
     * want to exist as a running app
     */
    the_app = hippo_app_new(options.instance_type, platform, dbus,
                            options.restart_argv, options.restart_argc);

    hippo_dbus_init_services(dbus);
    
    /* get rid of all this, the app has taken over */
    g_object_unref(dbus);
    dbus = NULL;
    g_object_unref(platform);
    platform = NULL;
    g_free(server);
    server = NULL;

    /* Ignore failure here */
    hippo_connection_signin(the_app->connection);

    /* enable stacker UI */
    hippo_app_set_show_stacker(the_app, TRUE);
    
    if (options.initial_debug_share) {
        /* timeout removes itself */
        g_timeout_add(1000, show_debug_share_timeout, the_app);
    }
    
    hippo_options_free_fields(&options);

    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    g_debug("Dropping loudmouth connection");
    hippo_connection_signout(the_app->connection);

    hippo_app_free(the_app);

    return 0;
}
