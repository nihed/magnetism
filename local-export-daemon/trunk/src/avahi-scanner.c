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
/* strnlen */
#define _GNU_SOURCE 1
#include <config.h>
#include <stdlib.h>
#include <string.h>
#include "avahi-scanner.h"
#include "hippo-dbus-helper.h"
#include "session-info.h"
#include <dbus/dbus-glib-lowlevel.h>
#include "main.h"
#include <avahi-client/lookup.h>

typedef enum {
    SESSION_STATE_RESOLVING,
    SESSION_STATE_FAILED_RESOLVE,
    SESSION_STATE_FAILED_CONNECT,
    SESSION_STATE_CONNECTING,
    SESSION_STATE_DISCONNECTED
} SessionState;

/*
 * Information that can be used to identify a service as it is added/removed
 */
typedef struct {
    AvahiIfIndex interface;
    AvahiProtocol protocol;
    char *name;
    char *type;
    char *domain;
} ServiceId;

/* Each session may be advertised on multiple interfaces/protocols,
 * e.g. ipv4 and v6, we only merge dups after we connect since then we
 * get the session ids.
 */
typedef struct {
    int refcount;    
    SessionState state;
    ServiceId id;    
    char *host_name;
    AvahiAddress address;
    guint16 port;
    /* may be found in TXT when we resolve; if not, is found
     * when we connect.
     */
    char *machine_id;
    char *session_id;
    /* Exists only if we're connecting */
    DBusConnection *connection;
    /* Exists only if we successfully requested it from the remote session */
    SessionInfos *infos;
} Session;

static AvahiServiceBrowser *service_browser = NULL;
static GTree *session_tree_by_service = NULL;
/* static GHashTable *session_hash_by_session_id = NULL; */

static void
session_ref(Session *session)
{
    session->refcount += 1;
}

static void
session_unref(Session *session)
{
    g_assert(session->refcount > 0);
    session->refcount -= 1;
    if (session->refcount == 0) {
        g_free(session->id.name);
        g_free(session->id.type);
        g_free(session->id.domain);
        g_free(session->host_name);
        g_free(session->machine_id);
        g_free(session->session_id);
        if (session->connection != NULL)
            dbus_connection_unref(session->connection);
        if (session->infos)
            session_infos_unref(session->infos);
        g_free(session);
    }
}

static Session*
session_new(const ServiceId    *id)
{
    Session *session;

    session = g_new0(Session, 1);
    session->refcount = 1;
    session->state = SESSION_STATE_RESOLVING;
    session->id = *id;
    session->id.name = g_strdup(session->id.name);
    session->id.type = g_strdup(session->id.type);
    session->id.domain = g_strdup(session->id.domain);
    
    return session;
}

static Session*
get_session_by_service(const ServiceId *id)
{
    return g_tree_lookup(session_tree_by_service, id);
}

static void
add_session_by_service(Session *session)
{
    g_tree_insert(session_tree_by_service,
                  &session->id,
                  session);
}

static void
remove_session_by_service(Session *session)
{
    g_tree_remove(session_tree_by_service,
                  &session->id);
}

static void
session_fail_resolve(Session *session)
{
    session->state = SESSION_STATE_FAILED_RESOLVE;
    remove_session_by_service(session);
    session_unref(session); /* drop the ref owned by the tree */
}

static void
session_fail_connect(Session *session)
{
    session->state = SESSION_STATE_FAILED_CONNECT;
    /* FIXME retry again later or something */
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
session_extract_values(Session *session,
                       const DBusMessageIter *orig_dict_iter)
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
            value = NULL;
        }
        
        if (value != NULL) {
            if (strcmp("key", "session") == 0) {
                if (session->session_id == NULL) {
                    session->session_id = g_strdup(value);
                } else if (strcmp(session->session_id, value) != 0) {
                    g_debug("Session ID in TXT record does not match session ID from protocol");
                    return FALSE;
                } else {
                    g_debug("Session ID from TXT record confirmed: %s", value);
                }
            } else if (strcmp("key", "machine") == 0) {
                if (session->machine_id == NULL) {
                    session->machine_id = g_strdup(value);
                } else if (strcmp(session->machine_id, value) != 0) {
                    g_debug("Machine ID in TXT record does not match machine ID from protocol");
                    return FALSE;
                } else {
                    g_debug("Machine ID from TXT record confirmed: %s", value);
                }
            } else {
                /* some property we don't know about */
            }
        }
            
        dbus_message_iter_next(&dict_iter);
    }

    return TRUE;
}

