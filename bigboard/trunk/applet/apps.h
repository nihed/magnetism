/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __APPS_H__
#define __APPS_H__

#include <config.h>
#include <glib.h>
#define DDM_I_KNOW_THIS_IS_UNSTABLE 1
#include <ddm/ddm.h>
#include <gdk-pixbuf/gdk-pixbuf.h>

G_BEGIN_DECLS

typedef struct App App;

App*        app_new         (DDMDataResource *resource);
void        app_ref         (App             *app);
void        app_unref       (App             *app);
const char *app_get_tooltip (App             *app);
GdkPixbuf*  app_get_icon    (App             *app);

G_END_DECLS

#endif /* __APPS_H__ */
