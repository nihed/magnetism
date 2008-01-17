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
#include "hippo-im.h"
#include "hippo-application-monitor.h"

#include <gdk/gdkx.h>

static const char *hippo_version_file = NULL;

/* How often we upload application data to the server */
#define UPLOAD_APPLICATIONS_TIMEOUT_SEC 3600 /* One hour */

/* How often we upload application data to the server in burst mode */
#define UPLOAD_APPLICATIONS_BURST_TIMEOUT_SEC 30

/* Length of the initial application burst */
#define UPLOAD_APPLICATIONS_BURST_LENGTH_SEC 3600 /* One hour */

/* The period of time our collected information about application extends over */
#define UPLOAD_APPLICATIONS_PERIOD_SEC 24 * 3600  /* One day */

struct HippoEngineApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoDBus *dbus;
    HippoApplicationMonitor *application_monitor;
    char **restart_argv;
    int restart_argc;
    /* see join_chat() comment */
    const char *creating_chat_id;
    GTime installed_version_timestamp;
    guint check_installed_timeout;
    int check_installed_fast_count;
    guint check_installed_timeout_fast : 1;
    guint upload_applications_timeout;
    int upload_applications_burst_count;
};

static void hippo_engine_app_start_check_installed_timeout(HippoEngineApp *app,
                                                           gboolean  fast);

void
hippo_engine_app_quit(HippoEngineApp *app)
{
    g_debug("Quitting main loop");
    g_main_loop_quit(app->loop);
}

HippoDataCache *
hippo_engine_app_get_data_cache (HippoEngineApp *app)
{
    return app->cache;
}

HippoDBus*
hippo_engine_app_get_dbus (HippoEngineApp *app)
{
    return app->dbus;
}

DDMDataModel*
hippo_engine_app_get_data_model (HippoEngineApp *app)
{
    return hippo_data_cache_get_model(app->cache);
}

