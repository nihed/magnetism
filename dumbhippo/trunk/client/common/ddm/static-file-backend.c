/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-backend.h"
#include "static-file-backend.h"
#include "ddm-data-query.h"
#include "ddm-notification-set.h"

typedef enum {
    PENDING_REQUEST_QUERY,
    PENDING_REQUEST_UPDATE
} PendingRequestType;

typedef struct {
    PendingRequestType type;
    DDMDataQuery *query;
} PendingRequest;

typedef struct {
    DDMDataModel *ddm_model;
    GSList *pending_requests;

    DDMDataModel *backend_model;
} StaticFileModel;

static StaticFileModel*
get_static_file_model(DDMDataModel *ddm_model)
{
    StaticFileModel *static_file_model = g_object_get_data(G_OBJECT(ddm_model), "dbus-data-model");

    return static_file_model;
}

static DDMDataResource *
model_ensure_resource(StaticFileModel *static_file_model,
                      DDMDataResource *backend_resource)
{
    return ddm_data_model_ensure_resource(static_file_model->ddm_model,
                                          ddm_data_resource_get_resource_id(backend_resource),
                                          ddm_data_resource_get_class_id(backend_resource));
}

static DDMDataResource *
model_process_query_recurse(StaticFileModel *static_file_model,
                            DDMDataQuery    *query,
                            DDMDataResource *backend_resource,
                            DDMDataFetch    *fetch)
{
    DDMDataFetchIter iter;
    DDMDataResource *resource;

    resource = model_ensure_resource(static_file_model, backend_resource);
    
    /* FIXME: this doesn't prevent infinite recursion; we need to track
     *   the cumulative fetch for each resource. We could store it here,
     *   like in hippo-dbus-model.c and the Java server, but maybe we
     *   should be tracking fetch in DDMDataResource? (There's a problem
     *   there for client-server in that it's a little hard to know the
     *   per-resource fetch that the server actually used for each
     *   resource. The client has to process the fetch itself after
     *   receiving the reply. Or have the server tell it; we have it
     *   in the XML protocol but not in the D-BUS protocol.)
     */

    /* Step 1: update the immediate properties of this resource; this may involve
     * creating referenced resources, but we don't descend into them
     */

    ddm_data_fetch_iter_init(&iter, backend_resource, fetch);

    while (ddm_data_fetch_iter_has_next(&iter)) {
        DDMDataProperty *backend_property;
        DDMDataProperty *frontend_property;
        DDMQName *property_id;
        DDMDataValue value;
        DDMDataCardinality cardinality;
        gboolean default_include;
        DDMDataFetch *default_children;
        char *default_children_string;
        GSList *l;

        ddm_data_fetch_iter_next(&iter, &backend_property, NULL);

        property_id = ddm_data_property_get_qname(backend_property);
        
        frontend_property = ddm_data_resource_get_property_by_qname(resource, property_id);
        if (frontend_property) /* Already have it */
            continue;

        ddm_data_property_get_value(backend_property, &value);
        cardinality = ddm_data_property_get_cardinality(backend_property);
        default_include = ddm_data_property_get_default_include(backend_property);
        default_children = ddm_data_property_get_default_children(backend_property);
        if (default_children != NULL)
            default_children_string = ddm_data_fetch_to_string(default_children);
        else
            default_children_string = NULL;

        if (DDM_DATA_BASE(value.type) == DDM_DATA_RESOURCE) {
            DDMDataValue new_value;

            new_value.type = DDM_DATA_RESOURCE;
            
            if (DDM_DATA_IS_LIST(value.type)) {
                for (l = value.u.list; l; l = l->next) {
                    new_value.u.resource = model_ensure_resource(static_file_model, l->data);
                    ddm_data_resource_update_property(resource, property_id,
                                                      DDM_DATA_UPDATE_ADD,
                                                      cardinality, default_include, default_children_string, &new_value);
                }
            } else {
                new_value.u.resource = model_ensure_resource(static_file_model, value.u.resource);
                ddm_data_resource_update_property(resource, property_id,
                                                  DDM_DATA_UPDATE_ADD,
                                                  cardinality, default_include, default_children_string, &new_value);
            }
        } else if (DDM_DATA_IS_LIST(value.type)) {
            for (l = value.u.list; l; l = l->next) {
                DDMDataValue element;
                
                ddm_data_value_get_element(&value, l, &element);
                ddm_data_resource_update_property(resource, property_id,
                                                  DDM_DATA_UPDATE_ADD,
                                                  cardinality, default_include, default_children_string, &element);
            }
        } else if (DDM_DATA_BASE(value.type) == DDM_DATA_NONE) { /* Empty list */
            ddm_data_resource_update_property(resource, property_id,
                                              DDM_DATA_UPDATE_CLEAR,
                                              cardinality, default_include, default_children_string, &value);
        } else {
            ddm_data_resource_update_property(resource, property_id,
                                              DDM_DATA_UPDATE_ADD,
                                              cardinality, default_include, default_children_string, &value);
        }

        g_free(default_children_string);
    }
    
    ddm_data_fetch_iter_clear(&iter);

    /* Step 2, descend into referenced resources
     */

    ddm_data_fetch_iter_init(&iter, backend_resource, fetch);

    while (ddm_data_fetch_iter_has_next(&iter)) {
        DDMDataProperty *backend_property;
        DDMDataFetch *children;
        DDMDataValue value;
        GSList *l;
        
        ddm_data_fetch_iter_next(&iter, &backend_property, &children);
        
        if (children != NULL) {
            ddm_data_property_get_value(backend_property, &value);
            
            if (DDM_DATA_BASE(value.type) == DDM_DATA_RESOURCE) { /* Could also be NONE */
                if (DDM_DATA_IS_LIST(value.type)) {
                    for (l = value.u.list; l; l = l->next) {
                        model_process_query_recurse(static_file_model, query,
                                                    l->data, children);
                    }
                } else {
                    model_process_query_recurse(static_file_model, query,
                                                value.u.resource, children);
                }
            }
        }
    }
    
    ddm_data_fetch_iter_clear(&iter);
    
    return resource;
}

