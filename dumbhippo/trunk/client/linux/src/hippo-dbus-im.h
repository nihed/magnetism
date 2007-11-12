/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IM_H__
#define __HIPPO_DBUS_IM_H__

/* implement im-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* generic IM utils interface */
#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"

void hippo_dbus_init_im(void);

void hippo_dbus_im_update_buddy       (const char           *buddy_id,
                                       const char           *protocol,
                                       const char           *name,
                                       const char           *alias,
                                       gboolean              is_online,
                                       const char           *status,
                                       const char           *webdav_url);
void hippo_dbus_im_update_buddy_icon  (const char           *buddy_id,
                                       const char           *icon_hash,
                                       const char           *icon_content_type,
                                       const char           *icon_binary_data,
                                       int                   icon_data_len);
void hippo_dbus_im_remove_buddy       (const char           *buddy_id);
gboolean hippo_dbus_im_has_icon_hash  (const char           *buddy_id,
                                       const char           *icon_hash);

G_END_DECLS

#endif /* __HIPPO_DBUS_IM_H__ */
