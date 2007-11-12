/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-internal.h"
#include "ddm-data-model-backend.h"
#include "ddm-data-resource-internal.h"
#include "ddm-data-query-internal.h"
#include "ddm-local-client.h"
#include "ddm-rule.h"

static void      ddm_data_model_init                (DDMDataModel       *model);
static void      ddm_data_model_class_init          (DDMDataModelClass  *klass);

static void      ddm_data_model_dispose             (GObject              *object);
static void      ddm_data_model_finalize            (GObject              *object);

struct _DDMDataModel {
    GObject parent;

    const DDMDataModelBackend *backend;
    void *backend_data;
    GFreeFunc free_backend_data_func;

    GHashTable *rules_by_target;
    GHashTable *rules_by_source;

    DDMClient *local_client;
    
    GHashTable *resources;
    GHashTable *changed_resources;

    DDMDataResource *global_resource;
    DDMDataResource *self_resource;

    GQueue *work_items;

    gint64 next_query_serial;
    gint64 max_answered_query_serial;

    guint flush_idle;

    guint ready : 1;
    guint connected : 1;
};

struct _DDMDataModelClass {
    GObjectClass parent_class;
};

enum {
    CONNECTED_CHANGED,
    READY,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];

G_DEFINE_TYPE(DDMDataModel, ddm_data_model, G_TYPE_OBJECT);

static void
ddm_data_model_init(DDMDataModel *model)
{
    model->rules_by_target = g_hash_table_new(g_str_hash, g_str_equal);
    model->rules_by_source = g_hash_table_new(g_str_hash, g_str_equal);
    
    model->resources = g_hash_table_new_full(g_str_hash, g_str_equal,
                                             NULL,
                                             (GDestroyNotify)ddm_data_resource_unref);
    model->changed_resources = g_hash_table_new_full(g_direct_hash, NULL,
                                                     NULL,
                                                     (GDestroyNotify)ddm_data_resource_unref);

    model->work_items = g_queue_new();

    model->max_answered_query_serial = -1;

    model->local_client = _ddm_local_client_new(model);
}

static void
ddm_data_model_class_init(DDMDataModelClass *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS (klass);

    object_class->dispose = ddm_data_model_dispose;
    object_class->finalize = ddm_data_model_finalize;

    signals[CONNECTED_CHANGED] =
        g_signal_new ("connected-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__BOOLEAN,
                      G_TYPE_NONE, 1, G_TYPE_BOOLEAN);
    
    signals[READY] =
        g_signal_new ("ready",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
}

static void
ddm_data_model_dispose(GObject *object)
{
    DDMDataModel *model = DDM_DATA_MODEL(object);

    if (model->backend != NULL) {
        model->backend->remove_model(model, model->backend_data);
        if (model->free_backend_data_func)
            (* model->free_backend_data_func) (model->backend_data);
        model->backend = NULL;
        model->backend_data = NULL;
        model->free_backend_data_func = NULL;
    }

    G_OBJECT_CLASS(ddm_data_model_parent_class)->dispose(object);
}

static void
free_source_rules_foreach(gpointer key,
                          gpointer value,
                          gpointer data)
{
    char *class_id = key;
    GSList *rule_list = value;

    g_free(class_id);
    g_slist_foreach(rule_list, (GFunc)ddm_rule_free, NULL);
    g_slist_free(rule_list);
}

static void
free_target_rules_foreach(gpointer key,
                          gpointer value,
                          gpointer data)
{
    char *class_id = key;
    GSList *rule_list = value;
    
    g_free(class_id);
    g_slist_free(rule_list);
}

static void
ddm_data_model_finalize(GObject *object)
{
    DDMDataModel *model = DDM_DATA_MODEL(object);

    if (model->flush_idle != 0)
        g_source_remove(model->flush_idle);

    /* FIXME: cancel and remove everything still in here */
    g_queue_free(model->work_items);
    
    g_hash_table_destroy(model->resources);
    g_hash_table_destroy(model->changed_resources);

    if (model->global_resource)
        ddm_data_resource_unref(model->global_resource);
    if (model->self_resource)
        ddm_data_resource_unref(model->self_resource);

    g_hash_table_foreach(model->rules_by_source, free_source_rules_foreach, NULL);
    g_hash_table_destroy(model->rules_by_source);

    g_hash_table_foreach(model->rules_by_target, free_target_rules_foreach, NULL);
    g_hash_table_destroy(model->rules_by_target);

    g_object_unref(model->local_client);

    G_OBJECT_CLASS(ddm_data_model_parent_class)->finalize(object);
}

DDMDataModel*
ddm_data_model_new_with_backend (const DDMDataModelBackend *backend,
                                 void                      *backend_data,
                                 GFreeFunc                  free_backend_data_func)
{
    DDMDataModel *model;

    g_return_val_if_fail(backend != NULL, NULL);
    
    model = g_object_new(DDM_TYPE_DATA_MODEL, NULL);

    model->backend = backend;
    model->backend_data = backend_data;
    model->free_backend_data_func = free_backend_data_func;

    model->backend->add_model (model, model->backend_data);
    
    return model;
}

DDMDataModel*
ddm_data_model_new_no_backend (void)
{
    DDMDataModel *model;

    model = g_object_new(DDM_TYPE_DATA_MODEL, NULL);

    model->backend = NULL;
    model->backend_data = NULL;
    model->free_backend_data_func = NULL;

    return model;
}

gboolean
ddm_data_model_get_connected(DDMDataModel   *model)
{
    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), FALSE);

    return model->connected;
}

