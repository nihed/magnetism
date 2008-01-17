/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_APPLICATION_MONITOR_H__
#define __HIPPO_APPLICATION_MONITOR_H__

/* 
 * Monitor active applications
 */

#include <config.h>
#include "main.h"

G_BEGIN_DECLS

typedef struct HippoApplicationMonitor HippoApplicationMonitor;

typedef void (* HippoApplicationMonitorChangedFunc) (gboolean idle, void *data);

HippoApplicationMonitor* hippo_application_monitor_add  (GdkDisplay              *display,
                                                         HippoDataCache          *cache);
void                     hippo_application_monitor_free (HippoApplicationMonitor *monitor);

/*
 * Returns lists of application names we've seen the user interacting with
 * within the last 'in_last_seconds' seconds. Free the names in the GSLists
 * with g_free(), the lists themselves with g_slist_free().
 */
void hippo_application_monitor_get_active_applications(HippoApplicationMonitor *monitor,
                                                       int                      in_last_seconds,
                                                       GSList                 **app_ids,
                                                       GSList                 **wm_classes);
    
G_END_DECLS

#endif /* __HIPPO_APPLICATION_MONITOR_H__ */
