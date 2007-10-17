/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include <hippo/hippo-settings.h>
#include "hippo-dbus-server.h"
#include "hippo-dbus-web.h"
#include "hippo-dbus-settings.h"
#include "hippo-dbus-helper.h"
#include "main.h"

/* This is its own file since it could end up being a fair bit of code eventually
 */

static void
on_ready_changed(HippoSettings *settings,
                 gboolean       ready,
                 void          *data)
{
    DBusConnection *dbus_connection = data;

    g_debug("emitting ReadyChanged ready=%d", ready);

    hippo_dbus_helper_emit_signal(dbus_connection,
                                  HIPPO_DBUS_ONLINE_PREFS_PATH,
                                  HIPPO_DBUS_PREFS_INTERFACE,
                                  "ReadyChanged",
                                  DBUS_TYPE_BOOLEAN, &ready,
                                  DBUS_TYPE_INVALID);
}

static void
on_setting_changed(HippoSettings *settings,
                   const char    *key,
                   void          *data)
{
    DBusConnection *dbus_connection = data;

    g_debug("emitting PreferenceChanged key=%s", key);

    hippo_dbus_helper_emit_signal(dbus_connection,
                                  HIPPO_DBUS_ONLINE_PREFS_PATH,
                                  HIPPO_DBUS_PREFS_INTERFACE,
                                  "PreferenceChanged",
                                  DBUS_TYPE_STRING, &key,
                                  DBUS_TYPE_INVALID);
}

static HippoSettings*
get_and_ref_settings(DBusConnection *dbus_connection)
{
    HippoDataCache *cache;
    HippoConnection *connection;
    HippoSettings *settings;
    
    cache = hippo_app_get_data_cache(hippo_get_app());
    connection = hippo_data_cache_get_connection(cache);

    settings = hippo_settings_get_and_ref(connection);

    if (!g_object_get_data(G_OBJECT(settings), "dbus-connected")) {
        g_debug("connecting to ready-changed on HippoSettings");
        g_object_set_data(G_OBJECT(settings), "dbus-connected", GINT_TO_POINTER(TRUE));

        /* these refs are never dropped at the moment but in practice the dbus connection
         * should never be disconnected so it's fine */
        
        dbus_connection_ref(dbus_connection);
        g_signal_connect_data(G_OBJECT(settings), "ready-changed", G_CALLBACK(on_ready_changed),
                              dbus_connection, (GClosureNotify) dbus_connection_unref, 0);
        dbus_connection_ref(dbus_connection);
        g_signal_connect_data(G_OBJECT(settings), "setting-changed", G_CALLBACK(on_setting_changed),
                              dbus_connection, (GClosureNotify) dbus_connection_unref, 0);        
    }
    
    return settings;
}

static DBusMessage*
handle_get_all_preference_names(void            *object,
                                DBusMessage     *message,
                                DBusError       *error)
{
    HippoSettings *settings;
    DBusConnection *dbus_connection;
    DBusMessageIter iter, array_iter;
    char **names;
    int i;
    DBusMessage *reply;
    
    dbus_connection = object;
    settings = get_and_ref_settings(dbus_connection);

    if (!hippo_settings_get_ready(settings)) {
        g_object_unref(G_OBJECT(settings));
        return dbus_message_new_error(message, HIPPO_DBUS_PREFS_ERROR_NOT_READY,
                                      _("Have not yet connected to server, can't get preference names"));
    }

    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "s",
                                     &array_iter);

    names = hippo_settings_get_all_names(settings);
    for (i = 0; names[i] != NULL; ++i) {
        dbus_message_iter_append_basic(&array_iter, DBUS_TYPE_STRING, &names[i]);
    }
    g_strfreev(names);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    g_object_unref(settings);
    
    return reply;

}

typedef struct SettingArrivedData SettingArrivedData;
struct SettingArrivedData {
    DBusMessage *method_call;
    const char *signature; /* points inside method_call */
    HippoSettings *settings;
    DBusConnection *connection;
};

