/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>

#define PURPLE_PLUGINS

#include <plugin.h>
#include <version.h>

#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>

#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"


typedef struct {
    DBusConnection *connection;

} PluginData;

static DBusMessage*
handle_get_buddies(void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;
    PluginData *pd;

    pd = object;
    
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    /* open an array of dict */
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "a{sv}", &array_iter);

    /* FIXME write the buddies */
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}


static const HippoDBusMember im_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetBuddyList", "", "aa{sv}", handle_get_buddies },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyListChanged", "", "", NULL },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyChanged", "", "a{sv}", NULL },
    { 0, NULL }
};

static void
hippo_dbus_im_emit_buddy_list_changed (DBusConnection *connection)
{
    hippo_dbus_helper_emit_signal(connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                  "BuddyListChanged", DBUS_TYPE_INVALID);
}


static gboolean
plugin_load(PurplePlugin *plugin)
{
    PluginData *pd;
    DBusConnection *connection;

    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    if (connection == NULL)
        return FALSE;

    pd = g_new0(PluginData, 1);
    pd->connection = connection;
    dbus_connection_ref(pd->connection);

    plugin->extra = pd;

    /* in case nobody has done it yet */
    dbus_connection_setup_with_g_main(connection, NULL); 
    
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_IM_INTERFACE,
                                         im_members, NULL);
    
    hippo_dbus_helper_register_object(connection, HIPPO_DBUS_IM_PATH,
                                      pd,
                                      HIPPO_DBUS_IM_INTERFACE, NULL);
    
    return TRUE;
}

/* this should be idempotent since I'm not sure
 * when pidgin calls this vs. unload()
 */
static void
plugin_destroy(PurplePlugin *plugin)
{
    PluginData *pd;

    pd = plugin->extra;

    if (pd != NULL) {
        hippo_dbus_helper_unregister_object(pd->connection, HIPPO_DBUS_IM_PATH);
        
        dbus_connection_unref(pd->connection);
        g_free(pd);
        plugin->extra = NULL;
    }
}

static gboolean
plugin_unload(PurplePlugin *plugin)
{
    plugin_destroy(plugin);
    return TRUE;
}

static void
plugin_init(PurplePlugin *plugin)
{
    
}

static PurplePluginInfo plugin_info = {
    PURPLE_PLUGIN_MAGIC,
    PURPLE_MAJOR_VERSION,
    PURPLE_MINOR_VERSION,
    PURPLE_PLUGIN_STANDARD,
    NULL,
    PURPLE_PLUGIN_FLAG_INVISIBLE,
    NULL,
    PURPLE_PRIORITY_LOWEST, /* load after other plugins (I think that's what this is) */
    "dbus-api-plugin",
    "Freedesktop.org D-Bus API",
    VERSION,
    "Exports a standard D-Bus API", /* brief */
    "Exports a standard D-Bus API", /* long desc */
    NULL, /* name and email */
    NULL, /* website for plugin */

    plugin_load,
    plugin_unload,
    plugin_destroy,
    
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
    
};

PURPLE_INIT_PLUGIN(dbus_api, plugin_init, plugin_info);

