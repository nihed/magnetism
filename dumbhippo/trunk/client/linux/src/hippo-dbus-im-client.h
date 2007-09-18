/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_IM_CLIENT_H__
#define __HIPPO_DBUS_IM_CLIENT_H__

/* implement client side of im-related dbus methods */

#include "hippo-dbus-helper.h"

G_BEGIN_DECLS

/* Use the standard IM interface on this bus name to update data model */
void hippo_dbus_im_client_add(DBusConnection *connection,
                              const char     *bus_name,
                              const char     *resource_path);

G_END_DECLS

#endif /* __HIPPO_DBUS_IM_CLIENT_H__ */
