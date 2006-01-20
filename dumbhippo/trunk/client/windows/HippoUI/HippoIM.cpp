/* HippoIM.cpp: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoChatRoom.h"
#include "HippoIM.h"
#include "HippoUI.h"
#include "HippoMySpace.h"

static const int SIGN_IN_INITIAL_TIMEOUT = 5000; /* 5 seconds */
static const int SIGN_IN_INITIAL_COUNT = 60;     /* 5 minutes */
static const int SIGN_IN_SUBSEQUENT_TIMEOUT = 30000; /* 30 seconds */

static int KEEP_ALIVE_RATE = 60;        /* 1 minute; 0 disables */

static const int RETRY_TIMEOUT = 60000; /* 1 minute */

class OutgoingMessage
{
public:
    OutgoingMessage(LmMessage *message, LmMessageHandler *handler) 
        : message_(message), handler_(handler) {
        if (message_ != 0)
            lm_message_ref(message_);
        if (handler_ != 0)
            lm_message_handler_ref(handler_);
    }

    ~OutgoingMessage() {
        if (message_ != 0)
            lm_message_unref(message_);
        if (handler_ != 0)
            lm_message_handler_unref(handler_);
    }

    LmMessage* getMessage() {
        return message_;
    }

    LmMessageHandler* getMessageHandler() {
        return handler_;
    }

private:
    LmMessage *message_;
    LmMessageHandler *handler_;
};

HippoIM::HippoIM()
{
    signInTimeoutID_ = 0;
    signInTimeoutCount_ = 0;
    retryTimeoutID_ = 0;
    state_ = SIGNED_OUT;
    lmConnection_ = NULL;
    ui_ = NULL;
    // queue of OutgoingMessage
    pending_messages_ = g_queue_new();
}

static void
delete_outgoing_message(void *p, void *user_data)
{
    OutgoingMessage *m = static_cast<OutgoingMessage*>(p);
    delete m;
}

HippoIM::~HippoIM()
{
    for (unsigned long i = 0; i < chatRooms_.length(); i++)
        delete chatRooms_[i];

    stopSignInTimeout();
    stopRetryTimeout();

    g_queue_foreach(pending_messages_, delete_outgoing_message, 0);
    g_queue_free(pending_messages_);
}

void
HippoIM::setUI(HippoUI *ui)
{
    ui_ = ui;
}

bool
HippoIM::signIn()
{
    ui_->getPreferences()->setSignIn(true);

    stopSignInTimeout();
    
    if (loadAuth()) {
        if (state_ == AUTH_WAIT)
            authenticate();
        else
            connect();
        return FALSE;
    } else {
        if (state_ != SIGN_IN_WAIT && state_ != AUTH_WAIT) {
            stateChange(SIGN_IN_WAIT);
            startSignInTimeout();
        }
        return TRUE;
    }
}

void
HippoIM::signOut()
{
    ui_->getPreferences()->setSignIn(false);

    stateChange(SIGNED_OUT);

    disconnect();
}

HippoIM::State
HippoIM::getState()
{
    return state_;
}

HRESULT
HippoIM::getUsername(BSTR *ret)
{
    return username_.CopyTo(ret);
}


HippoChatRoom *
HippoIM::joinChatRoom(BSTR postId)
{
    HippoChatRoom *chatRoom = new HippoChatRoom(this, postId);
    chatRooms_.append(chatRoom);

    if (state_ == AUTHENTICATED)
        sendChatRoomEnter(chatRoom);

    return chatRoom;
}

void 
HippoIM::leaveChatRoom(HippoChatRoom *chatRoom)
{
    for (unsigned long i = 0; i < chatRooms_.length(); i++) {
        if (chatRooms_[i] == chatRoom) {
            if (state_ == AUTHENTICATED)
                sendChatRoomLeave(chatRoom);

            chatRooms_.remove(i);
            delete chatRoom;

            return;
        }
    }

    assert(false);
}

void
HippoIM::sendChatRoomMessage(HippoChatRoom *chatRoom,
                             BSTR           text)
{
    char *postId = idToJabber(chatRoom->getPostId());
    char *to = g_strconcat(postId, "@rooms.dumbhippo.com", NULL);
    LmMessage *message = lm_message_new(to, LM_MESSAGE_TYPE_MESSAGE);

    char *textU = g_utf16_to_utf8(text, -1, NULL, NULL, NULL);
    LmMessageNode *body = lm_message_node_add_child(message->node, "body", textU);
    g_free (textU);

    sendMessage(message);
    lm_message_unref(message);

    g_free(postId);
    g_free(to);
}