static void
on_get_session_info_reply(DBusPendingCall *pending,
                          void            *user_data)
{
    DBusMessage *reply;
    Session *session;

    session = user_data;
    
    reply = dbus_pending_call_steal_reply(pending);

    if (reply != NULL && dbus_message_has_signature(reply, "a{sv}a(sa{sv})")) {
        DBusMessageIter iter;
        DBusMessageIter dict_iter;        
        DBusMessageIter infos_iter;
        
        g_debug("Got a reply");
        
        dbus_message_iter_init(reply, &iter);

        dbus_message_iter_recurse(&iter, &dict_iter);

        /* print_sv_dict(&dict_iter); */
        session_extract_values(session, &dict_iter);
        
        dbus_message_iter_next(&iter);
        dbus_message_iter_recurse(&iter, &infos_iter);
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
            
            /* print_sv_dict(&dict_iter); */
            info = info_new_from_data(info_name, &dict_iter);
            if (info != NULL) {
                if (session->infos == NULL)
                    session->infos = session_infos_new();
                session_infos_add(session->infos, info);
                info_unref(info);
            }
            
            dbus_message_iter_next(&infos_iter);
        }
        
    } else {
        if (reply == NULL)
            g_debug("NULL reply");
        else
            g_debug("reply had wrong signature '%s'", dbus_message_get_signature(reply));
    }

    if (reply != NULL)
        dbus_message_unref(reply);

    dbus_pending_call_unref(pending);

    g_debug("Disconnecting");
    dbus_connection_close(session->connection);
    session->connection = NULL;
    session->state = SESSION_STATE_DISCONNECTED;
    
    session_unref(session);
}

static void
session_connect(Session *session)
{
    char *dbus_address;
    char address_str[AVAHI_ADDRESS_STR_MAX];
    DBusError derror;
    DBusPendingCall *pending;
    DBusMessage *message;
    
    if (session->state == SESSION_STATE_CONNECTING)
        return;

#if 0
    if (session->id.protocol == AVAHI_PROTO_INET6) {
        /* dbus does not do IPv6 for now I don't think */
        session_fail_connect(session);
        return;
    }
#endif
    
    session->state = SESSION_STATE_CONNECTING;
    
    avahi_address_snprint(address_str, sizeof(address_str), &session->address);
    
    dbus_address = g_strdup_printf("tcp:host=%s,port=%u", address_str, session->port);

    g_debug("Connecting to '%s' protocol = %d (v4=%d v6=%d)",
            dbus_address, session->id.protocol, AVAHI_PROTO_INET, AVAHI_PROTO_INET6);
    
    dbus_error_init(&derror);
    session->connection = dbus_connection_open_private(dbus_address, &derror);
    g_free(dbus_address);
    
    if (session->connection == NULL) {
        g_debug("Failed: %s", derror.message);
        dbus_error_free(&derror); /* this is going to happen often enough that printing it isn't a good idea */
        session_fail_connect(session);
        return;
    }

    dbus_connection_setup_with_g_main(session->connection, NULL);

    g_debug("Sending call");
    
    message = dbus_message_new_method_call(NULL, SESSION_INFO_OBJECT_PATH,
                                           SESSION_INFO_INTERFACE,
                                           "GetInfoForSession");
    pending = NULL;
    dbus_connection_send_with_reply(session->connection,
                                    message,
                                    &pending,
                                    1000 * 120);
    dbus_message_unref(message);
    
    if (pending == NULL) { /* happens if we are already disconnected */
        session_fail_connect(session);
        return;
    }

    session_ref(session);
    dbus_pending_call_set_notify(pending, on_get_session_info_reply,
                                 session, NULL);
}

