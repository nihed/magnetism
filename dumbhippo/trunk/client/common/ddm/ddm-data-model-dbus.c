/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"
#include "ddm-data-model-backend.h"
#include "ddm-data-query.h"
#include "ddm-notification-set.h"
#include "hippo-dbus-helper.h"

#include <dbus/dbus.h>

typedef enum {
    PENDING_REQUEST_QUERY,
    PENDING_REQUEST_UPDATE
} PendingRequestType;

typedef struct {
    PendingRequestType type;
    DDMDataQuery *query;
    char         *method;
    GHashTable   *params;
} PendingRequest;

typedef struct {
    int             refcount;
    char           *path;
    DDMDataModel   *ddm_model;
    DBusConnection *connection;
    HippoDBusProxy *engine_proxy;
    HippoDBusProxy *engine_props_proxy;
    GSList *pending_requests;
} DBusModel;

static void ddm_dbus_send_query   (DDMDataModel *ddm_model,
                                   DDMDataQuery *query,
                                   void         *backend_data);
static void ddm_dbus_send_update  (DDMDataModel *ddm_model,
                                   DDMDataQuery *query,
                                   const char   *method,
                                   GHashTable   *params,
                                   void         *backend_data);

static void
model_ref(DBusModel *dbus_model)
{
    g_return_if_fail(dbus_model->refcount > 0);
    
    dbus_model->refcount += 1;
}

static void
model_unref(DBusModel *dbus_model)
{
    g_return_if_fail(dbus_model->refcount > 0);

    dbus_model->refcount -= 1;
    if (dbus_model->refcount == 0) {
        g_assert(dbus_model->connection == NULL);
        g_assert(dbus_model->path == NULL);

        g_free(dbus_model);
    }
}

static DBusModel*
get_dbus_model(DDMDataModel *ddm_model)
{
    DBusModel *dbus_model = g_object_get_data(G_OBJECT(ddm_model), "dbus-data-model");

    return dbus_model;
}

static void
model_add_pending_query(DBusModel    *dbus_model,
                        DDMDataQuery *query)
{
    PendingRequest *pr;

    g_debug("delaying Query to org.freedesktop.od.Engine until we connect");
    
    pr = g_new0(PendingRequest, 1);
    pr->type = PENDING_REQUEST_QUERY;
    pr->query = query;

    dbus_model->pending_requests = g_slist_append(dbus_model->pending_requests, pr);
}

static void
copy_foreach(void *key,
             void *value,
             void *data)
{
    GHashTable *copy = data;
    g_hash_table_replace(copy, g_strdup(key), g_strdup(value));
}

static GHashTable*
deep_copy_string_hash(GHashTable *orig)
{
    GHashTable *copy;
    
    copy = g_hash_table_new_full(g_str_hash, g_str_equal,
                                 (GDestroyNotify)g_free, (GDestroyNotify)g_free);

    g_hash_table_foreach(orig, copy_foreach, copy);

    return copy;
}

static void
model_add_pending_update(DBusModel    *dbus_model,
                         DDMDataQuery *query,
                         const char   *method,
                         GHashTable   *params)
{
    PendingRequest *pr;

    g_debug("delaying Update to org.freedesktop.od.Engine until we connect");
    
    pr = g_new0(PendingRequest, 1);
    pr->type = PENDING_REQUEST_UPDATE;
    pr->query = query;
    pr->method = g_strdup(method);
    pr->params = deep_copy_string_hash(params);
    
    dbus_model->pending_requests = g_slist_append(dbus_model->pending_requests, pr);
}

