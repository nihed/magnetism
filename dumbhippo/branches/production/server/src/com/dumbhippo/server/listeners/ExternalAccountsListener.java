package com.dumbhippo.server.listeners;

import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;

public interface ExternalAccountsListener {
	public void onExternalAccountCreated(User user, ExternalAccount external);
	
	/** "maybe" since we might get spurious notifies when ExternalAccount.isLovedAndEnabled() really returns 
	 *  the same value as before 
	 */
	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external);
}
