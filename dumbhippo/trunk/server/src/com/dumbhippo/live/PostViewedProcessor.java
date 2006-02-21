package com.dumbhippo.live;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Post;
import com.dumbhippo.persistence.Resource;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.IdentitySpider;
import com.dumbhippo.server.PostingBoard;

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
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		PostViewedEvent event = (PostViewedEvent)abstractEvent;

		Post post;
		try {
			post = postingBoard.loadRawPost(null, event.getPostId());
		} catch (NotFoundException e) {
			throw new RuntimeException("PostViewedEvent for non-existant post");
		}
		
		LivePost livePost = state.getLivePost(post.getGuid());
		livePost.addViewer(event.getViewerId(), event.getViewedDate());
		
		Set<LiveUser> concernedUsers = new HashSet<LiveUser>();
		
		for (Resource resource : post.getExpandedRecipients()) {
			User user = identitySpider.getUser(resource);
			if (user != null) {
				LiveUser liveUser = state.peekLiveUser(user.getGuid());
				if (liveUser != null) {
					concernedUsers.add(liveUser);
				}
			}
		}
		
		LiveUser liveUser = state.peekLiveUser(post.getPoster().getGuid());
		if (liveUser != null) {
			concernedUsers.add(liveUser);
		}

		logger.debug("{} clicked on {}", event.getViewerId(), event.getPostId());
		logger.debug("Post score is now {}", livePost.getScore());
	}
}
