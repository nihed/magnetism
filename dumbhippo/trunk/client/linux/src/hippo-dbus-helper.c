/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <glib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include "hippo-dbus-helper.h"

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
    GHashTable *interfaces;
    GHashTable *objects;
} HippoDBusHelper;

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

static void
iface_free(HippoDBusInterface *iface)
{
    g_free(iface->name);
    g_free(iface);
}

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

    /* FIXME free the hash contents */

    g_hash_table_destroy(helper->interfaces);
    g_hash_table_destroy(helper->objects);
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
        helper->objects = g_hash_table_new(g_str_hash, g_str_equal);

        dbus_connection_set_data(connection, slot, helper, helper_free);
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

    iface = iface_new(name, members, properties);

    helper = get_helper(connection);

    g_return_if_fail(g_hash_table_lookup(helper->interfaces, iface->name) == NULL);

    g_hash_table_replace(helper->interfaces, iface->name, iface);
}

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

    g_return_if_fail(g_hash_table_lookup(helper->objects, o->path) == NULL);

    g_hash_table_replace(helper->objects, o->path, o);
}

void
hippo_dbus_helper_unregister_object(DBusConnection    *connection,
                                    const char        *path)
{
    HippoDBusObject *o;
    HippoDBusHelper *helper;

    helper = get_helper(connection);

    o = g_hash_table_lookup(helper->objects, path);
    g_return_if_fail(o != NULL);

    g_hash_table_remove(helper->objects, path);

    object_free(o);
}

gboolean
hippo_dbus_helper_object_is_registered (DBusConnection          *connection,
                                        const char              *path)
{
    HippoDBusObject *o;
    HippoDBusHelper *helper;

    helper = get_helper(connection);

    o = g_hash_table_lookup(helper->objects, path);

    return o != NULL;
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
        dbus_message_iter_init_append(message, &iter);
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

    if (reply != NULL) {
        dbus_connection_send(connection, reply, NULL);
        dbus_message_unref(reply);
        return DBUS_HANDLER_RESULT_HANDLED;
    } else {
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    }
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
                               "    <property name=\"%s\" type=\"%s\" access=\"%s\">\n",
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

    if (!(iface == NULL || strcmp(iface, DBUS_INTERFACE_INTROSPECTABLE) == 0))
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    reply = NULL;
    dbus_error_init(&derror);

    xml = g_string_new(NULL);

    g_string_append(xml, DBUS_INTROSPECT_1_0_XML_DOCTYPE_DECL_NODE);

    g_string_append       (xml,
                           "<node>\n");

    g_string_append_printf(xml,
                           "  <interface name=\"%s\">\n", DBUS_INTERFACE_INTROSPECTABLE);

    g_string_append       (xml,
                           "    <method name=\"Introspect\">\n");

    g_string_append_printf(xml,
                           "      <arg name=\"data\" direction=\"out\" type=\"%s\"/>\n",
                           DBUS_TYPE_STRING_AS_STRING);

    g_string_append       (xml,
                           "    </method>\n");

    g_string_append       (xml,
                           "  </interface>\n");

    for (i = 0; o->interfaces[i] != NULL; ++i) {
        append_interface(xml, o->interfaces[i]);
    }

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

DBusHandlerResult
hippo_dbus_helper_handle_message(DBusConnection *connection,
                                 DBusMessage    *message)
{
    HippoDBusObject *o;
    HippoDBusHelper *helper;
    const char *path;
    const char *member;
    const char *iface;

    if (dbus_message_get_type(message) != DBUS_MESSAGE_TYPE_METHOD_CALL)
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    path = dbus_message_get_path(message);

    if (path == NULL)
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    helper = get_helper(connection);

    o = g_hash_table_lookup(helper->objects, path);

    if (o == NULL)
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
