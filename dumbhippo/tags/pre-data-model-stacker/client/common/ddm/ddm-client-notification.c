/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-client-notification.h"
#include "ddm-data-model-internal.h"
#include "ddm-work-item.h"

struct _DDMClientNotificationSet {
    guint refcount;

    DDMDataModel *model;
    GHashTable *work_items;
};

DDMClientNotificationSet *
_ddm_client_notification_set_new (DDMDataModel *model)
{
    DDMClientNotificationSet *notification_set;

    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), NULL);

    notification_set = g_new0(DDMClientNotificationSet, 1);

    notification_set->refcount = 1;
    notification_set->model = model;
    notification_set->work_items = g_hash_table_new_full(g_direct_hash, NULL,
                                                         NULL,
                                                         (GDestroyNotify)_ddm_work_item_unref);

    return notification_set;
}

DDMClientNotificationSet *
_ddm_client_notification_set_ref (DDMClientNotificationSet *notification_set)
{
    g_return_val_if_fail (notification_set != NULL, NULL);
    g_return_val_if_fail (notification_set->refcount > 0, NULL);

    notification_set->refcount++;
  
    return notification_set;
}

void
_ddm_client_notification_set_unref (DDMClientNotificationSet *notification_set)
{
    g_return_if_fail (notification_set != NULL);
    g_return_if_fail (notification_set->refcount > 0);

    notification_set->refcount--;
    if (notification_set->refcount == 0) {
        g_hash_table_destroy(notification_set->work_items);
        g_free(notification_set);
    }
}

void
_ddm_client_notification_set_add (DDMClientNotificationSet *notification_set,
                                  DDMDataResource          *resource,
                                  DDMClient                *client,
                                  DDMDataFetch             *fetch,
                                  GSList                   *changed_properties)
{
    DDMWorkItem *work_item;

    g_return_if_fail (notification_set != NULL);

    work_item = g_hash_table_lookup(notification_set->work_items, client);
    if (work_item == NULL) {
        work_item = _ddm_work_item_notify_client_new(notification_set->model, client);
        g_hash_table_insert(notification_set->work_items, client, work_item);
    }

    _ddm_work_item_notify_client_add(work_item, resource, fetch, changed_properties);
}

static void
add_work_item_foreach (gpointer key,
                       gpointer value,
                       gpointer data)
{
    DDMWorkItem *work_item = value;
    DDMClientNotificationSet *notification_set = data;

    _ddm_data_model_add_work_item(notification_set->model, work_item);
}

void
_ddm_client_notification_set_add_work_items (DDMClientNotificationSet *notification_set)
{
    g_hash_table_foreach(notification_set->work_items, add_work_item_foreach, notification_set);
    g_hash_table_remove_all(notification_set->work_items);
    
}
