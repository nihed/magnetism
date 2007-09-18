/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>

#include <string.h>

#define PURPLE_PLUGINS

#include <plugin.h>
#include <version.h>
#include <blist.h>
#include <util.h>
#include <cipher.h>

#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>

#define HIPPO_DBUS_IM_INTERFACE "org.freedesktop.od.IM"
#define HIPPO_DBUS_IM_PATH "/org/freedesktop/od/im"

typedef struct {
    char *hash;
    /* We can't just hold a ref to a PurpleBuddyIcon because
     * of purple_buddy_icon_set_data().
     */
    GString *data;
} IconData;

typedef struct {
    DBusConnection *connection;
    GHashTable *icons;
} PluginData;

static char*
hash_icon(const void *image_data,
          size_t      image_len)
{
    PurpleCipherContext *context;
    gchar digest[41];
    
    context = purple_cipher_context_new_by_name("sha1", NULL);
    if (context == NULL) {
        g_warning("Could not find SHA-1 cipher");
        g_assert_not_reached();
    }
    
    /* Hash the image data */
    purple_cipher_context_append(context, image_data, image_len);
    if (!purple_cipher_context_digest_to_str(context, sizeof(digest), digest, NULL)) {
        g_warning("Could not compute SHA-1 cipher");
        g_assert_not_reached();
    }
    purple_cipher_context_destroy(context);

    g_assert(digest[40] == '\0');
    g_assert(strlen(digest) == 40);
    return g_strdup(digest);
}

static IconData*
lookup_icon_data(PluginData      *pd,
                 const char      *hash)
{
    return g_hash_table_lookup(pd->icons, hash);
}

static IconData*
ensure_icon_data(PluginData      *pd,
                 PurpleBuddyIcon *icon)
{
    IconData *id;
    const void *image_data;
    size_t image_len;
    char *hash;    
    
    image_len = 0;
    image_data = purple_buddy_icon_get_data(icon, &image_len);

    hash = hash_icon(image_data, image_len);
    id = g_hash_table_lookup(pd->icons, hash);
    if (id != NULL) {
        g_free(hash);
        return id;
    }

    /* We store a copy of purple_buddy_icon_get_data() data because
     * there is a purple_buddy_icon_set_data() and it isn't clear
     * to me how we'd reliably get change notification when it's used
     * in order to re-hash.
     */
    
    id = g_new0(IconData, 1);
    id->hash = hash;
    id->data = g_string_new_len(image_data, image_len);

    g_hash_table_replace(pd->icons, id->hash, id);

    /* FIXME the only "robust" way to remove stuff from this icon hash
     * without precise semantics from libpurple would be to run a periodic
     * GC; i.e. walk all the buddies and see if any icons are no longer
     * present.
     * 
     * For now our goal is just to work, and we'll have to refine later.
     */
    
    return id;
}

