package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when another user adds or removes this user as
 * a contact.
 * 
 * @author otaylor
 */
public class ContactersChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;
	
	/**
	 * @param userId the user whose contacters changed
	 */
	public ContactersChangedEvent(Guid userId) {
		this.userId = userId;
	}
	
	public Guid getUserId() {
		return userId;
	}

	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return ContactersChangedEventProcessor.class;
	}
}
