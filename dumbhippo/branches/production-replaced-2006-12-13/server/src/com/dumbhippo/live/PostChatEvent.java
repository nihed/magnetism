package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

public class PostChatEvent implements LiveEvent {
	private static final long serialVersionUID = 1L;
	
	private Guid postId;
	
	/**
	 * @param postId the post that was chatted about 
	 */
	public PostChatEvent(Guid postId) {
		this.postId = postId;
	}

	public Guid getPostId() {
		return postId;
	}
	
	public Class<? extends LiveEventProcessor> getProcessorClass() {
		return PostChatEventProcessor.class;
	}
}
