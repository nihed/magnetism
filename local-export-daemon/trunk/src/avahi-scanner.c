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
#include <stdlib.h>
#include "avahi-scanner.h"
#include "hippo-dbus-helper.h"
#include "hippo-dbus-async.h"
#include "session-info.h"
#include "session-api.h"
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

typedef struct {
    int refcount;    
    ServiceId id;    
    AvahiServiceResolver *resolver;
} Resolver;

/* FIXME Each session may be advertised on multiple
 * interfaces/protocols, e.g. ipv4 and v6. Also, hostile peers might
 * forge duplicate session IDs for two sessions that are really
 * distinct. Right now we really aren't dealing with either of these
 * things; we just pass through the dups onto the session bus and
 * local apps.
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
    /* We only store this when we connect and get the new information,
     * i.e. it's the latest changes we have
     */
    unsigned long change_serial;
    /* Exists only if we're connecting */
    DBusConnection *connection;
    /* Exists only if we successfully requested it from the remote session */
    SessionInfos *infos;
} Session;

static AvahiServiceBrowser *service_browser = NULL;
static GTree *session_tree_by_service = NULL;
static GTree *resolver_tree_by_service = NULL;
/* static GHashTable *session_hash_by_session_id = NULL; */

static void
service_id_free_fields(ServiceId *id)
{
    g_free(id->name);
    g_free(id->type);
    g_free(id->domain);
}

static void
service_id_init_fields(ServiceId       *id,
                       const ServiceId *orig)
{
    *id = *orig;
    id->name = g_strdup(orig->name);
    id->type = g_strdup(orig->type);
    id->domain = g_strdup(orig->domain);
}

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
        service_id_free_fields(&session->id);
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
    service_id_init_fields(&session->id, id);
    
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
    session_ref(session);
}

static void
remove_session_by_service(Session *session)
{
    SessionChangeNotifySet *change_set;
    
    g_tree_remove(session_tree_by_service,
                  &session->id);

    /* Remove all the "info" and send notification */
    if (session->infos) {
        session_infos_push_change_notify_set(session->infos);
        session_infos_remove_all(session->infos);
        change_set = session_infos_pop_change_notify_set(session->infos);
        session_api_notify_changed(session->infos, change_set);
        session_change_notify_set_free(change_set);
    }    

    session_unref(session); /* drop the ref owned by the tree */
}

static void
session_fail_resolve(Session *session)
{
    session->state = SESSION_STATE_FAILED_RESOLVE;
    remove_session_by_service(session);
}

static void
session_fail_connect(Session *session)
{
    session->state = SESSION_STATE_FAILED_CONNECT;
    if (session->connection != NULL) {
        dbus_connection_unref(session->connection);
        session->connection = NULL;
    }
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
                    g_debug("Session ID in TXT record does not match session ID from protocol, or ID was changed");
                    return FALSE;
                } else {
                    g_debug("Session ID from TXT record confirmed: %s", value);
                }
            } else if (strcmp("key", "machine") == 0) {
                if (session->machine_id == NULL) {
                    session->machine_id = g_strdup(value);
                } else if (strcmp(session->machine_id, value) != 0) {
                    g_debug("Machine ID in TXT record does not match machine ID from protocol, or ID was changed");
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
    SessionChangeNotifySet *change_set;

    session = user_data;
    change_set = NULL;
    
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


        if (session->infos == NULL) {
            g_assert(session->machine_id);
            g_assert(session->session_id);
            session->infos = session_infos_new(session->machine_id,
                                               session->session_id);
        }
        
        dbus_message_iter_next(&iter);
        dbus_message_iter_recurse(&iter, &infos_iter);

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

            /* FIXME right now we never remove an info that's no longer there! */
            /* At first it looks like we could just push change set, remove all,
             * add back, and notify on the resulting change set. The problem with
             * this is that we would fail to short-circuit items that were not really
             * changed since we wouldn't have the original items due to the remove_all.
             * So some slightly more involved approach is needed.
             */
            
            dbus_message_iter_next(&infos_iter);
        }

        change_set = session_infos_pop_change_notify_set(session->infos);
        
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
    dbus_connection_unref(session->connection);
    session->connection = NULL;
    session->state = SESSION_STATE_DISCONNECTED;

    if (change_set != NULL) {
        session_api_notify_changed(session->infos, change_set);
        session_change_notify_set_free(change_set);
    }
    
    session_unref(session);
}

