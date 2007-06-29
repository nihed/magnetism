package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Event sent when a post is created.
 * 
 * @author walters
 */
public class PostCreatedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid postId;
	private Guid posterId;
	
	/**
	 * @param postId the post that was creatd
	 * @param posterId the user which created the post 
	 */
	public PostCreatedEvent(Guid postId, Guid posterId) {
		this.postId = postId;
		this.posterId = posterId;
	}

	public Guid getPostId() {
		return postId;
	}
	
	public Guid getPosterId() {
		return posterId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return PostCreatedEventProcessor.class;
	}
}