static void
icon_data_free(void *data)
{
    IconData *id = data;
    g_free(id->hash);
    g_string_free(id->data, TRUE);
    g_free(id);
}

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
append_buddy(PluginData             *pd,
             DBusMessageIter        *append_iter,
             PurpleBuddy            *buddy)
{
    DBusMessageIter dict_iter;
    dbus_bool_t is_online;
    const char *protocol;
    PurpleBuddyIcon *icon;
    
    dbus_message_iter_open_container(append_iter, DBUS_TYPE_ARRAY, "{sv}", &dict_iter);
    
    append_basic_entry(&dict_iter, "name", DBUS_TYPE_STRING, &buddy->name);

    is_online = PURPLE_BUDDY_IS_ONLINE(buddy);
    append_basic_entry(&dict_iter, "online", DBUS_TYPE_BOOLEAN, &is_online);

    if (strcmp(buddy->account->protocol_id, "prpl-aim") == 0)
        protocol = "aim";
    else if (strcmp(buddy->account->protocol_id, "prpl-jabber") == 0)
        protocol = "xmpp";
    else
        protocol = "unknown";

    append_basic_entry(&dict_iter, "protocol", DBUS_TYPE_STRING, &protocol);

    icon = purple_buddy_get_icon(buddy);
    if (icon) {
        IconData *id;

        id = ensure_icon_data(pd, icon);

        append_basic_entry(&dict_iter, "icon", DBUS_TYPE_STRING, &id->hash);
    }

    /* lots of paranoia about stuff being NULL here just because I'm
     * not sure when it will or won't be and it's not really worth
     * looking up
     */
    if (buddy->presence) {
        const char *status;
        PurpleStatus *pstatus;

        status = NULL;
        pstatus = purple_presence_get_active_status(buddy->presence);
        if (pstatus)
            status = purple_status_get_name(pstatus);
        if (status)
            append_basic_entry(&dict_iter, "status", DBUS_TYPE_STRING, &status);
    }
    
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
append_all_buddies(PluginData      *pd,
                   DBusMessageIter *append_iter)
{
    PurpleBlistNode *root;
    GSList *buddies;
    
    root = purple_blist_get_root();
    
    buddies = NULL;

    recurse_finding_buddies(root, &buddies);

    while (buddies != NULL) {
        PurpleBuddy *buddy = buddies->data;
        buddies = g_slist_remove(buddies, buddies->data);

        append_buddy(pd, append_iter, buddy);
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

    append_all_buddies(pd, &array_iter);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static DBusMessage*
handle_get_icon(void            *object,
                DBusMessage     *message,
                DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter append_iter;
    DBusMessageIter array_iter;
    const char *requested_id;
    IconData *id;
    PluginData *pd;
    char *content_type;

    pd = object;
    
    requested_id = NULL;
    if (!dbus_message_get_args(message, error,
                               DBUS_TYPE_STRING, &requested_id,
                               DBUS_TYPE_INVALID))
        return NULL;

    id = lookup_icon_data(pd, requested_id);
    if (id == NULL) {
        dbus_set_error(error, DBUS_ERROR_FAILED,
                       "Unknown icon ID");
        return NULL;
    }
    
    reply = dbus_message_new_method_return(message); 
    dbus_message_iter_init_append(reply, &append_iter);

    content_type = g_strdup_printf("image/%s",
                                   purple_util_get_image_extension(id->data->str,
                                                                   id->data->len));
    
    dbus_message_iter_append_basic(&append_iter, DBUS_TYPE_STRING, &content_type);
    g_free(content_type);

    dbus_message_iter_open_container(&append_iter, DBUS_TYPE_ARRAY, "y", &array_iter);
    dbus_message_iter_append_fixed_array(&array_iter, DBUS_TYPE_BYTE, &id->data->str, id->data->len);
    dbus_message_iter_close_container(&append_iter, &array_iter);
    
    return reply;
}

/* This interface is the same one used in the "Online Desktop Engine" aggregator
 * thing, keep them in sync.
 */
static const HippoDBusMember im_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetBuddyList", "", "aa{sv}", handle_get_buddies },

    /* args are "s" icon ID, and returns "s" content-type and "ay" the
     * icon in PNG or other common format.  the icon ID would be in
     * the key-value dict for a buddy, under key "icon"
     */
    
    { HIPPO_DBUS_MEMBER_METHOD, "GetIcon", "s", "say", handle_get_icon },
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

typedef struct {
    PluginData *pd;
    PurpleBuddy *buddy;
} BuddyAppenderData;

static dbus_bool_t
buddy_appender(DBusMessage *message,
               void        *data)
{
    DBusMessageIter iter;
    BuddyAppenderData *bad;

    bad = data;

    dbus_message_iter_init_append(message, &iter);
    
    append_buddy(bad->pd, &iter, bad->buddy);
    
    return TRUE;
}

static void
emit_buddy_changed (PluginData     *pd,
                    PurpleBuddy    *buddy)
{
    BuddyAppenderData bad;

    bad.pd = pd;
    bad.buddy = buddy;
    hippo_dbus_helper_emit_signal_appender(pd->connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                           "BuddyChanged", buddy_appender, &bad);
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
    
    emit_buddy_changed(pd, buddy);
}

static void
on_buddy_status_changed(PurpleBuddy *buddy,
                        void        *some_pidgin_status_thingy,
                        void        *some_pidgin_status_thingy2,
                        void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy status changed\n"); */
    
    emit_buddy_changed(pd, buddy);
}

static void
on_buddy_signed_on(PurpleBuddy *buddy,
                   void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy signed on\n"); */
    
    emit_buddy_changed(pd, buddy);
}

static void
on_buddy_signed_off(PurpleBuddy *buddy,
                    void        *data)
{
    PluginData *pd = data;

    /* g_printerr("buddy signed off\n"); */
    
    emit_buddy_changed(pd, buddy);
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

    pd->icons = g_hash_table_new_full(g_str_hash, g_str_equal, NULL,
                                      icon_data_free);
    
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

        g_hash_table_destroy(pd->icons);
        
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

