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

#ifndef __MAIN_H__
#define __MAIN_H__

#include <config.h>
#include <glib.h>
#include <dbus/dbus.h>

/* interface exported to the session bus */
#define LOCAL_EXPORT_BUS_NAME "org.freedesktop.od.LocalExport"
#define LOCAL_EXPORT_INTERFACE "org.freedesktop.od.LocalExport"
#define LOCAL_EXPORT_OBJECT_PATH "/org/freedesktop/od/LocalExport"

/* interface exported to the LAN containing info on our session */
#define SESSION_INFO_INTERFACE "org.freedesktop.od.SessionInfo"
#define SESSION_INFO_OBJECT_PATH "/org/freedesktop/od/SessionInfo"

void get_machine_and_session_ids(const char **machine_id_p,
                                 const char **session_id_p);

void append_string_pair(DBusMessageIter *dict_iter,
                        const char      *key,
                        const char      *value);

#endif /*  __MAIN_H__ */

