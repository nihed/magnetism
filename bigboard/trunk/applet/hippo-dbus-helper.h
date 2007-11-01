/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#ifndef __HIPPO_DBUS_HELPER_H__
#define __HIPPO_DBUS_HELPER_H__

/* D-Bus convenience thingy */

#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus.h>
#include <glib-object.h>

#include "hippo-dbus-helper-rename.h"

G_BEGIN_DECLS

typedef enum {
    HIPPO_DBUS_MEMBER_METHOD,
    HIPPO_DBUS_MEMBER_SIGNAL
} HippoDBusMemberType;

typedef enum {
    HIPPO_DBUS_SERVICE_START_IF_NOT_RUNNING = 1 << 0
} HippoDBusServiceTrackerFlags;

typedef enum {
    HIPPO_DBUS_NAME_SINGLE_INSTANCE_REPLACING_CURRENT_OWNER,
    HIPPO_DBUS_NAME_SINGLE_INSTANCE,
    HIPPO_DBUS_NAME_OWNED_OPTIONALLY
} HippoDBusNameOwnershipStyle;


typedef struct HippoDBusProxy HippoDBusProxy;
typedef struct HippoDBusMember HippoDBusMember;
typedef struct HippoDBusProperty HippoDBusProperty;
typedef struct HippoDBusServiceTracker HippoDBusServiceTracker;
typedef struct HippoDBusSignalTracker HippoDBusSignalTracker;
typedef struct HippoDBusConnectionTracker HippoDBusConnectionTracker;
typedef struct HippoDBusNameOwner HippoDBusNameOwner;


typedef void        (* HippoDBusReplyHandler) (DBusMessage *reply,
                                               void        *data);
typedef dbus_bool_t (* HippoDBusArgAppender)  (DBusMessage *message,
                                               void        *data);


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

typedef void (* HippoDBusServiceAvailableHandler)   (DBusConnection *connection,
                                                     const char     *well_known_name,
                                                     const char     *unique_name,
                                                     void           *data);
typedef void (* HippoDBusServiceUnavailableHandler) (DBusConnection *connection,
                                                     const char     *well_known_name,
                                                     const char     *unique_name,
                                                     void           *data);
/* if we were ever "productizing" this file, signal handlers should really be on
 * the DBusProxy objects or something, not on the service like this
 */
typedef void (* HippoDBusSignalHandler)             (DBusConnection *connection,
                                                     DBusMessage    *message,
                                                     void           *data);

typedef void (* HippoDBusConnectedHandler)          (DBusConnection *connection,
                                                     void           *data);
typedef void (* HippoDBusDisconnectedHandler)       (DBusConnection *connection,
                                                     void           *data);

typedef void (* HippoDBusNameOwnedHandler)          (DBusConnection *connection,
                                                     void           *data);
typedef void (* HippoDBusNameNotOwnedHandler)       (DBusConnection *connection,
                                                     void           *data);

