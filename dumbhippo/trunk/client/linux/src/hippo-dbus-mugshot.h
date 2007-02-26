/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_MUGSHOT_H__
#define __HIPPO_DBUS_MUGSHOT_H__

/* implement Mugshot-related dbus methods */

#include "hippo-dbus-server.h"
#include "hippo/hippo-connection.h"
#include "hippo/hippo-external-account.h"

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

DBusMessage* hippo_dbus_handle_mugshot_get_whereim  (HippoDBus       *dbus,
                                                     HippoConnection *xmpp_connection,
                                                     DBusMessage     *message);

DBusMessage* hippo_dbus_handle_mugshot_get_self 	(HippoDBus   *dbus,
             				                         DBusMessage  *message);   
                                                     
DBusMessage* hippo_dbus_handle_mugshot_get_network	(HippoDBus   *dbus,
             				                         DBusMessage  *message);                                                     

DBusMessage* hippo_dbus_handle_mugshot_introspect   (HippoDBus       *dbus,
                                                     DBusMessage     *message);

void hippo_dbus_try_acquire_mugshot                 (DBusConnection *connection,
                                                     gboolean        replace);

DBusMessage* hippo_dbus_mugshot_signal_whereim_changed      (HippoDBus            *dbus,
                                                             HippoConnection      *xmpp_connection,
                                                             HippoExternalAccount *acct);
                                                             
DBusMessage* hippo_dbus_mugshot_signal_entity_changed       (HippoDBus            *dbus,
                                                             HippoEntity          *entity);

G_END_DECLS

#endif /* __HIPPO_DBUS_SETTINGS_H__ */
