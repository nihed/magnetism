/* -*- mode: C; c-file-style: "gnu" -*- */
/* hh-client.h Client object
 *
 * Copyright (C) 2006 Red Hat Inc.
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
#if !defined (HH_INSIDE_HIPPOHERD_H) && !defined (HIPPOHERD_COMPILATION)
#error "Only <hh/hippoherd.h> can be included directly, this file may disappear or change contents."
#endif

#ifndef HH_CLIENT_H
#define HH_CLIENT_H

#include <glib.h>

G_BEGIN_DECLS

typedef struct HhClient HhClient;

HhClient* hh_client_new(void);

G_END_DECLS

#endif /* HH_CLIENT_H */