gboolean
ddm_data_model_is_ready(DDMDataModel   *model)
{
    g_return_val_if_fail(DDM_IS_DATA_MODEL(model), FALSE);

    return model->ready;
}

static GHashTable *
params_from_valist(va_list vap)
{
    GHashTable *params = g_hash_table_new(g_str_hash, g_str_equal);

    while (TRUE) {
        const char *param_name = va_arg(vap, const char *);
        const char *param_value;
        if (param_name == NULL)
            break;

        param_value = va_arg(vap, const char *);

        g_hash_table_insert(params, (char *)param_name, (char *)param_value);
    }

    return params;
}

static void
dump_param_foreach(gpointer k,
                   gpointer v,
                   gpointer data)
{
    DDMDataQuery *query = data;
    const char *key = k;
    const char *value = v;
    const char *id_string = ddm_data_query_get_id_string(query);
    
    g_debug("%s: %s='%s'", id_string, key, value);
}

static void
debug_dump_query(DDMDataQuery *query)
{
    const char *id_string = ddm_data_query_get_id_string(query);
    DDMQName *qname = ddm_data_query_get_qname(query);
    
    g_debug("%s: Sending to server", id_string);
    g_debug("%s: uri=%s#%s", id_string, qname->uri, qname->name);
    g_debug("%s: fetch=%s", id_string, ddm_data_query_get_fetch_string(query));
    g_hash_table_foreach(ddm_data_query_get_params(query), dump_param_foreach, query);
}

