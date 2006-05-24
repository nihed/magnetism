package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostingBoard;

// Handles processing incoming GroupEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.GroupEventProcessor")
public class GroupEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	LiveGroupUpdater groupUpdater;
	
	@EJB
	LiveUserUpdater userUpdater;
	
	@EJB
	PostingBoard postingBoard;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		GroupEvent event = (GroupEvent)abstractEvent;
		LiveGroup liveGroup = state.peekLiveGroup(event.getGroupId());
		if (event.getEvent() == GroupEvent.Type.MEMBERSHIP_CHANGE) {
			if (liveGroup != null)
				groupUpdater.groupMemberCountChanged(liveGroup);
			LiveUser liveUser = state.peekLiveUser(event.getResourceId());
			if (liveUser != null)
				userUpdater.handleGroupMembershipChanged(liveUser);
		} else if (event.getEvent() == GroupEvent.Type.POST_ADDED) {
			if (liveGroup != null)
				groupUpdater.groupPostReceived(liveGroup);
		}
	}
}