void 
HippoIM::stateChange(State state)
{
    state_ = state;
    ui_->debugLogW(L"IM connection state changed to %d", (int) state);

    flushPending(); // this could result in re-entrancy I think...

    ui_->onConnectionChange(state_ == AUTHENTICATED);
}

bool
HippoIM::hasAuth()
{
    if (username_ && password_)
        return true;
    else
        return loadAuth();
}

void 
HippoIM::notifyPostClickedU(const char *postGuid)
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *method = lm_message_node_add_child (node, "method", NULL);
    lm_message_node_set_attribute(method, "xmlns", "http://dumbhippo.com/protocol/servermethod");
    lm_message_node_set_attribute(method, "name", "postClicked");
    LmMessageNode *guidArg = lm_message_node_add_child (method, "arg", NULL);
    lm_message_node_set_value (guidArg, postGuid);

    sendMessage(message);

    lm_message_unref(message);
}

static void
addPropValue(LmMessageNode *node, const char *key, const HippoBSTR &value)
{
    char *valueU = g_utf16_to_utf8(value.m_str, value.Length(), NULL, NULL, NULL);
    if (valueU == 0) {
        hippoDebugLogU("Failed to convert property %s to UTF8", key);
        return;
    }
    LmMessageNode *propNode = lm_message_node_add_child(node, "prop", NULL);
    lm_message_node_set_attribute(propNode, "key", key);
    lm_message_node_set_value(propNode, valueU);
    g_free(valueU);
}

void
HippoIM::notifyMusicTrackChanged(bool haveTrack, const HippoTrackInfo & track)
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "musicChanged");

#define ADD_PROP(lower, upper) \
    if (track.has ## upper ()) addPropValue(music, #lower, track.get ## upper ())

    if (haveTrack) {
        ADD_PROP(type, Type);
        ADD_PROP(format, Format);
        ADD_PROP(name, Name);
        ADD_PROP(artist, Artist);
        ADD_PROP(album, Album);
        ADD_PROP(url, Url);
        ADD_PROP(duration, Duration);
        ADD_PROP(fileSize, FileSize);
        ADD_PROP(trackNumber, TrackNumber);
        ADD_PROP(discIdentifier, DiscIdentifier);
    }

    sendMessage(message);

    lm_message_unref(message);
    hippoDebugLogW(L"Sent music changed xmpp message");
}

void 
HippoIM::addMySpaceComment(const HippoMySpaceBlogComment &comment)
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *subnode = lm_message_node_add_child (node, "addBlogComment", NULL);
    lm_message_node_set_attribute(subnode, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(subnode, "type", "addBlogComment");
    LmMessageNode *prop = lm_message_node_add_child(subnode, "commentId", NULL);
    char *commentIdStr = g_strdup_printf("%d", comment.commentId);
    lm_message_node_set_value(prop, commentIdStr);
    prop = lm_message_node_add_child(subnode, "posterId", NULL);
    g_free(commentIdStr);
    char *posterIdStr = g_strdup_printf("%d", comment.posterId);
    lm_message_node_set_value(prop, posterIdStr);

    sendMessage(message);
    lm_message_unref(message);
    hippoDebugLogW(L"Sent MySpace comment xmpp message");
}

HRESULT
HippoIM::getAuthURL(BSTR *result)
{
    HippoBSTR url(L"http://");
    HippoBSTR webServer;
    HRESULT hr;

    hr = ui_->getPreferences()->getWebServer(&webServer);
    if (FAILED(hr))
        return hr;
    hr = url.Append(webServer);
    if (FAILED(hr))
        return hr;
    hr = url.Append(L"/jsf/");
    if (FAILED(hr))
        return hr;

    return url.CopyTo(result);
}

void
HippoIM::forgetAuth()
{
    HippoBSTR url;

    if (FAILED(getAuthURL(&url)))
        return;

    InternetSetCookie(url, NULL,  L"auth=; Path=/");
    username_ = NULL;
    password_ = NULL;
}

static bool
startsWith(WCHAR *str, WCHAR *prefix)
{
    size_t prefixlen = wcslen(prefix);
    return wcsncmp(str, prefix, prefixlen) == 0;
}

static void
copySubstring(WCHAR *str, WCHAR *end, BSTR *to) 
{
    unsigned int length = (unsigned int)(end - str);
    HippoBSTR tmp(length, str);
    tmp.CopyTo(to);
}

