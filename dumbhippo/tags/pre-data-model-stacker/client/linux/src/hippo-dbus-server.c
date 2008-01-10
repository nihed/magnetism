/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <stdlib.h>
#include <string.h>
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>
#include "hippo-dbus-server.h"
#include "hippo-dbus-client.h"
#include "hippo-dbus-contacts.h"
#include "hippo-dbus-cookies.h"
#include "hippo-dbus-model.h"
#include "hippo-dbus-mugshot.h"
#include "hippo-dbus-pidgin.h"
#include "hippo-dbus-local.h"
#include "hippo-dbus-settings.h"
#include "hippo-dbus-web.h"
#include "hippo-dbus-http.h"
#include "hippo-distribution.h"
#include <hippo/hippo-endpoint-proxy.h>
#include <hippo/hippo-stack-manager.h>
#include "main.h"

/* rhythmbox messages */
#define RB_SHELL_PATH          "/org/gnome/Rhythmbox/Shell"
#define RB_SHELL_IFACE         "org.gnome.Rhythmbox.Shell"
#define RB_PLAYER_IFACE        "org.gnome.Rhythmbox.Player"
#define RB_BUS_NAME            "org.gnome.Rhythmbox"
#define RB_PLAYING_URI_CHANGED "playingUriChanged"
#define RB_GET_SONG_PROPERTIES "getSongProperties"

/* banshee messages */
#define BANSHEE_MUGSHOT_IFACE  "org.gnome.Banshee.Mugshot"
#define BANSHEE_STATE_CHANGED  "StateChangedEvent"

/* muine messages */
#define MUINE_BUS_NAME      "org.gnome.Muine"
#define MUINE_PLAYER_IFACE  "org.gnome.Muine.Player"
/* #define MUINE_STATE_CHANGED "StateChanged" */
#define MUINE_SONG_CHANGED  "SongChanged"

/* quodlibet messages */
#define QL_PLAYER_IFACE        "net.sacredchao.QuodLibet"
#define QL_SONG_STARTED        "SongStarted"

typedef struct _HippoDBusListener HippoDBusListener;
typedef struct _HippoApplicationRequest HippoApplicationRequest;


static void      hippo_dbus_init                (HippoDBus       *dbus);
static void      hippo_dbus_class_init          (HippoDBusClass  *klass);

static void      hippo_dbus_finalize            (GObject          *object);

static void      hippo_dbus_disconnected        (HippoDBus        *dbus);

static DBusHandlerResult handle_message         (DBusConnection     *connection,
                                                 DBusMessage        *message,
                                                 void               *user_data);

static void disconnect_listener(HippoDBus         *dbus,
				HippoDBusListener *listener);

enum {
    DISCONNECTED,
    SONG_CHANGED,
    LAST_SIGNAL
};

static int signals[LAST_SIGNAL];  

struct _HippoDBusListener {
    HippoDBus *dbus;
    char *name;
    GSList *endpoints;
    GSList *application_requests;
};

struct _HippoDBus {
    GObject parent;
    char           *desktop_bus_name;    
    char           *stacker_bus_name;    
    DBusConnection *connection;
    unsigned int in_dispatch : 1; /* dbus is broken and we can't recurse right now */
    unsigned int xmpp_connected : 1;
    unsigned int emitted_disconnected : 1;
    GSList *listeners;
};

struct _HippoDBusClass {
    GObjectClass parent_class;

};

struct _HippoApplicationRequest {
    HippoDBusListener *listener;
    
    guint64 endpoint;
    char *application_id;
    char *package_names;
    char *desktop_names;
    
    gboolean can_install;
    gboolean can_run;
    char *version;

    gboolean package_response;
    gboolean application_response;
};

G_DEFINE_TYPE(HippoDBus, hippo_dbus, G_TYPE_OBJECT);

static void
hippo_dbus_init(HippoDBus  *dbus)
{

}

static void
hippo_dbus_class_init(HippoDBusClass  *klass)
{
    GObjectClass *object_class = G_OBJECT_CLASS(klass);

    object_class->finalize = hippo_dbus_finalize;
    
    signals[DISCONNECTED] =
        g_signal_new ("disconnected",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__VOID,
                      G_TYPE_NONE, 0);
    
    signals[SONG_CHANGED] =
        g_signal_new ("song-changed",
                      G_TYPE_FROM_CLASS (object_class),
                      G_SIGNAL_RUN_LAST,
                      0,
                      NULL, NULL,
                      g_cclosure_marshal_VOID__POINTER,
                      G_TYPE_NONE, 1, G_TYPE_POINTER);
}

DBusConnection*
hippo_dbus_get_connection(HippoDBus *dbus)
{
    return dbus->connection;
}

static gboolean
propagate_dbus_error(GError **error, DBusError *derror)
{
    if (dbus_error_is_set(derror)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED,
            _("D-BUS error: %s"), derror->message ? derror->message : derror->name);
        dbus_error_free(derror);
        return FALSE;
    } else {
        return TRUE;
    }
}

static gboolean
acquire_bus_name(DBusConnection *connection,
                 const char     *server,
                 gboolean        replace_existing,
                 const char     *bus_name,
                 GError        **error)
{
    DBusError derror;
    unsigned int flags;
    int result;
    
    flags = DBUS_NAME_FLAG_DO_NOT_QUEUE | DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
    if (replace_existing)
        flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
    
    dbus_error_init(&derror);
    result = dbus_bus_request_name(connection, bus_name,
                                   flags,
                                   &derror);
    if (dbus_error_is_set(&derror)) {
        propagate_dbus_error(error, &derror);
        return FALSE;
    }
    
    if (!(result == DBUS_REQUEST_NAME_REPLY_PRIMARY_OWNER ||
          result == DBUS_REQUEST_NAME_REPLY_ALREADY_OWNER)) {
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_ALREADY_RUNNING,
                    _("Another copy of %s is already running in this session for server %s"),
                    g_get_application_name(), server);               
        return FALSE;
    }

    g_debug("Acquired bus name %s", bus_name);
    return TRUE;
}

