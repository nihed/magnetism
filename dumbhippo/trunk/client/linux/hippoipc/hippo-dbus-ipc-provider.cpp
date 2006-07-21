/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <string.h>
#include <stdlib.h>

#include "src/hippo-dbus-client.h"
#define DBUS_API_SUBJECT_TO_CHANGE 1
#include <dbus/dbus-glib.h>
#include <dbus/dbus-glib-lowlevel.h>

#define _(m) m

#include "hippo-dbus-ipc-provider.h"

class HippoDBusIpcProviderImpl : public HippoDBusIpcProvider {
public:
    HippoDBusIpcProviderImpl(const char *serverName);
    virtual ~HippoDBusIpcProviderImpl();
    
    virtual void setListener(HippoIpcListener *listener);

    virtual HippoEndpointId registerEndpoint();
    virtual void unregisterEndpoint(HippoEndpointId endpoint);

    virtual void joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoEndpointId endpoint, const char *chatId);
    
    virtual void sendChatMessage(const char *chatId, const char *text);
    virtual void showChatWindow(const char *chatId);
    
private:
    DBusMessage *createMethodMessage(const char *name);
    DBusHandlerResult handleMethod(DBusMessage *message);
    DBusHandlerResult handleSignal(DBusMessage *message);
    bool isIpcConnected();
    bool tryIpcConnect();
    void setBusUniqueName(const char *uniqueName);
    void forgetBusConnection();
    void notifyRegisterEndpointOpportunity();
    void notifyEndpointsInvalidated();
    
    static DBusHandlerResult handleMessageCallback(DBusConnection *connection,
						   DBusMessage    *message,
						   void           *user_data);

    DBusConnection *connection_;
    char *serverName_;
    char *busName_;
    HippoIpcListener *listener_;
    char *busUniqueName_;
    char *busNameOwnerChangedRule_;
    // current connected state of the client (is it online via xmpp)
    bool clientConnected_;
};

HippoDBusIpcProvider *
HippoDBusIpcProvider::createInstance(const char *serverName)
{
    return new HippoDBusIpcProviderImpl(serverName);
}

HippoDBusIpcProviderImpl::HippoDBusIpcProviderImpl(const char *serverName)
{
    serverName_ = g_strdup(serverName);
    busName_ = hippo_dbus_full_bus_name(serverName);
    busNameOwnerChangedRule_ =
        g_strdup_printf("type='signal',sender='%s',member='NameOwnerChanged',arg0='%s'",
                        DBUS_SERVICE_DBUS, busName_);
    listener_ = NULL;
    busUniqueName_ = NULL;
    clientConnected_ = false;
    
    /* If for some reason we can't get a bus connection we want to just go into
     * a "no-op" mode; otherwise, we'd suddenly make Firefox rely on having
     * an active dbus, while currently it does not.
     */
    
    GError *gerror = NULL;
    DBusGConnection *gconnection = dbus_g_bus_get(DBUS_BUS_SESSION, &gerror);
    if (gconnection == NULL) {
	g_printerr("Can't get session bus connection: %s", gerror->message);
        g_error_free(gerror);
        return;
    }
    
    connection_ = dbus_g_connection_get_connection(gconnection);
    if (!dbus_connection_get_is_connected(connection_)) {
        /* This should be impossible with newer dbus which sets the
         * shared bus connection to NULL when it disconnects,
         * so we should not have gotten a disconnected one.
         * But we check here for old dbus to be sure we'll get
         * a Disconnected message later.
         */
	g_warning("Connection to the session's message bus is disconnected");
        dbus_connection_unref(connection_);
        connection_ = NULL;
        return;
    }

    DBusError derror;
    dbus_error_init(&derror);
    /* it's fine if multiple instances do this, the bus refcounts it */
    dbus_bus_add_match(connection_,
                       busNameOwnerChangedRule_,
                       &derror);
    if (dbus_error_is_set(&derror)) {
        g_warning("Failed to add name owner changed rule: %s: %s: %s",
                  busNameOwnerChangedRule_,
                  derror.name,
                  derror.message);
        dbus_error_free(&derror);
        dbus_connection_unref(connection_);
        connection_ = NULL;
        return;
    }
    
    if (!dbus_connection_add_filter(connection_, handleMessageCallback,
                                    (void *)this, NULL)) {
        g_error("no memory adding dbus connection filter");
    }

    g_debug("Connected to session bus");
    
    /* this may fail, in which case when we get the NameOwnerChanged indicating a new
     * client started up, we'll retry.
     */
    tryIpcConnect();
}

