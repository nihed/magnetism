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
#include <string.h>
#include <stdlib.h>
#include "avahi-scanner.h"
#include "hippo-avahi-helper.h"
#include "hippo-dbus-helper.h"
#include "hippo-dbus-async.h"
#include "session-info.h"
#include "session-api.h"
#include <dbus/dbus-glib-lowlevel.h>
#include "main.h"
#include <avahi-client/lookup.h>

typedef struct _InfoRetrieval InfoRetrieval;
typedef struct _Session       Session;

/* Information about an in-flight retrieval of the information we obtain by connecting
 * to a local-export service
 */
struct _InfoRetrieval {
    DBusConnection *connection;
    Session *session;
};

/* All information associated with a particular session ID; this may be merged from
 * multiple independent services
 */
struct _Session {
    char *session_id;

    HippoAvahiService *local_export_service;
    char *machine_id;

    unsigned long change_serial;
    SessionInfos *infos;
    InfoRetrieval *info_retrieval;

    HippoAvahiService *dav_service;
    char *dav_url;
};

static GHashTable *sessions_by_id = NULL;

static HippoAvahiSessionBrowser *local_export_browser = NULL;

static void session_info_retrieval_succeeded(Session     *session,
                                             DBusMessage *reply);
static void session_info_retrieval_failed   (Session *session);

static Session *
get_session(const char *id)
{
    return g_hash_table_lookup(sessions_by_id, id);
}

static Session *
ensure_session(const char *id)
{
    Session *session = g_hash_table_lookup(sessions_by_id, id);
    if (session == NULL) {
        session = g_new0(Session, 1);
        session->session_id = g_strdup(id);

        g_hash_table_insert(sessions_by_id, session->session_id, session);
    }

    return session;
}

static void
remove_session(const char *id)
{
    Session *session = g_hash_table_lookup(sessions_by_id, id);
    if (session == NULL) {
        g_warning("remove_session called on non-existent session");
        return;
    }

    g_hash_table_remove(sessions_by_id, id);

    g_assert(session->local_export_service == NULL);
    g_assert(session->dav_service == NULL);
    g_assert(session->info_retrieval == NULL);
    
    g_free(session->machine_id);
    g_free(session->session_id);
    if (session->infos)
        session_infos_unref(session->infos);

    g_free(session);
}

static void
info_retrieval_free(InfoRetrieval *retrieval)
{
    if (retrieval->connection) {
        g_debug("Disconnecting");

        dbus_connection_close(retrieval->connection);
        dbus_connection_unref(retrieval->connection);
    }
        
    g_free(retrieval);
}

static void
info_retrieval_on_get_session_info_reply(DBusPendingCall *pending,
                                         void            *user_data)
{
    DBusMessage *reply;
    InfoRetrieval *retrieval;

    retrieval = user_data;

    reply = dbus_pending_call_steal_reply(pending);

    if (reply != NULL) {
        if (retrieval->session)
            session_info_retrieval_succeeded(retrieval->session, reply);
        
        dbus_message_unref(reply);
    } else if (reply == NULL) {
        g_debug("NULL reply");
    }

    dbus_pending_call_unref(pending);

    info_retrieval_free(retrieval);
}

static void
info_retrieval_connection_opened_handler(DBusConnection  *connection_or_null,
                                         const DBusError *error_if_null,
                                         void            *data)
{
    InfoRetrieval *retrieval;
    DBusPendingCall *pending;
    DBusMessage *message;

    retrieval = data;

    if (connection_or_null == NULL) {
        g_debug("Failed to connect to local export service: %s", error_if_null->message);
        if (retrieval->session)
            session_info_retrieval_failed(retrieval->session);
        info_retrieval_free(retrieval);
        return;
    }

    if (retrieval->session == NULL) {
        info_retrieval_free(retrieval);
        return;
    }

    retrieval->connection = connection_or_null;
    dbus_connection_ref(retrieval->connection);
    dbus_connection_setup_with_g_main(retrieval->connection, NULL);

    g_debug("Sending GetInfoForSession message");
    
    message = dbus_message_new_method_call(NULL, SESSION_INFO_OBJECT_PATH,
                                           SESSION_INFO_INTERFACE,
                                           "GetInfoForSession");
    pending = NULL;
    dbus_connection_send_with_reply(retrieval->connection,
                                    message,
                                    &pending,
                                    1000 * 120);
    dbus_message_unref(message);
    
    if (pending == NULL) { /* Shouldn't happen */
        g_debug("Failed to send GetInfoForSession message");
        session_info_retrieval_failed(retrieval->session);
        info_retrieval_free(retrieval);
        return;
    }

    /* pass refcount on "pending" in here */
    dbus_pending_call_set_notify(pending, info_retrieval_on_get_session_info_reply,
                                 retrieval, NULL);
}

