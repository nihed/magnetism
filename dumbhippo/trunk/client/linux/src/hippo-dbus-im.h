/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IM_H__
#define __HIPPO_DBUS_IM_H__

/* implement im-related dbus methods */

#include <hippo/hippo-notification-set.h>

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* generic IM utils interface */
#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"

void hippo_dbus_init_im(DBusConnection *connection,
                        gboolean        replace);

HippoNotificationSet *hippo_dbus_im_start_notifications(void);

void hippo_dbus_im_update_buddy       (HippoNotificationSet *notifications,
                                       const char           *buddy_id,
                                       const char           *protocol,
                                       const char           *name,
                                       gboolean              is_online,
                                       const char           *status,
                                       const char           *webdav_url);
void hippo_dbus_im_remove_buddy       (HippoNotificationSet *notifications,
                                       const char           *buddy_id);

/* Differs from _hippo_notification_set_send(notifications) in that it will
 * also send out a D-BUS signal if the list of buddies changed.
 */
void hippo_dbus_im_send_notifications (HippoNotificationSet *notifications);

G_END_DECLS

#endif /* __HIPPO_DBUS_IM_H__ */