static DDMDataQuery *
data_model_query_params_internal(DDMDataModel *model,
                                 const char   *method,
                                 const char   *fetch,
                                 GHashTable   *params,
                                 gboolean      force_remote)
{
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail (model->backend != NULL, NULL);

    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL)
        return NULL;

    query = _ddm_data_query_new(model, method_qname, fetch, params, model->next_query_serial++);
    if (query == NULL) /* Bad fetch string */
        return NULL;

    if (!force_remote && method_qname == ddm_qname_get("http://mugshot.org/p/system", "getResource")) {
        const char *resource_id = g_hash_table_lookup(params, "resourceId");
        DDMDataResource *resource;
        GSList *results;
        
        if (resource_id == NULL) {
            ddm_data_query_error_async (query,
                                        DDM_DATA_ERROR_BAD_REQUEST,
                                        "resourceId parameter is required for http://mugshot.org/p/system#getResource");
            return query;
        }

        /* The reason why we ensure (without specifying a class_id) here is so that
         * we can record the fetches as they go out and avoid sending duplicate
         * fetches to the server even if we've never fetched anything for the
         * resource before.
         *
         * The actual fetch will be done when we process the response work item
         * and find out what we are missing.
         *
         * We do the lookup first to avoid complaints about calling ensure_resource()
         * on a local resource.
         */
        resource = ddm_data_model_lookup_resource(model, resource_id);
        if (resource == NULL)
            resource = ddm_data_model_ensure_resource(model, resource_id, NULL);
        results = g_slist_prepend(NULL, resource);

        _ddm_data_query_local_response(query, results);
        
        g_slist_free(results);
        
        return query;
    }
    
    debug_dump_query(query);
    model->backend->send_query(model, query, model->backend_data);
    return query;
}

DDMDataQuery *
ddm_data_model_query_params(DDMDataModel *model,
                            const char   *method,
                            const char   *fetch,
                            GHashTable   *params)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail (model->backend != NULL, NULL);

    return data_model_query_params_internal(model, method, fetch, params, FALSE);
}

DDMDataQuery *
ddm_data_model_query(DDMDataModel *model,
                     const char     *method,
                     const char     *fetch,
                     ...)
{
    DDMDataQuery *query;
    GHashTable *params;
    va_list vap;

    va_start(vap, fetch);
    params = params_from_valist(vap);
    va_end(vap);

    query = ddm_data_model_query_params(model, method, fetch, params);

    g_hash_table_destroy(params);

    return query;
}

static DDMDataQuery *
data_model_query_internal(DDMDataModel *model,
                          const char   *method,
                          const char   *fetch,
                          gboolean      force_remote,
                          ...)
{
    DDMDataQuery *query;
    GHashTable *params;
    va_list vap;

    va_start(vap, force_remote);
    params = params_from_valist(vap);
    va_end(vap);

    query = data_model_query_params_internal(model, method, fetch, params, force_remote);

    g_hash_table_destroy(params);

    return query;
}

DDMDataQuery *
ddm_data_model_query_resource (DDMDataModel    *model,
                               DDMDataResource *resource,
                               const char      *fetch)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail (resource != NULL, NULL);
    g_return_val_if_fail (fetch != NULL, NULL);

    return data_model_query_internal(model, "http://mugshot.org/p/system#getResource", fetch, FALSE,
                                     "resourceId", ddm_data_resource_get_resource_id(resource),
                                     NULL);
}

DDMDataQuery *
ddm_data_model_query_resource_by_id(DDMDataModel *model,
                                    const char     *resource_id,
                                    const char     *fetch)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail (resource_id != NULL, NULL);
    g_return_val_if_fail (fetch != NULL, NULL);

    return data_model_query_internal(model, "http://mugshot.org/p/system#getResource", fetch, FALSE,
                                     "resourceId", resource_id,
                                     NULL);
}

DDMDataQuery *
_ddm_data_model_query_remote_resource(DDMDataModel *model,
                                      const char   *resource_id,
                                      const char   *fetch)
{
    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);

    return data_model_query_internal(model, "http://mugshot.org/p/system#getResource", fetch, TRUE,
                                     "resourceId", resource_id,
                                     NULL);
}

DDMDataQuery *
ddm_data_model_update_params(DDMDataModel *model,
                             const char   *method,
                             GHashTable   *params)
{
    DDMDataQuery *query;
    DDMQName *method_qname;

    g_return_val_if_fail (DDM_IS_DATA_MODEL(model), NULL);
    g_return_val_if_fail (model->backend != NULL, NULL);
    
    method_qname = ddm_qname_from_uri(method);
    if (method_qname == NULL) /* Invalid method URI */
        return NULL;

    query = _ddm_data_query_new_update(model, method_qname, params, model->next_query_serial++);

    debug_dump_query(query);
    
    model->backend->send_update(model, query, model->backend_data);
    
    return query;
}