HippoDBus*
hippo_dbus_try_to_acquire(const char  *desktop_server,
                          const char  *stacker_server,
                          gboolean     replace_existing,
                          GError     **error)
{
    HippoDBus *dbus;
    DBusGConnection *gconnection;
    DBusConnection *connection;
    char *desktop_bus_name;
    char *stacker_bus_name;
    char *old_bus_name;
    DBusError derror;
    
    dbus_error_init(&derror);

    /* dbus_bus_get is a little hosed since you can't unref 
     * unless you know it's disconnected. I guess it turns
     * out we more or less want to do that anyway.
     */
    
    gconnection = dbus_g_bus_get(DBUS_BUS_SESSION, error);
    if (gconnection == NULL)
        return NULL;
    
    connection = dbus_g_connection_get_connection(gconnection);
    
    /* the purpose of this check is to be sure we will get a "Disconnected"
     * message in the future
     */
    if (!dbus_connection_get_is_connected(connection)) {
        dbus_connection_unref(connection);
        g_set_error(error, HIPPO_ERROR, HIPPO_ERROR_FAILED, 
            _("No active connection to the session's message bus"));
        return NULL;
    }

    stacker_bus_name = NULL;
    desktop_bus_name = NULL;


    /* FIXME we should only get the Engine name here with the desktop server, not
     * the stacker... that requires a little BigBoard fixage.
     */
    
    stacker_bus_name = hippo_dbus_full_bus_name(stacker_server);
    if (!acquire_bus_name(connection, stacker_server, replace_existing, stacker_bus_name, error)) {
        g_free(stacker_bus_name);
        /* FIXME leak bus connection since unref isn't allowed */
        return NULL;
    }

    desktop_bus_name = hippo_dbus_full_bus_name(desktop_server);
    if (!acquire_bus_name(connection, desktop_server, replace_existing, desktop_bus_name, error)) {
        g_free(stacker_bus_name);
        g_free(desktop_bus_name);
        /* FIXME leak bus connection since unref isn't allowed */
        return NULL;
    }
    
    /* The com.dumbhippo.Client name may be appropriate to keep forever, as the
     * canonical name of the Stacker-specific stuff, while desktop generic
     * stuff is accessed via the org.freedesktop.od.Engine name.
     *
     * So this is done only for the stacker server name.
     */
    old_bus_name = hippo_dbus_full_bus_name_com_dumbhippo_with_forward_hex(stacker_server);
    if (!acquire_bus_name(connection, stacker_server, replace_existing, old_bus_name, error)) {
        /* FIXME leak bus connection since unref isn't allowed */

        /* We need to give up the new bus name because we call ShowBrowser on
         * it in main.c if we fail to get both names, which deadlocks if
         * we own the new name
         */
        dbus_bus_release_name(connection, stacker_bus_name, NULL);
        dbus_bus_release_name(connection, desktop_bus_name, NULL);
        
        g_free(old_bus_name);
        g_free(stacker_bus_name);
        g_free(desktop_bus_name);
        return NULL;
    }

    g_free(old_bus_name);

    /* We also acquire our old broken bus name for compatibility with old versions
     * of client apps, and so that when this version is run with --replace
     * old versions will exit. We *don't* exit if we lose ownership of this
     * name ourself, since we running the older version with --replace to replace
     * the new version isn't very interesting
     *
     * Since this is compat gunk, it only uses the stacker server.
     */
    
    old_bus_name = hippo_dbus_full_bus_name_com_dumbhippo_with_backward_hex(stacker_server);
    if (!acquire_bus_name(connection, stacker_server, replace_existing, old_bus_name, error)) {
        /* FIXME leak bus connection since unref isn't allowed */

        /* We need to give up the new bus name because we call ShowBrowser on
         * it in main.c if we fail to get both names, which deadlocks if
         * we own the new name
         */
        dbus_bus_release_name(connection, stacker_bus_name, NULL);
        dbus_bus_release_name(connection, desktop_bus_name, NULL);
        
        g_free(old_bus_name);

        g_free(stacker_bus_name);
        g_free(desktop_bus_name);
        return NULL;
    }

    g_free(old_bus_name);

    /* Now we acquire the names without the server host/port appended,
     * we only optionally acquire these if nobody else has them.
     * This allows apps to avoid adding the server to the name - 
     * they can just use the "normal" name
     */
    {
    	DBusError tmp_derror;
        dbus_uint32_t flags;
        
    	dbus_error_init(&tmp_derror);        
        
        /* We do want to be queued if we don't get this right away */
        flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
        if (replace_existing)
            flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
        
        /* we just ignore errors on this */
        dbus_bus_request_name(connection, HIPPO_DBUS_ENGINE_BASE_BUS_NAME,
                              flags,
                              &tmp_derror);
        if (dbus_error_is_set(&tmp_derror))
        	g_debug("Failed to get bus name %s: %s", HIPPO_DBUS_ENGINE_BASE_BUS_NAME, tmp_derror.message);
       	else
       		g_debug("Acquired bus name %s", HIPPO_DBUS_ENGINE_BASE_BUS_NAME);       	
    }

    {
    	DBusError tmp_derror;    
        dbus_uint32_t flags;
        
    	dbus_error_init(&tmp_derror);        
        
        /* We do want to be queued if we don't get this right away */
        flags = DBUS_NAME_FLAG_ALLOW_REPLACEMENT;
        if (replace_existing)
            flags |= DBUS_NAME_FLAG_REPLACE_EXISTING;
        
        /* we just ignore errors on this */
        dbus_bus_request_name(connection, HIPPO_DBUS_STACKER_BASE_BUS_NAME,
                              flags,
                              &tmp_derror);
        if (dbus_error_is_set(&tmp_derror))
        	g_debug("Failed to get bus name %s: %s", HIPPO_DBUS_STACKER_BASE_BUS_NAME, tmp_derror.message);
       	else
       		g_debug("Acquired bus name %s", HIPPO_DBUS_STACKER_BASE_BUS_NAME);                              
    }
    
    /* the connection is already set up with the main loop. 
     * We just need to create our object, filters, etc. 
     */
    g_debug("D-BUS connection established");

    dbus = g_object_new(HIPPO_TYPE_DBUS, NULL);
    dbus->stacker_bus_name = stacker_bus_name;
    dbus->desktop_bus_name = desktop_bus_name;
    dbus->connection = connection;
    
    if (!dbus_connection_add_filter(connection, handle_message,
                                    dbus, NULL))
        g_error("no memory adding dbus connection filter");

    /* add an extra ref, which is owned by the "connected" state on 
     * the connection. We drop it in our filter func if we get 
     * the disconnected message or lose our bus name.
     */
    g_object_ref(dbus);

    /* we'll deal with this ourselves */
    dbus_connection_set_exit_on_disconnect(connection, FALSE);

    /* also returning a ref to the caller */    
    return dbus;
}

void
hippo_dbus_init_services(HippoDBus   *dbus)
{
    DBusConnection *connection;
    DBusError derror;
    
    connection = dbus->connection;

    dbus_error_init(&derror);
    
    /* Grab ownership of the Mugshot bus name */
    hippo_dbus_try_acquire_mugshot(connection, FALSE);

    /* Acquire online prefs manager; we continue even if this
     * fails. If another Mugshot had it, we should have replaced that
     * Mugshot above synchronously. So we don't pass replace=TRUE to
     * this.
     */
    hippo_dbus_try_acquire_online_prefs_manager(connection, FALSE);
    
    hippo_dbus_init_contacts(connection, FALSE);

    hippo_dbus_init_local(connection);
    hippo_dbus_init_pidgin(connection);    
    hippo_dbus_init_model(connection);
    hippo_dbus_init_http(connection);
    
    /* Add Rhythmbox signal match */
    dbus_bus_add_match(connection,
                       "type='signal',sender='"
                       RB_BUS_NAME
                       "',interface='"
                       RB_PLAYER_IFACE
                       "',member='"
                       RB_PLAYING_URI_CHANGED
                       "'",
                       &derror);

    if (dbus_error_is_set(&derror)) {
        g_warning("%s", derror.message);
        dbus_error_free(&derror);
    }

    dbus_bus_add_match(connection,
                       "type='signal',sender='"
                       MUINE_BUS_NAME
                       "',interface='"
                       MUINE_PLAYER_IFACE
                       "',member='"
                       MUINE_SONG_CHANGED
                       "'",
                       &derror);

    if (dbus_error_is_set(&derror)) {
        g_warning("%s", derror.message);
        dbus_error_free(&derror);
    }
    
    dbus_bus_add_match(connection,"type='signal',interface='"
                       BANSHEE_MUGSHOT_IFACE
                       "',member='"
                       BANSHEE_STATE_CHANGED
                       "'",
                       &derror);

    if (dbus_error_is_set(&derror)) {
        g_warning("%s", derror.message);
        dbus_error_free(&derror);
    }

    /* Add QuodLibet signal match */
    /* don't match on sender, because QL doesn't standardize on one
     * (which makes things really inefficient - bad QL)
     */
    dbus_bus_add_match(connection,
                       "type='signal',interface='" QL_PLAYER_IFACE
                       "',member='" QL_SONG_STARTED "'", 
                       &derror);
    
    if (dbus_error_is_set(&derror)) {
        g_warning("%s", derror.message);
        dbus_error_free(&derror);
    }
}

static void
hippo_dbus_finalize(GObject *object)
{
    HippoDBus *dbus = HIPPO_DBUS(object);

    g_debug("Finalizing dbus object");

    if (!dbus->emitted_disconnected)
        g_warning("Messed-up reference counting on HippoDBus object - connected state should own a ref");
    
    while (dbus->listeners)
	disconnect_listener(dbus, dbus->listeners->data);
    
    g_free(dbus->stacker_bus_name);
    g_free(dbus->desktop_bus_name);

#ifdef HAVE_DBUS_1_0
    /* pre-1.0 dbus is all f'd up and may crash if we do this when the
     * connection is still connected.
     */
    dbus_connection_unref(dbus->connection);
#endif
    
    G_OBJECT_CLASS(hippo_dbus_parent_class)->finalize(object);
}

static void
hippo_dbus_disconnected(HippoDBus *dbus)
{
    if (dbus->emitted_disconnected)
        return;

    dbus->emitted_disconnected = TRUE;
    
    while (dbus->listeners)
	disconnect_listener(dbus, dbus->listeners->data);
    
    /* the "connected" state owns one ref on the HippoDBus */
    g_signal_emit(G_OBJECT(dbus), signals[DISCONNECTED], 0);
    g_object_unref(dbus);
}

static HippoDBusListener *
find_listener_by_name(HippoDBus   *dbus,
                      const char  *name)
{
    GSList *l;

    for (l = dbus->listeners; l; l = l->next) {
	HippoDBusListener *listener = l->data;
	if (strcmp(listener->name, name) == 0) {
	    return listener;
	}
    }

    return NULL;
}

