package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.identity20.Guid;

// Handles processing incoming PostViewedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.PostCreatedEventProcessor")
public class PostCreatedEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	LiveUserUpdater userUpdater;
	
	public void process(LiveState state, LiveEvent abstractEvent, boolean isLocal) {
		PostCreatedEvent event = (PostCreatedEvent)abstractEvent;
		// posterId is null for FeedPost
		Guid posterId = event.getPosterId();
		if (posterId != null)
			userUpdater.handlePostCreated(posterId);	
	}
}