DDMDataQuery *
ddm_data_model_update(DDMDataModel *model,
                      const char   *method,
                      ...)
{
    DDMDataQuery *query;
    GHashTable *params;
    va_list vap;

    va_start(vap, method);
    params = params_from_valist(vap);
    va_end(vap);

    query = ddm_data_model_update_params(model, method, params);

    g_hash_table_destroy(params);

    return query;
}

DDMDataResource *
ddm_data_model_lookup_resource(DDMDataModel *model,
                               const char     *resource_id)
{
    return g_hash_table_lookup(model->resources, resource_id);
}

static DDMDataResource *
ensure_resource_internal(DDMDataModel *model,
                         const char   *resource_id,
                         const char   *class_id,
                         gboolean      local)
{
    DDMDataResource *resource;

    local = local != FALSE;

    resource = g_hash_table_lookup(model->resources, resource_id);
    if (resource) {
        if ((local != FALSE) != ddm_data_resource_is_local(resource)) {
            g_warning("Mismatch for 'local' nature of resource '%s', old=%d, new=%d",
                      resource_id, !local, local);
        }

        if (class_id) {
            const char *old_class_id = ddm_data_resource_get_class_id(resource);
            if (old_class_id) {
                if (strcmp(class_id, old_class_id) != 0)
                    g_warning("Mismatch for class_id of resonurce '%s', old=%s, new=%s",
                              resource_id, old_class_id, class_id);
            } else {
                ddm_data_resource_set_class_id(resource, class_id);
            }
        }

    } else {
        resource = _ddm_data_resource_new(model, resource_id, class_id, local);
        g_hash_table_insert(model->resources, (char *)ddm_data_resource_get_resource_id(resource), resource);
    }

    return resource;
}

DDMDataResource *
ddm_data_model_ensure_resource(DDMDataModel *model,
                               const char   *resource_id,
                               const char   *class_id)
{
    return ensure_resource_internal(model, resource_id, class_id, FALSE);
}

DDMDataResource *
ddm_data_model_ensure_local_resource(DDMDataModel *model,
                                     const char   *resource_id,
                                     const char   *class_id)
{
    return ensure_resource_internal(model, resource_id, class_id, TRUE);
}

static gboolean
model_reset_foreach (gpointer key,
                     gpointer value,
                     gpointer data)
{
    DDMDataResource *resource = value;

    return _ddm_data_resource_reset(resource);
}

static gboolean
model_reset_changed_resource_foreach (gpointer key,
                                      gpointer value,
                                      gpointer data)
{
    DDMDataResource *resource = value;

    return !ddm_data_resource_is_local(resource);
}

void
ddm_data_model_reset (DDMDataModel *model)
{
    g_hash_table_foreach_remove(model->resources, model_reset_foreach, NULL);
    g_hash_table_foreach_remove(model->changed_resources, model_reset_changed_resource_foreach, NULL);

    if (model->global_resource != NULL && !ddm_data_resource_is_local(model->global_resource)) {
        ddm_data_resource_unref(model->global_resource);
        model->global_resource = NULL;
    }
    
    if (model->self_resource != NULL && !ddm_data_resource_is_local(model->self_resource)) {
        ddm_data_resource_unref(model->self_resource);
        model->self_resource = NULL;
    }
}

void
ddm_data_model_set_connected (DDMDataModel   *model,
                              gboolean        connected)
{
    connected = connected != FALSE;
    if (connected == model->connected)
        return;

    model->connected = connected;
    g_signal_emit(G_OBJECT(model), signals[CONNECTED_CHANGED], 0, connected);
}

void
ddm_data_model_signal_ready (DDMDataModel *model)
{
    model->ready = TRUE;
    g_signal_emit(G_OBJECT(model), signals[READY], 0);
}