bool
HippoIM::loadAuth()
{
    WCHAR staticBuffer[1024];
    WCHAR *allocBuffer = NULL;
    WCHAR *cookieBuffer = staticBuffer;
    DWORD cookieSize = sizeof(staticBuffer) / sizeof(staticBuffer[0]);
    char *cookie = NULL;
    HippoBSTR url;

    username_ = NULL;
    password_ = NULL;

    // We look for an auth key for the particular server being used; otherwise,
    // we might get a cookie for dumbhippo.com when we are trying to log
    // into dogfood.dumbhippo.com.
    HippoBSTR matchHost;
    unsigned int matchPort; // unused
    ui_->getPreferences()->parseWebServer(&matchHost, &matchPort);

    if (FAILED(getAuthURL(&url)))
        goto out;

retry:
    if (!InternetGetCookieEx(url, 
                             L"auth",
                             cookieBuffer, &cookieSize,
                             0,
                             NULL))
    {
        if (GetLastError() == ERROR_INSUFFICIENT_BUFFER) {
            cookieBuffer = allocBuffer = new WCHAR[cookieSize];
            if (!cookieBuffer)
                goto out;
            goto retry;
        }
    }

    WCHAR *p = cookieBuffer;
    WCHAR *nextCookie = NULL;
    for (WCHAR *p = cookieBuffer; p < cookieBuffer + cookieSize; p = nextCookie + 1) {
        HippoBSTR host;
        HippoBSTR username;
        HippoBSTR password;

        nextCookie = wcschr(p, ';');
        if (!nextCookie)
            nextCookie = cookieBuffer + cookieSize;

        while (*p == ' ' || *p == '\t') // Skip whitespace after ;
            p++;

        if (!startsWith(p, L"auth="))
            continue;

        p += 5; // Skip 'auth='

        WCHAR *nextKey = NULL;
        for (; p < nextCookie; p = nextKey + 1) {
            nextKey = wcschr(p, '&');
            if (!nextKey)
                nextKey = nextCookie;

            if (startsWith(p, L"host="))
                copySubstring(p + 5, nextKey, &host);
            else if (startsWith(p, L"name="))
                copySubstring(p + 5, nextKey, &username);
            else if (startsWith(p, L"password="))
                copySubstring(p + 9, nextKey, &password);
        }

        // Old (pre-Jan 2005) cookies may not have a host
        if (host.m_str == 0 || wcscmp(host.m_str, matchHost.m_str) == 0) {
            username_ = username;
            password_ = password;
            if (host != NULL)
                break;
        }
    }

out:
    delete[] allocBuffer;

    ui_->debugLogW(L"authentication information: u=\"%s\" p=\"%s\"", username_.m_str ? username_.m_str : L"(null)",
                   password_.m_str ? password_.m_str : L"(null)");
    return (username_ && password_);
}

