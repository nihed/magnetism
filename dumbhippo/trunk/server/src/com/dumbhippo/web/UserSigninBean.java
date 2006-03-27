package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.UserViewpoint;

public class UserSigninBean extends SigninBean {
	private static final Logger logger = GlobalSetup.getLogger(UserSigninBean.class);

	private Boolean disabled; // lazily initialized
	private Boolean musicSharingEnabled; // lazily initialized

	private Guid userGuid;
	private User user; // lazily initialized
	
	/**
	 * Creates a new SigninBean object for a particular signed in
	 * user. DO NOT CALL THIS CONSTRUCTOR. Use Signin.getForRequest()
	 * instead. 
	 * 
	 * @param userGuid the GUID of the user
	 * @param user the user object, if we've already created one (may be null,
	 *        in which case the User object will be lazily created on access)
	 */
	UserSigninBean(Guid userGuid, User user) {
		this.userGuid = userGuid;
		this.user = user;
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
}