void
_ddm_data_model_mark_changed(DDMDataModel    *model,
                             DDMDataResource *resource)
{
    if (g_hash_table_lookup(model->changed_resources, resource) == NULL) {
        g_hash_table_insert(model->changed_resources, resource, ddm_data_resource_ref(resource));
    }

    ddm_data_model_schedule_flush(model);
}

static gboolean
do_flush(gpointer data)
{
    ddm_data_model_flush(data);

    return FALSE;
}

void
ddm_data_model_schedule_flush (DDMDataModel *model)
{
    if (model->flush_idle == 0)
        model->flush_idle = g_idle_add(do_flush, model);
}

static int
compare_work_items(gconstpointer  a,
                   gconstpointer  b,
                   gpointer       data)
{
    const DDMWorkItem *item_a = a;
    const DDMWorkItem *item_b = b;

    gint64 serial_a = _ddm_work_item_get_min_serial(item_a);
    gint64 serial_b = _ddm_work_item_get_min_serial(item_b);

    return (serial_a < serial_b) ? -1 : (serial_a == serial_b ? 0 : 1);
}

void
_ddm_data_model_add_work_item (DDMDataModel    *model,
                               DDMWorkItem     *item)
{
    GList *l;
    
    _ddm_work_item_ref(item);

    /* Two situations:
     *
     * We have a new item with min_serial = -1; it probably goes right
     * near the beginning of the queue.
     *
     * We have an item that now has a min_serial because we've sent a
     * request to the server; it goes at the end of the queue.
     *
     * Slightly over-optimize here by handling the two cases separately.
     * Note that both cases are different from g_queue_insert_sorted()
     * because we insert after on the tie-break to avoid reordering
     * when we pull things off the front.
     */
    if (_ddm_work_item_get_min_serial(item) == -1) {
        for (l = model->work_items->head; l; l = l->next) {
            if (compare_work_items(l->data, item, NULL) > 0)
                break;
        }

        if (l == NULL)
            g_queue_push_tail(model->work_items, item);
        else
            g_queue_insert_before(model->work_items, l, item);
        
    } else {
        for (l = model->work_items->tail; l; l = l->prev) {
            if (compare_work_items(l->data, item, NULL) <= 0)
                break;
        }
        
        if (l == NULL)
            g_queue_push_head(model->work_items, item);
        else
            g_queue_insert_after(model->work_items, l, item);
    }
    

    ddm_data_model_schedule_flush(model);
}

gboolean
ddm_data_model_needs_flush(DDMDataModel *model)
{
    return model->flush_idle != 0;
}

static void
flush_notifications_foreach(gpointer key,
                            gpointer value,
                            gpointer data)
{
    DDMDataResource *resource = key;
    DDMClientNotificationSet *notification_set = data;

    _ddm_data_resource_resolve_notifications(resource, notification_set);
}

static void
get_values_foreach(gpointer key,
                   gpointer value,
                   gpointer data)
{
    GSList **values = data;

    *values = g_slist_prepend(*values, value);
}

static GSList *
hash_table_get_values(GHashTable *hash_table)
{
    GSList *values = NULL;

    g_hash_table_foreach(hash_table, get_values_foreach, &values);

    return values;
}

static void
data_model_flush_rules(DDMDataModel *model)
{
    GSList *resources_to_process;
    GSList *l;
    
    if (g_hash_table_size(model->changed_resources) == 0)
        return;

    /* We need to snapshot the changed resources, since the set of changd resources
     * may be extended as we process rules.
     */
    resources_to_process = hash_table_get_values(model->changed_resources);

    for (l = resources_to_process; l; l = l->next) {
        _ddm_data_resource_update_rule_properties(l->data);
    }

    g_slist_free(resources_to_process);
}