static void
setting_arrived(const char *key,
                const char *value,
                void       *data)
{
    SettingArrivedData *sad = data;
    DBusMessage *reply;
    DBusMessageIter iter;
    DBusMessageIter variant_iter;
    
    if (value == NULL) {
        reply = dbus_message_new_error_printf(sad->method_call, HIPPO_DBUS_PREFS_ERROR_NOT_FOUND,                                      
                                              _("No value known for key '%s'"), key);
        goto out;
    }
    
    reply = dbus_message_new_method_return(sad->method_call);

    dbus_message_iter_init_append(reply, &iter);
    
    dbus_message_iter_open_container(&iter, DBUS_TYPE_VARIANT, sad->signature,
                                     &variant_iter);


    switch (*(sad->signature)) {
    case DBUS_TYPE_STRING:
        dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_STRING, &value);
        break;
    case DBUS_TYPE_INT32:
        {
            dbus_int32_t v_INT32;

            if (!hippo_parse_int32(value, &v_INT32)) {
                dbus_message_unref(reply);
                reply = dbus_message_new_error_printf(sad->method_call, HIPPO_DBUS_PREFS_ERROR_WRONG_TYPE,  
                                                      _("Value was '%s' not parseable as an INT32"), value);
                goto out;
            }
            
            dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_INT32, &v_INT32);
        }
        break;
    case DBUS_TYPE_BOOLEAN:
        {
            dbus_bool_t v_BOOLEAN;

            if (strcmp(value, "true") == 0)
                v_BOOLEAN = TRUE;
            else if (strcmp(value, "false") == 0)
                v_BOOLEAN = FALSE;
            else {
                dbus_message_unref(reply);
                reply = dbus_message_new_error_printf(sad->method_call, HIPPO_DBUS_PREFS_ERROR_WRONG_TYPE,  
                                                      _("Value was '%s' not parseable as a BOOLEAN"), value);
                goto out;
            }
            
            dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_BOOLEAN, &v_BOOLEAN);
        }
        break;
    case DBUS_TYPE_DOUBLE:
        {
            double v_DOUBLE;

            if (!hippo_parse_double(value, &v_DOUBLE)) {
                dbus_message_unref(reply);
                reply = dbus_message_new_error_printf(sad->method_call, HIPPO_DBUS_PREFS_ERROR_WRONG_TYPE,  
                                                      _("Value was '%s' not parseable as a double"), value);
                goto out;
            }
            
            dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_DOUBLE, &v_DOUBLE);
        }
        break;        
    }

    dbus_message_iter_close_container(&iter, &variant_iter);

 out:
    dbus_connection_send(sad->connection, reply, NULL);
    dbus_message_unref(reply);
    
    dbus_message_unref(sad->method_call);
    g_object_unref(sad->settings);
    dbus_connection_unref(sad->connection);
    g_free(sad);
}

static DBusMessage*
handle_get_preference(void            *object,
                      DBusMessage     *message,
                      DBusError       *error)
{
    const char *key;
    const char *signature;
    HippoSettings *settings;
    SettingArrivedData *sad;
    DBusConnection *dbus_connection;

    dbus_connection = object;
    
    key = NULL;
    signature = NULL;
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_STRING, &key,
                               DBUS_TYPE_SIGNATURE, &signature,
                               DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Expected two arguments, the key and the expected type signature"));
    }

    if (!dbus_signature_validate_single(signature, NULL)) {
        return dbus_message_new_error(message, DBUS_ERROR_INVALID_ARGS,
                                      _("Type signature must be a single complete type, not a list of types"));
    }

    if ( ! (*signature == DBUS_TYPE_INT32 ||
            *signature == DBUS_TYPE_STRING ||
            *signature == DBUS_TYPE_BOOLEAN ||
            *signature == DBUS_TYPE_DOUBLE) ) {
        return dbus_message_new_error(message, DBUS_ERROR_INVALID_ARGS,
                                      _("Only STRING, INT32, BOOLEAN, DOUBLE values supported for now"));
    }
    
    settings = get_and_ref_settings(dbus_connection);

    if (!hippo_settings_get_ready(settings)) {
        g_object_unref(G_OBJECT(settings));
        return dbus_message_new_error(message, HIPPO_DBUS_PREFS_ERROR_NOT_READY,
                                      _("Have not yet connected to server, can't get preferences"));
    }

    sad = g_new0(SettingArrivedData, 1);
    sad->connection = dbus_connection;
    dbus_connection_ref(sad->connection);
    
    sad->settings = settings;

    sad->method_call = message;
    dbus_message_ref(sad->method_call);
    sad->signature = signature; /* points inside sad->method_call */

    
    /* this may call setting_arrived synchronously if we already have it */
    hippo_settings_get(settings, key, setting_arrived, sad);    
    
    return NULL; /* no synchronous reply, we'll send it async or we just sent it above */
}

