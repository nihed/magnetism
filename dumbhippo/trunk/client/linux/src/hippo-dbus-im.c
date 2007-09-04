/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>

#include <hippo/hippo-notification-set.h>
#include <ddm/ddm.h>

#include "hippo-dbus-helper.h"
#include "hippo-dbus-im.h"
#include "main.h"

#define GLOBAL_RESOURCE "online-desktop:/o/global"
#define GLOBAL_CLASS "online-desktop:/p/o/global"
#define BUDDY_CLASS "online-desktop:/p/o/buddy"

typedef struct {
    GHashTable *buddies;
} HippoDBusIm;

typedef struct {
    char *resource_id;
    char *protocol;
    char *name;
    gboolean is_online;
    char *status;
    char *webdav_url;
} HippoDBusImBuddy;

static void hippo_dbus_im_append_buddy(DBusMessageIter        *append_iter,
                                       const HippoDBusImBuddy *buddy);

static void 
hippo_dbus_im_buddy_destroy(HippoDBusImBuddy *buddy)
{
    g_free(buddy->resource_id);
    g_free(buddy->protocol);
    g_free(buddy->name);
    g_free(buddy->status);
    g_free(buddy->webdav_url);
    g_free(buddy);
}

static void
hippo_dbus_im_destroy(HippoDBusIm *im)
{
    g_hash_table_destroy(im->buddies);
    g_free(im);
}

static HippoDBusIm *
hippo_dbus_im_get(HippoDataCache *cache)
{
    HippoDBusIm *im = g_object_get_data(G_OBJECT(cache), "hippo-dbus-im");
    if (im == NULL) {
        im = g_new0(HippoDBusIm, 1);
        im->buddies = g_hash_table_new_full(g_str_hash, g_str_equal,
                                            NULL, (GDestroyNotify)hippo_dbus_im_buddy_destroy);
        g_object_set_data_full(G_OBJECT(cache), "hippo-dbus-im", im, (GDestroyNotify)hippo_dbus_im_destroy);
    }

    return im;
}

static void
get_buddies_foreach(gpointer key,
                    gpointer value,
                    gpointer data)
{
    HippoDBusImBuddy *buddy = value;
    DBusMessageIter *append_iter = data;

    hippo_dbus_im_append_buddy(append_iter, buddy);
}

static DBusMessage*
handle_get_buddies(void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;

    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    /* open an array of dict */
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "a{sv}", &array_iter);
    
    g_hash_table_foreach(im->buddies, get_buddies_foreach, &array_iter);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static const HippoDBusMember im_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetBuddyList", "", "aa{sv}", handle_get_buddies },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyListChanged", "", "", NULL },
    { HIPPO_DBUS_MEMBER_SIGNAL, "BuddyChanged", "", "a{sv}", NULL },
    { 0, NULL }
};

void
hippo_dbus_init_im(DBusConnection *connection,
                   gboolean        replace)
{
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_IM_INTERFACE,
                                         im_members, NULL);
    
    hippo_dbus_helper_register_object(connection, HIPPO_DBUS_IM_PATH,
                                      NULL, HIPPO_DBUS_IM_INTERFACE,
                                      NULL);
}

static void
hippo_dbus_im_emit_buddy_list_changed (DBusConnection *connection)
{
    hippo_dbus_helper_emit_signal(connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                  "BuddyListChanged", DBUS_TYPE_INVALID);
}

