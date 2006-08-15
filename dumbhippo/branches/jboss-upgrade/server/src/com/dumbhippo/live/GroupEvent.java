package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Sent when a group is modified
 * 
 * @author walters
 */
public class GroupEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid groupId;	
	private Guid resourceId;
	
	public enum Type {
		POST_ADDED, MEMBERSHIP_CHANGE;
	};
	
	private Type event;
	
	/**
	 * @param groupId the group which changed
	 * @param resourceId the resource related to the change (new post, new member)
	 * @param event the event type
	 */
	public GroupEvent(Guid groupId, Guid resourceId, Type event) {
		this.groupId = groupId;
		this.resourceId = resourceId;
		this.event = event;
	}

	public Guid getGroupId() {
		return groupId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return GroupEventProcessor.class;
	}

	public Type getEvent() {
		return event;
	}
	
	public Guid getResourceId() {
		return resourceId;
	}
}
