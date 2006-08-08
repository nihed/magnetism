package com.dumbhippo.live;

import javax.ejb.Local;

import com.dumbhippo.identity20.Guid;

/**
 * Create and update LiveClientData objects using information from the
 * data store.
 */
@Local
public interface LiveClientDataUpdater {
	/**
	 * Does initialization of a newly created LiveClientData object.
	 * 
	 * @param user the LiveClientData object to initialize
	 */
	void initialize(LiveClientData user);
	
	void periodicUpdate(Guid userGuid);

	void sendAllNotifications(LiveClientData clientData);
}
