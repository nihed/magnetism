/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_SYSTEM_DBUS_H__
#define __HIPPO_SYSTEM_DBUS_H__

/* dbus system bus glue */

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoSystemDBus      HippoSystemDBus;
typedef struct _HippoSystemDBusClass HippoSystemDBusClass;

#define HIPPO_TYPE_SYSTEM_DBUS              (hippo_system_dbus_get_type ())
#define HIPPO_SYSTEM_DBUS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_SYSTEM_DBUS, HippoSystemDBus))
#define HIPPO_SYSTEM_DBUS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_SYSTEM_DBUS, HippoSystemDBusClass))
#define HIPPO_IS_SYSTEM_DBUS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_SYSTEM_DBUS))
#define HIPPO_IS_SYSTEM_DBUS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_SYSTEM_DBUS))
#define HIPPO_SYSTEM_DBUS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_SYSTEM_DBUS, HippoSystemDBusClass))

GType        	 hippo_system_dbus_get_type               (void) G_GNUC_CONST;

HippoSystemDBus* hippo_system_dbus_open                   (GError **error);

G_END_DECLS

#endif /* __HIPPO_SYSTEM_DBUS_H__ */
