/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_IDLE_H__
#define __HIPPO_IDLE_H__

/* 
 * Monitor idleness
 */

#include <config.h>
#include "main.h"

G_BEGIN_DECLS

typedef struct HippoIdleMonitor HippoIdleMonitor;

typedef void (* HippoIdleChangedFunc) (gboolean idle, void *data);

HippoIdleMonitor* hippo_idle_add  (GdkDisplay          *display,
                                   HippoDataCache      *cache,
                                   HippoIdleChangedFunc func,
                                   void                *data);
void              hippo_idle_free (HippoIdleMonitor    *monitor);

/*
 * Returns lists of application names we've seen the user interacting with
 * within the last 'in_last_seconds' seconds. Free the names in the GSLists
 * with g_free(), the lists themselves with g_slist_free().
 */
void hippo_idle_get_active_applications(HippoIdleMonitor *monitor,
                                        int               in_last_seconds,
                                        GSList          **app_ids,
                                        GSList          **wm_classes);
    
G_END_DECLS

#endif /* __HIPPO_IDLE_H__ */
