/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_COOKIES_H__
#define __HIPPO_DBUS_COOKIES_H__

/* implement cookies-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

DBusMessage* hippo_dbus_handle_get_cookies_to_send(HippoDBus   *dbus,
                                                   DBusMessage *message);

G_END_DECLS

#endif /* __HIPPO_DBUS_COOKIES_H__ */
