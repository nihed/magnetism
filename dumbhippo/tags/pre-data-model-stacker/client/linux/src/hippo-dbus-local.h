/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_LOCAL_H__
#define __HIPPO_DBUS_LOCAL_H__

/* implement local (gaim) related dbus methods */

#include <glib.h>
#include <dbus/dbus.h>

G_BEGIN_DECLS

void hippo_dbus_init_local(DBusConnection *connection);

G_END_DECLS

#endif /* __HIPPO_DBUS_LOCAL_H__ */
