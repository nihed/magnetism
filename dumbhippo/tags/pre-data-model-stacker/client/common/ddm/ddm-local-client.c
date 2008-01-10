/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-data-resource-internal.h"
#include "ddm-local-client.h"

static void ddm_local_client_iface_init(DDMClientIface *iface);

struct _DDMLocalClient {
    GObject parent;

    DDMDataModel *model;
};

struct _DDMLocalClientClass {
    GObjectClass parent_class;
};

#define ddm_local_client_get_type _ddm_local_client_get_type

G_DEFINE_TYPE_WITH_CODE(DDMLocalClient, ddm_local_client, G_TYPE_OBJECT,
                        G_IMPLEMENT_INTERFACE(DDM_TYPE_CLIENT, ddm_local_client_iface_init);)

#undef ddm_local_client_get_type

static void
ddm_local_client_dispose(GObject *object)
{
}

static void
ddm_local_client_finalize(GObject *object)
{
}

static void
ddm_local_client_init(DDMLocalClient *local_client)
{
}

static void
ddm_local_client_class_init(DDMLocalClientClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = ddm_local_client_dispose;
    object_class->finalize = ddm_local_client_finalize;
}

static gpointer
ddm_local_client_begin_notification (DDMClient *client)
{
    return NULL;
}

static void
ddm_local_client_notify (DDMClient       *client,
                         DDMDataResource *resource,
                         GSList          *changed_properties,
                         gpointer         notification_data)
{
    _ddm_data_resource_send_local_notifications(resource, changed_properties);
}

static void
ddm_local_client_end_notification (DDMClient       *client,
                                   gpointer         notification_data)
{
}

static void
ddm_local_client_iface_init(DDMClientIface *iface)
{
    iface->begin_notification = ddm_local_client_begin_notification;
    iface->notify = ddm_local_client_notify;
    iface->end_notification = ddm_local_client_end_notification;
}

DDMClient *
_ddm_local_client_new (DDMDataModel *model)
{
    DDMLocalClient *local_client = g_object_new(DDM_TYPE_LOCAL_CLIENT, NULL);

    local_client->model = model;

    return DDM_CLIENT(local_client);
}
