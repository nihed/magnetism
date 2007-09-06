/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HTTP_H__
#define __HTTP_H__

#include "hippo-dbus-helper.h"

G_BEGIN_DECLS

typedef void (* HttpFunc) (const char *content_type,
                           GString    *content_or_error,
                           void       *data);

void http_get(DBusConnection *connection,
              const char     *url,
              HttpFunc        func,
              void           *data);

G_END_DECLS

#endif /* __HTTP_H__ */
