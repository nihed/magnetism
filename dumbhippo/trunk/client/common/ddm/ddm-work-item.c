/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "ddm-work-item.h"
#include "ddm-data-model-internal.h"
#include "ddm-data-query-internal.h"
#include "ddm-data-resource-internal.h"

typedef struct {
    DDMDataResource *resource;
    DDMDataFetch *fetch;
    GSList *changed_properties;
} WorkItemNotifyResource;

struct _DDMWorkItem {
    guint refcount;

    enum {
        ITEM_NOTIFY,
        ITEM_QUERY_RESPONSE
    } type;

    DDMDataModel *model;
    DDMDataFetch *fetch;
    gint64 min_serial;

    union {
        struct {
            DDMClient *client;
            GHashTable *resources;
        } notify;
        
        struct {
            DDMDataQuery *query;
        } query_response;
    } u;

    char *id_string;
};

static WorkItemNotifyResource *
work_item_notify_resource_new (DDMDataResource *resource,
                               DDMDataFetch    *fetch,
                               GSList          *changed_properties)
{
    WorkItemNotifyResource *notify_resource = g_new0(WorkItemNotifyResource, 1);
    
    notify_resource->resource = resource;
    notify_resource->fetch = ddm_data_fetch_ref(fetch);
    notify_resource->changed_properties = g_slist_copy(changed_properties);

    return notify_resource;
}

static void
work_item_notify_resource_free (WorkItemNotifyResource *notify_resource)
{
    ddm_data_fetch_unref(notify_resource->fetch);
    g_slist_free(notify_resource->changed_properties);
    
    g_free(notify_resource);
}

DDMWorkItem *
_ddm_work_item_notify_client_new (DDMDataModel *model,
                                  DDMClient    *client)
{
    DDMWorkItem *item = g_new0(DDMWorkItem, 1);
    item->refcount = 1;
    item->model = model;
    item->type = ITEM_NOTIFY;
    item->min_serial = -1;

    item->u.notify.client = g_object_ref(client);
    item->u.notify.resources = g_hash_table_new_full(g_direct_hash, NULL,
                                                     NULL,
                                                     (GDestroyNotify)work_item_notify_resource_free);
    if (client == _ddm_data_model_get_local_client(model))
        item->id_string = g_strdup_printf("Notify-Local");
    else
        item->id_string = g_strdup_printf("Notify-%p", client);

    return item;
}

void
_ddm_work_item_notify_client_add (DDMWorkItem     *item,
                                  DDMDataResource *resource,
                                  DDMDataFetch    *fetch,
                                  GSList          *changed_properties)
{
    WorkItemNotifyResource *notify_resource;
    
    g_return_if_fail (item->type == ITEM_NOTIFY);

    notify_resource = g_hash_table_lookup(item->u.notify.resources, resource);
    if (notify_resource != NULL) {
        /* Not sure that this justifies a hash table, but I have some idea that we might
         * need to track more per-resource state in a way that changes as we resolve
         * fetches.
         */
        g_warning("Duplicate call to _ddm_work_item_notify_client_add for the same resource");
        return;
    }
    
    notify_resource = work_item_notify_resource_new(resource, fetch, changed_properties);
    g_hash_table_insert(item->u.notify.resources, resource, notify_resource);
}

DDMWorkItem *
_ddm_work_item_query_response_new (DDMDataModel *model,
				   DDMDataQuery *query)
{
    DDMWorkItem *item = g_new0(DDMWorkItem, 1);
    item->refcount = 1;
    item->model = model;
    item->type = ITEM_QUERY_RESPONSE;
    item->min_serial = -1;

    item->u.query_response.query = query;

    item->id_string = g_strdup_printf("QueryResponse-%" G_GINT64_MODIFIER "d", _ddm_data_query_get_serial(query));
    
    return item;
}

DDMWorkItem *
_ddm_work_item_ref (DDMWorkItem *item)
{
    g_return_val_if_fail(item != NULL, NULL);
    g_return_val_if_fail(item->refcount > 0, NULL);

    item->refcount++;

    return item;
}

void
_ddm_work_item_unref (DDMWorkItem *item)
{
    g_return_if_fail(item != NULL);
    g_return_if_fail(item->refcount > 0);

    item->refcount--;
    if (item->refcount == 0) {
        switch (item->type) {
        case ITEM_NOTIFY:
            g_object_unref(item->u.notify.client);
            g_hash_table_destroy(item->u.notify.resources);
            break;
        case ITEM_QUERY_RESPONSE:
            break;
        }

        g_free(item);
    }
}

static void
item_fetch_additional_at_resource(DDMWorkItem     *item,
                                  DDMDataResource *resource,
                                  DDMDataFetch    *unrequested_fetch)
{
    const char *resource_id = ddm_data_resource_get_resource_id(resource);
    char *fetch_string = ddm_data_fetch_to_string(unrequested_fetch);
    DDMDataQuery *query;
    gint64 serial;

    g_debug("%s: Must make additional request for %s, fetch=%s",
            item->id_string, resource_id, fetch_string);

    query = _ddm_data_model_query_remote_resource(item->model, resource_id, fetch_string);
    serial = _ddm_data_query_get_serial(query);

    _ddm_data_resource_fetch_requested(resource, unrequested_fetch, serial);
    item->min_serial = MAX(item->min_serial, serial);
    
    g_free(fetch_string);
}

