/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include <config.h>

#include <stdarg.h>
#include <string.h>

#include "ddm-data-model-dbus.h"
#include "ddm-data-model-backend.h"
#include "ddm-data-query.h"
#include "hippo-dbus-helper.h"

#include <dbus/dbus.h>

typedef struct {
    int             refcount;
    char           *path;
    DDMDataModel   *ddm_model;
    DBusConnection *connection;
    HippoDBusProxy *engine_proxy;
} DBusModel;

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

/* query_to_respond_to is NULL for a notification */
static void
handle_incoming_resource_updates(DBusModel       *dbus_model,
                                 DBusMessageIter *resource_array_iter,
                                 DDMDataQuery    *query_to_respond_to)
{
    /*
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

    
    /* FIXME */
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
    
    return NULL;
}

static const HippoDBusMember model_client_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "Notify", "a(ssba(ssyyyv))", "", handle_notify },

    { 0, NULL }
};

static const HippoDBusProperty model_client_properties[] = {
    { NULL }
};

static void
handle_engine_available(DBusConnection *connection,
                        const char     *well_known_name,
                        const char     *unique_name,
                        void           *data)
{
    DBusModel *dbus_model = data;

    
    
    /* FIXME get initial connected state */
}

static void
handle_engine_unavailable(DBusConnection *connection,
                          const char     *well_known_name,
                          const char     *unique_name,
                          void           *data)
{
    DBusModel *dbus_model = data;

    ddm_data_model_set_connected(dbus_model->ddm_model, FALSE);
}

static HippoDBusServiceTracker engine_tracker = {
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

    dbus_model->engine_proxy = hippo_dbus_proxy_new(dbus_model->connection,
                                                    "org.freedesktop.od.Engine",
                                                    "/org/freedesktop/od/data_model",
                                                    "org.freedesktop.od.Model");
}

static void
handle_session_bus_disconnected(DBusConnection *connection,
                                void           *data)
{
    DBusModel *dbus_model = data;

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

    hippo_dbus_proxy_unref(dbus_model->engine_proxy);
    dbus_model->engine_proxy = NULL;
    
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
    DBusMessageIter resource_array_iter;
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        /* the dbus API does not give us the error code right now */
        const char *message = NULL;
        dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &message, DBUS_TYPE_INVALID);
        ddm_data_query_error(qd->query,
                             DDM_DATA_ERROR_INTERNAL_SERVER_ERROR, /* arbitrary */
                             message ? message : "unknown error");
        return;
    }
    
    dbus_message_iter_init(reply, &resource_array_iter);

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
                                        DBUS_TYPE_STRING, &qd->dbus_model->path))
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

    if (dbus_model->connection == NULL) {
        ddm_data_query_error(query,
                             DDM_DATA_ERROR_NO_CONNECTION,
                             "Not connected to message bus");
        return;                             
    }

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

    if (dbus_model->connection == NULL) {
        ddm_data_query_error(query,
                             DDM_DATA_ERROR_NO_CONNECTION,
                             "Not connected to message bus");
        return;                             
    }

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
