/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_WINDOW_GTK_H__
#define __HIPPO_WINDOW_GTK_H__

/* Implementation of HippoWindow for GTK+ */

#include "hippo-window.h"
#include <cairo/cairo.h>

G_BEGIN_DECLS

typedef struct _HippoWindowGtk      HippoWindowGtk;
typedef struct _HippoWindowGtkClass HippoWindowGtkClass;

#define HIPPO_TYPE_WINDOW_GTK              (hippo_window_gtk_get_type ())
#define HIPPO_WINDOW_GTK(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_WINDOW_GTK, HippoWindowGtk))
#define HIPPO_WINDOW_GTK_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_WINDOW_GTK, HippoWindowGtkClass))
#define HIPPO_IS_WINDOW_GTK(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_WINDOW_GTK))
#define HIPPO_IS_WINDOW_GTK_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_WINDOW_GTK))
#define HIPPO_WINDOW_GTK_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_WINDOW_GTK, HippoWindowGtkClass))

GType        	 hippo_window_gtk_get_type               (void) G_GNUC_CONST;

HippoWindow* hippo_window_gtk_new    (void);


G_END_DECLS

#endif /* __HIPPO_WINDOW_GTK_H__ */
