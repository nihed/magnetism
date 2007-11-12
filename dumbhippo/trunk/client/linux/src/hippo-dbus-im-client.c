/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>

#include <ddm/ddm.h>
#include "hippo-dbus-helper.h"

#include "main.h"
#include "hippo-dbus-im.h"
#include "hippo-dbus-im-client.h"

#define IM_RESOURCE_BASE "online-desktop:/o"

typedef struct {
    DBusConnection *connection;
    char *resource_path;
    HippoDBusProxy *im_proxy;
    GHashTable *resource_ids;
    guint reload_idle;
    GHashTable *icon_requests;
} ImData;

typedef struct {
    ImData *id;
    char   *hash;
    char   *buddy_id;
} IconRequest;

static void
handle_get_icon_reply(DBusMessage *reply,
                      void        *data)
{
    IconRequest *ir = data;
    DBusMessageIter toplevel_iter, byte_array_iter;
    const char *content_type;
    const char *bytes;
    int bytes_len;

    g_debug("Received icon %s for %s", ir->hash, ir->buddy_id);
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        g_warning("Error getting icon: %s", dbus_message_get_error_name(reply));
        return;
    }

    dbus_message_iter_init(reply, &toplevel_iter);

    g_assert(dbus_message_iter_get_arg_type(&toplevel_iter) == DBUS_TYPE_STRING);

    dbus_message_iter_get_basic(&toplevel_iter, &content_type);

    dbus_message_iter_next(&toplevel_iter);

    g_assert(dbus_message_iter_get_arg_type(&toplevel_iter) == DBUS_TYPE_ARRAY);
    dbus_message_iter_recurse(&toplevel_iter, &byte_array_iter);

    dbus_message_iter_get_fixed_array(&byte_array_iter, &bytes, &bytes_len);

    hippo_dbus_im_update_buddy_icon(ir->buddy_id,
                                    ir->hash, content_type,
                                    bytes, bytes_len);

    g_hash_table_remove(ir->id->icon_requests,
                        ir->hash);
    
    g_free(ir->hash);
    g_free(ir->buddy_id);
    g_free(ir);
}

static void
make_icon_request(ImData     *id,
                  const char *icon_hash,
                  const char *buddy_id)
{
    IconRequest *ir;

    if (g_hash_table_lookup(id->icon_requests, icon_hash) != NULL)
        return; /* we have a request pending already */
    
    ir = g_new0(IconRequest, 1);
    ir->id = id;
    ir->hash = g_strdup(icon_hash);
    ir->buddy_id = g_strdup(buddy_id);
    
    hippo_dbus_proxy_call_method_async(id->im_proxy,
                                       "GetIcon",
                                       handle_get_icon_reply,
                                       ir,
                                       NULL,
                                       DBUS_TYPE_STRING, &ir->hash,
                                       DBUS_TYPE_INVALID);

    g_hash_table_replace(id->icon_requests,
                         ir->hash, ir);

    g_debug("Sent request for icon %s for buddy %s", ir->hash, ir->buddy_id);
}

static char *
make_buddy_resource_id(ImData     *id,
                       const char *protocol,
                       const char *name)
{
    return g_strdup_printf(IM_RESOURCE_BASE "/%s/buddy/%s.%s", id->resource_path, protocol, name);
}

static void
read_basic_variant(DBusMessageIter *variant_iter,
                   int              dbus_type,
                   void            *value_p)
{
    if (dbus_message_iter_get_arg_type(variant_iter) != dbus_type)
        return;
    
    dbus_message_iter_get_basic(variant_iter, value_p);
}