static gboolean
item_fetch_additional(DDMWorkItem     *item,
                      DDMDataResource *resource,
                      DDMDataFetch    *fetch)
{
    DDMDataFetchIter iter;
    gboolean all_satisfied = TRUE;

    if (fetch == NULL)
        return TRUE;

    if (!ddm_data_resource_is_local(resource)) {
        DDMDataFetch *received_fetch;
        DDMDataFetch *unreceived_fetch;
        
        received_fetch = _ddm_data_resource_get_received_fetch(resource);
        if (received_fetch != NULL) {
            unreceived_fetch = ddm_data_fetch_subtract(fetch, received_fetch);
        } else {
            unreceived_fetch = ddm_data_fetch_ref(fetch);
        }

        if (unreceived_fetch != NULL) {
            DDMDataFetch *requested_fetch;
            DDMDataFetch *unrequested_fetch;
            
            requested_fetch = _ddm_data_resource_get_requested_fetch(resource);
            if (requested_fetch != NULL) {
                unrequested_fetch = ddm_data_fetch_subtract(unreceived_fetch, requested_fetch);
            } else {
                unrequested_fetch = ddm_data_fetch_ref(unreceived_fetch);
            }

            if (unrequested_fetch != NULL) {
                item_fetch_additional_at_resource(item, resource, unrequested_fetch);
            } else {
                gint64 old_serial = _ddm_data_resource_get_requested_serial(resource);
                item->min_serial = MAX(item->min_serial, old_serial);
            }

            all_satisfied = FALSE;
            
            if (unrequested_fetch != NULL)
                ddm_data_fetch_unref(unrequested_fetch);
        }

        if (unreceived_fetch != NULL)
            ddm_data_fetch_unref(unreceived_fetch);
    }

    ddm_data_fetch_iter_init(&iter, resource, fetch);

    while (ddm_data_fetch_iter_has_next(&iter)) {
        DDMDataProperty *property;
        DDMDataFetch *children;
        DDMDataValue value;
        
        ddm_data_fetch_iter_next(&iter, &property, &children);

        if (children != NULL) {
            ddm_data_property_get_value(property, &value);
            
            g_assert (DDM_DATA_BASE(value.type) == DDM_DATA_RESOURCE);
            
            if (DDM_DATA_IS_LIST(value.type)) {
                GSList *l;
                
                for (l = value.u.list; l; l = l->next) {
                    if (!item_fetch_additional(item, l->data, children))
                        all_satisfied = FALSE;
                }
            } else {
                if (!item_fetch_additional(item, value.u.resource, children))
                    all_satisfied = FALSE;
            }
        }
    }
    
    ddm_data_fetch_iter_clear(&iter);

    return all_satisfied;
}

typedef struct {
    DDMWorkItem *item;
    gboolean all_satisfied;
} NotifyAddAdditionalClosure;

static void
notify_add_additional_foreach(gpointer key,
                              gpointer value,
                              gpointer data)
{
    WorkItemNotifyResource *notify_resource = value;
    NotifyAddAdditionalClosure *closure = data;

    if (!item_fetch_additional(closure->item, notify_resource->resource, notify_resource->fetch))
        closure->all_satisfied = FALSE;
}

typedef struct {
    DDMWorkItem *item;
    gpointer notification_data;
} NotifyProcessClosure;

static void
notify_process_foreach(gpointer key,
                       gpointer value,
                       gpointer data)
{
    WorkItemNotifyResource *notify_resource = value;
    NotifyProcessClosure *closure = data;

    ddm_client_notify(closure->item->u.notify.client,
                      notify_resource->resource,
                      notify_resource->changed_properties,
                      closure->notification_data);
}

gboolean
_ddm_work_item_process (DDMWorkItem *item)
{
    GSList *l;
    gboolean all_satisfied = TRUE;

    switch (item->type) {
    case ITEM_NOTIFY:
        {
            NotifyAddAdditionalClosure closure;

            closure.item = item;
            closure.all_satisfied = all_satisfied;
            
            g_hash_table_foreach(item->u.notify.resources, notify_add_additional_foreach, &closure);

            all_satisfied = closure.all_satisfied;
        }
        break;
    case ITEM_QUERY_RESPONSE:
        {
            GSList *resources = ddm_data_query_get_results(item->u.query_response.query);
            for (l = resources; l; l = l->next) {
                DDMDataResource *resource = l->data;
                
                if (!item_fetch_additional(item, resource,
                                           ddm_data_query_get_fetch(item->u.query_response.query)))
                    all_satisfied = FALSE;
            }
        }
        break;
    }

    if (all_satisfied) {
        g_debug("%s: all fetches are satisfied", item->id_string);
        
        switch (item->type) {
        case ITEM_NOTIFY:
            {
                NotifyProcessClosure closure;
                
                closure.item = item;
                closure.notification_data = ddm_client_begin_notification(item->u.notify.client);

                g_hash_table_foreach(item->u.notify.resources, notify_process_foreach, &closure);

                ddm_client_end_notification(item->u.notify.client,
                                            closure.notification_data);
            }
            break;
        case ITEM_QUERY_RESPONSE:
            _ddm_data_query_run_response(item->u.query_response.query);
            break;
        }
    } else {
        g_debug("%s: have unsatisfied fetches; need responses; min_serial=%" G_GINT64_MODIFIER "d", item->id_string,
                item->min_serial);
    }

    return all_satisfied;
}

gint64
_ddm_work_item_get_min_serial (const DDMWorkItem *item)
{
    return item->min_serial;
}