static void
hippo_engine_app_restart(HippoEngineApp *app)
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
        /* Just silently ignore the issue, we aren't supposed to have a user
         * interface so we shouldn't show a dialog. Hopefully the upgrade
         * wasn't really necessary.
         */
        g_debug("Failed to restart: %s", error->message);
        g_error_free(error);
    }
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
hippo_engine_app_open_url(HippoEngineApp   *app,
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
hippo_engine_app_show_home(HippoEngineApp *app)
{
    hippo_connection_open_maybe_relative_url(app->connection, "/");
}

static void
activate_window(Display *display, Window window)
{
    Window toplevel = window;
    Atom window_state_atom = gdk_x11_get_xatom_by_name("WM_STATE");
    Atom active_window_atom = gdk_x11_get_xatom_by_name("_NET_ACTIVE_WINDOW");
    Window root;
    XEvent xev;
    
    /* The window_id we have is the window ID of a child window. So, we first
     * need to walk up the window hierarachy until we find the WM_STATE window,
     * then activate that window. Lots of X roundtrips here, but we only do
     * this on a user click as an alternative to launching a new firefox 
     * process, so it doesn't really matter.
     */
    gdk_error_trap_push();

    while (TRUE) {
        Window parent;
        Window *children;
        guint n_children;
        
        Atom type;
        int format;
        gulong n_items;
        gulong bytes_after;
        guchar *data;

        if (!XQueryTree(display, toplevel, &root, &parent, &children, &n_children)) {
            g_debug("XQueryTree failed\n");
            goto out;
        }

        XFree(children);

        if (root == parent) /* No window manager or non-reparenting window manager */
            break;
        
        if (XGetWindowProperty(display, toplevel, window_state_atom,
                               0, G_MAXLONG, False, AnyPropertyType,
                               &type, &format, &n_items, &bytes_after, &data) != Success) {
            g_debug("XGetWindowProperty failed\n");
            goto out;
        }
        
        if (type != None) { /* Found the real client toplevel */
            XFree(data);
            break;
        }

        toplevel = parent;
    }

    xev.xclient.type = ClientMessage;
    xev.xclient.window = toplevel;
    xev.xclient.message_type = active_window_atom;
    xev.xclient.format = 32;
    xev.xclient.data.l[0] = 2; /* We're sort of like a pager ... we're activating a window
                                * from a different app as a response to direct user action
                                */
    xev.xclient.data.l[1] = gtk_get_current_event_time();
    xev.xclient.data.l[2] = None; /* We don't really have an active toplevel */
    xev.xclient.data.l[3] = 0;
    xev.xclient.data.l[4] = 0;

    XSendEvent(display, root, False, SubstructureNotifyMask | SubstructureRedirectMask, &xev);

 out:
    gdk_error_trap_pop();
}

static void
spawn_chat_window(HippoEngineApp   *app,
                  const char *chat_id)
{
    char *relative_url = g_strdup_printf("/chatwindow?chatId=%s", chat_id);
    char *absolute_url = hippo_connection_make_absolute_url(app->connection, relative_url);
    char *command = g_strdup_printf("firefox -chrome chrome://mugshot/content/chatWindow.xul?src=%s", absolute_url);
    GError *error = NULL;

    if (!g_spawn_command_line_async(command, &error)) {
        GtkWidget *dialog;
        
        dialog = gtk_message_dialog_new(NULL, 0, GTK_MESSAGE_ERROR,
                                        GTK_BUTTONS_CLOSE,
                                        _("Couldn't start Firefox to show quips and comments"));
        gtk_message_dialog_format_secondary_text(GTK_MESSAGE_DIALOG(dialog), "%s", error->message);
        g_signal_connect(dialog, "response", G_CALLBACK(gtk_widget_destroy), NULL);
        
        gtk_widget_show(dialog);
        
        g_debug("Failed to start Firefox to show quips and comments: %s\n", error->message);
        g_error_free(error);
    }

    g_free(relative_url);
    g_free(absolute_url);
    g_free(command);
}

static void 
join_chat_foreach(guint64 window_id, HippoChatState state, void *data)
{
    guint64 *found_id = data;

    if (state == HIPPO_CHAT_STATE_PARTICIPANT)
        *found_id = window_id;
}

void
hippo_engine_app_join_chat(HippoEngineApp   *app,
                           const char *chat_id)
{
    guint64 found_window_id = 0;
    
    hippo_dbus_foreach_chat_window(app->dbus, chat_id,
                                   join_chat_foreach, &found_window_id);

    if (found_window_id != 0)
        activate_window(GDK_DISPLAY_XDISPLAY(gdk_display_get_default()),
                        (Window)found_window_id);
    else
        spawn_chat_window(app, chat_id);
}

/* Doesn't handle HIPPO_WINDOW_STATE_ACTIVE - see comment below */
static HippoWindowState
get_window_state(Display *display, Window window)
{
    HippoWindowState result =  HIPPO_WINDOW_STATE_HIDDEN;
    XWindowAttributes window_attributes;
    GdkRectangle rect;
    GdkRegion *visible_region = NULL;
    Window child = None;
    
    Window root;
    Window parent;
    Window *children = NULL;
    guint n_children;

    gdk_error_trap_push();
    
    /* First check if the window and all ancestors are mapped
     */

    if (!XGetWindowAttributes(display, window, &window_attributes)) {
        g_debug("XGetWindowAttributes failed\n");
        goto out;
    }

    if (window_attributes.map_state != IsViewable)
        goto out;

    /* Get the area of the window in parent coordinates
     */
    rect.x = window_attributes.x;
    rect.y = window_attributes.y;
    rect.width = window_attributes.width;
    rect.height = window_attributes.height;

    visible_region = gdk_region_rectangle(&rect);

    if (!XQueryTree(display, window, &root, &parent, &children, &n_children)) {
        g_debug("XQueryTree failed\n");
        goto out;
    }

    XFree(children);
    children = NULL;

    child = window;
    window = parent;

    /* Walk up the hierarchy, clipping to parents, and subtracting
     * overlayed siblings (yuck!)
     */
    while (TRUE) {
        GdkRegion *parent_region;
        gboolean seen_child = FALSE;
        int x, y;
        unsigned int width, height, border, depth;
        unsigned int i;

        gdk_region_get_clipbox(visible_region, &rect);
        
        /* Clip to parent */
        if (!XGetGeometry(display, window, &root, &x, &y, &width, &height, &border, &depth)) {
            g_debug("XGetGeometry failed\n");
            goto out;
        }

        rect.x = 0;
        rect.y = 0;
        rect.width = width;
        rect.height= height;

        parent_region = gdk_region_rectangle(&rect);
        gdk_region_intersect(visible_region, parent_region);
        gdk_region_destroy(parent_region);

        if (gdk_region_empty(visible_region))
            goto out;
                
        if (!XQueryTree(display, window, &root, &parent, &children, &n_children)) {
            g_debug("XQueryTree failed\n");
            goto out;
        }

        for (i = 0; i < n_children; i++) {
            if (seen_child) {
                /* A sibling above */
                GdkRegion *child_region;
                XWindowAttributes child_attributes;
                
                if (!XGetWindowAttributes(display, children[i], &child_attributes)) {
                    g_debug("XGetWindowAttributes failed for child\n");
                    goto out;
                }

                if (child_attributes.map_state == IsViewable) {
                    rect.x = child_attributes.x - child_attributes.border_width;
                    rect.y = child_attributes.y - child_attributes.border_width;
                    rect.width = child_attributes.width + 2 * child_attributes.border_width;
                    rect.height = child_attributes.height + 2 * child_attributes.border_width;

                    child_region = gdk_region_rectangle(&rect);
                    gdk_region_subtract(visible_region, child_region);
                    gdk_region_destroy(child_region);
                    
                    if (gdk_region_empty(visible_region))
                        goto out;
                }
                
            } else if (children[i] == child) {
                seen_child = TRUE;
            }
        }
    
        XFree(children);
        children = NULL;

        if (window == root)
            break;
        
        child = window;
        window = parent;

        /* Translate to parent coordinates */
        gdk_region_offset(visible_region, x, y);
    }

    if (!gdk_region_empty(visible_region))
        result = HIPPO_WINDOW_STATE_ONSCREEN;

 out:
    gdk_error_trap_pop();

    if (children)
        XFree(children);

    if (visible_region)
        gdk_region_destroy(visible_region);

    return result;
}

static void 
get_chat_state_foreach(guint64 window_id, HippoChatState state, void *data)
{
    HippoWindowState *summary_state = data;

    HippoWindowState this_window_state = get_window_state(GDK_DISPLAY_XDISPLAY(gdk_display_get_default()),
                                                          (Window)window_id);
    
    if (this_window_state > *summary_state)
        *summary_state = this_window_state;
}

HippoWindowState
hippo_engine_app_get_chat_state (HippoEngineApp   *app,
                                 const char *chat_id)
{
    /* The client only uses hippo_platform_get_chat_window_state() to determine
     * one thing ... should it notify the user with a block when a new chat message
     * comes in. What we compute here is tuned to that - we don't try to compute
     * HIPPO_WINDOW_STATE_ACTIVE, but just return HIPPO_WINDOW_STATE_ONSCREEN
     * if some portion of a window displaying either a visitor or participant
     * chat is visible to the user.
     */
    
    HippoWindowState summary_state = HIPPO_WINDOW_STATE_CLOSED;
    
    hippo_dbus_foreach_chat_window(app->dbus, chat_id,
                                   get_chat_state_foreach, &summary_state);

    return summary_state;
}

static void
hippo_engine_app_check_installed(HippoEngineApp *app)
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
        hippo_engine_app_restart(app);
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
on_check_installed_timeout(HippoEngineApp *app)
{    
    hippo_engine_app_check_installed(app);

    if (app->check_installed_timeout_fast) {
        app->check_installed_fast_count += 1;

        /* if we run 1 slow timeout's worth of fast timeouts, then switch
         * back to slow
         */
        if (app->check_installed_fast_count >
            (CHECK_INSTALLED_SLOW_TIMEOUT / CHECK_INSTALLED_FAST_TIMEOUT)) {
            g_debug("switching back to slow check installed timeout");
            hippo_engine_app_start_check_installed_timeout(app, FALSE);
        }
    }
    
    return TRUE;
}

static void
hippo_engine_app_start_check_installed_timeout(HippoEngineApp *app,
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

static gboolean
on_upload_applications_timeout(gpointer data)
{
    HippoEngineApp *app = (HippoEngineApp *)data;

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

        hippo_application_monitor_get_active_applications(app->application_monitor,
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
                     HippoEngineApp  *app)
{
    if (!hippo_data_cache_get_music_sharing_enabled(app->cache))
        return;

    if (song)
        hippo_connection_notify_music_changed(app->connection, TRUE, song);
}

static void
on_dbus_disconnected(HippoDBus *dbus,
                     HippoEngineApp  *app)
{
    hippo_engine_app_quit(app);
}

static void
on_initial_application_burst(HippoConnection *connection,
                             void            *data)
{
    HippoEngineApp *app = data;

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
    HippoEngineApp *app = data;

    hippo_dbus_notify_xmpp_connected(app->dbus, connected);
}

static void
on_auth_failed(HippoConnection *connection,
               void            *data)
{
    /* Ignore this - we display as a disconnected icon */
}

static HippoEngineApp*
hippo_engine_app_new(HippoInstanceType  instance_type,
                     HippoPlatform     *platform,
                     HippoDBus         *dbus,
                     char             **restart_argv,
                     int                restart_argc)
{
    HippoEngineApp *app = g_new0(HippoEngineApp, 1);

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
    
    g_signal_connect(G_OBJECT(app->connection), "auth-failed",
                     G_CALLBACK(on_auth_failed), app);                     
    g_signal_connect(G_OBJECT(app->connection), "connected-changed",
                     G_CALLBACK(on_connected_changed), app);
    g_signal_connect(G_OBJECT(app->connection), "initial-application-burst",
                     G_CALLBACK(on_initial_application_burst), app);
                     
    /* initially be sure we are the latest installed, though it's 
     * tough to imagine this happening outside of testing 
     * in a local source tree (which is why it's here...)
     */
    hippo_engine_app_check_installed(app);
    
    /* start slow timeout to look for new installed versions */
    hippo_engine_app_start_check_installed_timeout(app, FALSE);
 
    app->application_monitor = hippo_application_monitor_add(gdk_display_get_default(), app->cache);

    app->upload_applications_timeout = g_timeout_add(UPLOAD_APPLICATIONS_TIMEOUT_SEC * 1000,
                                                     on_upload_applications_timeout,
                                                     app);
    
    return app;
}

static void
hippo_engine_app_free(HippoEngineApp *app)
{
    g_source_remove(app->upload_applications_timeout);
    
    hippo_application_monitor_free(app->application_monitor);

    if (app->check_installed_timeout != 0)
        g_source_remove(app->check_installed_timeout);

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_disconnected), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_song_changed), app);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_auth_failed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_connected_changed), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_initial_application_burst), app);

    g_object_unref(app->cache);
    g_object_unref(app->dbus);
    g_main_loop_unref(app->loop);
    
    g_strfreev(app->restart_argv);
    
    g_free(app);
}

