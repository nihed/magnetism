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
#include "hippo-dbus-mugshot.h"
#include "main.h"

void
hippo_dbus_try_acquire_mugshot(DBusConnection *connection,
                               gboolean        replace)
{
    dbus_uint32_t flags;

    /* We do want to be queued if we don't get this right away */
    flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;

    /* we just ignore errors on this */
    dbus_bus_request_name(connection, HIPPO_DBUS_MUGSHOT_BUS_NAME,
                          flags,
                          NULL);
}

DBusMessage*
hippo_dbus_handle_mugshot_get_whereim(HippoDBus   *dbus,
									  HippoConnection *connection,
                                      DBusMessage *message)
{
    hippo_connection_request_mugshot_whereim(connection);
    
    return dbus_message_new_method_return(message);  /* Send out the results as signals.  */
}

DBusMessage*
hippo_dbus_handle_mugshot_introspect(HippoDBus   *dbus,
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
                           HIPPO_DBUS_MUGSHOT_INTERFACE);

    g_string_append(xml,
                    "    <method name=\"GetWhereim\"/>\n");

    g_string_append(xml, "  </interface>\n");        
  
    g_string_append(xml, "</node>\n");


    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);
    
    return reply;
}

DBusMessage*
hippo_dbus_mugshot_signal_whereim_changed(HippoDBus            *dbus,
                                          HippoConnection      *connection,
                                          HippoExternalAccount *acct)
{
	DBusMessage *signal;
	char *name, *icon_url, *abs_icon_url;
	
	g_object_get(acct, "name", &name, "icon-url", &icon_url, NULL);
	
	signal = dbus_message_new_signal(HIPPO_DBUS_MUGSHOT_PATH,
	                                 HIPPO_DBUS_MUGSHOT_INTERFACE,
	                                 "whereimChanged");
	abs_icon_url = hippo_connection_make_absolute_url(connection, icon_url);
	dbus_message_append_args(signal, DBUS_TYPE_STRING, &name,
	                                 DBUS_TYPE_STRING, &abs_icon_url,
	                                 DBUS_TYPE_INVALID);
    g_free(name);
    g_free(icon_url);
    g_free(abs_icon_url);
    return signal;	                                 
}
