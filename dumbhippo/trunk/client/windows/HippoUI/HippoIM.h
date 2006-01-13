/* HippoIM.h: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <HippoUtil.h>
#include "HippoMusicMonitor.h"
#include <loudmouth/loudmouth.h>

class HippoUI;
class HippoChatRoom;

class HippoIM 
{
public:
    enum State {
        SIGNED_OUT,     // User hasn't asked to connect
        SIGN_IN_WAIT,   // Waiting for the user to sign in
        CONNECTING,     // Waiting for connecting to server
        RETRYING,       // Connection to server failed, retrying
        AUTHENTICATING, // Waiting for authentication
        AUTH_WAIT,      // Authentication failed, waiting for new creds
        AUTHENTICATED   // Authenticated to server
    };

    HippoIM();
    ~HippoIM();

    // Set the main UI object. Must be called before use
    void setUI(HippoUI *ui);

    void notifyPostClickedU(const char *postGuid);

    void notifyMusicTrackChanged(bool haveTrack, const HippoTrackInfo & track);

    // Try to sign in. Returns TRUE if we need to show a web page where
    // the user can sign in
    bool signIn();

    // Sign out from the server
    void signOut();

    // Check if we have authentication information stored
    bool hasAuth();

    // Forget any remembered authentication information
    void forgetAuth();

    // Get the current state
    State getState();

    HRESULT getUsername(BSTR *ret);

    HippoChatRoom *joinChatRoom(BSTR postId);
    void leaveChatRoom(HippoChatRoom *chatRoom);
    void sendChatRoomMessage(HippoChatRoom *chatRoom, BSTR text);

private:
    HRESULT getAuthURL(BSTR *result);

    void stateChange(State state);

    bool loadAuth();
    void connect();
    void disconnect();
    void authenticate();
    void getClientInfo();

    void startSignInTimeout();
    void stopSignInTimeout();

    void startRetryTimeout();
    void stopRetryTimeout();

    void clearConnection();

    void sendChatRoomPresence(HippoChatRoom *chatRoom, LmMessageSubType subType);
    void sendChatRoomEnter(HippoChatRoom *chatRoom);
    void sendChatRoomLeave(HippoChatRoom *chatRoom);

    bool checkRoomMessage(LmMessage      *message,
                          HippoChatRoom **chatRoom,
                          BSTR           *userId);
    bool getChatUserInfo(LmMessageNode *parent,
                         int           *version,
                         BSTR          *name);
    LmHandlerResult handleRoomMessage(LmMessage     *message,
                                      HippoChatRoom *chatRoom,
                                      BSTR           userId);

    void connectFailure(char *message);
    void authFailure(char *message);

    void sendMessage(LmMessage *message);
    void sendMessage(LmMessage *message, LmMessageHandler *handler);
    void flushPending();

    static gboolean onSignInTimeout(gpointer data);
    static gboolean onRetryTimeout(gpointer data);

    static void onConnectionOpen(LmConnection *connection,
                                 gboolean      success,
                                 gpointer      userData);

    static void onConnectionAuthenticate(LmConnection *connection,
                                         gboolean      success,
                                         gpointer      userData);

    static void onDisconnect(LmConnection       *connection,
                             LmDisconnectReason  reason,
                             gpointer            userData);

    static LmHandlerResult onClientInfoReply(LmMessageHandler *handler,
                                             LmConnection     *connection,
                                             LmMessage        *message,
                                             gpointer          userData);

    static LmHandlerResult onMessage(LmMessageHandler *handler,
                                     LmConnection     *connection,
                                     LmMessage        *message,
                                     gpointer          userData);

    static LmHandlerResult onPresence(LmMessageHandler *handler,
                                      LmConnection     *connection,
                                      LmMessage        *message,
                                      gpointer          userData);

    static char *idToJabber(WCHAR *id);
    static bool idFromJabber(const char *jabber, 
                             BSTR       *guid);
    static bool parseRoomJid(const char *jid, 
                             BSTR       *postId, 
                             BSTR       *userId);
    static LmMessageNode *findChildNode(LmMessageNode *node, 
                                        const char    *elementNamespace, 
                                        const char    *elementName);

private:
    State state_;

    HippoUI *ui_;
    LmConnection *lmConnection_;
    HippoBSTR username_;
    HippoBSTR password_;

    // Timeout waiting for user info. We can be in one of two states
    //
    // SIGN_IN_WAIT: We haven't tried to connect yet
    // AUTH_WAIT: We connected, and then authentication failed
    //
    // (We could always connect immediately to eliminate the first case)
    //
    guint signInTimeoutID_;   // GSource ID for timeout
    int signInTimeoutCount_;  // Number of times we've checked

    // Timeout waiting for the server to appear; state is RETRYING
    guint retryTimeoutID_;    // GSource ID for timeout

    HippoArray<HippoChatRoom *> chatRooms_;

    GQueue *pending_messages_;
};
