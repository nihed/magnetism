/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <glib.h>
#include <string.h>
#include "hippo-dbus-helper.h"
#include <dbus/dbus-glib-lowlevel.h>

typedef struct {
    char *name;
    const HippoDBusMember   *members;
    const HippoDBusProperty *properties;
} HippoDBusInterface;

typedef struct {
    char *path;
    void *object;
    HippoDBusInterface *interfaces[1]; /* actually allocated large enough for however many */
} HippoDBusObject;

typedef struct {
    guint refcount;
    DBusConnection *connection;
    char *well_known_name;
    char *owner;
    void *data;
    const HippoDBusServiceTracker *tracker;
    const HippoDBusSignalTracker  *signal_handlers;
    guint removed : 1;
} HippoDBusService;

typedef struct {
    GHashTable *interfaces;
    GHashTable *services_by_well_known;
    GHashTable *services_by_owner;
    GHashTable *name_owners;
    GSList *connection_trackers;
} HippoDBusHelper;

typedef struct {
    const HippoDBusConnectionTracker *tracker;
    void *data;
    DBusConnection *connection;
} HippoDBusConnection;

typedef enum {
    NAME_OWNERSHIP_UNKNOWN,
    NAME_OWNERSHIP_OWNED,
    NAME_OWNERSHIP_NOT_OWNED
} NameOwnerState;

typedef struct {
    NameOwnerState state;
    HippoDBusNameOwnershipStyle style;
    char *well_known_name;
    const HippoDBusNameOwner *owner;
    void *data;
} NameOwner;

static guint try_session_connect_timeout = 0;
static GSList *session_connection_trackers = NULL;
static gboolean session_ever_attempted = FALSE;

static DBusHandlerResult hippo_dbus_helper_handle_object_message(DBusConnection  *connection,
                                                                 DBusMessage     *message,
                                                                 HippoDBusObject *object);
static DBusHandlerResult hippo_dbus_helper_filter_message       (DBusConnection  *connection,
                                                                 DBusMessage     *message,
                                                                 void            *data);

static HippoDBusInterface*
iface_new(const char              *name,
          const HippoDBusMember   *members,
          const HippoDBusProperty *properties)
{
    HippoDBusInterface *iface;

    iface = g_new0(HippoDBusInterface, 1);
    iface->name = g_strdup(name);
    iface->members = members;
    iface->properties = properties;

    return iface;
}

#if 0
static void
iface_free(HippoDBusInterface *iface)
{
    g_free(iface->name);
    g_free(iface);
}
#endif

static const HippoDBusProperty*
iface_find_property(HippoDBusInterface *iface,
                    const char         *name)
{
    int i;

    if (iface->properties == NULL)
        return NULL;

    for (i = 0; iface->properties[i].name != NULL; ++i) {
        if (strcmp(name, iface->properties[i].name) == 0)
            return &iface->properties[i];
    }
    return NULL;
}

static const HippoDBusMember*
iface_find_member(HippoDBusInterface *iface,
                  const char         *name)
{
    int i;

    if (iface->members == NULL)
        return NULL;

    for (i = 0; iface->members[i].name != NULL; ++i) {
        if (strcmp(name, iface->members[i].name) == 0)
            return &iface->members[i];
    }
    return NULL;
}

static HippoDBusObject*
object_new(const char          *path,
           void                *object,
           HippoDBusInterface **ifaces,
           int                  n_ifaces)
{
    HippoDBusObject *o;
    int i;

    o = g_malloc0(G_STRUCT_OFFSET(HippoDBusObject, interfaces) +
                  sizeof(HippoDBusInterface*) * (n_ifaces + 1));
    o->path = g_strdup(path);
    o->object = object;

    for (i = 0; i < n_ifaces; ++i) {
        o->interfaces[i] = ifaces[i];
    }

    return o;
}

static void
object_free(HippoDBusObject *o)
{
    g_free(o->path);
    g_free(o);
}

static HippoDBusInterface*
object_find_interface(HippoDBusObject  *o,
                      const char       *name)
{
    int i;

    for (i = 0; o->interfaces[i] != NULL; ++i) {
        if (strcmp(name, o->interfaces[i]->name) == 0)
            return o->interfaces[i];
    }

    return NULL;
}

static const HippoDBusProperty*
object_find_property(HippoDBusObject    *o,
                     const char         *iface_name,
                     const char         *prop_name)
{
    HippoDBusInterface *iface;

    iface = object_find_interface(o, iface_name);
    if (iface == NULL)
        return NULL;

    return iface_find_property(iface, prop_name);
}

static const HippoDBusMember*
object_find_member(HippoDBusObject    *o,
                   const char         *iface_name,
                   const char         *member_name)
{
    if (iface_name != NULL) {
        HippoDBusInterface *iface;

        iface = object_find_interface(o, iface_name);
        if (iface == NULL)
            return NULL;

        return iface_find_member(iface, member_name);
    } else {
        int i;

        for (i = 0; o->interfaces[i] != NULL; ++i) {
            HippoDBusInterface *iface = o->interfaces[i];
            const HippoDBusMember *member = iface_find_member(iface, member_name);
            if (member != NULL)
                return member;
        }

        return NULL;
    }
}

static void
helper_free(void *data)
{
    HippoDBusHelper *helper = data;

    /* we should be disconnected, so these should have been notified...
     * unless we managed to unref the connection without ever
     * completing dispatch. This warning probably isn't hard to
     * fix but since it's probably an app bug if it happens,
     * let's leave it for now.
     */
    if (helper->connection_trackers != NULL) {
        g_warning("DBusConnection finalized without dispatching the incoming queue, which is not supported by hippo-dbus-helper.c right now (are you sure you meant to finalize it?)");
    }
    
    /* FIXME free the hash contents */

    g_hash_table_destroy(helper->services_by_well_known);
    g_hash_table_destroy(helper->services_by_owner);
    g_hash_table_destroy(helper->name_owners);
    g_hash_table_destroy(helper->interfaces);
    g_free(helper);
}

static dbus_int32_t slot = -1;

static HippoDBusHelper*
get_helper(DBusConnection *connection)
{
    HippoDBusHelper *helper;

    if (slot < 0)
        dbus_connection_allocate_data_slot(&slot);

    helper = dbus_connection_get_data(connection, slot);

    if (helper == NULL) {
        helper = g_new0(HippoDBusHelper, 1);
        helper->interfaces = g_hash_table_new(g_str_hash, g_str_equal);

        helper->services_by_well_known = g_hash_table_new(g_str_hash, g_str_equal);
        helper->services_by_owner = g_hash_table_new(g_str_hash, g_str_equal);

        /* string name to GSList of NameOwner */
        helper->name_owners = g_hash_table_new_full(g_str_hash, g_str_equal,
                                                    g_free, NULL);
        
        dbus_connection_set_data(connection, slot, helper, helper_free);

        if (!dbus_connection_add_filter(connection,
                                        hippo_dbus_helper_filter_message,
                                        NULL, NULL))
            g_error("no memory adding dbus helper connection filter");
    }

    return helper;
}

/* members and properties can be NULL (which means the same as empty) */
void
hippo_dbus_helper_register_interface(DBusConnection          *connection,
                                     const char              *name,
                                     const HippoDBusMember   *members,
                                     const HippoDBusProperty *properties)
{
    HippoDBusInterface *iface;
    HippoDBusHelper *helper;

    helper = get_helper(connection);
    
    iface = g_hash_table_lookup(helper->interfaces, name);
    if (iface == NULL) {    
        iface = iface_new(name, members, properties);
        g_hash_table_replace(helper->interfaces, iface->name, iface);
    } else {
        /* If we ever add unregister_interface() then we'll have to refcount
         * the interface object and increment the count here.
         * But since you can't unregister right now, we just do nothing
         * on a second registration.
         */
        if (iface->members != members ||
            iface->properties != properties) {
            g_warning("registered an interface twice, differently each time");
        }
    }
}

static void
unregister_object (DBusConnection  *connection,
                   void            *user_data)
{
    object_free(user_data);
}

static DBusHandlerResult
handle_object_message(DBusConnection  *connection,
                      DBusMessage     *message,
                      void            *user_data)
{
    return hippo_dbus_helper_handle_object_message(connection, message, user_data);
}

static DBusObjectPathVTable object_vtable = {
    unregister_object,
    handle_object_message,
    NULL, NULL, NULL, NULL
};

