package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.Account;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonViewer;
import com.dumbhippo.server.views.PersonView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.UserViewpoint;

public class UserSigninBean extends SigninBean {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(UserSigninBean.class);

	private Boolean musicSharingEnabled; // lazily initialized
	private Boolean defaultSharePublic; // lazily initialized

	private IdentitySpider spider;
	private PersonViewer viewer;
	private PersonView userFromSystemView;
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
	
	private IdentitySpider getSpider() {
		if (spider == null)
			spider = WebEJBUtil.defaultLookup(IdentitySpider.class);
		return spider;
	}
	
	private PersonViewer getViewer() {
		if (viewer == null)
			viewer = WebEJBUtil.defaultLookup(PersonViewer.class);
		return viewer;
	}
		
	public User getUser() {
		if (user == null) {
			try {
				user = getSpider().lookupGuid(User.class, userGuid);
			} catch (NotFoundException e) {
				throw new RuntimeException("Could not lazily create User object");
			}
		}

		return user;
	}
	
	public PersonView getViewedUserFromSystem() {
		if (userFromSystemView == null)
			userFromSystemView = getViewer().getPersonView(SystemViewpoint.getInstance(), getUser());
		return userFromSystemView;
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
	public boolean isActive() {
		return getUser().getAccount().isActive();
	}
	
	@Override
	public void resetSessionObjects() {
		user = null;
	}	

	@Override
	public boolean getNeedsTermsOfUse() {
		return !getUser().getAccount().getHasAcceptedTerms();
	}
	
	@Override
	public boolean isDisabled() {
		return getUser().getAccount().isDisabled();
	}
	
	public boolean isMusicSharingEnabled() {
		if (musicSharingEnabled == null) {
			IdentitySpider identitySpider = WebEJBUtil.defaultLookup(IdentitySpider.class);
			musicSharingEnabled = Boolean.valueOf(identitySpider.getMusicSharingEnabled(getUser(), Enabled.RAW_PREFERENCE_ONLY));
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
