/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include "main.h"
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"
#include "hippo-chat-window.h"
#include "hippo-bubble.h"
#include "hippo-bubble-manager.h"
#include "hippo-stack-manager.h"
#include "hippo-dbus-server.h"
#include "hippo-idle.h"
#include "hippo-canvas.h"

static const char *hippo_version_file = NULL;

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
    GHashTable *chat_windows;
    HippoImageCache *photo_cache;
    HippoDBus *dbus;
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
    HippoIdleMonitor *idle_monitor;
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
    if (app->about_dialog == NULL) {
        app->about_dialog = g_object_new(GTK_TYPE_ABOUT_DIALOG,
            "name", "Mugshot",
            "version", VERSION,
            "copyright", "Copyright 2006 Red Hat, Inc. and others",
            "website", "http://mugshot.org",
            "logo-icon-name", "mugshot",
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
    url = make_absolute_url(app, "/");
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

void
hippo_app_visit_post_id(HippoApp   *app,
                        const char *guid)
{
    HippoPost *post;
    
    post = hippo_data_cache_lookup_post(app->cache, guid);
    if (post == NULL) {
        g_warning("don't know about post '%s' can't open its page", guid);
        return;
    }
    hippo_app_visit_post(app, post);
}

void
hippo_app_ignore_post_id(HippoApp   *app,
                         const char *guid)
{
    HippoPost *post;
    
    post = hippo_data_cache_lookup_post(app->cache, guid);
    if (post == NULL) {
        g_warning("don't know about post '%s' can't ignore it", guid);
        return;
    }
    hippo_connection_set_post_ignored(app->connection, guid);
}

void
hippo_app_ignore_entity_id(HippoApp    *app,
                           const char  *guid)
{
    HippoEntity *entity;
    entity = hippo_data_cache_lookup_entity(app->cache, guid);
    if (entity == NULL) {
        g_warning("Don't know about entity '%s' can't ignore", guid);
        return;
    }
    hippo_entity_set_ignored(entity, TRUE);
}

void
hippo_app_ignore_entity_chat_id(HippoApp    *app,
                                const char  *guid)
{
    HippoEntity *entity;
    HippoChatRoom *room;
    entity = hippo_data_cache_lookup_entity(app->cache, guid);
    if (entity == NULL) {
        g_warning("Don't know about entity '%s' can't ignore chat", guid);
        return;
    }
    room = hippo_entity_get_chat_room(entity);
    hippo_chat_room_set_ignored(room, TRUE);
}


void
hippo_app_visit_entity(HippoApp    *app,
                       HippoEntity *entity)
{
    const char *home_url = hippo_entity_get_home_url(entity);
    if (home_url) {
	hippo_app_open_url(app, TRUE, home_url);
    } else {
	g_warning("Don't know how to go to the home page for entity '%s'", hippo_entity_get_guid(entity));
    }
}
                       
void
hippo_app_visit_entity_id(HippoApp    *app,
                          const char  *guid)
{
    HippoEntity *entity;
    entity = hippo_data_cache_lookup_entity(app->cache, guid);
    if (entity == NULL) {
        g_warning("Don't know about entity '%s' can't go to their page", guid);
        return;
    }
    hippo_app_visit_entity(app, entity);
}

void
hippo_app_invite_to_group(HippoApp   *app,
                          const char *group_id,
                          const char *user_id)
{
	hippo_connection_do_invite_to_group(app->connection, group_id, user_id);
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

        /* this can cause a bubble, which will open since post_is_active relies
         * on the chat window being inserted, something we're about to do... 
         * so we have a little hack here
         */
        g_assert(app->creating_chat_id == NULL); /* no recursion */
        app->creating_chat_id = chat_id;
        
        room = hippo_data_cache_ensure_chat_room(app->cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);
        window = hippo_chat_window_new(app->cache, room);
        g_hash_table_replace(app->chat_windows, g_strdup(chat_id), window);
        g_signal_connect(window, "destroy", G_CALLBACK(on_chat_window_destroy), app);
        
        app->creating_chat_id = NULL;
    }
    /* Displaying a chat window unignores the chat */
    hippo_chat_room_set_ignored(hippo_chat_window_get_room(window), FALSE);    
    gtk_window_present(GTK_WINDOW(window));   
}

gboolean
hippo_app_post_is_active(HippoApp   *app,
                         const char *post_id)
{
    /* FIXME we should also detect having a post open in a browser */
    
    if (app->creating_chat_id && strcmp(post_id, app->creating_chat_id) == 0)
        return TRUE;
        
    return hippo_app_chat_is_active(app, post_id);
}

gboolean
hippo_app_chat_is_active(HippoApp   *app,
                         const char *post_id)
{
	return g_hash_table_lookup(app->chat_windows, post_id) != NULL;    
}

void
hippo_app_load_photo(HippoApp               *app,
                     HippoEntity            *entity,
                     HippoImageCacheLoadFunc func,
                     void                   *data)
{
    const char *url;
    
    url = hippo_entity_get_small_photo_url(entity);
    
    g_debug("Loading photo for entity '%s' url '%s'",
        hippo_entity_get_guid(entity),
        url ? url : "null");
    
    if (url == NULL) {
        /* not gonna succeed in loading this... */
        (* func)(NULL, data);
    } else {
        char *absolute = make_absolute_url(app, url);
        hippo_image_cache_load(app->photo_cache, absolute, func, data);
        g_free(absolute);
    }
}

/* FIXME this function should be nuked once we drop the bubble */
void
hippo_app_put_window_by_icon(HippoApp  *app,
                             GtkWindow *window)
{
    GtkOrientation orientation;
    int x, y, width, height;
    GdkScreen *screen;
    GdkRectangle monitor;
    int monitor_num;
    int window_x, window_y;
    GtkRequisition req;
    gboolean is_west;
    gboolean is_north;

    orientation = hippo_gtk_status_icon_get_orientation(GTK_STATUS_ICON(app->icon));
    hippo_gtk_status_icon_get_screen_geometry(GTK_STATUS_ICON(app->icon), &screen,
        &x, &y, &width, &height);

    monitor_num = gdk_screen_get_monitor_at_point(screen, x, y);
    if (monitor_num < 0)
        monitor_num = 0;

    gdk_screen_get_monitor_geometry(screen, monitor_num, &monitor);

#define GAP 3

    is_west = ((x + width / 2) < (monitor.x + monitor.width / 2));
    is_north = ((y + height / 2) < (monitor.y + monitor.height / 2));

    /* Let's try pretending the status icon is always in the corner; if it isn't,
     * the bubble gets too annoying
     */
    if (is_west) {
        x = monitor.x;
    } else {
        x = monitor.x + monitor.width - width;
    }
    
    if (is_north) {
        y = monitor.y;
    } else {
        y = monitor.y + monitor.height - height;
    }

    /* this just assumes a borderless window for now, doesn't mess with gravity,
     * though it would be easily fixed if we ever cared
     */

    gtk_widget_size_request(GTK_WIDGET(window), &req);
    if (orientation == GTK_ORIENTATION_VERTICAL) {
        if (is_west) {
            window_x = x + width + GAP;
        } else {
            window_x = x - req.width - GAP;
        }
        if (is_north) {
            window_y = y;
        } else {
            window_y = y + height - req.height;
        }
    } else {
        if (is_west) {
            window_x = x;
        } else {
            window_x = x + width - req.width;
        }
        if (is_north) {
            window_y = y + height + GAP;
        } else {
            window_y = y - req.height - GAP;
        }
    }
    
    gtk_window_move(window, window_x, window_y);
}

void
hippo_app_get_screen_info(HippoApp         *app,
                          HippoRectangle   *monitor_rect_p,
                          HippoRectangle   *tray_icon_rect_p,
                          HippoOrientation *tray_icon_orientation_p)
{
    GtkOrientation orientation;
    int x, y, width, height;
    GdkScreen *screen;
    GdkRectangle monitor;
    int monitor_num;
    
    orientation = hippo_gtk_status_icon_get_orientation(GTK_STATUS_ICON(app->icon));
    hippo_gtk_status_icon_get_screen_geometry(GTK_STATUS_ICON(app->icon), &screen,
        &x, &y, &width, &height);

    monitor_num = gdk_screen_get_monitor_at_point(screen, x, y);
    if (monitor_num < 0)
        monitor_num = 0;

    gdk_screen_get_monitor_geometry(screen, monitor_num, &monitor);

    if (monitor_rect_p) {
        monitor_rect_p->x = monitor.x;
        monitor_rect_p->y = monitor.y;
        monitor_rect_p->width = monitor.width;
        monitor_rect_p->height = monitor.height;
    }

    if (tray_icon_rect_p) {
        tray_icon_rect_p->x = x;
        tray_icon_rect_p->y = y;
        tray_icon_rect_p->width = width;
        tray_icon_rect_p->height = height;
    }

    if (tray_icon_orientation_p) {
        if (orientation == GTK_ORIENTATION_VERTICAL)
            *tray_icon_orientation_p = HIPPO_ORIENTATION_VERTICAL;
        else
            *tray_icon_orientation_p = HIPPO_ORIENTATION_HORIZONTAL;
    }
}

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
    
    s = make_absolute_url(app, "/upgrade?platform=linux");
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
                    _("Your Mugshot software is too old to work with this server."));
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

    hippo_bubble_manager_set_idle(app->cache, idle);
}                