static HippoDBusListener *
find_listener(HippoDBus    *dbus,
	      DBusMessage  *message,
              DBusMessage **reply_p)
{
    const char *sender = dbus_message_get_sender(message);
    HippoDBusListener *listener = find_listener_by_name(dbus, sender);

    if (reply_p) {    
        if (listener == NULL)
            *reply_p = dbus_message_new_error(message,
                                              "com.dumbhippo.Error.BadId",
                                              _("Can't find any endpoint IDs for this listener"));
        else
            *reply_p = NULL;
    }
    
    return listener;
}

static char*
connection_gone_rule(const char *listener_name)
{
    return g_strdup_printf("type='signal',sender='%s',member='NameOwnerChanged',arg0='%s',arg1='%s',arg2=''",
                           DBUS_SERVICE_DBUS, listener_name, listener_name);
}

/* this can be called more than once since the bus "refcounts" identical rules */
void
hippo_dbus_watch_for_disconnect(HippoDBus  *dbus,
                                const char *name)
{
    char *rule;
    DBusError derror;

    if (!dbus_connection_get_is_connected(dbus->connection))
        return;
    
    dbus_error_init(&derror);

    rule = connection_gone_rule(name);
    
    dbus_bus_add_match(dbus->connection,
                       rule,
                       &derror);

    if (dbus_error_is_set(&derror)) {
        g_warning("Failed to add watch rule: %s: %s: %s",
                  rule,
                  derror.name,
                  derror.message);
        dbus_error_free(&derror);
    }
    g_free(rule);
}

void
hippo_dbus_unwatch_for_disconnect(HippoDBus  *dbus,
                                  const char *name)
{
    char *rule;
    DBusError derror;

    if (!dbus_connection_get_is_connected(dbus->connection))
        return;
    
    dbus_error_init(&derror);

    rule = connection_gone_rule(name);
    
    dbus_bus_remove_match(dbus->connection,
                          rule,
                          &derror);
    if (dbus_error_is_set(&derror)) {
        g_warning("Failed to remove watch rule: %s: %s: %s",
                  rule,
                  derror.name,
                  derror.message);
        dbus_error_free(&derror);
    }
    g_free(rule);
}

static HippoDBusListener *
add_new_listener(HippoDBus   *dbus,
		 DBusMessage *message)
{
    HippoDBusListener *listener = g_new0(HippoDBusListener, 1);
    listener->dbus = dbus;
    listener->name = g_strdup(dbus_message_get_sender(message));
    dbus->listeners = g_slist_prepend(dbus->listeners, listener);

    hippo_dbus_watch_for_disconnect(dbus, listener->name);

    g_debug("added listener %s", listener->name);
    
    return listener;
}

static void
on_endpoint_user_join(HippoEndpointProxy *proxy,
		      HippoChatRoom      *chat_room,
		      HippoPerson        *user,
                      gboolean            participant,
		      HippoDBusListener  *listener)
{
    DBusMessage *message;
    guint64 endpoint = hippo_endpoint_proxy_get_id(proxy);
    const char *chat_id = hippo_chat_room_get_id(chat_room);
    const char *user_id = hippo_entity_get_guid(HIPPO_ENTITY(user));
    dbus_bool_t participant_bool = participant;
    
    message = dbus_message_new_method_call(listener->name,
                                           HIPPO_DBUS_STACKER_LISTENER_PATH,
                                           HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                           "UserJoin");
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_STRING, &chat_id,
			     DBUS_TYPE_STRING, &user_id,
                 DBUS_TYPE_BOOLEAN, &participant_bool,
			     DBUS_TYPE_INVALID);

    dbus_message_set_no_reply(message, TRUE);
    dbus_connection_send(listener->dbus->connection, message, NULL);
    dbus_message_unref(message);
}

static void
on_endpoint_user_leave(HippoEndpointProxy *proxy,
		       HippoChatRoom     *chat_room,
		       HippoPerson       *user,
		       HippoDBusListener *listener)
{
    DBusMessage *message;
    
    guint64 endpoint = hippo_endpoint_proxy_get_id(proxy);
    const char *chat_id = hippo_chat_room_get_id(chat_room);
    const char *user_id = hippo_entity_get_guid(HIPPO_ENTITY(user));
    
    message = dbus_message_new_method_call(listener->name,
                                           HIPPO_DBUS_STACKER_LISTENER_PATH,
                                           HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                           "UserLeave");
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_STRING, &chat_id,
			     DBUS_TYPE_STRING, &user_id,
			     DBUS_TYPE_INVALID);

    dbus_message_set_no_reply(message, TRUE);
    dbus_connection_send(listener->dbus->connection, message, NULL);
    dbus_message_unref(message);
}

static void
on_endpoint_message(HippoEndpointProxy *proxy,
		    HippoChatRoom     *chat_room,
		    HippoChatMessage  *chat_message,
		    HippoDBusListener *listener)
{
    DBusMessage *message;
    
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    HippoConnection *connection = hippo_data_cache_get_connection(cache);
    guint64 endpoint = hippo_endpoint_proxy_get_id(proxy);
    const char *chat_id = hippo_chat_room_get_id(chat_room);
    const char *user_id = hippo_entity_get_guid(HIPPO_ENTITY(hippo_chat_message_get_person(chat_message)));
    const char *text = hippo_chat_message_get_text(chat_message);
    dbus_int32_t sentiment = 0;
    double timestamp;
    dbus_int32_t serial = hippo_chat_message_get_serial(chat_message);

    switch (hippo_chat_message_get_sentiment(chat_message)) {
    case HIPPO_SENTIMENT_INDIFFERENT:
        sentiment = 0;
        break;
    case HIPPO_SENTIMENT_LOVE:
        sentiment = 1;
        break;
    case HIPPO_SENTIMENT_HATE:
        sentiment = 2;
        break;
    }

    /* Time in millseconds */
    timestamp = hippo_chat_message_get_timestamp(chat_message) * 1000.;
    /* Convert server time to client time, so the Javascript can format it correctly */
    timestamp -= hippo_connection_get_server_time_offset(connection);

    g_debug("Sending message, %s, serial=%d", text, serial);

    message = dbus_message_new_method_call(listener->name,
                                           HIPPO_DBUS_STACKER_LISTENER_PATH,
                                           HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                           "Message");
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_STRING, &chat_id,
			     DBUS_TYPE_STRING, &user_id,
			     DBUS_TYPE_STRING, &text,
			     DBUS_TYPE_INT32, &sentiment,
			     DBUS_TYPE_DOUBLE, &timestamp,
			     DBUS_TYPE_INT32, &serial,
			     DBUS_TYPE_INVALID);

    dbus_message_set_no_reply(message, TRUE);
    dbus_connection_send(listener->dbus->connection, message, NULL);
    dbus_message_unref(message);
}

static void
on_endpoint_entity_info(HippoEndpointProxy *proxy,
		        HippoEntity        *entity,
                        HippoDBusListener  *listener)
{
    DBusMessage *message;

    guint64 endpoint = hippo_endpoint_proxy_get_id(proxy);
    const char *user_id = hippo_entity_get_guid(entity);
    const char *name = hippo_entity_get_name(entity);
    const char *photo_url = hippo_entity_get_photo_url(entity);

    if (HIPPO_IS_PERSON(entity)) {
        HippoPerson *person = HIPPO_PERSON(entity);
    
        const char *current_song = hippo_person_get_current_song(person);
        const char *current_artist = hippo_person_get_current_artist(person);
        dbus_bool_t music_playing = hippo_person_get_music_playing(person);
    
        message = dbus_message_new_method_call(listener->name,
                                               HIPPO_DBUS_STACKER_LISTENER_PATH,
                                               HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                               "UserInfo");

        /* dbus doesn't allow null strings */
        if (current_song == NULL)
            current_song = "";
        if (current_artist == NULL)
            current_artist = "";
        
        dbus_message_append_args(message,
                                 DBUS_TYPE_UINT64, &endpoint,
                                 DBUS_TYPE_STRING, &user_id,
                                 DBUS_TYPE_STRING, &name,
                                 DBUS_TYPE_STRING, &photo_url,
                                 DBUS_TYPE_STRING, &current_song,
                                 DBUS_TYPE_STRING, &current_artist,
                                 DBUS_TYPE_BOOLEAN, &music_playing,
                                 DBUS_TYPE_INVALID);

        dbus_message_set_no_reply(message, TRUE);
        dbus_connection_send(listener->dbus->connection, message, NULL);
        dbus_message_unref(message);
    }
}

