package com.dumbhippo.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	// are we a friend of User?
	private Map<User,Boolean> cachedFriendOfStatus;
	// if all friend of status is cached, not having a user in the cachedFriendOfStatus map
	// means that we are not a friend of that user
	private boolean allFriendOfStatusCached;
	
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
		this.allFriendOfStatusCached = false;
	}
	
	public User getViewer() {
		return viewer;
	}
	
	@Override
	public boolean isOfUser(User user) {
		return viewer.equals(user);
	}
	 
	public boolean isFriendOfStatusCached(User user) {
		if (cachedFriendOfStatus == null)
			return false;
		else
			return (allFriendOfStatusCached || cachedFriendOfStatus.containsKey(user));
	}
	
	/**
	 * You must check isFriendOfStatusCached() first 
	 * and call this only if it returns true...
	 * indicates whether we are a friend of the user
	 * 
	 * @param user
	 * @return
	 */
	public boolean getCachedFriendOfStatus(User user) {
		if (allFriendOfStatusCached && !cachedFriendOfStatus.containsKey(user))
			return false;
		
		return cachedFriendOfStatus.get(user);
	}
	
	public void setCachedFriendOfStatus(User user, boolean isFriendOf) {
		if (cachedFriendOfStatus == null)
			cachedFriendOfStatus = new HashMap<User,Boolean>();
		cachedFriendOfStatus.put(user, isFriendOf);
	}

	public void cacheAllFriendOfStatus(Set<User> usersFriendOf) {
		if (cachedFriendOfStatus == null)
			cachedFriendOfStatus = new HashMap<User,Boolean>();
		
		for (User userFriendOf : usersFriendOf) {
			cachedFriendOfStatus.put(userFriendOf, true);
		}
		allFriendOfStatusCached = true;
	}
	
	@Override
	public String toString() {
		return "{UserViewpoint " + viewer + "}";
	}
}
