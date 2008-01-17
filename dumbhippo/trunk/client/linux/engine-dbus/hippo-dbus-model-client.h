/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#ifndef __HIPPO_DBUS_MODEL_CLIENT_H__
#define __HIPPO_DBUS_MODEL_CLIENT_H__

#include <ddm/ddm.h>
#include "hippo-dbus-helper.h"

G_BEGIN_DECLS

/* Client object representing an incoming D-BUS connection to the data model
 */

typedef struct _HippoDBusModelClient      HippoDBusModelClient;
typedef struct _HippoDBusModelClientClass HippoDBusModelClientClass;
typedef struct _HippoDBusModelClientId    HippoDBusModelClientId;

#define HIPPO_TYPE_DBUS_MODEL_CLIENT              (hippo_dbus_model_client_get_type ())
#define HIPPO_DBUS_MODEL_CLIENT(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DBUS_MODEL_CLIENT, HippoDBusModelClient))
#define HIPPO_DBUS_MODEL_CLIENT_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DBUS_MODEL_CLIENT, HippoDBusModelClientClass))
#define HIPPO_IS_DBUS_MODEL_CLIENT(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DBUS_MODEL_CLIENT))
#define HIPPO_IS_DBUS_MODEL_CLIENT_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DBUS_MODEL_CLIENT))
#define HIPPO_DBUS_MODEL_CLIENT_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DBUS_MODEL_CLIENT, HippoDBusModelClientClass))

GType hippo_dbus_model_client_get_type (void) G_GNUC_CONST;

HippoDBusModelClient *hippo_dbus_model_client_new (DBusConnection *connection,
                                                   DDMDataModel   *model,
                                                   const char     *bus_name,
                                                   const char     *path);

const char *hippo_dbus_model_client_get_bus_name (HippoDBusModelClient *client);
void        hippo_dbus_model_client_disconnected (HippoDBusModelClient *client);

gboolean hippo_dbus_model_client_do_query  (HippoDBusModelClient *client,
                                            DBusMessage          *message,
                                            const char           *method_uri,
                                            DDMDataFetch         *fetch,
                                            GHashTable           *params);

/* Since the update() method doesn't take a notification path, we don't
 * know or need to know the client it corresponds to. But we put it in
 * here because of it's close connection to do_query()
 */
gboolean hippo_dbus_model_client_do_update (DDMDataModel         *model,
                                            DBusConnection       *connection,
                                            DBusMessage          *message,
                                            const char           *method_uri,
                                            GHashTable           *params);

G_END_DECLS

#endif /* __HIPPO_DBUS_MODEL_CLIENT_H__ */