static InfoRetrieval *
info_retrieval_start(Session *session)
{
    AvahiAddress address;
    char address_str[AVAHI_ADDRESS_STR_MAX];
    guint16 port;
    char *dbus_address;
    InfoRetrieval *retrieval;

    hippo_avahi_service_get_address(session->local_export_service, &address);
    port = hippo_avahi_service_get_port(session->local_export_service);
    
    avahi_address_snprint(address_str, sizeof(address_str), &address);
    
    dbus_address = g_strdup_printf("tcp:host=%s,port=%u", address_str, port);

    g_debug("Connecting to '%s' protocol = %d (v4=%d v6=%d)",
            hippo_avahi_service_get_name(session->local_export_service),
            address.proto, AVAHI_PROTO_INET, AVAHI_PROTO_INET6);

    retrieval = g_new0(InfoRetrieval, 1);
    retrieval->session = session;
    hippo_dbus_connection_open_private_async(dbus_address, info_retrieval_connection_opened_handler,
                                             retrieval);
    g_free(dbus_address);

    return retrieval;
}

static void
info_retrieval_cancel(InfoRetrieval *retrieval)
{
    /* We just flag the retrieval as canceled by removing the session pointer,
     * from it, we otherwise let it proceed to completion.
     */
    retrieval->session = NULL;
}

static void
print_sv_dict(const DBusMessageIter *orig_dict_iter)
{
    DBusMessageIter dict_iter;

    dict_iter = *orig_dict_iter;
    while (dbus_message_iter_get_arg_type(&dict_iter) == DBUS_TYPE_DICT_ENTRY) {
        DBusMessageIter entry_iter;
        DBusMessageIter variant_iter;
        const char *key;
        const char *value;
        
        dbus_message_iter_recurse(&dict_iter, &entry_iter);

        key = NULL;
        dbus_message_iter_get_basic(&entry_iter, &key);
        dbus_message_iter_next(&entry_iter);

        g_assert(dbus_message_iter_get_arg_type(&entry_iter) == DBUS_TYPE_VARIANT);
        dbus_message_iter_recurse(&entry_iter, &variant_iter);

        if (dbus_message_iter_get_arg_type(&variant_iter) == DBUS_TYPE_STRING) {
            dbus_message_iter_get_basic(&variant_iter, &value);
        } else {
            value = "?";
        }
        
        g_debug("prop '%s'='%s'", key, value);
            
        dbus_message_iter_next(&dict_iter);
    }
}

static gboolean
session_extract_values(Session               *session,
                       const DBusMessageIter *orig_dict_iter)
{
    DBusMessageIter dict_iter;

    /* The 'values' here are a shadow in the local-export protocol of the properties also provided in
     * the TXT record. This in theory allows us to conform to a "SHOULD" on the dns-sd
     * spec and avoid depending on the TXT record for the local-export service, but since we use
     * the same infrastructure with the session ID stored in the TXT record for webdav,
     * trying to use the session ID from the protocol would complicate things. So we check
     * to see that the values match and warn on mismatch, but otherwise ignore them.
     */
    
    dict_iter = *orig_dict_iter;
    while (dbus_message_iter_get_arg_type(&dict_iter) == DBUS_TYPE_DICT_ENTRY) {
        DBusMessageIter entry_iter;
        DBusMessageIter variant_iter;
        const char *key;
        const char *value;
        
        dbus_message_iter_recurse(&dict_iter, &entry_iter);

        key = NULL;
        dbus_message_iter_get_basic(&entry_iter, &key);
        dbus_message_iter_next(&entry_iter);

        g_assert(dbus_message_iter_get_arg_type(&entry_iter) == DBUS_TYPE_VARIANT);
        dbus_message_iter_recurse(&entry_iter, &variant_iter);

        if (dbus_message_iter_get_arg_type(&variant_iter) == DBUS_TYPE_STRING) {
            dbus_message_iter_get_basic(&variant_iter, &value);
        } else {
            value = NULL;
        }
        
        if (value != NULL) {
            if (strcmp("key", "session") == 0) {
                if (strcmp(session->session_id, value) != 0)
                    g_warning("Session ID in TXT record does not match session ID from protocol, or ID was changed");
            } else if (strcmp("key", "machine") == 0) {
                if (session->machine_id == NULL || (strcmp(session->machine_id, value) != 0))
                    g_warning("Machine ID in TXT record does not match machine ID from protocol, or ID was changed");
            }
        }
            
        dbus_message_iter_next(&dict_iter);
    }

    return TRUE;
}

