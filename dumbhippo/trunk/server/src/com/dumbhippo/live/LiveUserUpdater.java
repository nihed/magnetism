package com.dumbhippo.live;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;

/**
 * Create and update LivePost objects using information from the
 * data store.
 */
@Local
public interface LiveUserUpdater {
	/**
	 * Does initialization of a newly created LiveUser object.
	 * 
	 * @param user the LiveUser object to initialize
	 */
	void initialize(LiveUser user);
	
	void handlePostCreated(Guid userGuid);
	
	public void handleGroupMembershipChanged(Guid userGuid);
	
	public void handleContactsChanged(Guid userGuid);
}
