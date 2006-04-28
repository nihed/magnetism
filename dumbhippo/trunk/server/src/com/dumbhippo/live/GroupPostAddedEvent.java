package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Sent when a group is a recipient of a post
 * 
 * @author walters
 */
public class GroupPostAddedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid groupId;	
	
	/**
	 * @param groupId the group which was a recipient of a post
	 */
	public GroupPostAddedEvent(Guid groupId) {
		this.groupId = groupId;
	}

	public Guid getGroupId() {
		return groupId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return GroupPostAddedEventProcessor.class;
	}
}