static void
hippo_dbus_helper_register_object_valist(DBusConnection    *connection,
                                         const char        *path,
                                         void              *object,
                                         const char        *first_interface,
                                         va_list            args)
{
    HippoDBusObject *o;
    HippoDBusHelper *helper;
#define MAX_IFACES 10
    HippoDBusInterface *ifaces[MAX_IFACES];
    int i;

    helper = get_helper(connection);

    i = 0;
    if (first_interface != NULL) {
        const char *name;
        ifaces[i] = g_hash_table_lookup(helper->interfaces, first_interface);
        g_return_if_fail(ifaces[i] != NULL);
        ++i;

        name = va_arg(args, const char*);
        while (name != NULL) {
            g_assert(i < MAX_IFACES);

            ifaces[i] = g_hash_table_lookup(helper->interfaces, name);
            g_return_if_fail(ifaces[i] != NULL);
            ++i;

            name = va_arg(args, const char*);
        }

        ifaces[i] = NULL;
    }

    o = object_new(path, object, ifaces, i);

    dbus_connection_register_object_path(connection, path, &object_vtable, o);
}

void
hippo_dbus_helper_unregister_object(DBusConnection    *connection,
                                    const char        *path)
{
    dbus_connection_unregister_object_path(connection, path);
}

gboolean
hippo_dbus_helper_object_is_registered (DBusConnection          *connection,
                                        const char              *path)
{
    void *object_data;

    dbus_connection_get_object_path_data(connection, path, &object_data);

    return object_data != NULL;
}

void
hippo_dbus_helper_register_object(DBusConnection    *connection,
                                  const char        *path,
                                  void              *object,
                                  const char        *first_interface,
                                  ...)
{
    va_list args;
    va_start(args, first_interface);
    hippo_dbus_helper_register_object_valist(connection, path, object, first_interface, args);
    va_end(args);
}

typedef struct {
    DBusConnection *connection;
    char *path;
} UnregisterInfo;

static void
unregister_destroyed_object(void         *data,
                            GObject      *where_the_object_was)
{
    UnregisterInfo *info = data;

    hippo_dbus_helper_unregister_object(info->connection, info->path);

    g_free(info->path);
    g_free(info);
}

void
hippo_dbus_helper_register_g_object(DBusConnection          *connection,
                                    const char              *path,
                                    GObject                 *object,
                                    const char              *first_interface,
                                    ...)
{
    va_list args;
    UnregisterInfo *info;
    
    va_start(args, first_interface);
    hippo_dbus_helper_register_object_valist(connection, path, object, first_interface, args);
    va_end(args);

    info = g_new(UnregisterInfo, 1);
    info->connection = connection;
    info->path = g_strdup(path);
    
    g_object_weak_ref(object, unregister_destroyed_object, info);
}

static DBusHandlerResult
handle_properties(DBusConnection  *connection,
                  DBusMessage     *message,
                  const char      *iface,
                  const char      *member,
                  HippoDBusObject *o)
{
    DBusMessage *reply;
    DBusError derror;

    if (!(iface == NULL || strcmp(iface, DBUS_INTERFACE_PROPERTIES) == 0))
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    reply = NULL;
    dbus_error_init(&derror);

    if (strcmp(member, "Get") == 0) {
        const char *prop_iface;
        const char *prop_name;
        const HippoDBusProperty *prop;
        DBusMessageIter iter;
        DBusMessageIter variant_iter;

        if (!dbus_message_get_args(message, &derror,
                                   DBUS_TYPE_STRING, &prop_iface,
                                   DBUS_TYPE_STRING, &prop_name,
                                   DBUS_TYPE_INVALID))
            goto out;

        prop = object_find_property(o, prop_iface, prop_name);
        if (prop == NULL) {
            dbus_set_error(&derror, DBUS_ERROR_FAILED,
                           _("Object has no property '%s' on interface '%s'"),
                           prop_name, prop_iface);
            goto out;
        }

        if (prop->getter == NULL) {
            dbus_set_error(&derror, DBUS_ERROR_FAILED,
                           _("Object property '%s' on interface '%s' is not readable"),
                           prop_name, prop_iface);
            goto out;
        }

        reply = dbus_message_new_method_return(message);
        dbus_message_iter_init_append(reply, &iter);
        dbus_message_iter_open_container(&iter, DBUS_TYPE_VARIANT,
                                         prop->signature,
                                         &variant_iter);
        if (!(* prop->getter)(o->object, prop->name, &variant_iter, &derror)) {
            dbus_message_unref(reply);
            reply = NULL;
        } else {
            dbus_message_iter_close_container(&iter, &variant_iter);
            
        }
    } else if (strcmp(member, "Set") == 0) {
        const char *prop_iface;
        const char *prop_name;
        DBusMessageIter iter;
        DBusMessageIter variant_iter;
        char *value_signature;
        const HippoDBusProperty *prop;

        dbus_message_iter_init(message, &iter);
        if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
            dbus_set_error(&derror, DBUS_ERROR_INVALID_ARGS,
                           _("First argument should be an interface name"));
            goto out;
        }
        dbus_message_iter_get_basic(&iter, &prop_iface);
        dbus_message_iter_next(&iter);

        if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING) {
            dbus_set_error(&derror, DBUS_ERROR_INVALID_ARGS,
                           _("Second argument should be a property name"));
            goto out;
        }
        dbus_message_iter_get_basic(&iter, &prop_name);
        dbus_message_iter_next(&iter);

        prop = object_find_property(o, prop_iface, prop_name);
        if (prop == NULL) {
            dbus_set_error(&derror, DBUS_ERROR_FAILED,
                           _("Object has no property '%s' on interface '%s'"),
                           prop_name, prop_iface);
            goto out;
        }

        if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_VARIANT) {
            dbus_set_error(&derror, DBUS_ERROR_INVALID_ARGS,
                           _("Third argument should be a property value (variant)"));
            goto out;
        }

        dbus_message_iter_recurse(&iter, &variant_iter);
        value_signature = dbus_message_iter_get_signature(&variant_iter);
        if (value_signature == NULL || strcmp(value_signature, prop->signature) != 0) {
            dbus_set_error(&derror, DBUS_ERROR_INVALID_ARGS,
                           _("Value of property '%s' has type '%s' but '%s' expected"),
                           prop_name, value_signature, prop->signature);
            dbus_free(value_signature);
            goto out;
        }
        dbus_free(value_signature);

        if (prop->setter == NULL) {
            dbus_set_error(&derror, DBUS_ERROR_FAILED,
                           _("Object property '%s' on interface '%s' is not writable"),
                           prop_name, prop_iface);
            goto out;
        }

        if (!(* prop->setter)(o->object, prop->name, &variant_iter, &derror)) {
            goto out;
        }

        /* send an "ack" reply */
        reply = dbus_message_new_method_return(message);

    } else if (strcmp(member, "GetAll") == 0) {
        const char *prop_iface;
        HippoDBusInterface *iface;
        int i;
        DBusMessageIter iter;
        DBusMessageIter dict_iter;

        if (!dbus_message_get_args(message, &derror,
                                   DBUS_TYPE_STRING, &prop_iface,
                                   DBUS_TYPE_INVALID))
            goto out;

        iface = object_find_interface(o, prop_iface);
        if (iface == NULL) {
            dbus_set_error(&derror, DBUS_ERROR_FAILED,
                           _("Object has no interface '%s'"),
                           prop_iface);
            goto out;
        }

        reply = dbus_message_new_method_return(message);
        dbus_message_iter_init_append(reply, &iter);
        dbus_message_iter_open_container(&iter,
                                         DBUS_TYPE_ARRAY,
                                         DBUS_DICT_ENTRY_BEGIN_CHAR_AS_STRING
                                         DBUS_TYPE_STRING_AS_STRING
                                         DBUS_TYPE_VARIANT_AS_STRING
                                         DBUS_DICT_ENTRY_END_CHAR_AS_STRING,
                                         &dict_iter);

        if (iface->properties != NULL) {
            for (i = 0; iface->properties[i].name != NULL; ++i) {
                DBusMessageIter entry_iter;
                DBusMessageIter variant_iter;
                const HippoDBusProperty *prop;

                prop = &iface->properties[i];

                if (prop->getter != NULL) {

                    dbus_message_iter_open_container(&dict_iter, DBUS_TYPE_DICT_ENTRY,
                                                     NULL, &entry_iter);
                    dbus_message_iter_append_basic(&entry_iter, DBUS_TYPE_STRING,
                                                   &prop->name);
                    dbus_message_iter_open_container(&entry_iter, DBUS_TYPE_VARIANT,
                                                     prop->signature,
                                                     &variant_iter);
                    if (!(* prop->getter)(o->object, prop->name, &variant_iter, &derror)) {
                        dbus_message_unref(reply);
                        reply = NULL;
                        goto out;
                    }

                    dbus_message_iter_close_container(&entry_iter, &variant_iter);
                    dbus_message_iter_close_container(&dict_iter, &entry_iter);
                }
            }
        }
        dbus_message_iter_close_container(&iter, &dict_iter);

    } else {
        /* don't send an error if iface is NULL, since we'll continue on to
         * look for some interface that implements the method name
         */
        if (iface != NULL) {
            reply = dbus_message_new_error_printf(message,
                                                  DBUS_ERROR_UNKNOWN_METHOD,
                                                  _("No method '%s' in %s interface"),
                                                  member, DBUS_INTERFACE_PROPERTIES);
        }
    }

 out:

    if (dbus_error_is_set(&derror)) {
        g_assert(reply == NULL);

        reply = dbus_message_new_error(message,
                                       derror.name,
                                       derror.message);
        dbus_error_free(&derror);
    }

    if (reply != NULL) {
        dbus_connection_send(connection, reply, NULL);
        dbus_message_unref(reply);
        return DBUS_HANDLER_RESULT_HANDLED;
    } else {
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    }
}

