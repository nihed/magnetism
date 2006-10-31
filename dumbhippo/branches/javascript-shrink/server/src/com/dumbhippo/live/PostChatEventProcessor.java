package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.SystemViewpoint;

// Handles processing incoming PostViewedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.PostChatEventProcessor")
public class PostChatEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PostChatEventProcessor.class);

	@EJB
	PostingBoard postingBoard;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		PostChatEvent event = (PostChatEvent)abstractEvent;
		
		Post post;
		try {
			post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
		} catch (NotFoundException e) {
			logger.error("Unknown post processing chat update to live post");
			return;
		}
		
		LivePost livePost = state.getLivePostForUpdate(event.getPostId());
		try {
			livePost.setRecentMessageCount(postingBoard.getRecentPostMessageCount(post, LivePostUpdaterBean.RECENT_MESSAGES_AGE_IN_SECONDS));
		} finally {
			state.updateLivePost(livePost);
		}
	
		logger.debug("Post score is now {} after recent chat messages became {}", livePost.getScore(), livePost.getRecentMessageCount());
		
		// FIXME at least two problems here:
		// - we never send out any XMPP
		// - we don't drop recent post count as time passes, unless there's more chat
	}
}
