package com.dumbhippo.server.views;

import com.dumbhippo.persistence.User;

/**
 * UserViewpoint represents the view onto the system and access
 * controls for a logged in user. It is also used as a place to
 * cache data associated with that user's view for the scope
 * of a request. (Keeping the cache request scoped means that 
 * we generally don't need to worry about invalidating the
 * cached data.)
 * 
 * @author otaylor
 */
public class UserViewpoint extends Viewpoint {
	
	private final User viewer;
	
	/**
	 * Creates a UserViewpoint for a new user; calling this method
	 * directly defeats caching, so try not to use it when the 
	 * UserViewpoint could be passed in from the caller. (In the
	 * normal case, the UserViewpoint is created from the web tier,
	 * and passed down to the rest of the code.)
	 * 
	 * @param viewer the user viewing the system
	 */
	public UserViewpoint(User viewer) {
		if (viewer == null)
			throw new NullPointerException("UserViewpoint created with null user");
		this.viewer = viewer;
	}
	
	public User getViewer() {
		return viewer;
	}
	
	@Override
	public boolean isOfUser(User user) {
		return viewer.equals(user);
	}
	 
	@Override
	public String toString() {
		return "{UserViewpoint " + viewer + "}";
	}
}
