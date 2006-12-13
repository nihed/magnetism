package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;

public interface ExternalAccountsListener {
	public void onExternalAccountCreated(User user, ExternalAccount external);
}
