package com.dumbhippo.web.pages;

import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.util.EJBUtil;

public class StackedMugshotPersonPage extends StackedPersonPage {
	
	private AccountSystem accounts;
	
	public StackedMugshotPersonPage() {
		accounts = EJBUtil.defaultLookup(AccountSystem.class);
		setNeedExternalAccounts(true);
		setViewedUser(accounts.getCharacter(Character.MUGSHOT));
	}
}
