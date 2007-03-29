/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-server.h"
#include "hippo-dbus-web.h"
#include "hippo-dbus-mugshot.h"
#include "main.h"

static void
append_strings_as_dict(DBusMessageIter *iter,
                       ...)
{
    const char *name;
    va_list args;
    DBusMessageIter subiter;

    va_start(args, iter);

    dbus_message_iter_open_container(iter,
                                     DBUS_TYPE_ARRAY,
                                     DBUS_DICT_ENTRY_BEGIN_CHAR_AS_STRING
                                     DBUS_TYPE_STRING_AS_STRING
                                     DBUS_TYPE_STRING_AS_STRING
                                     DBUS_DICT_ENTRY_END_CHAR_AS_STRING,
                                     &subiter);



    while ((name = va_arg(args, const char*)) != NULL) {
        DBusMessageIter subsubiter;
        const char *value = va_arg(args, const char*);
        if (value) { /* skip pairs with a null value */
            dbus_message_iter_open_container(&subiter, DBUS_TYPE_DICT_ENTRY, NULL, &subsubiter);
            dbus_message_iter_append_basic(&subsubiter, DBUS_TYPE_STRING, &name);
            dbus_message_iter_append_basic(&subsubiter, DBUS_TYPE_STRING, &value);
            dbus_message_iter_close_container(&subiter, &subsubiter);
        }
    }
    dbus_message_iter_close_container(iter, &subiter);
}

static void
append_entity(HippoDBus         *dbus,
              DBusMessage       *message,
              HippoEntity       *entity)
{
    DBusMessageIter iter;
    HippoConnection *connection;
    const char *guid;
    const char *name;
    const char *home_url;
    const char *photo_url;
    const char *type;

    connection = hippo_data_cache_get_connection(hippo_app_get_data_cache(hippo_get_app()));

    guid = hippo_entity_get_guid(entity);
    name = hippo_entity_get_name(entity);
    home_url = hippo_entity_get_home_url(entity);
    photo_url = hippo_entity_get_photo_url(entity);

    type = NULL;
    switch (hippo_entity_get_entity_type(entity)) {
    case HIPPO_ENTITY_PERSON:
        type = "person";
        break;
    case HIPPO_ENTITY_GROUP:
        type = "group";
        break;
    case HIPPO_ENTITY_RESOURCE:
        type = "resource";
        break;
    case HIPPO_ENTITY_FEED:
        type = "feed";
        break;
    }

    /* append_strings_as_dict will skip pairs with null value */
    dbus_message_iter_init_append(message, &iter);
    append_strings_as_dict(&iter, "guid", guid, "type", type, "name", name, "home-url", home_url, "photo-url", photo_url, NULL);
}

static HippoEntity *
entity_from_ref(HippoDBus *dbus,
                const char *ref)
{
    HippoDataCache *cache;
     
    cache = hippo_app_get_data_cache(hippo_get_app());
    
    if (!g_str_has_prefix(ref, HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX))
        return NULL;
    
    return hippo_data_cache_lookup_entity(cache, ref + strlen(HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX));
}
                
static char *
get_entity_path(HippoEntity *entity)
{
	return g_strdup_printf(HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX "%s", hippo_entity_get_guid(entity));  
}

static void
append_entity_ref(HippoDBus *dbus,
                  DBusMessage *message,
                  HippoEntity *entity)
{
	char *opath;
	opath = get_entity_path(entity);
    dbus_message_append_args(message, DBUS_TYPE_OBJECT_PATH, &opath, DBUS_TYPE_INVALID);
}

void
hippo_dbus_try_acquire_mugshot(DBusConnection *connection,
                               gboolean        replace)
{
    dbus_uint32_t flags;

    /* We do want to be queued if we don't get this right away */
    flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;

    /* we just ignore errors on this */
    dbus_bus_request_name(connection, HIPPO_DBUS_MUGSHOT_BUS_NAME,
                          flags,
                          NULL);
}

