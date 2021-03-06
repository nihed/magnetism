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

#ifndef __SESSION_INFO_H__
#define __SESSION_INFO_H__

#include <config.h>
#include <glib.h>
#include <dbus/dbus.h>

typedef struct SessionInfos SessionInfos;
typedef struct Info Info;
typedef struct SessionChangeNotifySet SessionChangeNotifySet;

typedef gboolean (*SessionInfoMatchFunc) (Info *info,
                                          void *data);

Info*       info_new_from_message (DBusMessage     *method_call);
Info*       info_new_from_data    (const char      *name,
                                   DBusMessageIter *dict_iter);
Info*       info_ref              (Info            *info);
void        info_unref            (Info            *info);
gboolean    info_equal            (Info            *a,
                                   Info            *b);
gboolean    info_write            (Info            *info,
                                   DBusMessageIter *dict_iter);
const char* info_get_name         (Info            *info);


SessionInfos* session_infos_new               (const char *machine_id,
                                               const char *session_id);
SessionInfos* session_infos_new_with_builtins (const char *machine_id,
                                               const char *session_id);
void          session_infos_ref               (SessionInfos    *infos);
void          session_infos_unref             (SessionInfos    *infos);
void          session_infos_add               (SessionInfos    *infos,
                                               Info            *info);
Info*         session_infos_get               (SessionInfos    *infos,
                                               const char      *name);
/* Returns a newly allocated string => info hash table that is a copy of
 * all the names and infos in the hash table. Free with g_hash_table_destroy();
 * that will free the names/infos as well.
 */
GHashTable*   session_infos_get_all           (SessionInfos    *infos);
void          session_infos_remove            (SessionInfos    *infos,
                                               const char      *name);
void          session_infos_remove_all        (SessionInfos    *infos);

void          session_infos_remove_matching   (SessionInfos         *infos,
                                               SessionInfoMatchFunc  match_func,
                                               void                 *data);

gboolean      session_infos_append_all        (SessionInfos    *infos,
                                               DBusMessageIter *array_iter);
guint32       session_infos_get_change_serial (SessionInfos    *infos);

void          session_infos_churn_bogus_info  (SessionInfos    *infos);

void                    session_infos_push_change_notify_set (SessionInfos *infos);
SessionChangeNotifySet* session_infos_pop_change_notify_set  (SessionInfos *infos);

void     session_change_notify_set_free             (SessionChangeNotifySet *set);
Info*    session_change_notify_set_pop              (SessionChangeNotifySet *set);
char*    session_change_notify_set_pop_removal      (SessionChangeNotifySet *set);

gboolean session_infos_write           (SessionInfos    *infos,
                                        DBusMessageIter *session_struct_iter);
gboolean session_infos_write_with_info (SessionInfos    *infos,
                                        Info            *info,
                                        DBusMessageIter *iter);


#endif /* __SESSION_INFO_H__ */

