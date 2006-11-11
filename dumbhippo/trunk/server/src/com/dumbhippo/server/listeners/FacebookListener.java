package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.FacebookEvent;
import com.dumbhippo.persistence.User;

/**
 * These listener methods all take a User object, which is technically 
 * redundant since you can do event.getFacebookAccount.getExternalAccount().getAccount().getOwner(),
 * but it's certainly more convenient like this.
 */
public interface FacebookListener {
	public void onFacebookEventCreated(User user, FacebookEvent event);
	public void onFacebookEvent(User user, FacebookEvent event);
}
