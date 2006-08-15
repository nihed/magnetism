package com.dumbhippo.live;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.GroupMember;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.MessageSender;
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
	LiveGroupUpdater groupUpdater;
	
	@EJB
	LiveUserUpdater userUpdater;
	
	@EJB
	PostingBoard postingBoard;
	
	@EJB
	MessageSender messageSender;

	@EJB
	GroupSystem groupSystem;
	
	public void process(LiveState state, LiveEvent abstractEvent) {
		GroupEvent event = (GroupEvent)abstractEvent;
		
		if (event.getEvent() == GroupEvent.Type.MEMBERSHIP_CHANGE) {
			groupUpdater.groupMemberCountChanged(event.getGroupId());
			userUpdater.handleGroupMembershipChanged(event.getResourceId());

			try {
				Group group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), event.getGroupId());
				GroupMember groupMember = groupSystem.getGroupMember(group, event.getResourceId());
				if (groupMember.getStatus().equals(MembershipStatus.FOLLOWER) ||
				    groupMember.getStatus().equals(MembershipStatus.ACTIVE)) {					
				    messageSender.sendGroupMembershipUpdate(group, groupMember);
				}
			} catch (NotFoundException e) {
				// probably a follower or an invited e-mail resource was deleted
				logger.debug("Group with guid {} or groupMember for resource with guid {} could not be found: {}",
						     new Object[]{e.getMessage(), event.getGroupId(), event.getResourceId()});
			}
		} else if (event.getEvent() == GroupEvent.Type.POST_ADDED) {
			groupUpdater.groupPostReceived(event.getGroupId());
		}
	}
}