static void
model_process_query(StaticFileModel *static_file_model,
                    DDMDataQuery    *query)
{
    DDMQName *query_qname = ddm_data_query_get_qname(query);
    GHashTable *params = ddm_data_query_get_params(query);
    DDMDataFetch *fetch = ddm_data_query_get_fetch(query);
    const char *resource_id;
    DDMDataResource *backend_resource;
    DDMDataResource *resource;
    GSList *results;

    if (query_qname != ddm_qname_get("http://mugshot.org/p/system", "getResource")) {
        ddm_data_query_error (query,
                              DDM_DATA_ERROR_ITEM_NOT_FOUND,
                              "Static file model only supports http://mugshot.org/p/system#getResource");
        return;
    }

    resource_id = g_hash_table_lookup(params, "resourceId");
    if (resource_id == NULL) {
        ddm_data_query_error (query,
                              DDM_DATA_ERROR_BAD_REQUEST,
                              "resourceId parameter is required for http://mugshot.org/p/system#getResource");
        return;
    }

    backend_resource = ddm_data_model_lookup_resource(static_file_model->backend_model,
                                                      resource_id);

    if (backend_resource == NULL) {
        ddm_data_query_error (query,
                              DDM_DATA_ERROR_ITEM_NOT_FOUND,
                              "No such resource");
        return;
    }

    resource = model_process_query_recurse(static_file_model, query, backend_resource, fetch);

    results = g_slist_prepend(NULL, resource);
    ddm_data_query_response(query, results);
    g_slist_free(results);
}
    
