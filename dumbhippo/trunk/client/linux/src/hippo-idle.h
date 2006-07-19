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

G_END_DECLS

#endif /* __HIPPO_IDLE_H__ */
