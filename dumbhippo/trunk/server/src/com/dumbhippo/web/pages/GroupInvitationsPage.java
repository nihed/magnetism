package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.User;

/**
 * backing bean for /group-invitations
 * 
 */

public class GroupInvitationsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ViewPersonPage.class);	
	
	public GroupInvitationsPage() {
	}

	@Override
	protected void setViewedUser(User user) {
		super.setViewedUser(user);
		if (isSelf()) { 
			Account acct = getViewedUser().getAccount();
			acct.touchLastSeenGroupInvitations();
		}
	}
	
}
