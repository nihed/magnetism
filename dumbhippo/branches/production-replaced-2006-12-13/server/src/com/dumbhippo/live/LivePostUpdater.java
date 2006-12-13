package com.dumbhippo.live;

import javax.ejb.Local;

/**
 * Create and update LivePost objects using information from the
 * data store.
 */
@Local
public interface LivePostUpdater {
	/**
	 * Does initialization of a newly created LivePost object.
	 * 
	 * @param user the LivePost object to initialize
	 */
	void initialize(LivePost post);
}
