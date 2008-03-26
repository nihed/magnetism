/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_STACKER_H__
#define __HIPPO_DBUS_STACKER_H__

/* dbus server-side glue */

#include <glib-object.h>
#include <hippo/hippo-basics.h>
#include "main.h"

G_BEGIN_DECLS

typedef struct _HippoDBusStacker      HippoDBusStacker;
typedef struct _HippoDBusStackerClass HippoDBusStackerClass;

#define HIPPO_TYPE_DBUS_STACKER              (hippo_dbus_stacker_get_type ())
#define HIPPO_DBUS_STACKER(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DBUS_STACKER, HippoDBusStacker))
#define HIPPO_DBUS_STACKER_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DBUS_STACKER, HippoDBusStackerClass))
#define HIPPO_IS_DBUS_STACKER(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DBUS_STACKER))
#define HIPPO_IS_DBUS_STACKER_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DBUS_STACKER))
#define HIPPO_DBUS_STACKER_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DBUS_STACKER, HippoDBusStackerClass))

GType        	 hippo_dbus_stacker_get_type               (void) G_GNUC_CONST;

HippoDBusStacker* hippo_dbus_stacker_try_to_acquire           (const char  *stacker_server,
                                                               gboolean     replace_existing,
                                                               GError     **error);

/* This is just an arbitrary macro defined in dbus.h, the idea is to avoid requiring dbus.h for
 * this header
 */
#ifdef DBUS_MAJOR_PROTOCOL_VERSION
DBusConnection* hippo_dbus_stacker_get_connection(HippoDBusStacker *dbus);
                                                
#endif /* "only if dbus.h already included" */

G_END_DECLS

#endif /* __HIPPO_DBUS_STACKER_H__ */
