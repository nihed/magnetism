package com.dumbhippo.server;

import java.io.Serializable;

import com.dumbhippo.persistence.Person;

public class AbstractLoginRequired implements LoginRequired, Serializable {

	private static final long serialVersionUID = 0L;
	private String loggedInUserId;

	private transient Person cachedLoggedInUser;
	
	public String getLoggedInUserId() {
		return loggedInUserId;
	}

	public void setLoggedInUserId(String loggedInUserId) {
		this.loggedInUserId = loggedInUserId;
	}
	
	public boolean isLoggedIn() {
		return loggedInUserId != null;
	}
	
	/**
	 * Looks up the user in the identity spider and caches it, returning the 
	 * cached copy on all subsequent calls. Note that this means the Person 
	 * is normally detached from the persistence context.
	 * 
	 * @param identitySpider
	 * @return the Person the object is logged in as
	 */
	protected Person getLoggedInUser(IdentitySpider identitySpider) {
		if (cachedLoggedInUser == null && getLoggedInUserId() != null) {
			cachedLoggedInUser = identitySpider.lookupPersonById(getLoggedInUserId());
		}
		
		if (cachedLoggedInUser == null)
			throw new IllegalStateException("Trying to use " + this.getClass().getCanonicalName() + " when not logged in");
		
		return cachedLoggedInUser; 
	}
}