static DBusHandlerResult
handle_method(DBusConnection  *connection,
              DBusMessage     *message,
              const char      *iface,
              const char      *member_name,
              HippoDBusObject *o)
{
    const HippoDBusMember *member;
    DBusMessage *reply;
    DBusError derror;

    reply = NULL;
    dbus_error_init(&derror);

    member = object_find_member(o, iface, member_name);
    if (member == NULL)
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    if (member->member_type != HIPPO_DBUS_MEMBER_METHOD) {
        dbus_set_error(&derror, DBUS_ERROR_FAILED,
                       _("'%s' is a signal, you tried to invoke it as a method"),
                       member_name);
        goto out;
    }

    if (!dbus_message_has_signature(message, member->in_args)) {
        dbus_set_error(&derror, DBUS_ERROR_FAILED,
                       _("'%s' should have signature '%s' not '%s'"),
                       member_name, member->in_args, dbus_message_get_signature(message));
        goto out;
    }
    
    reply = (* member->handler) (o->object, message, &derror);

    if (reply != NULL && dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        if (!dbus_message_has_signature(reply, member->out_args)) {
            g_warning("Wrong method reply signature for '%s' should be '%s' was '%s'",
                      member_name, member->out_args, dbus_message_get_signature(reply));
            goto out;
        }
    }
    
 out:

    if (dbus_error_is_set(&derror)) {
        g_assert(reply == NULL);

        reply = dbus_message_new_error(message,
                                       derror.name,
                                       derror.message);
        dbus_error_free(&derror);
    }

    /* Null reply means the handler is handling the method async */
    if (reply != NULL) {
        dbus_connection_send(connection, reply, NULL);
        dbus_message_unref(reply);
    }

    return DBUS_HANDLER_RESULT_HANDLED;
}

static void
append_properties(GString                 *xml,
                  const HippoDBusProperty *props)
{
    int i;

    for (i = 0; props[i].name != NULL; ++i) {
        const char *rw;

        if (props[i].getter && props[i].setter)
            rw = "readwrite";
        else if (props[i].getter)
            rw = "read";
        else if (props[i].setter)
            rw = "write";
        else
            rw = ""; /* this is not going to work out well */
        
        g_string_append_printf(xml,
                               "    <property name=\"%s\" type=\"%s\" access=\"%s\"/>\n",
                               props[i].name, props[i].signature, rw);
        
    }
}

static void
write_args_for_direction (GString    *xml,
			  const char *signature,
			  dbus_bool_t in)
{
    DBusSignatureIter iter;
    int current_type;

    dbus_signature_iter_init(&iter, signature);

    while ((current_type = dbus_signature_iter_get_current_type (&iter)) != DBUS_TYPE_INVALID) {
        char *sig;

        sig = dbus_signature_iter_get_signature(&iter);

        g_string_append_printf(xml,
                               "      <arg direction=\"%s\" type=\"%s\"/>\n",
                               in ? "in" : "out",
                               sig);

        dbus_free(sig);

        dbus_signature_iter_next(&iter);
    }
}

static void
append_members(GString               *xml,
               const HippoDBusMember *members)
{
    int i;

    for (i = 0; members[i].name != NULL; ++i) {

        const char *type_name;

        if (members[i].member_type == HIPPO_DBUS_MEMBER_METHOD)
            type_name = "method";
        else
            type_name = "signal";

        g_string_append_printf(xml,
                               "    <%s name=\"%s\">\n",
                               type_name, members[i].name);

        if (members[i].in_args != NULL)
            write_args_for_direction(xml, members[i].in_args, TRUE);
        if (members[i].out_args != NULL)
            write_args_for_direction(xml, members[i].out_args, FALSE);

        g_string_append_printf(xml,
                               "    </%s>\n",
                               type_name);
    }
}

static void
append_interface(GString            *xml,
                 HippoDBusInterface *iface)
{
    g_string_append_printf(xml,
                           "  <interface name=\"%s\">\n",
                           iface->name);

    if (iface->members != NULL)
        append_members(xml, iface->members);

    if (iface->properties != NULL)
        append_properties(xml, iface->properties);

    g_string_append       (xml,
                           "  </interface>\n");
}


