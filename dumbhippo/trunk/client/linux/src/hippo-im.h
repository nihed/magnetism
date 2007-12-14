/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_IM_H__
#define __HIPPO_IM_H__

/* implement im-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

void hippo_im_init (void);

void     hippo_im_update_buddy      (const char *buddy_id,
                                     const char *protocol,
                                     const char *name,
                                     const char *alias,
                                     gboolean    is_online,
                                     const char *status,
                                     const char *status_message,
                                     const char *webdav_url);
void     hippo_im_update_buddy_icon (const char *buddy_id,
                                     const char *icon_hash,
                                     const char *icon_content_type,
                                     const char *icon_binary_data,
                                     int         icon_data_len);
void     hippo_im_remove_buddy      (const char *buddy_id);
gboolean hippo_im_has_icon_hash     (const char *buddy_id,
                                     const char *icon_hash);

G_END_DECLS

#endif /* __HIPPO_IM_H__ */