static DBusMessage*
handle_register_endpoint(HippoDBus   *dbus,
                         DBusMessage *message)
{
    HippoDataCache *cache = hippo_app_get_data_cache(hippo_get_app());
    DBusMessage *reply;
    HippoEndpointProxy *proxy;
    HippoDBusListener *listener;
    dbus_uint64_t endpoint;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected no arguments"));
    }

    if (!dbus->xmpp_connected) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_FAILED,
				      _("XMPP connection not active"));
    }
    
    listener = find_listener(dbus, message, NULL);
    if (!listener) {
	listener = add_new_listener(dbus, message);
    }

    proxy = hippo_endpoint_proxy_new(cache);

    listener->endpoints = g_slist_prepend(listener->endpoints, proxy);

    g_signal_connect(proxy, "user-join",
		     G_CALLBACK(on_endpoint_user_join), listener);
    g_signal_connect(proxy, "user-leave",
		     G_CALLBACK(on_endpoint_user_leave), listener);
    g_signal_connect(proxy, "message",
		     G_CALLBACK(on_endpoint_message), listener);
    g_signal_connect(proxy, "entity-info",
		     G_CALLBACK(on_endpoint_entity_info), listener);
    
    reply = dbus_message_new_method_return(message);
    endpoint = hippo_endpoint_proxy_get_id(proxy),
    dbus_message_append_args(reply,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_INVALID);
    
    return reply;
}

static HippoEndpointProxy *
find_endpoint(HippoDBusListener *listener,
	      guint64            endpoint,
	      DBusMessage       *message,
	      DBusMessage      **reply)
{
    *reply = NULL;

    if (listener) {
	GSList *l;

	for (l = listener->endpoints; l; l = l->next) {
	    HippoEndpointProxy *proxy = l->data;
	    if (hippo_endpoint_proxy_get_id(proxy) == endpoint)
		return proxy;
	}
    }

    *reply = dbus_message_new_error(message,
				    "com.dumbhippo.Error.BadId",
				    _("Can't find endpoint ID"));

    return NULL;
}

static void
unregister_endpoint(HippoDBusListener *listener,
		    HippoEndpointProxy *proxy)
{
    g_signal_handlers_disconnect_by_func(proxy, (void *)on_endpoint_user_join, listener);
    g_signal_handlers_disconnect_by_func(proxy, (void *)on_endpoint_user_leave, listener);
    g_signal_handlers_disconnect_by_func(proxy, (void *)on_endpoint_message, listener);
    g_signal_handlers_disconnect_by_func(proxy, (void *)on_endpoint_entity_info, listener);

    listener->endpoints = g_slist_remove(listener->endpoints, proxy);
    hippo_endpoint_proxy_unregister(proxy);
    g_object_unref(proxy);
}

static void
disconnect_listener(HippoDBus         *dbus,
		    HippoDBusListener *listener)
{
    g_debug("Disconnecting listener %s", listener->name);
    
    while (listener->endpoints != NULL)
	unregister_endpoint(listener, listener->endpoints->data);

    while (listener->application_requests != NULL) {
        HippoApplicationRequest *request = listener->application_requests->data;
        request->listener = NULL;

        listener->application_requests = g_slist_delete_link(listener->application_requests, listener->application_requests);
    }
    
    dbus->listeners = g_slist_remove(dbus->listeners, listener);

    hippo_dbus_unwatch_for_disconnect(dbus, listener->name);
    
    g_free(listener->name);
    g_free(listener);
}

static DBusMessage*
handle_unregister_endpoint(HippoDBus   *dbus,
                           DBusMessage *message)
{
    DBusMessage *reply;
    guint64 endpoint;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected one argument, the endpoint ID"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    unregister_endpoint(listener, proxy);
    if (listener->endpoints == NULL)
	disconnect_listener(dbus, listener);
	
    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_set_window_id(HippoDBus   *dbus,
                     DBusMessage *message)
{
    DBusMessage *reply;
    guint64 endpoint;
    guint64 window_id;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_UINT64, &window_id,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected two arguments, the endpoint ID and the window ID"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    hippo_endpoint_proxy_set_window_id(proxy, window_id);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_join_chat_room(HippoDBus   *dbus,
		      DBusMessage *message)
{
    DBusMessage *reply;
    guint64 endpoint;
    const char *chat_id;
    dbus_bool_t participant;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    
    chat_id = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_STRING, &chat_id,
			       DBUS_TYPE_BOOLEAN, &participant,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected three arguments, the endpoint ID, the chat ID, and participant boolean"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    hippo_endpoint_proxy_join_chat_room(proxy, chat_id,
					participant ? HIPPO_CHAT_STATE_PARTICIPANT : HIPPO_CHAT_STATE_VISITOR);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_leave_chat_room(HippoDBus   *dbus,
		       DBusMessage *message)
{
    DBusMessage *reply;
    const char *chat_id;
    guint64 endpoint;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    
    chat_id = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_STRING, &chat_id,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected two arguments, the endpoint ID and the chat ID"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    hippo_endpoint_proxy_leave_chat_room(proxy, chat_id);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_show_chat_window(HippoDBus   *dbus,
			DBusMessage *message)
{
    DBusMessage *reply;
    const char *chat_id;
    
    chat_id = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_STRING, &chat_id,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected one string arg, the chat ID"));
    }

    hippo_app_join_chat(hippo_get_app(), chat_id);
    
    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_send_chat_message(HippoDBus   *dbus,
			 DBusMessage *message)
{
    DBusMessage *reply;
    HippoDataCache *cache;
    HippoConnection *connection;
    HippoChatRoom *room;
    const char *chat_id;
    const char *message_text;
    dbus_int32_t sentiment;
    HippoSentiment hippoSentiment;

    cache = hippo_app_get_data_cache(hippo_get_app());
    connection = hippo_data_cache_get_connection(cache);
    
    chat_id = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_STRING, &chat_id,
			       DBUS_TYPE_STRING, &message_text,
                               DBUS_TYPE_INT32, &sentiment,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected three args, the chat ID (string), the message text (string), and the sentiment (0,1,2)"));
    }

    switch (sentiment) {
    case 0:
        hippoSentiment = HIPPO_SENTIMENT_INDIFFERENT;
        break;
    case 1:
        hippoSentiment = HIPPO_SENTIMENT_LOVE;
        break;
    case 2:
        hippoSentiment = HIPPO_SENTIMENT_HATE;
        break;
    default:
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Invalid sentiment, should be 0=INDIFFERENT, 1=LOVE, or 2=HATE"));
    }

    room = hippo_data_cache_ensure_chat_room(cache, chat_id, HIPPO_CHAT_KIND_UNKNOWN);
    hippo_connection_send_chat_room_message(connection, room, message_text, hippoSentiment);
    
    reply = dbus_message_new_method_return(message);
    return reply;
}

static HippoApplicationRequest *
hippo_application_request_new(HippoDBusListener  *listener,
                              guint64             endpoint,
                              const char         *application_id,
                              const char         *package_names,
                              const char         *desktop_names)
{
    HippoApplicationRequest *request = g_new0(HippoApplicationRequest, 1);

    request->listener = listener;
    request->endpoint = endpoint;
    request->application_id = g_strdup(application_id);
    request->package_names = g_strdup(package_names);
    request->desktop_names = g_strdup(desktop_names);

    request->package_response = FALSE;
    request->application_response = FALSE;

    listener->application_requests = g_slist_prepend(listener->application_requests, listener);

    return request;
}

static void
hippo_application_request_finish(HippoApplicationRequest *request)
{
    if (request->listener) {
        HippoDBusListener *listener = request->listener;
        DBusMessage *message;

        const char *version = request->version ? request->version : "";
        dbus_bool_t can_install = request->can_install;
        dbus_bool_t can_run = request->can_run;
            
        listener->application_requests = g_slist_remove(listener->application_requests, request);

        message = dbus_message_new_method_call(listener->name,
                                               HIPPO_DBUS_STACKER_LISTENER_PATH,
                                               HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                               "ApplicationInfo");

        dbus_message_append_args(message,
                                 DBUS_TYPE_UINT64, &request->endpoint,
                                 DBUS_TYPE_STRING, &request->application_id,
                                 DBUS_TYPE_BOOLEAN, &can_install,
                                 DBUS_TYPE_BOOLEAN, &can_run,
                                 DBUS_TYPE_STRING, &version,
                                 DBUS_TYPE_INVALID);

        dbus_message_set_no_reply(message, TRUE);
        dbus_connection_send(listener->dbus->connection, message, NULL);
        dbus_message_unref(message);
    }
    
    g_free(request->application_id);
    g_free(request->package_names);
    g_free(request->desktop_names);
    g_free(request->version);
    g_free(request);
}

