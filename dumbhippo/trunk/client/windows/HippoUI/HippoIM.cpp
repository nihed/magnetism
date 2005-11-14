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

static const int RETRY_TIMEOUT = 60000; /* 1 minute */

HippoIM::HippoIM()
{
    signInTimeoutID_ = 0;
    signInTimeoutCount_ = 0;
    retryTimeoutID_ = 0;
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

    disconnect();

    stateChange(SIGNED_OUT);
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
    lm_message_node_set_attribute(node, "xmlns", "http://dumbhippo.com/protocol/servermethod");

    LmMessageNode *method = lm_message_node_add_child (node, "method", NULL);
    lm_message_node_set_attribute(method, "name", "postClicked");
    LmMessageNode *guidArg = lm_message_node_add_child (method, "arg", NULL);
    lm_message_node_set_value (guidArg, postGuid);

    GError *error = NULL;
    lm_connection_send(lmConnection_, message, &error);
    if (error) {
        hippoDebug(L"Failed to send presence: %s", error->message);
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
    char *messageServer;
    unsigned int port;

    ui_->getPreferences()->parseMessageServer(&messageServer, &port);

    lmConnection_ = lm_connection_new(messageServer);
    lm_connection_set_port(lmConnection_, port);

    ui_->debugLogU("Connecting to %s:%d", messageServer, port);

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
        char *usernameUTF = g_utf16_to_utf8(username_, -1, NULL, NULL, NULL);
        char *passwordUTF = g_utf16_to_utf8(password_, -1, NULL, NULL, NULL);

        GError *error = NULL;

        if (!lm_connection_authenticate(lmConnection_, 
                                        usernameUTF, passwordUTF, "DumbHippo",
                                        onConnectionAuthenticate, (gpointer)this, NULL, &error)) 
        {
            authFailure(error ? error->message : NULL);
            if (error)
                g_error_free(error);
        } else {
            stateChange(AUTHENTICATING);
        }
    } else {
        authFailure("Not signed in");
    }
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
HippoIM::connectFailure(char *message)
{
    if (message)
        ui_->debugLogU("Disconnected: %s", message);
    else
        ui_->debugLogU("Disconnected from server");

    lm_connection_unref(lmConnection_);
    lmConnection_ = NULL;
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

    im->connectFailure("Lost connection to server");
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

                /* WARNING !  Must be in same order as XML stream */
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

                node = lm_message_node_get_child (child, "description");
                if (!(node && node->value))
                    continue;
                linkshare.description.setUTF8(node->value);

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

                im->ui_->onLinkMessage(linkshare);
            }
            else if (strcmp (child->name, "linkClicked") == 0)
            {
                HippoLinkSwarm linkswarm;

                im->ui_->debugLogU("Got link clicked");

                const char *postId = lm_message_node_get_attribute(child, "id");
                if (!postId) {
                    im->ui_->debugLogU("Malformed swarm message, no post ID");
                    continue;
                }
                linkswarm.postId.setUTF8(postId);

                const char *swarmerId = lm_message_node_get_attribute(child, "swarmerId");
                if (!swarmerId) {
                    im->ui_->debugLogU("Malformed swarm message, no swarmer ID");
                    continue;
                }
                linkswarm.swarmerId.setUTF8(swarmerId);

                LmMessageNode *clickerName = lm_message_node_get_child(child, "swarmerName");
                if (clickerName && clickerName->value)
                    linkswarm.swarmerName.setUTF8(clickerName->value);

                LmMessageNode *titleNode = lm_message_node_get_child(child, "postTitle");
                if (titleNode && titleNode->value)
                    linkswarm.postTitle.setUTF8(titleNode->value);

                im->ui_->onLinkClicked(linkswarm);
            } else {
                im->ui_->debugLogU("Unknown message \"%s\", delegating to next handler", child->name ? child->name : "(null)");
            }
        }
    }

    return LM_HANDLER_RESULT_ALLOW_MORE_HANDLERS;
}
