/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_WINDOW_GTK_H__
#define __HIPPO_WINDOW_GTK_H__

/* GtkWindow (via HippoCanvasWindow) subclass used by HippoWindowWrapper */

#include <hippo/hippo-window.h>
#include <cairo.h>

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

HippoWindowGtk* hippo_window_gtk_new    (void);

void            hippo_window_gtk_set_role           (HippoWindowGtk  *window_gtk,
                                                     HippoWindowRole  role);
HippoWindowRole hippo_window_gtk_get_role           (HippoWindowGtk  *window_gtk);
void            hippo_window_gtk_set_resize_gravity (HippoWindowGtk  *window_gtk,
                                                     HippoGravity     resize_gravity);
HippoGravity    hippo_window_gtk_get_resize_gravity (HippoWindowGtk  *window_gtk);
void            hippo_window_gtk_set_position       (HippoWindowGtk  *window_gtk,
                                                     int              x,
                                                     int              y);

G_END_DECLS

#endif /* __HIPPO_WINDOW_GTK_H__ */
