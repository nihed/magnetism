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
	
	public enum Detail {
		POST_ADDED, MEMBERS_CHANGED;
	};
	
	private Detail detail;
	
	/**
	 * @param groupId the group which changed
	 * @param resourceId the resource related to the change (new post, new member)
	 * @param event the event type
	 */
	public GroupEvent(Guid groupId, Guid resourceId, Detail detail) {
		this.groupId = groupId;
		this.resourceId = resourceId;
		this.detail = detail;
	}

	public Guid getGroupId() {
		return groupId;
	}
	
	public Detail getDetail() {
		return detail;
	}
	
	public Guid getResourceId() {
		return resourceId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return GroupEventProcessor.class;
	}
}
