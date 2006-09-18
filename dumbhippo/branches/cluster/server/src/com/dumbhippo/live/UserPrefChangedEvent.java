package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a user's preference changes.
 * 
 * @author walters
 */
public class UserPrefChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;

	private String key;

	private String value;
	
	public enum Detail {
		MUSIC,
		PHOTO,
		PREFS
	}
	
	/**
	 * @param userId the userID
	 */
	public UserPrefChangedEvent(Guid userID, String key, String value) {
		this.userId = userID;
		this.key = key;
		this.value = value;
	}

	public Guid getUserId() {
		return userId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}
