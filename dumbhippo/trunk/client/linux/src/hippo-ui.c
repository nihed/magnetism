/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib.h>
#include <glib/gi18n-lib.h>
#include "hippo-ui.h"
#include <hippo/hippo-stack-manager.h>
#include "hippo-status-icon.h"
#include "hippo-embedded-image.h"
#include <string.h>
#include <hippo/hippo-canvas.h>
#include <gdk/gdkx.h>
#include <X11/Xatom.h>

struct HippoUI {
    HippoPlatform *platform;
    HippoDataCache *cache;
    HippoConnection *connection;
    HippoDBus *dbus;
    
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
    HippoPixbufCache *photo_cache;
};

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
spawn_chat_window(HippoUI    *ui,
                  const char *chat_id)
{
    char *relative_url = g_strdup_printf("/chatwindow?chatId=%s", chat_id);
    char *absolute_url = hippo_connection_make_absolute_url(ui->connection, relative_url);
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
hippo_ui_join_chat(HippoUI    *ui,
                   const char *chat_id)
{
    guint64 found_window_id = 0;
    
    hippo_dbus_foreach_chat_window(ui->dbus, chat_id,
                                   join_chat_foreach, &found_window_id);

    if (found_window_id != 0)
        activate_window(GDK_DISPLAY_XDISPLAY(gdk_display_get_default()),
                        (Window)found_window_id);
    else
        spawn_chat_window(ui, chat_id);
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
hippo_ui_get_chat_state (HippoUI    *ui,
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
    
    hippo_dbus_foreach_chat_window(ui->dbus, chat_id,
                                   get_chat_state_foreach, &summary_state);

    return summary_state;
}

void
hippo_ui_load_photo(HippoUI                 *ui,
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
        char *absolute = hippo_connection_make_absolute_url(ui->connection,
                                                            url);
        hippo_pixbuf_cache_load(ui->photo_cache, absolute, func, data);
        g_free(absolute);
    }
}

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
    HippoUI *ui = data;
    HippoRectangle monitor;
    HippoRectangle icon;
    HippoOrientation icon_orientation;

    hippo_ui_get_screen_info(ui, &monitor, &icon, &icon_orientation);

    hippo_stack_manager_set_screen_info(ui->cache,
                                        &monitor, &icon, icon_orientation);

    /* TRUE to keep gtk from scaling our pixbuf, FALSE to do the default pixbuf
     * scaling.
     */
    return FALSE;
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
hippo_ui_get_screen_info(HippoUI          *ui,
                         HippoRectangle   *monitor_rect_p,
                         HippoRectangle   *tray_icon_rect_p,
                         HippoOrientation *tray_icon_orientation_p)
{
    GtkOrientation orientation;
    GdkScreen *screen;
    GdkRectangle icon_rect;
    GdkRectangle monitor;
    int monitor_num;
    
    gtk_status_icon_get_geometry(GTK_STATUS_ICON(ui->icon),
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
hippo_ui_get_pointer_position (HippoUI  *ui,
                               int      *x_p,
                               int      *y_p)
{
    GdkScreen *screen;
    GdkScreen *pointer_screen;
    int x, y;
    
    gtk_status_icon_get_geometry(GTK_STATUS_ICON(ui->icon),
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

#define STANDARD_COPYRIGHT "Copyright 2006 Red Hat, Inc. and others"

void
hippo_ui_show_about(HippoUI *ui)
{
    if (ui->about_dialog == NULL) {
        const char *copyright;
        
        if (HIPPO_ABOUT_MESSAGE[0]) {
            copyright = STANDARD_COPYRIGHT "\n\n" HIPPO_ABOUT_MESSAGE;
        } else {
            copyright = STANDARD_COPYRIGHT;
        }
        
        ui->about_dialog = g_object_new(GTK_TYPE_ABOUT_DIALOG,
            "name", "Mugshot",
            "version", VERSION,
            "copyright", copyright,
            "website", "http://mugshot.org",
            "logo-icon-name", "mugshot",
            NULL);
        g_signal_connect(ui->about_dialog, "response",
            G_CALLBACK(gtk_widget_destroy), NULL);
        g_signal_connect(ui->about_dialog, "destroy",
            G_CALLBACK(gtk_widget_destroyed), &ui->about_dialog);
    }
    
    gtk_window_present(GTK_WINDOW(ui->about_dialog));
}

HippoUI*
hippo_ui_new(HippoDataCache *cache,
             HippoDBus      *dbus)
{
    HippoUI *ui;

    hippo_canvas_set_load_image_hook(canvas_load_image_hook);
    
    ui = g_new0(HippoUI, 1);

    ui->cache = cache;
    g_object_ref(ui->cache);

    ui->connection = hippo_data_cache_get_connection(ui->cache);
    ui->platform = hippo_connection_get_platform(ui->connection);

    ui->dbus = dbus;
    g_object_ref(ui->dbus);
    
    ui->photo_cache = hippo_pixbuf_cache_new(ui->platform);
    
    hippo_stack_manager_manage(ui->cache);
    
    ui->icon = hippo_status_icon_new(ui->cache);

    g_signal_connect(G_OBJECT(ui->icon),
                     "size-changed",
                     G_CALLBACK(on_icon_size_changed),
                     ui);
    
    return ui;
}

void
hippo_ui_free(HippoUI *ui)
{
    hippo_stack_manager_unmanage(ui->cache);
    
    if (ui->about_dialog)
        gtk_object_destroy(GTK_OBJECT(ui->about_dialog));


    g_signal_handlers_disconnect_by_func(G_OBJECT(ui->icon),
                                         G_CALLBACK(on_icon_size_changed),
                                         ui);
    
    g_object_unref(ui->icon);
    g_object_unref(ui->photo_cache);
    g_object_unref(ui->dbus);
    g_object_unref(ui->cache);
    g_free(ui);
}

void
hippo_ui_show(HippoUI *ui)
{
    gtk_status_icon_set_visible(GTK_STATUS_ICON(ui->icon), TRUE);
}

void
hippo_ui_set_idle(HippoUI          *ui,
                  gboolean          idle)
{
    hippo_stack_manager_set_idle(ui->cache, idle);
}
