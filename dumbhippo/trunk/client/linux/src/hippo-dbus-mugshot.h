/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_MUGSHOT_H__
#define __HIPPO_DBUS_MUGSHOT_H__

/* implement Mugshot-related dbus methods */








/*
 ****************
 *  This whole "Mugshot" D-Bus API is deprecated. Use the data model, which should be accessed
 *  via the org.freedesktop.od.Engine bus name, or for Stacker-specific rather than desktop
 *  generic API, use the com.dumbhippo.Client name.
 * 
 *  Eventually this codebase should be split into an "online desktop engine" (org.freedesktop.od)
 *  and the Mugshot stacker client (com.dumbhippo.)
 *
 *  "Mugshot" should not be in any namespaces or in the code at all really, other than as
 *  a configurable parameter used when the server is mugshot.org.
 ****************
 */











#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* This section is for methods related specifically to the Mugshot
 * website core, as opposed to a service that might be provided
 * by others.
 */

/* generic preferences interface */
#define HIPPO_DBUS_MUGSHOT_INTERFACE "org.mugshot.Mugshot"
/* this is a bus name owned by the current "store prefs online" program */
#define HIPPO_DBUS_MUGSHOT_BUS_NAME "org.mugshot.Mugshot"
#define HIPPO_DBUS_MUGSHOT_PATH "/org/mugshot/Mugshot"

#define HIPPO_DBUS_MUGSHOT_ENTITY_INTERFACE "org.mugshot.Mugshot.Entity"
#define HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX "/org/mugshot/Mugshot/datacache/"

DBusMessage* hippo_dbus_handle_mugshot_get_connection_status(HippoDBus   *dbus,
                                                             DBusMessage  *message);

DBusMessage* hippo_dbus_handle_mugshot_get_baseprops(HippoDBus       *dbus,
                                                     DBusMessage     *message);

DBusMessage* hippo_dbus_handle_mugshot_send_external_iq  (HippoDBus       *dbus,
                                                          DBusMessage     *message);

DBusMessage* hippo_dbus_handle_mugshot_get_self 	(HippoDBus   *dbus,
             				                         DBusMessage  *message);   
                                                     
DBusMessage* hippo_dbus_handle_mugshot_introspect   (HippoDBus       *dbus,
                                                     DBusMessage     *message);

void hippo_dbus_try_acquire_mugshot                 (DBusConnection *connection,
                                                     gboolean        replace);

DBusMessage* hippo_dbus_mugshot_signal_connection_changed       (HippoDBus            *dbus);

DBusMessage* hippo_dbus_mugshot_signal_pref_changed         (HippoDBus            *dbus,
                                                             const char           *key,
                                                             gboolean              value);
                                                             
DBusMessage* hippo_dbus_mugshot_signal_external_iq_return (HippoDBus            *dbus,
                                                           guint                 id,
                                                           const char           *content);
G_END_DECLS

#endif /* __HIPPO_DBUS_MUGSHOT_H__ */
