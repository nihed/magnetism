/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib.h>
#include <glib/gi18n-lib.h>
#include "hippo-ui.h"
#include <stacker/hippo-stack-manager.h>
#include "hippo-status-icon.h"
#include "hippo-embedded-image.h"
#include "hippo-stacker-platform-impl.h"
#include <string.h>
#include <hippo/hippo-canvas.h>
#include <gdk/gdkx.h>
#include <X11/Xatom.h>

struct HippoUI {
    HippoStackerPlatform *stacker_platform;
    DDMDataModel *model;

    HippoStackManager *stack;
    
    HippoStatusIcon *icon;
    GtkWidget *about_dialog;
};

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

    hippo_stack_manager_set_screen_info(ui->stack,
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
#ifndef WITH_MAEMO
    Atom current_desktop_atom = gdk_x11_get_xatom_by_name_for_display(display, "_NET_CURRENT_DESKTOP");
#endif
    Atom workarea_atom = gdk_x11_get_xatom_by_name_for_display(display, "_NET_WORKAREA");
    int format;
    Atom type;
    unsigned long n_items;
    unsigned long bytes_after;
    unsigned char *data;
    guint current_desktop = 0;
    guint n_desktops;
    
#ifndef WITH_MAEMO
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
#endif    

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
    work_area->width = ((unsigned long *)data)[current_desktop * 4 + 2];
#ifndef WITH_MAEMO
    work_area->y = ((unsigned long *)data)[current_desktop * 4 + 1];
    work_area->height = ((unsigned long *)data)[current_desktop * 4 + 3];
#else
    work_area->y = ((unsigned long *)data)[current_desktop * 4 + 1] + 50;
    work_area->height = ((unsigned long *)data)[current_desktop * 4 + 3] - 50;
#endif
    
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
hippo_ui_new(DDMDataModel   *model)
{
    HippoUI *ui;

    hippo_canvas_set_load_image_hook(canvas_load_image_hook);
    
    ui = g_new0(HippoUI, 1);

    ui->model = g_object_ref(model);

    ui->stacker_platform = hippo_stacker_platform_impl_new();

    ui->stack = hippo_stack_manager_new(model, ui->stacker_platform);
    
    ui->icon = hippo_status_icon_new(ui->model);

    g_signal_connect(G_OBJECT(ui->icon),
                     "size-changed",
                     G_CALLBACK(on_icon_size_changed),
                     ui);
    
    return ui;
}

void
hippo_ui_free(HippoUI *ui)
{
    hippo_stack_manager_free(ui->stack);
    ui->stack = NULL;
    
    if (ui->about_dialog)
        gtk_object_destroy(GTK_OBJECT(ui->about_dialog));


    g_signal_handlers_disconnect_by_func(G_OBJECT(ui->icon),
                                         G_CALLBACK(on_icon_size_changed),
                                         ui);
    
    g_object_unref(ui->icon);
    g_object_unref(ui->model);
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
    hippo_stack_manager_set_idle(ui->stack, idle);
}

HippoStackManager*
hippo_ui_get_stack_manager  (HippoUI *ui)
{
    return ui->stack;
}

HippoStackerPlatform *
hippo_ui_get_stacker_platform (HippoUI *ui)
{
    return ui->stacker_platform;
}

