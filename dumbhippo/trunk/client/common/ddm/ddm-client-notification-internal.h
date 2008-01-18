/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_CLIENT_NOTIFICATION_INTERNAL_H__
#define __DDM_CLIENT_NOTIFICATION_INTERNAL_H__

#include "ddm-client-notification.h"
#include "ddm-client.h"
#include "ddm-data-model.h"
#include "ddm-data-resource.h"

G_BEGIN_DECLS

DDMClientNotificationSet *_ddm_client_notification_set_new (DDMDataModel *model);

void _ddm_client_notification_set_add (DDMClientNotificationSet *notification_set,
				       DDMDataResource          *resource,
				       DDMClient                *client,
				       DDMDataFetch             *fetch,
				       GSList                   *changed_properties);

void   _ddm_client_notification_set_add_feed_timestamp (DDMClientNotificationSet *notification_set,
                                                        DDMFeed                  *feed,
                                                        gint64                    timestamp);

 void _ddm_client_notification_set_add_work_items (DDMClientNotificationSet *notification_set);

G_END_DECLS

#endif /* __DDM_CLIENT_NOTIFICATION_INTERNAL_H__ */