HippoDBusIpcProviderImpl::~HippoDBusIpcProviderImpl()
{
    forgetBusConnection();
    
    g_free(busName_);
    g_free(serverName_);
    g_free(busUniqueName_);
    g_free(busNameOwnerChangedRule_);
}

bool
HippoDBusIpcProviderImpl::isIpcConnected()
{
    return connection_ != NULL && busUniqueName_ != NULL;
}

// this should not be called from inside methods like registerEndpoint, see
// comment in front of notifyRegisterEndpointOpportunity()
bool
HippoDBusIpcProviderImpl::tryIpcConnect()
{
    if (isIpcConnected())
        return true;

    if (connection_ == NULL)
        return false;
    
    DBusMessage *message = dbus_message_new_method_call(DBUS_SERVICE_DBUS,
                                                        DBUS_PATH_DBUS,
                                                        DBUS_INTERFACE_DBUS,
                                                        "GetNameOwner");
    if (message == NULL)
        g_error("out of memory");

    if (!dbus_message_append_args(message, DBUS_TYPE_STRING,
                                  &busName_, DBUS_TYPE_INVALID))
        g_error("out of memory");

    
    DBusError derror;
    dbus_error_init(&derror);
    DBusMessage *reply = dbus_connection_send_with_reply_and_block(connection_,
                                                                   message,
                                                                   -1, &derror);
    dbus_message_unref(message);

    if (reply == NULL) {
        g_debug("Error getting owner %s: %s",
                derror.name, derror.message);
        dbus_error_free(&derror);
        return false;
    }

    const char *owner = NULL;
    if (!dbus_message_get_args(reply, NULL, DBUS_TYPE_STRING, &owner, DBUS_TYPE_INVALID)) {
        dbus_message_unref(reply);
        return false;
    }

    // on initial connect, the client app does not send us a Connect signal; instead, we
    // start out assuming it has a live xmpp connection, and don't decide otherwise until
    // we get a Disconnect or registerEndpoint fails.
    clientConnected_ = true;
    
    setBusUniqueName(owner);
    dbus_message_unref(reply);
    return true;
}

static char*
connected_rule(const char *name)
{
    return g_strdup_printf("type='signal',sender='%s',path='%s',interface='%s',member='Connected'",
                           name, HIPPO_DBUS_LISTENER_PATH, HIPPO_DBUS_LISTENER_INTERFACE);
}

static char*
disconnected_rule(const char *name)
{
    return g_strdup_printf("type='signal',sender='%s',path='%s',interface='%s',member='Disconnected'",
                           name, HIPPO_DBUS_LISTENER_PATH, HIPPO_DBUS_LISTENER_INTERFACE);
}

void
HippoDBusIpcProviderImpl::setBusUniqueName(const char *uniqueName)
{
    g_debug("unique name of client: %s", uniqueName ? uniqueName : "NULL");

    if (uniqueName == NULL && busUniqueName_ == NULL)
        return;
    if (uniqueName && busUniqueName_ && strcmp(uniqueName, busUniqueName_) == 0)
        return;
    
    if (busUniqueName_ != NULL && connection_) {
        char *connectedRule = connected_rule(busUniqueName_);
        char *disconnectedRule = disconnected_rule(busUniqueName_);

        // both of these will fail if the matched busUniqueName_ is disconnected,
        // since the bus garbage collects the match rules for nonexistent unique
        // names. we just want to ignore the failure.
        
        g_debug("removing rule %s", connectedRule);
        dbus_bus_remove_match(connection_, connectedRule, NULL);
        g_debug("removing rule %s", disconnectedRule);
        dbus_bus_remove_match(connection_, disconnectedRule, NULL);

        g_free(connectedRule);
        g_free(disconnectedRule);
    }
    
    /* note the new unique name can be NULL */
    busUniqueName_ = g_strdup(uniqueName);

    if (busUniqueName_ != NULL && connection_) {
        char *connectedRule = connected_rule(busUniqueName_);
        char *disconnectedRule = disconnected_rule(busUniqueName_);

        g_debug("adding rule %s", connectedRule);
        dbus_bus_add_match(connection_, connectedRule, NULL);
        g_debug("adding rule %s", disconnectedRule);
        dbus_bus_add_match(connection_, disconnectedRule, NULL);

        g_free(connectedRule);
        g_free(disconnectedRule);
    }
    
    if (busUniqueName_ != NULL)
        notifyRegisterEndpointOpportunity();
    else
        notifyEndpointsInvalidated();
}