void
HippoIM::connect()
{
    HippoBSTR messageServer;
    char *messageServerU;
    unsigned int port;

    ui_->getPreferences()->parseMessageServer(&messageServer, &port);
    messageServerU = g_utf16_to_utf8(messageServer.m_str, -1, NULL, NULL, NULL);
    
    if (lmConnection_) {
        hippoDebug(L"connect() called when there is an existing connection");
        return;
    }

    lmConnection_ = lm_connection_new(messageServerU);
    lm_connection_set_port(lmConnection_, port);
    lm_connection_set_keep_alive_rate(lmConnection_, KEEP_ALIVE_RATE);

    ui_->debugLogU("Connecting to %s:%d", messageServerU, port);

    LmMessageHandler *handler = lm_message_handler_new(onMessage, (gpointer)this, NULL);
    lm_connection_register_message_handler(lmConnection_, handler, 
                                           LM_MESSAGE_TYPE_MESSAGE, 
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    handler = lm_message_handler_new(onPresence, (gpointer)this, NULL);
    lm_connection_register_message_handler(lmConnection_, handler, 
                                           LM_MESSAGE_TYPE_PRESENCE, 
                                           LM_HANDLER_PRIORITY_NORMAL);
    lm_message_handler_unref(handler);

    lm_connection_set_disconnect_function(lmConnection_, onDisconnect, (gpointer)this, NULL);

    stateChange(CONNECTING);
    GError *error = NULL;

    /* If lm_connection returns false, then onConnectionOpen won't be called
     * at all. On a true return it will be called exactly once, but that 
     * call might occur before or after lm_connection_open() returns, and
     * may occur for success or for failure.
     */
    if (!lm_connection_open(lmConnection_, 
                            onConnectionOpen, (gpointer)this, NULL, 
                            &error)) 
    {
        connectFailure(error ? error->message : "");
        if (error)
            g_error_free(error);
    }

    g_free(messageServerU);
}

void
HippoIM::disconnect()
{
    if (lmConnection_) {
        // This normally calls our disconnect handler which clears 
        // and unrefs lmConnection_
        lm_connection_close(lmConnection_, NULL);
        
        // To be safe
        if (lmConnection_) {
            lm_connection_unref(lmConnection_);
            lmConnection_ = NULL;
        }
    }
}

void
HippoIM::authenticate()
{
    if (username_ && password_) {
        char *usernameUTF = idToJabber(username_);
        char *passwordUTF = g_utf16_to_utf8(password_, -1, NULL, NULL, NULL);

        GError *error = NULL;

        // Create an XMPP resource identifier based on this machine's hardware
        // profile GUID.
        HW_PROFILE_INFO hwProfile;
        if (!GetCurrentHwProfile(&hwProfile)) {
            hippoDebugLogW(L"Failed to get hardware profile!");
            return;
        }
        gchar *guidUTF = g_utf16_to_utf8 (hwProfile.szHwProfileGuid, -1, NULL, NULL, NULL);

        if (!lm_connection_authenticate(lmConnection_, 
                                        usernameUTF, passwordUTF, guidUTF,
                                        onConnectionAuthenticate, (gpointer)this, NULL, &error)) 
        {
            authFailure(error ? error->message : NULL);
            if (error)
                g_error_free(error);
        } else {
            stateChange(AUTHENTICATING);
        }
        g_free(usernameUTF);
        g_free(guidUTF);
        g_free(passwordUTF);
    } else {
        authFailure("Not signed in");
    }
}

void
HippoIM::sendMessage(LmMessage *message)
{
    sendMessage(message, 0);
}

void
HippoIM::sendMessage(LmMessage *message, LmMessageHandler *handler)
{
    g_queue_push_tail(pending_messages_, new OutgoingMessage(message, handler));

    flushPending();
}

void
HippoIM::flushPending()
{
    while (state_ == AUTHENTICATED && pending_messages_->length > 0) {
        OutgoingMessage *om = static_cast<OutgoingMessage*>(g_queue_pop_head(pending_messages_));
        LmMessage *message = om->getMessage();
        LmMessageHandler *handler = om->getMessageHandler();
        GError *error = 0;
        if (handler != 0)
            lm_connection_send_with_reply(lmConnection_, message, handler, &error);
        else
            lm_connection_send(lmConnection_, message, &error);
        if (error) {
            ui_->debugLogU("Failed sending message: %s", error->message);
            g_error_free(error);
        }
        delete om;
    }
}

void
HippoIM::getClientInfo()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "clientInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/clientinfo");
    lm_message_node_set_attribute(child, "platform", "windows");
    LmMessageHandler *handler = lm_message_handler_new(onClientInfoReply, this, NULL);

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);

    ui_->debugLogU("Sent request for clientInfo");
}

void
HippoIM::getMySpaceName()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getName");
    LmMessageHandler *handler = lm_message_handler_new(onGetMySpaceNameReply, this, NULL);

    GError *error = NULL;
    lm_connection_send_with_reply(lmConnection_, message, handler, &error);
    if (error) {
        ui_->debugLogU("Failed sending clientInfo IQ: %s", error->message);
        g_error_free(error);
    }
    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for MySpace name");
}

void
HippoIM::getMySpaceSeenBlogComments()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getBlogComments");
    LmMessageHandler *handler = lm_message_handler_new(onGetMySpaceBlogCommentsReply, this, NULL);

    GError *error = NULL;
    lm_connection_send_with_reply(lmConnection_, message, handler, &error);
    if (error) {
        ui_->debugLogU("Failed sending clientInfo IQ: %s", error->message);
        g_error_free(error);
    }
    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for MySpace blog comments");
}

void 
HippoIM::startSignInTimeout()
{
    if (!signInTimeoutID_) {
        signInTimeoutID_ = g_timeout_add(SIGN_IN_INITIAL_TIMEOUT, 
                                         onSignInTimeout, (gpointer)this);
        signInTimeoutCount_ = 0;
    }
}

void 
HippoIM::stopSignInTimeout()
{
    if (signInTimeoutID_) {
        g_source_remove (signInTimeoutID_);
        signInTimeoutID_ = 0;
        signInTimeoutCount_ = 0;
    }
}

void 
HippoIM::startRetryTimeout()
{
    if (!retryTimeoutID_)
        retryTimeoutID_ = g_timeout_add(RETRY_TIMEOUT, 
                                        onRetryTimeout, (gpointer)this);
}

