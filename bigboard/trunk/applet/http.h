/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HTTP_H__
#define __HTTP_H__

#include "hippo-dbus-helper.h"
#include <gdk-pixbuf/gdk-pixbuf.h>

G_BEGIN_DECLS

typedef void (* HttpFunc) (const char *content_type,
                           GString    *content_or_error,
                           void       *data);

typedef void (* HttpPixbufFunc) (GdkPixbuf  *pixbuf_or_null,
                                 void       *data);

void http_get(DBusConnection *connection,
              const char     *url,
              HttpFunc        func,
              void           *data);

void http_get_pixbuf(DBusConnection *connection,
                     const char     *url,
                     HttpPixbufFunc  func,
                     void           *data);

G_END_DECLS

#endif /* __HTTP_H__ */
