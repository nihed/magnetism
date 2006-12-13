package com.dumbhippo.live;

import java.util.Date;

import com.dumbhippo.identity20.Guid;

/**
 * Update notification object sent when a user views a post for the
 * the first time.
 * 
 * @author otaylor
 */
public class PostViewedEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid postId;
	private Guid viewerId;
	private long viewedDate;
	
	/**
	 * @param postId the post that was viewed
	 * @param viewerId the user viewing the post
	 * @param viewedDate the time at which the user viewed the post 
	 */
	public PostViewedEvent(Guid postId, Guid viewerId, Date viewedDate) {
		this.postId = postId;
		this.viewerId = viewerId;
		this.viewedDate = viewedDate.getTime();
	}

	public Guid getPostId() {
		return postId;
	}

	public Guid getViewerId() {
		return viewerId;
	}
	
	public Date getViewedDate() {
		return new Date(this.viewedDate);
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return PostViewedProcessor.class;
	}
}
