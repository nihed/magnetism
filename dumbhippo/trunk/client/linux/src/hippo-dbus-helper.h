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
DBusHandlerResult hippo_dbus_helper_filter_message       (DBusConnection          *connection,
                                                          DBusMessage             *message);
void              hippo_dbus_helper_emit_signal          (DBusConnection          *connection,
                                                          const char              *path,
                                                          const char              *interface,
                                                          const char              *signal_name,
                                                          int                      first_arg_type,
                                                          ...);
void              hippo_dbus_helper_emit_signal_valist   (DBusConnection          *connection,
                                                          const char              *path,
                                                          const char              *interface,
                                                          const char              *signal_name,
                                                          int                      first_arg_type,
                                                          va_list                  args);


typedef struct HippoDBusProxy HippoDBusProxy;

HippoDBusProxy*   hippo_dbus_proxy_new                     (DBusConnection          *connection,
                                                            const char              *bus_name,
                                                            const char              *path,
                                                            const char              *interface);
void              hippo_dbus_proxy_unref                   (HippoDBusProxy          *proxy);

DBusMessage*      hippo_dbus_proxy_call_method_sync        (HippoDBusProxy          *proxy,
                                                            const char              *method,
                                                            DBusError               *error,
                                                            int                      first_arg_type,
                                                            ...);
DBusMessage*      hippo_dbus_proxy_call_method_sync_valist (HippoDBusProxy          *proxy,
                                                            const char              *method,
                                                            DBusError               *error,
                                                            int                      first_arg_type,
                                                            va_list                  args);

/* this takes ownership of the error and the reply which means it also
 * frees the returned args unless they are just primitives. Not usable with array returns.
 */
dbus_bool_t       hippo_dbus_proxy_finish_method_call_freeing_reply (DBusMessage *reply,
                                                                     const char  *method,
                                                                     DBusError   *error,
                                                                     int          first_arg_type,
                                                                     ...);

/* This one frees the error but not the reply */
dbus_bool_t       hippo_dbus_proxy_finish_method_call_keeping_reply (DBusMessage *reply,
                                                                     const char  *method,
                                                                     DBusError   *error,
                                                                     int          first_arg_type,
                                                                     ...);

dbus_bool_t hippo_dbus_proxy_INT32__VOID              (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t    *out1_p);
dbus_bool_t hippo_dbus_proxy_INT32__INT32             (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1_p,
                                                       dbus_int32_t    *out1_p);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__INT32        (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1_p,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__VOID         (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__INT32_STRING (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1_p,
                                                       const char      *in2_p,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len);
dbus_bool_t hippo_dbus_proxy_STRING__INT32            (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1_p,
                                                       char           **out1_p);



G_END_DECLS

#endif /* __HIPPO_DBUS_HELPER_H__ */
