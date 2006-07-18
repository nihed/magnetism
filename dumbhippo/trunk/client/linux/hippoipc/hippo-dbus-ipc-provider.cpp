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

    virtual HippoIpcId connect();
    virtual void disconnect(HippoIpcId source);

    virtual void joinChatRoom(HippoIpcId source, const char *chatId, bool participant);
    virtual void leaveChatRoom(HippoIpcId source, const char *chatId);
    
    virtual void sendChatMessage(const char *chatId, const char *text);
    virtual void showChatWindow(const char *chatId);

private:
    DBusMessage *createMethodMessage(const char *name);
    DBusHandlerResult handleMethod(DBusMessage *message);
    DBusHandlerResult handleSignal(DBusMessage *message);
    
    static DBusHandlerResult handleMessageCallback(DBusConnection *connection,
						   DBusMessage    *message,
						   void           *user_data);

    DBusConnection *connection_;
    char *serverName_;
    char *busName_;
    HippoIpcListener *listener_;
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
    listener_ = NULL;

    DBusGConnection *gconnection = dbus_g_bus_get(DBUS_BUS_SESSION, NULL);
    if (gconnection == NULL) {
	g_printerr("Can't get session bus connection");
	exit(1);
    }
    
    connection_ = dbus_g_connection_get_connection(gconnection);
    if (!dbus_connection_get_is_connected(connection_)) {
	g_printerr("No connection to the session's message bus");
	exit(1);
    }

    if (!dbus_connection_add_filter(connection_, handleMessageCallback,
                                    (void *)this, NULL)) {
        g_printerr("no memory adding dbus connection filter");
	exit(1);
    }
}

HippoDBusIpcProviderImpl::~HippoDBusIpcProviderImpl()
{
    g_free(busName_);
    g_free(serverName_);
}

void
HippoDBusIpcProviderImpl::setListener(HippoIpcListener *listener)
{
    listener_ = listener;
}

DBusMessage *
HippoDBusIpcProviderImpl::createMethodMessage(const char *name)
{
    DBusMessage *message = dbus_message_new_method_call(busName_,
							HIPPO_DBUS_PATH,
							HIPPO_DBUS_INTERFACE,
							name);
    if (message == NULL)
        g_error("out of memory");
        
    /* we don't want to start a client if none is already there */
    dbus_message_set_auto_start(message, FALSE);

    return message;
}

HippoIpcId
HippoDBusIpcProviderImpl::connect()
{
    DBusError derror;

    DBusMessage *message = createMethodMessage("Connect");

    dbus_error_init(&derror);
    DBusMessage *reply = dbus_connection_send_with_reply_and_block(connection_, message, -1,
                                                      &derror);

    guint64 sourceId = 0;

    if (!reply) {
	g_warning("Can't send connect() message: %s\n", derror.message);
	dbus_error_free(&derror);
    }
    
    if (reply &&
	!dbus_message_get_args(reply, &derror,
			       DBUS_TYPE_UINT64, &sourceId,
	                       DBUS_TYPE_INVALID)) {
	g_warning("connect() message didn't return a source ID: %s\n", derror.message);
	dbus_error_free(&derror);
    }

    dbus_message_unref(message);
    if (reply)
	dbus_message_unref(reply);

    return sourceId;
}

void
HippoDBusIpcProviderImpl::disconnect(HippoIpcId source)
{
    DBusMessage *message = createMethodMessage("Disconnect");
    
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &source,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}

void
HippoDBusIpcProviderImpl::joinChatRoom(HippoIpcId source, const char *chatId, bool participant)
{
    DBusMessage *message = createMethodMessage("JoinChatRoom");

    dbus_bool_t participantArg = participant;
    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &source,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_BOOLEAN, &participantArg,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}

