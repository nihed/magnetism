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

Info*       info_new_from_message (DBusMessage     *method_call);
Info*       info_new_from_data    (const char      *name,
                                   DBusMessageIter *dict_iter);
void        info_ref              (Info            *info);
void        info_unref            (Info            *info);
gboolean    info_write            (Info            *info,
                                   DBusMessageIter *dict_iter);
const char* info_get_name         (Info            *info);



SessionInfos* session_infos_new    (void);
void          session_infos_ref    (SessionInfos *infos);
void          session_infos_unref  (SessionInfos *infos);
void          session_infos_add    (SessionInfos *infos,
                                    Info         *info);
Info*         session_infos_get    (SessionInfos *infos,
                                    const char   *name);
void          session_infos_remove (SessionInfos *infos,
                                    const char   *name);


#endif /* __SESSION_INFO_H__ */