static void
data_model_flush_notifications(DDMDataModel *model)
{
    DDMClientNotificationSet *notification_set;
    
    if (g_hash_table_size(model->changed_resources) == 0)
        return;

    notification_set = _ddm_client_notification_set_new(model);

    g_hash_table_foreach(model->changed_resources, flush_notifications_foreach, notification_set);
    g_hash_table_remove_all(model->changed_resources);

    _ddm_client_notification_set_add_work_items(notification_set);
    _ddm_client_notification_set_unref(notification_set);
}

static void
data_model_flush_work_items(DDMDataModel *model)
{
    GList *items;
    GList *l;
    int count = 0;
    
    /* Find the work items that might possibly be ready to process */
    count = 0;
    for (l = model->work_items->head; l; l = l->next) {
        DDMWorkItem *item = l->data;

        if (_ddm_work_item_get_min_serial(item) > model->max_answered_query_serial)
            break;

        count++;
    }

    /* And chop them out of the list of pending items */
    if (l == model->work_items->head) {
        return;
    } else if (l == NULL) {
        items = model->work_items->head;
        model->work_items->head = NULL;
        model->work_items->tail = NULL;
        model->work_items->length = 0;
    } else {
        l->prev->next = NULL;
        l->prev = NULL;
        
        items = model->work_items->head;
        model->work_items->head = l;
        model->work_items->length -= count;
    }

    /* Now walk through the possibly ready items, process them and insert them
     * back into the list if they are still not ready.
     */
    for (l = items; l; l = l->next) {
        DDMWorkItem *item = l->data;

        if (!_ddm_work_item_process(item))
            _ddm_data_model_add_work_item(model, item);

        _ddm_work_item_unref(item);
    }

    g_list_free(items);
}

void
ddm_data_model_flush(DDMDataModel *model)
{
    if (model->flush_idle == 0)
        return;

    g_debug("Flushing Data Model");

    g_source_remove(model->flush_idle);
    model->flush_idle = 0;

    if (model->backend->flush)
        model->backend->flush(model, model->backend_data);

    data_model_flush_rules(model);
    data_model_flush_notifications(model);
    data_model_flush_work_items(model);
}

void
ddm_data_model_add_rule (DDMDataModel       *model,
                         const char         *target_class_id,
                         const char         *target_property,
                         const char         *source_class_id,
                         DDMDataCardinality  cardinality,
                         gboolean            default_include,
                         const char         *default_children,
                         const char         *condition)
{
    DDMRule *rule;
    GSList *target_rules;
    GSList *source_rules;

    g_return_if_fail(DDM_IS_DATA_MODEL(model));
    g_return_if_fail(target_class_id != NULL);
    g_return_if_fail(target_property != NULL);
    g_return_if_fail(source_class_id != NULL);
    g_return_if_fail(cardinality == DDM_DATA_CARDINALITY_01 || cardinality == DDM_DATA_CARDINALITY_N);
    g_return_if_fail(condition != NULL);

    rule = ddm_rule_new(target_class_id, target_property,
                        source_class_id,
                        cardinality, default_include, default_children,
                        condition);
    
    if (rule == NULL) /* failed validation, will have warned */
        return;

    target_rules = g_hash_table_lookup(model->rules_by_target, target_class_id);
    if (target_rules) {
        target_rules = g_slist_prepend(target_rules, rule);
        g_hash_table_insert(model->rules_by_target,
                            (char *)target_class_id, /* old value is kept */
                            target_rules);
    } else {
        target_rules = g_slist_prepend(NULL, rule);
        g_hash_table_insert(model->rules_by_target,
                            g_strdup(target_class_id),
                            target_rules);
    }
    
    source_rules = g_hash_table_lookup(model->rules_by_source, source_class_id);
    source_rules = g_slist_prepend(source_rules, rule);
    if (source_rules) {
        source_rules = g_slist_prepend(source_rules, rule);
        g_hash_table_insert(model->rules_by_source,
                            (char *)source_class_id,
                            source_rules);
    } else {
        source_rules = g_slist_prepend(NULL, rule);
        g_hash_table_insert(model->rules_by_source,
                            g_strdup(source_class_id), /* old value is kept */
                            source_rules);
    }
}