static DBusHandlerResult
handle_introspection(DBusConnection  *connection,
                     DBusMessage     *message,
                     const char      *iface,
                     const char      *member,
                     HippoDBusObject *o)
{
    DBusMessage *reply;
    DBusError derror;
    GString *xml;
    int i;
    char **children;  
    
    if (!(iface == NULL || strcmp(iface, DBUS_INTERFACE_INTROSPECTABLE) == 0))
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    
    reply = NULL;
    dbus_error_init(&derror);

    xml = g_string_new(NULL);

    g_string_append(xml, DBUS_INTROSPECT_1_0_XML_DOCTYPE_DECL_NODE);

    g_string_append       (xml,
                           "<node>\n");

    /* Some boilerplate here from dbus-glib */
    
    /* We are introspectable, though I guess that was pretty obvious */
    g_string_append_printf (xml, "  <interface name=\"%s\">\n", DBUS_INTERFACE_INTROSPECTABLE);
    g_string_append (xml, "    <method name=\"Introspect\">\n");
    g_string_append_printf (xml, "      <arg name=\"data\" direction=\"out\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
    g_string_append (xml, "    </method>\n");
    g_string_append (xml, "  </interface>\n");

    /* We support get/set properties */
    g_string_append_printf (xml, "  <interface name=\"%s\">\n", DBUS_INTERFACE_PROPERTIES);
    g_string_append (xml, "    <method name=\"Get\">\n");
    g_string_append_printf (xml, "      <arg name=\"interface\" direction=\"in\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
    g_string_append_printf (xml, "      <arg name=\"propname\" direction=\"in\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
    g_string_append_printf (xml, "      <arg name=\"value\" direction=\"out\" type=\"%s\"/>\n", DBUS_TYPE_VARIANT_AS_STRING);
    g_string_append (xml, "    </method>\n");
    g_string_append (xml, "    <method name=\"Set\">\n");
    g_string_append_printf (xml, "      <arg name=\"interface\" direction=\"in\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
    g_string_append_printf (xml, "      <arg name=\"propname\" direction=\"in\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);
    g_string_append_printf (xml, "      <arg name=\"value\" direction=\"in\" type=\"%s\"/>\n", DBUS_TYPE_VARIANT_AS_STRING);
    g_string_append (xml, "    </method>\n");
    g_string_append (xml, "  </interface>\n");

    for (i = 0; o->interfaces[i] != NULL; ++i) {
        append_interface(xml, o->interfaces[i]);
    }

    /* Child nodes */
    if (!dbus_connection_list_registered (connection, 
                                          dbus_message_get_path (message),
                                          &children))
        g_error ("Out of memory");

    for (i = 0; children[i]; i++)
        g_string_append_printf (xml, "  <node name=\"%s\"/>\n",
                                children[i]);

    dbus_free_string_array(children);

    /* Close the document */
    g_string_append(xml, "</node>\n");

    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);

    /* out: */
    if (dbus_error_is_set(&derror)) {
        g_assert(reply == NULL);

        reply = dbus_message_new_error(message,
                                       derror.name,
                                       derror.message);
        dbus_error_free(&derror);
    }

    if (reply != NULL) {
        dbus_connection_send(connection, reply, NULL);
        dbus_message_unref(reply);
        return DBUS_HANDLER_RESULT_HANDLED;
    } else {
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    }
}

static DBusHandlerResult
hippo_dbus_helper_handle_object_message(DBusConnection  *connection,
                                        DBusMessage     *message,
                                        HippoDBusObject *o)
{
    const char *path;
    const char *member;
    const char *iface;

    if (dbus_message_get_type(message) != DBUS_MESSAGE_TYPE_METHOD_CALL)
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    path = dbus_message_get_path(message);

    if (path == NULL)
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    member = dbus_message_get_member(message);
    g_assert(member != NULL); /* libdbus should not allow */

    /* remember interface can be NULL which means "any interface" */
    iface = dbus_message_get_interface(message);

    {
        /* Try properties-related method calls */

        DBusHandlerResult result =
            handle_properties(connection, message, iface, member, o);
        if (result != DBUS_HANDLER_RESULT_NOT_YET_HANDLED)
            return result;
    }

    {
        /* Try other method calls */

        DBusHandlerResult result =
            handle_method(connection, message, iface, member, o);
        if (result != DBUS_HANDLER_RESULT_NOT_YET_HANDLED)
            return result;
    }

    {
        /* Try introspection */

        DBusHandlerResult result =
            handle_introspection(connection, message, iface, member, o);
        if (result != DBUS_HANDLER_RESULT_NOT_YET_HANDLED)
            return result;
    }

    {
        /* Our object doesn't support this method */
        DBusMessage *reply;
        reply = dbus_message_new_error_printf(message, DBUS_ERROR_UNKNOWN_METHOD,
                                              _("Unknown method '%s' on object '%s'"),
                                              member, path);
        dbus_connection_send(connection, reply, NULL);
        dbus_message_unref(reply);

        return DBUS_HANDLER_RESULT_HANDLED;
    }
}

static void
emit_signal_valist_appender(DBusConnection          *connection,
                            const char              *path,
                            const char              *interface,
                            const char              *signal_name,
                            HippoDBusArgAppender     appender,
                            void                    *appender_data,
                            int                      first_arg_type,
                            va_list                  args)
{
    DBusMessage *message;
    void *object_data;
    const HippoDBusMember *member;
    
    dbus_connection_get_object_path_data(connection, path, &object_data);
    if (object_data == NULL) {
        g_warning("No object at %s found to emit %s", path, signal_name);
        return;
    }

    member = object_find_member(object_data, interface, signal_name);
    if (member == NULL) {
        g_warning("Object %s does not have signal %s on %s",
                  path, signal_name, interface);
        return;
    }
        
    message = dbus_message_new_signal(path, interface, signal_name);

    if (first_arg_type != DBUS_TYPE_INVALID) {
        dbus_message_append_args_valist(message, first_arg_type, args);
    }
    
    if (appender != NULL) {
        if (!(* appender)(message, appender_data)) {
            g_warning("no memory to append signal args");
        }
    }
    
    if (!dbus_message_has_signature(message, member->out_args)) {
        g_warning("Tried to emit signal %s %s with args %s but should have been %s",
                  interface, signal_name, dbus_message_get_signature(message), member->out_args);
        dbus_message_unref(message);
        return;
    }
    
    dbus_connection_send(connection, message, NULL);

    dbus_message_unref(message);
}

void
hippo_dbus_helper_emit_signal_valist(DBusConnection          *connection,
                                     const char              *path,
                                     const char              *interface,
                                     const char              *signal_name,
                                     int                      first_arg_type,
                                     va_list                  args)
{
    emit_signal_valist_appender(connection, path, interface, signal_name,
                                NULL, NULL, first_arg_type, args);
}

void
hippo_dbus_helper_emit_signal_appender (DBusConnection          *connection,
                                        const char              *path,
                                        const char              *interface,
                                        const char              *signal_name,
                                        HippoDBusArgAppender     appender,
                                        void                    *appender_data)
{
    va_list dummy_args;
    
    emit_signal_valist_appender(connection, path, interface, signal_name,
                                appender, appender_data, DBUS_TYPE_INVALID, dummy_args);
}

void
hippo_dbus_helper_emit_signal(DBusConnection          *connection,
                              const char              *path,
                              const char              *interface,
                              const char              *signal_name,
                              int                      first_arg_type,
                              ...)
{
    va_list args;
    
    va_start(args, first_arg_type);

    hippo_dbus_helper_emit_signal_valist(connection, path, interface, signal_name,
                                         first_arg_type, args);
    va_end(args);
}

static HippoDBusService*
service_new(DBusConnection                *connection,
            const char                    *well_known_name,
            const HippoDBusServiceTracker *tracker,
            const HippoDBusSignalTracker  *signal_handlers,
            void                          *data)
{
    HippoDBusService *service;

    service = g_new0(HippoDBusService, 1);
    service->refcount = 1;
    service->connection = connection;
    service->well_known_name = g_strdup(well_known_name);
    service->tracker = tracker;
    service->signal_handlers = signal_handlers;
    service->data = data;

    return service;
}

static HippoDBusService *
service_ref(HippoDBusService *service)
{
    service->refcount++;

    return service;
}

static void
service_unref(HippoDBusService *service)
{
    service->refcount--;
    if (service->refcount == 0) {
        g_free(service->well_known_name);
        g_free(service->owner);
        g_free(service);
    }
}

static void
set_signal_handlers_matched(DBusConnection                *connection,
                            const char                    *bus_name,
                            const HippoDBusSignalTracker  *signal_handlers,
                            gboolean                       matched)
{
    int i;

    for (i = 0; signal_handlers[i].interface != NULL; ++i) {
        char *s;
        
        s = g_strdup_printf("type='signal',sender='"
                            "%s"
                            "',interface='"
                            "%s"
                            "',member='"
                            "%s"
                            "'",
                            bus_name,
                            signal_handlers[i].interface,
                            signal_handlers[i].signal);
        
        /* Since we don't ask for the error, this should be async (not block) */
        if (matched)
            dbus_bus_add_match(connection, s, NULL);
        else
            dbus_bus_remove_match(connection, s, NULL);

        g_free(s);
    }
}

static void
set_owner_matched(DBusConnection *connection,
                  const char     *bus_name,
                  gboolean        matched)
{
    char *s;
    
    s = g_strdup_printf("type='signal',sender='"
                        DBUS_SERVICE_DBUS
                        "',interface='"
                        DBUS_INTERFACE_DBUS
                        "',member='"
                        "NameOwnerChanged"
                        "',arg0='%s'",
                        bus_name);
    
    /* this is nonblocking since we don't ask for errors */
    if (matched)
        dbus_bus_add_match(connection,
                           s, NULL);
    else
        dbus_bus_remove_match(connection, s, NULL);
    
    g_free(s);
}

static void
on_startup_nonexistence(HippoDBusService *service)
{
    if (!service->removed)
        (* service->tracker->unavailable_handler) (service->connection,
                                                   service->well_known_name,
                                                   NULL,
                                                   service->data);
}

static void
handle_name_owner_changed(DBusConnection *connection,
                          const char     *well_known_name,
                          const char     *old_owner,
                          const char     *new_owner)
{
    HippoDBusHelper *helper;
    HippoDBusService *service;
    
    helper = get_helper(connection); 
   
    service = g_hash_table_lookup(helper->services_by_well_known, well_known_name);
    if (service == NULL)
        return; /* we don't care about this */

    if ((new_owner == NULL ||
         (service->owner != NULL && strcmp(service->owner, new_owner) != 0))) {
        /* Our previously-believed owner is going away, or we didn't find
         * the service on startup. */
        char *owner;

        g_hash_table_remove(helper->services_by_owner,
                            service->owner);
        owner = service->owner;
        service->owner = NULL;
        
        g_debug("Service '%s' is now unavailable, old owner was '%s'",
                service->well_known_name,
                owner ? owner : "");

        (* service->tracker->unavailable_handler) (connection,
                                                   service->well_known_name,
                                                   owner,
                                                   service->data);
        g_free(owner);
    }

    if (service->owner == NULL && new_owner) {
        service->owner = g_strdup(new_owner);
        /* We had no owner and now we do */
        
        g_hash_table_replace(helper->services_by_owner,
                             service->owner, service);

        g_debug("Service '%s' is now available, new owner is '%s'",
                service->well_known_name,
                service->owner);
        
        (* service->tracker->available_handler) (connection,
                                                 service->well_known_name,
                                                 service->owner,
                                                 service->data);
    }
}

static void
on_start_service_by_name_reply(DBusPendingCall *pending,
                               void            *user_data)
{
    DBusMessage *reply;
    HippoDBusService *service;

    service = user_data;
    
    reply = dbus_pending_call_steal_reply(pending);
    if (reply == NULL) {
        g_warning("NULL reply in on_start_service_by_name_reply?");
        return;
    }
    
    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        /* Just ignore, we'll get a NameOwnerChanged */
    } else {
        const char *message = "Unknown error";
        dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &message, DBUS_TYPE_INVALID);
        
        g_warning("Initial activation of service '%s' failed: %s", service->well_known_name, message);
        on_startup_nonexistence(service);        
    }
}

static void
on_get_owner_reply(DBusPendingCall *pending,
                   void            *user_data)
{
    DBusMessage *reply;
    HippoDBusService *service;

    service = user_data;
    
    reply = dbus_pending_call_steal_reply(pending);
    if (reply == NULL) {
        g_warning("NULL reply in on_get_owner_reply?");
        return;
    }
    
    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        const char *v_STRING = NULL;
        if (!dbus_message_get_args(reply, NULL,
                                   DBUS_TYPE_STRING, &v_STRING,
                                   DBUS_TYPE_INVALID)) {
            g_warning("GetNameOwner has wrong args '%s'",
                      dbus_message_get_signature(reply));
            on_startup_nonexistence(service);
        } else {
            handle_name_owner_changed(service->connection,
                                      service->well_known_name,
                                      NULL,
                                      v_STRING);
        }
    } else  {
        if (strcmp(dbus_message_get_error_name(reply),
                   "org.freedesktop.DBus.Error.NameHasNoOwner") == 0) {

            /* One time on startup, we will start the service if
             * it's not running, which should lead to a notification.
             * We don't check the error though and just give up
             * if it fails.
             */
            if ((service->tracker->flags & HIPPO_DBUS_SERVICE_START_IF_NOT_RUNNING) != 0) {
                DBusMessage *msg;
                dbus_uint32_t flags;
                DBusPendingCall *call;

                msg = dbus_message_new_method_call (DBUS_SERVICE_DBUS,
                                                    DBUS_PATH_DBUS,
                                                    DBUS_INTERFACE_DBUS,
                                                    "StartServiceByName");
                
                flags = 0;
                if (!dbus_message_append_args (msg, DBUS_TYPE_STRING, &service->well_known_name,
                                               DBUS_TYPE_UINT32, &flags, DBUS_TYPE_INVALID))
                    g_error("out of memory");

                call = NULL;
                if (!dbus_connection_send_with_reply(service->connection, msg, &call, -1))
                    g_error("out of memory");

                /* Call == NULL means that we're already disconnected; in that case, we'll
                 * omit any notification and assume that we're in some sort of
                 * start-and-immediately-exit race.
                 */
                if (call != NULL) {
                    if (!dbus_pending_call_set_notify(call, on_start_service_by_name_reply,
                                                      service_ref(service),
                                                      (DBusFreeFunction) service_unref))
                        g_error("out of memory");

                    /* rely on connection to hold a reference to it */
                    dbus_pending_call_unref(call);
                }
                dbus_message_unref(msg);
            } else {
                on_startup_nonexistence(service);
            }
            
        } else {
            const char *message = "Unknown error";
        
            dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &message, DBUS_TYPE_INVALID);
            g_warning("GetNameOwner failed: %s", message);
            on_startup_nonexistence(service);
        }
    }
    
    dbus_message_unref(reply);
}

