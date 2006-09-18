package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a few particular details about a user such as current
 * music track or photo change.
 * 
 * @author walters
 */
public class UserDetailChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;
	
	/**
	 * @param userId the userID
	 */
	public UserDetailChangedEvent(Guid userID) {
		this.userId = userID;
	}

	public Guid getUserId() {
		return userId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}
}
