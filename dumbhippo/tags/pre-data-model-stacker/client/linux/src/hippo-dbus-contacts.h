/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_CONTACTS_H__
#define __HIPPO_DBUS_CONTACTS_H__

/* implement contacts-related dbus methods */


/*
 ****************
 *
 *  This API is deprecated junk, use the data model. We need to clean this out.
 *
 ****************
 */



#include "hippo-dbus-server.h"

G_BEGIN_DECLS

/* generic social network interface */
#define HIPPO_DBUS_NETWORK_INTERFACE "org.freedesktop.od.Network"
/* this is a bus name owned by the current "manage my social network" program */
#define HIPPO_DBUS_NETWORK_BUS_NAME "org.freedesktop.od.FriendNetwork"
#define HIPPO_DBUS_NETWORK_PATH "/org/freedesktop/od/network"

#define HIPPO_DBUS_ENTITY_INTERFACE "org.freedesktop.od.Entity"

/* try to acquire the bus name, and register our objects */
void hippo_dbus_init_contacts(DBusConnection *connection,
                              gboolean        replace);

G_END_DECLS

#endif /* __HIPPO_DBUS_CONTACTS_H__ */
