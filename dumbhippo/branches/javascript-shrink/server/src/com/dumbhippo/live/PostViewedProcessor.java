package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Person;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.server.MessageSender;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.SystemViewpoint;

// Handles processing incoming PostViewedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.PostViewedProcessor")
public class PostViewedProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	private PostingBoard postingBoard;
	
	@EJB
	private MessageSender messageSender;
	
	private boolean numIsPow2(long num) {
		return num != 0 && ((num & (num - 1)) == 0);
	}
	
	// Thinking about this a bit more, interesting should probably be
	// based at least partially on a delta since a given time period,
	// not absolutes
	private boolean countIsInteresting(long count) {
		if (count >= 3 && count <= 5)
			return true;
		return numIsPow2(count) || (numIsPow2(count - Math.round(count*.1))
				         || numIsPow2(count + Math.round(count*.1)));		
	}	
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		PostViewedEvent event = (PostViewedEvent)abstractEvent;

		Post post;
		try {
			post = postingBoard.loadRawPost(SystemViewpoint.getInstance(), event.getPostId());
		} catch (NotFoundException e) {
			throw new RuntimeException("PostViewedEvent for non-existant post");
		}
		
		LivePost livePost = state.getLivePostForUpdate(post.getGuid());
		try {
			livePost.addViewer(event.getViewerId(), event.getViewedDate());
			livePost.setTotalViewerCount(livePost.getTotalViewerCount() + 1);
		} finally {
			state.updateLivePost(livePost);
		}
	
		logger.debug("{} clicked on {} new viewer count " + livePost.getTotalViewerCount(), event.getViewerId(), event.getPostId());
		logger.debug("Post score is now {}", livePost.getScore());
		
		Person poster = post.getPoster();
		// Suppress notification of the poster viewing their own post, and check that the
		// count is "interesting", presently defined by being close to a power of two.
		if ((poster == null || !event.getViewerId().equals(poster.getGuid()))
			&& (countIsInteresting(livePost.getTotalViewerCount()) || countIsInteresting(livePost.getRecentMessageCount()))) {
			messageSender.sendLivePostChanged(livePost, event.getViewerId());
		}		
	}
}
