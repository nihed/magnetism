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
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;

// Handles processing incoming GroupEvent

@Stateless
@LocalBinding(jndiBinding="com.dumbhippo.live.GroupEventProcessor")
public class GroupEventProcessor implements LiveEventProcessor {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LiveEventProcessor.class);
	
	@EJB
	GroupSystem groupSystem;
	
	@EJB
	PostingBoard postingBoard;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		GroupEvent event = (GroupEvent)abstractEvent;

		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), event.getGroupId());
		} catch (NotFoundException e) {
			throw new RuntimeException("GroupEvent for non-existant group");
		}
		
		LiveGroup liveGroup = state.getLiveGroup(group.getGuid());
		liveGroup = (LiveGroup) liveGroup.clone();
		if (event.getEvent() == GroupEvent.Type.POST_ADDED) {
			liveGroup.setTotalReceivedPosts(postingBoard.getGroupPostsCount(SystemViewpoint.getInstance(), group));
		} else if (event.getEvent() == GroupEvent.Type.MEMBERSHIP_CHANGE) {
			liveGroup.setMemberCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE));			
		}
		state.updateLiveGroup(liveGroup);
	}
}
