package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.SystemViewpoint;

// Handles processing incoming GroupMembershipChangeEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.GroupMembershipChangeProcessor")
public class GroupMembershipChangeEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	GroupSystem groupSystem;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		GroupMembershipChangeEvent event = (GroupMembershipChangeEvent)abstractEvent;

		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), event.getGroupId());
		} catch (NotFoundException e) {
			throw new RuntimeException("unknown group id " + event.getGroupId());
		}
		
		LiveGroup liveGroup = state.getLiveGroup(event.getGroupId());
		liveGroup = (LiveGroup) liveGroup.clone();
		liveGroup.setMemberCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE));
		state.updateLiveGroup(liveGroup);	
	}
}
