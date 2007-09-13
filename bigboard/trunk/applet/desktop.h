/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __DESKTOP_H__
#define __DESKTOP_H__

#include <config.h>
#include <glib.h>
#include <gdk/gdk.h>

G_BEGIN_DECLS

gboolean desktop_launch      (GdkScreen   *screen,
                              const char  *desktop_name,
                              GError     **error);
gboolean desktop_launch_list (GdkScreen   *screen,
                              const char  *desktop_names,
                              GError     **error);

G_END_DECLS

#endif /* __DESKTOP_H__ */
