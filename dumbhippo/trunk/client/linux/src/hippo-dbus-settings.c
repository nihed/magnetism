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
#include "main.h"

/* This is its own file since it could end up being a fair bit of code eventually
 */

static void
on_ready_changed(HippoSettings *settings,
                 gboolean       ready,
                 void          *data)
{
    DBusConnection *dbus_connection = data;
    DBusMessage *message;

    g_debug("emitting ReadyChanged ready=%d", ready);
    
    message = dbus_message_new_signal(HIPPO_DBUS_ONLINE_PREFS_PATH,
                                      HIPPO_DBUS_PREFS_INTERFACE,
                                      "ReadyChanged");
    dbus_message_append_args(message, DBUS_TYPE_BOOLEAN, &ready, DBUS_TYPE_INVALID);
    dbus_connection_send(dbus_connection, message, NULL);
    dbus_message_unref(message);
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

    if (!g_object_get_data(G_OBJECT(settings), "ready-connected")) {
        g_debug("connecting to ready-changed on HippoSettings");
        g_object_set_data(G_OBJECT(settings), "ready-connected", GINT_TO_POINTER(TRUE));
        dbus_connection_ref(dbus_connection);
        g_signal_connect_data(G_OBJECT(settings), "ready-changed", G_CALLBACK(on_ready_changed),
                              dbus_connection, (GClosureNotify) dbus_connection_unref, 0);
    }
    
    return settings;
}

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

DBusMessage*
hippo_dbus_handle_get_preference(HippoDBus   *dbus,
                                 DBusMessage *message)
{
    const char *key;
    const char *signature;
    HippoSettings *settings;
    SettingArrivedData *sad;
    DBusConnection *dbus_connection;
    
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
            *signature == DBUS_TYPE_BOOLEAN) ) {
        return dbus_message_new_error(message, DBUS_ERROR_INVALID_ARGS,
                                      _("Only STRING, INT32, BOOLEAN values supported for now"));
    }

    dbus_connection = hippo_dbus_get_connection(dbus);    
    
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

DBusMessage*
hippo_dbus_handle_set_preference(HippoDBus   *dbus,
                                 DBusMessage *message)
{
    DBusMessage *reply;
    HippoSettings *settings;
    DBusMessageIter iter;
    DBusMessageIter variant_iter;
    const char *key;
    int value_type;
    char *value;
    
    if (!dbus_message_has_signature(message, "sv")) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Expected two arguments, the string key and the variant value"));
    }
    
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
    default:
        return dbus_message_new_error_printf(message,
                                             DBUS_ERROR_INVALID_ARGS,
                                             _("Unable to handle values of type '%c' right now"), value_type);
        break;
    }

    settings = get_and_ref_settings(hippo_dbus_get_connection(dbus));
    
    hippo_settings_set(settings, key, value);

    g_free(value);
    g_object_unref(settings);
    
    /* Just an empty "ack" reply */
    reply = dbus_message_new_method_return(message);

    return reply;
}

DBusMessage*
hippo_dbus_handle_is_ready(HippoDBus   *dbus,
                           DBusMessage *message)
{
    HippoSettings *settings;
    dbus_bool_t v_BOOLEAN;
    DBusMessage *reply;

    g_debug("handling IsReady()");
    
    if (!dbus_message_has_signature(message, "")) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Expected zero arguments"));
    }

    settings = get_and_ref_settings(hippo_dbus_get_connection(dbus));
    v_BOOLEAN = hippo_settings_get_ready(settings);
    g_object_unref(settings);

    reply = dbus_message_new_method_return(message);
    dbus_message_append_args(reply, DBUS_TYPE_BOOLEAN, &v_BOOLEAN, DBUS_TYPE_INVALID);

    return reply;
}


DBusMessage*
hippo_dbus_handle_introspect_prefs(HippoDBus   *dbus,
                                   DBusMessage *message)
{
    GString *xml;
    DBusMessage *reply;

    xml = g_string_new(NULL);
    
    g_string_append(xml, DBUS_INTROSPECT_1_0_XML_DOCTYPE_DECL_NODE);
        
    g_string_append(xml, "<node>\n");
        
    g_string_append_printf(xml, "  <interface name=\"%s\">\n", DBUS_INTERFACE_INTROSPECTABLE);
        
    g_string_append(xml, "    <method name=\"Introspect\">\n");
        
    g_string_append_printf(xml, "      <arg name=\"data\" direction=\"out\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
        
    g_string_append(xml, "    </method>\n");
        
    g_string_append(xml, "  </interface>\n");

    g_string_append_printf(xml, "  <interface name=\"%s\">\n",
                           HIPPO_DBUS_PREFS_INTERFACE);

    g_string_append(xml,
                    "    <method name=\"GetPreference\">\n"
                    "      <arg direction=\"in\" type=\"s\"/>\n"
                    "      <arg direction=\"in\" type=\"g\"/>\n"
                    "      <arg direction=\"out\" type=\"v\"/>\n"
                    "    </method>\n");

    g_string_append(xml,
                    "    <method name=\"SetPreference\">\n"
                    "      <arg direction=\"in\" type=\"s\"/>\n"
                    "      <arg direction=\"in\" type=\"v\"/>\n"
                    "    </method>\n");

    g_string_append(xml,
                    "    <method name=\"IsReady\">\n"
                    "      <arg direction=\"out\" type=\"b\"/>\n"
                    "    </method>\n");

    g_string_append(xml,
                    "    <signal name=\"ReadyChanged\">\n"
                    "      <arg direction=\"out\" type=\"b\"/>\n"
                    "    </signal>\n");
    
    g_string_append(xml, "  </interface>\n");        
  
    g_string_append(xml, "</node>\n");


    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);
    
    return reply;
}
