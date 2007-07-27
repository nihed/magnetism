/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_NOTIFICATION_SET_H__
#define __HIPPO_NOTIFICATION_SET_H__

#include <hippo/hippo-data-model.h>
#include <hippo/hippo-data-resource.h>

G_BEGIN_DECLS

typedef struct _HippoNotificationSet      HippoNotificationSet;

HippoNotificationSet *_hippo_notification_set_new  (HippoDataModel       *model);
void                  _hippo_notification_set_free (HippoNotificationSet *notifications);
void                  _hippo_notification_set_add  (HippoNotificationSet *notifications,
                                                    HippoDataResource    *resource,
                                                    HippoQName           *property_id);
void                  _hippo_notification_set_send (HippoNotificationSet *notifications);
void                  _hippo_notification_set_save_to_disk (HippoNotificationSet *notifications,
                                                            gint64                timestamp);
gboolean              _hippo_notification_set_has_property (HippoNotificationSet *notifications,
                                                            const char           *resource_id,
                                                            HippoQName           *property_id);

G_END_DECLS

#endif /* __HIPPO_NOTIFICATION_SET_H__ */
