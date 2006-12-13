package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.FacebookAccount;
import com.dumbhippo.persistence.FacebookEventType;
import com.dumbhippo.persistence.User;

/**
 * These listener methods all take a User object, which is technically 
 * redundant since you can do facebookAccount.getExternalAccount().getAccount().getOwner(),
 * but it's certainly more convenient like this.
 */
public interface FacebookListener {
	public void onFacebookSignedIn(User user, FacebookAccount facebookAccount, long activity);
	public void onFacebookSignedOut(User user, FacebookAccount facebookAccount);
	// this should maybe take a FacebookEvent object? it seems that not all events
	// have those though
	public void onFacebookEvent(User user, FacebookEventType eventType, FacebookAccount facebookAccount, long updateTime);
}
