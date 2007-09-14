/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>

#include <string.h>

#define PURPLE_PLUGINS

#include <plugin.h>
#include <version.h>
#include <blist.h>

#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>

#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"


typedef struct {
    DBusConnection *connection;

} PluginData;


static void
append_basic_entry(DBusMessageIter *dict_iter,
                   const char      *key,
                   int              type,
                   const void      *value)
{
    DBusMessageIter entry_iter, variant_iter;
    char type_str[2];
    type_str[0] = type;
    type_str[1] = '\0';

    dbus_message_iter_open_container(dict_iter, DBUS_TYPE_DICT_ENTRY, NULL, &entry_iter);
    
    dbus_message_iter_append_basic(&entry_iter, DBUS_TYPE_STRING, &key);

    dbus_message_iter_open_container(&entry_iter, DBUS_TYPE_VARIANT, type_str, &variant_iter);
    
    dbus_message_iter_append_basic(&variant_iter, type, value);

    dbus_message_iter_close_container(&entry_iter, &variant_iter);

    dbus_message_iter_close_container(dict_iter, &entry_iter);
}

static void
append_buddy(DBusMessageIter        *append_iter,
             PurpleBuddy            *buddy)
{
    DBusMessageIter dict_iter;
    dbus_bool_t is_online;
    const char *protocol;

    dbus_message_iter_open_container(append_iter, DBUS_TYPE_ARRAY, "{sv}", &dict_iter);
    
    append_basic_entry(&dict_iter, "name", DBUS_TYPE_STRING, &buddy->name);

    is_online = PURPLE_BUDDY_IS_ONLINE(buddy);
    append_basic_entry(&dict_iter, "online", DBUS_TYPE_BOOLEAN, &is_online);

    if (strcmp(buddy->account->protocol_id, "prpl-aim") == 0)
        protocol = "aim";
    else
        protocol = "unknown";

    append_basic_entry(&dict_iter, "protocol", DBUS_TYPE_STRING, &protocol);
    
#if 0
    /* FIXME */
    append_basic_entry(&dict_iter, "status", DBUS_TYPE_STRING, &buddy->status);
#endif
    
    dbus_message_iter_close_container(append_iter, &dict_iter);
}

static void
recurse_finding_buddies(PurpleBlistNode *node,
                        GSList         **buddies_p)
{
    PurpleBlistNode *iter;
    
    for (iter = node; iter != NULL; iter = iter->next) {

        if (iter->type == PURPLE_BLIST_BUDDY_NODE) {
            *buddies_p = g_slist_prepend(*buddies_p, iter);
        }

        if (iter->child) {
            recurse_finding_buddies(iter->child, buddies_p);
        }
    }
}

static void
append_all_buddies(DBusMessageIter *append_iter)
{
    PurpleBlistNode *root;
    GSList *buddies;
    
    root = purple_blist_get_root();
    
    buddies = NULL;

    recurse_finding_buddies(root, &buddies);

    while (buddies != NULL) {
        PurpleBuddy *buddy = buddies->data;
        buddies = g_slist_remove(buddies, buddies->data);

        append_buddy(append_iter, buddy);
    }
}

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

    append_all_buddies(&array_iter);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static DBusMessage*
handle_get_icon(void            *object,
                DBusMessage     *message,
                DBusError       *error)
{
    /* FIXME */
    
    return dbus_message_new_method_return(message);
}

static const HippoDBusMember im_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetBuddyList", "", "aa{sv}", handle_get_buddies },
    /* args are "s" icon ID, and returns "ay" the icon in PNG or other common format.
     * the icon ID would be in the key-value dict for a buddy, under key "icon"
     */
    { HIPPO_DBUS_MEMBER_METHOD, "GetIcon", "s", "ay", handle_get_icon },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyListChanged", "", "", NULL },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyChanged", "", "a{sv}", NULL },
    { 0, NULL }
};

static void
emit_buddy_list_changed (DBusConnection *connection)
{
    hippo_dbus_helper_emit_signal(connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                  "BuddyListChanged", DBUS_TYPE_INVALID);
}

