/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_SERVER_H__
#define __HIPPO_DBUS_SERVER_H__

/* dbus server-side glue */

#include <glib-object.h>
#include <hippo/hippo-basics.h>
#include <hippo/hippo-external-account.h>
#include "main.h"

G_BEGIN_DECLS

typedef struct _HippoDBusClass HippoDBusClass;

#define HIPPO_TYPE_DBUS              (hippo_dbus_get_type ())
#define HIPPO_DBUS(object)           (G_TYPE_CHECK_INSTANCE_CAST ((object), HIPPO_TYPE_DBUS, HippoDBus))
#define HIPPO_DBUS_CLASS(klass)      (G_TYPE_CHECK_CLASS_CAST ((klass), HIPPO_TYPE_DBUS, HippoDBusClass))
#define HIPPO_IS_DBUS(object)        (G_TYPE_CHECK_INSTANCE_TYPE ((object), HIPPO_TYPE_DBUS))
#define HIPPO_IS_DBUS_CLASS(klass)   (G_TYPE_CHECK_CLASS_TYPE ((klass), HIPPO_TYPE_DBUS))
#define HIPPO_DBUS_GET_CLASS(obj)    (G_TYPE_INSTANCE_GET_CLASS ((obj), HIPPO_TYPE_DBUS, HippoDBusClass))

GType        	 hippo_dbus_get_type               (void) G_GNUC_CONST;

HippoDBus* hippo_dbus_try_to_acquire           (const char  *desktop_server,
                                                const char  *stacker_server,
                                                gboolean     replace_existing,
                                                GError     **error);
void       hippo_dbus_init_services            (HippoDBus   *dbus);

/* These add the signal match rules, but you need to modify
 * hippo-dbus-server.c:handle_message() to actually deal with the notifications
 */
void hippo_dbus_watch_for_disconnect(HippoDBus  *dbus,
                                     const char *name);
void hippo_dbus_unwatch_for_disconnect(HippoDBus  *dbus,
                                       const char *name);


void       hippo_dbus_notify_auth_changed      (HippoDBus   *dbus);

void       hippo_dbus_notify_contacts_loaded   (HippoDBus   *dbus);

void       hippo_dbus_notify_pref_changed      (HippoDBus   *dbus,
                                                const char  *key,
                                                gboolean     value);

void       hippo_dbus_notify_xmpp_connected    (HippoDBus   *dbus,
                                                gboolean     connected);
                                                
void       hippo_dbus_notify_whereim_changed   (HippoDBus               *dbus,
                                                HippoConnection         *xmpp_connection,
                                                HippoExternalAccount    *acct);                                                

void       hippo_dbus_notify_entity_changed    (HippoDBus               *dbus,
                                                HippoEntity             *entity);

void       hippo_dbus_notify_external_iq_return(HippoDBus            *dbus,
                                                guint                 id,
                                                const char           *content);

typedef void (*HippoChatWindowForeach)(guint64 window_id, HippoChatState state, void *data);

void hippo_dbus_foreach_chat_window(HippoDBus             *dbus,
                                    const char            *chat_id,
                                    HippoChatWindowForeach function,
                                    void                  *data);

/* This is just an arbitrary macro defined in dbus.h, the idea is to avoid requiring dbus.h for
 * this header
 */
#ifdef DBUS_MAJOR_PROTOCOL_VERSION
DBusConnection* hippo_dbus_get_connection(HippoDBus *dbus);
                                                
#endif /* "only if dbus.h already included" */


G_END_DECLS

#endif /* __HIPPO_DBUS_SERVER_H__ */
