/* HippoIM.h: Manage the connection to the message server
 *
 * Copyright Red Hat, Inc. 2005
 **/

#pragma once

#include <HippoUtil.h>
#include <loudmouth/loudmouth.h>

class HippoUI;

class HippoIM 
{
public:
    enum State {
	SIGNED_OUT,     // User hasn't asked to connect
	SIGN_IN_WAIT,   // Waiting for the user to sign in
	CONNECTING,     // Waiting for connecting to server
	RETRYING,       // Connection to server failed, retrying
	AUTHENTICATING, // Waiting for authentication
	AUTHENTICATED   // Authenticated to server
    };

    HippoIM();

    // Set the main UI object. Must be called before use
    void setUI(HippoUI *ui);

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

private:
    HRESULT getAuthURL(BSTR *result);
    bool loadAuth();
    void connect();
    void disconnect();
    void authFailure(char *message);

    static void onConnectionOpen (LmConnection *connection,
				  gboolean      success,
				  gpointer      userData);

    static void onConnectionAuthenticate (LmConnection *connection,
				          gboolean      success,
				          gpointer      userData);

    static LmHandlerResult onMessage (LmMessageHandler *handler,
				      LmConnection     *connection,
				      LmMessage        *message,
				      gpointer          userData);
private:
    State state_;

    HippoUI *ui_;
    LmConnection *lmConnection_;
    HippoBSTR username_;
    HippoBSTR password_;
};
