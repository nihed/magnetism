package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a few particular details about a user such as current
 * music track or photo change.
 * 
 * @author walters
 */
public class UserChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;

	private Detail detail;
	
	public enum Detail {
		MUSIC,
		PHOTO,
		PREFS
	}
	
	/**
	 * @param userId the userID
	 */
	public UserChangedEvent(Guid userID, Detail detail) {
		this.userId = userID;
		this.detail = detail;
	}

	public Guid getUserId() {
		return userId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}

	public Detail getDetail() {
		return detail;
	}
}
