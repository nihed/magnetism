/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-local.h"
#include "hippo-dbus-im.h"

typedef struct {
    DBusConnection *connection;
    HippoDBusProxy *proxy;

} LocalState;

static dbus_bool_t
append_string_pair(DBusMessageIter *dict_iter,
                   const char      *key,
                   const char      *value)
{
    DBusMessageIter entry_iter;
    DBusMessageIter variant_iter;
    
    if (!dbus_message_iter_open_container(dict_iter, DBUS_TYPE_DICT_ENTRY, NULL, &entry_iter))
        return FALSE;

    if (!dbus_message_iter_append_basic(&entry_iter, DBUS_TYPE_STRING, &key))
        return FALSE;

    if (!dbus_message_iter_open_container(&entry_iter, DBUS_TYPE_VARIANT, "s", &variant_iter))
        return FALSE;
    
    if (!dbus_message_iter_append_basic(&variant_iter, DBUS_TYPE_STRING, &value))
        return FALSE;

    if (!dbus_message_iter_close_container(&entry_iter, &variant_iter))
        return FALSE;
    
    if (!dbus_message_iter_close_container(dict_iter, &entry_iter))
        return FALSE;

    return TRUE;
}

/* FIXME we need to subscribe to changes on this and update our info when it changes */
static const char*
get_self_id()
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *hippo_connection = hippo_data_cache_get_connection(cache);
    const char *self_resource_id = hippo_connection_get_self_resource_id(hippo_connection);

    return self_resource_id;
}

static dbus_bool_t
append_mugshot_info(DBusMessage *message,
                    void        *data)
{
    DBusMessageIter iter, prop_iter;
    const char *name;
    const char *s;
    
    dbus_message_iter_init_append(message, &iter);

    name = "org.mugshot.MugshotInfo";
    if (!dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &name))
        return FALSE;

    if (!dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &prop_iter))
        return FALSE;

    s = get_self_id();
    if (s != NULL) {
        if (!append_string_pair(&prop_iter, "user_resource_id", s))
            return FALSE;
    }
        
    if (!dbus_message_iter_close_container(&iter, &prop_iter))
        return FALSE;

    return TRUE;
}

static void
handle_service_available(DBusConnection *connection,
                         const char     *well_known_name,
                         const char     *unique_name,
                         void           *data)
{
    HippoDBusProxy *proxy;
    
    g_debug("%s (%s) appeared", well_known_name, unique_name);
    
    proxy = hippo_dbus_proxy_new(connection, unique_name,
                                 "/org/freedesktop/od/LocalExport",
                                 "org.freedesktop.od.LocalExport");

    hippo_dbus_proxy_call_method_async_appender(proxy, "AddInfoToOurSession",
                                                NULL, NULL, NULL,
                                                append_mugshot_info, NULL);
}

static void
handle_service_unavailable(DBusConnection *connection,
                           const char     *well_known_name,
                           const char     *unique_name,
                           void           *data)
{
    g_debug("%s (%s) going away", well_known_name, unique_name);
}

static const HippoDBusSignalTracker signal_handlers[] = {
    { NULL, NULL, NULL }
};

static const HippoDBusServiceTracker service_tracker = {
    handle_service_available,
    handle_service_unavailable
};

void
hippo_dbus_init_local(DBusConnection *connection)
{
    hippo_dbus_helper_register_service_tracker(connection,
                                               "org.freedesktop.od.LocalExport",
                                               &service_tracker,
                                               signal_handlers,
                                               NULL);    
}

#if 0

/* cc -Wall -ggdb -O2 `pkg-config --cflags --libs dbus-glib-1 glib-2.0 dbus-1` -I ../build/config hippo-dbus-local.c hippo-dbus-helper.c -o foo && ./foo */

#include <dbus/dbus-glib-lowlevel.h>

int
main(int argc, char **argv)
{
    GMainLoop *loop;
    DBusConnection *connection;

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    dbus_connection_setup_with_g_main(connection, NULL);

    hippo_dbus_init_local(connection);
    
    loop = g_main_loop_new(NULL, FALSE);
    
    g_main_loop_run(loop);

    return 0;
}

#endif
