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
	
	public enum Type {
		POST_ADDED, MEMBERSHIP_CHANGE;
	};
	
	private Type event;
	
	/**
	 * @param groupId the group which was a recipient of a post
	 */
	public GroupEvent(Guid groupId, Type event) {
		this.groupId = groupId;
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

	public void setEvent(Type event) {
		this.event = event;
	}
}
