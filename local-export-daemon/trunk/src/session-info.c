/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/*
 * Copyright (C) 2007 Red Hat Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
#include <config.h>
#include "session-info.h"
#include "hippo-dbus-helper.h"
#include "main.h"

static gboolean message_iter_copy(DBusMessageIter *src_iter,
                                  DBusMessageIter *dest_iter);

struct Info {
    int refcount;
    DBusMessage *message; /* signature is sa{sv} where the string is the info name and the dict is the info props */
};

struct SessionInfos {
    int refcount;
    GHashTable *by_name;
};

Info*
info_new_from_message (DBusMessage *method_call)
{
    Info *info;

    /* signature was already checked by dbus helper */
    g_assert(dbus_message_has_signature(method_call, "sa{sv}"));

    info = g_new0(Info, 1);
    info->refcount = 1;
    info->message = method_call;
    dbus_message_ref(info->message);    

    return info;
}

Info*
info_new_from_data (const char      *name,
                    DBusMessageIter *dict_iter)
{
    DBusMessage *message;
    DBusMessageIter iter, prop_iter;
    DBusMessageIter src;
    Info *info;
    
    /* create dummy message just to store stuff in */
    message = dbus_message_new_method_call("a.b.c", "/a/b", "a.b.c", "abc");

    dbus_message_iter_init_append(message, &iter);
    dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &name);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &prop_iter);
    src = *dict_iter;
    if (!message_iter_copy(&src, &prop_iter)) {
        dbus_message_unref(message);
        return NULL;
    }
    dbus_message_iter_close_container(&iter, &prop_iter);

    info = info_new_from_message(message);
    dbus_message_unref(message);

    return info;
}

void
info_ref (Info *info)
{
    info->refcount += 1;
}

void
info_unref (Info *info)
{
    g_return_if_fail(info->refcount > 0);
    info->refcount -= 1;
    if (info->refcount == 0) {
        if (info->message)
            dbus_message_unref(info->message);
        g_free(info);
    }
}

static gboolean
message_iter_copy(DBusMessageIter *src_iter,
                  DBusMessageIter *dest_iter)
{
    int type;
    DBusMessageIter next;

    type = dbus_message_iter_get_arg_type(src_iter);

    if (type == DBUS_TYPE_INVALID) {

        /* done! */
        return TRUE;

    } else if (dbus_type_is_basic(type)) {

        dbus_uint64_t value;
        dbus_message_iter_get_basic(src_iter, &value);

        if (!dbus_message_iter_append_basic(dest_iter, type, &value))
            return FALSE;

    } else if (type == DBUS_TYPE_VARIANT) {

        DBusMessageIter variant_src;
        DBusMessageIter variant_dest;
        char *signature;
        
        dbus_message_iter_recurse(src_iter, &variant_src);
        signature = dbus_message_iter_get_signature(&variant_src);

        dbus_message_iter_open_container(dest_iter, DBUS_TYPE_VARIANT,
                                         signature, &variant_dest);
        dbus_free(signature);

        if (!message_iter_copy(&variant_src, &variant_dest))
            return FALSE;
        
        dbus_message_iter_close_container(dest_iter, &variant_dest);

    } else if (type == DBUS_TYPE_DICT_ENTRY || type == DBUS_TYPE_STRUCT) {

        DBusMessageIter struct_src;
        DBusMessageIter struct_dest;

        dbus_message_iter_recurse(src_iter, &struct_src);

        dbus_message_iter_open_container(dest_iter, type, NULL, &struct_dest);
        if (!message_iter_copy(&struct_src, &struct_dest))
            return FALSE;
        dbus_message_iter_close_container(dest_iter, &struct_dest);
        
    } else if (type == DBUS_TYPE_ARRAY) {

        DBusMessageIter array_src;
        DBusMessageIter array_dest;
        char *signature;
        int element_type;
        
        dbus_message_iter_recurse(src_iter, &array_src);
        signature = dbus_message_iter_get_signature(&array_src);

        dbus_message_iter_open_container(dest_iter, DBUS_TYPE_ARRAY,
                                         signature, &array_dest);
        dbus_free(signature);

        element_type = dbus_message_iter_get_arg_type(&array_src);
        if (dbus_type_is_fixed(element_type)) {
            void *value;
            int array_len;

            dbus_message_iter_get_fixed_array(&array_src, &value, &array_len);

            if (!dbus_message_iter_append_fixed_array(&array_dest, element_type, value, array_len))
                return FALSE;
        } else {
            if (!message_iter_copy(&array_src, &array_dest))
                return FALSE;
        }
        
        dbus_message_iter_close_container(dest_iter, &array_dest);
    }

    next = *src_iter;
    dbus_message_iter_next(&next);
    return message_iter_copy(&next, dest_iter);
}

gboolean
info_write (Info            *info,
            DBusMessageIter *dict_iter)
{
    DBusMessageIter iter;
    DBusMessageIter src_iter;

    dbus_message_iter_init(info->message, &iter);
    /* skip info name */
    dbus_message_iter_next(&iter);

    /* recurse into the dict */
    dbus_message_iter_recurse(&iter, &src_iter);

    return message_iter_copy(&src_iter, dict_iter);
}

const char*
info_get_name (Info *info)
{
    DBusMessageIter iter;
    const char *name;
    
    dbus_message_iter_init(info->message, &iter);

    name = NULL;
    dbus_message_iter_get_basic(&iter, &name);

    return name;
}

SessionInfos*
session_infos_new (void)
{
    SessionInfos *infos;

    infos = g_new0(SessionInfos, 1);
    infos->refcount = 1;

    infos->by_name = g_hash_table_new(g_str_hash, g_str_equal);
    
    return infos;
}

void
session_infos_ref (SessionInfos *infos)
{
    g_return_if_fail(infos->refcount > 0);

    infos->refcount += 1;
}

static gboolean
remove_by_name_foreach (gpointer  key,
                        gpointer  value,
                        gpointer  user_data)
{
    Info *info = value;

    info_unref (info);

    return TRUE; /* TRUE means remove it */
}

void
session_infos_unref (SessionInfos *infos)
{
    g_return_if_fail(infos->refcount > 0);

    infos->refcount -= 1;
    if (infos->refcount == 0) {
        g_hash_table_foreach_remove (infos->by_name, remove_by_name_foreach, NULL);
        
        g_hash_table_destroy(infos->by_name);
        
        g_free(infos);
    }
}

void
session_infos_add (SessionInfos *infos,
                   Info         *info)
{
    const char *name;

    info_ref(info);
    
    name = info_get_name(info);
    
    session_infos_remove(infos, name);
    
    g_hash_table_replace(infos->by_name,
                         (char*) name, info);
}

Info*
session_infos_get (SessionInfos *infos,
                   const char   *name)
{
    return g_hash_table_lookup(infos->by_name, name);
}

void
session_infos_remove (SessionInfos *infos,
                      const char   *name)
{
    Info *info;
    
    info = g_hash_table_lookup(infos->by_name, name);
    if (info != NULL) {
        g_hash_table_remove(infos->by_name, name);
        info_unref(info);
    }
}
