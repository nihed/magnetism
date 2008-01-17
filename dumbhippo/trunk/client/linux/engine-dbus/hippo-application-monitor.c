/* -*- mode; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>

#include <X11/Xlib.h>
#include <X11/Xatom.h>
#ifndef WITH_MAEMO
#include <X11/extensions/scrnsaver.h>
#endif
#include <gdk/gdkx.h>

#include "hippo-application-monitor.h"

typedef struct HippoApplicationInfo HippoApplicationInfo;

struct HippoApplicationInfo {
    GTime active_time;
};

struct HippoApplicationMonitor {
#ifndef WITH_MAEMO
    XScreenSaverInfo *info;
#endif
    GdkDisplay *display;
    HippoDataCache *cache;
    GTime activity_time;
    unsigned long last_idle;
    guint poll_id;
    gboolean currently_idle;
    void *data;
    GHashTable *applications_by_app_id;
    GHashTable *applications_by_wm_class;
};

/* Code to find the active window in order to collect stats for social
 * application browsing.
 */
static void
get_active_application_properties(HippoApplicationMonitor *monitor,
                                  char            **wm_class,
                                  char            **title)
{
    Display *xdisplay = GDK_DISPLAY_XDISPLAY(monitor->display);
    int n_screens = gdk_display_get_n_screens(monitor->display);
    Atom net_active_window_x = gdk_x11_get_xatom_by_name_for_display(monitor->display,
                                                                     "_NET_ACTIVE_WINDOW");
    GdkAtom net_active_window_gdk = gdk_atom_intern("_NET_ACTIVE_WINDOW", FALSE);
    Window active_window = None;
    int i;

    Atom type;
    int format;
    unsigned long n_items;
    unsigned long bytes_after;
    guchar *data;
    gboolean is_desktop = FALSE;
        
    if (wm_class)
        *wm_class = NULL;
    if (title)
        *title = NULL;

    /* Find the currently focused window by looking at the _NET_ACTIVE_WINDOW property
     * on all the screens of the display.
     */
    for (i = 0; i < n_screens; i++) {
        GdkScreen *screen = gdk_display_get_screen(monitor->display, i);
        GdkWindow *root = gdk_screen_get_root_window(screen);

        if (!gdk_x11_screen_supports_net_wm_hint (screen, net_active_window_gdk))
            continue;

        XGetWindowProperty (xdisplay, GDK_DRAWABLE_XID(root),
                            net_active_window_x,
                            0, 1, False, XA_WINDOW,
                            &type, &format, &n_items, &bytes_after, &data);
        if (type == XA_WINDOW) {
            active_window = *(Window *)data;
            XFree(data);
            break;
        }
    }

    /* Now that we have the active window, figure out the application name and WM class
     */
    gdk_error_trap_push();
        
    if (active_window && wm_class) {
        if (XGetWindowProperty (xdisplay, active_window,
                                XA_WM_CLASS,
                                0, G_MAXLONG, False, XA_STRING,
                                &type, &format, &n_items, &bytes_after, &data) == Success &&
            type == XA_STRING)
        {
            if (format == 8) {
                char **list;
                int count;
                
                count = gdk_text_property_to_utf8_list_for_display(monitor->display, GDK_TARGET_STRING,
                                                                   8, data, n_items, &list);

                if (count > 1) {
		    /* This is a check for Nautilus, which sets the instance to this
		     * value for the desktop window; we do this rather than check for
		     * the more general _NET_WM_WINDOW_TYPE_DESKTOP to avoid having
		     * to do another XGetProperty on every iteration. We generally
		     * don't want to count the desktop being focused as application
		     * usage because it frequently can be a false-positive of an
		     * empty workspace.
		     */
		    if (strcmp(list[0], "desktop_window") == 0)
			is_desktop = TRUE;
		    else
			*wm_class = g_strdup(list[1]);
		}

                if (list)
                    g_strfreev(list);
            }
            
            XFree(data);
        }
    }

    if (is_desktop)
	active_window = None;

    if (active_window && title) {
        Atom utf8_string = gdk_x11_get_xatom_by_name_for_display(monitor->display, "UTF8_STRING");
        
        if (XGetWindowProperty (xdisplay, active_window,
                                gdk_x11_get_xatom_by_name_for_display(monitor->display, "_NET_WM_NAME"),
                                0, G_MAXLONG, False, utf8_string,
                                &type, &format, &n_items, &bytes_after, &data) == Success &&
            type == utf8_string)
        {
            if (format == 8 && g_utf8_validate((char *)data, -1, NULL)) {
                *title = g_strdup((char *)data);
            }
            
            XFree(data);
        }
    }

    if (active_window && title && *title == NULL) {
        if (XGetWindowProperty (xdisplay, active_window,
                                XA_WM_NAME,
                                0, G_MAXLONG, False, AnyPropertyType,
                                &type, &format, &n_items, &bytes_after, &data) == Success &&
            type != None)
        {
            if (format == 8) {
                char **list;
                int count;
                
                count = gdk_text_property_to_utf8_list_for_display(monitor->display,
                                                                   gdk_x11_xatom_to_atom_for_display(monitor->display, type),
                                                                   8, data, n_items, &list);

                if (count > 0)
                    *title = g_strdup(list[0]);
                
                if (list)
                    g_strfreev(list);
            }
            
            XFree(data);
        }
    }
        
    gdk_error_trap_pop();
}