struct HippoDBusMember
{
    HippoDBusMemberType member_type;
    const char *name;
    const char *in_args;
    const char *out_args; /* FIXME right now for signals we use out_args, should be in_args */
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

struct HippoDBusServiceTracker
{
    HippoDBusServiceTrackerFlags flags;
    HippoDBusServiceAvailableHandler available_handler;
    HippoDBusServiceUnavailableHandler unavailable_handler;
};

struct HippoDBusSignalTracker
{
    /* FIXME there's no way to specify the object path */
    /* FIXME specify the signature so it can be automatically checked */
    const char *interface;
    const char *signal;
    HippoDBusSignalHandler handler;
};

struct HippoDBusConnectionTracker
{
    HippoDBusConnectedHandler connected_handler;
    HippoDBusDisconnectedHandler disconnected_handler;
};

struct HippoDBusNameOwner
{
    HippoDBusNameOwnedHandler owned_handler;
    HippoDBusNameNotOwnedHandler  not_owned_handler;
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
void              hippo_dbus_helper_emit_signal_appender (DBusConnection          *connection,
                                                          const char              *path,
                                                          const char              *interface,
                                                          const char              *signal_name,
                                                          HippoDBusArgAppender     appender,
                                                          void                    *appender_data);
                                                          
void hippo_dbus_helper_register_service_tracker      (DBusConnection                   *connection,
                                                      const char                       *well_known_name,
                                                      const HippoDBusServiceTracker    *tracker,
                                                      const HippoDBusSignalTracker     *signal_handlers,
                                                      void                             *data);
void hippo_dbus_helper_unregister_service_tracker    (DBusConnection                   *connection,
                                                      const char                       *well_known_name,
                                                      const HippoDBusServiceTracker    *tracker,
                                                      void                             *data);
void hippo_dbus_helper_register_connection_tracker   (DBusBusType                       bus_type,
                                                      const HippoDBusConnectionTracker *tracker,
                                                      void                             *data);
void hippo_dbus_helper_unregister_connection_tracker (DBusBusType                       bus_type,
                                                      const HippoDBusConnectionTracker *tracker,
                                                      void                             *data);
void hippo_dbus_helper_register_name_owner           (DBusConnection                   *connection,
                                                      const char                       *well_known_name,
                                                      HippoDBusNameOwnershipStyle       ownership_style,
                                                      const HippoDBusNameOwner         *owner,
                                                      void                             *data);
void hippo_dbus_helper_unregister_name_owner         (DBusConnection                   *connection,
                                                      const char                       *well_known_name,
                                                      const HippoDBusNameOwner         *owner,
                                                      void                             *data);
/* these two aren't implemented yet */
void hippo_dbus_helper_register_name_owner_on_bus    (DBusBusType                       bus_type,
                                                      const char                       *well_known_name,
                                                      HippoDBusNameOwnershipStyle       ownership_style,
                                                      const HippoDBusNameOwner         *owner,
                                                      void                             *data);
void hippo_dbus_helper_unregister_name_owner_on_bus  (DBusBusType                       bus_type,
                                                      const char                       *well_known_name,
                                                      const HippoDBusNameOwner         *owner,
                                                      void                             *data);



HippoDBusProxy*   hippo_dbus_proxy_new                     (DBusConnection          *connection,
                                                            const char              *bus_name,
                                                            const char              *path,
                                                            const char              *interface);
/* Set a constant string to prepend to the method name passed to the following functions
 * before calling it. This is to manage Gaim/Pidgin where all methods were previously
 * Gaim* and now are Purple*
 */
void              hippo_dbus_proxy_set_method_prefix       (HippoDBusProxy          *proxy,
                                                            const char              *method_prefix);
void              hippo_dbus_proxy_unref                   (HippoDBusProxy          *proxy);

DBusMessage* hippo_dbus_proxy_call_method_sync           (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          DBusError             *error,
                                                          int                    first_arg_type,
                                                          ...);
DBusMessage* hippo_dbus_proxy_call_method_sync_valist    (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          DBusError             *error,
                                                          int                    first_arg_type,
                                                          va_list                args);
DBusMessage* hippo_dbus_proxy_call_method_sync_appender  (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          DBusError             *error,
                                                          HippoDBusArgAppender   appender,
                                                          void                  *appender_data);
void         hippo_dbus_proxy_call_method_async          (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          HippoDBusReplyHandler  handler,
                                                          void                  *data,
                                                          DBusFreeFunction       free_data_func,
                                                          int                    first_arg_type,
                                                          ...);
void         hippo_dbus_proxy_call_method_async_valist   (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          HippoDBusReplyHandler  handler,
                                                          void                  *data,
                                                          DBusFreeFunction       free_data_func,
                                                          int                    first_arg_type,
                                                          va_list                args);
void         hippo_dbus_proxy_call_method_async_appender (HippoDBusProxy        *proxy,
                                                          const char            *method,
                                                          HippoDBusReplyHandler  handler,
                                                          void                  *data,
                                                          DBusFreeFunction       free_data_func,
                                                          HippoDBusArgAppender   appender,
                                                          void                  *appender_data);


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

dbus_bool_t hippo_dbus_proxy_VOID__VOID               (HippoDBusProxy  *proxy,
                                                       const char      *method);
dbus_bool_t hippo_dbus_proxy_VOID__UINT32             (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_uint32_t    in1);
dbus_bool_t hippo_dbus_proxy_INT32__VOID              (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t    *out1_p);
dbus_bool_t hippo_dbus_proxy_INT32__INT32             (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1,
                                                       dbus_int32_t    *out1_p);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__INT32        (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len_p);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__VOID         (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len_p);
dbus_bool_t hippo_dbus_proxy_ARRAYINT32__INT32_STRING (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1,
                                                       const char      *in2,
                                                       dbus_int32_t   **out1_p,
                                                       dbus_int32_t    *out1_len_p);
dbus_bool_t hippo_dbus_proxy_STRING__INT32            (HippoDBusProxy  *proxy,
                                                       const char      *method,
                                                       dbus_int32_t     in1,
                                                       char           **out1_p);



G_END_DECLS

#endif /* __HIPPO_DBUS_HELPER_H__ */