static void
notify_buddy(ImData             *id,
             DBusMessageIter    *buddy_iter,
             GHashTable         *new_resource_ids /* may be NULL */)
{
    char *resource_id = NULL;

    /* Buddy properties */
    const char *protocol = NULL;
    const char *name = NULL;
    const char *alias = NULL; /* Human visible name */
    dbus_bool_t is_online = FALSE;
    const char *status = NULL;
    const char *webdav_url = NULL;
    const char *icon = NULL;    
    
    while (dbus_message_iter_get_arg_type(buddy_iter) != DBUS_TYPE_INVALID) {
        DBusMessageIter entry_iter, variant_iter;
        const char *field;
        
        g_assert(dbus_message_iter_get_arg_type(buddy_iter) == DBUS_TYPE_DICT_ENTRY);
        
        dbus_message_iter_recurse(buddy_iter, &entry_iter);
        g_assert(dbus_message_iter_get_arg_type(&entry_iter) == DBUS_TYPE_STRING);

        field = NULL;
        dbus_message_iter_get_basic(&entry_iter, &field);
        g_assert(field != NULL);
        
        dbus_message_iter_next(&entry_iter);
        g_assert(dbus_message_iter_get_arg_type(&entry_iter) == DBUS_TYPE_VARIANT);
        dbus_message_iter_recurse(&entry_iter, &variant_iter);

        if (strcmp(field, "protocol") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &protocol);
        } else if (strcmp(field, "name") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &name);
        } else if (strcmp(field, "alias") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &alias);
        } else if (strcmp(field, "status") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &status);
        } else if (strcmp(field, "online") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_BOOLEAN, &is_online);
        } else if (strcmp(field, "webdav-url") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &webdav_url);
        } else if (strcmp(field, "icon") == 0) {
            read_basic_variant(&variant_iter, DBUS_TYPE_STRING, &icon);
        } /* ignore any unknown fields */        
        
        dbus_message_iter_next(buddy_iter);
    }
    
    if (protocol == NULL || name == NULL) {
        /* not enough info */
        return;
    }

    resource_id = make_buddy_resource_id(id, protocol, name);

    hippo_dbus_im_update_buddy(resource_id,
                               protocol,
                               name, alias,
                               is_online,
                               status,
                               webdav_url);

    /* has_icon_hash() allows icon==NULL. It checks whether
     * the buddy we have stored has a matching hash, including
     * matching hash of NULL
     */
    if (!hippo_dbus_im_has_icon_hash(resource_id, icon)) {
        make_icon_request(id, icon, resource_id);
    } else {
        g_debug("It looks like we already have icon %s", icon ? icon : "(none)");
    }
    
    if (new_resource_ids)
        g_hash_table_replace(new_resource_ids,
                             resource_id,
                             GINT_TO_POINTER(1));
    else
        g_free(resource_id);
}

static void
handle_buddy_changed(DBusConnection *connection,
                     DBusMessage    *message,
                     void           *data)
{
    ImData *id = data;
    DBusMessageIter toplevel_iter, buddy_iter;
    
    if (id->reload_idle != 0)
        return; /* no point doing any work if we're going to do a full reload anyway */

    dbus_message_iter_init(message, &toplevel_iter);
    g_assert(dbus_message_iter_get_arg_type(&toplevel_iter) == DBUS_TYPE_ARRAY);
    dbus_message_iter_recurse(&toplevel_iter, &buddy_iter);
    
    notify_buddy(id, &buddy_iter, NULL);
}


typedef struct {
    GHashTable *new_resource_ids;
} FindRemovedResourcesClosure;

static void
find_removed_resources_foreach(gpointer key,
                               gpointer value,
                               gpointer data)
{
    const char *old_resource_id = key;
    FindRemovedResourcesClosure *closure = data;

    if (closure->new_resource_ids == NULL ||
        g_hash_table_lookup(closure->new_resource_ids, old_resource_id) == NULL)
        hippo_dbus_im_remove_buddy(old_resource_id);
}

static void
remove_old_resources(GHashTable         *old_resource_ids,
                     GHashTable         *new_resource_ids /* may be NULL */)
{
    FindRemovedResourcesClosure closure;

    closure.new_resource_ids = new_resource_ids;
    
    g_hash_table_foreach(old_resource_ids,
                         find_removed_resources_foreach,
                         &closure);    
}

static void
load_state_from_buddy_list(ImData          *id,
                           DBusMessageIter *buddy_array_iter)
{
    GHashTable *new_buddy_resource_ids;
    
    new_buddy_resource_ids = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                   g_free, NULL);
    
    while (dbus_message_iter_get_arg_type(buddy_array_iter) != DBUS_TYPE_INVALID) {
        DBusMessageIter buddy_iter;
        
        g_assert(dbus_message_iter_get_arg_type(buddy_array_iter) == DBUS_TYPE_ARRAY);
        dbus_message_iter_recurse(buddy_array_iter, &buddy_iter);

        notify_buddy(id, &buddy_iter, new_buddy_resource_ids);        
        
        dbus_message_iter_next(buddy_array_iter);
    }

    /* Figure out what we removed */
    remove_old_resources(id->resource_ids, new_buddy_resource_ids);
    
    g_hash_table_destroy(id->resource_ids);
    id->resource_ids = new_buddy_resource_ids;
}