static DBusMessage*
handle_set_preference(void            *object,
                      DBusMessage     *message,
                      DBusError       *error)
{
    DBusMessage *reply;
    HippoSettings *settings;
    DBusMessageIter iter;
    DBusMessageIter variant_iter;
    const char *key;
    int value_type;
    char *value;
    DBusConnection *dbus_connection;
    
    dbus_connection = object;    
    
    dbus_message_iter_init(message, &iter);

    key = NULL;
    dbus_message_iter_get_basic(&iter, &key);

    dbus_message_iter_next(&iter);

    dbus_message_iter_recurse(&iter, &variant_iter);

    value_type = dbus_message_iter_get_arg_type(&variant_iter);

    value = NULL;
    switch (value_type) {
    case DBUS_TYPE_STRING:
        {
            const char *v_STRING;
            dbus_message_iter_get_basic(&variant_iter, &v_STRING);
            value = g_strdup(v_STRING);
        }
        break;
    case DBUS_TYPE_INT32:
        {
            dbus_int32_t v_INT32;
            dbus_message_iter_get_basic(&variant_iter, &v_INT32);
            value = g_strdup_printf("%d", v_INT32);
        }
        break;
    case DBUS_TYPE_BOOLEAN:
        {
            dbus_bool_t v_BOOLEAN;
            dbus_message_iter_get_basic(&variant_iter, &v_BOOLEAN);
            value = g_strdup_printf("%s", v_BOOLEAN ? "true" : "false");
        }
        break;
    case DBUS_TYPE_DOUBLE:
        {
            double v_DOUBLE;
            char buf[G_ASCII_DTOSTR_BUF_SIZE];
            dbus_message_iter_get_basic(&variant_iter, &v_DOUBLE);
            g_ascii_dtostr(buf, G_ASCII_DTOSTR_BUF_SIZE, v_DOUBLE);
            value = g_strdup(buf);
        }
        break;
    default:
        g_debug("dbus value type we don't know how to handle");
        return dbus_message_new_error_printf(message,
                                             DBUS_ERROR_INVALID_ARGS,
                                             _("Unable to handle values of type '%c' right now"), value_type);
        break;
    }

    settings = get_and_ref_settings(dbus_connection);
    
    hippo_settings_set(settings, key, value);

    g_free(value);
    g_object_unref(settings);
    
    /* Just an empty "ack" reply */
    reply = dbus_message_new_method_return(message);

    return reply;
}

static DBusMessage*
handle_unset_preference(void            *object,
                        DBusMessage     *message,
                        DBusError       *error)
{
    DBusMessage *reply;
    HippoSettings *settings;
    const char *key;
    DBusConnection *dbus_connection;

    dbus_connection = object;

    if (!dbus_message_get_args(message, error,
                               DBUS_TYPE_STRING, &key,
                               DBUS_TYPE_INVALID))
        return NULL;

    settings = get_and_ref_settings(dbus_connection);    
    
    hippo_settings_set(settings, key, NULL);

    g_object_unref(settings);
    
    /* Just an empty "ack" reply */
    reply = dbus_message_new_method_return(message);

    return reply;
}

