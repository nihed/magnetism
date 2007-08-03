/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include <hippo/hippo-data-cache.h>
#include "hippo-dbus-contacts.h"
#include "main.h"






/*
 ****************
 *
 *  This API is deprecated junk, use the data model. We need to clean this out.
 *
 ****************
 */







#define HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX "/org/mugshot/datacache/"

static void
ensure_entity_registered(DBusConnection  *connection,
                         const char      *path,
                         HippoEntity     *entity)
{
    if (!hippo_dbus_helper_object_is_registered(connection, path)) {
        /* register_g_object will set up a weak ref to unregister a destroyed entity */
        hippo_dbus_helper_register_g_object(connection,
                                            path, G_OBJECT(entity),
                                            HIPPO_DBUS_ENTITY_INTERFACE,
                                            NULL);
    }
}
                         

#if 0
static HippoEntity *
entity_from_path(HippoDBus  *dbus,
                 const char *path)
{
    HippoDataCache *cache;
    HippoEntity *entity;
    
    cache = hippo_app_get_data_cache(hippo_get_app());

    if (!g_str_has_prefix(path, HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX))
        return NULL;

    entity = hippo_data_cache_lookup_entity(cache, path + strlen(HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX));

    /* generally the entity should be registered already since the path was handed out,
     * but just to be sure
     */
    ensure_entity_registered(hippo_dbus_get_connection(dbus), path, entity);
    
    return entity;
}
#endif

static char*
get_entity_path(DBusConnection  *connection,
                HippoEntity     *entity)
{
    char *path;
    
    path = g_strdup_printf(HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX "%s",
                           hippo_entity_get_guid(entity));

    /* if we're handing out the path we need to be sure it exists */
    ensure_entity_registered(connection, path, entity);

    return path;
}

static void
append_entity_to_iter(DBusConnection  *connection,
                      DBusMessageIter *iter,
                      HippoEntity     *entity)
{
    char *path;
    
    path = get_entity_path(connection, entity);
    dbus_message_iter_append_basic(iter, DBUS_TYPE_OBJECT_PATH, &path);
    g_free(path);
}

static void
append_entity_to_message(DBusConnection  *connection,
                         DBusMessage     *message,
                         HippoEntity     *entity)
{
    DBusMessageIter iter;
    
    dbus_message_iter_init_append(message, &iter);
    append_entity_to_iter(connection, &iter, entity);
}

static void
append_entity_if_in_network(void *entity_ptr, gpointer data)
{
    HippoEntity *entity = HIPPO_ENTITY(entity_ptr);
    DBusMessageIter *iter = (DBusMessageIter*) data;
    
    if (hippo_entity_get_in_network(entity)) {
        DBusConnection *connection;
        
        connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
        
        append_entity_to_iter(connection, iter, entity);
    }
}

static void
append_entity_if_person_in_network(void *entity_ptr, gpointer data)
{
    HippoEntity *entity = HIPPO_ENTITY(entity_ptr);
    DBusMessageIter *iter = (DBusMessageIter*) data;
    
    if (hippo_entity_get_in_network(entity) && HIPPO_IS_PERSON(entity)) {
        DBusConnection *connection;
        
        connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
        
        append_entity_to_iter(connection, iter, entity);
    }
}

static void
append_entity_if_group_in_network(void *entity_ptr, void *data)
{
    HippoEntity *entity = HIPPO_ENTITY(entity_ptr);
    DBusMessageIter *iter = (DBusMessageIter*) data;
    
    if (hippo_entity_get_in_network(entity) && HIPPO_IS_GROUP(entity)) {
        DBusConnection *connection;
        
        connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
        
        append_entity_to_iter(connection, iter, entity);
    }
}

static DBusMessage*
handle_get_entity_list (void            *object,
                        DBusMessage     *message,
                        DBusError       *error,
                        void (* appender) (void *entity_ptr, void *data))
{
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;
    HippoDataCache *cache;

    cache = hippo_app_get_data_cache(hippo_get_app());
    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);
    dbus_message_iter_open_container(&iter,
                                     DBUS_TYPE_ARRAY,
                                     DBUS_TYPE_OBJECT_PATH_AS_STRING,
                                     &array_iter);

    hippo_data_cache_foreach_entity(cache, appender, &array_iter);

    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

static DBusMessage*
handle_get_people (void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    return handle_get_entity_list(object, message, error, append_entity_if_person_in_network);
}

static DBusMessage*
handle_get_groups (void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    return handle_get_entity_list(object, message, error, append_entity_if_group_in_network);
}

