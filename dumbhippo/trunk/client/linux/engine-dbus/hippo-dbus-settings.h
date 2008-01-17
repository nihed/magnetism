/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_SETTINGS_H__
#define __HIPPO_DBUS_SETTINGS_H__

/* implement settings-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* The dbus API is named "Preferences" which is the better name, but the
 * Mugshot name is "settings" since Mugshot already uses "prefs" for its own
 * prefs
 */

/* generic preferences interface */
#define HIPPO_DBUS_PREFS_INTERFACE "org.freedesktop.Preferences"
/* this is a bus name owned by the current "store prefs online" program */
#define HIPPO_DBUS_ONLINE_PREFS_BUS_NAME "org.freedesktop.OnlinePreferencesManager"
#define HIPPO_DBUS_ONLINE_PREFS_PATH "/org/freedesktop/online_preferences"

#define HIPPO_DBUS_PREFS_ERROR_NOT_READY "org.freedesktop.Preferences.Error.NotReady"
#define HIPPO_DBUS_PREFS_ERROR_NOT_FOUND "org.freedesktop.Preferences.Error.NotFound"
#define HIPPO_DBUS_PREFS_ERROR_WRONG_TYPE "org.freedesktop.Preferences.Error.WrongType" 

void hippo_dbus_try_acquire_online_prefs_manager(DBusConnection *connection,
                                                 gboolean        replace);

G_END_DECLS

#endif /* __HIPPO_DBUS_SETTINGS_H__ */