static void
remove_name_from_infos(void *key,
                       void *value,
                       void *data)
{
    SessionInfos *infos = data;
    
    session_infos_remove(infos, key);
}

static void
session_info_retrieval_succeeded(Session     *session,
                                 DBusMessage *reply)
{
    SessionChangeNotifySet *change_set;
    DBusMessageIter iter;
    DBusMessageIter dict_iter;        
    DBusMessageIter infos_iter;
    GHashTable *old_infos;
    
    session->info_retrieval = NULL;

    if (!dbus_message_has_signature(reply, "a{sv}a(sa{sv})")) {
        g_debug("reply had wrong signature '%s'", dbus_message_get_signature(reply));
        return;
    }
    
    dbus_message_iter_init(reply, &iter);
    
    dbus_message_iter_recurse(&iter, &dict_iter);
    
    /* print_sv_dict(&dict_iter); */
    session_extract_values(session, &dict_iter);

    if (session->infos == NULL) {
        g_assert(session->machine_id);
        g_assert(session->session_id);
        session->infos = session_infos_new(session->machine_id,
                                           session->session_id);
    }
    
    dbus_message_iter_next(&iter);
    dbus_message_iter_recurse(&iter, &infos_iter);
    
    /* Remember what was there, so we can remove anything we don't see from this message */
    old_infos = session_infos_get_all(session->infos);
        
    session_infos_push_change_notify_set(session->infos);
    
    while (dbus_message_iter_get_arg_type(&infos_iter) == DBUS_TYPE_STRUCT) {
        DBusMessageIter info_iter;
        const char *info_name;
        Info *info;
        
        dbus_message_iter_recurse(&infos_iter, &info_iter);
        
        info_name = NULL;
        dbus_message_iter_get_basic(&info_iter, &info_name);
        
        dbus_message_iter_next(&info_iter);
        
        g_debug("Got info '%s':", info_name);
        dbus_message_iter_recurse(&info_iter, &dict_iter);
        
        print_sv_dict(&dict_iter);
        info = info_new_from_data(info_name, &dict_iter);
        if (info != NULL) {
            session_infos_add(session->infos, info);
            info_unref(info);
        }
        
        /* We've seen the key, remove it from the set of infos to remove */
        g_hash_table_remove(old_infos, info_name);
        
        dbus_message_iter_next(&infos_iter);
    }
    
    /* Now remove any names in old_infos that we didn't see */
    g_hash_table_foreach(old_infos, remove_name_from_infos, session->infos);
    g_hash_table_destroy(old_infos);
    
    change_set = session_infos_pop_change_notify_set(session->infos);
    session_api_notify_changed(session->infos, change_set);
    session_change_notify_set_free(change_set);
}

static void
session_info_retrieval_failed(Session *session)
{
    session->info_retrieval = NULL;
    
    /* FIXME retry again later or something */
}

static void
session_connect(Session *session)
{
    if (session->info_retrieval) {
        /* Cancel any outstanding retrieval */
        
        info_retrieval_cancel(session->info_retrieval);
        session->info_retrieval = NULL;
    }

    session->info_retrieval = info_retrieval_start(session);
}

static guint32
get_txt_property_uint32(HippoAvahiService *service,
                        const char        *key)
{
    char *s;
    guint64 l;
    char *end;
    s = hippo_avahi_service_get_txt_property(service, key);
    if (s == NULL)
        return 0;

    end = NULL;
    l = g_ascii_strtoull(s, &end, 10);
    if (end == s)
        l = 0;

    if (l > G_MAXUINT32)
        l = G_MAXUINT32;
    
    g_free(s);
    return l;
}

static void
on_service_address_changed(HippoAvahiService *service,
                           Session           *session)
{
    /* The address or port changed, reconnect to the local export daemon and get new values */
    
    session_connect(session);
}
    
