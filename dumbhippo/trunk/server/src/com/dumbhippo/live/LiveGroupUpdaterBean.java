package com.dumbhippo.live;

import javax.annotation.EJB;
import javax.ejb.Stateless;

import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.GroupSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.Viewpoint;

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
	
	public void initialize(LiveGroup liveGroup) {
		Viewpoint viewpoint = SystemViewpoint.getInstance();
		Group group = loadGroup(liveGroup);
		int totalPosts = postingBoard.getGroupPostsCount(viewpoint, group);
		liveGroup.setTotalReceivedPosts(totalPosts);
		
		liveGroup.setMemberCount(group.getMembers().size());
	}

	public void groupPostReceived(LiveGroup liveGroup) {
		LiveState state = LiveState.getInstance();
		LiveGroup newGroup = (LiveGroup) liveGroup.clone();
		Group group = loadGroup(liveGroup);		
		newGroup.setTotalReceivedPosts(postingBoard.getGroupPostsCount(SystemViewpoint.getInstance(), group));
		state.updateLiveGroup(newGroup);		
	}

	public void groupMemberCountChanged(LiveGroup liveGroup) {
		LiveState state = LiveState.getInstance();
		LiveGroup newGroup = (LiveGroup) liveGroup.clone();
		Group group = loadGroup(liveGroup);
		newGroup.setMemberCount(groupSystem.getMembersCount(SystemViewpoint.getInstance(), group, MembershipStatus.ACTIVE));
		state.updateLiveGroup(newGroup);				
	}
}
