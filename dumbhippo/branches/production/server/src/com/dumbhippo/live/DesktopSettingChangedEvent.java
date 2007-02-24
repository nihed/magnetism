package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a desktop setting changes for a user
 * 
 */
public class DesktopSettingChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;

	private String key;

	private String value;
	
	/**
	 * @param userId the userID
	 */
	public DesktopSettingChangedEvent(Guid userID, String key, String value) {
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

	/** 
	 * value will be null if the setting was unset.
	 * @return
	 */
	public String getValue() {
		return value;
	}
}
