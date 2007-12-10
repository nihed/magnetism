/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
/* HippoScrollRibbon based on EogThumbNav
 *
 * Copyright (C) 2007 Red Hat, Inc.
 * Copyright (C) 2006 The Free Software Foundation
 *
 * Author: Lucas Rocha <lucasr@gnome.org>
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

#ifndef __HIPPO_SCROLL_RIBBON_H__
#define __HIPPO_SCROLL_RIBBON_H__

#include <gtk/gtk.h>

G_BEGIN_DECLS

typedef struct _HippoScrollRibbon HippoScrollRibbon;
typedef struct _HippoScrollRibbonClass HippoScrollRibbonClass;
typedef struct _HippoScrollRibbonPrivate HippoScrollRibbonPrivate;

#define HIPPO_TYPE_SCROLL_RIBBON            (hippo_scroll_ribbon_get_type ())
#define HIPPO_SCROLL_RIBBON(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), HIPPO_TYPE_SCROLL_RIBBON, HippoScrollRibbon))
#define HIPPO_SCROLL_RIBBON_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass),  HIPPO_TYPE_SCROLL_RIBBON, HippoScrollRibbonClass))
#define HIPPO_IS_SCROLL_RIBBON(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), HIPPO_TYPE_SCROLL_RIBBON))
#define HIPPO_IS_SCROLL_RIBBON_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass),  HIPPO_TYPE_SCROLL_RIBBON))
#define HIPPO_SCROLL_RIBBON_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS((obj),  HIPPO_TYPE_SCROLL_RIBBON, HippoScrollRibbonClass))

struct _HippoScrollRibbon {
    GtkVBox base_instance;

    HippoScrollRibbonPrivate *priv;
};

struct _HippoScrollRibbonClass {
    GtkVBoxClass parent_class;
};

GType	         hippo_scroll_ribbon_get_type          (void) G_GNUC_CONST;

GtkWidget       *hippo_scroll_ribbon_new               (GtkOrientation     orientation);

G_END_DECLS

#endif /* __HIPPO_SCROLL_RIBBON_H__ */