static void
model_send_pending(DBusModel *dbus_model)
{
    g_debug("Sending %d pending requests to org.freedesktop.od.Engine",
            g_slist_length(dbus_model->pending_requests));
    
    while (dbus_model->pending_requests != NULL) {
        PendingRequest *pr = dbus_model->pending_requests->data;
        dbus_model->pending_requests = g_slist_remove(dbus_model->pending_requests,
                                                      dbus_model->pending_requests->data);
        
        if (pr->type == PENDING_REQUEST_QUERY) {
            ddm_dbus_send_query(dbus_model->ddm_model, pr->query, NULL);
            g_free(pr);
        } else if (pr->type == PENDING_REQUEST_UPDATE) {
            ddm_dbus_send_update(dbus_model->ddm_model, pr->query,
                                 pr->method, pr->params, NULL);
            g_free(pr->method);
            g_hash_table_destroy(pr->params);
            g_free(pr);
        } else {
            g_warning("unknown pending request type");
        }
    }
}

static gboolean
read_data_attributes(DBusMessageIter    *prop_struct_iter,
                     DDMDataUpdate      *update_type_p,
                     DDMDataType        *data_type_p,
                     DDMDataCardinality *cardinality_p)
{
    DDMDataType data_type;
    DDMDataUpdate update_type;
    DDMDataCardinality cardinality;
    guchar update_type_wire, data_type_wire, cardinality_wire;

    g_assert(dbus_message_iter_get_arg_type(prop_struct_iter) == DBUS_TYPE_BYTE);
    dbus_message_iter_get_basic(prop_struct_iter, &update_type_wire);
    dbus_message_iter_next(prop_struct_iter);

    g_assert(dbus_message_iter_get_arg_type(prop_struct_iter) == DBUS_TYPE_BYTE);
    dbus_message_iter_get_basic(prop_struct_iter, &data_type_wire);
    dbus_message_iter_next(prop_struct_iter);

    g_assert(dbus_message_iter_get_arg_type(prop_struct_iter) == DBUS_TYPE_BYTE);
    dbus_message_iter_get_basic(prop_struct_iter, &cardinality_wire);
    dbus_message_iter_next(prop_struct_iter);

    switch (update_type_wire) {
    case 'a':
        update_type = DDM_DATA_UPDATE_ADD;
        break;
    case 'r':
        update_type = DDM_DATA_UPDATE_REPLACE;
        break;
    case 'd':
        update_type = DDM_DATA_UPDATE_DELETE;
        break;
    case 'c':
        update_type = DDM_DATA_UPDATE_CLEAR;
        break;
    default:
        g_warning("Unknown update type %c", update_type_wire);
        return FALSE;   
    }

    /* We should get type LIST as a REPLACE of one value, then ADD, ADD, ADD of
     * multiple values, so LIST is never on the wire. Similarly, type NONE
     * comes through as a CLEAR. So in short we don't see values of NONE or LIST
     * on this end.
     */
    switch (data_type_wire) {
    case 's':
        data_type = DDM_DATA_STRING;
        break;
    case 'r':
        data_type = DDM_DATA_RESOURCE;
        break;
    case 'b':
        data_type = DDM_DATA_BOOLEAN;
        break;
    case 'i':
        data_type = DDM_DATA_INTEGER;
        break;
    case 'l':
        data_type = DDM_DATA_LONG;
        break;
    case 'f':
        data_type = DDM_DATA_FLOAT;
        break;
    case 'u':
        data_type = DDM_DATA_URL;
        break;
    default:
        g_warning("Unknown data type %c", data_type_wire);
        return FALSE;
    }

    switch (cardinality_wire) {
    case '.':
        cardinality = DDM_DATA_CARDINALITY_1;
        break;
    case '?':
        cardinality = DDM_DATA_CARDINALITY_01;
        break;
    case '*':
        cardinality = DDM_DATA_CARDINALITY_N;
        break;
    default:
        g_warning("Unknown data cardinality %c", cardinality_wire);
        return FALSE;
    }

    *update_type_p = update_type;
    *data_type_p = data_type;
    *cardinality_p = cardinality;

    return TRUE;
}

