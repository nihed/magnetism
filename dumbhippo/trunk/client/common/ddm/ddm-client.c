/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-client.h"

GType
ddm_client_get_type (void)
{
    static GType type = 0;

    if (!type)
        type = g_type_register_static_simple(G_TYPE_INTERFACE, "DDMClient",
                                             sizeof (DDMClientIface),
                                             NULL, 0, NULL, 0);
    
    return type;
}

gpointer
ddm_client_begin_notification (DDMClient *client)
{
    g_return_val_if_fail(DDM_IS_CLIENT(client), NULL);

    return DDM_CLIENT_GET_IFACE(client)->begin_notification(client);
}

void
ddm_client_notify (DDMClient                *client,
                   DDMClientNotificationSet *notification_set,
		   DDMDataResource          *resource,
		   GSList                   *changed_properties,
		   gpointer                  notification_data)
{
    g_return_if_fail(DDM_IS_CLIENT(client));

    DDM_CLIENT_GET_IFACE(client)->notify(client, notification_set, resource, changed_properties, notification_data);
}

void
ddm_client_end_notification (DDMClient       *client,
			     gpointer         notification_data)
{
    g_return_if_fail(DDM_IS_CLIENT(client));

    DDM_CLIENT_GET_IFACE(client)->end_notification(client, notification_data);
}
