package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.User;

/**
 * backing bean for /groups
 * 
 */

public class GroupsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ViewPersonPage.class);	
	
	private String mode;
	
	public GroupsPage() {
	}

	@Override
	protected void setViewedUser(User user) {
		super.setViewedUser(user);
	}
	
	public void setMode(String mode) {
		this.mode = mode;
		if (mode != null && mode.equals("invited")) {
			if (isSelf() && (getViewedUser() != null)) { 
				Account acct = getViewedUser().getAccount();
				acct.touchLastSeenGroupInvitations();
			}
		}
	}
	
	public String getMode() {
		return mode;
	}

}