static gboolean
read_variant_value(DDMDataModel       *ddm_model,
                   DBusMessageIter    *variant_iter,
                   DDMDataType         data_type,
                   DDMDataValue       *value)
{
    switch (data_type) {
    case DDM_DATA_BOOLEAN:
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_BOOLEAN) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        {
            dbus_bool_t v_BOOLEAN;
            dbus_message_iter_get_basic(variant_iter, &v_BOOLEAN);
            value->u.boolean = v_BOOLEAN;
        }
        break;

    case DDM_DATA_INTEGER:
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_INT32) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        dbus_message_iter_get_basic(variant_iter, &value->u.integer);
        break;

    case DDM_DATA_LONG:
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_INT64) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        dbus_message_iter_get_basic(variant_iter, &value->u.long_);
        break;

    case DDM_DATA_FLOAT:
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_DOUBLE) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        dbus_message_iter_get_basic(variant_iter, &value->u.float_);
        break;

    case DDM_DATA_STRING:
    case DDM_DATA_URL:        
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_STRING) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        /* the memory management here is (intended to be) that we call
         * ddm_data_resource_update_property() before we free
         * the DBusMessage and it makes a copy.
         */
        dbus_message_iter_get_basic(variant_iter, &value->u.string);
        break;
        
    case DDM_DATA_RESOURCE:
        if (!dbus_message_iter_get_arg_type(variant_iter) == DBUS_TYPE_STRING) {
            g_warning("dbus type does not match expected data type");
            return FALSE;
        }
        {
            const char *resource_id;
            dbus_message_iter_get_basic(variant_iter, &resource_id);
            value->u.resource = ddm_data_model_lookup_resource(ddm_model, resource_id);
            if (value->u.resource == NULL) {
                g_warning("Unknown resource id '%s'", resource_id);
                return FALSE;
            }
        }
        break;

    default:
        g_warning("Don't know how to demarshal data type %d from dbus", data_type);
        return FALSE;
    }
    
    return TRUE;
}

static void
update_property(DBusModel          *dbus_model,
                DDMDataResource    *resource,
                DBusMessageIter    *prop_struct_iter,
                DDMNotificationSet *notifications)
{
    const char *param_namespace = NULL;
    const char *param_name = NULL;
    DDMQName *property_qname;
    DBusMessageIter variant_iter;
    DDMDataUpdate update_type;
    DDMDataType data_type = DDM_DATA_NONE;
    DDMDataCardinality cardinality = DDM_DATA_CARDINALITY_1;
    gboolean changed = FALSE;
    DDMDataValue value;
    
    /* "(ssyyyv)"
     *        Parameter ID namespace uri
     *        Parameter ID local name
     *        Update type ('a'=add, 'r'=replace, 'd'=delete, 'c'=clear)
     *        Data type ('s'=string, 'r'=resource)
     *        Cardinality ('.'=1, '?'=01, '*'=N)
     *        Value (variant)
     */
    
    g_assert(dbus_message_iter_get_arg_type(prop_struct_iter) == DBUS_TYPE_STRING);
    dbus_message_iter_get_basic(prop_struct_iter, &param_namespace);
    dbus_message_iter_next(prop_struct_iter);

    g_assert(dbus_message_iter_get_arg_type(prop_struct_iter) == DBUS_TYPE_STRING);
    dbus_message_iter_get_basic(prop_struct_iter, &param_name);
    dbus_message_iter_next(prop_struct_iter);

    if (!read_data_attributes(prop_struct_iter, &update_type, &data_type, &cardinality))
        return;

    dbus_message_iter_recurse(prop_struct_iter, &variant_iter);
    value.type = data_type;
    if (!read_variant_value(dbus_model->ddm_model, &variant_iter, data_type, &value))
        return;

    property_qname = ddm_qname_get(param_namespace, param_name);
    
    /* The defaultInclude and defaultChildren attributes of a property
     * are not included in the D-Bus protocol because we only need
     * them when interpreting a fetch string. Here in a client
     * application, we just set them to FALSE and NULL respectively.
     */
    
    if (update_type == DDM_DATA_UPDATE_CLEAR) {
        changed = ddm_data_resource_update_property(resource, property_qname, update_type, cardinality,
                                                    FALSE, NULL, NULL);
    } else {
        changed = ddm_data_resource_update_property(resource, property_qname, update_type, cardinality,
                                                    FALSE, NULL, &value);
    }

    if (changed) {
        if (notifications)
            ddm_notification_set_add(notifications, resource, property_qname);
    }
}
     