static void
handle_buddy_list_reply(DBusMessage *reply,
                        void        *data)
{
    ImData *id = data;
    DBusMessageIter toplevel_iter, buddy_array_iter;
    
    if (dbus_message_get_type(reply) != DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        g_warning("Error getting buddy list: %s", dbus_message_get_error_name(reply));
        return;
    }
    
    dbus_message_iter_init(reply, &toplevel_iter);
    g_assert(dbus_message_iter_get_arg_type(&toplevel_iter) == DBUS_TYPE_ARRAY);
    dbus_message_iter_recurse(&toplevel_iter, &buddy_array_iter);
    load_state_from_buddy_list(id, &buddy_array_iter);
}

static gboolean
reload_idle_handler(void *data)
{
    ImData *id = data;

    id->reload_idle = 0;

    if (id->im_proxy == NULL) /* happens if the service went away before idle */
        return FALSE;
    
    hippo_dbus_proxy_call_method_async(id->im_proxy,
                                       "GetBuddyList",
                                       handle_buddy_list_reply,
                                       id,
                                       NULL,
                                       DBUS_TYPE_INVALID);
    
    return FALSE;
}

static void
queue_reload_buddy_list(ImData *id)
{
    g_return_if_fail(id->im_proxy != NULL);
    
    if (id->reload_idle == 0) {
        id->reload_idle = g_idle_add(reload_idle_handler, id);
    }
}

static void
handle_buddy_list_changed(DBusConnection *connection,
                          DBusMessage    *message,
                          void           *data)
{
    ImData *id = data;
    queue_reload_buddy_list(id);
}

static void
handle_service_available(DBusConnection *connection,
                         const char     *tracked_name,
                         const char     *unique_name,
                         void           *data)
{
    ImData *id = data;

    if (id->im_proxy != NULL)
        return;
    
    id->im_proxy = hippo_dbus_proxy_new(id->connection,
                                        unique_name,
                                        HIPPO_DBUS_IM_PATH,
                                        HIPPO_DBUS_IM_INTERFACE);
    queue_reload_buddy_list(id);
}

static void
handle_service_unavailable(DBusConnection *connection,
                           const char     *tracked_name,
                           const char     *unique_name,
                           void           *data)
{
    ImData *id = data;

    if (id->im_proxy == NULL)
        return;

    hippo_dbus_proxy_unref(id->im_proxy);
    id->im_proxy = NULL;

    if (id->reload_idle) {
        g_source_remove(id->reload_idle);
        id->reload_idle = 0;
    }
    
    remove_old_resources(id->resource_ids, NULL);

    g_hash_table_remove_all(id->resource_ids);
}

static const HippoDBusSignalTracker signal_handlers[] = {
    { HIPPO_DBUS_IM_INTERFACE, "BuddyChanged",     handle_buddy_changed },
    { HIPPO_DBUS_IM_INTERFACE, "BuddyListChanged", handle_buddy_list_changed },
    { NULL, NULL, NULL }
};

static const HippoDBusServiceTracker service_tracker = {
    0,
    handle_service_available,
    handle_service_unavailable
};

void
hippo_dbus_im_client_add(DBusConnection *connection,
                         const char     *bus_name,
                         const char     *resource_path)
{
    ImData *id;

    id = g_new0(ImData, 1);
    id->connection = connection;
    dbus_connection_ref(id->connection);
    id->resource_path = g_strdup(resource_path);
    id->resource_ids = g_hash_table_new_full(g_str_hash, g_str_equal,
                                             g_free, NULL);
    id->icon_requests = g_hash_table_new_full(g_str_hash, g_str_equal,
                                              NULL, NULL);
    
    hippo_dbus_helper_register_service_tracker(connection,
                                               bus_name,
                                               &service_tracker,
                                               signal_handlers,
                                               id);
}
