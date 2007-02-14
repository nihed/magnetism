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
                                   HippoIdleChangedFunc func,
                                   void                *data);
void              hippo_idle_free (HippoIdleMonitor    *monitor);

/*
 * Set whether we collect statistics on the users application usage
 */
void hippo_idle_set_collect_application_usage(HippoIdleMonitor *monitor,
                                              gboolean          collect_application_usage);

/*
 * Returns a list of application names we've seen the user interacting with
 * within the last 'in_last_seconds' seconds. Free the names in the GSList
 * with g_free(), the list itself with g_slist_free().
 */
GSList *hippo_idle_get_active_applications(HippoIdleMonitor *monitor,
                                           int               in_last_seconds);

G_END_DECLS

#endif /* __HIPPO_IDLE_H__ */
