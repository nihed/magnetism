#ifndef __HIPPO_BUBBLE_UTIL_H__
#define __HIPPO_BUBBLE_UTIL_H__

/* 
 * This file has the glue between the bubble and our data cache
 * types and stuff like that; hippo-bubble.[hc] is purely the 
 * rendering-related bits.
 */

#include <config.h>
#include "hippo-bubble.h"
#include "main.h"

G_BEGIN_DECLS

void             hippo_bubble_set_post               (HippoBubble    *bubble,
                                                      HippoPost      *post,
                                                      HippoDataCache *cache);
HippoPost*       hippo_bubble_get_post               (HippoBubble    *bubble);

void             hippo_bubble_set_group              (HippoBubble    *bubble,
                                                      HippoEntity    *group,
                                                      HippoDataCache *cache);
HippoEntity *    hippo_bubble_get_group              (HippoBubble *bubble);

void             hippo_bubble_set_group_membership_change (HippoBubble    *bubble,
                                                           HippoEntity    *group,
                                                           HippoEntity    *user,
                                                           const char     *status,                                         
                                                           HippoDataCache *cache);
                                                           
void             hippo_bubble_get_group_membership_change (HippoBubble    *bubble,
                                                           HippoEntity    **group,
                                                           HippoEntity    **user);


G_END_DECLS

#endif /* __HIPPO_BUBBLE_UTIL_H__ */
