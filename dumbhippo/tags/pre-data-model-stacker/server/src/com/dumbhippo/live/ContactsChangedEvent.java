package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a contact is added to or removed from a user
 * 
 * @author otaylor
 */
public class ContactsChangedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid userId;
	
	/**
	 * @param contactId the user who's contacts changed
	 */
	public ContactsChangedEvent(Guid userId) {
		this.userId = userId;
	}
	
	public Guid getUserId() {
		return userId;
	}

	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return ContactsChangedEventProcessor.class;
	}
}
