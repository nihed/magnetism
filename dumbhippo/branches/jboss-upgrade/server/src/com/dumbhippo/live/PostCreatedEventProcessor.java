package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

// Handles processing incoming PostViewedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.PostCreatedEventProcessor")
public class PostCreatedEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	LiveUserUpdater userUpdater;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		PostCreatedEvent event = (PostCreatedEvent)abstractEvent;
		userUpdater.handlePostCreated(event.getPosterId());	
	}
}