static char*
get_txt_record(AvahiStringList *list,
               const char      *key)
{
    AvahiStringList *found;
    char *k;
    char *v;
    size_t len;
    
    found = avahi_string_list_find(list, key);
    if (found == NULL)
        return NULL;

    k = NULL;
    v = NULL;
    len = 0;
    avahi_string_list_get_pair(found, &k, &v, &len);

    avahi_free(k);

    /* if the string isn't nul terminated then bail */
    if (v != NULL &&
        (strnlen(v, len) < len ||
         (strnlen(v, len) == len && v[len] != '\0'))) {
        avahi_free(v);
        v = NULL;
    }
    
    return v;
}

static void
resolve_callback(AvahiServiceResolver *r,
                 AvahiIfIndex interface,
                 AvahiProtocol protocol,
                 AvahiResolverEvent event,
                 const char *name,
                 const char *type,
                 const char *domain,
                 const char *host_name,
                 const AvahiAddress *address,
                 uint16_t port,
                 AvahiStringList *txt,
                 AvahiLookupResultFlags flags,
                 void* data)
{
    /* Called whenever a service has been resolved successfully or timed out */
    Session *session = data;

    switch (event) {
    case AVAHI_RESOLVER_FAILURE:
        session_fail_resolve(session);
        break;
        
    case AVAHI_RESOLVER_FOUND:
        {
            session->host_name = g_strdup(host_name);
            session->address = *address;
            session->port = port;
            
            g_debug("Resolved %s:%d", host_name, port);
            
            /* these may or may not be present. If they are, we verify their
             * accuracy later.
             */
            session->machine_id = get_txt_record(txt, "org.freedesktop.od.machine");
            session->session_id = get_txt_record(txt, "org.freedesktop.od.session");

            session_connect(session);
        }
    }
    
    avahi_service_resolver_free(r);
    /* this callback owned one ref */
    session_unref(session);
}

static void
browse_callback(AvahiServiceBrowser *b,
                AvahiIfIndex interface,
                AvahiProtocol protocol,
                AvahiBrowserEvent event,
                const char *name,
                const char *type,
                const char *domain,
                AvahiLookupResultFlags flags,
                void* data)
{    
    /* Called whenever a new services becomes available on the LAN or is removed from the LAN */
    ServiceId id;
    
    id.interface = interface;
    id.protocol = protocol;
    id.name = (char*) name;
    id.type = (char*) type;
    id.domain = (char*) domain;
    
    switch (event) {
    case AVAHI_BROWSER_FAILURE:
        /* Nothing sane to do here - help */
        g_printerr("Avahi browser failed: %s\n", avahi_strerror(avahi_client_errno(avahi_service_browser_get_client(b))));
        exit(1);
        return;

    case AVAHI_BROWSER_NEW:
        {
            AvahiServiceResolver *resolver;
            Session *session;

            g_debug("Browsed %s", name);
            
            session = get_session_by_service(&id);
            if (session != NULL) {
                /* Should not happen - we already browsed this thing. Print a warning and ignore. */
                g_warning("Avahi reported a service we already knew about?\n");
                return;
            }
            session = session_new(&id);
            add_session_by_service(session); /* the tree will own one ref */
            session_ref(session); /* the resolver callback below will own one ref */

            /* We ignore the returned resolver object. In the callback
             * function we free it. If the server is terminated before
             * the callback function is called the server will free
             * the resolver for us.
             */
            resolver = avahi_service_resolver_new(avahi_glue_get_client(),
                                                  interface, protocol, name, type, domain, AVAHI_PROTO_UNSPEC, 0,
                                                  resolve_callback, session /* callback data */);
            if (resolver == NULL) {
                g_printerr("Failed to create service resolver: '%s': %s\n", name, avahi_strerror(avahi_client_errno(avahi_glue_get_client())));
            }
        }
        break;

    case AVAHI_BROWSER_REMOVE:
        {
            Session *session;

            session = get_session_by_service(&id);
            if (session == NULL) {
                /* removed before we ever added it, I guess */
                return;
            }
            remove_session_by_service(session);
            session_unref(session);
        }
        break;

    case AVAHI_BROWSER_ALL_FOR_NOW:
    case AVAHI_BROWSER_CACHE_EXHAUSTED:
        /* fprintf(stderr, "(Browser) %s\n", event == AVAHI_BROWSER_CACHE_EXHAUSTED ? "CACHE_EXHAUSTED" : "ALL_FOR_NOW"); */
        break;
    }
}

