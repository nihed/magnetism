package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.persistence.OnlineAccountType;

/**
 * Event sent when an external account changes for a user
 * 
 */
public class ExternalAccountChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private long id = -1;
	
	private Guid userId;

	private String type;	
	
	/**
	 * @param userId the userID
	 */
	public ExternalAccountChangedEvent(Guid userID, OnlineAccountType type, long id) {
		this.userId = userID;
		this.type = type.getName();
		this.id = id;
	}

	public Guid getUserId() {
		return userId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return null;
	}

	public String getType() {
		return type;
	}
	
	public long getId() {
		return id;
	}
}
