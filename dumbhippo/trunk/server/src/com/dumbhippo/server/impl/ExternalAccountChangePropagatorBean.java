package com.dumbhippo.server.impl;

import javax.ejb.Stateless;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.ExternalAccountChangedEvent;
import com.dumbhippo.live.LiveState;
import com.dumbhippo.persistence.ExternalAccount;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.ExternalAccountChangePropagator;
import com.dumbhippo.server.dm.DataService;
import com.dumbhippo.server.dm.ExternalAccountDMO;
import com.dumbhippo.server.dm.ExternalAccountKey;
import com.dumbhippo.server.dm.UserDMO;

@Stateless
public class ExternalAccountChangePropagatorBean implements ExternalAccountChangePropagator {

	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(ExternalAccountChangePropagator.class);

	private void notify(User user, ExternalAccount external) {
		LiveState.getInstance().queueUpdate(new ExternalAccountChangedEvent(user.getGuid(), external.getAccountType()));		
	}
	
	public void onExternalAccountCreated(User user, ExternalAccount external) {
		DataService.currentSessionRW().changed(UserDMO.class, user.getGuid(), "lovedAccounts");
		notify(user, external);
	}

	public void onExternalAccountLovedAndEnabledMaybeChanged(User user, ExternalAccount external) {
		DataService.currentSessionRW().changed(UserDMO.class, user.getGuid(), "lovedAccounts");
		DataService.currentSessionRW().changed(ExternalAccountDMO.class, new ExternalAccountKey(external), "link");
		notify(user, external);
	}
}
