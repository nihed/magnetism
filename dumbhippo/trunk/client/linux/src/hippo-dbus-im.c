/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include "hippo-dbus-pidgin.h"
#include "hippo-dbus-im.h"
#include "main.h"

static DBusMessage*
handle_get_buddies(void            *object,
                   DBusMessage     *message,
                   DBusError       *error)
{
    DBusMessage *reply;
    DBusMessageIter iter, array_iter;

    reply = dbus_message_new_method_return(message);

    dbus_message_iter_init_append(reply, &iter);

    /* open an array of dict */
    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "a{sv}", &array_iter);

    hippo_pidgin_append_buddies(&array_iter);
    
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

void
hippo_dbus_im_emit_buddy_list_changed (DBusConnection *connection)
{
    hippo_dbus_helper_emit_signal(connection, HIPPO_DBUS_IM_PATH, HIPPO_DBUS_IM_INTERFACE,
                                  "BuddyListChanged", DBUS_TYPE_INVALID);
}

void
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

void
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
