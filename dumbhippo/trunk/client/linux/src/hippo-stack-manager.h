/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STACK_MANAGER_H__
#define __HIPPO_STACK_MANAGER_H__

/* 
 * Manage all the windows that make up the stacker UI, when to display them, etc.
 */

#include <config.h>
#include "main.h"

G_BEGIN_DECLS


typedef enum {
    HIPPO_STACK_MODE_HIDDEN,
    HIPPO_STACK_MODE_SINGLE_BLOCK,
    HIPPO_STACK_MODE_STACK
} HippoStackMode;

void hippo_stack_manager_manage           (HippoDataCache  *cache);
void hippo_stack_manager_unmanage         (HippoDataCache  *cache);

void hippo_stack_manager_set_idle         (HippoDataCache  *cache,
                                           gboolean         idle);
void hippo_stack_manager_set_mode         (HippoDataCache  *cache,
                                           HippoStackMode   mode);
void hippo_stack_manager_set_screen_info  (HippoDataCache  *cache,
                                           HippoRectangle  *monitor,
                                           HippoRectangle  *icon,
                                           HippoOrientation icon_orientation);

G_END_DECLS

#endif /* __HIPPO_STACK_MANAGER_H__ */