static DDMDataResource*
handle_incoming_resource_update(DBusModel          *dbus_model,
                                DBusMessageIter    *resource_struct_iter,
                                DDMNotificationSet *notifications)
{
    DDMDataResource *resource;    
    const char *resource_id = NULL;
    const char *resource_class_id = NULL;
    dbus_bool_t indirect = FALSE;
    DBusMessageIter prop_array_iter;
    
    g_assert(dbus_message_iter_get_arg_type(resource_struct_iter) == DBUS_TYPE_STRING);
    dbus_message_iter_get_basic(resource_struct_iter, &resource_id);
    dbus_message_iter_next(resource_struct_iter);
    
    g_assert(dbus_message_iter_get_arg_type(resource_struct_iter) == DBUS_TYPE_STRING);
    dbus_message_iter_get_basic(resource_struct_iter, &resource_class_id);
    dbus_message_iter_next(resource_struct_iter);

    g_assert(dbus_message_iter_get_arg_type(resource_struct_iter) == DBUS_TYPE_BOOLEAN);
    dbus_message_iter_get_basic(resource_struct_iter, &indirect);
    dbus_message_iter_next(resource_struct_iter);

    resource = ddm_data_model_ensure_resource(dbus_model->ddm_model,
                                              resource_id,
                                              resource_class_id);

    /* hippo-connection.c does not bother with notifications if indirect=TRUE,
     * should we do the same?
     */

    dbus_message_iter_recurse(resource_struct_iter, &prop_array_iter);
    while (dbus_message_iter_get_arg_type(&prop_array_iter) != DBUS_TYPE_INVALID) {
        DBusMessageIter prop_struct_iter;

        dbus_message_iter_recurse(&prop_array_iter, &prop_struct_iter);

        update_property(dbus_model, resource, &prop_struct_iter, notifications);
        
        dbus_message_iter_next(&prop_array_iter);
    }
    
    return resource;
}

/* query_to_respond_to is NULL for a notification */
static void
handle_incoming_resource_updates(DBusModel          *dbus_model,
                                 DBusMessageIter    *resource_array_iter_orig,
                                 DDMDataQuery       *query_to_respond_to)
{
    DBusMessageIter resource_array_iter, resource_struct_iter;
    GSList *resources;
    DDMNotificationSet *notifications;    
    
    /*
     *   "a(ssba(ssyyyv))"
     *   Array of resources: 
     *     Resource ID
     *     Resource class ID
     *     Indirect? (boolean)
     *     Array of parameters:
     *        Parameter ID namespace uri
     *        Parameter ID local name
     *        Update type ('a'=add, 'r'=replace, 'd'=delete, 'c'=clear)
     *        Data type ('s'=string, 'r'=resource)
     *        Cardinality ('.'=1, '?'=01, '*'=N)
     *        Value (variant)
     */

    notifications = ddm_notification_set_new(dbus_model->ddm_model);
    
    resources = NULL;
    resource_array_iter = *resource_array_iter_orig;
    
    while (dbus_message_iter_get_arg_type(&resource_array_iter) != DBUS_TYPE_INVALID) {
        DDMDataResource *resource;
        
        dbus_message_iter_recurse(&resource_array_iter, &resource_struct_iter);
        
        resource = handle_incoming_resource_update(dbus_model, &resource_struct_iter, notifications);

        if (query_to_respond_to)
            resources = g_slist_prepend(resources, resource);

        dbus_message_iter_next(&resource_array_iter);
    }
    
    if (query_to_respond_to) {

        g_debug("Query had %d resources in reply", g_slist_length(resources));
        
        resources = g_slist_reverse(resources);
        ddm_data_query_response(query_to_respond_to, resources);
        g_slist_free(resources);
    } else {
        g_assert(resources == NULL);
    }

    /* hippo-connection.c does not send notifications for a query response,
     * so maybe we should move this into the !query_to_respond_to case
     * above?
     */
    ddm_notification_set_send(notifications);
    ddm_notification_set_free(notifications);
}

