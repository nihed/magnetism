/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-data-resource.h"
#include "ddm-notification-set.h"

typedef struct
{
    DDMDataResource *resource;
    GSList *changed_properties;
} ResourceInfo;

struct _DDMNotificationSet
{
    DDMDataModel *model;
    GHashTable   *resources;
};

static void
free_resource_info (ResourceInfo *info)
{
    g_slist_free(info->changed_properties);
    g_free(info);
}

DDMNotificationSet *
ddm_notification_set_new (DDMDataModel *model)
{
    DDMNotificationSet *notifications = g_new0(DDMNotificationSet, 1);

    notifications->model = model;
    notifications->resources = g_hash_table_new_full(g_str_hash, g_str_equal, NULL, (GDestroyNotify)free_resource_info);

    return notifications;
}

void
ddm_notification_set_add (DDMNotificationSet *notifications,
                          DDMDataResource    *resource,
                          DDMQName           *property_id)
{
    const char *resource_id = ddm_data_resource_get_resource_id(resource);

    ResourceInfo *info = g_hash_table_lookup(notifications->resources, resource_id);
    if (info == NULL) {
        info = g_new0(ResourceInfo, 1);

        info->resource = resource;
        info->changed_properties = NULL;

        g_hash_table_insert(notifications->resources, (char *)resource_id, info);
    }

    if (g_slist_find(info->changed_properties, property_id) == NULL)
        info->changed_properties = g_slist_prepend(info->changed_properties, property_id);
}

gboolean
ddm_notification_set_has_property (DDMNotificationSet *notifications,
                                   const char         *resource_id,
                                   DDMQName           *property_id)
{
    ResourceInfo *info = g_hash_table_lookup(notifications->resources, resource_id);
    if (info == NULL)
        return FALSE;

    return g_slist_find(info->changed_properties, property_id) != NULL;
}

static void
send_notification_foreach(gpointer key,
                          gpointer value,
                          gpointer data)
{
#if 0    
    /* DDMNotificationSet *notifications = data; */
    ResourceInfo *info = value;

    ddm_data_resource_on_resource_change(info->resource, info->changed_properties);
#endif
}

void
ddm_notification_set_send (DDMNotificationSet *notifications)
{
    g_hash_table_foreach(notifications->resources, send_notification_foreach, notifications);
}

typedef struct {
    DDMNotificationSet *notifications;
    DDMNotificationSetForeachFunc func;
    void *func_data;
} ForeachClosure;

static void
foreach_notification_foreach(gpointer key,
                             gpointer value,
                             gpointer data)
{
    ForeachClosure *fec = data;
    ResourceInfo *info = value;

    (* fec->func) (fec->notifications, info->resource, info->changed_properties, fec->func_data);
}

void
ddm_notification_set_foreach (DDMNotificationSet            *notifications,
                              DDMNotificationSetForeachFunc  func,
                              void                          *data)
{
    ForeachClosure fec;

    fec.notifications = notifications;
    fec.func = func;
    fec.func_data = data;

    g_hash_table_foreach(notifications->resources, foreach_notification_foreach, &fec);
}

void
ddm_notification_set_free (DDMNotificationSet *notifications)
{
    g_hash_table_destroy(notifications->resources);
    g_free(notifications);
}