static void
hippo_dbus_im_emit_buddy_changed(DBusConnection         *connection,
                                 const HippoDBusImBuddy *buddy)
{
    DBusMessage *message;
    DBusMessageIter iter;
    
    message = dbus_message_new_signal(HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                      "BuddyChanged");

    dbus_message_iter_init_append(message, &iter);

    hippo_dbus_im_append_buddy(&iter, buddy);
    
    dbus_connection_send(connection, message, NULL);

    dbus_message_unref(message);
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
hippo_dbus_im_append_buddy(DBusMessageIter        *append_iter,
                           const HippoDBusImBuddy *buddy)
{
    DBusMessageIter dict_iter;
    
    dbus_message_iter_open_container(append_iter, DBUS_TYPE_ARRAY, "{sv}", &dict_iter);
    
    append_basic_entry(&dict_iter, "protocol", DBUS_TYPE_STRING, &buddy->protocol);
    append_basic_entry(&dict_iter, "name", DBUS_TYPE_STRING, &buddy->name);
    append_basic_entry(&dict_iter, "status", DBUS_TYPE_STRING, &buddy->status);
    append_basic_entry(&dict_iter, "online", DBUS_TYPE_BOOLEAN, &buddy->is_online);
    
    dbus_message_iter_close_container(append_iter, &dict_iter);
}

static DDMDataResource *
get_system_resource(DDMDataModel *model)
{
    return ddm_data_model_ensure_resource(model, GLOBAL_RESOURCE, GLOBAL_CLASS);
}

static gboolean
compare_strings(const char *a, const char *b)
{
    if (a == b)
        return TRUE;

    if (a == NULL || b == NULL)
        return FALSE;

    return strcmp(a, b) == 0;
}

HippoNotificationSet *
hippo_dbus_im_start_notifications(void)
{
    HippoApp *app = hippo_get_app();
    HippoDataCache *cache;
    DDMDataModel *model;
    
    if (!app)
        return NULL;
    
    cache = hippo_app_get_data_cache(hippo_get_app());
    model = hippo_data_cache_get_model(cache);
    
    return _hippo_notification_set_new(model);
}

static void
update_property (DDMDataResource    *resource,
                 DDMQName           *property_id,
                 DDMDataUpdate       update,
                 DDMDataCardinality  cardinality,
                 gboolean              default_include,
                 const char           *default_children,
                 DDMDataValue       *value,
                 HippoNotificationSet *notifications)
{
    if (ddm_data_resource_update_property(resource, property_id, update,
                                           cardinality, default_include, default_children,
                                           value))
        _hippo_notification_set_add(notifications, resource, property_id);
}

void
hippo_dbus_im_update_buddy(HippoNotificationSet *notifications,
                           const char           *buddy_id,
                           const char           *protocol,
                           const char           *name,
                           gboolean              is_online,
                           const char           *status,
                           const char           *webdav_url)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    gboolean new_buddy = FALSE;
    gboolean online_changed;
    DDMDataResource *buddy_resource;
    DDMDataValue value;
    gboolean buddy_changed = FALSE;

    HippoDBusImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);
    if (buddy == NULL) {
        buddy = g_new0(HippoDBusImBuddy, 1);
        buddy->resource_id = g_strdup(buddy_id);
        g_hash_table_insert(im->buddies, buddy->resource_id, buddy);
        new_buddy = TRUE;
    }

    buddy_resource = ddm_data_model_ensure_resource(model, buddy_id, BUDDY_CLASS);

    if (new_buddy || !compare_strings(protocol, buddy->protocol)) {
        g_free(buddy->protocol);
        buddy->protocol = g_strdup(protocol);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->protocol;
        
        update_property(buddy_resource,
                        ddm_qname_get(BUDDY_CLASS, "protocol"),
                        DDM_DATA_UPDATE_REPLACE,
                        DDM_DATA_CARDINALITY_1,
                        TRUE, NULL,
                        &value,
                        notifications);

        buddy_changed = !new_buddy;
    }

    if (new_buddy || !compare_strings(name, buddy->name)) {
        g_free(buddy->name);
        buddy->name = g_strdup(name);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->name;
        
        update_property(buddy_resource,
                        ddm_qname_get(BUDDY_CLASS, "name"),
                        DDM_DATA_UPDATE_REPLACE,
                        DDM_DATA_CARDINALITY_1,
                        TRUE, NULL,
                        &value,
                        notifications);
        
        buddy_changed = !new_buddy;
    }

    online_changed= !new_buddy && is_online != buddy->is_online;
    
    if (new_buddy || is_online != buddy->is_online) {
        buddy->is_online = is_online;

        value.type = DDM_DATA_BOOLEAN;
        value.u.boolean = buddy->is_online;
        
        update_property(buddy_resource,
                        ddm_qname_get(BUDDY_CLASS, "isOnline"),
                        DDM_DATA_UPDATE_REPLACE,
                        DDM_DATA_CARDINALITY_1,
                        TRUE, NULL,
                        &value,
                        notifications);
        
        buddy_changed = !new_buddy;
    }

    if (new_buddy || !compare_strings(status, buddy->status)) {
        g_free(buddy->status);
        buddy->status = g_strdup(status);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->status;

        update_property(buddy_resource,
                                             ddm_qname_get(BUDDY_CLASS, "status"),
                                             buddy->status ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_CLEAR,
                                             DDM_DATA_CARDINALITY_01,
                                             TRUE, NULL,
                                             &value,
                                             notifications);
        
        buddy_changed = !new_buddy;
    }

    if (new_buddy || !compare_strings(webdav_url, buddy->webdav_url)) {
        g_free(buddy->webdav_url);
        buddy->webdav_url = g_strdup(webdav_url);

        value.type = DDM_DATA_STRING;
        value.u.string = buddy->webdav_url;

        update_property(buddy_resource,
                                             ddm_qname_get(BUDDY_CLASS, "webdavUrl"),
                                             buddy->webdav_url ? DDM_DATA_UPDATE_REPLACE : DDM_DATA_UPDATE_CLEAR,
                                             DDM_DATA_CARDINALITY_01,
                                             TRUE, NULL,
                                             &value,
                                             notifications);
        
        buddy_changed = !new_buddy;
    }

    if (online_changed || (new_buddy && buddy->is_online)) {
        DDMDataResource *system_resource = get_system_resource(model);
        DDMDataValue value;

        value.type = DDM_DATA_RESOURCE;
        value.u.resource = buddy_resource;
        
        update_property(system_resource,
                                             ddm_qname_get(GLOBAL_CLASS, "onlineBuddies"),
                                             buddy->is_online ? DDM_DATA_UPDATE_ADD : DDM_DATA_UPDATE_DELETE,
                                             DDM_DATA_CARDINALITY_N,
                                             FALSE, NULL,
                                             &value,
                                             notifications);
        
    }
    
    if (buddy_changed) {
        DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
        hippo_dbus_im_emit_buddy_changed (connection, buddy);
    }
}

