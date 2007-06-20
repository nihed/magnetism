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
#include <unistd.h>
#include <sys/types.h>
#include <time.h>
#include <string.h>
#include "session-info.h"
#include "hippo-dbus-helper.h"
#include "main.h"

static gboolean message_iter_copy (DBusMessageIter *src_iter,
                                   DBusMessageIter *dest_iter);
static gboolean message_iter_equal(DBusMessageIter *lhs,
                                   DBusMessageIter *rhs);

struct Info {
    int refcount;
    DBusMessage *message; /* signature is sa{sv} where the string is the info name and the dict is the info props */
};

struct SessionInfos {
    int refcount;
    GHashTable *by_name;
    guint32 serial;
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

/**
 * A simple 8-byte value union that lets you access 8 bytes as if they
 * were various types; useful when dealing with basic types via
 * void pointers and varargs.
 */
typedef union
{
  dbus_int16_t  i16;   /**< as int16 */
  dbus_uint16_t u16;   /**< as int16 */
  dbus_int32_t  i32;   /**< as int32 */
  dbus_uint32_t u32;   /**< as int32 */
  dbus_int64_t  i64;   /**< as int64 */
  dbus_uint64_t u64;   /**< as int64 */
  double dbl;          /**< as double */
  unsigned char byt;   /**< as byte */
  char *str;           /**< as char* */
} BasicValue;

static void
basic_value_zero (BasicValue *value)
{
    value->u64 = 0;
}

static dbus_bool_t
basic_value_equal (int             type,
                   BasicValue     *lhs,
                   BasicValue     *rhs)
{
    if (type == DBUS_TYPE_STRING ||
        type == DBUS_TYPE_SIGNATURE ||
        type == DBUS_TYPE_OBJECT_PATH) {
        return strcmp (lhs->str, rhs->str) == 0;
    } else {
        return lhs->u64 == rhs->u64;
    }
}

static gboolean
single_complete_type_iter_equal(DBusMessageIter *lhs,
                                DBusMessageIter *rhs)
{
    int lhs_type;
    int rhs_type;

    lhs_type = dbus_message_iter_get_arg_type (lhs);
    rhs_type = dbus_message_iter_get_arg_type (rhs);

    if (lhs_type != rhs_type)
        return FALSE;

    if (lhs_type == DBUS_TYPE_INVALID)
        return TRUE;

    if (dbus_type_is_basic (lhs_type)) {
        BasicValue lhs_value;
        BasicValue rhs_value;

        basic_value_zero (&lhs_value);
        basic_value_zero (&rhs_value);
      
        dbus_message_iter_get_basic (lhs, &lhs_value);
        dbus_message_iter_get_basic (rhs, &rhs_value);

        return basic_value_equal (lhs_type, &lhs_value, &rhs_value);
    } else {
        DBusMessageIter lhs_sub;
        DBusMessageIter rhs_sub;

        dbus_message_iter_recurse (lhs, &lhs_sub);
        dbus_message_iter_recurse (rhs, &rhs_sub);

        return message_iter_equal (&lhs_sub, &rhs_sub);
    }
}

static gboolean
message_iter_equal(DBusMessageIter *lhs,
                   DBusMessageIter *rhs)
{
    DBusMessageIter lhs_next;
    DBusMessageIter rhs_next;
    int lhs_type;
    int rhs_type;
    
    if (!single_complete_type_iter_equal(lhs, rhs))
        return FALSE;

    lhs_type = dbus_message_iter_get_arg_type (lhs);
    rhs_type = dbus_message_iter_get_arg_type (rhs);

    g_assert (lhs_type == rhs_type);

    if (lhs_type == DBUS_TYPE_INVALID)
        return TRUE;
        
    lhs_next = *lhs;
    rhs_next = *rhs;
    dbus_message_iter_next(&lhs_next);
    dbus_message_iter_next(&rhs_next);
    return message_iter_equal(&lhs_next, &rhs_next);
}

gboolean
info_equal(Info *a,
           Info *b)
{
    if (a == b)
        return TRUE;

    if (a->message && b->message) {
        DBusMessageIter a_iter;
        DBusMessageIter b_iter;

        dbus_message_iter_init (a->message, &a_iter);
        dbus_message_iter_init (b->message, &b_iter);

        return message_iter_equal(&a_iter, &b_iter);
    } else {
        return a->message == b->message; /* both NULL would be caught here */
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

    /* starting with a time means we have a chance to get ordering
     * right across restarts of the daemon, though obviously if the
     * clock is messed with this will break down
     */
    infos->serial = time(NULL);
    
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
    Info *old;
    
    info_ref(info);
    
    name = info_get_name(info);

    old = session_infos_get(infos, name);

    if (old && info_equal(old, info)) {
        g_debug("%s did not really change (new info equals old)", name);
        return;
    }
    
    if (old != NULL) {
        session_infos_remove(infos, name);
    }
    
    g_hash_table_replace(infos->by_name,
                         (char*) name, info);

    infos->serial += 1;
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
        infos->serial += 1;
    }
}

typedef struct {
    DBusMessageIter *array_iter;
    gboolean failed;
} AppendAllData;

static void
append_all_foreach (gpointer  key,
                    gpointer  value,
                    gpointer  user_data)
{
    Info *info = value;
    AppendAllData *aad = user_data;
    DBusMessageIter struct_iter, dict_iter;
    const char *info_name;
    
    if (aad->failed)
        return;
    
    dbus_message_iter_open_container(aad->array_iter, DBUS_TYPE_STRUCT, NULL, &struct_iter);

    info_name = info_get_name(info);
    dbus_message_iter_append_basic(&struct_iter, DBUS_TYPE_STRING, &info_name);

    dbus_message_iter_open_container(&struct_iter, DBUS_TYPE_ARRAY, "{sv}", &dict_iter);

    if (!info_write(info, &dict_iter)) {
        aad->failed = TRUE;
        return;
    }

    dbus_message_iter_close_container(&struct_iter, &dict_iter);
    
    dbus_message_iter_close_container(aad->array_iter, &struct_iter);    
}

gboolean
session_infos_append_all (SessionInfos    *infos,
                          DBusMessageIter *array_iter)
{
    AppendAllData aad;

    g_assert(infos != NULL);
    g_assert(array_iter != NULL);
    
    aad.array_iter = array_iter;
    aad.failed = FALSE;

    g_hash_table_foreach (infos->by_name, append_all_foreach, &aad);
    
    return !aad.failed;
}

typedef void (* AppendBuiltinFunc) (DBusMessageIter *dict_iter);

static void
append_unix_account_info(DBusMessageIter *dict_iter)
{
    char *s;
    
    append_string_pair(dict_iter, "name", g_get_real_name());

    s = g_strdup_printf("%d", (int) getuid());
    append_string_pair(dict_iter, "uid", s);
    g_free(s);
}

static void
add_builtin_info(SessionInfos     *infos,
                 const char       *name,
                 AppendBuiltinFunc func)
{
    DBusMessage *message;
    DBusMessageIter iter, prop_iter;
    Info *info;
    
    /* create dummy message just to store stuff in */
    message = dbus_message_new_method_call("a.b.c", "/a/b", "a.b.c", "abc");

    dbus_message_iter_init_append(message, &iter);
    dbus_message_iter_append_basic(&iter, DBUS_TYPE_STRING, &name);

    dbus_message_iter_open_container(&iter, DBUS_TYPE_ARRAY, "{sv}", &prop_iter);

    (* func) (&prop_iter);

    dbus_message_iter_close_container(&iter, &prop_iter);

    info = info_new_from_message(message);
    dbus_message_unref(message);

    session_infos_add(infos, info);
    info_unref(info);
}

/* Create a SessionInfos with some builtin default info bundles */
SessionInfos*
session_infos_new_with_builtins (void)
{
    SessionInfos *infos;

    infos = session_infos_new();
    add_builtin_info(infos,
                     "org.freedesktop.od.UnixAccount", append_unix_account_info);

    return infos;
}

guint32
session_infos_get_change_serial(SessionInfos *infos)
{
    return infos->serial;
}

static void
append_bogus_info(DBusMessageIter *dict_iter)
{
    char *s;
    
    append_string_pair(dict_iter, "name", g_get_real_name());

    s = g_strdup_printf("Time %d, churn is %u", (int) time(NULL),
                        g_random_int());
    append_string_pair(dict_iter, "time", s);
    g_free(s);
}

void
session_infos_churn_bogus_info (SessionInfos *infos)
{
    g_debug("Modifying random churn info");
    add_builtin_info(infos,
                     "org.freedesktop.od.BogusTestInfo", append_bogus_info);
}