void
HippoDBusIpcProviderImpl::leaveChatRoom(HippoIpcId source, const char *chatId)
{
    DBusMessage *message = createMethodMessage("LeaveChatRoom");

    dbus_message_append_args(message,
			     DBUS_TYPE_UINT64, &source,
			     DBUS_TYPE_STRING, &chatId,
			     DBUS_TYPE_INVALID);
    
    dbus_connection_send(connection_, message, NULL);
    dbus_message_unref(message);
}
    
void
HippoDBusIpcProviderImpl::sendChatMessage(const char *chatId, const char *text)
{
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
	dbus_uint64_t sourceId;
	const char *chatId;
	const char *userId;
	
	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &sourceId,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->onUserJoin(sourceId, chatId, userId);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected userJoin(uint64 sourceId, string chatId, string userId)"));
	}
	
    } else if (strcmp(member, "UserLeave") == 0) {
	dbus_uint64_t sourceId;
	const char *chatId;
	const char *userId;

	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &sourceId,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_INVALID)) {
	if (listener_)
	    listener_->onUserLeave(sourceId, chatId, userId);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected userLeave(uint64 sourceId, string chatId, string userId)"));
	}

    } else if (strcmp(member, "Message") == 0) {
	dbus_uint64_t sourceId;
	const char *chatId;
	const char *userId;
	const char *text;
	double timestamp;
	int serial;

	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &sourceId,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_STRING, &text,
				  DBUS_TYPE_DOUBLE, &timestamp,
				  DBUS_TYPE_INT32, &serial,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->onMessage(sourceId, chatId, userId, text, timestamp, serial);
	} else {
	    reply = dbus_message_new_error(message,
					  DBUS_ERROR_INVALID_ARGS,
					  _("Expected Messsage(uint64 sourceId, string chatId, string userId, string text, double timestamp, int32 serial)"));
	}

    } else if (strcmp(member, "Reconnect") == 0) {
	dbus_uint64_t sourceId;
	const char *chatId;

	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &sourceId,
				  DBUS_TYPE_STRING, &chatId,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->onReconnect(sourceId, chatId);
	} else {
	    reply = dbus_message_new_error(message,
					  DBUS_ERROR_INVALID_ARGS,
					  _("Expected Reconnect(uint64 sourceId, string chatId)"));
	}
	
    } else if (strcmp(member, "UserInfo") == 0) {
	dbus_uint64_t sourceId;
	const char *userId;
	const char *name;
	const char *smallPhotoUrl;
	const char *currentSong;
	const char *currentArtist;
	dbus_bool_t musicPlaying;
	
	if (dbus_message_get_args(message, NULL,
				  DBUS_TYPE_UINT64, &sourceId,
				  DBUS_TYPE_STRING, &userId,
				  DBUS_TYPE_STRING, &name,
				  DBUS_TYPE_STRING, &smallPhotoUrl,
				  DBUS_TYPE_STRING, &currentSong,
				  DBUS_TYPE_STRING, &currentArtist,
				  DBUS_TYPE_BOOLEAN, &musicPlaying,
				  DBUS_TYPE_INVALID)) {
	    if (listener_)
		listener_->userInfo(sourceId, userId, name, smallPhotoUrl,
				    currentSong, currentArtist, musicPlaying);
	} else {
	    reply = dbus_message_new_error(message,
					   DBUS_ERROR_INVALID_ARGS,
					   _("Expected UserInfo(uint64 sourceId, string userId, string name, string smallPhotoUrl, string currentSong, boolean musicPlaying)"));
	}

    } else {
	reply = dbus_message_new_error(message,
				       DBUS_ERROR_UNKNOWN_METHOD,
				       _("Unknown callback method"));
    }

    if (!reply)
	reply =dbus_message_new_method_return(message); // empty reply

    dbus_connection_send(connection_, reply, NULL);
    dbus_message_unref(reply);

    return DBUS_HANDLER_RESULT_HANDLED;
}

DBusHandlerResult
HippoDBusIpcProviderImpl::handleSignal(DBusMessage *message)
{
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
    } else {
	return DBUS_HANDLER_RESULT_NOT_YET_HANDLED;
    }
}
