/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_PIDGIN_H__
#define __HIPPO_DBUS_PIDGIN_H__

/* implement pidgin (gaim) related dbus methods */

#include <glib.h>
#include <dbus/dbus.h>

G_BEGIN_DECLS

typedef struct {
    const char *protocol;
    const char *name;
    gboolean is_online;
    const char *status;
} HippoDBusImBuddy;

void hippo_dbus_init_pidgin(DBusConnection *connection);

void hippo_pidgin_append_buddies(DBusMessageIter *append_iter);

G_END_DECLS

#endif /* __HIPPO_DBUS_PIDGIN_H__ */
