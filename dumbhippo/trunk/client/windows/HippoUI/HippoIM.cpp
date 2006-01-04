/* HippoIM.cpp: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#include "stdafx.h"
#include "HippoIM.h"
#include "HippoUI.h"

static const int SIGN_IN_INITIAL_TIMEOUT = 5000; /* 5 seconds */
static const int SIGN_IN_INITIAL_COUNT = 60;     /* 5 minutes */
static const int SIGN_IN_SUBSEQUENT_TIMEOUT = 30000; /* 30 seconds */

static int KEEP_ALIVE_RATE = 60;        /* 1 minute; 0 disables */

static const int RETRY_TIMEOUT = 60000; /* 1 minute */

HippoIM::HippoIM()
{
    signInTimeoutID_ = 0;
    signInTimeoutCount_ = 0;
    retryTimeoutID_ = 0;
    state_ = SIGNED_OUT;
    lmConnection_ = NULL;
    ui_ = NULL;
}

HippoIM::~HippoIM()
{
    stopSignInTimeout();
    stopRetryTimeout();
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
HippoIM::stateChange(State state)
{
    state_ = state;
    ui_->debugLogW(L"IM connection state changed to %d", (int) state);
    ui_->onConnectionChange(state == AUTHENTICATED);
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

    GError *error = NULL;
    lm_connection_send(lmConnection_, message, &error);
    if (error) {
        hippoDebug(L"Failed to send post clicked notification: %s", error->message);
        g_error_free(error);
    }
    lm_message_unref(message);
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

    if (wcsncmp(cookieBuffer, L"auth=", 5) != 0)
        goto out;

    for (WCHAR *p = cookieBuffer + 5; *p;) {
        WCHAR *next = wcschr(p, '&');
        if (!next)
            next = p + wcslen(p);
        if (wcsncmp(p, L"name=", 5) == 0)
        {
            HippoBSTR tmp = HippoBSTR(next - (p + 5), p + 5);
            username_ = tmp;
        }
        else if (wcsncmp(p, L"password=", 9) == 0)
        {
            HippoBSTR tmp = HippoBSTR(next - (p + 9), p + 9);
            password_ = tmp;
        }

        p = next;
        if (*p) // Skip &
            p++;
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
        lm_connection_close(lmConnection_, NULL);
        lm_connection_unref(lmConnection_);
        lmConnection_ = NULL;
    }
}


void
HippoIM::authenticate()
{
    if (username_ && password_) {
        GString *usernameString = g_string_new(NULL);
        for (WCHAR *p = username_; *p; p++) {
            WCHAR c = *p;
            // A usename in our system, is alphanumeric, with case sensitivity
            // convert to lowercase only, by using _ to mark lowercase in the
            // original.
            if (c >= 'A' && c <= 'Z') {
                g_string_append_c(usernameString, c + ('a' - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                g_string_append_c(usernameString, (char)c);
                g_string_append_c(usernameString, '_');
            } else if (c >= '0' && c <= '9') {
                g_string_append_c(usernameString, (char)c);
            }
        }

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
                                        usernameString->str, passwordUTF, guidUTF,
                                        onConnectionAuthenticate, (gpointer)this, NULL, &error)) 
        {
            authFailure(error ? error->message : NULL);
            if (error)
                g_error_free(error);
        } else {
            stateChange(AUTHENTICATING);
        }
        g_string_free(usernameString, TRUE);
        g_free(guidUTF);
        g_free(passwordUTF);
    } else {
        authFailure("Not signed in");
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

    GError *error = NULL;
    lm_connection_send_with_reply(lmConnection_, message, handler, &error);
    if (error) {
        ui_->debugLogU("Failed sending clientInfo IQ: %s", error->message);
        g_error_free(error);
    }
    lm_message_unref(message);
    lm_message_handler_unref(handler);
    ui_->debugLogU("Sent request for clientInfo");
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

LmHandlerResult
HippoIM::onClientInfoReply(LmMessageHandler *handler,
                           LmConnection     *connection,
                           LmMessage        *message,
                           gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    LmMessageNode *child = message->node->children;
    const char *ns;
    if (child)
        ns = lm_message_node_get_attribute(child, "xmlns");

    if (lm_message_get_type(message) != LM_MESSAGE_TYPE_IQ ||
        lm_message_get_sub_type(message) != LM_MESSAGE_SUB_TYPE_RESULT ||
        !child || child->next ||
        !ns || strcmp(ns, "http://dumbhippo.com/protocol/clientinfo") != 0 ||
        strcmp(child->name, "clientInfo") != 0)
    {
        im->ui_->debugLogU("Got a bad reply to clientInfo IQ");
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

    return LM_HANDLER_RESULT_REMOVE_MESSAGE;
}
   
LmHandlerResult 
HippoIM::onMessage (LmMessageHandler *handler,
                    LmConnection     *connection,
                    LmMessage        *message,
                    gpointer          userData)
{
    HippoIM *im = (HippoIM *)userData;

    if (lm_message_get_sub_type(message) == LM_MESSAGE_SUB_TYPE_HEADLINE) {
        for (LmMessageNode *child = message->node->children; child; child = child->next) {
            const char *ns = lm_message_node_get_attribute(child, "xmlns");
            // We really should allow xmlns="foo:http://...", but lazy for now
            if (!(ns && strcmp(ns, "http://dumbhippo.com/protocol/linkshare") == 0 && child->name))
                continue;
       
            if (strcmp (child->name, "link") == 0)
            {
                HippoLinkShare linkshare;
                LmMessageNode *node;

                const char *url = lm_message_node_get_attribute(child, "href");
                if (!url) {
                    im->ui_->debugLogU("Malformed link message, no URL");
                    continue;
                }
                linkshare.url.setUTF8(url);

                const char *postId = lm_message_node_get_attribute(child, "id");
                if (!postId) {
                    im->ui_->debugLogU("Malformed link message, no post ID");
                    continue;
                }
                linkshare.postId.setUTF8(postId);

                node = lm_message_node_get_child (child, "title");
                if (!(node && node->value))
                    continue;
                linkshare.title.setUTF8(node->value);

                node = lm_message_node_get_child (child, "senderName");
                if (!(node && node->value))
                    continue;
                linkshare.senderName.setUTF8(node->value);

                node = lm_message_node_get_child (child, "senderGuid");
                if (!(node && node->value))
                    continue;
                linkshare.senderId.setUTF8(node->value);

                node = lm_message_node_get_child (child, "senderPhotoUrl");
                if (!(node && node->value))
                    continue;
                linkshare.senderPhotoUrl.setUTF8(node->value);

                node = lm_message_node_get_child (child, "description");
                if (!(node))
                    continue;
                if (node->value)
                    linkshare.description.setUTF8(node->value);
                else
                    linkshare.description = L"";

                node = lm_message_node_get_child (child, "postInfo");
                if (!(node))
                    continue;
                if (node->value)
                    linkshare.info.setUTF8(node->value);
                else
                    linkshare.info = L"";

                node = lm_message_node_get_child (child, "timeout");
                if (node && node->value)
                    linkshare.timeout.setUTF8(node->value);
                else
                    linkshare.timeout = L"default";

                node = lm_message_node_get_child (child, "recipients");
                if (!node)
                    continue;
                LmMessageNode *subchild;
                for (subchild = node->children; subchild; subchild = subchild->next) {
                    if (strcmp (subchild->name, "recipient") != 0)
                        continue;
                    if (!subchild->value)
                        continue;
                    HippoBSTR str;
                    str.setUTF8(subchild->value);
                    linkshare.personRecipients.append(str);
                }
                node = lm_message_node_get_child (child, "groupRecipients");
                if (!node)
                    continue;
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
                        HippoBSTR str;
                        str.setUTF8(subchild->value);
                        linkshare.viewers.append(str);
                    }
                }

                im->ui_->onLinkMessage(linkshare);
            } else {
                im->ui_->debugLogU("Unknown message \"%s\", delegating to next handler", child->name ? child->name : "(null)");
            }
        }
    }

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}
