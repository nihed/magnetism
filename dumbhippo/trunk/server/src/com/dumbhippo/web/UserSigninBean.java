package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UserViewpoint;

public class UserSigninBean extends SigninBean {
	private static final Logger logger = GlobalSetup.getLogger(UserSigninBean.class);

	private Boolean disabled; // lazily initialized
	private Boolean musicSharingEnabled; // lazily initialized
	private Boolean defaultSharePublic; // lazily initialized

	private Guid userGuid;
	private User user; // lazily initialized
	
	/**
	 * Creates a new SigninBean object for a particular signed in
	 * user. DO NOT CALL THIS CONSTRUCTOR. Use Signin.getForRequest()
	 * instead. 
	 * 
	 * @param user the account object
	 */
	UserSigninBean(Account account) {
		this.user = account.getOwner();
		this.userGuid = user.getGuid();
	}
		
	public User getUser() {
		if (user == null) {
			IdentitySpider spider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			try {
				user = spider.lookupGuid(User.class, userGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Could not lazily create User object");
			}
		}

		return user;
	}
	
	public String getUserId() {
		return userGuid.toString();
	}
	
	public Guid getUserGuid() {
		return userGuid;
	}
	
	@Override
	public UserViewpoint getViewpoint() {
		return new UserViewpoint(getUser());
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override
	public void resetSessionObjects() {
		user = null;
	}	

	@Override
	public boolean getNeedsTermsOfUse() {
		return false; // we never create a UserSigninBean if this isn't true
	}
	
	public boolean isDisabled() {
		if (disabled == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			disabled = Boolean.valueOf(identitySpider.getAccountDisabled(getUser()));
			logger.debug("AccountPage loaded disabled = {}", disabled);
		}
		return disabled;
	}
	
	public boolean isMusicSharingEnabled() {
		if (musicSharingEnabled == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			musicSharingEnabled = Boolean.valueOf(identitySpider.getMusicSharingEnabled(getUser()));
		}
		return musicSharingEnabled;
	}
	
	public boolean isNotifyPublicShares() {
		if (defaultSharePublic == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			defaultSharePublic = Boolean.valueOf(identitySpider.getNotifyPublicShares(getUser()));
		}
		return defaultSharePublic;
	}
	
}
