/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include <hippo/hippo-data-cache.h>
#include "hippo-dbus-contacts.h"
#include "main.h"

static DBusMessage*
handle_get_people (void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    g_printerr("%s\n", dbus_message_get_member(message));
    dbus_set_error(error, DBUS_ERROR_NOT_SUPPORTED,
                   "Method '%s' not yet implemented", dbus_message_get_member(message));
    return NULL;
}

static DBusMessage*
handle_get_groups (void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    g_printerr("%s\n", dbus_message_get_member(message));
    dbus_set_error(error, DBUS_ERROR_NOT_SUPPORTED,
                   "Method '%s' not yet implemented", dbus_message_get_member(message));
    return NULL;
}

static DBusMessage*
handle_get_self (void            *object,
                 DBusMessage     *message,
                 DBusError       *error)
{
    g_printerr("%s\n", dbus_message_get_member(message));
    dbus_set_error(error, DBUS_ERROR_NOT_SUPPORTED,
                   "Method '%s' not yet implemented", dbus_message_get_member(message));
    return NULL;
}

static DBusMessage*
handle_get_network (void            *object,
                    DBusMessage     *message,
                    DBusError       *error)
{
    g_printerr("%s\n", dbus_message_get_member(message));
    dbus_set_error(error, DBUS_ERROR_NOT_SUPPORTED,
                   "Method '%s' not yet implemented", dbus_message_get_member(message));
    return NULL;
}

static dbus_bool_t
handle_get_type(void            *object,
                const char      *prop_name,
                DBusMessageIter *append_iter,
                DBusError       *error)
{
    /* FIXME */
    g_printerr("Get '%s'\n", prop_name);
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &prop_name);

    return TRUE;
}

static dbus_bool_t
handle_get_mugshot_guid(void            *object,
                        const char      *prop_name,
                        DBusMessageIter *append_iter,
                        DBusError       *error)
{
    /* FIXME */
    g_printerr("Get '%s'\n", prop_name);
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &prop_name);

    return TRUE;
}

static dbus_bool_t
handle_get_name(void            *object,
                const char      *prop_name,
                DBusMessageIter *append_iter,
                DBusError       *error)
{
    /* FIXME */
    g_printerr("Get '%s'\n", prop_name);
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &prop_name);

    return TRUE;
}

static dbus_bool_t
handle_get_home_url(void            *object,
                    const char      *prop_name,
                    DBusMessageIter *append_iter,
                    DBusError       *error)
{
    /* FIXME */
    g_printerr("Get '%s'\n", prop_name);
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &prop_name);

    return TRUE;
}

static dbus_bool_t
handle_get_photo_url(void            *object,
                     const char      *prop_name,
                     DBusMessageIter *append_iter,
                     DBusError       *error)
{
    /* FIXME */
    g_printerr("Get '%s'\n", prop_name);
    dbus_message_iter_append_basic(append_iter, DBUS_TYPE_STRING, &prop_name);

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

