/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_HTTP_H__
#define __HIPPO_DBUS_HTTP_H__

/* implement http-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

#define HIPPO_DBUS_HTTP_BUS_NAME  "org.freedesktop.od.Http"
#define HIPPO_DBUS_HTTP_INTERFACE "org.freedesktop.od.Http"
#define HIPPO_DBUS_HTTP_PATH      "/org/freedesktop/od/http"

/* This interface is used for callbacks from the server to a client
 */
#define HIPPO_DBUS_HTTP_DATA_SINK_INTERFACE  "org.freedesktop.od.HttpDataSink"

void hippo_dbus_init_http                     (DBusConnection *connection);

G_END_DECLS

#endif /* __HIPPO_DBUS_HTTP_H__ */