void
hippo_dbus_helper_register_service_tracker (DBusConnection                *connection,
                                            const char                    *well_known_name,
                                            const HippoDBusServiceTracker *tracker,
                                            const HippoDBusSignalTracker  *signal_handlers,
                                            void                          *data)
{
    HippoDBusHelper *helper;
    HippoDBusService *service;
    DBusPendingCall *call;
    DBusMessage *get_owner;
    
    helper = get_helper(connection);

    /* multiple registrations for the same name isn't allowed for now */
    g_return_if_fail(g_hash_table_lookup(helper->services_by_well_known, well_known_name) == NULL);
    
    service = service_new(connection, well_known_name, tracker, signal_handlers, data);

    g_hash_table_replace(helper->services_by_well_known, service->well_known_name, service);

    set_signal_handlers_matched(connection, well_known_name, signal_handlers, TRUE);

    set_owner_matched(connection, well_known_name, TRUE);

    /* Initially get the owner */

    get_owner = dbus_message_new_method_call(DBUS_SERVICE_DBUS, DBUS_PATH_DBUS,
                                             DBUS_INTERFACE_DBUS,
                                             "GetNameOwner");
    if (get_owner == NULL)
        g_error("out of memory");
    if (!dbus_message_append_args(get_owner, DBUS_TYPE_STRING, &well_known_name,
                                  DBUS_TYPE_INVALID))
        g_error("out of memory");
    
    call = NULL;               
    if (!dbus_connection_send_with_reply(connection, get_owner, &call, -1))
        g_error("out of memory");

    /* Call == NULL means that we're already disconnected; in that case, we'll
     * omit any notification and assume that we're in some sort of
     * start-and-immediately-exit race.
     */
    if (call != NULL) {
        if (!dbus_pending_call_set_notify(call, on_get_owner_reply,
                                          service_ref(service),
                                          (DBusFreeFunction) service_unref))
            g_error("out of memory");
        
        /* rely on connection to hold a reference to it, if finalized
         * I think on_get_owner_reply won't get called. Again we just omit
         * notification.
         */
        dbus_pending_call_unref(call);
    }
}

void
hippo_dbus_helper_unregister_service_tracker (DBusConnection                *connection,
                                              const char                    *well_known_name,
                                              const HippoDBusServiceTracker *tracker,
                                              void                          *data)
{
    HippoDBusHelper *helper;
    HippoDBusService *service;
    
    helper = get_helper(connection);

    service = g_hash_table_lookup(helper->services_by_well_known, well_known_name);
    g_return_if_fail(service != NULL);
    if (service->tracker != tracker) {
        g_warning("Multiple registered trackers for same service doesn't work yet");
        return;
    }

    service->removed = TRUE;
    
    set_signal_handlers_matched(connection, well_known_name, service->signal_handlers, FALSE);
    set_owner_matched(connection, well_known_name, FALSE);
    
    if (service->owner)
        g_hash_table_remove(helper->services_by_owner, service->owner);
    g_hash_table_remove(helper->services_by_well_known, service->well_known_name);

    if (service->owner) {
        (* service->tracker->unavailable_handler) (connection,
                                                   service->well_known_name,
                                                   service->owner,
                                                   service->data);
    }

    service_unref(service);
}

static gboolean attempt_session_connect_timeout(void *data);

static void
ensure_session_connect_attempt(void)
{
    if (try_session_connect_timeout == 0 &&
        session_connection_trackers != NULL) {
        int interval_secs;
        if (session_ever_attempted) {
            interval_secs = 15; /* retry periodically */
        } else {
            interval_secs = 0;  /* on startup, try immediately */
        }
        /* FIXME use g_timeout_add_seconds() once it's been available longer */
        try_session_connect_timeout =            
            g_timeout_add(interval_secs * 1000, attempt_session_connect_timeout, NULL);
    }
}

static gboolean
attempt_session_connect_timeout(void *data)
{
    DBusConnection *connection;
    GSList *tmp;
    GSList *copy;
    HippoDBusHelper *helper;

    /* we always return FALSE to remove ourselves */
    try_session_connect_timeout = 0;
    
    session_ever_attempted = TRUE;
    
    connection = dbus_bus_get(DBUS_BUS_SESSION, NULL);
    if (connection == NULL) {
        /* try again (since we set session_ever_attempted = TRUE, this
         * will be after a timeout; also we may exit if that's what the
         * app normally does on session bus disconnect)
         */
        ensure_session_connect_attempt();
        return FALSE; /* remove ourselves */
    }

    dbus_connection_setup_with_g_main(connection, NULL);

    helper = get_helper(connection);
    
    /* this is broken of course, since it means
     * we can invoke a handler after it's removed.
     */
    copy = g_slist_copy(session_connection_trackers);

    for (tmp = copy; tmp != NULL; tmp = tmp->next) {
        HippoDBusConnection *closure = tmp->data;

        closure->connection = connection;
        dbus_connection_ref(closure->connection);
        
        (* closure->tracker->connected_handler) (closure->connection, closure->data);

        helper->connection_trackers = g_slist_prepend(helper->connection_trackers,
                                                      closure);
    }

    g_slist_free(copy);

    dbus_connection_unref(connection);

    return FALSE;
}

void
hippo_dbus_helper_register_connection_tracker (DBusBusType                       bus_type,
                                               const HippoDBusConnectionTracker *tracker,
                                               void                             *data)
{
    HippoDBusConnection *closure;
    
    if (bus_type != DBUS_BUS_SESSION) {
        g_warning ("Only the session bus is supported in %s for now", __FUNCTION__);
        return;
    }

    closure = g_new0(HippoDBusConnection, 1);
    closure->tracker = tracker;
    closure->data = data;
    
    session_connection_trackers = g_slist_append(session_connection_trackers, closure);

    ensure_session_connect_attempt();
}

void
hippo_dbus_helper_unregister_connection_tracker (DBusBusType                       bus_type,
                                                 const HippoDBusConnectionTracker *tracker,
                                                 void                             *data)
{
    GSList *tmp;
    HippoDBusConnection *closure;

    closure = NULL;
    for (tmp = session_connection_trackers; tmp != NULL; tmp = tmp->next) {
        closure = tmp->data;
        if (closure->tracker == tracker && closure->data == data)
            break;
    }

    if (closure == NULL) {
        g_warning("attempted to unregister not-registered connection tracker");
        return;
    }

    session_connection_trackers = g_slist_remove(session_connection_trackers, closure);

    if (closure->connection) {
        HippoDBusHelper *helper;
        
        helper = get_helper(closure->connection);

        helper->connection_trackers = g_slist_remove(helper->connection_trackers, closure);
        
        (* closure->tracker->disconnected_handler) (closure->connection, closure->data);
        dbus_connection_unref(closure->connection);
        closure->connection = NULL;
    }
    
    g_free(closure);
}