static void
get_application_info_check_package_callback(gboolean    is_installed,
                                            gboolean    is_installable,
                                            const char *installed_version,
                                            void       *data)
{
    HippoApplicationRequest *request = data;

    request->can_install = is_installable;
    request->version = g_strdup(installed_version);
    request->package_response = TRUE;

    if (request->package_response && request->application_response)
        hippo_application_request_finish(request);
}

static void
get_application_info_check_application_callback(gboolean  is_runnable,
                                                void     *data)
{
    HippoApplicationRequest *request = data;

    request->can_run = is_runnable;
    request->application_response = TRUE;
    
    if (request->package_response && request->application_response)
        hippo_application_request_finish(request);
}

static void
do_get_application_info(HippoApplicationRequest *request)
{
    HippoDistribution *distro = hippo_distribution_get();

    /* Call check_package() before check_application() because check_package()
     * is currently out-of-process and async while check_application() isn't
     */
    hippo_distribution_check_package(distro,
                                     request->package_names,
                                     get_application_info_check_package_callback,
                                     request);
    hippo_distribution_check_application(distro,
                                         request->desktop_names,
                                         get_application_info_check_application_callback,
                                         request);
}

static DBusMessage*
handle_get_application_info(HippoDBus   *dbus,
                            DBusMessage *message)
{
    DBusMessage *reply;
    guint64 endpoint;
    const char *application_id;
    const char *package_names;
    const char *desktop_names;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    HippoApplicationRequest *request;
    
    application_id = NULL;
    package_names = NULL;
    desktop_names = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_STRING, &application_id,
			       DBUS_TYPE_STRING, &package_names,
			       DBUS_TYPE_STRING, &desktop_names,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected four arguments, the endpoint ID, the application Id, possible package names, possible desktop names"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    g_debug("GetApplicationInfo(%" G_GUINT64_FORMAT ", %s, %s, %s)\n", endpoint, application_id, package_names, desktop_names);

    request = hippo_application_request_new(listener, endpoint, application_id, package_names, desktop_names);
    do_get_application_info(request);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static void
install_application_install_package_callback(GError   *error,
                                             void     *data)
{
    HippoApplicationRequest *request = data;

    do_get_application_info(request);
}


static void
do_install_application(HippoApplicationRequest *request)
{
    HippoDistribution *distro = hippo_distribution_get();

    hippo_distribution_install_package(distro,
                                       request->package_names,
                                       install_application_install_package_callback,
                                       request);
}

static DBusMessage*
handle_install_application(HippoDBus   *dbus,
                           DBusMessage *message)
{
    DBusMessage *reply;
    guint64 endpoint;
    const char *application_id;
    const char *package_names;
    const char *desktop_names;
    HippoDBusListener *listener;
    HippoEndpointProxy *proxy;
    HippoApplicationRequest *request;
    
    application_id = NULL;
    package_names = NULL;
    desktop_names = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_UINT64, &endpoint,
			       DBUS_TYPE_STRING, &application_id,
			       DBUS_TYPE_STRING, &package_names,
			       DBUS_TYPE_STRING, &desktop_names,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected four arguments, the endpoint ID, the application Id, possible package names, possible desktop names"));
    }
    
    listener = find_listener(dbus, message, &reply);
    if (!listener)
        return reply;
    proxy = find_endpoint(listener, endpoint, message, &reply);
    if (!proxy)
	return reply;

    g_debug("InstallApplication(%" G_GUINT64_FORMAT ", %s, %s, %s)\n", endpoint, application_id, package_names, desktop_names);

    request = hippo_application_request_new(listener, endpoint, application_id, package_names, desktop_names);
    do_install_application(request);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static void
run_application_callback(GError   *error,
                         void     *data)
{
}

static void
do_run_application(const char *desktop_names,
                   guint32     timestamp)
{

    HippoDistribution *distro = hippo_distribution_get();

    hippo_distribution_run_application(distro, desktop_names, timestamp,
                                       run_application_callback, NULL);
}

static DBusMessage*
handle_run_application(HippoDBus   *dbus,
                       DBusMessage *message)
{
    DBusMessage *reply;
    const char *desktop_names;
    dbus_uint32_t timestamp;
    
    desktop_names = NULL;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_STRING, &desktop_names,
			       DBUS_TYPE_UINT32, &timestamp,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected two arguments, possible desktop names and timestamp"));
    }
    
    g_debug("RunApplication(%s, %d)\n", desktop_names, timestamp);

    do_run_application(desktop_names, timestamp);

    reply = dbus_message_new_method_return(message);
    return reply;
}

static DBusMessage*
handle_show_browser(HippoDBus   *dbus,
                    DBusMessage *message)
{
    DBusMessage *reply;
    HippoDataCache *cache;
    
    if (!dbus_message_get_args(message, NULL,
			       DBUS_TYPE_INVALID)) {
        return dbus_message_new_error(message,
				      DBUS_ERROR_INVALID_ARGS,
				      _("Expected no arguments"));
    }

    cache = hippo_app_get_data_cache(hippo_get_app());

    hippo_app_set_show_stacker(hippo_get_app(), TRUE);
    hippo_stack_manager_show_browser(hippo_app_get_stack(hippo_get_app()),
                                     FALSE);
    
    reply = dbus_message_new_method_return(message);
    return reply;
}

