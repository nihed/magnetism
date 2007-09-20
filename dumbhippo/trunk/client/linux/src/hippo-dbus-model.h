/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_MODEL_H__
#define __HIPPO_DBUS_MODEL_H__

/* implement data-model-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

#define HIPPO_DBUS_MODEL_INTERFACE "org.freedesktop.od.Model"
#define HIPPO_DBUS_MODEL_PATH      "/org/freedesktop/od/data_model"

/* We use one error for all errors, and pass an error code as the second
 * argument after the string message. The error code is directly from
 * the DataModel protocol, and this avoids us having to make up new names
 * for each error code, including ones we don't know about yet.
 */
#define HIPPO_DBUS_MODEL_ERROR     "org.freedesktop.od.Model.Error"

/* This interface is used for callbacks from the server to a client
 */
#define HIPPO_DBUS_MODEL_CLIENT_INTERFACE  "org.freedesktop.od.ModelClient"

void hippo_dbus_init_model                     (DBusConnection *connection);
void hippo_dbus_model_name_gone                (const char     *name);

G_END_DECLS

#endif /* __HIPPO_DBUS_MODEL_H__ */