static void
on_service_txt_changed(HippoAvahiService *service,
                       Session           *session)
{
    guint32 new_serial;
    char *new_machine_id;

    new_machine_id = hippo_avahi_service_get_txt_property(service, "org.freedesktop.od.machine");
    
    /* For now we warn and ignore the change */
    if (new_machine_id && session->machine_id &&
        strcmp(new_machine_id, session->machine_id) != 0) {
        g_printerr("Remote session changed its machine ID, which is not allowed\n");
    }
    g_free(new_machine_id);
    
    new_serial = get_txt_property_uint32(service, "org.freedesktop.od.serial");
    
    g_debug("new change serial %u", new_serial);
    
    if (new_serial > 0 &&
        new_serial != session->change_serial) {
        /* Connect again */
        session_connect(session); 
    }
}
    
static void
on_local_export_service_added(HippoAvahiBrowser *browser,
                              HippoAvahiService *service)
{
    Session *session = ensure_session(hippo_avahi_service_get_session_id(service));

    if (session->local_export_service) {
        g_warning("local service added when we already had one for that ID");
        return;
    }

    session->local_export_service = service;
    
    session->machine_id = hippo_avahi_service_get_txt_property(service, "org.freedesktop.od.machine");
    g_signal_connect(service, "address-changed",
                     G_CALLBACK(on_service_address_changed), session);
    g_signal_connect(service, "txt-changed",
                     G_CALLBACK(on_service_txt_changed), session);

    session_connect(session);
}

static void
on_local_export_service_removed(HippoAvahiBrowser *browser,
                                HippoAvahiService *service)
{
    Session *session = get_session(hippo_avahi_service_get_session_id(service));
    SessionChangeNotifySet *change_set;

    if (session == NULL) {
        g_warning("local export service removed we don't know about");
        return;
    }

    if (session->local_export_service != service) {
        g_warning("Mismatch between removed service and service cached in session");
        return;
    }

    g_signal_handlers_disconnect_by_func(service, 
                                         (void *)on_service_address_changed,
                                         session);
    g_signal_handlers_disconnect_by_func(service, 
                                         (void *)on_service_txt_changed,
                                         session);

    if (session->info_retrieval) {
        info_retrieval_cancel(session->info_retrieval);
        session->info_retrieval = NULL;
    }

    /* Remove all the "info" and send notification */
    if (session->infos) {
        session_infos_push_change_notify_set(session->infos);
        session_infos_remove_all(session->infos);
        change_set = session_infos_pop_change_notify_set(session->infos);
        session_api_notify_changed(session->infos, change_set);
        session_change_notify_set_free(change_set);
    }    

    session->local_export_service = NULL;
    
    g_free(session->machine_id);
    session->machine_id = NULL;

    if (session->dav_service == NULL)
        remove_session(session->session_id);
}

gboolean
avahi_scanner_init(void)
{
    g_assert(local_export_browser == NULL);

    sessions_by_id = g_hash_table_new(g_str_hash, g_str_equal);

    local_export_browser = hippo_avahi_session_browser_new("_freedesktop_local_export._tcp");
    g_signal_connect(local_export_browser, "service-added",
                     G_CALLBACK(on_local_export_service_added), NULL);
    g_signal_connect(local_export_browser, "service-removed",
                     G_CALLBACK(on_local_export_service_removed), NULL);
    
    return TRUE;
}

typedef struct {
    const char *info_name;
    DBusMessageIter *array_iter;
    gboolean failed;
} ForeachAppendData;

static void
session_foreach_append_func(void              *key,
                            void              *value,
                            void              *data)
{
    ForeachAppendData *fad = data;
    Session *session = value;
    Info *info;

    if (fad->failed)
        return;
    
    info = NULL;
    if (session->infos != NULL)
        info = session_infos_get(session->infos, fad->info_name);
    if (info == NULL)
        return;
    
    if (!session_infos_write_with_info(session->infos,
                                       info,
                                       fad->array_iter)) {
        fad->failed = TRUE;
    }
}
    

gboolean
avahi_scanner_append_infos_with_name(const char      *name,
                                     DBusMessageIter *array_iter)
{
    ForeachAppendData fad;

    fad.info_name = name;
    fad.array_iter = array_iter;
    fad.failed = FALSE;
    
    g_hash_table_foreach(sessions_by_id, session_foreach_append_func, &fad);

    return !fad.failed;
}