static NameOwner*
name_owner_new(const char                   *well_known_name,
               HippoDBusNameOwnershipStyle   ownership_style,
               const HippoDBusNameOwner     *owner,
               void                         *data)
{
    NameOwner *no;

    no = g_new0(NameOwner, 1);
    no->state = NAME_OWNERSHIP_UNKNOWN;
    no->well_known_name = g_strdup(well_known_name);
    no->style = ownership_style;
    no->owner = owner;
    no->data = data;

    return no;
}

static void
name_owner_free(NameOwner *no)
{
    g_free(no->well_known_name);
    g_free(no);
}

/* FIXME I think this isn't really needed since the bus always sends us
 * this signal even if we don't match it
 */
static void
set_name_acquired_matched(DBusConnection *connection,
                          const char     *bus_name,
                          gboolean        matched)
{
    char *s;
    
    s = g_strdup_printf("type='signal',sender='"
                        DBUS_SERVICE_DBUS
                        "',interface='"
                        DBUS_INTERFACE_DBUS
                        "',member='"
                        "NameAcquired"
                        "',arg0='%s'",
                        bus_name);
    
    /* this is nonblocking since we don't ask for errors */
    if (matched)
        dbus_bus_add_match(connection,
                           s, NULL);
    else
        dbus_bus_remove_match(connection, s, NULL);
    
    g_free(s);
}

/* FIXME I think this isn't really needed since the bus always sends us
 * this signal even if we don't match it
 */
static void
set_name_lost_matched(DBusConnection *connection,
                      const char     *bus_name,
                      gboolean        matched)
{
    char *s;
    
    s = g_strdup_printf("type='signal',sender='"
                        DBUS_SERVICE_DBUS
                        "',interface='"
                        DBUS_INTERFACE_DBUS
                        "',member='"
                        "NameLost"
                        "',arg0='%s'",
                        bus_name);
    
    /* this is nonblocking since we don't ask for errors */
    if (matched)
        dbus_bus_add_match(connection,
                           s, NULL);
    else
        dbus_bus_remove_match(connection, s, NULL);
    
    g_free(s);
}

static gboolean
request_name(DBusConnection             *connection,
             const char                 *bus_name,
             HippoDBusNameOwnershipStyle ownership_style)
{
    unsigned int flags;
    int result;

    flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (ownership_style != HIPPO_DBUS_NAME_OWNED_OPTIONALLY)        
        flags |= DBUS_NAME_FLAG_DO_NOT_QUEUE;
    if (ownership_style == HIPPO_DBUS_NAME_SINGLE_INSTANCE_REPLACING_CURRENT_OWNER)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;

    /* FIXME this should really be async */

    /* on error, result is -1 */
    result = dbus_bus_request_name(connection, bus_name,
                                   flags,
                                   NULL);
    
    if (result == DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER ||
        result == DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER) {
        return TRUE;
    } else {
        return FALSE;
    }
}

static void
handle_name_ownership_maybe_changed(DBusConnection *connection,
                                    const char     *name,
                                    gboolean        owned)
{
    GSList *list;
    GSList *l;
    HippoDBusHelper *helper;

    helper = get_helper(connection);

    list = g_hash_table_lookup(helper->name_owners, name);
    if (list == NULL)
        return;

    /* FIXME not safe against registration/unregistration during callbacks */
    
    for (l = list; l != NULL; l = l->next) {
        NameOwner *no = l->data;

        if (owned && no->state != NAME_OWNERSHIP_OWNED) {
            no->state = NAME_OWNERSHIP_OWNED;
            (* no->owner->owned_handler) (connection, no->data);            
        } else if (!owned && no->state != NAME_OWNERSHIP_NOT_OWNED) {
            no->state = NAME_OWNERSHIP_NOT_OWNED;
            (* no->owner->not_owned_handler) (connection, no->data);
        }
    }
}

void
hippo_dbus_helper_register_name_owner(DBusConnection                   *connection,
                                      const char                       *well_known_name,
                                      HippoDBusNameOwnershipStyle       ownership_style,
                                      const HippoDBusNameOwner         *owner,
                                      void                             *data)
{
    NameOwner *no;
    GSList *list;
    HippoDBusHelper *helper;
    gboolean owned;

    helper = get_helper(connection);

    no = name_owner_new(well_known_name, ownership_style, owner, data);

    list = g_hash_table_lookup(helper->name_owners, well_known_name);
    list = g_slist_prepend(list, no);
    g_hash_table_replace(helper->name_owners, g_strdup(well_known_name), list);

    set_name_acquired_matched(connection, well_known_name, TRUE);
    set_name_lost_matched(connection, well_known_name, TRUE);

    owned = request_name(connection, well_known_name, ownership_style);
    
    handle_name_ownership_maybe_changed(connection, well_known_name,
                                        owned);
}

void
hippo_dbus_helper_unregister_name_owner(DBusConnection                   *connection,
                                        const char                       *well_known_name,
                                        const HippoDBusNameOwner         *owner,
                                        void                             *data)
{    
    NameOwner *no;
    GSList *list;
    GSList *l;
    HippoDBusHelper *helper;

    helper = get_helper(connection);

    list = g_hash_table_lookup(helper->name_owners, well_known_name);

    no = NULL;
    for (l = list; l != NULL; l = l->next) {
        NameOwner *existing = l->data;
        if (existing->owner == owner &&
            existing->data == data &&
            strcmp(existing->well_known_name, well_known_name) == 0) {
            no = existing;
            break;
        }
    }
    if (no == NULL) {
        g_warning("Attempt to unregister name owner for %s, but none found", well_known_name);
        return;
    }

    list = g_slist_remove(list, no);
    g_hash_table_replace(helper->name_owners, g_strdup(well_known_name), list);    

    set_name_acquired_matched(connection, well_known_name, FALSE);
    set_name_lost_matched(connection, well_known_name, FALSE);

    if (no->state != NAME_OWNERSHIP_NOT_OWNED) {
        (* no->owner->not_owned_handler) (connection, no->data);
    }
    
    name_owner_free(no);
}