static void
connection_opened_handler(DBusConnection  *connection_or_null,
                          const DBusError *error_if_null,
                          void            *data)
{
    Session *session;
    DBusPendingCall *pending;
    DBusMessage *message;

    session = data;

    g_assert(session->connection == NULL);
    g_assert(session->state == SESSION_STATE_CONNECTING);
    
    if (connection_or_null == NULL) {
        g_debug("Failed: %s", error_if_null->message);
        session_fail_connect(session);
        session_unref(session);
        return;
    }

    session->connection = connection_or_null;
    dbus_connection_ref(session->connection);
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
        session_unref(session);
        return;
    }

    /* pass refcount on "pending" and "session" in here */
    dbus_pending_call_set_notify(pending, on_get_session_info_reply,
                                 session, NULL);
}

static void
session_connect(Session *session)
{
    char *dbus_address;
    char address_str[AVAHI_ADDRESS_STR_MAX];
    
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

    session_ref(session);
    hippo_dbus_connection_open_private_async(dbus_address, connection_opened_handler,
                                             session);

    g_free(dbus_address);
}

static void
resolver_ref(Resolver *resolver)
{
    resolver->refcount += 1;
}

static void
resolver_unref(Resolver *resolver)
{
    g_assert(resolver->refcount > 0);
    resolver->refcount -= 1;
    if (resolver->refcount == 0) {
        service_id_free_fields(&resolver->id);

        if (resolver->resolver) /* only NULL if we failed to create it in resolver_new */
            avahi_service_resolver_free(resolver->resolver);
        
        g_free(resolver);
    }
}

static Resolver*
get_resolver_by_service(const ServiceId *id)
{
    return g_tree_lookup(resolver_tree_by_service, id);
}

static void
add_resolver_by_service(Resolver *resolver)
{
    g_tree_insert(resolver_tree_by_service,
                  &resolver->id,
                  resolver);
    resolver_ref(resolver);
}

