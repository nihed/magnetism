package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;

// Handles processing incoming ContactersChangedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.ContactersChangedEventProcessor")
public class ContactersChangedEventProcessor implements LiveEventProcessor {
	@EJB
	LiveUserUpdater userUpdater;
	
	public void process(LiveState state, LiveEvent abstractEvent, boolean isLocal) {
		if (!isLocal) {
			ContactersChangedEvent event = (ContactersChangedEvent)abstractEvent;
			state.invalidateLocalContacters(event.getUserId());
		}
	}
}