static void
on_connected_changed(HippoConnection *connection,
                     gboolean         connected,
                     void            *data)
{
    HippoApp *app = data;

    hippo_dbus_notify_xmpp_connected(app->dbus, connected);
}

/* Since we're doing this anyway, hippo_platform_get_screen_info becomes mostly
 * pointless... really should either remove screen info from HippoPlatform,
 * or put a "screen-info-changed" signal on HippoPlatform.
 * 
 * Also, this callback is kind of wrong; the icon size is not the geometry of
 * the underlying GdkWindow, it's a separate property; and we should probably
 * watch for size changes on the screen also to handle xrandr type stuff.
 */
static gboolean
on_icon_size_changed(GtkStatusIcon *tray_icon,
                     int            size,
                     void          *data)
{
    HippoApp *app = data;
    HippoRectangle monitor;
    HippoRectangle icon;
    HippoOrientation icon_orientation;

    hippo_app_get_screen_info(app, &monitor, &icon, &icon_orientation);

    hippo_stack_manager_set_screen_info(app->cache,
                                        &monitor, &icon, icon_orientation);

    /* TRUE to keep gtk from scaling our pixbuf, FALSE to do the default pixbuf
     * scaling.
     */
    return FALSE;
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
    app->icon = hippo_status_icon_new(app->cache);
    
    g_signal_connect(G_OBJECT(app->connection), "client-info-available", 
                     G_CALLBACK(on_client_info_available), app);
    g_signal_connect(G_OBJECT(app->connection), "connected-changed",
                     G_CALLBACK(on_connected_changed), app);
    
    app->photo_cache = hippo_image_cache_new();
    
    app->chat_windows = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);

    hippo_bubble_manager_manage(app->cache);
    hippo_stack_manager_manage(app->cache, app->platform);
    
    /* initially be sure we are the latest installed, though it's 
     * tough to imagine this happening outside of testing 
     * in a local source tree (which is why it's here...)
     */
    hippo_app_check_installed(app);
    /* start slow timeout to look for new installed versions */
    hippo_app_start_check_installed_timeout(app, FALSE);
 
    app->idle_monitor = hippo_idle_add(gdk_display_get_default(), on_idle_changed, app); 
    
    return app;
}