static void
remove_resolver_by_service(Resolver *resolver)
{
    g_tree_remove(resolver_tree_by_service,
                  &resolver->id);
    
    resolver_unref(resolver); /* drop the ref owned by the tree */
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

static guint32
get_txt_record_uint32(AvahiStringList *list,
                      const char      *key)
{
    char *s;
    guint64 l;
    char *end;
    s = get_txt_record(list, key);
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
    Resolver *resolver = data;
    Session *session;

    session = get_session_by_service(&resolver->id);
    
    switch (event) {
    case AVAHI_RESOLVER_FAILURE:
        /* This can happen on "change notification" (after we already found the
         * session once) it seems?
         */
        g_debug("service failed to resolve, dropping it");
        if (session != NULL)
            session_fail_resolve(session);

        /* not sure we should free the resolver? can it still get "FOUND" later?
         * With this setup, we will need another browser event to recreate it.
         */
        remove_resolver_by_service(resolver);
        break;
        
    case AVAHI_RESOLVER_FOUND:
        g_debug("Resolved %s:%d", host_name, port);
        
        if (session == NULL) {
            session = session_new(&resolver->id);
            add_session_by_service(session); /* this increments the refcount */
            session_unref(session); /* drop our refcount from new() */
        }

        if (session->state == SESSION_STATE_RESOLVING) {
            session->host_name = g_strdup(host_name);
            session->address = *address;
            session->port = port;
            
            /* these may or may not be present. If they are, we verify their
             * accuracy later.
             */
            session->machine_id = get_txt_record(txt, "org.freedesktop.od.machine");
            session->session_id = get_txt_record(txt, "org.freedesktop.od.session");
            
            session_connect(session);
        } else {
            /* Presumably a change notification */
            guint32 new_serial;
            char *new_machine_id;
            char *new_session_id;

            /* Can these really change? */
            if (strcmp(session->host_name, host_name) != 0) {
                g_free(session->host_name);
                session->host_name = g_strdup(host_name);
            }
            session->address = *address;
            session->port = port;

            /* check new machine/session ID */
            new_machine_id = get_txt_record(txt, "org.freedesktop.od.machine");
            new_session_id = get_txt_record(txt, "org.freedesktop.od.session");

            /* If these change for now we warn and ignore the change */
            if (new_machine_id && session->machine_id &&
                strcmp(new_machine_id, session->machine_id) != 0) {
                g_printerr("Remote session changed its machine ID, which is not allowed\n");
            }
            if (new_session_id && session->session_id &&
                strcmp(new_session_id, session->session_id) != 0) {
                g_printerr("Remote session changed its session ID, which is not allowed\n");
            }
            g_free(new_machine_id);
            g_free(new_session_id);
            
            new_serial = get_txt_record_uint32(txt, "org.freedesktop.od.serial");

            g_debug("new change serial %u", new_serial);
            
            if (new_serial > 0 &&
                new_serial != session->change_serial) {
                /* Connect again */
                session_connect(session); 
            }
        }
        break;
    default:
        g_debug("Unknown resolver event");
        break;
    }
}

static Resolver*
resolver_new(const ServiceId      *id)
{
    Resolver *resolver;

    resolver = g_new0(Resolver, 1);
    resolver->refcount = 1;
    service_id_init_fields(&resolver->id, id);

    /* According to the Avahi examples "if the server is
     * terminated before the callback function is called the
     * server will free the resolver for us." I'm not sure
     * what "server is terminated" means, but I don't see how
     * we could handle Avahi freeing the resolver out from
     * under us. For now, we'll just hope it doesn't do that.
     */
    
    resolver->resolver = avahi_service_resolver_new(avahi_glue_get_client(),
                                                    resolver->id.interface,
                                                    resolver->id.protocol,
                                                    resolver->id.name,
                                                    resolver->id.type,
                                                    resolver->id.domain,
                                                    AVAHI_PROTO_UNSPEC, 0,
                                                    resolve_callback,
                                                    resolver /* callback data */);

    if (resolver->resolver == NULL) {
        g_printerr("Failed to create service resolver: '%s': %s\n", resolver->id.name,
                   avahi_strerror(avahi_client_errno(avahi_glue_get_client())));
        resolver_unref(resolver);
        return NULL;
    }
    
    return resolver;
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
            Resolver *resolver;

            g_debug("Browsed %s", name);
            
            resolver = resolver_new(&id);
            if (resolver != NULL) {
                add_resolver_by_service(resolver);
                resolver_unref(resolver);
            }
        }
        break;

    case AVAHI_BROWSER_REMOVE:
        {
            Resolver *resolver;
            Session *session;

            resolver = get_resolver_by_service(&id);
            if (resolver != NULL) {
                remove_resolver_by_service(resolver);
            }
            
            session = get_session_by_service(&id);
            if (session != NULL) {
                remove_session_by_service(session);
            }
        }
        break;

    case AVAHI_BROWSER_ALL_FOR_NOW:
    case AVAHI_BROWSER_CACHE_EXHAUSTED:
        g_debug("Avahi browser event %s",
                event == AVAHI_BROWSER_CACHE_EXHAUSTED ? "CACHE_EXHAUSTED" : "ALL_FOR_NOW");
        break;
        
    default:
        g_debug("Unknown Avahi browser event %d", event);
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
    resolver_tree_by_service = g_tree_new(service_id_cmp);
    
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

    info = NULL;
    if (session->infos != NULL)
        info = session_infos_get(session->infos, taad->info_name);
    if (info == NULL)
        return FALSE; /* FALSE = keep traversing */
    
    if (!session_infos_write_with_info(session->infos,
                                       info,
                                       taad->array_iter)) {
        taad->failed = TRUE;
        return TRUE; /* stop traversing */
    }
    
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
