/* -*- mode; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <string.h>

#include <X11/Xlib.h>
#include <X11/Xatom.h>
#ifndef WITH_MAEMO
#include <X11/extensions/scrnsaver.h>
#endif
#include <gdk/gdkx.h>

#include "hippo-idle.h"

struct HippoIdleMonitor {
#ifndef WITH_MAEMO
    XScreenSaverInfo *info;
#endif
    GdkDisplay *display;
    GTime activity_time;
    unsigned long last_idle;
    guint poll_id;
    gboolean currently_idle;
    HippoIdleChangedFunc func;
    void *data;
};

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
    HippoIdleMonitor *monitor = data;
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
    
    if (was_idle != monitor->currently_idle) {
        (* monitor->func) (monitor->currently_idle, monitor->data);
    }

    return TRUE;
}

HippoIdleMonitor*
hippo_idle_add (GdkDisplay          *display,
                HippoIdleChangedFunc func,
                void                *data)
{
    int event_base, error_base;
    Display *xdisplay;
    HippoIdleMonitor *monitor;
        
    xdisplay = GDK_DISPLAY_XDISPLAY(display);
#ifndef WITH_MAEMO   
    if (!XScreenSaverQueryExtension(xdisplay, &event_base, &error_base)) {
        g_warning("Screensaver extension not found on X display, can't detect user idleness");
        return NULL;
    }
#endif
    
    monitor = g_new0(HippoIdleMonitor, 1);
    monitor->display = g_object_ref(display);
#ifndef WITH_MAEMO
    monitor->info = XScreenSaverAllocInfo();
#endif
    monitor->activity_time = get_time();
    monitor->last_idle = 0;    
    monitor->currently_idle = FALSE;
    monitor->func = func;
    monitor->data = data;
    monitor->poll_id = g_timeout_add(5000, poll_for_idleness, monitor);

    return monitor;
}

void
hippo_idle_free (HippoIdleMonitor *monitor)
{
    if (monitor == NULL)
        return; /* means no screensaver extension */
#ifndef WITH_MAEMO
    XFree(monitor->info);
#endif
    g_source_remove(monitor->poll_id);
    g_object_unref(monitor->display);
    g_free(monitor);
}

