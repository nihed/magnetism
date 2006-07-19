/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_CLIENT_H__
#define __HIPPO_DBUS_CLIENT_H__

/* dbus client-side stuff ... shared between main mugshot client and the uri handler */

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

/* combined with host/port the client is connected to, to create full bus name */
#define HIPPO_DBUS_BASE_BUS_NAME   "com.dumbhippo.Client"
/* these are redundant with the bus name since we just have one global singleton object 
 * with one interface, at least so far
 */
#define HIPPO_DBUS_INTERFACE           "com.dumbhippo.Client"
#define HIPPO_DBUS_PATH                "/com/dumbhippo/client"

/* This interface/path is used for callbacks from the server to a client (establish
 * using the Connect() method */
#define HIPPO_DBUS_LISTENER_INTERFACE  "com.dumbhippo.Listener"
#define HIPPO_DBUS_LISTENER_PATH       "/com/dumbhippo/listener"

char*    hippo_dbus_full_bus_name     (const char   *server);

gboolean hippo_dbus_open_chat_blocking(const char   *server,
                                       HippoChatKind kind,
                                       const char   *chat_id,
                                       GError      **error);


G_END_DECLS

#endif /* __HIPPO_DBUS_CLIENT_H__ */
