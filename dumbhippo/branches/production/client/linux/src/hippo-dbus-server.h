#ifndef __HIPPO_DBUS_SERVER_H__
#define __HIPPO_DBUS_SERVER_H__

/* dbus server-side glue */

#include <glib-object.h>
#include <hippo/hippo-basics.h>

G_BEGIN_DECLS

typedef struct _HippoDBus      HippoDBus;
typedef struct _HippoDBusClass HippoDBusClass;

#define HIPPO_TYPE_DBUS              (hippo_dbus_get_type ())
#define HIPPO_DBUS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DBUS, HippoDBus))
#define HIPPO_DBUS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DBUS, HippoDBusClass))
#define HIPPO_IS_DBUS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DBUS))
#define HIPPO_IS_DBUS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DBUS))
#define HIPPO_DBUS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DBUS, HippoDBusClass))

GType        	 hippo_dbus_get_type               (void) G_GNUC_CONST;

HippoDBus* hippo_dbus_try_to_acquire           (const char  *server,
                                                gboolean     replace_existing,
                                                GError     **error);
/* drop the connection, but assume we'll go back to the main 
 * loop to deal with the consequences
 */
void       hippo_dbus_disconnect               (HippoDBus   *dbus);
/* disconnect, then process anything queued up 
 * to and including the Disconnected message
 */
void       hippo_dbus_blocking_shutdown        (HippoDBus   *dbus);

G_END_DECLS

#endif /* __HIPPO_DBUS_SERVER_H__ */
