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
#include "avahi-glue.h"
#include "avahi-advertiser.h"

static const AvahiPoll *poll_api = NULL;
static AvahiGLibPoll *glib_poll = NULL;
static AvahiClient *avahi_client = NULL;

const AvahiPoll*
avahi_glue_get_poll(void)
{
    return poll_api;
} 

AvahiGLibPoll*
avahi_glue_get_glib_poll(void)
{
    return glib_poll;
}

AvahiClient*
avahi_glue_get_client(void)
{
    return avahi_client;
}

static void
client_callback(AvahiClient     *client,
                AvahiClientState state,
                void            *data)
{
    /* Called whenever the client or server state changes */
    
    switch (state) {
    case AVAHI_CLIENT_S_RUNNING:
            
        /* The server has started up successfully and registered
         * its host name on the network, so it's time to create
         * our services
         */
        avahi_advertiser_ensure_entry_group(client);
        break;
            
    case AVAHI_CLIENT_FAILURE:

        /* FIXME: This means that we got disconnected from the Avahi server; we need to
         * clean everything up and restart.
         */
            
        g_printerr("Avahi client failure: %s\n", avahi_strerror(avahi_client_errno(client)));
        exit(1);
            
        break;

    case AVAHI_CLIENT_S_COLLISION:
        
        /* Let's drop our registered services. When the server is
         * back in AVAHI_SERVER_RUNNING state we will register
         * them again with the new host name.
         */
        /* FALL THROUGH */
            
    case AVAHI_CLIENT_S_REGISTERING:

        /* The server records are now being established. This
         * might be caused by a host name change. We need to wait
         * for our own records to register until the host name is
         * properly established.
         */

        avahi_advertiser_reset_entry_group();
        break;

    case AVAHI_CLIENT_CONNECTING:
        ;
    }
}


gboolean
avahi_glue_init(void)
{
    int errcode;
    
    g_assert(poll_api == NULL);
    
    avahi_set_allocator(avahi_glib_allocator());
    
    glib_poll = avahi_glib_poll_new(NULL, G_PRIORITY_DEFAULT);
    poll_api = avahi_glib_poll_get(glib_poll);

    /* If we had a shutdown: avahi_glib_poll_free (glib_poll); avahi_client_free(client); */

    errcode = AVAHI_OK;
    avahi_client = avahi_client_new(poll_api, AVAHI_CLIENT_NO_FAIL, client_callback, NULL, &errcode);
    
    if (avahi_client == NULL) {
        g_printerr("Failed to create Avahi client: %s\n", avahi_strerror(errcode));
        return FALSE;
    }
    
    return TRUE;
}