static void
hippo_app_free(HippoApp *app)
{
    hippo_idle_free(app->idle_monitor);

    if (app->check_installed_timeout != 0)
        g_source_remove(app->check_installed_timeout);

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_disconnected), app);
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->dbus),
                                         G_CALLBACK(on_dbus_song_changed), app);
    
    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_client_info_available), app);

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->connection),
                                         G_CALLBACK(on_connected_changed), app);
    
    hippo_bubble_manager_unmanage(app->cache);
    hippo_stack_manager_unmanage(app->cache);
    
    g_hash_table_destroy(app->chat_windows);
    app->chat_windows = NULL;

    if (app->about_dialog)
        gtk_object_destroy(GTK_OBJECT(app->about_dialog));
    if (app->upgrade_dialog)
        gtk_object_destroy(GTK_OBJECT(app->upgrade_dialog));
    if (app->installed_dialog)
        gtk_object_destroy(GTK_OBJECT(app->installed_dialog));

    g_signal_handlers_disconnect_by_func(G_OBJECT(app->icon),
                                         G_CALLBACK(on_icon_size_changed),
                                         app);
    
    g_object_unref(app->icon);
    g_object_unref(app->cache);
    g_object_unref(app->photo_cache);
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

    if (options.instance_type == HIPPO_INSTANCE_DEBUG) {
        gtk_icon_theme_append_search_path(gtk_icon_theme_get_default(),
                                          ABSOLUTE_TOP_SRCDIR "/icons");
    }
    
    platform = hippo_platform_impl_new(options.instance_type);
    server = hippo_platform_get_web_server(platform);

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
        g_printerr(_("Can't connect to session message bus: %s\n"), error->message);
        g_error_free(error);
        return 1;
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

    /* get rid of all this, the app has taken over */
    g_object_unref(dbus);
    dbus = NULL;
    g_object_unref(platform);
    platform = NULL;
    g_free(server);
    server = NULL;

    if (hippo_connection_signin(the_app->connection))
        g_debug("Waiting for user to sign in");
    else
        g_debug("Found login cookie");

    gtk_status_icon_set_visible(GTK_STATUS_ICON(the_app->icon), TRUE);

    g_signal_connect(G_OBJECT(the_app->icon),
                     "size-changed",
                     G_CALLBACK(on_icon_size_changed),
                     the_app);
    
    if (options.initial_debug_share) {
        /* timeout removes itself */
        g_timeout_add(1000, show_debug_share_timeout, the_app);
    }
    
    hippo_options_free_fields(&options);

#if 0
    {
        GtkWidget *window = gtk_window_new(GTK_WINDOW_TOPLEVEL);
        GtkWidget *bubble = hippo_bubble_new();
        gtk_container_set_border_width(GTK_CONTAINER(bubble), 10);
        gtk_container_add(GTK_CONTAINER(window), bubble);
        gtk_widget_show_all(window);
    }
#endif

    hippo_stack_manager_set_mode(the_app->cache, HIPPO_STACK_MODE_SINGLE_BLOCK);
    
    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    g_debug("Dropping loudmouth connection");
    hippo_connection_signout(the_app->connection);

    g_debug("Releasing dbus");
    hippo_dbus_blocking_shutdown(the_app->dbus);

    hippo_app_free(the_app);

    return 0;
}
