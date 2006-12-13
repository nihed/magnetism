package com.dumbhippo.server.blocks;

import javax.ejb.Local;

import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountFeedListener;
import com.dumbhippo.server.listeners.ExternalAccountsListener;

@Local
public interface MySpacePersonBlockHandler extends BlogLikeBlockHandler, ExternalAccountsListener, ExternalAccountFeedListener {
	public void migrate(User user);	
}