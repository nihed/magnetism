#ifndef __HIPPO_BUBBLE_MANAGER_H__
#define __HIPPO_BUBBLE_MANAGER_H__

/* 
 * Manage multiple bubbles, when we bubble, paging through bubbles, etc.
 */

#include <config.h>
#include "hippo-bubble-util.h"
#include "hippo-status-icon.h"
#include "main.h"

G_BEGIN_DECLS

void hippo_bubble_manager_manage  (HippoDataCache  *cache);

void hippo_bubble_manager_unmanage(HippoDataCache  *cache);

G_END_DECLS

#endif /* __HIPPO_BUBBLE_MANAGER_H__ */
