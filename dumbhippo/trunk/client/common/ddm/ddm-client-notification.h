/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_CLIENT_NOTIFICATION_H__
#define __DDM_CLIENT_NOTIFICATION_H__

#include "ddm-client.h"
#include "ddm-data-model.h"
#include "ddm-data-resource.h"

G_BEGIN_DECLS

/* A "client notification set" is used to track notifications to send
 * out to the different clients. At idle, we iterate over all
 * resources that have changed and add them to the client notification
 * set using _ddm_data_resource_resolve_notifications(). This creates
 * a work item for each client.
 *
 * We then add the work items to the queue, the standard process runs
 * to make sure that we have all fetches complete we need to send out
 * the notification, and when the fetches are complete, we fire the
 * notification on the DDDClient.
 */

typedef struct _DDMClientNotificationSet DDMClientNotificationSet;

DDMClientNotificationSet *_ddm_client_notification_set_new (DDMDataModel *model);
DDMClientNotificationSet *_ddm_client_notification_set_ref (DDMClientNotificationSet *notification_set);

void _ddm_client_notification_set_unref (DDMClientNotificationSet *notification_set);

void _ddm_client_notification_set_add (DDMClientNotificationSet *notification_set,
				       DDMDataResource          *resource,
				       DDMClient                *client,
				       DDMDataFetch             *fetch,
				       GSList                   *changed_properties);

void _ddm_client_notification_set_add_work_items (DDMClientNotificationSet *notification_set);

G_END_DECLS

#endif /* __DDM_CLIENT_NOTIFICATION_H__ */