static dbus_bool_t
buddy_appender(DBusMessage *message,
               void        *data)
{
    DBusMessageIter iter;
    PurpleBuddy *buddy;

    buddy = data;
    
    dbus_message_iter_init_append(message, &iter);
    
    append_buddy(&iter, buddy);
    
    return TRUE;
}

static void
emit_buddy_changed (DBusConnection *connection,
                    PurpleBuddy    *buddy)
{
    hippo_dbus_helper_emit_signal_appender(connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                           "BuddyChanged", buddy_appender, buddy);
}


/* means the buddy was added to the persistent list, not that it was added
 * to the buddy list object in this process.
 */
static void
on_buddy_added(PurpleBuddy *buddy,
               void        *data)
{
    PluginData *pd = data;
    
    emit_buddy_list_changed(pd->connection);
}

/* means the buddy was removed from the persistent list, not that it was removed
 * from the buddy list object in this process.
 */
static void
on_buddy_removed(PurpleBuddy *buddy,
                 void        *data)
{
    PluginData *pd = data;
    
    emit_buddy_list_changed(pd->connection);
}

static void
on_buddy_icon_changed(PurpleBuddy *buddy,
                      void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy icon changed\n"); */
    
    emit_buddy_changed(pd->connection, buddy);
}

static void
on_buddy_status_changed(PurpleBuddy *buddy,
                        void        *some_pidgin_status_thingy,
                        void        *some_pidgin_status_thingy2,
                        void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy status changed\n"); */
    
    emit_buddy_changed(pd->connection, buddy);
}

static void
on_buddy_signed_on(PurpleBuddy *buddy,
                   void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy signed on\n"); */
    
    emit_buddy_changed(pd->connection, buddy);
}

static void
on_buddy_signed_off(PurpleBuddy *buddy,
                    void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy signed off\n"); */
    
    emit_buddy_changed(pd->connection, buddy);
}

static void*
get_signal_handle(void)
{
    static int signal_handle;

    return &signal_handle;
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

    purple_signal_connect(purple_blist_get_handle(), "buddy-added",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_added), pd);
    
    purple_signal_connect(purple_blist_get_handle(), "buddy-removed",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_removed), pd);
    
    purple_signal_connect(purple_blist_get_handle(), "buddy-icon-changed",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_icon_changed), pd);
    
    purple_signal_connect(purple_blist_get_handle(), "buddy-status-changed",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_status_changed), pd);

    purple_signal_connect(purple_blist_get_handle(), "buddy-signed-on",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_signed_on), pd);

    purple_signal_connect(purple_blist_get_handle(), "buddy-signed-off",
                          get_signal_handle(),
                          PURPLE_CALLBACK(on_buddy_signed_off), pd);
    
    return TRUE;
}

/* Pidgin calls unload() then destroy() apparently */
static void
plugin_destroy(PurplePlugin *plugin)
{
    PluginData *pd;
    
    pd = plugin->extra;

    if (pd != NULL) {
        hippo_dbus_helper_unregister_object(pd->connection, HIPPO_DBUS_IM_PATH);

        purple_signals_disconnect_by_handle(get_signal_handle());
        
        dbus_connection_unref(pd->connection);
        g_free(pd);
        plugin->extra = NULL;
    }
}

static gboolean
plugin_unload(PurplePlugin *plugin)
{
    /* I guess Pidgin does this for us, but since we set up in load() I feel
     * like we should clean up in unload...
     */
    plugin_destroy(plugin);
    return TRUE;
}

static gboolean
self_load_idle(void *data)
{
    PurplePlugin *plugin = data;
    purple_plugin_load(plugin);
    return FALSE;
}

static void
plugin_init(PurplePlugin *plugin)
{

    /* Lame hack to avoid user having to manually enable this plugin;
     * we'll see how it works out...
     */
    g_idle_add(self_load_idle, plugin);
}

static PurplePluginInfo plugin_info = {
    PURPLE_PLUGIN_MAGIC,
    PURPLE_MAJOR_VERSION,
    PURPLE_MINOR_VERSION,
    PURPLE_PLUGIN_STANDARD,
    NULL,
    PURPLE_PLUGIN_FLAG_INVISIBLE, /* since we self-enable */
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

