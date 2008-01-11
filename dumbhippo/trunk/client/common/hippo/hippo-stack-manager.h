/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_STACK_MANAGER_H__
#define __HIPPO_STACK_MANAGER_H__

/* 
 * Manage all the windows that make up the stacker UI, when to display them, etc.
 */

#include <ddm/ddm.h>
#include <hippo/hippo-stacker-platform.h>

G_BEGIN_DECLS

HippoStackManager* hippo_stack_manager_new (DDMDataModel         *model,
                                            HippoStackerPlatform *platform);

void               hippo_stack_manager_free  (HippoStackManager *manager);

void hippo_stack_manager_set_idle        (HippoStackManager   *manager,
                                          gboolean             idle);
void hippo_stack_manager_set_screen_info (HippoStackManager   *manager,
                                          HippoRectangle      *monitor,
                                          HippoRectangle      *icon,
                                          HippoOrientation     icon_orientation);

/* Temporary suppresses display of notification window */
void hippo_stack_manager_hush               (HippoStackManager   *manager);
/* Called on explicit close of notification window */
void hippo_stack_manager_close_notification (HippoStackManager   *manager);
/* Called on explicit close of browser window */
void hippo_stack_manager_close_browser      (HippoStackManager   *manager);
/* If the browser is closed or obscured, show it / raise it. If
 * hide_if_visible is true and it's open and not obscured, hide it */
void hippo_stack_manager_show_browser       (HippoStackManager   *manager,
                                             gboolean             hide_if_visible);
/* Show/Hide stack filter */
void hippo_stack_manager_toggle_filter      (HippoStackManager   *manager);
/* Enable/Disable feed filter */
void hippo_stack_manager_toggle_nofeed      (HippoStackManager   *manager);
/* Enable/Disable self source filter */
void hippo_stack_manager_toggle_noselfsource(HippoStackManager   *manager);

G_END_DECLS

#endif /* __HIPPO_STACK_MANAGER_H__ */
