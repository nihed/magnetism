/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_ACTIONS_H__
#define __HIPPO_ACTIONS_H__

/* Encapsulates user interface actions. These are split out primarily to avoid passing
 * pointers to the data cache, platform, etc. throughout the UI code; we don't want
 * canvas items to be directly triggering XMPP requests and such since it just makes
 * a mess. So the widgetry goes through this interface in order to make modifications to the
 * data model or application state.
 * 
 * In general all methods on HippoActions should:
 *  - either get a UI element, like an image, or map directly to the user clicking something
 *  - contain little intelligence and just chain to a lower level
 */

#include <hippo/hippo-data-cache.h>

G_BEGIN_DECLS

typedef struct _HippoActions      HippoActions;
typedef struct _HippoActionsClass HippoActionsClass;

#define HIPPO_TYPE_ACTIONS              (hippo_actions_get_type ())
#define HIPPO_ACTIONS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_ACTIONS, HippoActions))
#define HIPPO_ACTIONS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_ACTIONS, HippoActionsClass))
#define HIPPO_IS_ACTIONS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_ACTIONS))
#define HIPPO_IS_ACTIONS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_ACTIONS))
#define HIPPO_ACTIONS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_ACTIONS, HippoActionsClass))

GType        	 hippo_actions_get_type               (void) G_GNUC_CONST;

HippoActions* hippo_actions_new                       (HippoDataCache *cache);

void          hippo_actions_visit_post                (HippoActions   *actions,
                                                       HippoPost      *post);

G_END_DECLS

#endif /* __HIPPO_ACTIONS_H__ */