static DBusMessage*
handle_is_ready(void            *object,
                DBusMessage     *message,
                DBusError       *error)
{
    HippoSettings *settings;
    dbus_bool_t v_BOOLEAN;
    DBusMessage *reply;
    DBusConnection *dbus_connection;

    dbus_connection = object;
    
    g_debug("handling IsReady()");    
    
    settings = get_and_ref_settings(dbus_connection);
    v_BOOLEAN = hippo_settings_get_ready(settings);
    g_object_unref(settings);
    
    reply = dbus_message_new_method_return(message);
    dbus_message_append_args(reply, DBUS_TYPE_BOOLEAN, &v_BOOLEAN, DBUS_TYPE_INVALID);
    
    return reply;
}

static dbus_bool_t
handle_get_ready(void            *object,
                 const char      *prop_name,
                 DBusMessageIter *append_iter,
                 DBusError       *error)
{
    HippoSettings *settings;
    dbus_bool_t v_BOOLEAN;
    DBusConnection *dbus_connection;

    dbus_connection = object;
    
    g_debug("handling GetProperty 'ready'");
    
    settings = get_and_ref_settings(dbus_connection);
    v_BOOLEAN = hippo_settings_get_ready(settings);
    g_object_unref(settings);
    
    return dbus_message_iter_append_basic(append_iter, DBUS_TYPE_BOOLEAN, &v_BOOLEAN);
}


/*
 * Lame summary of the org.freedesktop.Preferences interface
 *
 * ARRAY of STRING GetAllPreferenceNames() throws NotReady
 * VARIANT GetPreference(STRING key, SIGNATURE expectedType) throws NotFound, WrongType, NotReady
 * void SetPreference(STRING key, VARIANT v) # just queues up if offline
 * void UnsetPreference(STRING key)
 * BOOLEAN IsReady()
 * signal ReadyChanged(BOOLEAN status)
 * signal PreferenceChanged(STRING key)
 *
 */

static const HippoDBusMember prefs_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetAllPreferenceNames", "", "as", handle_get_all_preference_names },    
    { HIPPO_DBUS_MEMBER_METHOD, "GetPreference", "sg", "v", handle_get_preference },
    { HIPPO_DBUS_MEMBER_METHOD, "SetPreference", "sv", "", handle_set_preference },
    { HIPPO_DBUS_MEMBER_METHOD, "UnsetPreference", "s", "", handle_unset_preference },
    /* deprecated, use the Ready property */
    { HIPPO_DBUS_MEMBER_METHOD, "IsReady", "", "b", handle_is_ready },

    /* FIXME the signal params should be "in" params */
    { HIPPO_DBUS_MEMBER_SIGNAL, "ReadyChanged", "", "b", NULL },
    /* FIXME the signal params should be "in" params */
    { HIPPO_DBUS_MEMBER_SIGNAL, "PreferenceChanged", "", "s", NULL },

    { 0, NULL }
};

static const HippoDBusProperty prefs_properties[] = {
    { "ready", DBUS_TYPE_STRING_AS_STRING, handle_get_ready, NULL },
    { NULL, }
};

void
hippo_dbus_try_acquire_online_prefs_manager(DBusConnection *connection,
                                            gboolean        replace)
{
    dbus_uint32_t flags;

    /* We do want to be queued if we don't get this right away */
    flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;

    /* we just ignore errors on this */
    dbus_bus_request_name(connection, HIPPO_DBUS_ONLINE_PREFS_BUS_NAME,
                          flags,
                          NULL);

    /* note that calling get_and_ref_settings() in here is bad, because HippoApp has
     * not been created
     */

    hippo_dbus_helper_register_interface(connection,
                                         HIPPO_DBUS_PREFS_INTERFACE,
                                         prefs_members,
                                         prefs_properties);

    hippo_dbus_helper_register_object(connection,
                                      HIPPO_DBUS_ONLINE_PREFS_PATH,
                                      connection,
                                      HIPPO_DBUS_PREFS_INTERFACE,
                                      NULL);
}
