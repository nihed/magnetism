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

#ifndef __AVAHI_ADVERTISER_H__
#define __AVAHI_ADVERTISER_H__

#include <config.h>
#include <glib.h>
#include "avahi-glue.h"

gboolean avahi_advertiser_init               (void);
void     avahi_advertiser_reset_entry_group  (void);
gboolean avahi_advertiser_ensure_entry_group (AvahiClient *client);
void     avahi_advertiser_queue_republish    (void);


#endif /* __AVAHI_ADVERTISER_H__ */

