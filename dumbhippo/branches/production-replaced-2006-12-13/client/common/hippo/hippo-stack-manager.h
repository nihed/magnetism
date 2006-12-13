/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STACK_MANAGER_H__
#define __HIPPO_STACK_MANAGER_H__

/* 
 * Manage all the windows that make up the stacker UI, when to display them, etc.
 */

#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS


void hippo_stack_manager_manage   (HippoDataCache *cache);
void hippo_stack_manager_unmanage (HippoDataCache *cache);

void hippo_stack_manager_set_idle        (HippoDataCache   *cache,
                                          gboolean          idle);
void hippo_stack_manager_set_screen_info (HippoDataCache   *cache,
                                          HippoRectangle   *monitor,
                                          HippoRectangle   *icon,
                                          HippoOrientation  icon_orientation);

/* Temporary suppresses display of notification window */
void hippo_stack_manager_hush               (HippoDataCache   *cache);
/* Called on explicit close of notification window */
void hippo_stack_manager_close_notification (HippoDataCache   *cache);
/* Called on explicit close of browser window */
void hippo_stack_manager_close_browser      (HippoDataCache   *cache);
/* If the browser is closed or obscured, show it / raise it. If open
 * and not obscured, hide it */
void hippo_stack_manager_toggle_browser     (HippoDataCache   *cache);


G_END_DECLS

#endif /* __HIPPO_STACK_MANAGER_H__ */