static void
emit_song_changed_from_rb_message(HippoDBus   *dbus,
                                  DBusMessage *message)
{
#define MAX_PROPS 10
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    HippoSong song;
    char **keys;
    char **values;
    int i;

    i = 0;
    keys = g_new0(char*, MAX_PROPS + 1);
    values = g_new0(char*, MAX_PROPS + 1);
    
    /* signature supposed to be checked already */
    
    dbus_message_iter_init(message, &iter);
    if (dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_INVALID)
        goto bad_args;
    
    dbus_message_iter_recurse(&iter, &array_iter);
    
    while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
        DBusMessageIter struct_iter;
        DBusMessageIter variant_iter;
        const char *prop_name;
        int prop_type;
        
        prop_name = NULL;
    
        dbus_message_iter_recurse(&array_iter, &struct_iter);
        
        /* struct_iter should have a string and a variant in it */
        dbus_message_iter_get_basic(&struct_iter, &prop_name);
        dbus_message_iter_next(&struct_iter);
        dbus_message_iter_recurse(&struct_iter, &variant_iter);
        
        prop_type = dbus_message_iter_get_arg_type(&variant_iter);
        
        /* g_debug("Property '%s' has type '%c'", prop_name, prop_type); */

        if (strcmp(prop_name, "type") == 0) {
            /* type UINT32 maybe 0=song 1=iradio_station 2=podcast_post 3=podcast_feed
             * going to 
             * key "type" TrackType enum UNKNOWN, FILE, CD, NETWORK_STREAM, PODCAST 
             */
             /* this doesn't really map, so we just skip it for now */
        } else if (strcmp(prop_name, "mimetype") == 0) {
            /* type STRING
             * going to 
             * key "format" MediaFileFormat enum UNKNOWN, MP3, WMA, AAC, VORBIS
             */
             const char *prop_val = NULL;
             if (prop_type != DBUS_TYPE_STRING)
                goto bad_args;
             dbus_message_iter_get_basic(&variant_iter, &prop_val);
             g_debug("mime type %s", prop_val);
             if (strcmp(prop_val, "application/ogg") == 0) {
                keys[i] = g_strdup("format");
                values[i] = g_strdup("VORBIS");
                ++i;
             } else {
                
             }
        } else if (strcmp(prop_name, "title") == 0) {
            /* type STRING going to key "name" */
            const char *prop_val = NULL;            
            if (prop_type != DBUS_TYPE_STRING)
               goto bad_args;            
            dbus_message_iter_get_basic(&variant_iter, &prop_val);
            g_debug("title %s", prop_val);
            keys[i] = g_strdup("name");
            values[i] = g_strdup(prop_val);
            ++i;
        } else if (strcmp(prop_name, "artist") == 0) {
            /* type STRING going to key "artist" */
            const char *prop_val = NULL;            
            if (prop_type != DBUS_TYPE_STRING)
               goto bad_args;            
            dbus_message_iter_get_basic(&variant_iter, &prop_val);
            g_debug("artist %s", prop_val);
            keys[i] = g_strdup("artist");
            values[i] = g_strdup(prop_val);
            ++i;            
        } else if (strcmp(prop_name, "album") == 0) {
            /* type STRING going to key "album" */
            const char *prop_val = NULL;            
            if (prop_type != DBUS_TYPE_STRING)
               goto bad_args;                        
            dbus_message_iter_get_basic(&variant_iter, &prop_val);
            g_debug("album %s", prop_val);
            keys[i] = g_strdup("album");
            values[i] = g_strdup(prop_val);
            ++i;                        
        } else if (strcmp(prop_name, "duration") == 0) {
            /* type UINT32 in seconds (I think) going to key "duration" in seconds */
            dbus_uint32_t val = 0;
            if (prop_type != DBUS_TYPE_UINT32)
               goto bad_args;                        
            dbus_message_iter_get_basic(&variant_iter, &val);
            g_debug("duration %u", val);
            keys[i] = g_strdup("duration");
            values[i] = g_strdup_printf("%u", val);
            ++i;                  
        } else if (strcmp(prop_name, "file-size") == 0) {
            /* type UINT64 going to key "fileSize" in bytes */
            dbus_uint64_t val;
            if (prop_type != DBUS_TYPE_UINT64)
               goto bad_args;                    
            dbus_message_iter_get_basic(&variant_iter, &val);
            g_debug("file-size %" G_GUINT64_FORMAT, val);
            keys[i] = g_strdup("fileSize");
            values[i] = g_strdup_printf("%" G_GUINT64_FORMAT, val);
            ++i;
        } else if (strcmp(prop_name, "track-number") == 0) {
            /* type UINT32 ?-based going to key "trackNumber" 1-based */
            dbus_uint32_t val = 0;
            if (prop_type != DBUS_TYPE_UINT32)
               goto bad_args;                     
            dbus_message_iter_get_basic(&variant_iter, &val);
            g_debug("track-number %u", val);
            /* FIXME Skipping this for now since I'm not sure if it's 1-based from rb */
        } else {
            /* Not interested in this property */
            /* the server supports a "discIdentifier" also but rhythmbox doesn't seem to */
        }
            
        dbus_message_iter_next(&array_iter);
    }

    g_assert(i < MAX_PROPS);
    
    song.keys = keys;
    song.values = values;
    g_signal_emit(G_OBJECT(dbus), signals[SONG_CHANGED], 0, &song);

    g_strfreev(keys);
    g_strfreev(values);

    return;

  bad_args:
    g_debug("Failed to interpret args to getSongProperties");
    g_strfreev(keys);
    g_strfreev(values);
}                   

static void
on_get_song_props_reply(DBusPendingCall *pending,
                        void            *user_data)
{
    HippoDBus *dbus;
    DBusMessage *reply;
    
    dbus = HIPPO_DBUS(user_data);

    reply = dbus_pending_call_steal_reply(pending);
    if (reply == NULL) {
        g_warning("NULL reply in on_get_song_props_reply?");
        return;
    }
    
    if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_METHOD_RETURN) {
        /* Traverse array of struct { string; variant; } */
        if (!dbus_message_has_signature(reply, "a{sv}")) {
            g_debug("getSongProperties reply has wrong signature '%s'",
                dbus_message_get_signature(reply));
        } else {

            g_debug("getSongProperties reply received");
            
            emit_song_changed_from_rb_message(dbus, reply);
        }
    } else if (dbus_message_get_type(reply) == DBUS_MESSAGE_TYPE_ERROR) {
        hippo_dbus_debug_log_error("getSongProperties", reply);
    } else {
        g_warning("weird unknown reply type %d to get_song_props_reply",
            dbus_message_get_type(reply));
    }
    
    dbus_message_unref(reply);
}

static void
handle_banshee_state_changed( HippoDBus   *dbus,
                              DBusMessage *message)
{

    int state,duration;
    const char *artist, *title, *album;
   
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_INT32, &state,
                               DBUS_TYPE_STRING, &artist,
                               DBUS_TYPE_STRING, &title,
                               DBUS_TYPE_STRING, &album,
                               DBUS_TYPE_INT32, &duration,
                               DBUS_TYPE_INVALID)) {
        g_warning("Banshee stateChanged signal had unexpected arguments");
        return;                           
    }

    if (state) {

        HippoSong song;
        char **keys;
        char **values;

        keys = g_new0(char*, 5);
        values = g_new0(char*, 5);
        
        keys[0] = g_strdup("name");
        values[0] = g_strdup(title);

        keys[1] = g_strdup("artist");
        values[1] = g_strdup(artist);

        keys[2] = g_strdup("album");
        values[2] = g_strdup(album);

        keys[3] = g_strdup("duration");
        values[3] = g_strdup_printf("%u",duration);

        song.keys = keys;
        song.values = values;
        g_signal_emit(G_OBJECT(dbus), signals[SONG_CHANGED], 0, &song);
        g_strfreev(keys);
        g_strfreev(values);

    }
    else {
        /*TODO send music stopped signal */
    }
}

static void
handle_ql_song_started(HippoDBus   *dbus,
                       DBusMessage *message)
{
#define MAX_PROPS 10
    DBusMessageIter iter;
    DBusMessageIter array_iter;
    HippoSong song;
    char **keys;
    char **values;
    int i;

    i = 0;
    keys = g_new0(char*, MAX_PROPS+1);
    values = g_new0(char*, MAX_PROPS+1);

    dbus_message_iter_init(message, &iter);
    if(dbus_message_iter_get_arg_type(&iter) == DBUS_TYPE_INVALID)
        goto bad_args;

    dbus_message_iter_recurse(&iter, &array_iter);

    while (dbus_message_iter_get_arg_type(&array_iter) != DBUS_TYPE_INVALID) {
        DBusMessageIter struct_iter;
        const char *prop_name;
        const char *prop_val;

        prop_name = NULL;

        dbus_message_iter_recurse(&array_iter, &struct_iter);

        /* struct_iter should have two strings (name and value) in it */
        if(dbus_message_iter_get_arg_type(&struct_iter) != DBUS_TYPE_STRING)
            continue;
        dbus_message_iter_get_basic(&struct_iter, &prop_name);
        if(!dbus_message_iter_has_next(&struct_iter))
            continue;
        dbus_message_iter_next(&struct_iter);
        if(dbus_message_iter_get_arg_type(&struct_iter) != DBUS_TYPE_STRING)
            continue;
        dbus_message_iter_get_basic(&struct_iter, &prop_val);

        if (strcmp(prop_name, "title") == 0) {
            g_debug("title %s", prop_val);
            keys[i] = g_strdup("name");
            values[i] = g_strdup(prop_val);
            ++i;
        } else if (strcmp(prop_name, "artist") == 0) {
            g_debug("artist %s", prop_val);
            keys[i] = g_strdup("artist");
            values[i] = g_strdup(prop_val);
            ++i;            
        } else if (strcmp(prop_name, "album") == 0) {
            g_debug("album %s", prop_val);
            keys[i] = g_strdup("album");
            values[i] = g_strdup(prop_val);
            ++i;                        
        } else if (strcmp(prop_name, "~#length") == 0) {
            /* type STRING in seconds going to key "duration" in seconds */
            g_debug("duration %s", prop_val);
            keys[i] = g_strdup("duration");
            values[i] = g_strdup(prop_val);
            ++i;                  
        } else if (strcmp(prop_name, "tracknumber") == 0) {
            /* type STRING of form "01" or "01/10" going to key "trackNumber" of form "1" */
            const char *track;
            const char *endtrack;
            for (track = prop_val; *track < '1' || *track > '9'; track++)
                ;
            for (endtrack = track; *endtrack >= '0' && *endtrack <= '9'; endtrack++)
                ;
            if (endtrack > track) {
                keys[i] = g_strdup("trackNumber");
                values[i] = g_strndup(track, endtrack - track);
                g_debug("trackNumber %s", values[i]);
                ++i;
            }
        } else {
            /* Not interested in this property */
            /* QL also supports date, genre, discnumber, ~#rating, ~#bitrate etc. */
            /* the server also supports fileSize, MediaFileFormat, discIdentifier, 
               and TrackType */
            g_debug("discarded property '%s' value '%s'", prop_name, prop_val);
        }
            
        dbus_message_iter_next(&array_iter);
    }

    g_assert(i < MAX_PROPS);
    
    song.keys = keys;
    song.values = values;
    g_signal_emit(G_OBJECT(dbus), signals[SONG_CHANGED], 0, &song);

    g_strfreev(keys);
    g_strfreev(values);

    return;

  bad_args:
    g_debug("Failed to parse dbus message in handle_ql_song_started");
    g_strfreev(keys);
    g_strfreev(values);
}