static DBusHandlerResult
hippo_dbus_helper_filter_message(DBusConnection *connection,
                                 DBusMessage    *message,
                                 void           *data)
{
    if (dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameOwnerChanged") &&
        dbus_message_has_sender(message, DBUS_SERVICE_DBUS)) {
        const char *name = NULL;
        const char *old = NULL;
        const char *new = NULL;
        if (dbus_message_get_args(message, NULL,
                                  DBUS_TYPE_STRING, &name,
                                  DBUS_TYPE_STRING, &old,
                                  DBUS_TYPE_STRING, &new,
                                  DBUS_TYPE_INVALID)) {
            g_debug("helper.c NameOwnerChanged %s '%s' -> '%s'", name, old, new);
            if (*old == '\0')
                old = NULL;
            if (*new == '\0')
                new = NULL;
            
            handle_name_owner_changed(connection, name, old, new);
        } else {
            g_warning("NameOwnerChanged had wrong args???");
        }
    } else if (dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameLost") &&
               dbus_message_has_sender(message, DBUS_SERVICE_DBUS)) {
        const char *name = NULL;
        if (dbus_message_get_args(message, NULL,
                                  DBUS_TYPE_STRING, &name,
                                  DBUS_TYPE_INVALID)) {
            g_debug("helper.c NameLost %s", name);
            
            handle_name_ownership_maybe_changed(connection, name, FALSE);
        } else {
            g_warning("NameLost had wrong args???");
        }
    } else if (dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameAcquired") &&
               dbus_message_has_sender(message, DBUS_SERVICE_DBUS)) {
        const char *name = NULL;
        if (dbus_message_get_args(message, NULL,
                                  DBUS_TYPE_STRING, &name,
                                  DBUS_TYPE_INVALID)) {
            g_debug("helper.c NameAcquired %s", name);
            
            handle_name_ownership_maybe_changed(connection, name, TRUE);
        } else {
            g_warning("NameAcquired had wrong args???");
        }
    }

    if (dbus_message_get_type(message) == DBUS_MESSAGE_TYPE_SIGNAL) {
        HippoDBusHelper *helper = get_helper(connection); 
        const char *sender = dbus_message_get_sender(message);
        HippoDBusService *service = g_hash_table_lookup(helper->services_by_owner, sender);

        if (service != NULL) {
            int i;

            for (i = 0; service->signal_handlers[i].interface != NULL; i++) {
                const HippoDBusSignalTracker *tracker = &service->signal_handlers[i];
                if (dbus_message_is_signal(message, tracker->interface, tracker->signal)) {
                    tracker->handler(connection, message, service->data);
                }
            }
        }

        if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {

            /* FIXME we need to pretend we got NameLost on all owned bus names */
            
            while (helper->connection_trackers != NULL) {
                HippoDBusConnection *closure;

                closure = helper->connection_trackers->data;
                helper->connection_trackers = g_slist_remove(helper->connection_trackers, closure);
                
                (* closure->tracker->disconnected_handler) (closure->connection, closure->data);
                dbus_connection_unref(closure->connection);
                closure->connection = NULL;
            }

            /* If we don't exit (since someone told libdbus not to), then we'll try connecting
             * again in a while
             */
            ensure_session_connect_attempt();
        }
    }
    
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

struct HippoDBusProxy {
    int                refcount;
    DBusConnection    *connection;
    char              *bus_name;
    char              *path;
    char              *interface;
    char              *method_prefix;
};

HippoDBusProxy*
hippo_dbus_proxy_new(DBusConnection          *connection,
                     const char              *bus_name,
                     const char              *path,
                     const char              *interface)
{
    HippoDBusProxy *proxy;

    proxy = g_new0(HippoDBusProxy, 1);

    proxy->refcount = 1;
    proxy->connection = connection;
    dbus_connection_ref(proxy->connection);
    proxy->bus_name = g_strdup(bus_name);
    proxy->path = g_strdup(path);
    proxy->interface = g_strdup(interface);

    return proxy;
}

void
hippo_dbus_proxy_set_method_prefix(HippoDBusProxy *proxy,
                                   const char     *method_prefix)
{
    if (proxy->method_prefix == method_prefix)
        return;

    if (proxy->method_prefix)
        g_free(proxy->method_prefix);

    proxy->method_prefix = g_strdup(method_prefix);
}

void
hippo_dbus_proxy_unref(HippoDBusProxy          *proxy)
{
    proxy->refcount -= 1;
    if (proxy->refcount == 0) {
        dbus_connection_unref(proxy->connection);
        g_free(proxy->bus_name);
        g_free(proxy->path);
        g_free(proxy->interface);
        g_free(proxy->method_prefix);
        g_free(proxy);
    }
}


static DBusMessage*
call_method_sync_valist_appender (HippoDBusProxy          *proxy,
                                  const char              *method,
                                  DBusError               *error,
                                  HippoDBusArgAppender     appender,
                                  void                    *appender_data,
                                  int                      first_arg_type,
                                  va_list                  args)
{
    DBusMessage *call;
    DBusMessage *reply;
    char *prefixed_method;

    prefixed_method = NULL;
    reply = NULL;

    if (proxy->method_prefix) {
        prefixed_method = g_strconcat(proxy->method_prefix, method, NULL);
        method = prefixed_method;
    }

    call = dbus_message_new_method_call(proxy->bus_name, proxy->path, proxy->interface, method);

    if (proxy->method_prefix)
        g_free(prefixed_method);

    if (first_arg_type != DBUS_TYPE_INVALID) {
        if (!dbus_message_append_args_valist(call, first_arg_type, args)) {
            dbus_set_error_const(error, DBUS_ERROR_NO_MEMORY, "No memory");
            goto failed;
        }
    }

    if (appender != NULL) {
        if (!(* appender)(call, appender_data)) {
            dbus_set_error_const(error, DBUS_ERROR_NO_MEMORY, "No memory");
            goto failed;
        }
    }
    
    reply = dbus_connection_send_with_reply_and_block(proxy->connection, call, -1, error);
    
    dbus_message_unref(call);
    
    return reply;
    
 failed:
    if (call)
        dbus_message_unref(call);
    if (reply)
        dbus_message_unref(reply);
    
    return NULL;
}

DBusMessage*
hippo_dbus_proxy_call_method_sync_valist (HippoDBusProxy          *proxy,
                                          const char              *method,
                                          DBusError               *error,
                                          int                      first_arg_type,
                                          va_list                  args)
{
    return call_method_sync_valist_appender(proxy, method, error,
                                            NULL, NULL, first_arg_type, args);
}

DBusMessage*
hippo_dbus_proxy_call_method_sync_appender  (HippoDBusProxy        *proxy,
                                             const char            *method,
                                             DBusError             *error,
                                             HippoDBusArgAppender   appender,
                                             void                  *appender_data)
{
    va_list dummy_valist;
    
    return call_method_sync_valist_appender(proxy, method, error,
                                            appender, appender_data, DBUS_TYPE_INVALID,
                                            dummy_valist);
}

DBusMessage*
hippo_dbus_proxy_call_method_sync(HippoDBusProxy          *proxy,
                                  const char              *method,
                                  DBusError               *error,
                                  int                      first_arg_type,
                                  ...)
{
    va_list args;
    DBusMessage *reply;
    
    va_start(args, first_arg_type);
    reply = hippo_dbus_proxy_call_method_sync_valist(proxy, method, error,
                                                     first_arg_type, args);
    va_end(args);

    return reply;
}

void
hippo_dbus_proxy_call_method_async (HippoDBusProxy          *proxy,
                                    const char              *method,
                                    HippoDBusReplyHandler    handler,
                                    void                    *data,
                                    DBusFreeFunction         free_data_func,
                                    int                      first_arg_type,
                                    ...)
{
    va_list args;
    
    va_start(args, first_arg_type);
    hippo_dbus_proxy_call_method_async_valist(proxy, method,
                                              handler, data, free_data_func,
                                              first_arg_type, args);
    va_end(args);
}

typedef struct {
    HippoDBusReplyHandler    handler;
    void                    *data;
    DBusFreeFunction         free_data_func;
} AsyncReplyClosure;

static AsyncReplyClosure*
async_reply_closure_new(HippoDBusReplyHandler    handler,
                        void                    *data,
                        DBusFreeFunction         free_data_func)
{
    AsyncReplyClosure *closure;
    closure = g_new(AsyncReplyClosure, 1);
    closure->handler = handler;
    closure->data = data;
    closure->free_data_func = free_data_func;

    return closure;
}

static void
async_reply_closure_free(AsyncReplyClosure *closure)
{
    if (closure->free_data_func)
        (* closure->free_data_func) (closure->data);
    g_free(closure);
}

static void
async_reply_closure_free_func(void *data)
{
    async_reply_closure_free(data);
}

static void
async_reply_closure_invoke(AsyncReplyClosure *closure,
                           DBusMessage       *reply)
{
    (* closure->handler) (reply, closure->data);
}

static void
on_async_reply(DBusPendingCall *pending,
               void            *data)
{
    AsyncReplyClosure *closure;
    DBusMessage *reply;
    
    closure = data;
    
    reply = dbus_pending_call_steal_reply(pending);
    if (reply == NULL) {
        g_warning("NULL reply in on_async_reply?");
        return;
    }

    async_reply_closure_invoke(closure, reply);
    
    dbus_message_unref(reply);

    /* free data func should free the closure */
}

static void
call_method_async_valist_appender(HippoDBusProxy          *proxy,
                                  const char              *method,
                                  HippoDBusReplyHandler    handler,
                                  void                    *data,
                                  DBusFreeFunction         free_data_func,
                                  HippoDBusArgAppender     appender,
                                  void                    *appender_data,
                                  int                      first_arg_type,
                                  va_list                  args)
{
    DBusMessage *call;
    char *prefixed_method;

    prefixed_method = NULL;

    if (proxy->method_prefix) {
        prefixed_method = g_strconcat(proxy->method_prefix, method, NULL);
        method = prefixed_method;
    }

    call = dbus_message_new_method_call(proxy->bus_name, proxy->path, proxy->interface, method);

    if (proxy->method_prefix)
        g_free(prefixed_method);

    if (first_arg_type != DBUS_TYPE_INVALID) {
        if (!dbus_message_append_args_valist(call, first_arg_type, args)) {
            g_warning("No memory to append args to async call");
            goto failed;
        }
    }

    if (appender != NULL) {
        if (!(* appender)(call, appender_data)) {
            g_warning("No memory to append args to async call");
            goto failed;
        }
    }
    
    if (handler != NULL) {
        DBusPendingCall *pending;
        
        pending = NULL;
        dbus_connection_send_with_reply(proxy->connection, call, &pending, -1);

        if (pending == NULL) {
            g_warning("Failed to send method call %s (probably connection is disconnected)", method);
            goto failed;
        }
        
        if (!dbus_pending_call_set_notify(pending, on_async_reply,
                                          async_reply_closure_new(handler, data, free_data_func),
                                          async_reply_closure_free_func))
            g_error("out of memory");
        
        
        /* rely on connection to hold a reference to it, if finalized
         * I think on_async_reply won't get called though, 
         * which is fine currently
         */
        dbus_pending_call_unref(pending);
    } else {
        dbus_message_set_no_reply(call, TRUE);
        dbus_connection_send(proxy->connection, call, NULL);
    }
    
    dbus_message_unref(call);
    
    return;
    
 failed:
    if (call)
        dbus_message_unref(call);

    return;
}

void
hippo_dbus_proxy_call_method_async_valist(HippoDBusProxy          *proxy,
                                          const char              *method,
                                          HippoDBusReplyHandler    handler,
                                          void                    *data,
                                          DBusFreeFunction         free_data_func,
                                          int                      first_arg_type,
                                          va_list                  args)
{
    call_method_async_valist_appender(proxy, method, handler, data, free_data_func,
                                      NULL, NULL, first_arg_type, args);
}

void
hippo_dbus_proxy_call_method_async_appender (HippoDBusProxy        *proxy,
                                             const char            *method,
                                             HippoDBusReplyHandler  handler,
                                             void                  *data,
                                             DBusFreeFunction       free_data_func,
                                             HippoDBusArgAppender   appender,
                                             void                  *appender_data)
{
    va_list dummy_args;
    
    call_method_async_valist_appender(proxy, method, handler, data, free_data_func,
                                      appender, appender_data, DBUS_TYPE_INVALID, dummy_args);
}


/* this is sort of bizarre in that it warns about then frees the passed-in error,
 * rather than returning it.
 */
dbus_bool_t
hippo_dbus_proxy_finish_method_call_freeing_reply (DBusMessage *reply,
                                                   const char  *method,
                                                   DBusError   *error,
                                                   int          first_arg_type,
                                                   ...)
{
    va_list args;

    if (reply == NULL) {
        g_warning("No reply to %s: %s", method, error->message);
        dbus_error_free(error);
        return FALSE;
    }

    if (dbus_set_error_from_message(error, reply)) {
        g_warning("Error from %s: %s: %s", method, error->name, error->message);
        dbus_error_free(error);
        return FALSE;
    }
    
    va_start(args, first_arg_type);
    if (dbus_message_get_args_valist(reply, error, first_arg_type, args)) {
        va_end(args);
        dbus_message_unref(reply);
        return TRUE;
    } else {
        va_end(args);
        dbus_message_unref(reply);

        g_warning("Error getting method call %s reply: %s", method, error->message);
        dbus_error_free(error);
        
        return FALSE;
    }
}

/* this is sort of bizarre in that it warns about then frees the passed-in error,
 * rather than returning it.
 */
dbus_bool_t
hippo_dbus_proxy_finish_method_call_keeping_reply (DBusMessage *reply,
                                                   const char  *method,
                                                   DBusError   *error,
                                                   int          first_arg_type,
                                                   ...)
{
    va_list args;

    if (reply == NULL) {
        g_warning("No reply to %s: %s", method, error->message);
        dbus_error_free(error);
        return FALSE;
    }

    if (dbus_set_error_from_message(error, reply)) {
        g_warning("Error from %s: %s: %s", method, error->name, error->message);
        dbus_error_free(error);
        return FALSE;
    }
    
    va_start(args, first_arg_type);
    if (dbus_message_get_args_valist(reply, error, first_arg_type, args)) {
        va_end(args);
        return TRUE;
    } else {
        va_end(args);

        g_warning("Error getting method call %s reply: %s", method, error->message);
        dbus_error_free(error);
        
        return FALSE;
    }
}

dbus_bool_t
hippo_dbus_proxy_VOID__VOID(HippoDBusProxy *proxy,
                            const char     *method)
{
    DBusMessage *reply;
    DBusError derror;
    
    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INVALID);
    
    return hippo_dbus_proxy_finish_method_call_freeing_reply(reply, method, &derror,
                                                             DBUS_TYPE_INVALID);
}