static void
model_process_update(StaticFileModel *static_file_model,
                     DDMDataQuery    *query)
{
    ddm_data_query_error (query,
                          DDM_DATA_ERROR_ITEM_NOT_FOUND,
                          "Static file model has no update methods");
}

static void
static_file_add_model (DDMDataModel *ddm_model,
		       void         *backend_data)
{
    StaticFileModel *static_file_model;
    
    static_file_model = g_new0(StaticFileModel, 1);
    static_file_model->ddm_model = ddm_model;
    
    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", static_file_model);

    ddm_data_model_set_connected(static_file_model->ddm_model, TRUE);
}

static void
static_file_remove_model (DDMDataModel *ddm_model,
                          void         *backend_data)
{
    StaticFileModel *static_file_model;

    static_file_model = get_static_file_model(ddm_model);
    
    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", NULL);

    while (static_file_model->pending_requests != NULL) {
        PendingRequest *pr = static_file_model->pending_requests->data;
        static_file_model->pending_requests = g_slist_delete_link(static_file_model->pending_requests,
                                                                  static_file_model->pending_requests);

        ddm_data_query_error (pr->query,
                              DDM_DATA_ERROR_INTERNAL,
                              "Pending request on model shutdown");

    }

    /* FIXME: free backend_model */
    
    g_free(static_file_model);
}

static void
model_add_pending_request(StaticFileModel *static_file_model,
                          PendingRequest  *pr)
{
    static_file_model->pending_requests = g_slist_append(static_file_model->pending_requests, pr);
    ddm_data_model_schedule_flush(static_file_model->ddm_model);
}

static void
static_file_send_query (DDMDataModel *ddm_model,
                        DDMDataQuery *query,
                        void         *backend_data)
{
    StaticFileModel *static_file_model;
    PendingRequest *pr;

    static_file_model = get_static_file_model(ddm_model);

    pr = g_new0(PendingRequest, 1);
    pr->type = PENDING_REQUEST_QUERY;
    pr->query = query;

    model_add_pending_request(static_file_model, pr);
}

static void
static_file_send_update (DDMDataModel *ddm_model,
			 DDMDataQuery *query,
			 void         *backend_data)
{
    StaticFileModel *static_file_model;
    PendingRequest *pr;

    static_file_model = get_static_file_model(ddm_model);

    pr = g_new0(PendingRequest, 1);
    pr->type = PENDING_REQUEST_UPDATE;
    pr->query = query;
    
    model_add_pending_request(static_file_model, pr);
}

static void
static_file_flush (DDMDataModel *model,
                   void         *backend_data)
{
    StaticFileModel *static_file_model = get_static_file_model(model);

    while (static_file_model->pending_requests != NULL) {
        PendingRequest *pr = static_file_model->pending_requests->data;
        static_file_model->pending_requests = g_slist_remove(static_file_model->pending_requests,
                                                             static_file_model->pending_requests->data);
        
        if (pr->type == PENDING_REQUEST_QUERY) {
            model_process_query(static_file_model, pr->query);
        } else if (pr->type == PENDING_REQUEST_UPDATE) {
            model_process_update(static_file_model, pr->query);
        } else {
            g_error("unknown pending request type");
        }
        
        g_free(pr);
    }
}

static const DDMDataModelBackend static_file_backend = {
    static_file_add_model,
    static_file_remove_model,
    static_file_send_query,
    static_file_send_update,
    static_file_flush
};

DDMDataModel*
ddm_static_file_model_new (const char *filename,
                           GError    **error)
{
    DDMDataModel *model = ddm_data_model_new_with_backend(&static_file_backend,
                                                          NULL, NULL);
    StaticFileModel *static_file_model;

    static_file_model = get_static_file_model(model);
    static_file_model->backend_model = ddm_data_model_new_no_backend();
    
    if (!ddm_static_file_parse(filename, static_file_model->backend_model, error)) {
        /* FIXME: cleanup and free the models */
        return NULL;
    }

    return model;
}

