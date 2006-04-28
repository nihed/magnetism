package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.SystemViewpoint;

// Handles processing incoming GroupPostAddedEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.GroupPostAddedEventProcessor")
public class GroupPostAddedEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	GroupSystem groupSystem;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		GroupPostAddedEvent event = (GroupPostAddedEvent)abstractEvent;

		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), event.getGroupId());
		} catch (NotFoundException e) {
			throw new RuntimeException("GroupPostAddedEvent for non-existant group");
		}
		
		LiveGroup liveGroup = state.getLiveGroup(group.getGuid());
		liveGroup = (LiveGroup) liveGroup.clone();
		liveGroup.setTotalReceivedPosts(liveGroup.getTotalReceivedPosts()+1);
		state.updateLiveGroup(liveGroup);
	}
}
