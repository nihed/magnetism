/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include "main.h"
#include <hippo/hippo-stack-manager.h>
#include <hippo/hippo-group.h>
#include "hippo-platform-impl.h"
#include "hippo-status-icon.h"
#include "hippo-chat-window.h"
#include "hippo-dbus-server.h"
#include "hippo-embedded-image.h"
#include "hippo-idle.h"
#include <hippo/hippo-canvas.h>
#include <gdk/gdkx.h>
#include <X11/Xatom.h>

static const char *hippo_version_file = NULL;

struct HippoApp {
    GMainLoop *loop;
    HippoPlatform *platform;
    HippoConnection *connection;
    HippoDataCache *cache;
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
    GHashTable *chat_windows;
    HippoPixbufCache *photo_cache;
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


/* This is copied from gdk_cairo_set_source_pixbuf()
 * in GDK
 */
static cairo_surface_t*
cairo_surface_from_pixbuf(GdkPixbuf *pixbuf)
{
    int width = gdk_pixbuf_get_width (pixbuf);
    int height = gdk_pixbuf_get_height (pixbuf);
    guchar *gdk_pixels = gdk_pixbuf_get_pixels (pixbuf);
    int gdk_rowstride = gdk_pixbuf_get_rowstride (pixbuf);
    int n_channels = gdk_pixbuf_get_n_channels (pixbuf);
    guchar *cairo_pixels;
    cairo_format_t format;
    cairo_surface_t *surface;
    static const cairo_user_data_key_t key;
    int j;
    
    if (n_channels == 3)
        format = CAIRO_FORMAT_RGB24;
    else
        format = CAIRO_FORMAT_ARGB32;

    cairo_pixels = g_malloc(4 * width * height);
    surface = cairo_image_surface_create_for_data((unsigned char *)cairo_pixels,
                                                  format,
                                                  width, height, 4 * width);
    cairo_surface_set_user_data(surface, &key,
                                cairo_pixels, (cairo_destroy_func_t)g_free);

    for (j = height; j; j--) {
        guchar *p = gdk_pixels;
        guchar *q = cairo_pixels;

        if (n_channels == 3) {
            guchar *end = p + 3 * width;
	  
            while (p < end) {
#if G_BYTE_ORDER == G_LITTLE_ENDIAN
                q[0] = p[2];
                q[1] = p[1];
                q[2] = p[0];
#else	  
                q[1] = p[0];
                q[2] = p[1];
                q[3] = p[2];
#endif
                p += 3;
                q += 4;
            }
        } else {
            guchar *end = p + 4 * width;
            guint t1,t2,t3;
	    
#define MULT(d,c,a,t) G_STMT_START { t = c * a + 0x7f; d = ((t >> 8) + t) >> 8; } G_STMT_END

            while (p < end) {
#if G_BYTE_ORDER == G_LITTLE_ENDIAN
                MULT(q[0], p[2], p[3], t1);
                MULT(q[1], p[1], p[3], t2);
                MULT(q[2], p[0], p[3], t3);
                q[3] = p[3];
#else	  
                q[0] = p[3];
                MULT(q[1], p[0], p[3], t1);
                MULT(q[2], p[1], p[3], t2);
                MULT(q[3], p[2], p[3], t3);
#endif
                
                p += 4;
                q += 4;
            }            
#undef MULT
        }

        gdk_pixels += gdk_rowstride;
        cairo_pixels += 4 * width;
    }
    return surface;
}

static cairo_surface_t*
canvas_load_image_hook(HippoCanvasContext *context,
                       const char         *image_name)
{
    GdkPixbuf *pixbuf;
    cairo_surface_t *surface;

    pixbuf = hippo_embedded_image_get(image_name);
    if (pixbuf == NULL) {
        return NULL;
    }

    surface = g_object_get_data(G_OBJECT(pixbuf),
                                "hippo-cairo-surface");
    if (surface == NULL) {
        surface = cairo_surface_from_pixbuf(pixbuf);
        g_object_set_data_full(G_OBJECT(pixbuf),
                               "hippo-cairo-surface",
                               surface,
                               (GDestroyNotify) cairo_surface_destroy);
    }

    cairo_surface_reference(surface);
    return surface;
}

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
        g_signal_connect(app->about_dialog, "response",
            G_CALLBACK(gtk_widget_destroy), NULL);
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
hippo_app_visit_post(HippoApp   *app,
                     HippoPost  *post)
{
    hippo_connection_visit_post(app->connection, post);
}

void
hippo_app_visit_post_id(HippoApp   *app,
                        const char *guid)
{
    hippo_connection_visit_post_id(app->connection, guid);
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
    if (!HIPPO_IS_GROUP(entity)) {
        g_warning("Can't ignore entity %s, it's not a group", guid);
        return;
    }
        
    hippo_group_set_ignored(HIPPO_GROUP(entity), TRUE);
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
    if (!HIPPO_IS_GROUP(entity)) {
        g_warning("Can't ignore entity %s's chat room, entity isn't a group", guid);
        return;
    }
    room = hippo_group_get_chat_room(HIPPO_GROUP(entity));
    hippo_chat_room_set_ignored(room, TRUE);
}

void
hippo_app_visit_entity(HippoApp    *app,
                       HippoEntity *entity)
{
    hippo_connection_visit_entity(app->connection, entity);
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

HippoWindowState
hippo_app_get_chat_state (HippoApp   *app,
                          const char *chat_id)
{
    HippoChatWindow *window = g_hash_table_lookup(app->chat_windows, chat_id);
    if (window == NULL)
        return HIPPO_WINDOW_STATE_CLOSED;

    return hippo_chat_window_get_state(window);
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
hippo_app_load_photo(HippoApp                *app,
                     HippoEntity             *entity,
                     HippoPixbufCacheLoadFunc func,
                     void                    *data)
{
    const char *url;
    
    url = hippo_entity_get_photo_url(entity);
    
    g_debug("Loading photo for entity '%s' url '%s'",
        hippo_entity_get_guid(entity),
        url ? url : "null");
    
    if (url == NULL) {
        /* not gonna succeed in loading this... */
        (* func)(NULL, data);
    } else {
        char *absolute = hippo_connection_make_absolute_url(app->connection,
                                                            url);
        hippo_pixbuf_cache_load(app->photo_cache, absolute, func, data);
        g_free(absolute);
    }
}

static void
screen_get_work_area(GdkScreen      *screen,
                     HippoRectangle *work_area)
{
    /* Making two round trips to the X server everytime the code calls get_screen_info()
     * has a certain potential for performance problems. We might want to consider
     * caching the results for a small amount of time.
     */
    GdkDisplay *display = gdk_screen_get_display(screen);
    GdkWindow *root = gdk_screen_get_root_window(screen);
    Atom current_desktop_atom = gdk_x11_get_xatom_by_name_for_display(display, "_NET_CURRENT_DESKTOP");
    Atom workarea_atom = gdk_x11_get_xatom_by_name_for_display(display, "_NET_WORKAREA");
    int format;
    Atom type;
    unsigned long n_items;
    unsigned long bytes_after;
    unsigned char *data;
    guint current_desktop;
    guint n_desktops;
    
    if (XGetWindowProperty(GDK_WINDOW_XDISPLAY(root), GDK_WINDOW_XWINDOW(root),
                           current_desktop_atom, 
                           0, G_MAXLONG, False, XA_CARDINAL,
                           &type, &format, &n_items, &bytes_after, &data) != Success) {
        g_warning("Failed to get _NET_CURRENT_DESKTOP property");
        goto fail;
    }
        
    if (format != 32 || type != XA_CARDINAL || n_items != 1) {
        g_warning("Bad _NET_CURRENT_DESKTOP property");
        XFree(data);
        goto fail;
    }

    current_desktop = ((unsigned long *)data)[0];
    XFree(data);
    

    if (XGetWindowProperty(GDK_WINDOW_XDISPLAY(root), GDK_WINDOW_XWINDOW(root),
                           workarea_atom, 
                           0, G_MAXLONG, False, XA_CARDINAL,
                           &type, &format, &n_items, &bytes_after, &data) != Success) {
        g_warning("Failed to get _NET_WORKAREA property");
        goto fail;
    }
        
    if (format != 32 ||  type != XA_CARDINAL || n_items < 4 || (n_items % 4) != 0) {
        g_warning("Bad _NET_WORKAREA property");
        XFree(data);
        goto fail;
    }

    n_desktops = n_items / 4;
    if (current_desktop > n_desktops) {
        g_warning("Current desktop out of range");
        current_desktop = 0;
    }

    work_area->x = ((unsigned long *)data)[current_desktop * 4];
    work_area->y = ((unsigned long *)data)[current_desktop * 4 + 1];
    work_area->width = ((unsigned long *)data)[current_desktop * 4 + 2];
    work_area->height = ((unsigned long *)data)[current_desktop * 4 + 3];
    
    XFree(data);
    return;

 fail:
    work_area->x = 0;
    work_area->y = 0;
    work_area->width = gdk_screen_get_width(screen);
    work_area->height = gdk_screen_get_height(screen);
}

void
hippo_app_get_screen_info(HippoApp         *app,
                          HippoRectangle   *monitor_rect_p,
                          HippoRectangle   *tray_icon_rect_p,
                          HippoOrientation *tray_icon_orientation_p)
{
    GtkOrientation orientation;
    GdkScreen *screen;
    GdkRectangle icon_rect;
    GdkRectangle monitor;
    int monitor_num;
    
    gtk_status_icon_get_geometry(GTK_STATUS_ICON(app->icon),
                                 &screen, 
                                 &icon_rect, 
                                 &orientation);

    if (monitor_rect_p) {
        HippoRectangle work_area;

        monitor_num = gdk_screen_get_monitor_at_point(screen,
                                                      icon_rect.x + icon_rect.width / 2,
                                                      icon_rect.y + icon_rect.height / 2);
        if (monitor_num < 0)
            monitor_num = 0;
        
        gdk_screen_get_monitor_geometry(screen, monitor_num, &monitor);
        
        monitor_rect_p->x = monitor.x;
        monitor_rect_p->y = monitor.y;
        monitor_rect_p->width = monitor.width;
        monitor_rect_p->height = monitor.height;
        
        screen_get_work_area(screen, &work_area);
        hippo_rectangle_intersect(monitor_rect_p, &work_area, monitor_rect_p);
    }

    if (tray_icon_rect_p) {
        tray_icon_rect_p->x = icon_rect.x;
        tray_icon_rect_p->y = icon_rect.y;
        tray_icon_rect_p->width = icon_rect.width;
        tray_icon_rect_p->height = icon_rect.height;
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

    hippo_stack_manager_set_idle(app->cache, idle);
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
    
    app->photo_cache = hippo_pixbuf_cache_new(app->platform);
    
    app->chat_windows = g_hash_table_new_full(g_str_hash, g_str_equal, g_free, NULL);

    hippo_stack_manager_manage(app->cache);
    
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

    hippo_canvas_set_load_image_hook(canvas_load_image_hook);
    
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

    g_main_loop_run(the_app->loop);

    g_debug("Main loop exited");

    g_debug("Dropping loudmouth connection");
    hippo_connection_signout(the_app->connection);

    /* FIXME: with D-Bus 0.93 / D-Bus GLib 0.93, trying to shutdown
     *  the bus warns and hangs because it's trying to shut down
     *  the shared singleton bus. So, we forget about cleanup for
     *  the moment; the only real disadvantage I know about is that
     *  it may make it harder to detect memory leaps. I'm skipping
     *  the hippo_app_free() because conceptually that's assuming
     *  that no further callbacks will come in from D-Bus, though
     *  (with the main loop not running) it doesn't really matter.
     */
#if 0
    g_debug("Releasing dbus");
    hippo_dbus_blocking_shutdown(the_app->dbus);

    hippo_app_free(the_app);
#endif

    return 0;
}
