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
}

DBusMessage*
hippo_dbus_handle_get_preference(HippoDBus   *dbus,
                                 DBusMessage *message)
{
    DBusMessage *reply;
    const char *key;
    const char *signature;
    DBusMessageIter iter;
    DBusMessageIter variant_iter;
    dbus_int32_t v_INT32;

    key = NULL;
    signature = NULL;
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_STRING, &key,
                               DBUS_TYPE_SIGNATURE, &signature,
                               DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
                                      DBUS_ERROR_INVALID_ARGS,
                                      _("Expected two arguments, the key and the expected typecode"));
    }

    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_VARIANT, DBUS_TYPE_INT32_AS_STRING,
                                     &variant_iter);

    v_INT32 = 42;
    dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_INT32, &v_INT32);

    dbus_message_iter_close_container(&iter, &variant_iter);

    return reply;
}

DBusMessage*
hippo_dbus_handle_set_preference(HippoDBus   *dbus,
                                 DBusMessage *message)
{
    DBusMessage *reply;
    
    reply = dbus_message_new_method_return(message);

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
    
    g_string_append(xml, "  </interface>\n");        
  
    g_string_append(xml, "</node>\n");


    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);
    
    return reply;
}