static DBusMessage*
handle_notify (void            *object,
               DBusMessage     *message,
               DBusError       *error)
{
    DBusModel *dbus_model = object;    
    DBusMessageIter resource_array_iter;
    
    dbus_message_iter_init(message, &resource_array_iter);

    handle_incoming_resource_updates(dbus_model, &resource_array_iter, NULL);
    
    return dbus_message_new_method_return(message);
}

static const HippoDBusMember model_client_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "Notify", "a(ssba(ssyyyv))", "", handle_notify },

    { 0, NULL }
};

static const HippoDBusProperty model_client_properties[] = {
    { NULL }
};

static void
handle_get_connected_reply(DBusMessage *reply,
                           void        *data)
{
    DBusModel *dbus_model = data;
    DBusMessageIter variant_iter;
    DBusMessageIter toplevel_iter;
    dbus_bool_t connected;

    if (dbus_model->ddm_model == NULL) /* happens if the reply comes in after we nuke the model */
        return;
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        return;        
    }

    if (!dbus_message_has_signature(reply, "v")) {
        g_warning("wrong signature on reply to Properties.Get");
        return;
    }
    
    dbus_message_iter_init(reply, &toplevel_iter);
    dbus_message_iter_recurse(&toplevel_iter, &variant_iter);

    if (dbus_message_iter_get_arg_type(&variant_iter) != DBUS_TYPE_BOOLEAN) {
        g_warning("Expecting Connected prop to have type boolean");
        return;
    }

    connected = FALSE;
    dbus_message_iter_get_basic(&variant_iter, &connected);

    /* protect against a race / duplicate effort */
    if (dbus_model->connection == NULL ||
        !dbus_connection_get_is_connected(dbus_model->connection))
        connected = FALSE;
    
    ddm_data_model_set_connected(dbus_model->ddm_model, connected);
    if (connected)
        model_send_pending(dbus_model);
}

static void
handle_engine_available(DBusConnection *connection,
                        const char     *well_known_name,
                        const char     *unique_name,
                        void           *data)
{
    DBusModel *dbus_model = data;
    const char *iface;
    const char *method;

    g_debug("org.freedesktop.od.Engine available");
    
    dbus_model->engine_proxy = hippo_dbus_proxy_new(dbus_model->connection,
                                                    unique_name,
                                                    "/org/freedesktop/od/data_model",
                                                    "org.freedesktop.od.Model");
    
    dbus_model->engine_props_proxy = hippo_dbus_proxy_new(dbus_model->connection,
                                                          unique_name,
                                                          "/org/freedesktop/od/data_model",
                                                          DBUS_INTERFACE_PROPERTIES);

    iface = "org.freedesktop.od.Model";
    method = "Connected";
    model_ref(dbus_model);
    hippo_dbus_proxy_call_method_async(dbus_model->engine_props_proxy,
                                       "Get",
                                       handle_get_connected_reply,
                                       dbus_model,
                                       (GFreeFunc) model_unref,
                                       DBUS_TYPE_STRING,
                                       &iface,
                                       DBUS_TYPE_STRING,
                                       &method,
                                       DBUS_TYPE_INVALID);
}

static void
handle_engine_unavailable(DBusConnection *connection,
                          const char     *well_known_name,
                          const char     *unique_name,
                          void           *data)
{
    DBusModel *dbus_model = data;

    g_debug("org.freedesktop.od.Engine went away");
    
    g_assert(dbus_model->engine_props_proxy != NULL);
    
    hippo_dbus_proxy_unref(dbus_model->engine_props_proxy);
    dbus_model->engine_props_proxy = NULL;

    hippo_dbus_proxy_unref(dbus_model->engine_proxy);
    dbus_model->engine_proxy = NULL;
    
    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);
}

static HippoDBusServiceTracker engine_tracker = {
    HIPPO_DBUS_SERVICE_START_IF_NOT_RUNNING,
    handle_engine_available,
    handle_engine_unavailable
};

