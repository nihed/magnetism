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
#include <stdlib.h>
#include <avahi-client/publish.h>
#include "main.h"
#include "avahi-advertiser.h"
#include "tcp-listener.h"
#include "session-api.h"

static AvahiEntryGroup *entry_group = NULL;
static char *current_entry_group_name = NULL;
static int name_number = 0;
static int last_tried_name_number = -1;
static int listening_on_port = -1;
static guint32 last_published_serial = 0;
static gboolean republish_pending = FALSE;

static AvahiStringList*
new_text_record_list(const char *first_key,
                     const char *first_value,
                     ...)
{
    va_list args;
    const char *k;
    const char *v;
    AvahiStringList *list;

    if (first_key == NULL)
        return NULL;

    list = NULL;
    
    list = avahi_string_list_add_pair(list, first_key, first_value);
    
    va_start(args, first_value);
    k = va_arg(args, const char*);
    if (k)
        v = va_arg(args, const char*);
    while (k != NULL) {
        list = avahi_string_list_add_pair(list, k, v);
        
        k = va_arg(args, const char*);
        if (k)
            v = va_arg(args, const char*);
    }
    
    va_end(args);
    
    return list;
}

static AvahiStringList*
get_txt_records(void)
{
    AvahiStringList *txt_records;
    const char *machine_id;
    const char *session_id;
    char *serial;
    
    get_machine_and_session_ids(&machine_id, &session_id);

    serial = g_strdup_printf("%u", session_api_get_change_serial());
    
    /* Guidelines at http://www.zeroconf.org/Rendezvous/txtrecords.html say 1300 bytes is max
     * size of all the TXT records that's a good idea, and smaller is better
     */
    
    txt_records = new_text_record_list("org.freedesktop.od.machine", machine_id,
                                       "org.freedesktop.od.session", session_id,
                                       "org.freedesktop.od.serial", serial,
                                        NULL);
    g_free(serial);

    return txt_records;
}

static gboolean
avahi_advertiser_add_services(void)
{
    int result;
    char *name;
    AvahiStringList *txt_records;
    
    if (last_tried_name_number < name_number) {
        last_tried_name_number = name_number;
    } else {
        /* already done */
        return TRUE;
    }

    if (last_tried_name_number == 0) {
        name = g_strdup_printf("%s's Stuff", g_get_real_name());
    } else {
        name = g_strdup_printf("%s's Stuff (%d)", g_get_real_name(), last_tried_name_number);
    }

    g_assert (listening_on_port >= 0);

    txt_records = get_txt_records();
    
    result = avahi_entry_group_add_service_strlst(entry_group, AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC,
                                                  0, /* flags */
                                                  name, /* "user visible" name of the thing, like "Joe's Files" or whatever */
                                                  "_freedesktop_local_export._tcp", /* DNS service type */
                                                  NULL, /* domain, NULL for default */
                                                  NULL, /* host, NULL for default */
                                                  listening_on_port, /* port we are listening on */
                                                  txt_records);
    avahi_string_list_free(txt_records);
    
    if (result < 0) {
        g_printerr("avahi_entry_group_add_service() failed: %s\n", avahi_strerror(result));
        goto failed;
    }

    /* Go! */
    result = avahi_entry_group_commit(entry_group);
    if (result < 0) {
        g_printerr("avahi_entry_group_commit() failed: %s\n", avahi_strerror(result));
        goto failed;
    }

    last_published_serial = session_api_get_change_serial();
    current_entry_group_name = name;
    
    return TRUE;

 failed:
    avahi_entry_group_free(entry_group);
    g_free(name);
    entry_group = NULL;
    g_free(current_entry_group_name);
    current_entry_group_name = NULL;
    return FALSE;
}


static void
entry_group_callback(AvahiEntryGroup *g,
                     AvahiEntryGroupState state,
                     void *data)
{
    /* Called whenever the entry group state changes */

    switch (state) {
    case AVAHI_ENTRY_GROUP_ESTABLISHED:
        /* The entry group has been established successfully */
        break;

    case AVAHI_ENTRY_GROUP_COLLISION:
        ++name_number;
        avahi_advertiser_add_services();
        break;
        
    case AVAHI_ENTRY_GROUP_FAILURE :

        /* Not sure when this would happen or what we are supposed to do about it */
        g_printerr("Entry group failure: %s\n",
                   avahi_strerror(avahi_client_errno(avahi_entry_group_get_client(g))));
        exit(1);
        break;

    case AVAHI_ENTRY_GROUP_UNCOMMITED:
    case AVAHI_ENTRY_GROUP_REGISTERING:
        break;
    }
}

gboolean
avahi_advertiser_ensure_entry_group(AvahiClient *client)
{
    if (entry_group == NULL) {
        entry_group = avahi_entry_group_new(client, entry_group_callback, NULL /* data */);
        if (entry_group == NULL) {
            g_printerr("avahi_entry_group_new() failed: %s\n", avahi_strerror(avahi_client_errno(client)));
            return FALSE;
        }
    }

    g_assert(entry_group != NULL);
    
    if (!avahi_advertiser_add_services())
        return FALSE;

    return TRUE;
}

/* This happens when the systemwide daemon is not ready to publish
 * services yet
 */
void
avahi_advertiser_reset_entry_group(void)
{
    if (entry_group) {
        avahi_entry_group_reset(entry_group);
        avahi_entry_group_free(entry_group);
        entry_group = NULL;
    }
}

gboolean
avahi_advertiser_init(void)
{
    /* avahi_glue_init() hasn't been called here, so we can't do any avahi stuff */
    
    if (!tcp_listener_init())
        return FALSE;
    
    listening_on_port = tcp_listener_get_port();

    return TRUE;
}

static gboolean
republish_idle(void *data)
{
    guint32 current_serial;
    
    if (entry_group != NULL) {
        current_serial = session_api_get_change_serial();

        g_debug("Consider republish, last %u new %u",
                last_published_serial, current_serial);
        
        if (current_serial != last_published_serial) {
            AvahiStringList *txt_records;
            int result;
            
            txt_records = get_txt_records();

            result = avahi_entry_group_update_service_txt_strlst(entry_group,
                                                                 AVAHI_IF_UNSPEC,
                                                                 AVAHI_PROTO_UNSPEC,
                                                                 0,
                                                                 current_entry_group_name,
                                                                 "_freedesktop_local_export._tcp", /* DNS service type */
                                                                 NULL, /* domain, NULL for default */
                                                                 txt_records);
            if (result < 0) {
                g_printerr("avahi_entry_group_update_service_txt_strlist() failed: %s\n",
                           avahi_strerror(result));
            } else {
                g_debug("Updated txt records");
                last_published_serial = current_serial;
            }

            avahi_string_list_free(txt_records);
        }
    }

    /* remove the idle */
    republish_pending = FALSE;
    return FALSE;
}

void
avahi_advertiser_queue_republish(void)
{
    if (republish_pending)
        return;
    
    g_idle_add(republish_idle, NULL);
    republish_pending = TRUE;
}
    
