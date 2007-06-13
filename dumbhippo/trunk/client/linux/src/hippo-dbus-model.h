/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_MODEL_H__
#define __HIPPO_DBUS_MODEL_H__

/* implement data-model-related dbus methods */

#include "hippo-dbus-server.h"

G_BEGIN_DECLS

#define HIPPO_DBUS_MODEL_INTERFACE "org.mugshot.dm.Model"
#define HIPPO_DBUS_MODEL_PATH      "/org/mugshot/data_model"

/* This interface is used for callbacks from the server to a client
 */
#define HIPPO_DBUS_MODEL_CLIENT_INTERFACE  "org.mugshot.dm.Client"

void hippo_dbus_init_model                     (DBusConnection *connection);
void hippo_dbus_model_name_gone                (const char     *name);
void hippo_dbus_model_notify_connected_changed (gboolean        connected);

G_END_DECLS

#endif /* __HIPPO_DBUS_MODEL_H__ */
