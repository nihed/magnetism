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
                               G_MAXLONG, 0, False, AnyPropertyType,
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
spawn_chat_window(HippoApp   *app,
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
hippo_app_join_chat(HippoApp   *app,
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
hippo_app_get_chat_state (HippoApp   *app,
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

gboolean
hippo_app_get_pointer_position (HippoApp *app,
                                int      *x_p,
                                int      *y_p)
{
    GdkScreen *screen;
    GdkScreen *pointer_screen;
    int x, y;
    
    gtk_status_icon_get_geometry(GTK_STATUS_ICON(app->icon),
                                 &screen, NULL, NULL);

    gdk_display_get_pointer(gdk_screen_get_display(screen),
                            &pointer_screen, &x, &y, NULL);
    
    if (pointer_screen != screen) {
        x = 0;
        y = 0;
    }

    if (x_p)
        *x_p = x;
    if (y_p)
        *y_p = y;

    return pointer_screen == screen;
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

    hippo_app_free(the_app);

    return 0;
}
