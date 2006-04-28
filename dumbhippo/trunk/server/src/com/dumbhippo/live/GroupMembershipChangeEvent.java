package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Notification sent when a member is added or removed from a group.
 * 
 * @author walters
 */
public class GroupMembershipChangeEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid groupId;
	boolean addition;
	
	/**
	 * @param groupId group with membership change
	 * @param isAddition whether or not this change represents an addition
	 */
	public GroupMembershipChangeEvent(Guid groupId, boolean isAddition) {
		this.groupId = groupId;
		this.addition = isAddition;
	}

	public boolean isAddition() {
		return addition;
	}
	
	public Guid getGroupId() {
		return groupId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return GroupMembershipChangeEventProcessor.class;
	}
}