static void
handle_connected_changed(DBusConnection *connection,
                         DBusMessage    *message,
                         void           *data)
{
    DBusModel *dbus_model = data;
    dbus_bool_t is_connected;
    const char *self_id;

    is_connected = FALSE;
    self_id = NULL;

    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_BOOLEAN, &is_connected,
                               DBUS_TYPE_STRING, &self_id,
                               DBUS_TYPE_INVALID)) {
        g_warning("bad args to ConnectedChanged signal");
        return;
    }

    ddm_data_model_set_connected(dbus_model->ddm_model, is_connected);
    if (is_connected)
        model_send_pending(dbus_model);
}

static HippoDBusSignalTracker engine_signal_handlers[] = {
    { "org.freedesktop.od.Model", "ConnectedChanged",
      handle_connected_changed },
    { NULL, NULL, NULL }
};

static char*
generate_path(void)
{
    static int counter = 0;

    counter += 1;

    return g_strdup_printf("/org/freedesktop/od/ddm/client/%d", counter);
}

static void
handle_session_bus_connected(DBusConnection *connection,
                             void           *data)
{
    DBusModel *dbus_model = data;

    g_debug("Connected to session bus");
    
    g_assert(dbus_model->connection == NULL);
    
    dbus_model->connection = connection;
    dbus_connection_ref(dbus_model->connection);
    
    /* dbus helper ignores multiple interface registrations */
    hippo_dbus_helper_register_interface(connection, "org.freedesktop.od.ModelClient",
                                         model_client_members, model_client_properties);

    dbus_model->path = generate_path();
    
    hippo_dbus_helper_register_object(connection, dbus_model->path,
                                      dbus_model, "org.freedesktop.od.ModelClient",
                                      NULL);
    
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.od.Engine",
                                               &engine_tracker,
                                               engine_signal_handlers,
                                               dbus_model);                                   
}

static void
handle_session_bus_disconnected(DBusConnection *connection,
                                void           *data)
{
    DBusModel *dbus_model = data;

    g_debug("Disconnected from session bus");
    
    g_assert(dbus_model->connection != NULL);
    g_assert(dbus_model->connection == connection);

    hippo_dbus_helper_unregister_service_tracker(connection,
                                                 "org.freedesktop.od.Engine",
                                                 &engine_tracker,
                                                 dbus_model);

    hippo_dbus_helper_unregister_object(connection, dbus_model->path);

    g_free(dbus_model->path);
    dbus_model->path = NULL;
    
    /* there's no interface unregistration right now */
    
    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);
    
    dbus_connection_unref(dbus_model->connection);
    dbus_model->connection = NULL;
}

static HippoDBusConnectionTracker connection_tracker = {
    handle_session_bus_connected,
    handle_session_bus_disconnected
};

static void
ddm_dbus_add_model (DDMDataModel *ddm_model,
                    void         *backend_data)
{
    DBusModel *dbus_model;
    
    dbus_model = g_new0(DBusModel, 1);
    dbus_model->refcount = 1;
    dbus_model->ddm_model = ddm_model;
    
    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", dbus_model);

    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);

    hippo_dbus_helper_register_connection_tracker(DBUS_BUS_SESSION,
                                                  &connection_tracker,
                                                  dbus_model);
}

static void
ddm_dbus_remove_model (DDMDataModel *ddm_model,
                       void         *backend_data)
{
    DBusModel *dbus_model;

    dbus_model = get_dbus_model(ddm_model);
    
    hippo_dbus_helper_unregister_connection_tracker(DBUS_BUS_SESSION,
                                                    &connection_tracker,
                                                    dbus_model);

    g_object_set_data(G_OBJECT(ddm_model), "dbus-data-model", NULL);

    g_assert(dbus_model->connection == NULL);
    g_assert(dbus_model->path == NULL);

    dbus_model->ddm_model = NULL; /* indicates we are removed */
    
    model_unref(dbus_model);
}