void 
hippo_dbus_im_remove_buddy(HippoNotificationSet *notifications,
                           const char           *buddy_id)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoDBusIm *im = hippo_dbus_im_get(cache);
    DDMDataModel *model = hippo_data_cache_get_model(cache);
    DDMDataResource *system_resource = get_system_resource(model);

    HippoDBusImBuddy *buddy = g_hash_table_lookup(im->buddies, buddy_id);

    if (buddy == NULL)
        return;

    if (buddy->is_online) {
        DDMDataResource *buddy_resource = ddm_data_model_lookup_resource(model, buddy_id);
        DDMDataValue value;

        value.type = DDM_DATA_RESOURCE;
        value.u.resource = buddy_resource;
        
        update_property(system_resource,
                        ddm_qname_get(GLOBAL_CLASS, "onlineBuddies"),
                        DDM_DATA_UPDATE_DELETE,
                        DDM_DATA_CARDINALITY_N,
                        FALSE, NULL,
                        &value,
                        notifications);
    }

    g_hash_table_remove(im->buddies, buddy_id);
}
    
void
hippo_dbus_im_send_notifications(HippoNotificationSet *notifications)
{
    if (_hippo_notification_set_has_property(notifications,
                                             GLOBAL_RESOURCE,
                                             ddm_qname_get(GLOBAL_CLASS, "onlineBuddies"))) {
        DBusConnection *connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
        hippo_dbus_im_emit_buddy_list_changed (connection);        
    }
    
    _hippo_notification_set_send(notifications);
    _hippo_notification_set_free(notifications);
}
