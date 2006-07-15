package com.dumbhippo.live;

import javax.ejb.Local;

/**
 * Create and update LiveGroup objects using information from the
 * data store.
 */
@Local
public interface LiveGroupUpdater {
	/**
	 * Does initialization of a newly created LiveGroup object.
	 * 
	 * @param user the LiveGroup object to initialize
	 */
	void initialize(LiveGroup group);
	
	void groupPostReceived(LiveGroup group);
	
	void groupMemberCountChanged(LiveGroup group);
}
