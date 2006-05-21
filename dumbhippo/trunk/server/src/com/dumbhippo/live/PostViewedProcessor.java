package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;

// Handles processing incoming PostViewedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.PostViewedProcessor")
public class PostViewedProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	PostingBoard postingBoard;
	
	@EJB
	IdentitySpider identitySpider;
	
	@EJB
	MessageSender messageSender;
	
	private boolean numIsPow2(long num) {
		return num != 0 && ((num & (num - 1)) == 0);
	}
	
	private boolean countIsInteresting(long count) {
		if (count >= 3 && count <= 5)
			return true;
		if (count > 128)
			return false;
		return numIsPow2(count);		
	}	
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		PostViewedEvent event = (PostViewedEvent)abstractEvent;

		Post post;
		try {
			post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
		} catch (NotFoundException e) {
			throw new RuntimeException("PostViewedEvent for non-existant post");
		}
		
		LivePost livePost = state.getLivePost(post.getGuid());
		livePost = (LivePost) livePost.clone();
		livePost.addViewer(event.getViewerId(), event.getViewedDate());
		livePost.setTotalViewerCount(livePost.getTotalViewerCount() + 1);
		state.updateLivePost(livePost);
	
		logger.debug("{} clicked on {}", event.getViewerId(), event.getPostId());
		logger.debug("Post score is now {}", livePost.getScore());
		if (countIsInteresting(livePost.getTotalViewerCount()) || countIsInteresting(livePost.getRecentMessageCount())) {
			messageSender.sendLivePostChanged(livePost);
		}		
	}
}
