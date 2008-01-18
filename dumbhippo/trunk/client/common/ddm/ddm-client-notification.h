/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __DDM_CLIENT_NOTIFICATION_H__
#define __DDM_CLIENT_NOTIFICATION_H__

#include <ddm/ddm-feed.h>

G_BEGIN_DECLS

typedef struct _DDMClientNotificationSet DDMClientNotificationSet;

DDMClientNotificationSet *ddm_client_notification_set_ref    (DDMClientNotificationSet *notification_set);
void                      ddm_client_notification_set_unref (DDMClientNotificationSet *notification_set);

gint64 ddm_client_notification_set_get_feed_timestamp (DDMClientNotificationSet *notification_set,
                                                       DDMFeed                  *feed);

G_END_DECLS

#endif /* __DDM_CLIENT_NOTIFICATION_H__ */

