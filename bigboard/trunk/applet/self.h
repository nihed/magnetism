/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __SELF_H__
#define __SELF_H__

#include <config.h>
#include <gdk-pixbuf/gdk-pixbuf.h>

G_BEGIN_DECLS

typedef void (* SelfIconChangedCallback) (GdkPixbuf *pixbuf,
                                          void      *data);

void self_add_icon_changed_callback   (SelfIconChangedCallback  callback,
                                       void                    *data);
void self_remove_icon_changed_callback(SelfIconChangedCallback  callback,
                                       void                    *data);


G_END_DECLS

#endif /* __SELF_H__ */
