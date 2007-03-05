package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.ExternalAccountType;

/**
 * Event sent when an external account changes for a user
 * 
 */
public class ExternalAccountChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;

	private ExternalAccountType type;
	
	/**
	 * @param userId the userID
	 */
	public ExternalAccountChangedEvent(Guid userID, ExternalAccountType type) {
		this.userId = userID;
		this.type = type;
	}

	public Guid getUserId() {
		return userId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}

	public ExternalAccountType getType() {
		return type;
	}
}
