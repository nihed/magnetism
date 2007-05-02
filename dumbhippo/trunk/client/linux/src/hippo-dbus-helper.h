/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_HELPER_H__
#define __HIPPO_DBUS_HELPER_H__

/* D-Bus convenience thingy */

#include <dbus/dbus.h>
#include <glib-object.h>

G_BEGIN_DECLS

typedef enum {
    HIPPO_DBUS_MEMBER_METHOD,
    HIPPO_DBUS_MEMBER_SIGNAL
} HippoDBusMemberType;

typedef struct HippoDBusMember HippoDBusMember;

typedef struct HippoDBusProperty HippoDBusProperty;

typedef DBusMessage* (* HippoDBusHandler) (void            *object,
                                           DBusMessage     *message,
                                           DBusError       *error);
typedef dbus_bool_t (* HippoDBusGetter)   (void            *object,
                                           const char      *prop_name,
                                           DBusMessageIter *append_iter,
                                           DBusError       *error);
typedef dbus_bool_t (* HippoDBusSetter)   (void            *object,
                                           const char      *prop_name,
                                           DBusMessageIter *value_iter,
                                           DBusError       *error);

struct HippoDBusMember
{
    HippoDBusMemberType member_type;
    const char *name;
    const char *in_args;
    const char *out_args;
    /* for a signal the handler is NULL
     */
    HippoDBusHandler handler;
};

struct HippoDBusProperty
{
    const char *name;
    const char *signature;
    /* read or write only have NULL getter or setter */
    HippoDBusGetter getter;
    HippoDBusSetter setter;
};

void              hippo_dbus_helper_register_interface   (DBusConnection          *connection,
                                                          const char              *name,
                                                          const HippoDBusMember   *members,
                                                          const HippoDBusProperty *properties);
void              hippo_dbus_helper_register_object      (DBusConnection          *connection,
                                                          const char              *path,
                                                          void                    *object,
                                                          const char              *first_interface,
                                                          ...) G_GNUC_NULL_TERMINATED;
void              hippo_dbus_helper_register_g_object    (DBusConnection          *connection,
                                                          const char              *path,
                                                          GObject                 *object,
                                                          const char              *first_interface,
                                                          ...) G_GNUC_NULL_TERMINATED;
void              hippo_dbus_helper_unregister_object    (DBusConnection          *connection,
                                                          const char              *path);
gboolean          hippo_dbus_helper_object_is_registered (DBusConnection          *connection,
                                                          const char              *path);
DBusHandlerResult hippo_dbus_helper_handle_message       (DBusConnection          *connection,
                                                          DBusMessage             *message);


DBusMessage*      hippo_dbus_client_call_method_sync     (DBusConnection          *connection,
                                                          const char              *bus_name,
                                                          const char              *path,
                                                          const char              *interface,
                                                          const char              *method,
                                                          DBusError               *error,
                                                          int                      first_arg_type,
                                                          ...);

G_END_DECLS

#endif /* __HIPPO_DBUS_HELPER_H__ */