static void
update_application_info(HippoApplicationMonitor *monitor)
{
    char *wm_class;
    char *title;
    HippoApplicationInfo *info = NULL;
    GTimeVal now;

    get_active_application_properties(monitor, &wm_class, &title);

    if (title) {
        const char *app_id = hippo_data_cache_match_application_title(monitor->cache, title);
        if (app_id) {
            info = g_hash_table_lookup(monitor->applications_by_app_id, app_id);
            if (!info) {
                info = g_new(HippoApplicationInfo, 1);
                g_hash_table_insert(monitor->applications_by_app_id, g_strdup(app_id), info);
            }
        }
    }

    if (!info && wm_class) {
        info = g_hash_table_lookup(monitor->applications_by_wm_class, wm_class);
        if (!info) {
            info = g_new(HippoApplicationInfo, 1);
            g_hash_table_insert(monitor->applications_by_wm_class, g_strdup(wm_class), info);
        }
    }

    if (info) {
        g_get_current_time(&now);
        info->active_time = now.tv_sec;
    }
        
    g_free(wm_class);
    g_free(title);
}
    
static GTime
get_time (void)
{
    GTimeVal tv;
    g_get_current_time(&tv);
    return tv.tv_sec;
}

static gboolean
poll_for_idleness (void *data)
{
    HippoApplicationMonitor *monitor = data;
    int i;
    int n_screens;
    unsigned long idle_time;
    gboolean was_idle;
    
    idle_time = G_MAXINT;
    n_screens = gdk_display_get_n_screens(monitor->display);
    for (i = 0; i < n_screens; ++i) {
        int result = 0;
        GdkScreen *screen;

        screen = gdk_display_get_screen(monitor->display, i);       
#ifndef WITH_MAEMO
        result = XScreenSaverQueryInfo(GDK_DISPLAY_XDISPLAY(monitor->display),
                                GDK_SCREEN_XSCREEN(screen)->root,
                                monitor->info);
#endif
        if (result == 0) {
            g_warning("Failed to get idle time from screensaver extension");
            break;
        }
        
        /* monitor->info->idle is time in milliseconds since last user interaction event */
#ifndef WITH_MAEMO
        idle_time = MIN(monitor->info->idle, idle_time);
#endif
    }
    
    was_idle = monitor->currently_idle;
    
    /* If the idle time has gone down, there must have been activity since we last checked */
    if (idle_time < monitor->last_idle) {
        monitor->activity_time = get_time();
        monitor->currently_idle = FALSE;
    } else {
        /* If no activity, see how long ago it was and count ourselves idle 
         * if it's been a short while. We keep this idle really short, since it
         * simply results in keeping bubbles up; we want to do this if people 
         * just look away for a minute, really. It can be "more aggressive" 
         * about idle detection than a screensaver would be.
         */
        GTime now = get_time();
        if (now < monitor->activity_time) {
            /* clock went backward... just "catch up" 
             * then wait until the idle timeout expires again
             */
            monitor->activity_time = now;
        } else if ((now - monitor->activity_time) > 120) { /* 120 = 2 minutes */
            monitor->currently_idle = TRUE;
        }
    }
 
    monitor->last_idle = idle_time;
    
    if (hippo_data_cache_get_application_usage_enabled(monitor->cache) && !monitor->currently_idle) {
        update_application_info(monitor);
    }
    
    return TRUE;
}

