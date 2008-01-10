/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_CLIENT_H__
#define __HIPPO_DBUS_CLIENT_H__

/* dbus client-side stuff ... shared between main mugshot client and the uri handler */

#include <glib-object.h>
#include <hippo/hippo-basics.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus.h>

G_BEGIN_DECLS

/* combined with host/port the client is connected to, to create full bus name */
#define HIPPO_DBUS_STACKER_BASE_BUS_NAME   "com.dumbhippo.Client"

/* combined with host/port the client is connected to, to create full bus name */
#define HIPPO_DBUS_ENGINE_BASE_BUS_NAME    "org.freedesktop.od.Engine"

/* these are used for operations specific to the stacker, vs. the
 * stuff that belongs to the desktop data model engine
 */
#define HIPPO_DBUS_STACKER_INTERFACE           "com.dumbhippo.Client"
#define HIPPO_DBUS_STACKER_PATH                "/com/dumbhippo/client"

/* This interface/path is used for callbacks from the server to a client (establish
 * using the Connect() method
 */
#define HIPPO_DBUS_STACKER_LISTENER_INTERFACE  "com.dumbhippo.Listener"
#define HIPPO_DBUS_STACKER_LISTENER_PATH       "/com/dumbhippo/listener"

/* these are used for operations that are not related to the stacker,
 * but are rather desktop-generic, like the data model
 */
#define HIPPO_DBUS_ENGINE_INTERFACE        "org.freedesktop.od.Engine"
#define HIPPO_DBUS_ENGINE_PATH             "/org/freedesktop/od/Engine"


/* This is in the new org.freedesktop.od namespace
 */
char*    hippo_dbus_full_bus_name     (const char   *server);

/* This is in the old "com.dumbhippo." namespace with the hex done properly,
 * replaced by org.freedesktop.od. namespace 2007-08-03
 */
char*    hippo_dbus_full_bus_name_com_dumbhippo_with_forward_hex (const char   *server);

/* This has non-letter characters hex-encoded with the two nibbles backwards,
 * we grab this name for backwards compatibility. This can be dropped in a
 * few releases after 2007-06-08.
 */
char*    hippo_dbus_full_bus_name_com_dumbhippo_with_backward_hex (const char   *server);

gboolean hippo_dbus_open_chat_blocking(const char   *server,
                                       HippoChatKind kind,
                                       const char   *chat_id,
                                       GError      **error);
gboolean hippo_dbus_show_browser_blocking(const char   *server,
                                          GError      **error);

void     hippo_dbus_debug_log_error   (const char   *where,
                                       DBusMessage  *message);

G_END_DECLS

#endif /* __HIPPO_DBUS_CLIENT_H__ */