void 
HippoIM::stopRetryTimeout()
{
    if (retryTimeoutID_) {
        g_source_remove (retryTimeoutID_);
        retryTimeoutID_ = 0;
    }
}

void
HippoIM::clearConnection()
{  
    lm_connection_unref(lmConnection_);
    lmConnection_ = NULL;
}

void 
HippoIM::sendChatRoomPresence(HippoChatRoom *chatRoom, LmMessageSubType subType)
{
    char *postId = idToJabber(chatRoom->getPostId());
    char *to = g_strconcat(postId, "@rooms.dumbhippo.com", NULL);
    LmMessage *message = lm_message_new_with_sub_type(to, LM_MESSAGE_TYPE_PRESENCE, subType);

    GError *error = NULL;
    lm_connection_send(lmConnection_, message, &error);
    if (error) {
        hippoDebugLogU("Error sending chat room presence %s: %s", 
                       subType == LM_MESSAGE_SUB_TYPE_AVAILABLE ? "available" : "unavailable",
                       error->message);
        g_error_free(error);
    }

    lm_message_unref(message);

    g_free(postId);
    g_free(to);
}

void 
HippoIM::sendChatRoomEnter(HippoChatRoom *chatRoom)
{
    sendChatRoomPresence(chatRoom, LM_MESSAGE_SUB_TYPE_AVAILABLE);
}

void 
HippoIM::sendChatRoomLeave(HippoChatRoom *chatRoom)
{
    sendChatRoomPresence(chatRoom, LM_MESSAGE_SUB_TYPE_UNAVAILABLE);
}

bool
HippoIM::checkRoomMessage (LmMessage      *message,
                           HippoChatRoom **chatRoom,
                           BSTR           *userId)
{
    const char *from = lm_message_node_get_attribute(message->node, "from");

    HippoBSTR tmpPostId;
    HippoBSTR tmpUserId;

    if (!from || !parseRoomJid(from, &tmpPostId, &tmpUserId))
        return false;

    *chatRoom = NULL;
    for (unsigned long i = 0; i < chatRooms_.length(); i++) {
        if (wcscmp(chatRooms_[i]->getPostId(), tmpPostId) == 0) {
            if (FAILED(tmpUserId.CopyTo(userId)))
                return false;

            *chatRoom = chatRooms_[i];
            return true;
        }
    }

    hippoDebugLogW(L"Ignoring message from unknown room: %ls", tmpPostId.m_str);
    return false;
}

bool
HippoIM::getChatUserInfo(LmMessageNode *parent,
                         int           *version,
                         BSTR          *name)
{
    LmMessageNode *infoNode = findChildNode(parent, "http://dumbhippo.com/protocol/rooms", "userInfo");
    if (!infoNode) {
        hippoDebugLogW(L"Can't find userInfo node");
        return false;
    }

    const char *versionU = lm_message_node_get_attribute(infoNode, "version");
    const char *nameU = lm_message_node_get_attribute(infoNode, "name");

    if (!versionU || !nameU) {
        hippoDebugLogW(L"uesrInfo node without name and version");
        return false;
    }

    *version = atoi(versionU);
    HippoBSTR tmpName;
    tmpName.setUTF8(nameU);

    if (!tmpName || FAILED(tmpName.CopyTo(name)))
        return false;

    return true;
}