DBusMessage* 
hippo_dbus_handle_mugshot_get_baseprops(HippoDBus       *dbus,
                                        DBusMessage     *message)
{
    DBusMessageIter iter;
    DBusMessage *reply;
    HippoDataCache *cache;
    HippoConnection *connection;
    char *baseurl;

    cache = hippo_app_get_data_cache(hippo_get_app());
    connection = hippo_data_cache_get_connection(cache);
    
    baseurl = hippo_connection_make_absolute_url(connection, "/");
    
    reply = dbus_message_new_method_return(message);   
    dbus_message_iter_init_append(reply, &iter);
     
    append_strings_as_dict(&iter, 
                           "baseurl", baseurl,
                           NULL);
    g_free(baseurl);
    
    return reply;    
}

DBusMessage*
hippo_dbus_handle_mugshot_get_self(HippoDBus   *dbus,
                                   DBusMessage  *message)
{
    HippoPerson *self;
    DBusMessage *reply;
    HippoDataCache *cache;

    cache = hippo_app_get_data_cache(hippo_get_app());

    self = hippo_data_cache_get_self(cache);
    if (self == NULL) {
    	return dbus_message_new_error(message, "org.mugshot.Mugshot.Disconnected", "Not connected");
    }

    reply = dbus_message_new_method_return(message);
    append_entity_ref(dbus, reply, HIPPO_ENTITY(self));					
    return reply;
}

DBusMessage*
hippo_dbus_handle_mugshot_get_connection_status(HippoDBus   *dbus,
                                                DBusMessage  *message)
{
    DBusMessage *reply;	
	gboolean have_auth;
	gboolean connected;
	gboolean contacts_loaded;
    HippoDataCache *cache;
	HippoConnection *connection;
	
    cache = hippo_app_get_data_cache(hippo_get_app());
    connection = hippo_data_cache_get_connection(cache);
    
    have_auth = hippo_connection_get_has_auth(connection);
	connected = hippo_connection_get_connected(connection);
	contacts_loaded = hippo_connection_get_contacts_loaded(connection);
    
    reply = dbus_message_new_method_return(message);    
    dbus_message_append_args(reply, DBUS_TYPE_BOOLEAN, &have_auth, 
                                    DBUS_TYPE_BOOLEAN, &connected,
                                    DBUS_TYPE_BOOLEAN, &contacts_loaded, 
                                    DBUS_TYPE_INVALID);
    
    return reply;
}

DBusMessage*
hippo_dbus_handle_mugshot_entity_message(HippoDBus   *dbus,
                                         DBusMessage  *message)
{
	DBusError error;
    HippoEntity *entity;
    DBusMessage *reply = NULL;

    dbus_error_init(&error);
    
    entity = entity_from_ref(dbus, dbus_message_get_path(message));
    
    if (!entity) {
    	return dbus_message_new_error(message, "org.mugshot.Mugshot.UnknownEntity", "Unknown entity");    	
    }
    
    if (!strcmp(dbus_message_get_member(message), "GetProperties")) {
	    reply = dbus_message_new_method_return(message);
        append_entity(dbus, reply, HIPPO_ENTITY(entity));	
    }
    	
    return reply;
}

DBusMessage*
hippo_dbus_handle_mugshot_get_whereim(HippoDBus   *dbus,
                                      HippoConnection *connection,
                                      DBusMessage *message)
{
    hippo_connection_request_mugshot_whereim(connection);

    return dbus_message_new_method_return(message);  /* Send out the results as signals.  */
}

