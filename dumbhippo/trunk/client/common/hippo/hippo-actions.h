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
 *
 * If this starts replicating most of DataCache we should probably pack it in and
 * just pass around the data cache.
 * 
 */

#include <hippo/hippo-data-cache.h>
#include <hippo/hippo-group.h>
#include <hippo/hippo-canvas-item.h>

G_BEGIN_DECLS

typedef struct _HippoActions      HippoActions;
typedef struct _HippoActionsClass HippoActionsClass;

#define HIPPO_TYPE_ACTIONS              (hippo_actions_get_type ())
#define HIPPO_ACTIONS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_ACTIONS, HippoActions))
#define HIPPO_ACTIONS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_ACTIONS, HippoActionsClass))
#define HIPPO_IS_ACTIONS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_ACTIONS))
#define HIPPO_IS_ACTIONS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_ACTIONS))
#define HIPPO_ACTIONS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_ACTIONS, HippoActionsClass))

GType            hippo_actions_get_type               (void) G_GNUC_CONST;

HippoActions* hippo_actions_new                     (HippoDataCache  *cache);
void          hippo_actions_visit_post              (HippoActions    *actions,
                                                     HippoPost       *post);
void          hippo_actions_visit_entity            (HippoActions    *actions,
                                                     HippoEntity     *entity);
void          hippo_actions_load_entity_photo_async (HippoActions    *actions,
                                                     HippoEntity     *entity,
                                                     int              size,
                                                     HippoCanvasItem *image_item);
HippoEntity*  hippo_actions_lookup_entity           (HippoActions    *actions,
                                                     const char      *entity_guid);
gint64        hippo_actions_get_server_time_offset  (HippoActions    *actions);
void          hippo_actions_close_browser           (HippoActions    *actions);
void          hippo_actions_close_notification      (HippoActions    *actions);
void          hippo_actions_hush_notification       (HippoActions    *actions);
void          hippo_actions_expand_notification     (HippoActions    *actions);
void          hippo_actions_open_home_page          (HippoActions    *actions);
void          hippo_actions_open_absolute_url       (HippoActions    *actions,
                                                     const char      *url);
void          hippo_actions_hush_block              (HippoActions    *actions,
                                                     HippoBlock      *block);
void          hippo_actions_unhush_block            (HippoActions    *actions,
                                                     HippoBlock      *block);
void          hippo_actions_add_to_faves            (HippoActions    *actions,
                                                     HippoBlock      *block);
void          hippo_actions_join_chat_id            (HippoActions    *actions,
                                                     const char      *chat_id);
void          hippo_actions_invite_to_group         (HippoActions    *actions,
                                                     HippoGroup      *group,
                                                     HippoPerson     *person);
                                                     

G_END_DECLS

#endif /* __HIPPO_ACTIONS_H__ */