static DBusMessage*
handle_get_self (void            *object,
                 DBusMessage     *message,
                 DBusError       *error)
{
    HippoPerson *self;
    DBusMessage *reply;
    HippoDataCache *cache;
    DBusConnection *connection;
    
    connection = hippo_dbus_get_connection(hippo_app_get_dbus(hippo_get_app()));
    cache = hippo_app_get_data_cache(hippo_get_app());

    self = hippo_data_cache_get_self(cache);
    if (self == NULL) {
    	return dbus_message_new_error(message, "org.freedesktop.od.NotOnline", "Not online right now");
    }

    reply = dbus_message_new_method_return(message);
    append_entity_to_message(connection, reply, HIPPO_ENTITY(self));
    return reply;
}

static DBusMessage*
handle_get_network (void            *object,
                    DBusMessage     *message,
                    DBusError       *error)
{
    return handle_get_entity_list(object, message, error, append_entity_if_in_network);
}

static dbus_bool_t
handle_get_type(void            *object,
                const char      *prop_name,
                DBusMessageIter *append_iter,
                DBusError       *error)
{
    const char *type;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(object);
    
    switch (hippo_entity_get_entity_type(entity)) {
    case HIPPO_ENTITY_PERSON:
        type = "person";
        break;
    case HIPPO_ENTITY_GROUP:
        type = "group";
        break;
    case HIPPO_ENTITY_RESOURCE:
        type = "resource";
        break;
    case HIPPO_ENTITY_FEED:
        type = "feed";
        break;
    default:
        g_warning("Unknown entity type %s", g_type_name_from_instance((GTypeInstance*)entity));
        type = "unknown";
        break;
    }
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &type);

    return TRUE;
}

static dbus_bool_t
handle_get_mugshot_guid(void            *object,
                        const char      *prop_name,
                        DBusMessageIter *append_iter,
                        DBusError       *error)
{
    const char *guid;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(object);

    guid = hippo_entity_get_guid(entity);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &guid);

    return TRUE;
}

static dbus_bool_t
handle_get_name(void            *object,
                const char      *prop_name,
                DBusMessageIter *append_iter,
                DBusError       *error)
{
    const char *name;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(object);

    name = hippo_entity_get_name(entity);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &name);

    return TRUE;
}

static dbus_bool_t
handle_get_home_url(void            *object,
                    const char      *prop_name,
                    DBusMessageIter *append_iter,
                    DBusError       *error)
{
    const char *home_url;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(object);

    home_url = hippo_entity_get_home_url(entity);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &home_url);

    return TRUE;
}

static dbus_bool_t
handle_get_photo_url(void            *object,
                     const char      *prop_name,
                     DBusMessageIter *append_iter,
                     DBusError       *error)
{
    const char *photo_url;
    HippoEntity *entity;

    entity = HIPPO_ENTITY(object);

    photo_url = hippo_entity_get_photo_url(entity);
    
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &photo_url);

    return TRUE;
}

static const HippoDBusMember network_members[] = {
    { HIPPO_DBUS_MEMBER_METHOD, "GetPeople", "", "ao", handle_get_people },
    { HIPPO_DBUS_MEMBER_METHOD, "GetGroups", "", "ao", handle_get_groups },
    { HIPPO_DBUS_MEMBER_METHOD, "GetSelf", "", "o", handle_get_self },
    { HIPPO_DBUS_MEMBER_METHOD, "GetNetwork", "", "ao", handle_get_network },
    { 0, NULL }
};

static const HippoDBusProperty entity_properties[] = {
    { "type", DBUS_TYPE_STRING_AS_STRING, handle_get_type, NULL },
    { "mugshotGuid", DBUS_TYPE_STRING_AS_STRING, handle_get_mugshot_guid, NULL },
    { "name", DBUS_TYPE_STRING_AS_STRING, handle_get_name, NULL },
    { "home-url", DBUS_TYPE_STRING_AS_STRING, handle_get_home_url, NULL },
    { "photo-url", DBUS_TYPE_STRING_AS_STRING, handle_get_photo_url, NULL },
    { NULL }
};

void
hippo_dbus_init_contacts(DBusConnection *connection,
                         gboolean        replace)
{
    dbus_uint32_t flags;
    
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_NETWORK_INTERFACE,
                                         network_members, NULL);
    hippo_dbus_helper_register_interface(connection, HIPPO_DBUS_ENTITY_INTERFACE,
                                         NULL, entity_properties);
    
    hippo_dbus_helper_register_object(connection, HIPPO_DBUS_NETWORK_PATH,
                                      NULL, HIPPO_DBUS_NETWORK_INTERFACE,
                                      NULL);


    /* We do want to be queued if we don't get this right away */
    flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;

    /* we just ignore errors on this */
    dbus_bus_request_name(connection, HIPPO_DBUS_NETWORK_BUS_NAME,
                          flags,
                          NULL);
}