static void
append_param(void *key,
             void *value,
             void *data)
{
    DBusMessageIter *params_dict_iter = data;
    DBusMessageIter param_entry_iter;
    
    /* out-of-memory is ignored in here */
    
    dbus_message_iter_open_container(params_dict_iter, DBUS_TYPE_DICT_ENTRY,
                                     NULL, &param_entry_iter);
    
    dbus_message_iter_append_basic(&param_entry_iter, DBUS_TYPE_STRING, &key);
    dbus_message_iter_append_basic(&param_entry_iter, DBUS_TYPE_STRING, &value);    
    
    dbus_message_iter_close_container(params_dict_iter, &param_entry_iter);
}

static dbus_bool_t
append_params(DBusMessageIter *params_dict_iter,
              GHashTable      *params)
{
    g_hash_table_foreach(params, append_param, params_dict_iter);

    return TRUE;
}

typedef struct {
    DBusModel *dbus_model;
    DDMDataQuery *query;
} QueryData;

static void
free_query_data(void *data)
{
    QueryData *qd = data;

    model_unref(qd->dbus_model);
    g_free(qd);
}

static void
handle_query_reply(DBusMessage *reply,
                   void        *data)
{
    QueryData *qd = data;
    DBusMessageIter toplevel_iter;
    DBusMessageIter resource_array_iter;

    if (qd->dbus_model->ddm_model == NULL) /* happens if the reply comes in after we nuke the model */
        return;
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        /* the dbus API does not give us the error code right now */
        const char *message = NULL;
        dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &message, DBUS_TYPE_INVALID);
        ddm_data_query_error(qd->query,
                             DDM_DATA_ERROR_INTERNAL_SERVER_ERROR, /* arbitrary */
                             message ? message : "unknown error");
        return;
    }

    if (!dbus_message_has_signature(reply, "a(ssba(ssyyyv))")) {
        ddm_data_query_error(qd->query,
                             DDM_DATA_ERROR_BAD_REPLY,
                             "Received bad reply from desktop engine");
        return;
    }
    
    dbus_message_iter_init(reply, &toplevel_iter);
    dbus_message_iter_recurse(&toplevel_iter, &resource_array_iter);

    handle_incoming_resource_updates(qd->dbus_model, &resource_array_iter, qd->query);
}

static dbus_bool_t
append_query_args(DBusMessage *message,
                  void        *data)
{
    QueryData *qd = data;
    DBusMessageIter toplevel_iter, params_dict_iter;
    char *s;
    const char *cs;
    
    dbus_message_iter_init_append(message, &toplevel_iter);

    /* notification path */
    if (!dbus_message_iter_append_basic(&toplevel_iter,
                                        DBUS_TYPE_OBJECT_PATH, &qd->dbus_model->path))
        return FALSE;

    /* method uri */
    s = ddm_qname_to_uri(ddm_data_query_get_qname(qd->query));
    if (!dbus_message_iter_append_basic(&toplevel_iter,
                                        DBUS_TYPE_STRING, &s)) {
        g_free(s);
        return FALSE;
    }
    g_free(s);

    /* fetch string */
    cs = ddm_data_query_get_fetch(qd->query);
    if (!dbus_message_iter_append_basic(&toplevel_iter,
                                        DBUS_TYPE_STRING, &cs))
        return FALSE;

    /* dictionary of params, string:string */

    if (!dbus_message_iter_open_container(&toplevel_iter, DBUS_TYPE_ARRAY,
                                          "{ss}", &params_dict_iter))
        return FALSE;

    if (!append_params(&params_dict_iter, ddm_data_query_get_params(qd->query)))
        return FALSE;
    
    if (!dbus_message_iter_close_container(&toplevel_iter, &params_dict_iter))
        return FALSE;

    return TRUE;
}

