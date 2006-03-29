/* HippoIM.h: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <HippoUtil.h>
#include "HippoDataCache.h"
#include "HippoChatRoom.h"
#include "HippoMusicMonitor.h"
#include <loudmouth/loudmouth.h>

class HippoUI;
class HippoChatRoom;
class HippoMySpaceBlogComment;
class HippoMySpaceContact;

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

    void addMySpaceComment(const HippoMySpaceBlogComment &comment);
    void notifyMySpaceContactPost(HippoMySpaceContact *contact);

    void getMySpaceContacts();
    void getMySpaceSeenBlogComments();

    // Try to sign in. Returns TRUE if we need to show a web page where
    // the user can sign in
    bool signIn();

    // Sign out from the server
    void signOut();

    // Check if we have authentication information stored
    bool hasAuth();

    // Forget any remembered authentication information
    void forgetAuth() throw (std::bad_alloc);

    // Get the current state
    State getState();

    void getUsername(BSTR *ret) throw (std::bad_alloc);

    HRESULT findChatRoom(BSTR postId, IHippoChatRoom **chatRoom);
    HRESULT getChatRoom(BSTR postId, IHippoChatRoom **chatRoom);

    // Called by HippoChatRoom
    void onChatRoomStateChange(HippoChatRoom *chatRoom, HippoChatRoom::State oldState);
    void sendChatRoomMessage(HippoChatRoom *chatRoom, BSTR text);
    void removeChatRoom(HippoChatRoom *chatRoom);

    // called by HippoUI
    bool getNeedPrimingTracks();
    void providePrimingTracks(HippoPlaylist *playlist);

private:
    void getAuthURL(BSTR *result) throw (std::bad_alloc);

    void stateChange(State state);

    bool loadAuth();
    void connect();
    void disconnect();
    void authenticate();
    void getClientInfo();
    void getMySpaceName();
    void getHotness();
    void getRecentPosts();

    void startSignInTimeout();
    void stopSignInTimeout();

    void startRetryTimeout();
    void stopRetryTimeout();

    void clearConnection();

    void sendChatRoomPresence(HippoChatRoom *chatRoom, LmMessageSubType subType, bool participant = true);
    void sendChatRoomEnter(HippoChatRoom *chatRoom, bool participant);
    void sendChatRoomLeave(HippoChatRoom *chatRoom);

    bool checkRoomMessage(LmMessage      *message,
                          HippoChatRoom **chatRoom,
                          BSTR           *userId) throw (std::bad_alloc);
    bool getChatUserInfo(LmMessageNode *parent,
                         int           *version,
                         BSTR          *name,
                         bool          *participant) throw (std::bad_alloc);
    bool getChatMessageInfo(LmMessageNode *parent,
                            int           *version,
                            BSTR          *name,
                            INT64         *timestamp,
                            int           *serial) throw (std::bad_alloc);
    LmHandlerResult handleRoomMessage(LmMessage     *message,
                                      HippoChatRoom *chatRoom,
                                      BSTR           userId);

    bool checkMySpaceNameChangedMessage(LmMessage      *message,
                                        char           **name);
    void handleMySpaceNameChangedMessage(char           *name);
    bool checkMySpaceContactCommentMessage(LmMessage      *message);
    void handleMySpaceContactCommentMessage();

    bool handleHotnessMessage(LmMessage *message);
    bool handleActivePostsMessage(LmMessage *message);
    bool handlePrefsChangedMessage(LmMessage *message);
    bool handleLivePostChangedMessage(LmMessage *message);

    void connectFailure(char *message);
    void authFailure(char *message);

    void sendMessage(LmMessage *message);
    void sendMessage(LmMessage *message, LmMessageHandler *handler);
    void flushPending();

    void updatePrefs();
    void processPrefsNode(LmMessageNode *node);

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

    static bool nodeMatches(LmMessageNode *node, const char *name, const char *expectedNamespace);
    static bool messageIsIqWithNamespace(HippoIM *im, LmMessage *message, const char *expectedNamespace, const char *documentElementName);
    bool parseEntityIdentifier(LmMessageNode *node, HippoBSTR &id);
    bool isEntity(LmMessageNode *node);
    bool parseEntity(LmMessageNode *node, HippoEntity *person);
    bool isLivePost(LmMessageNode *node);
    bool parseLivePost(LmMessageNode *postNode, HippoPost *post);
    bool isPost(LmMessageNode *node);
    bool parsePost(LmMessageNode *postNode, HippoPost *post);
    bool parsePostStream(LmMessageNode *node, const char *funcName, HippoPost *post);

    static LmHandlerResult onClientInfoReply(LmMessageHandler *handler,
                                             LmConnection     *connection,
                                             LmMessage        *message,
                                             gpointer          userData);
    static LmHandlerResult onGetMySpaceNameReply(LmMessageHandler *handler,
                                                 LmConnection     *connection,
                                                 LmMessage        *message,
                                                 gpointer          userData);
    static LmHandlerResult onGetMySpaceBlogCommentsReply(LmMessageHandler *handler,
                                                         LmConnection     *connection,
                                                         LmMessage        *message,
                                                         gpointer          userData);
    static LmHandlerResult onGetMySpaceContactsReply(LmMessageHandler *handler,
                                                     LmConnection     *connection,
                                                     LmMessage        *message,
                                                     gpointer          userData);
    static LmHandlerResult onGetHotnessReply(LmMessageHandler *handler,
                                             LmConnection     *connection,
                                             LmMessage        *message,
                                             gpointer          userData);
    static LmHandlerResult onGetRecentPostsReply(LmMessageHandler *handler,
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

    static LmHandlerResult onIQ(LmMessageHandler *handler,
                                LmConnection     *connection,
                                LmMessage        *message,
                                gpointer          userData);

    static LmHandlerResult onPrefsReply(LmMessageHandler *handler,
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

    bool musicSharingEnabled_;
    bool musicSharingPrimed_;
};