HippoApplicationMonitor*
hippo_application_monitor_add (GdkDisplay          *display,
			       HippoDataCache      *cache)
{
    int event_base, error_base;
    Display *xdisplay;
    HippoApplicationMonitor *monitor;
        
    xdisplay = GDK_DISPLAY_XDISPLAY(display);
#ifndef WITH_MAEMO   
    if (!XScreenSaverQueryExtension(xdisplay, &event_base, &error_base)) {
        g_warning("Screensaver extension not found on X display, can't detect user idleness");
        return NULL;
    }
#endif
    
    monitor = g_new0(HippoApplicationMonitor, 1);
    monitor->display = g_object_ref(display);
    monitor->cache = g_object_ref(cache);
#ifndef WITH_MAEMO
    monitor->info = XScreenSaverAllocInfo();
#endif
    monitor->activity_time = get_time();
    monitor->last_idle = 0;    
    monitor->currently_idle = FALSE;
    monitor->poll_id = g_timeout_add(5000, poll_for_idleness, monitor);
    monitor->applications_by_app_id = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                            (GDestroyNotify)g_free, (GDestroyNotify)g_free);
    monitor->applications_by_wm_class = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                              (GDestroyNotify)g_free, (GDestroyNotify)g_free);

    return monitor;
}

void
hippo_application_monitor_free (HippoApplicationMonitor *monitor)
{
    if (monitor == NULL)
        return; /* means no screensaver extension */
#ifndef WITH_MAEMO
    XFree(monitor->info);
#endif
    g_source_remove(monitor->poll_id);
    g_object_unref(monitor->display);
    g_object_unref(monitor->cache);
    g_hash_table_destroy(monitor->applications_by_app_id);
    g_hash_table_destroy(monitor->applications_by_wm_class);
    g_free(monitor);
}

typedef struct {
    GSList *result;
    GTime start_time;
} ActiveApplicationsData;

static void
active_applications_foreach(gpointer key,
                            gpointer value,
                            gpointer data)
{
    char *name = key;
    HippoApplicationInfo *info = value;
    ActiveApplicationsData *app_data = data;

    if (info->active_time > app_data->start_time)
        app_data->result = g_slist_prepend(app_data->result, g_strdup(name));
}

void
hippo_application_monitor_get_active_applications(HippoApplicationMonitor *monitor,
						  int                      in_last_seconds,
						  GSList                 **app_ids,
						  GSList                 **wm_classes)
{
    GTimeVal now;
    ActiveApplicationsData app_data;

    g_get_current_time(&now);

    app_data.start_time = now.tv_sec - in_last_seconds;

    if (app_ids) {
        app_data.result = NULL;
        g_hash_table_foreach(monitor->applications_by_app_id, active_applications_foreach, &app_data);
        *app_ids = app_data.result;
    }

    if (wm_classes) {
        app_data.result = NULL;
        g_hash_table_foreach(monitor->applications_by_wm_class, active_applications_foreach, &app_data);
        *wm_classes = app_data.result;
    }
}
