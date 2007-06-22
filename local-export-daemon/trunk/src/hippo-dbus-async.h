/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_ASYNC_H__
#define __HIPPO_DBUS_ASYNC_H__

/* D-Bus utils that need threads */

#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus.h>
#include <glib-object.h>

G_BEGIN_DECLS

typedef void (* HippoDBusConnectionOpenedHandler) (DBusConnection  *connection_or_null,
                                                   const DBusError *error_if_null,
                                                   void            *data);

void hippo_dbus_connection_open_private_async (const char                      *address,
                                               HippoDBusConnectionOpenedHandler handler,
                                               void                            *data);

G_END_DECLS

#endif /* __HIPPO_DBUS_ASYNC_H__ */