typedef struct {
    const char *class_id;
    DDMCondition *condition;
    gboolean find_sources;
    GSList *results;
} FindResourcesClosure;

static void
find_resources_foreach(gpointer key,
                       gpointer value,
                       gpointer data)
{
    DDMDataResource *resource = value;
    FindResourcesClosure *closure = data;
    const char *class_id = ddm_data_resource_get_class_id(resource);
    gboolean match;

    if (class_id == NULL || strcmp(class_id, closure->class_id) != 0)
        return;

    if (closure->find_sources)
        match = ddm_condition_matches_source(closure->condition, resource);
    else
        match = ddm_condition_matches_target(closure->condition, resource);

    if (!match)
        return;

    closure->results = g_slist_prepend(closure->results, resource);
}

static GSList *
find_resources(DDMDataModel *model,
               const char   *class_id,
               DDMCondition *condition,
               gboolean      find_sources)
{
    /* This is the most inefficient implementation possible; to improve, you'd
     * want to:
     *
     *  A) Index resources in the model by class_id
     *  B) Index resources in the model by property values that rules match upon
     *
     * It might be easiest to do B with an explicit:
     *
     *  ddm_data_model_add_index(DDMDataModel *model,
     *                           const char   *property_uri);
     */
    
    FindResourcesClosure closure;

    closure.class_id = class_id;
    closure.condition = condition;
    closure.find_sources = find_sources;
    closure.results = NULL;

    g_hash_table_foreach(model->resources, find_resources_foreach, &closure);

    return closure.results;
}

GSList *
_ddm_data_model_find_sources(DDMDataModel *model,
                             const char   *source_class_id,
                             DDMCondition *condition)
{
    return find_resources(model, source_class_id, condition, TRUE);
}

GSList *
_ddm_data_model_find_targets(DDMDataModel *model,
                             const char   *target_class_id,
                             DDMCondition *condition)
{
    return find_resources(model, target_class_id, condition, FALSE);
}

GSList *
_ddm_data_model_get_target_rules(DDMDataModel *model,
                                 const char   *class_id)
{
    return g_hash_table_lookup(model->rules_by_target, class_id);
}

GSList *
_ddm_data_model_get_source_rules(DDMDataModel *model,
                                 const char   *class_id)
{
    return g_hash_table_lookup(model->rules_by_source, class_id);
}

void
_ddm_data_model_query_answered (DDMDataModel *model,
                                DDMDataQuery *query)
{
    gint64 serial = _ddm_data_query_get_serial(query);
    if (serial > model->max_answered_query_serial)
        model->max_answered_query_serial = serial;
}

DDMClient *
_ddm_data_model_get_local_client (DDMDataModel *model)
{
    return model->local_client;
}

void
ddm_data_model_set_global_resource (DDMDataModel    *model,
                                    DDMDataResource *global_resource)
{
    if (global_resource == model->global_resource)
        return;

    if (model->global_resource)
        ddm_data_resource_unref(model->global_resource);
    
    model->global_resource = global_resource;
    
    if (model->global_resource)
        ddm_data_resource_ref(model->global_resource);
}

void
ddm_data_model_set_self_resource (DDMDataModel    *model,
                                  DDMDataResource *self_resource)
{
    if (self_resource == model->self_resource)
        return;

    if (model->self_resource)
        ddm_data_resource_unref(model->self_resource);
    
    model->self_resource = self_resource;
    
    if (model->self_resource)
        ddm_data_resource_ref(model->self_resource);
}

DDMDataResource *
ddm_data_model_get_global_resource(DDMDataModel *model)
{
    return model->global_resource;
}

DDMDataResource *
ddm_data_model_get_self_resource(DDMDataModel *model)
{
    return model->self_resource;
}