static void
handle_muine_song_changed(HippoDBus   *dbus,
                          DBusMessage *message)
{
#define MAX_PROPS 10
    HippoSong song;
    char *string, *value;
    char **keys, **values;
    char **fields;
    int i, j;

    i = 0;

    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_STRING, &string,
                               DBUS_TYPE_INVALID)) {
        g_warning("Muine SongChanged signal had unexpected arguments");

        return;
    }

    keys = g_new0(char*, MAX_PROPS + 1);
    values = g_new0(char*, MAX_PROPS + 1);
    
    fields = g_strsplit(string, "\n", 0);

    for (j = 0; fields[j] != NULL; j++) {
        value = strchr(fields[j], ':');
        if (value != NULL && value[1] != '\0' && value[2] != '\0') {
            value += 2;
            if (g_strncasecmp(fields[j], "artist", 6) == 0) {
                g_debug(fields[j]);
                keys[i] = g_strdup("artist");
                values[i] = g_strdup(value);
                ++i;
            } else if (g_strncasecmp(fields[j], "title", 5) == 0) {
                g_debug(fields[j]);
                keys[i] = g_strdup("name");
                values[i] = g_strdup(value);
                ++i;
            } else if (g_strncasecmp(fields[j], "album", 5) == 0) {
                g_debug(fields[j]);
                keys[i] = g_strdup("album");
                values[i] = g_strdup(value);
                ++i;
            } else if (g_strncasecmp(fields[j], "duration", 8) == 0) {
                g_debug(fields[j]);
                keys[i] = g_strdup("duration");
                values[i] = g_strdup(value);
                ++i;
            } else if (g_strncasecmp(fields[j], "track_number", 12) == 0) {
                g_debug(fields[j]);
                keys[i] = g_strdup("track-number");
                values[i] = g_strdup(value);
                ++i;
            }
        }
    }
    g_assert(i < MAX_PROPS);
    g_strfreev(fields);

    song.keys = keys;
    song.values = values;
    g_signal_emit(G_OBJECT(dbus), signals[SONG_CHANGED], 0, &song);

    g_strfreev(keys);
    g_strfreev(values);
}

static void
handle_rb_playing_uri_changed(HippoDBus   *dbus,
                              DBusMessage *message)
{
    DBusMessage *get_props;
    DBusPendingCall *call;
    const char *uri;
    
    uri = NULL;
    if (!dbus_message_get_args(message, NULL,
                               DBUS_TYPE_STRING, &uri,
                               DBUS_TYPE_INVALID)) {
        g_warning("Rhythmbox playingUriChanged signal had unexpected arguments");
        return;                           
    }
    
    get_props = dbus_message_new_method_call(RB_BUS_NAME, RB_SHELL_PATH, RB_SHELL_IFACE,
                                                RB_GET_SONG_PROPERTIES);
    if (get_props == NULL)
        g_error("out of memory");
    if (!dbus_message_append_args(get_props, DBUS_TYPE_STRING, &uri, DBUS_TYPE_INVALID))
        g_error("out of memory");

    call = NULL;               
    dbus_connection_send_with_reply(dbus->connection, get_props, &call, -1);
    if (call != NULL) {
        g_object_ref(dbus);
        if (!dbus_pending_call_set_notify(call, on_get_song_props_reply,
                                          dbus, (DBusFreeFunction) g_object_unref))
            g_error("out of memory");

        /* rely on connection to hold a reference to it, if finalized
         * I think on_get_song_props_reply won't get called though, 
         * which is fine currently
         */
        dbus_pending_call_unref(call);        
    }
    dbus_message_unref(get_props);
}

