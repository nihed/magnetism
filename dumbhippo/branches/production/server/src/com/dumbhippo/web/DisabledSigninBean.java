package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.AccountSystem;
import com.dumbhippo.server.AnonymousViewpoint;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Viewpoint;

public class DisabledSigninBean extends SigninBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(DisabledSigninBean.class);

	private Guid userGuid;
	private Account account; // lazily initialized
	
	/**
	 * Creates a new DisabledSigninBean object for a user that either
	 * hasn't accepted the terms of use or has explicitely disabled
	 * their account. DO NOT CALL THIS CONSTRUCTOR. 
	 * Use Signin.getForRequest() instead. 
	 * 
	 * @param account the account object
	 */
	DisabledSigninBean(Account account) {
		this.userGuid = account.getOwner().getGuid();
		this.account = account;
	}
	
	private Account getAccount() {
		if (account == null) {
			AccountSystem accountSystem = WebEJBUtil.defaultLookup(AccountSystem.class);
			try {
				account = accountSystem.lookupAccountByOwnerId(userGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Could not lazily create Account object");
			}
		}
		
		return account;
	}
		
	/**
	 * Gets the user that is authenticated to the system, but whose count
	 * is disabled. This method is called getDisabledUser() not getUser()
	 * to avoid a JSP accidentally accessing the user as signin.user,
	 * as if the signin object where a UserSigninBean.
	 * @return the User object
	 */
	public User getDisabledUser() {
		return getAccount().getOwner();
	}
	
	@Override
	public Viewpoint getViewpoint() {
		return AnonymousViewpoint.getInstance();
	}
	
	@Override
	public boolean isValid() {
		return false;
	}
	
	@Override
	public void resetSessionObjects() {
		account = null;
	}
	
	@Override
	public boolean getNeedsTermsOfUse() {
		return !getAccount().getHasAcceptedTerms();
	}

	@Override
	public boolean isDisabled() {
		return getAccount().isDisabled();
	}
}