static void
ddm_dbus_send_query   (DDMDataModel *ddm_model,
                       DDMDataQuery *query,
                       void         *backend_data)
{
    DBusModel *dbus_model;
    QueryData *qd;

    dbus_model = get_dbus_model(ddm_model);

    if (dbus_model->engine_proxy == NULL) {
        model_add_pending_query(dbus_model, query);
        return;
    }

    g_debug("sending Query to org.freedesktop.od.Engine");
    
    g_assert(dbus_model->engine_proxy != NULL); /* since connection != NULL */

    qd = g_new(QueryData, 1);

    model_ref(dbus_model);
    qd->dbus_model = dbus_model;
    qd->query = query;
    
    hippo_dbus_proxy_call_method_async_appender(dbus_model->engine_proxy,
                                                "Query",
                                                handle_query_reply,
                                                qd,
                                                free_query_data,
                                                append_query_args,
                                                qd);
}


static void
handle_update_reply(DBusMessage *reply,
                    void        *data)
{
    QueryData *qd = data;

    if (qd->dbus_model->ddm_model == NULL) /* happens if the reply comes in after we nuke the model */
        return;
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        /* the dbus API does not give us the error code right now */
        const char *message = NULL;
        dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &message, DBUS_TYPE_INVALID);
        ddm_data_query_error(qd->query,
                             DDM_DATA_ERROR_INTERNAL_SERVER_ERROR, /* arbitrary */
                             message ? message : "unknown error");
        return;
    }

    /* a successful "ack" reply */
    ddm_data_query_response(qd->query, NULL);
}

static dbus_bool_t
append_update_args(DBusMessage *message,
                   void        *data)
{
    QueryData *qd = data;
    DBusMessageIter toplevel_iter, params_dict_iter;
    char *s;
    
    dbus_message_iter_init_append(message, &toplevel_iter);

    /* method uri */
    s = ddm_qname_to_uri(ddm_data_query_get_qname(qd->query));
    if (!dbus_message_iter_append_basic(&toplevel_iter,
                                        DBUS_TYPE_STRING, &s)) {
        g_free(s);
        return FALSE;
    }
    g_free(s);

    /* dictionary of params, string:string */

    if (!dbus_message_iter_open_container(&toplevel_iter, DBUS_TYPE_ARRAY,
                                          "{ss}", &params_dict_iter))
        return FALSE;

    if (!append_params(&params_dict_iter, ddm_data_query_get_params(qd->query)))
        return FALSE;
    
    if (!dbus_message_iter_close_container(&toplevel_iter, &params_dict_iter))
        return FALSE;

    return TRUE;
}

static void
ddm_dbus_send_update (DDMDataModel *ddm_model,
                      DDMDataQuery *query,
                      const char   *method,
                      GHashTable   *params,
                      void         *backend_data)
{
    DBusModel *dbus_model;
    QueryData *qd;

    dbus_model = get_dbus_model(ddm_model);

    if (dbus_model->engine_proxy == NULL) {
        model_add_pending_update(dbus_model, query, method, params);
        return;
    }

    g_debug("sending Update to org.freedesktop.od.Engine");
    
    g_assert(dbus_model->engine_proxy != NULL); /* since connection != NULL */

    qd = g_new(QueryData, 1);

    model_ref(dbus_model);
    qd->dbus_model = dbus_model;
    qd->query = query;
    
    hippo_dbus_proxy_call_method_async_appender(dbus_model->engine_proxy,
                                                "Update",
                                                handle_update_reply,
                                                qd,
                                                free_query_data,
                                                append_update_args,
                                                qd);
}

static const DDMDataModelBackend dbus_backend = {
    ddm_dbus_add_model,
    ddm_dbus_remove_model,
    ddm_dbus_send_query,
    ddm_dbus_send_update,
    NULL,
};

const DDMDataModelBackend*
ddm_data_model_get_dbus_backend(void)
{
    return &dbus_backend;
}

/* This is the default model for platforms that have dbus only.
 * Other platforms will have to do something else.
 */

static DDMDataModel *default_model = NULL;

DDMDataModel*
ddm_data_model_get_default (void)
{
    if (default_model == NULL) {
        default_model = ddm_data_model_new_with_backend(ddm_data_model_get_dbus_backend(),
                                                        NULL, NULL);
    }

    g_object_ref(default_model);
    return default_model;
}