DBusMessage*
hippo_dbus_handle_mugshot_send_external_iq(HippoDBus   *dbus,
                                           DBusMessage *message)
{
    DBusMessage *reply;
    gboolean is_set;
    const char *element;
    char **attrs;
    int attrs_count;
    const char *content;
    DBusError error;
    HippoConnection *connection;
    guint request_id;
    
    dbus_error_init(&error);
    
    if (!dbus_message_get_args(message,
                               &error, 
                               DBUS_TYPE_BOOLEAN, &is_set,
                               DBUS_TYPE_STRING, &element,
                               DBUS_TYPE_ARRAY, DBUS_TYPE_STRING, &attrs, &attrs_count,
                               DBUS_TYPE_STRING, &content,
                               DBUS_TYPE_INVALID)) {
        reply = dbus_message_new_error(message, error.name, error.message);
        dbus_error_free(&error);
        return reply;
    }
    
    if (attrs_count % 2 != 0) {
    	return dbus_message_new_error(message, "org.mugshot.Mugshot.InvalidAttributes", "Invalid attribute count");	
    }
    
    connection = hippo_data_cache_get_connection(hippo_app_get_data_cache(hippo_get_app()));
    
    request_id = hippo_connection_send_external_iq(connection, is_set, element, attrs_count, attrs, content);
    
    dbus_free_string_array(attrs);    
    
    reply = dbus_message_new_method_return(message);
    dbus_message_append_args(reply, DBUS_TYPE_UINT32, &request_id, NULL);
    return reply;
}

static void
append_network_ref(gpointer entity_ptr, gpointer data)
{
    HippoEntity *entity = (HippoEntity*) entity_ptr; 	
    DBusMessageIter *iter = (DBusMessageIter*) data; 

    if (hippo_entity_get_in_network(entity)) {
    	char *ref;
    	ref = get_entity_path(entity);
        dbus_message_iter_append_basic(iter, DBUS_TYPE_OBJECT_PATH, &ref);
        g_free(ref);
    }   
}

DBusMessage*
hippo_dbus_handle_mugshot_get_network(HippoDBus   *dbus,
                                      DBusMessage  *message)
{
	DBusMessage *reply;
	DBusMessageIter iter, array_iter;
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());

	reply = dbus_message_new_method_return(message);
	dbus_message_iter_init_append(reply, &iter);
	dbus_message_iter_open_container(&iter, 
	                                 DBUS_TYPE_ARRAY,
	                                 DBUS_TYPE_OBJECT_PATH_AS_STRING,
	                                 &array_iter);

    hippo_data_cache_foreach_entity(cache, append_network_ref, &array_iter);
    
    dbus_message_iter_close_container(&iter, &array_iter);

    return reply;
}

DBusMessage*
hippo_dbus_handle_mugshot_introspect(HippoDBus   *dbus,
                                     DBusMessage *message)
{
    GString *xml;
    DBusMessage *reply;

    xml = g_string_new(NULL);

    g_string_append(xml, DBUS_INTROSPECT_1_0_XML_DOCTYPE_DECL_NODE);

    g_string_append(xml, "<node>\n");

    g_string_append_printf(xml, "  <interface name=\"%s\">\n", DBUS_INTERFACE_INTROSPECTABLE);

    g_string_append(xml, "    <method name=\"Introspect\">\n");

    g_string_append_printf(xml, "      <arg name=\"data\" direction=\"out\" type=\"%s\"/>\n", DBUS_TYPE_STRING_AS_STRING);

    g_string_append(xml, "    </method>\n");

    g_string_append(xml, "  </interface>\n");

    g_string_append_printf(xml, "  <interface name=\"%s\">\n",
                           HIPPO_DBUS_MUGSHOT_INTERFACE);
    g_string_append(xml,
                    "    <method name=\"GetBaseProperties\">\n"
                    "      <arg direction=\"out\" type=\"a{ss}\"/>\n"
                    "    </method>"
                    );
    g_string_append(xml,
                    "    <method name=\"SendExternalIQ\">\n"
                    "      <arg direction=\"in\" type=\"b\"/>\n"
                    "      <arg direction=\"in\" type=\"s\"/>\n"
                    "      <arg direction=\"in\" type=\"as\"/>\n"                    
                    "      <arg direction=\"in\" type=\"s\"/>\n"
                    "      <arg direction=\"out\" type=\"u\"/>\n"                    
                    "    </method>"
                    );                    
    g_string_append(xml,
                    "    <method name=\"NotifyAllWhereim\"/>\n");
    g_string_append(xml,
                    "    <method name=\"GetNetwork\">\n"
                    "      <arg direction=\"out\" type=\"ao\"/>\n"
                    "    </method>"
                    );

    g_string_append(xml,
                    "    <signal name=\"WhereimChanged\">\n"
                    "      <arg direction=\"in\" type=\"a{ss}\"/>\n"
                    "    </signal>\n");

    g_string_append(xml,
                    "    <signal name=\"EntityChanged\">\n"
                    "      <arg direction=\"in\" type=\"a{ss}\"/>\n"
                    "    </signal>\n");

    g_string_append(xml, "  </interface>\n");

    g_string_append(xml, "</node>\n");


    reply = dbus_message_new_method_return(message);

    dbus_message_append_args(reply, DBUS_TYPE_STRING, &xml->str, DBUS_TYPE_INVALID);

    g_string_free(xml, TRUE);

    return reply;
}