dbus_bool_t
hippo_dbus_proxy_INT32__VOID(HippoDBusProxy *proxy,
                             const char     *method,
                             dbus_int32_t   *out1_p)
{
    DBusMessage *reply;
    DBusError derror;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INVALID);
    
    return hippo_dbus_proxy_finish_method_call_freeing_reply(reply, method, &derror,
                                                             DBUS_TYPE_INT32, out1_p,
                                                             DBUS_TYPE_INVALID);
}

dbus_bool_t
hippo_dbus_proxy_INT32__INT32(HippoDBusProxy *proxy,
                              const char     *method,
                              dbus_int32_t    in1_p,
                              dbus_int32_t   *out1_p)
{
    DBusMessage *reply;
    DBusError derror;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INT32, &in1_p,
                                              DBUS_TYPE_INVALID);
    
    return hippo_dbus_proxy_finish_method_call_freeing_reply(reply, method, &derror,
                                                             DBUS_TYPE_INT32, out1_p,
                                                             DBUS_TYPE_INVALID);
}

dbus_bool_t
hippo_dbus_proxy_ARRAYINT32__INT32(HippoDBusProxy *proxy,
                                   const char     *method,
                                   dbus_int32_t    in1,
                                   dbus_int32_t  **out1_p,
                                   dbus_int32_t   *out1_len)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const dbus_int32_t *v_OUT1;
    dbus_int32_t len_OUT1;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INT32, &in1,
                                              DBUS_TYPE_INVALID);

    v_OUT1 = NULL;
    len_OUT1 = 0;
    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, method, &derror,
                                                               DBUS_TYPE_ARRAY, DBUS_TYPE_INT32,
                                                               &v_OUT1, &len_OUT1,
                                                               DBUS_TYPE_INVALID);
    if (v_OUT1 != NULL) {
        *out1_p = g_new(dbus_int32_t, len_OUT1);
        memcpy(*out1_p, v_OUT1, sizeof(dbus_int32_t) * len_OUT1);
    } else {
        *out1_p = NULL;
    }
    *out1_len = len_OUT1;

    if (reply != NULL)
        dbus_message_unref(reply);

    return retval;
}

dbus_bool_t
hippo_dbus_proxy_ARRAYINT32__VOID(HippoDBusProxy *proxy,
                                  const char     *method,
                                  dbus_int32_t  **out1_p,
                                  dbus_int32_t   *out1_len)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const dbus_int32_t *v_OUT1;
    dbus_int32_t len_OUT1;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INVALID);

    v_OUT1 = NULL;
    len_OUT1 = 0;
    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, method, &derror,
                                                               DBUS_TYPE_ARRAY, DBUS_TYPE_INT32,
                                                               &v_OUT1, &len_OUT1,
                                                               DBUS_TYPE_INVALID);
    if (v_OUT1 != NULL) {
        *out1_p = g_new(dbus_int32_t, len_OUT1);
        memcpy(*out1_p, v_OUT1, sizeof(dbus_int32_t) * len_OUT1);
    } else {
        *out1_p = NULL;
    }
    *out1_len = len_OUT1;

    if (reply != NULL)
        dbus_message_unref(reply);

    return retval;
}

dbus_bool_t
hippo_dbus_proxy_ARRAYINT32__INT32_STRING(HippoDBusProxy *proxy,
                                          const char     *method,
                                          dbus_int32_t    in1,
                                          const char     *in2,
                                          dbus_int32_t  **out1_p,
                                          dbus_int32_t   *out1_len)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const dbus_int32_t *v_OUT1;
    dbus_int32_t len_OUT1;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INT32, &in1,
                                              DBUS_TYPE_STRING, &in2,
                                              DBUS_TYPE_INVALID);

    v_OUT1 = NULL;
    len_OUT1 = 0;
    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, method, &derror,
                                                               DBUS_TYPE_ARRAY, DBUS_TYPE_INT32,
                                                               &v_OUT1, &len_OUT1,
                                                               DBUS_TYPE_INVALID);
    if (v_OUT1 != NULL) {
        *out1_p = g_new(dbus_int32_t, len_OUT1);
        memcpy(*out1_p, v_OUT1, sizeof(dbus_int32_t) * len_OUT1);
    } else {
        *out1_p = NULL;
    }
    *out1_len = len_OUT1;

    if (reply != NULL)
        dbus_message_unref(reply);

    return retval;
}

dbus_bool_t
hippo_dbus_proxy_STRING__INT32(HippoDBusProxy *proxy,
                               const char     *method,
                               dbus_int32_t    in1_p,
                               char          **out1_p)
{
    DBusMessage *reply;
    DBusError derror;
    dbus_bool_t retval;
    const char *v_OUT1;

    dbus_error_init(&derror);    
    reply = hippo_dbus_proxy_call_method_sync(proxy, method, &derror,
                                              DBUS_TYPE_INT32, &in1_p,
                                              DBUS_TYPE_INVALID);

    v_OUT1 = NULL;
    retval = hippo_dbus_proxy_finish_method_call_keeping_reply(reply, method, &derror,
                                                               DBUS_TYPE_STRING,
                                                               &v_OUT1,
                                                               DBUS_TYPE_INVALID);

    *out1_p = g_strdup(v_OUT1);

    if (reply != NULL)
        dbus_message_unref(reply);

    return retval;
}
