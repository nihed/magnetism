package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;

// Implementation of LiveGroupUpdater
@Stateless
public class LiveGroupUpdaterBean implements LiveGroupUpdater {
	@EJB
	PostingBoard postingBoard;
	
	@EJB
	GroupSystem groupSystem;

	private Group loadGroup(LiveGroup liveGroup) {
		Group group;
		try {
			group = groupSystem.lookupGroupById(SystemViewpoint.getInstance(), liveGroup.getGuid());
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		return group;
	}
	
	private void initializeTotalReceivedPosts(LiveGroup liveGroup, Group group) {
		liveGroup.setTotalReceivedPosts(postingBoard.getGroupPostsCount(SystemViewpoint.getInstance(), group));
	}
	
	private void initializeMemberCount(LiveGroup liveGroup, Group group) {
		liveGroup.setMemberCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE));
		liveGroup.setInvitedMemberCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.INVITED));
		liveGroup.setFollowerCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.FOLLOWER));
		liveGroup.setInvitedFollowerCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.INVITED_TO_FOLLOW));		
	}

	public void initialize(LiveGroup liveGroup) {
		Group group = loadGroup(liveGroup);
		initializeTotalReceivedPosts(liveGroup, group);
		initializeMemberCount(liveGroup, group);
	}

	public void groupPostReceived(LiveGroup liveGroup) {
		LiveState state = LiveState.getInstance();
		LiveGroup newGroup = (LiveGroup) liveGroup.clone();
		initializeTotalReceivedPosts(newGroup, loadGroup(newGroup));
		state.updateLiveGroup(newGroup);		
	}

	public void groupMemberCountChanged(LiveGroup liveGroup) {
		LiveState state = LiveState.getInstance();
		LiveGroup newGroup = (LiveGroup) liveGroup.clone();
		initializeMemberCount(newGroup, loadGroup(newGroup));
		state.updateLiveGroup(newGroup);				
	}
}
