/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IM_H__
#define __HIPPO_DBUS_IM_H__

/* implement im-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* generic IM utils interface */
#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"

void hippo_dbus_init_im(DBusConnection *connection,
                        gboolean        replace);


void hippo_dbus_im_emit_buddy_list_changed (DBusConnection         *connection);
void hippo_dbus_im_emit_buddy_changed      (DBusConnection         *connection,
                                            const HippoDBusImBuddy *buddy);
void hippo_dbus_im_append_buddy            (DBusMessageIter        *append_iter,
                                            const HippoDBusImBuddy *buddy);


G_END_DECLS

#endif /* __HIPPO_DBUS_IM_H__ */
