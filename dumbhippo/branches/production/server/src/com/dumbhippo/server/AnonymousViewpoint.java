package com.dumbhippo.server;

import com.dumbhippo.persistence.User;

/**
 * AnonymousViewpoint represents a anonymous public view onto
 * the system. Only data that should be visible to someone
 * not logged into the system can be accessed, and no modifications
 * can be made to the database.
 * 
 * @author otaylor
 */
public class AnonymousViewpoint extends Viewpoint {
	private AnonymousViewpoint() {
	}
	
	static private AnonymousViewpoint instance = new AnonymousViewpoint();
	
	/**
	 * Gets the anonymous viewpoint singleton.
	 * 
	 * @return the global anonymous viewpoint
	 */	
	static public AnonymousViewpoint getInstance() {
		return instance;
	}

	@Override
	public boolean isOfUser(User user) {
		return false;
	}
}
