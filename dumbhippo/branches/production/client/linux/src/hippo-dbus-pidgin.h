/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_PIDGIN_H__
#define __HIPPO_DBUS_PIDGIN_H__

/* implement pidgin (gaim) related dbus methods */

#include <glib.h>
#include <dbus/dbus.h>

G_BEGIN_DECLS

void hippo_dbus_init_pidgin(DBusConnection *connection);

void hippo_dbus_pidgin_restore_state(void);

G_END_DECLS

#endif /* __HIPPO_DBUS_PIDGIN_H__ */