void
HippoDBusIpcProviderImpl::forgetBusConnection()
{
    setBusUniqueName(NULL);
    
    if (connection_ != NULL) {
        dbus_connection_remove_filter(connection_, handleMessageCallback,
                                      (void*) this);

        /* should silently be an error if we're disconnected */
        dbus_bus_remove_match(connection_, busNameOwnerChangedRule_, NULL);
        
        /* With older dbus versions, this will cause a warning if
         * we're still connected since it could be the last reference.
         * In newer versions libdbus is holding a ref so unrefing is safe.
         * Should not crash with the older versions though.
         */
        dbus_connection_unref(connection_);
        connection_ = NULL;
        g_debug("Dropped bus connection");
    }

    // this is a pointless extra call really since setting the unique name to NULL
    // already did it too
    notifyEndpointsInvalidated();
}

// connection notification works as follows:
// - on initial provider creation, a connection attempt (registerEndpoint)
//   is assumed possible
// - if a disconnected event occurs, then all endpoints are invalidated
// - if a connected event occurs, it's a "connection opportunity" and
//   registerEndpoint may be attempted
// We need to always notify on disconnect/connection-oppty in an async way,
// i.e. not in response to errors on method call attempts, or "during"
// a method call including during registerEndpoint.
//
// A "connection opportunity" exists if all of:
// - we are connected to session bus
// - there is a mugshot client running
// - the mugshot client is connected via xmpp to the mugshot server
// so we emit onConnected when these become true and were not before.
//
// To avoid notifying inside methods, we don't process messages
// inside messages (so don't become disconnected there) and we don't
// try to connect inside messages - we only try to connect on
// provider construction, and in response to dbus events indicating
// that connection could now succeed.
void
HippoDBusIpcProviderImpl::notifyRegisterEndpointOpportunity()
{
    bool nowConnected;

    nowConnected = isIpcConnected() && clientConnected_;

    if (listener_ && nowConnected) {
        listener_->onConnect();
    }
}

void
HippoDBusIpcProviderImpl::notifyEndpointsInvalidated()
{
    bool nowConnected;

    nowConnected = isIpcConnected() && clientConnected_;

    if (listener_ && !nowConnected) {
        listener_->onDisconnect();
    }
}

void
HippoDBusIpcProviderImpl::setListener(HippoIpcListener *listener)
{
    listener_ = listener;
}

DBusMessage *
HippoDBusIpcProviderImpl::createMethodMessage(const char *name)
{
    g_assert(busUniqueName_ != NULL);
    
    DBusMessage *message = dbus_message_new_method_call(busUniqueName_,
							HIPPO_DBUS_PATH,
							HIPPO_DBUS_INTERFACE,
							name);
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there
     * (we wouldn't anyway for now since there's no .service file
     *  for the mugshot client)
     */
    dbus_message_set_auto_start(message, FALSE);

    return message;
}

HippoEndpointId
HippoDBusIpcProviderImpl::registerEndpoint()
{
    DBusError derror;

    // as mentioned several times in this file, and elaborated on above
    // notifyRegisterEndpointOpportunity()
    // _do not_ put a tryIpcConnect() call here.
    if (!isIpcConnected())
        return 0;
    
    DBusMessage *message = createMethodMessage("RegisterEndpoint");

    dbus_error_init(&derror);
    DBusMessage *reply = dbus_connection_send_with_reply_and_block(connection_, message, -1,
								   &derror);

    guint64 endpoint = 0;

    if (!reply) {
	g_debug("Error from registerEndpoint(): %s", derror.message);
	dbus_error_free(&derror);
        // the mugshot app does not send us the initial clientConnected_ value,
        // but registerEndpoint is guaranteed not to succeed unless the
        // mugshot app is connected via xmpp. This may change us to
        // disconnected but we don't want to notify here since the
        // failure of registerEndpoint indicates that already and we
        // don't want to synchronously invoke listeners.
        clientConnected_ = false;
    } else if (reply &&
	!dbus_message_get_args(reply, &derror,
			       DBUS_TYPE_UINT64, &endpoint,
	                       DBUS_TYPE_INVALID)) {
	g_warning("registerEndpoint() message didn't return a endpoint ID: %s\n", derror.message);
	dbus_error_free(&derror);
    }

    dbus_message_unref(message);
    if (reply)
	dbus_message_unref(reply);
    
    return endpoint;
}