static DBusHandlerResult
handle_message(DBusConnection     *connection,
               DBusMessage        *message,
               void               *user_data)
{
    HippoDBus *dbus;
    int type;
    DBusHandlerResult result;
    HippoDataCache *cache;
    HippoConnection *xmpp_connection;
    
    dbus = HIPPO_DBUS(user_data);
    
    type = dbus_message_get_type(message);

    result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    dbus->in_dispatch = TRUE;

    cache = hippo_app_get_data_cache(hippo_get_app());
    xmpp_connection = hippo_data_cache_get_connection(cache);

    if (type == DBUS_MESSAGE_TYPE_METHOD_CALL) {
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);
        const char *path = dbus_message_get_path(message);        
        
        g_debug("method call from %s %s.%s on %s", sender ? sender : "NULL",
                interface ? interface : "NULL",
                member ? member : "NULL",
                path ? path : "NULL");
    
        if (path && member &&
            strcmp(path, HIPPO_DBUS_STACKER_PATH) == 0) {
            DBusMessage *reply;
            
            reply = NULL;
            result = DBUS_HANDLER_RESULT_HANDLED;
            
            if (strcmp(member, "RegisterEndpoint") == 0) {
                reply = handle_register_endpoint(dbus, message);
	    } else if (strcmp(member, "UnregisterEndpoint") == 0) {
                reply = handle_unregister_endpoint(dbus, message);
	    } else if (strcmp(member, "SetWindowId") == 0) {
                reply = handle_set_window_id(dbus, message);
	    } else if (strcmp(member, "JoinChatRoom") == 0) {
                reply = handle_join_chat_room(dbus, message);
	    } else if (strcmp(member, "LeaveChatRoom") == 0) {
                reply = handle_leave_chat_room(dbus, message);
	    } else if (strcmp(member, "SendChatMessage") == 0) {
                reply = handle_send_chat_message(dbus, message);
	    } else if (strcmp(member, "ShowChatWindow") == 0 ||
                       /* JoinChat is the legacy name until we have an rpm built with new url handler */
                       strcmp(member, "JoinChat") == 0) {
                reply = handle_show_chat_window(dbus, message);
	    } else if (strcmp(member, "GetApplicationInfo") == 0) {
                reply = handle_get_application_info(dbus, message);
	    } else if (strcmp(member, "InstallApplication") == 0) {
                reply = handle_install_application(dbus, message);
	    } else if (strcmp(member, "RunApplication") == 0) {
                reply = handle_run_application(dbus, message);
	    } else if (strcmp(member, "ShowBrowser") == 0) {
                reply = handle_show_browser(dbus, message);
            } else {
                /* Set this back so the default handler can return an error */
                result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
            }
            
            if (reply != NULL) {
                dbus_connection_send(dbus->connection, reply, NULL);
                dbus_message_unref(reply);
            }
        } else if (path && member &&
                   strcmp(path, HIPPO_DBUS_WEB_PATH) == 0) {
            DBusMessage *reply;
            
            reply = NULL;
            result = DBUS_HANDLER_RESULT_HANDLED;

            if (strcmp(member, "GetCookiesToSend") == 0) {
                reply = hippo_dbus_handle_get_cookies_to_send(dbus, message);
            } else if (strcmp(member, "Introspect") == 0) {
                reply = hippo_dbus_handle_introspect_web(dbus, message);                
            } else {
                /* Set this back so the default handler can return an error */
                result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
            }

            if (reply != NULL) {
                dbus_connection_send(dbus->connection, reply, NULL);
                dbus_message_unref(reply);
            }
        } else if (path && member &&
                   strcmp(path, HIPPO_DBUS_MUGSHOT_PATH) == 0) {
            DBusMessage *reply;
            
            reply = NULL;
            result = DBUS_HANDLER_RESULT_HANDLED;

            if (strcmp(member, "GetConnectionStatus") == 0) {
                reply = hippo_dbus_handle_mugshot_get_connection_status(dbus, message);
            } else if (strcmp(member, "NotifyAllWhereim") == 0) {
                reply = hippo_dbus_handle_mugshot_get_whereim(dbus, xmpp_connection, message);
            } else if (strcmp(member, "GetNetwork") == 0) {
                reply = hippo_dbus_handle_mugshot_get_network(dbus, message);
            } else if (strcmp(member, "SendExternalIQ") == 0) {
                reply = hippo_dbus_handle_mugshot_send_external_iq(dbus, message);                       
            } else if (strcmp(member, "GetBaseProperties") == 0) {
                reply = hippo_dbus_handle_mugshot_get_baseprops(dbus, message);                   
            } else if (strcmp(member, "GetSelf") == 0) {
            	reply = hippo_dbus_handle_mugshot_get_self(dbus, message);                
            } else if (strcmp(member, "Introspect") == 0) {
                reply = hippo_dbus_handle_mugshot_introspect(dbus, message);                
            } else {
                /* Set this back so the default handler can return an error */
                result = DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
            }

            if (reply != NULL) {
                dbus_connection_send(dbus->connection, reply, NULL);
                dbus_message_unref(reply);
            }        
        } else if (path && member &&
                   g_str_has_prefix(path, HIPPO_DBUS_MUGSHOT_DATACACHE_PATH_PREFIX)) {
            DBusMessage *reply;
                               	
            reply = hippo_dbus_handle_mugshot_entity_message(dbus, message);
            
            if (reply != NULL) {
            	result = DBUS_HANDLER_RESULT_HANDLED;
        	    dbus_connection_send(dbus->connection, reply, NULL);
                dbus_message_unref(reply);
            }
        }        
    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
        const char *sender = dbus_message_get_sender(message);
        const char *interface = dbus_message_get_interface(message);
        const char *member = dbus_message_get_member(message);

        g_debug("signal from %s %s.%s", sender ? sender : "NULL", interface, member);
   
        if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
            dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameLost")) {
            /* If we lose our name, we behave as if disconnected */
            const char *name = NULL;
            if (dbus_message_get_args(message, NULL, DBUS_TYPE_STRING, &name, DBUS_TYPE_INVALID) && 
                (strcmp(name, dbus->stacker_bus_name) == 0 ||
                 strcmp(name, dbus->desktop_bus_name) == 0)) {

                hippo_dbus_disconnected(dbus);
                dbus = NULL;
            }
        } else if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
                   dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameOwnerChanged")) {
            const char *name = NULL;
            const char *old = NULL;
            const char *new = NULL;
            if (dbus_message_get_args(message, NULL,
                                      DBUS_TYPE_STRING, &name,
                                      DBUS_TYPE_STRING, &old,
                                      DBUS_TYPE_STRING, &new,
                                      DBUS_TYPE_INVALID)) {
                g_debug("NameOwnerChanged %s '%s' -> '%s'", name, old, new);
                if (*old == '\0')
                    old = NULL;
                if (*new == '\0')
                    new = NULL;
                if (old && strcmp(name, old) == 0) {
                    HippoDBusListener *listener = find_listener_by_name(dbus, old);
                    if (listener != NULL) {
                        /* free this listener and forget about it (along with all its endpoints) */
                        disconnect_listener(dbus, listener);
                    } else {
                        hippo_dbus_model_name_gone(old);
                    }
                }
            } else {
                g_warning("NameOwnerChanged had wrong args???");
            }
        } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
            hippo_dbus_disconnected(dbus);
            dbus = NULL;
        } else if (dbus_message_is_signal(message, RB_PLAYER_IFACE, RB_PLAYING_URI_CHANGED)) {
            handle_rb_playing_uri_changed(dbus, message);
        } else if (dbus_message_is_signal(message, BANSHEE_MUGSHOT_IFACE, BANSHEE_STATE_CHANGED)) {
            handle_banshee_state_changed(dbus, message);
        } else if (dbus_message_is_signal(message, MUINE_PLAYER_IFACE, MUINE_SONG_CHANGED)) {
            handle_muine_song_changed(dbus, message);
        } else if (dbus_message_is_signal(message, QL_PLAYER_IFACE, QL_SONG_STARTED)) {
            handle_ql_song_started(dbus, message);
        }
    } else if (dbus_message_get_type(message) == DBUS_MESSAGE_TYPE_ERROR) {
        hippo_dbus_debug_log_error("main connection handler", message);
    } else {
        /* g_debug("got message type %s\n", 
           dbus_message_type_to_string(type));    */
        ;
    }
    
    if (dbus)
        dbus->in_dispatch = FALSE;
        
    return result;
}

void
hippo_dbus_notify_auth_changed(HippoDBus   *dbus)
{
	DBusMessage *message;
	message = hippo_dbus_mugshot_signal_connection_changed(dbus);
    dbus_connection_send(dbus->connection, message, NULL);
    dbus_message_unref(message);	
}                               

void
hippo_dbus_notify_contacts_loaded(HippoDBus   *dbus)
{
	DBusMessage *message;
	message = hippo_dbus_mugshot_signal_connection_changed(dbus);
    dbus_connection_send(dbus->connection, message, NULL);
    dbus_message_unref(message);	
}   

void
hippo_dbus_notify_xmpp_connected(HippoDBus   *dbus,
                                 gboolean     connected)
{
	DBusMessage *message;
    if (dbus->xmpp_connected == (connected != FALSE))
        return;
    
    dbus->xmpp_connected = connected != FALSE;

    message = hippo_dbus_mugshot_signal_connection_changed(dbus);
    dbus_connection_send(dbus->connection, message, NULL);
    dbus_message_unref(message);
        
    if (dbus->xmpp_connected) {
        /* notify all the listeners */
        message = dbus_message_new_signal(HIPPO_DBUS_STACKER_LISTENER_PATH,
                                          HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                          "Connected");
        
        dbus_connection_send(dbus->connection, message, NULL);
        dbus_message_unref(message);
    } else {
        message = dbus_message_new_signal(HIPPO_DBUS_STACKER_LISTENER_PATH,
                                          HIPPO_DBUS_STACKER_LISTENER_INTERFACE,
                                          "Disconnected");
        
        dbus_connection_send(dbus->connection, message, NULL);
        dbus_message_unref(message);        

        /* disconnect all the listeners (includes notifying them) */
        while (dbus->listeners)
            disconnect_listener(dbus, dbus->listeners->data);
    }
}

void 
hippo_dbus_foreach_chat_window(HippoDBus             *dbus,
                               const char            *chat_id,
                               HippoChatWindowForeach function,
                               void *                 data)
{
    GSList *l, *ll;

    for (l = dbus->listeners; l; l = l->next) {
	HippoDBusListener *listener = l->data;

	for (ll = listener->endpoints; ll; ll = ll->next) {
	    HippoEndpointProxy *proxy = ll->data;
            guint64 window_id = hippo_endpoint_proxy_get_window_id(proxy);
            HippoChatState state = hippo_endpoint_proxy_get_chat_state(proxy, chat_id);

            if (state != HIPPO_CHAT_STATE_NONMEMBER && window_id != 0)
                (*function) (window_id, state, data);
	}
    }
}

void       
hippo_dbus_notify_whereim_changed(HippoDBus               *dbus,
                                  HippoConnection         *xmpp_connection,
                                  HippoExternalAccount    *acct)
{
    DBusMessage *signal;
    signal = hippo_dbus_mugshot_signal_whereim_changed(dbus, xmpp_connection, acct);
    dbus_connection_send(dbus->connection, signal, NULL);
    dbus_message_unref(signal);
}

void       
hippo_dbus_notify_entity_changed(HippoDBus               *dbus,
                                 HippoEntity             *entity)
{
    DBusMessage *signal;
    signal = hippo_dbus_mugshot_signal_entity_changed(dbus, entity);
    dbus_connection_send(dbus->connection, signal, NULL);
    dbus_message_unref(signal);
}

void
hippo_dbus_notify_pref_changed(HippoDBus   *dbus,
                               const char  *key,
                               gboolean     value)
{
    DBusMessage *signal;
    signal = hippo_dbus_mugshot_signal_pref_changed(dbus, key, value);
    dbus_connection_send(dbus->connection, signal, NULL);
    dbus_message_unref(signal);	
}
       
void       
hippo_dbus_notify_external_iq_return (HippoDBus            *dbus,
                                      guint                 id,
                                      const char           *content)
{
    DBusMessage *signal;
    signal = hippo_dbus_mugshot_signal_external_iq_return(dbus, id, content);
    dbus_connection_send(dbus->connection, signal, NULL);
    dbus_message_unref(signal);    
}
