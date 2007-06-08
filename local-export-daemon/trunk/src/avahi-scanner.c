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
#include "avahi-scanner.h"
#include "hippo-dbus-helper.h"
#include "main.h"
#include <avahi-client/lookup.h>

static AvahiServiceBrowser *service_browser = NULL;

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

    switch (event) {
    case AVAHI_RESOLVER_FAILURE:
        /* Silently ignore this one then */
        break;
        
    case AVAHI_RESOLVER_FOUND:
        {
#if 0
            char a[AVAHI_ADDRESS_STR_MAX], *t;
            
            fprintf(stderr, "Service '%s' of type '%s' in domain '%s':\n", name, type, domain);
            
            avahi_address_snprint(a, sizeof(a), address);
            t = avahi_string_list_to_string(txt);
            fprintf(stderr,
                    "\t%s:%u (%s)\n"
                    "\tTXT=%s\n"
                    "\tcookie is %u\n"
                    "\tis_local: %i\n"
                    "\tour_own: %i\n"
                    "\twide_area: %i\n"
                    "\tmulticast: %i\n"
                    "\tcached: %i\n",
                    host_name, port, a,
                    t,
                    avahi_string_list_get_service_cookie(txt),
                    !!(flags & AVAHI_LOOKUP_RESULT_LOCAL),
                    !!(flags & AVAHI_LOOKUP_RESULT_OUR_OWN),
                    !!(flags & AVAHI_LOOKUP_RESULT_WIDE_AREA),
                    !!(flags & AVAHI_LOOKUP_RESULT_MULTICAST),
                    !!(flags & AVAHI_LOOKUP_RESULT_CACHED));
                
            avahi_free(t);
#endif
        }
    }

    avahi_service_resolver_free(r);
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

    switch (event) {
    case AVAHI_BROWSER_FAILURE:
        /* Nothing sane to do here - help */
        g_printerr("Avahi browser failed: %s\n", avahi_strerror(avahi_client_errno(avahi_service_browser_get_client(b))));
        exit(1);
        return;

    case AVAHI_BROWSER_NEW:

        /* We ignore the returned resolver object. In the callback
         * function we free it. If the server is terminated before
         * the callback function is called the server will free
         * the resolver for us.
         */
        {
            AvahiServiceResolver *resolver;
            
            resolver = avahi_service_resolver_new(avahi_glue_get_client(),
                                                  interface, protocol, name, type, domain, AVAHI_PROTO_UNSPEC, 0,
                                                  resolve_callback, NULL /* callback data */);
            if (resolver == NULL) {
                g_printerr("Failed to create service resolver: '%s': %s\n", name, avahi_strerror(avahi_client_errno(avahi_glue_get_client())));
            }
        }
        break;

    case AVAHI_BROWSER_REMOVE:
        /* FIXME */
        /* fprintf(stderr, "(Browser) REMOVE: service '%s' of type '%s' in domain '%s'\n", name, type, domain); */
        break;

    case AVAHI_BROWSER_ALL_FOR_NOW:
    case AVAHI_BROWSER_CACHE_EXHAUSTED:
        /* fprintf(stderr, "(Browser) %s\n", event == AVAHI_BROWSER_CACHE_EXHAUSTED ? "CACHE_EXHAUSTED" : "ALL_FOR_NOW"); */
        break;
    }
}

gboolean
avahi_scanner_init(void)
{
    g_assert(service_browser == NULL);
    
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
