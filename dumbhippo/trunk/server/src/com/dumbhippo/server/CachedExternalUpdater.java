package com.dumbhippo.server;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.listeners.ExternalAccountsListener;

public interface CachedExternalUpdater<Status> extends ExternalAccountsListener {

	public Status getCachedStatus(User user) throws NotFoundException;
	public Status getCachedStatus(ExternalAccount external) throws NotFoundException;
	public Status getCachedStatus(String key) throws NotFoundException;

	public void periodicUpdate(String key);
}