void
HippoDBusIpcProviderImpl::unregisterEndpoint(HippoEndpointId endpoint)
{
    if (!isIpcConnected())
        return;
    
    DBusMessage *message = createMethodMessage("UnregisterEndpoint");
    
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}

void
HippoDBusIpcProviderImpl::joinChatRoom(HippoEndpointId endpoint, const char *chatId, bool participant)
{
    if (!isIpcConnected())
        return;
    
    DBusMessage *message = createMethodMessage("JoinChatRoom");

    dbus_bool_t participantArg = participant;
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_BOOLEAN, &participantArg,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}

void
HippoDBusIpcProviderImpl::leaveChatRoom(HippoEndpointId endpoint, const char *chatId)
{
    if (!isIpcConnected())
        return;
    
    DBusMessage *message = createMethodMessage("LeaveChatRoom");

    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &endpoint,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}
    
void
HippoDBusIpcProviderImpl::sendChatMessage(const char *chatId, const char *text)
{
    if (!isIpcConnected())
        return;
    
    DBusMessage *message = createMethodMessage("LeaveChatRoom");

    dbus_message_append_args(message,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_STRING, &text,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}
       
void
HippoDBusIpcProviderImpl::showChatWindow(const char *chatId)
{
    if (!isIpcConnected())
        return;

    DBusMessage *message = createMethodMessage("ShowChatWindow");

    dbus_message_append_args(message,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}

DBusHandlerResult
HippoDBusIpcProviderImpl::handleMethod(DBusMessage *message)
{    
    g_assert(connection_ != NULL);
    
    DBusMessage *reply = NULL;
    
    const char *sender = dbus_message_get_sender(message);
    const char *interface = dbus_message_get_interface(message);
    const char *member = dbus_message_get_member(message);
    const char *path = dbus_message_get_path(message);

    g_debug("method call from %s %s.%s on %s", sender ? sender : "NULL",
	    interface ? interface : "NULL",
	    member ? member : "NULL",
	    path ? path : "NULL");

    if (!path || strcmp(path, HIPPO_DBUS_LISTENER_PATH) != 0)
	return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    if (!interface || strcmp(interface, HIPPO_DBUS_LISTENER_INTERFACE) != 0)
	return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;

    if (strcmp(member, "UserJoin") == 0) {
	dbus_uint64_t endpoint;
	const char *chatId;
	const char *userId;
	
	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &endpoint,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->onUserJoin(endpoint, chatId, userId);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected userJoin(uint64 endpoint, string chatId, string userId)"));
	}
	
    } else if (strcmp(member, "UserLeave") == 0) {
	dbus_uint64_t endpoint;
	const char *chatId;
	const char *userId;

	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &endpoint,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_INVALID)) {
	if (listener_)
	    listener_->onUserLeave(endpoint, chatId, userId);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected userLeave(uint64 endpoint, string chatId, string userId)"));
	}

    } else if (strcmp(member, "Message") == 0) {
	dbus_uint64_t endpoint;
	const char *chatId;
	const char *userId;
	const char *text;
	double timestamp;
	int serial;

	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &endpoint,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_STRING, &text,
				  DBUS_TYPE_DOUBLE, &timestamp,
				  DBUS_TYPE_INT32, &serial,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->onMessage(endpoint, chatId, userId, text, timestamp, serial);
	} else {
	    reply = dbus_message_new_error(message,
					  DBUS_ERROR_INVALID_ARGS,
					  _("Expected Messsage(uint64 endpoint, string chatId, string userId, string text, double timestamp, int32 serial)"));
	}

    } else if (strcmp(member, "UserInfo") == 0) {
	dbus_uint64_t endpoint;
	const char *userId;
	const char *name;
	const char *smallPhotoUrl;
	const char *currentSong;
	const char *currentArtist;
	dbus_bool_t musicPlaying;
	
	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &endpoint,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_STRING, &name,
				  DBUS_TYPE_STRING, &smallPhotoUrl,
				  DBUS_TYPE_STRING, &currentSong,
				  DBUS_TYPE_STRING, &currentArtist,
				  DBUS_TYPE_BOOLEAN, &musicPlaying,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->userInfo(endpoint, userId, name, smallPhotoUrl,
				    currentSong, currentArtist, musicPlaying);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected UserInfo(uint64 endpoint, string userId, string name, string smallPhotoUrl, string currentSong, boolean musicPlaying)"));
	}

    } else {
	reply = dbus_message_new_error(message,
				       DBUS_ERROR_UNKNOWN_METHOD,
				       _("Unknown callback method"));
    }

    if (!reply)
	reply = dbus_message_new_method_return(message); // empty reply

    dbus_connection_send(connection_, reply, NULL);
    dbus_message_unref(reply);

    return DBUS_HANDLER_RESULT_HANDLED;
}

