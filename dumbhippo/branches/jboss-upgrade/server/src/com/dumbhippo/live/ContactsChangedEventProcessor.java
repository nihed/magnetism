package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;

// Handles processing incoming ContactsChangedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.ContactsChangedEventProcessor")
public class ContactsChangedEventProcessor implements LiveEventProcessor {
	@EJB
	LiveUserUpdater userUpdater;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		ContactsChangedEvent event = (ContactsChangedEvent)abstractEvent;
		userUpdater.handleContactsChanged(event.getUserId());	
	}
}