LmHandlerResult 
HippoIM::handleRoomMessage(LmMessage     *message,
                           HippoChatRoom *chatRoom,
                           BSTR           userId)
{
    HippoBSTR text;

    const char *textU = NULL;
    LmMessageNode *bodyNode = lm_message_node_find_child(message->node, "body");
    if (bodyNode)
        textU = lm_message_node_get_value(bodyNode);

    if (!textU) {
        hippoDebugLogW(L"Chat room message without body");
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
    }
    text.setUTF8(textU);

    int version;
    HippoBSTR name;
    if (!getChatUserInfo(message->node, &version, &name))
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

    chatRoom->addMessage(userId, version, name, text);

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

void
HippoIM::connectFailure(char *message)
{
    if (message)
        ui_->debugLogU("Disconnected: %s", message);
    else
        ui_->debugLogU("Disconnected from server");

    clearConnection();
    startRetryTimeout();
    stateChange(RETRYING);
}

void
HippoIM::authFailure(char *message)
{
    ui_->debugLogU("Failed to authenticate%s%s", 
                   message ? ": " : "",
                   message ? message : "");

    forgetAuth();
    startSignInTimeout();
    stateChange(AUTH_WAIT);
    ui_->onAuthFailure();
}

gboolean 
HippoIM::onSignInTimeout(gpointer data)
{
    HippoIM *im = (HippoIM *)data;

    if (im->loadAuth()) {
        im->stopSignInTimeout();

        if (im->state_ == AUTH_WAIT)
            im->authenticate();
        else
            im->connect();

        return FALSE;
    }

    im->signInTimeoutCount_++;
    if (im->signInTimeoutCount_ == SIGN_IN_INITIAL_COUNT) {
        // Try more slowly
        g_source_remove (im->signInTimeoutID_);
        im->signInTimeoutID_ = g_timeout_add (SIGN_IN_SUBSEQUENT_TIMEOUT, onSignInTimeout, 
                                              (gpointer)im);
        return FALSE;
    }

    return TRUE;
}

gboolean 
HippoIM::onRetryTimeout(gpointer data)
{
    HippoIM *im = (HippoIM *)data;

    im->stopRetryTimeout();

    im->connect();

    return FALSE;
}

void 
HippoIM::onConnectionOpen (LmConnection *connection,
                           gboolean      success,
                           gpointer      userData)
{
    HippoIM *im = (HippoIM *)userData;

    if (success) {
        im->ui_->debugLogU("Connected successfully");
        im->authenticate();
    } else {
        im->connectFailure(NULL);
    }
}

void 
HippoIM::onConnectionAuthenticate (LmConnection *connection,
                                   gboolean      success,
                                   gpointer      userData)
{
    HippoIM *im = (HippoIM *)userData;

    if (success) {
        im->ui_->debugLogU("Authenticated successfully");

        LmMessage *message;
        message = lm_message_new_with_sub_type(NULL, 
                                               LM_MESSAGE_TYPE_PRESENCE, 
                                               LM_MESSAGE_SUB_TYPE_AVAILABLE);

        GError *error = NULL;
        lm_connection_send(connection, message, &error);
        if (error) {
            hippoDebug(L"Failed to send presence: %s", error->message);
            g_error_free(error);
        }
        lm_message_unref(message);
        im->stateChange(AUTHENTICATED);

        // Enter any chatrooms that we are (logically) connected to
        for (unsigned long i = 0; i < im->chatRooms_.length(); i++) {
            // We left the previous contents there while we were disconnected,
            // clear it since we'll now get the current contents sent
            im->chatRooms_[i]->clear();
            im->sendChatRoomEnter(im->chatRooms_[i]);
        }

        im->getClientInfo();
        im->ui_->onAuthSuccess();
    } else {
        im->authFailure(NULL);
    }
}


void 
HippoIM::onDisconnect(LmConnection       *connection,
                      LmDisconnectReason  reason,
                      gpointer            userData)
{
    HippoIM *im = (HippoIM *)userData;

    if (im->state_ == SIGNED_OUT) {
        im->clearConnection();
    } else {
        im->connectFailure("Lost connection to server");
    }
}

bool
HippoIM::messageIsIqWithNamespace(HippoIM *im, LmMessage *message, const char *expectedNamespace, const char *documentElementName)
{
    LmMessageNode *child = message->node->children;

    const char *ns;
    if (child)
        ns = lm_message_node_get_attribute(child, "xmlns");

    if (lm_message_get_type(message) != LM_MESSAGE_TYPE_IQ ||
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_RESULT ||
        !child || child->next ||
        !ns || strcmp(ns, expectedNamespace) != 0 ||
        strcmp(child->name, documentElementName) != 0)
    {
        im->ui_->debugLogU("Got a bad reply to IQ");
        return false;
    }
    return true;
}

LmHandlerResult
HippoIM::onClientInfoReply(LmMessageHandler *handler,
                           LmConnection     *connection,
                           LmMessage        *message,
                           gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/clientinfo", "clientInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    const char *minimum = lm_message_node_get_attribute(child, "minimum");
    const char *current = lm_message_node_get_attribute(child, "current");
    const char *download = lm_message_node_get_attribute(child, "download");

    if (!minimum || !current || !download) {
        im->ui_->debugLogU("clientInfo reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    im->ui_->debugLogU("Got clientInfo response: minimum=%s, current=%s, download=%s", minimum, current, download);
    im->ui_->setClientInfo(minimum, current, download);

    // Next get the MySpace info
    im->getMySpaceName();

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}


LmHandlerResult
HippoIM::onGetMySpaceNameReply(LmMessageHandler *handler,
                               LmConnection     *connection,
                               LmMessage        *message,
                               gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    const char *name = lm_message_node_get_attribute(child, "mySpaceName");

    if (!name) {
        im->ui_->debugLogU("getMySpaceName reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    im->ui_->debugLogU("getMySpaceName response: name=%s", name);
    im->ui_->setMySpaceName(name);
    im->getMySpaceSeenBlogComments(); // Now retrieve known blog comments

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

LmHandlerResult
HippoIM::onGetMySpaceBlogCommentsReply(LmMessageHandler *handler,
                                       LmConnection     *connection,
                                       LmMessage        *message,
                                       gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    HippoArray<HippoMySpaceBlogComment> comments;

    for (LmMessageNode *subchild = child->children; subchild; subchild = subchild->next) {
        LmMessageNode *commentNode;
        HippoMySpaceBlogComment comment;
    
        if (strcmp (subchild->name, "comment") != 0)
            continue;

        commentNode = lm_message_node_get_child (subchild, "commentId");
        if (!(commentNode && commentNode->value)) {
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
        comment.commentId = strtol(commentNode->value, NULL, 10);
        commentNode = lm_message_node_get_child (subchild, "posterId");
        if (!(commentNode && commentNode->value)) {
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
        comment.posterId = strtol(commentNode->value, NULL, 10);
        im->ui_->debugLogU("getMySpaceComments: commentid=%d", comment.commentId);
        comments.append(comment);
    }

    im->ui_->setSeenMySpaceComments(comments);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}
   
LmHandlerResult 
HippoIM::onMessage (LmMessageHandler *handler,
                    LmConnection     *connection,
                    LmMessage        *message,
                    gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    HippoChatRoom *chatRoom;
    HippoBSTR userId;
    if (im->checkRoomMessage(message, &chatRoom, &userId))
        return im->handleRoomMessage(message, chatRoom, userId);

    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/linkshare", "link");
        if (child) {
            HippoLinkShare linkshare;
            LmMessageNode *node;

            const char *url = lm_message_node_get_attribute(child, "href");
            if (!url) {
                im->ui_->debugLogU("Malformed link message, no URL");
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            }
            linkshare.url.setUTF8(url);

            const char *postId = lm_message_node_get_attribute(child, "id");
            if (!postId) {
                im->ui_->debugLogU("Malformed link message, no post ID");
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            }
            linkshare.postId.setUTF8(postId);

            node = lm_message_node_get_child (child, "title");
            if (!(node && node->value))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            linkshare.title.setUTF8(node->value);

            node = lm_message_node_get_child (child, "senderName");
            if (!(node && node->value))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            linkshare.senderName.setUTF8(node->value);

            node = lm_message_node_get_child (child, "senderGuid");
            if (!(node && node->value))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            linkshare.senderId.setUTF8(node->value);

            node = lm_message_node_get_child (child, "senderPhotoUrl");
            if (!(node && node->value))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            linkshare.senderPhotoUrl.setUTF8(node->value);

            node = lm_message_node_get_child (child, "description");
            if (!(node))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            if (node->value)
                linkshare.description.setUTF8(node->value);
            else
                linkshare.description = L"";

            node = lm_message_node_get_child (child, "postInfo");
            if (!(node))
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            if (node->value)
                linkshare.info.setUTF8(node->value);
            else
                linkshare.info = L"";

            node = lm_message_node_get_child (child, "timeout");
            if (node && node->value)
                linkshare.timeout = atoi(node->value);
            else
                linkshare.timeout = 0; // Default

            node = lm_message_node_get_child (child, "recipients");
            if (!node)
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            LmMessageNode *subchild;
            for (subchild = node->children; subchild; subchild = subchild->next) {
                if (strcmp (subchild->name, "recipient") != 0)
                    continue;
                if (!subchild->value)
                    continue;
                HippoLinkRecipient recipient;
                recipient.name.setUTF8(subchild->value);
                const char *id = lm_message_node_get_attribute(subchild, "id");
                if (id)
                    recipient.id.setUTF8(id);
                linkshare.personRecipients.append(recipient);
            }
            node = lm_message_node_get_child (child, "groupRecipients");
            if (!node)
                return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
            for (subchild = node->children; subchild; subchild = subchild->next) {
                if (strcmp (subchild->name, "recipient") != 0)
                    continue;
                if (!subchild->value)
                    continue;
                HippoBSTR str;
                str.setUTF8(subchild->value);
                linkshare.groupRecipients.append(str);
            }
            node = lm_message_node_get_child (child, "viewers");
            if (node) {
                for (subchild = node->children; subchild; subchild = subchild->next) {
                    if (strcmp (subchild->name, "viewer") != 0)
                        continue;
                    if (!subchild->value)
                        continue;
                    HippoLinkRecipient recipient;
                    recipient.name.setUTF8(subchild->value);
                    const char *id = lm_message_node_get_attribute(subchild, "id");
                    if (id)
                        recipient.id.setUTF8(id);
                    linkshare.viewers.append(recipient);
                }
            }

            im->ui_->onLinkMessage(linkshare);
        } 
    }

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

LmHandlerResult 
HippoIM::onPresence (LmMessageHandler *handler,
                     LmConnection     *connection,
                     LmMessage        *message,
                     gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    const char *from = lm_message_node_get_attribute(message->node, "from");

    HippoChatRoom *chatRoom;
    HippoBSTR userId;

    if (!im->checkRoomMessage(message, &chatRoom, &userId))
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

    LmMessageSubType subType = lm_message_get_sub_type(message);

    if (subType == LM_MESSAGE_SUB_TYPE_AVAILABLE) {
        LmMessageNode *xNode = findChildNode(message->node, "http://jabber.org/protocol/muc#user", "x");
        if (!xNode) {
            hippoDebugLogW(L"Presence without x child");
            return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
        }
        
        int version;
        HippoBSTR name;
        if (!im->getChatUserInfo(xNode, &version, &name))
            return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

        chatRoom->addUser(userId, version, name);

    } else if (subType == LM_MESSAGE_SUB_TYPE_UNAVAILABLE) {
        chatRoom->removeUser(userId);
    }
    
    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

char *
HippoIM::idToJabber(WCHAR *guid)
{
    GString *str = g_string_new(NULL);
    for (WCHAR *p = guid; *p; p++) {
        WCHAR c = *p;
        // A usename in our system, is alphanumeric, with case sensitivity
        // convert to lowercase only, by using _ to mark lowercase in the
        // original.
        if (c >= 'A' && c <= 'Z') {
            g_string_append_c(str, c + ('a' - 'A'));
        } else if (c >= 'a' && c <= 'z') {
            g_string_append_c(str, (char)c);
            g_string_append_c(str, '_');
        } else if (c >= '0' && c <= '9') {
            g_string_append_c(str, (char)c);
        }
    }

    return g_string_free(str, FALSE);
}

bool
HippoIM::idFromJabber(const char *jabber, 
                      BSTR       *guid)
{
    unsigned int count = 0;
    for (const char *p = jabber; *p; p++) {
        if (*(p + 1) && *(p + 1) == '_') {
            count++;
            p++;
        }
        count++;
    }

    WCHAR *tmp = g_new(WCHAR, count + 1);
    WCHAR *out = tmp;
    for (const char *p = jabber; *p; p++) {
        WCHAR c = *p;
        if (*(p + 1) && *(p + 1) == '_') {
            if (*p >= 'A' && c <= 'Z') {
                c = c + ('a' - 'A');
            }
            p++;
        } else {
            if (*p >= 'a' && c <= 'z') {
                c = c - ('a' - 'A');
            }
        }
        *(out++) = c;
    }
    *out = '\0';

    *guid = SysAllocString(tmp);
    g_free(tmp);

    return *guid != NULL;
}

bool
HippoIM::parseRoomJid(const char *jid,
                      BSTR       *postId,
                      BSTR       *userId)
{
    *postId = NULL;
    *userId = NULL;

    char *at = strchr(jid, '@');
    if (!at)
        return false;

    char *slash = strchr(at + 1, '/');
    if (!slash)
        slash = (at + 1) + strlen(at + 1);
    if (strncmp(at + 1, "rooms.dumbhippo.com", slash - (at + 1)) != 0)
        return false;

    BSTR tmpPostId = NULL;
    BSTR tmpUserId = NULL;

    char *room = g_strndup(jid, at - jid);
    bool result = idFromJabber(room, &tmpPostId);
    g_free(room);

    if (!result)
        goto error;

    if (slash) {
        if (!idFromJabber(slash + 1, &tmpUserId))
            goto error;
    }

    *postId = tmpPostId;
    *userId = tmpUserId;
    
    return true;

error:
    if (tmpPostId)
        SysFreeString(tmpPostId);
    if (tmpUserId)
        SysFreeString(tmpPostId);

    return false;
}

LmMessageNode *
HippoIM::findChildNode(LmMessageNode *node, 
                       const char    *elementNamespace, 
                       const char    *elementName)
{
    for (LmMessageNode *child = node->children; child; child = child->next) {
        const char *ns = lm_message_node_get_attribute(child, "xmlns");
        if (!(ns && strcmp(ns, elementNamespace) == 0 && child->name))
            continue;
        if (strcmp(child->name, elementName) != 0)
            continue;

        return child;
    }

    return NULL;
}