DBusHandlerResult
HippoDBusIpcProviderImpl::handleSignal(DBusMessage *message)
{
    g_assert(connection_ != NULL);

    if (dbus_message_has_sender(message, DBUS_SERVICE_DBUS) &&
        dbus_message_is_signal(message, DBUS_INTERFACE_DBUS, "NameOwnerChanged")) {
        const char *name = NULL;
        const char *old_owner = NULL;
        const char *new_owner = NULL;
        if (dbus_message_get_args(message, NULL,
                                  DBUS_TYPE_STRING, &name,
                                  DBUS_TYPE_STRING, &old_owner,
                                  DBUS_TYPE_STRING, &new_owner,
                                  DBUS_TYPE_INVALID)) {
            g_debug("NameOwnerChanged %s '%s' -> '%s'", name, old_owner, new_owner);
            if (*old_owner == '\0')
                old_owner = NULL;
            if (*new_owner == '\0')
                new_owner = NULL;

            if (strcmp(name, busName_) == 0) {
                // this will notify about disconnection from old
                if (busUniqueName_ &&
                    old_owner &&
                    strcmp(busUniqueName_, old_owner) == 0) {
                    setBusUniqueName(NULL);
                }

                // now notify about connection to something new
                if (new_owner) {
                    setBusUniqueName(new_owner);
                }
            }
        } else {
            g_warning("NameOwnerChanged had wrong args???");
        }
    } else if (dbus_message_is_signal(message, DBUS_INTERFACE_LOCAL, "Disconnected")) {
        forgetBusConnection();
    } else if (busUniqueName_ &&
               dbus_message_has_sender(message, busUniqueName_) &&
               dbus_message_is_signal(message, HIPPO_DBUS_LISTENER_INTERFACE, "Connected")) {
        clientConnected_ = true;
        notifyRegisterEndpointOpportunity();
    } else if (busUniqueName_ &&
               dbus_message_has_sender(message, busUniqueName_) &&
               dbus_message_is_signal(message, HIPPO_DBUS_LISTENER_INTERFACE, "Disconnected")) {
        clientConnected_ = false;
        notifyEndpointsInvalidated();
    }

    /* we never want to "HANDLED" a signal really, doesn't make sense */
    return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
}

DBusHandlerResult
HippoDBusIpcProviderImpl::handleMessageCallback(DBusConnection *connection,
						DBusMessage    *message,
						void           *user_data)
{
    HippoDBusIpcProviderImpl *provider = (HippoDBusIpcProviderImpl *)user_data;

    int type = dbus_message_get_type(message);
    
    if (type == DBUS_MESSAGE_TYPE_METHOD_CALL) {
	return provider->handleMethod(message);
    } else if (type == DBUS_MESSAGE_TYPE_SIGNAL) {
	return provider->handleSignal(message);
    } else if (type == DBUS_MESSAGE_TYPE_ERROR) {
        hippo_dbus_debug_log_error("control", message);
        return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    } else {
	return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    }
}
