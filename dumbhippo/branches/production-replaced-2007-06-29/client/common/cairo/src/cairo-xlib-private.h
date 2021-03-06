/* Cairo - a vector graphics library with display and print output
 *
 * Copyright © 2005 Red Hat, Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it either under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * (the "LGPL") or, at your option, under the terms of the Mozilla
 * Public License Version 1.1 (the "MPL"). If you do not alter this
 * notice, a recipient may use your version of this file under either
 * the MPL or the LGPL.
 *
 * You should have received a copy of the LGPL along with this library
 * in the file COPYING-LGPL-2.1; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * You should have received a copy of the MPL along with this library
 * in the file COPYING-MPL-1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the LGPL or the MPL for
 * the specific language governing rights and limitations.
 *
 * The Original Code is the cairo graphics library.
 *
 * The Initial Developer of the Original Code is Red Hat, Inc.
 */

#ifndef CAIRO_XLIB_PRIVATE_H
#define CAIRO_XLIB_PRIVATE_H

#include "cairoint.h"
#include "cairo-xlib.h"

typedef struct _cairo_xlib_screen_info cairo_xlib_screen_info_t;

struct _cairo_xlib_screen_info {
    cairo_xlib_screen_info_t *next;

    Display *display;
    Screen *screen;
    cairo_bool_t has_render;

    cairo_font_options_t font_options;
};

cairo_private cairo_xlib_screen_info_t *
_cairo_xlib_screen_info_get (Display *display, Screen *screen);

#if CAIRO_HAS_XLIB_XRENDER_SURFACE

#include "cairo-xlib-xrender.h"
slim_hidden_proto (cairo_xlib_surface_create_with_xrender_format);

#endif

#endif /* CAIRO_XLIB_PRIVATE_H */
