/* HippoIM.cpp: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoChatRoom.h"
#include "HippoIM.h"
#include "HippoUI.h"
#include "HippoUIUtil.h"
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
    musicSharingEnabled_ = false;
    musicSharingPrimed_ = false;
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

void
HippoIM::getUsername(BSTR *ret)
{
    username_.CopyTo(ret);
}

HRESULT
HippoIM::findChatRoom(BSTR postId, IHippoChatRoom **chatRoom)
{
    for (unsigned long i = 0; i < chatRooms_.length(); i++) {
        if (wcscmp(chatRooms_[i]->getPostId(), postId) == 0) {
            *chatRoom = chatRooms_[i];
            chatRooms_[i]->AddRef();

            return S_OK;
        }
    }
    return E_FAIL;
}

HRESULT
HippoIM::getChatRoom(BSTR postId, IHippoChatRoom **chatRoom)
{
    HRESULT ret = findChatRoom(postId, chatRoom);
    if (SUCCEEDED(ret))
        return ret;
    HippoChatRoom *newRoom = new HippoChatRoom(this, postId);
    if (newRoom) {
        chatRooms_.append(newRoom);
        *chatRoom = newRoom;
        return S_OK;
    } else {
        return E_OUTOFMEMORY;
    }
}

void 
HippoIM::onChatRoomStateChange(HippoChatRoom *chatRoom, HippoChatRoom::State oldState)
{
    if (state_ != AUTHENTICATED)
        return;

    if (oldState == HippoChatRoom::NONMEMBER) {
        chatRoom->clear();
        sendChatRoomEnter(chatRoom, chatRoom->getState() == HippoChatRoom::PARTICIPANT);
    } else if (chatRoom->getState() == HippoChatRoom::NONMEMBER) {
        sendChatRoomLeave(chatRoom);
    } else {
        // Change from Visitor => Participant or vice-versa
        sendChatRoomEnter(chatRoom, chatRoom->getState() == HippoChatRoom::PARTICIPANT);
    }
}

void
HippoIM::removeChatRoom(HippoChatRoom *chatRoom)
{
    for (unsigned long i = 0; i < chatRooms_.length(); i++) {
        if (chatRooms_[i] == chatRoom) {
            chatRooms_.remove(i);
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

    HippoUStr textU(text);
    LmMessageNode *body = lm_message_node_add_child(message->node, "body", textU.c_str());

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
    HippoUStr valueU(value);
    if (valueU.c_str() == 0) {
        hippoDebugLogU("Failed to convert property %s to UTF8", key);
        return;
    }
    LmMessageNode *propNode = lm_message_node_add_child(node, "prop", NULL);
    lm_message_node_set_attribute(propNode, "key", key);
    lm_message_node_set_value(propNode, valueU.c_str());
}

static void
addTrackProps(LmMessageNode *node, const HippoTrackInfo & track)
{
#define ADD_PROP(lower, upper) \
    if (track.has ## upper ()) addPropValue(node, #lower, track.get ## upper ())

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

void
HippoIM::notifyMusicTrackChanged(bool haveTrack, const HippoTrackInfo & track)
{
    if (!musicSharingEnabled_) {
        hippoDebugLogW(L"Music sharing disabled, not sending track changed notification for '%s'", track.toString().m_str);
        return;
    }

    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "musicChanged");

    if (haveTrack) {
        addTrackProps(music, track);
    }

    sendMessage(message);

    lm_message_unref(message);
    hippoDebugLogW(L"Sent music changed xmpp message for '%s'", track.toString().m_str);
}

bool
HippoIM::getNeedPrimingTracks()
{
    return musicSharingEnabled_ && !musicSharingPrimed_;
}

void
HippoIM::providePrimingTracks(HippoPlaylist *playlist)
{
    if (!musicSharingEnabled_ || musicSharingPrimed_) {
        hippoDebugLogW(L"Didn't need the priming after all");
        return;
    }

    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *music = lm_message_node_add_child (node, "music", NULL);
    lm_message_node_set_attribute(music, "xmlns", "http://dumbhippo.com/protocol/music");
    lm_message_node_set_attribute(music, "type", "primingTracks");

    for (int i = 0; i < playlist->size(); ++i) {
        LmMessageNode *track = lm_message_node_add_child(music, "track", NULL);
        addTrackProps(track, playlist->getTrack(i));
    }

    sendMessage(message);

    lm_message_unref(message);
    hippoDebugLogW(L"Sent priming tracks xmpp message");

    // we should also get back a notification from the server when this changes,
    // but we want to avoid re-priming so this adds robustness
    musicSharingPrimed_ = true;
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
    g_free(commentIdStr);
    prop = lm_message_node_add_child(subnode, "posterId", NULL);
    char *posterIdStr = g_strdup_printf("%d", comment.posterId);
    lm_message_node_set_value(prop, posterIdStr);
    g_free(posterIdStr);

    sendMessage(message);
    lm_message_unref(message);
    hippoDebugLogW(L"Sent MySpace comment xmpp message");
}

void 
HippoIM::notifyMySpaceContactPost(HippoMySpaceContact *contact)
{
   LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_SET);
    LmMessageNode *node = lm_message_get_node(message);

    LmMessageNode *subnode = lm_message_node_add_child (node, "notifyContactComment", NULL);
    lm_message_node_set_attribute(subnode, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(subnode, "type", "notifyContactComment");
    HippoUStr nameU(contact->getName());
    lm_message_node_set_attribute(subnode, "name", nameU.c_str());

    sendMessage(message);
    lm_message_unref(message);
    hippoDebugLogW(L"Sent MySpace contact post xmpp message");
}

void
HippoIM::getAuthURL(BSTR *result)
{
    HippoBSTR url(L"http://");
    HippoBSTR webServer;

    ui_->getPreferences()->getWebServer(&webServer);
    url.Append(webServer);
    url.Append(L"/jsf/");
    url.CopyTo(result);
}

void
HippoIM::forgetAuth()
{
    HippoBSTR url;

    getAuthURL(&url);

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

    try {
        getAuthURL(&url);
    } catch (...) {
        goto out;
    }

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
    unsigned int port;

    ui_->getPreferences()->parseMessageServer(&messageServer, &port);
    HippoUStr messageServerU(messageServer);
    
    if (lmConnection_) {
        hippoDebug(L"connect() called when there is an existing connection");
        return;
    }

    lmConnection_ = lm_connection_new(messageServerU.c_str());
    lm_connection_set_port(lmConnection_, port);
    lm_connection_set_keep_alive_rate(lmConnection_, KEEP_ALIVE_RATE);

    ui_->debugLogU("Connecting to %s:%d", messageServerU.c_str(), port);

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

    handler = lm_message_handler_new(onIQ, (gpointer)this, NULL);
    lm_connection_register_message_handler(lmConnection_, handler, 
                                           LM_MESSAGE_TYPE_IQ, 
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
        HippoUStr passwordUTF(password_);

        GError *error = NULL;

        // Create an XMPP resource identifier based on this machine's hardware
        // profile GUID.
        HW_PROFILE_INFO hwProfile;
        if (!GetCurrentHwProfile(&hwProfile)) {
            hippoDebugLogW(L"Failed to get hardware profile!");
            return;
        }
        HippoUStr guidUTF(hwProfile.szHwProfileGuid);

        if (!lm_connection_authenticate(lmConnection_, 
                                        usernameUTF, passwordUTF.c_str(), guidUTF.c_str(),
                                        onConnectionAuthenticate, (gpointer)this, NULL, &error)) 
        {
            authFailure(error ? error->message : NULL);
            if (error)
                g_error_free(error);
        } else {
            stateChange(AUTHENTICATING);
        }
        g_free(usernameUTF);
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
    if (pending_messages_->length > 1 && state_ == AUTHENTICATED)
        hippoDebugLogW(L"%d messages backlog to clear", pending_messages_->length);

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

    if (pending_messages_->length > 0)
        hippoDebugLogW(L"%d messages could not be sent now, since we aren't connected; deferring", pending_messages_->length);
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

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for MySpace name");
}

void
HippoIM::getHotness()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "hotness", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/hotness");
    lm_message_node_set_attribute(child, "type", "getValue");
    LmMessageHandler *handler = lm_message_handler_new(onGetHotnessReply, this, NULL);

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for hotness");
}

void
HippoIM::getRecentPosts()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "recentPosts", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/post");
    LmMessageHandler *handler = lm_message_handler_new(onGetRecentPostsReply, this, NULL);

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for recent posts");
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

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for MySpace blog comments");
}

void
HippoIM::getMySpaceContacts()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "mySpaceInfo", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/myspace");
    lm_message_node_set_attribute(child, "type", "getContacts");
    LmMessageHandler *handler = lm_message_handler_new(onGetMySpaceContactsReply, this, NULL);

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for MySpace contacts");
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
HippoIM::sendChatRoomPresence(HippoChatRoom *chatRoom, LmMessageSubType subType, bool participant)
{
    char *postId = idToJabber(chatRoom->getPostId());
    char *to = g_strconcat(postId, "@rooms.dumbhippo.com", NULL);
    LmMessage *message = lm_message_new_with_sub_type(to, LM_MESSAGE_TYPE_PRESENCE, subType);

    if (subType == LM_MESSAGE_SUB_TYPE_AVAILABLE) {
        LmMessageNode *xNode = lm_message_node_add_child(message->node, "x", NULL);
        lm_message_node_set_attribute(xNode, "xmlns", "http://jabber.org/protocol/muc");

        LmMessageNode *userInfoNode = lm_message_node_add_child(xNode, "userInfo", NULL);
        lm_message_node_set_attribute(userInfoNode, "xmlns", "http://dumbhippo.com/protocol/rooms");
        lm_message_node_set_attribute(userInfoNode, "role", participant ? "participant" : "visitor");
    }

    sendMessage(message);

    lm_message_unref(message);

    g_free(postId);
    g_free(to);
}

void 
HippoIM::sendChatRoomEnter(HippoChatRoom *chatRoom, bool participant)
{
    sendChatRoomPresence(chatRoom, LM_MESSAGE_SUB_TYPE_AVAILABLE, participant);
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
            tmpUserId.CopyTo(userId);

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
                         BSTR          *name,
                         bool          *participant,
                         BSTR          *arrangementName,
                         BSTR          *artist,
                         bool          *musicPlaying)
{
    LmMessageNode *infoNode = findChildNode(parent, "http://dumbhippo.com/protocol/rooms", "userInfo");
    if (!infoNode) {
        hippoDebugLogW(L"Can't find userInfo node");
        return false;
    }

    const char *versionU = lm_message_node_get_attribute(infoNode, "version");
    const char *nameU = lm_message_node_get_attribute(infoNode, "name");
    const char *roleU = lm_message_node_get_attribute(infoNode, "role");
    const char *arrangementNameU = lm_message_node_get_attribute(infoNode, "arrangementName");
    const char *artistU = lm_message_node_get_attribute(infoNode, "artist");
    const char *musicPlayingU = lm_message_node_get_attribute(infoNode, "musicPlaying");

    if (!versionU || !nameU) {
        hippoDebugLogW(L"userInfo node without name and version");
        return false;
    }

    *version = atoi(versionU);
    *participant = !roleU || strcmp(roleU, "participant") == 0;

    HippoBSTR tmpName;
    tmpName.setUTF8(nameU);
    if (!tmpName)
        return false;
    tmpName.CopyTo(name);

    HippoBSTR tmpArrangementName;
    tmpArrangementName.setUTF8(arrangementNameU);
    if (tmpArrangementName)
        tmpArrangementName.CopyTo(arrangementName);

    HippoBSTR tmpArtist;
    tmpArtist.setUTF8(artistU);
    if (tmpArtist)
        tmpArtist.CopyTo(artist);

    *musicPlaying = strcmp(musicPlayingU, "true") == 0;

    return true;
}

bool
HippoIM::getChatMessageInfo(LmMessageNode *parent,
                            int           *version,
                            BSTR          *name,
                            INT64         *timestamp,
                            int           *serial)
{
    LmMessageNode *infoNode = findChildNode(parent, "http://dumbhippo.com/protocol/rooms", "messageInfo");
    if (!infoNode) {
        hippoDebugLogW(L"Can't find messageInfo node");
        return false;
    }

    const char *versionU = lm_message_node_get_attribute(infoNode, "version");
    const char *nameU = lm_message_node_get_attribute(infoNode, "name");
    const char *timestampU = lm_message_node_get_attribute(infoNode, "timestamp");
    const char *serialU = lm_message_node_get_attribute(infoNode, "serial");

    if (!versionU || !nameU) {
        hippoDebugLogW(L"messageInfo node without name, version, timestamp, and serial");
        return false;
    }

    *version = atoi(versionU);
    *timestamp = _atoi64(timestampU);
    *serial = atoi(serialU);
    HippoBSTR tmpName;
    tmpName.setUTF8(nameU);

    if (!tmpName)
        return false;
    tmpName.CopyTo(name);

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
    INT64 timestamp;
    int serial;
    HippoBSTR name;
    if (!getChatMessageInfo(message->node, &version, &name, &timestamp, &serial))
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

    chatRoom->addMessage(userId, version, name, text, timestamp, serial);

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

bool 
HippoIM::checkMySpaceNameChangedMessage(LmMessage      *message,
                                        char          **nameRet)
{
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return false;
    LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/myspace", "mySpaceNameChanged");
    const char *name = lm_message_node_get_attribute(child, "name");
    if (!name)
       return false;
    *nameRet = g_strdup(name);
    return true;
}

bool 
HippoIM::checkMySpaceContactCommentMessage(LmMessage      *message)
{
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return false;
    LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/myspace", "mySpaceContactComment");
    return child != NULL;
}

void
HippoIM::handleMySpaceNameChangedMessage(char           *name)
{
    ui_->setMySpaceName(name);
    g_free(name);
}

void
HippoIM::handleMySpaceContactCommentMessage()
{
    ui_->onReceivingMySpaceContactPost();
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

        im->sendMessage(message);

        im->stateChange(AUTHENTICATED);

        // Enter any chatrooms that we are (logically) connected to
        for (unsigned long i = 0; i < im->chatRooms_.length(); i++) {
            // We left the previous contents there while we were disconnected,
            // clear it since we'll now get the current contents sent
            im->chatRooms_[i]->clear();
            if (im->chatRooms_[i]->getState() != HippoChatRoom::NONMEMBER) 
                im->sendChatRoomEnter(im->chatRooms_[i], im->chatRooms_[i]->getState() == HippoChatRoom::PARTICIPANT);
        }

        im->getClientInfo();
        im->updatePrefs();
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
HippoIM::nodeMatches(LmMessageNode *node, const char *name, const char *expectedNamespace)
{
    const char *ns = lm_message_node_get_attribute(node, "xmlns");
    if (expectedNamespace && !ns)
        return false;
    return strcmp(name, node->name) == 0 && (expectedNamespace == NULL || strcmp(expectedNamespace, ns) == 0);
}

bool
HippoIM::messageIsIqWithNamespace(HippoIM *im, LmMessage *message, const char *expectedNamespace, const char *documentElementName)
{
    LmMessageNode *child = message->node->children;

    if (lm_message_get_type(message) != LM_MESSAGE_TYPE_IQ ||
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_RESULT ||
        !child || child->next ||
        !nodeMatches(child, documentElementName, expectedNamespace))
    {
        im->ui_->debugLogU("Got a bad reply to IQ, expected ns '%s' elem '%s'",
            expectedNamespace, documentElementName);
        char *s = lm_message_node_to_string(lm_message_get_node(message));
        im->ui_->debugLogU("Node is '%s'", s);
        g_free(s);
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

    // Next get the MySpace info and current hotness
    im->getMySpaceName();
    im->getHotness();
    im->getRecentPosts();

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

    char *name = g_strdup(lm_message_node_get_attribute(child, "mySpaceName"));

    if (!name) {
        im->ui_->debugLogU("getMySpaceName reply missing attributes");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    im->ui_->debugLogU("getMySpaceName response: name=%s", name);
    im->handleMySpaceNameChangedMessage(name);

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
    HippoArray<HippoMySpaceBlogComment*> comments;

    for (LmMessageNode *subchild = child->children; subchild; subchild = subchild->next) {
        LmMessageNode *commentNode;
        HippoMySpaceBlogComment *comment = new HippoMySpaceBlogComment();
    
        if (strcmp (subchild->name, "comment") != 0)
            continue;

        commentNode = lm_message_node_get_child (subchild, "commentId");
        if (!(commentNode && commentNode->value)) {
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
        comment->commentId = strtol(commentNode->value, NULL, 10);
        commentNode = lm_message_node_get_child (subchild, "posterId");
        if (!(commentNode && commentNode->value)) {
            return LM_HANDLER_RESULT_REMOVE_MESSAGE;
        }
        comment->posterId = strtol(commentNode->value, NULL, 10);
        im->ui_->debugLogU("getMySpaceComments: commentid=%d", comment->commentId);
        comments.append(comment);
    }

    // Takes ownership of comments
    im->ui_->setSeenMySpaceComments(&comments);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}


LmHandlerResult
HippoIM::onGetMySpaceContactsReply(LmMessageHandler *handler,
                                   LmConnection     *connection,
                                   LmMessage        *message,
                                   gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    im->ui_->debugLogU("got reply for getMySpaceContacts");

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/myspace", "mySpaceInfo")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }
    HippoArray<HippoMySpaceContact *> contacts;

    for (LmMessageNode *subchild = child->children; subchild; subchild = subchild->next) {
    
        if (strcmp (subchild->name, "contact") != 0)
            continue;
        const char *name = lm_message_node_get_attribute(subchild, "name");
        if (!name)
            continue;
        const char *friendID = lm_message_node_get_attribute(subchild, "friendID");
        if (!friendID)
            continue;

        HippoBSTR contactName;
        contactName.setUTF8(name);
        HippoBSTR contactFriendId;
        contactFriendId.setUTF8(friendID);
        HippoMySpaceContact * contact = new HippoMySpaceContact(contactName, contactFriendId);
        contacts.append(contact);
        im->ui_->debugLogU("getMySpaceContacts: contact=%s", name);
    }

    im->ui_->setMySpaceContacts(contacts);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

LmHandlerResult
HippoIM::onGetHotnessReply(LmMessageHandler *handler,
                           LmConnection     *connection,
                           LmMessage        *message,
                           gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    im->ui_->debugLogU("got reply for getHotness");

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/hotness", "hotness")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    HippoBSTR hotness;
    const char *hotnessStr = lm_message_node_get_attribute(child, "value");
    if (!hotnessStr)
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    hotness.setUTF8(hotnessStr);

    im->ui_->setHotness(hotness);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

LmHandlerResult
HippoIM::onGetRecentPostsReply(LmMessageHandler *handler,
                               LmConnection     *connection,
                               LmMessage        *message,
                               gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;

    hippoDebugLogW(L"Got reply for getRecentPosts");

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/post", "recentPosts")) {
        hippoDebugLogW(L"Mismatched getRecentPosts reply");
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    HippoPost post;
    im->parsePostStream(child, "onGetRecentPostsReply", &post);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}

bool
HippoIM::handleHotnessMessage(LmMessage *message)
{
   if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/hotness", "hotness");
        if (!child)
            return false;
        HippoBSTR hotness;
        const char *hotnessStr = lm_message_node_get_attribute(child, "value");
        if (!hotnessStr)
            return false;
        hotness.setUTF8(hotnessStr);
 
        ui_->setHotness(hotness);
        return true;
   }
   return false;
}

bool
HippoIM::handleActivePostsMessage(LmMessage *message)
{
   if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE
       || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NOT_SET) {
        LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/post", "activePostsChanged");
        LmMessageNode *subchild;
        if (!child)
            return false;
        ui_->debugLogU("handling activePostsChanged message");
        ui_->clearActivePosts();
        for (subchild = child->children; subchild; subchild = subchild->next) {
            HippoPost post;
            HippoEntity entity;

            if (isEntity(subchild)) {
                if (!parseEntity(subchild, &entity)) {
                    ui_->logErrorU("failed to parse entity in activePostsChanged");
                } else {
                    ui_->addEntity(entity);
                }
            } else if (isPost(subchild)) {
                if (!parsePost(subchild, &post)) {
                    ui_->logErrorU("failed to parse post in activePostsChanged");
                }
                // The ordering is important here - we expect the post node to come first,
                // when the live post data is seen we add it
                continue;
            } else if (isLivePost(subchild)) {
                if (!parseLivePost(subchild, &post)) {
                    ui_->logErrorU("failed to parse live post in activePostsChanged");
                } else {
                    ui_->addActivePost(post);
                }
                continue;
            }
        }
        return true;
   }
   return false;
}
   
bool
HippoIM::handlePrefsChangedMessage(LmMessage *message)
{
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE)
        return false;

    LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/prefs", "prefs");
    if (child == 0)
        return false;
    ui_->debugLogU("handling prefsChanged message");

    processPrefsNode(child);

    return true;
}

bool
HippoIM::isLivePost(LmMessageNode *node)
{
    return nodeMatches(node, "livepost", NULL);
}

bool
HippoIM::parseLivePost(LmMessageNode *child, HippoPost *post)
{
    LmMessageNode *node;
    const char *attr;

    attr = lm_message_node_get_attribute (child, "id");
    if (!attr)
        return false;
    HippoBSTR postId;
    postId.setUTF8(attr);
    HippoDataCache &cache = ui_->getDataCache();

    if (!cache.getPost(postId, post))
        return false;

    node = lm_message_node_get_child (child, "recentViewers");
    if (!node)
        return false;
    LmMessageNode *subchild;
    post->viewers.clear();
    for (subchild = node->children; subchild; subchild = subchild->next) {
        HippoBSTR id;
        if (!parseEntityIdentifier(subchild, id))
            return false;
        post->viewers.push_back(id);
    }

    node = lm_message_node_get_child (child, "chattingUserCount");
    if (!(node && node->value))
        return false;
    post->chattingUserCount = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (child, "viewingUserCount");
    if (!(node && node->value))
        return false;
    post->viewingUserCount = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (child, "totalViewers");
    if (!(node && node->value))
        return false;
    post->totalViewers = strtol(node->value, NULL, 10);

    cache.addPost(*post);

    return true;
}

bool
HippoIM::handleLivePostChangedMessage(LmMessage *message)
{
    if (lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_HEADLINE
        && lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_NOT_SET)
        return false;

    LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/post", "livePostChanged");
    if (child == NULL)
        return false;   

    ui_->debugLogU("handling livePostChanged message");

    HippoPost post;
    if (!parsePostStream(child, "livePostChanged", &post))
        ui_->logErrorU("failed to parse post stream from livePostChanged");

    ui_->onLinkMessage(post, false);

    return true;
}

bool
HippoIM::isEntity(LmMessageNode *node)
{
    if (strcmp(node->name, "resource") == 0 || strcmp(node->name, "group") == 0
        || strcmp(node->name, "user") == 0)
        return true;
    return false;
}

bool
HippoIM::parseEntity(LmMessageNode *node, HippoEntity *person)
{
    if (strcmp(node->name, "resource") == 0)
        person->type = HippoEntity::EntityType::RESOURCE;
    else if (strcmp(node->name, "group") == 0)
        person->type = HippoEntity::EntityType::GROUP;
    else if (strcmp(node->name, "user") == 0)
        person->type = HippoEntity::EntityType::PERSON;
    else
        return false;

    const char *attr = lm_message_node_get_attribute(node, "id");
    if (!attr)
        return false;
    person->id.setUTF8(attr);

    if (person->type == HippoEntity::EntityType::RESOURCE)
        person->name = NULL;
    else {
        attr = lm_message_node_get_attribute(node, "name");
        if (!attr)
            return false;
        person->name.setUTF8(attr);
    }

    if (person->type == HippoEntity::EntityType::RESOURCE)
        person->smallPhotoUrl = NULL;
    else {
        attr = lm_message_node_get_attribute(node, "smallPhotoUrl");
        if (!attr)
            return false;
        person->smallPhotoUrl.setUTF8(attr);
    }
    return true;
}

bool
HippoIM::parseEntityIdentifier(LmMessageNode *node, HippoBSTR &id)
{
    const char *attr = lm_message_node_get_attribute(node, "id");
    if (!attr)
        return false;
    id.setUTF8(attr);
    return true;
}

bool
HippoIM::isPost(LmMessageNode *node)
{
    return nodeMatches(node, "post", NULL);
}

bool
HippoIM::parsePost(LmMessageNode *postNode, HippoPost *post)
{
    LmMessageNode *node;
    const char *attr;

    attr = lm_message_node_get_attribute (postNode, "id");
    if (!attr)
        return false;
    post->postId.setUTF8(attr);

    node = lm_message_node_get_child (postNode, "poster");
    if (!(node && node->value))
        return false;
    post->senderId.setUTF8(node->value);

    node = lm_message_node_get_child (postNode, "href");
    if (!(node && node->value))
        return false;
    post->url.setUTF8(node->value);

    node = lm_message_node_get_child (postNode, "title");
    if (!(node && node->value))
        return false;
    post->title.setUTF8(node->value);

    node = lm_message_node_get_child (postNode, "text");
    if (!(node && node->value))
        post->description.setUTF8("");
    else
        post->description.setUTF8(node->value);

    node = lm_message_node_get_child (postNode, "postInfo");
    if (!(node && node->value))
        post->info = NULL;
    else
        post->info.setUTF8(node->value);

    node = lm_message_node_get_child (postNode, "postDate");
    if (!(node && node->value))
        return false;
    post->postDate = strtol(node->value, NULL, 10);

    node = lm_message_node_get_child (postNode, "recipients");
    if (!node)
        return false;
    post->recipients.clear();
    LmMessageNode *subchild;
    for (subchild = node->children; subchild; subchild = subchild->next) {
        HippoBSTR id;
        if (!parseEntityIdentifier(subchild, id))
            return false;
        post->recipients.push_back(id);
    }

    ui_->getDataCache().addPost(*post);

    return true;
}

bool
HippoIM::parsePostStream(LmMessageNode *node, const char *funcName, HippoPost *post)
{
    LmMessageNode *subchild;
    for (subchild = node->children; subchild; subchild = subchild->next) {
        HippoEntity entity;
        if (isEntity(subchild)) {
            if (!parseEntity(subchild, &entity)) {
                ui_->logErrorU("failed to parse entity in %s", funcName);
                return false;
            } else {
                ui_->addEntity(entity);
            }
        } else if (isPost(subchild)) {
            if (!parsePost(subchild, post)) {
                ui_->logErrorU("failed to parse post in %s", funcName);
                return false;
            }
        } else if (isLivePost(subchild)) {
            if (!parseLivePost(subchild, post)) {
                ui_->logErrorU("failed to parse live post in %s", funcName);
                return false;
            }
        }
    }
    return true;
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

    char *mySpaceName = NULL;
    if (im->checkMySpaceNameChangedMessage(message, &mySpaceName)) {
        im->handleMySpaceNameChangedMessage(mySpaceName);
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handleHotnessMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handleActivePostsMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handleLivePostChangedMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->checkMySpaceContactCommentMessage(message)) {
        im->handleMySpaceContactCommentMessage();
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    if (im->handlePrefsChangedMessage(message)) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    // Messages used to be HEADLINE, we accept both for compatibility
    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NORMAL
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_NOT_SET // Shouldn't need this, default should be normal
        || lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        LmMessageNode *child = findChildNode(message->node, "http://dumbhippo.com/protocol/post", "newPost");
        if (child) {
            HippoPost post;
            if (im->parsePostStream(child, "newPost", &post)) {
                im->ui_->onLinkMessage(post, true);
            } else {
                im->ui_->logErrorU("failed to parse post stream in newPost");
            }
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
        bool participant;
        bool musicPlaying;
        HippoBSTR artist = NULL;
        HippoBSTR arrangementName = NULL;
        if (!im->getChatUserInfo(xNode, &version, &name, &participant, &arrangementName, &artist, &musicPlaying))
            return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

        chatRoom->addUser(userId, version, name, participant);

        if (artist && arrangementName) {
            chatRoom->updateMusicForUser(userId, arrangementName, artist, musicPlaying);
        }  
    } else if (subType == LM_MESSAGE_SUB_TYPE_UNAVAILABLE) {
        chatRoom->removeUser(userId);
    }
    
    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}

LmHandlerResult 
HippoIM::onIQ (LmMessageHandler *handler,
               LmConnection     *connection,
               LmMessage        *message,
               gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    const char *from = lm_message_node_get_attribute(message->node, "from");

    HippoChatRoom *chatRoom;
    HippoBSTR userId;

    // we only process IQ messages that apply to chat rooms for now
    if (!im->checkRoomMessage(message, &chatRoom, &userId))
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;

    LmMessageNode *musicInfoNode = lm_message_node_get_child(message->node, "music");
    if (!musicInfoNode) {
        im->ui_->logErrorU("Can't find musicInfo node");
        return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
    }

    HippoBSTR artist = NULL;
    HippoBSTR arrangementName = NULL;
    for (LmMessageNode *propNode = musicInfoNode->children; propNode; propNode = propNode->next) {   
        if (strcmp (propNode->name, "prop") != 0) {
            im->ui_->logErrorU("encountered a child node of musicInfo node that is not named \"prop\"");
            continue;
        }
        const char *key = lm_message_node_get_attribute(propNode, "key");
         
        if (key == 0) {
            im->ui_->logErrorU("ignoring node '%s' with no 'key' attribute",
                               propNode->name);
            continue;
        } else {
            if (strcmp (key, "artist") == 0) {
                artist.setUTF8(propNode->value);
            }
            if (strcmp (key, "name") == 0) {
                arrangementName.setUTF8(propNode->value);
            }
        }
        if (artist && arrangementName) {
            break;
        }
    }

    if (artist || arrangementName) {
        chatRoom->updateMusicForUser(userId, arrangementName, artist, true);
    } else {
        // an IQ message with music node, but no artist or arrangementName 
        // means that the music has stopped
        chatRoom->musicStoppedForUser(userId);
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

    char *at = strchr((char *)jid, '@');
    if (!at)
        return false;

    char *slash = strchr(at + 1, '/');
    if (!slash)
        slash = (at + 1) + strlen(at + 1);
    if (strncmp(at + 1, "rooms.dumbhippo.com", slash - (at + 1)) != 0)
        return false;

    BSTR tmpPostId = NULL;
    BSTR tmpUserId = NULL;

    char *room = g_strndup(jid, static_cast<gsize>(at - jid));
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

void
HippoIM::updatePrefs()
{
    LmMessage *message;
    message = lm_message_new_with_sub_type("admin@dumbhippo.com", LM_MESSAGE_TYPE_IQ,
                                           LM_MESSAGE_SUB_TYPE_GET);
    LmMessageNode *node = lm_message_get_node(message);
    
    LmMessageNode *child = lm_message_node_add_child (node, "prefs", NULL);
    lm_message_node_set_attribute(child, "xmlns", "http://dumbhippo.com/protocol/prefs");
    LmMessageHandler *handler = lm_message_handler_new(onPrefsReply, this, NULL);

    sendMessage(message, handler);

    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for prefs");
}

void
HippoIM::processPrefsNode(LmMessageNode *prefsNode)
{
    for (LmMessageNode *child = prefsNode->children; child != 0; child = child->next) {
        const char *key = lm_message_node_get_attribute(child, "key");
        const char *value = lm_message_node_get_value(child);

        if (key == 0) {
            ui_->debugLogU("ignoring node '%s' with no 'key' attribute in prefs reply",
                child->name);
            continue;
        }
        
        if (strcmp(key, "musicSharingEnabled") == 0) {
            musicSharingEnabled_ = value != 0 && strcmp(value, "true") == 0;
            ui_->debugLogW(L"musicSharingEnabled set to %d", (int) musicSharingEnabled_);
        } else if (strcmp(key, "musicSharingPrimed") == 0) {
            musicSharingPrimed_ = value != 0 && strcmp(value, "true") == 0;
            ui_->debugLogW(L"musicSharingPrimed set to %d", (int) musicSharingPrimed_);
        } else {
            ui_->debugLogU("Unknown pref '%s'", key);
        }
    }
    // notify the music monitor engines that they may want to kick in or out
    ui_->setMusicSharingEnabled(musicSharingEnabled_);
}

LmHandlerResult
HippoIM::onPrefsReply(LmMessageHandler *handler,
                      LmConnection     *connection,
                      LmMessage        *message,
                      gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    im->ui_->debugLogU("got reply for prefs");

    if (!messageIsIqWithNamespace(im, message, "http://dumbhippo.com/protocol/prefs", "prefs")) {
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;
    }

    LmMessageNode *prefsNode = message->node->children;

    if (prefsNode == 0 || strcmp(prefsNode->name, "prefs") != 0)
        return LM_HANDLER_RESULT_REMOVE_MESSAGE;

    im->processPrefsNode(prefsNode);

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}