/* 
 * Singleton HippoEngineApp and main()
 */

static HippoEngineApp *the_app;

HippoEngineApp*
hippo_get_engine_app(void)
{
    return the_app;
}

int
main(int argc, char **argv)
{
    HippoOptions options;
    HippoDBus *dbus;
    HippoPlatform *platform;
    char *stacker_server;
    char *desktop_server;
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

    hippo_initialize_logging(&options);

    if (options.debug_updates)
        gdk_window_set_debug_updates(TRUE);
    
    if (options.instance_type == HIPPO_INSTANCE_DEBUG) {
        gtk_icon_theme_append_search_path(gtk_icon_theme_get_default(),
                                          ABSOLUTE_TOP_SRCDIR "/icons");
    }
    
    platform = hippo_platform_impl_new(options.instance_type);
    stacker_server = hippo_platform_get_web_server(platform, HIPPO_SERVER_STACKER);
    desktop_server = hippo_platform_get_web_server(platform, HIPPO_SERVER_DESKTOP);

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
    dbus = hippo_dbus_try_to_acquire(desktop_server, stacker_server,
                                     (options.quit_existing || options.replace_existing),
                                     &error);
    if (dbus == NULL) {
        g_assert(error != NULL);

        if (g_error_matches(error, HIPPO_ERROR, HIPPO_ERROR_ALREADY_RUNNING)) {
            g_debug("Failed to get D-BUS names: %s\n", error->message);
            g_error_free(error);
            error = NULL;

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

    /* Now let HippoEngineApp take over the logic, since we know we 
     * want to exist as a running app
     */
    the_app = hippo_engine_app_new(options.instance_type, platform, dbus,
                            options.restart_argv, options.restart_argc);

    hippo_dbus_init_services(dbus);
    hippo_im_init();
    
    /* get rid of all this, the app has taken over */
    g_object_unref(dbus);
    dbus = NULL;
    g_object_unref(platform);
    platform = NULL;
    g_free(stacker_server);
    stacker_server = NULL;
    g_free(desktop_server);
    desktop_server = NULL;
    
    /* Ignore failure here */
    hippo_connection_signin(the_app->connection);

    hippo_options_free_fields(&options);

    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    g_debug("Dropping loudmouth connection");
    hippo_connection_signout(the_app->connection);

    hippo_engine_app_free(the_app);

    return 0;
}
