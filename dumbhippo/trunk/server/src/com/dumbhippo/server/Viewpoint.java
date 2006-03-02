package com.dumbhippo.server;

import java.util.HashMap;
import java.util.Map;

import com.dumbhippo.persistence.User;

/**
 * The Viewpoint class is simply a wrapper for Person to give us
 * type checking in functions that take both a Person that is viewing
 * and a person that is being viewed.
 * 
 * It can also be used to cache information about the viewer with request 
 * scope for the cache (i.e. a new Viewpoint has to be created per-request
 * and ideally we only create one per request)
 * 
 * Eventually this should maybe be a Request object even
 * 
 * @author otaylor
 */
public class Viewpoint {
	private final User viewer;
	// are we a friend of User?
	private Map<User,Boolean> cachedFriendOfStatus;
	
	public Viewpoint(User viewer) {
		this.viewer = viewer;
	}
	
	public User getViewer() {
		return viewer;
	}
	 
	public boolean isFriendOfStatusCached(User user) {
		if (cachedFriendOfStatus == null)
			return false;
		else
			return cachedFriendOfStatus.containsKey(user);
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
		return cachedFriendOfStatus.get(user);
	}
	
	public void setCachedFriendOfStatus(User user, boolean isFriendOf) {
		if (cachedFriendOfStatus == null)
			cachedFriendOfStatus = new HashMap<User,Boolean>();
		cachedFriendOfStatus.put(user, isFriendOf);
	}
}