static int
int_cmp(const int a,
        const int b)
{
    if (a < b)
        return -1;
    else if (a > b)
        return 1;
    else
        return 0;
}

static int
service_id_cmp(const void *a,
               const void *b)
{
    const ServiceId *service_id_a = a;
    const ServiceId *service_id_b = b;
    int v;

    v = int_cmp(service_id_a->interface, service_id_b->interface);
    if (v != 0)
        return v;
    v = int_cmp(service_id_a->protocol, service_id_b->protocol);
    if (v != 0)
        return v;
    /* do name first since it's most likely to be different, thus saving
     * the other comparisons. In fact type and domain will generally
     * all be the same I think.
     */
    v = strcmp(service_id_a->name, service_id_b->name);
    if (v != 0)
        return v;
    v = strcmp(service_id_a->type, service_id_b->type);
    if (v != 0)
        return v;
    v = strcmp(service_id_a->domain, service_id_b->domain);
    if (v != 0)
        return v;

    return 0;
}              

gboolean
avahi_scanner_init(void)
{
    g_assert(service_browser == NULL);

    session_tree_by_service = g_tree_new(service_id_cmp);
    
    service_browser = avahi_service_browser_new(avahi_glue_get_client(),
                                                AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC, "_freedesktop_local_export._tcp",
                                                NULL, /* domain */
                                                0, /* flags */
                                                browse_callback,
                                                NULL /* callback data */);
    
    if (service_browser == NULL) {
        g_printerr("Failed to create service browser: %s\n", avahi_strerror(avahi_client_errno(avahi_glue_get_client())));
        return FALSE;
    }
    
    return TRUE;
}

typedef struct {
    const char *info_name;
    DBusMessageIter *array_iter;
    gboolean failed;
} TraverseAndAppendData;

static gboolean
session_traverse_and_append_func(void *key,
                                 void *value,
                                 void *data)
{
    TraverseAndAppendData *taad = data;
    Session *session = value;
    Info *info;
    DBusMessageIter session_struct_iter, session_props_iter, info_dict_iter;

    info = NULL;
    if (session->infos != NULL)
        info = session_infos_get(session->infos, taad->info_name);
    if (info == NULL)
        return FALSE; /* FALSE = keep traversing */
    
    dbus_message_iter_open_container(taad->array_iter, DBUS_TYPE_STRUCT, NULL, &session_struct_iter);
    
    /* Append session properties */
    
    dbus_message_iter_open_container(&session_struct_iter, DBUS_TYPE_ARRAY, "{sv}", &session_props_iter);

    append_string_pair(&session_props_iter, "session", session->session_id);
    append_string_pair(&session_props_iter, "machine", session->machine_id);
    
    dbus_message_iter_close_container(&session_struct_iter, &session_props_iter);
    
    /* Append requested info bundle */
    
    dbus_message_iter_open_container(&session_struct_iter, DBUS_TYPE_ARRAY, "{sv}", &info_dict_iter);

    if (!info_write(info, &info_dict_iter)) {
        taad->failed = TRUE;
        return TRUE; /* stop traversing */
    }
    
    dbus_message_iter_close_container(&session_struct_iter, &info_dict_iter);
    
    dbus_message_iter_close_container(taad->array_iter, &session_struct_iter);

    return FALSE; /* keep traversing */
}

gboolean
avahi_scanner_append_infos_with_name(const char      *name,
                                     DBusMessageIter *array_iter)
{
    TraverseAndAppendData taad;

    taad.info_name = name;
    taad.array_iter = array_iter;
    taad.failed = FALSE;
    
    g_tree_foreach(session_tree_by_service, session_traverse_and_append_func, &taad);

    return !taad.failed;
}
