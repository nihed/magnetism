/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-server.h"
#include "hippo-dbus-web.h"
#include "main.h"


DBusMessage*
hippo_dbus_handle_introspect_web(HippoDBus   *dbus,
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
                           HIPPO_DBUS_WEB_INTERFACE);

    g_string_append(xml,
                    "    <method name=\"GetCookiesToSend\">\n"
                    "      <arg direction=\"in\" type=\"s\"/>\n"
                    "      <arg direction=\"out\" type=\"a(ss)\"/>\n"
                    "    </method>\n");

    
    g_string_append(xml, "  </interface>\n");        
  
    g_string_append(xml, "</node>\n");


    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);
    
    return reply;
}