DBusMessage*
hippo_dbus_mugshot_signal_whereim_changed(HippoDBus            *dbus,
                                          HippoConnection      *connection,
                                          HippoExternalAccount *acct)
{
    DBusMessage *signal;
    DBusMessageIter iter;
    char *name, *sentiment, *icon_url, *link;

    g_object_get(acct, "name", &name, "sentiment", &sentiment, "icon-url", &icon_url, "link", &link, NULL);

    signal = dbus_message_new_signal(HIPPO_DBUS_MUGSHOT_PATH,
                                     HIPPO_DBUS_MUGSHOT_INTERFACE,
                                     "WhereimChanged");
    dbus_message_iter_init_append(signal, &iter);
                                         
	append_strings_as_dict(&iter, 
						   "name", name,
						   "sentiment", sentiment,
						   "icon-url", icon_url,
						   "link", link,
						   NULL);
    g_free(name);
    g_free(sentiment);
    g_free(icon_url);
    g_free(link);
    return signal;
}

DBusMessage*
hippo_dbus_mugshot_signal_connection_changed(HippoDBus            *dbus)
{
    DBusMessage *signal;
    signal = dbus_message_new_signal(HIPPO_DBUS_MUGSHOT_PATH,
                                     HIPPO_DBUS_MUGSHOT_INTERFACE,
                                     "ConnectionStatusChanged");
    return signal;	
}

DBusMessage*
hippo_dbus_mugshot_signal_entity_changed(HippoDBus            *dbus,
                                         HippoEntity          *entity)
{
    DBusMessage *signal;
    char *path;
    path = get_entity_path(entity);
    signal = dbus_message_new_signal(path,
                                     HIPPO_DBUS_MUGSHOT_ENTITY_INTERFACE,
                                     "Changed");
    g_free(path);
    return signal;
}

DBusMessage*
hippo_dbus_mugshot_signal_pref_changed(HippoDBus            *dbus,
                                       const char           *key,
                                       gboolean              value)
{
    DBusMessage *signal;
    signal = dbus_message_new_signal(HIPPO_DBUS_MUGSHOT_PATH,
                                     HIPPO_DBUS_MUGSHOT_INTERFACE,
                                     "PrefChanged");
    dbus_message_append_args(signal, 
                             DBUS_TYPE_STRING, &key,
                             DBUS_TYPE_BOOLEAN, &value, 
                             DBUS_TYPE_INVALID);
    return signal;
}

DBusMessage* 
hippo_dbus_mugshot_signal_external_iq_return(HippoDBus            *dbus,
                                             guint                 id,
                                             const char           *content)
{
    DBusMessage *signal;
    signal = dbus_message_new_signal(HIPPO_DBUS_MUGSHOT_PATH,
                                     HIPPO_DBUS_MUGSHOT_INTERFACE,
                                     "ExternalIQReturn");
    dbus_message_append_args(signal, DBUS_TYPE_UINT32, &id, DBUS_TYPE_STRING, &content, DBUS_TYPE_INVALID);
    return signal;
}                                                   
