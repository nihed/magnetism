package com.dumbhippo.web.pages;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.persistence.MembershipStatus;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker.GroupQueryType;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.GroupView;

/**
 * backing bean for /groups
 * 
 */

public class GroupsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(GroupsPage.class);	
	
	private static final int GROUPS_PER_PAGE = 10;
	private static final int BLOCKS_PER_INVITED_GROUP = 2;
	private static final int BLOCKS_PER_GROUP = 2;
	
	private int activeGroupsCount = -1; 
	private int followedGroupsCount = -1;

	private Pageable<GroupMugshotView> activeGroups;
	private Pageable<GroupMugshotView> activeFollowedGroups;	
	private List<GroupMugshotView> invitedGroupMugshots;
	
	public Pageable<GroupMugshotView> getActiveGroups() {
		if (activeGroups == null) {
			activeGroups = pagePositions.createPageable("groups");
			activeGroups.setInitialPerPage(GROUPS_PER_PAGE);
			activeGroups.setSubsequentPerPage(GROUPS_PER_PAGE);
			
			stacker.pageUserGroupActivity(getViewpoint(), getViewedUser(), BLOCKS_PER_GROUP, GroupQueryType.ACTIVE, activeGroups);
		}
		return activeGroups;
	}
	
	public Pageable<GroupMugshotView> getActiveFollowedGroups() {
		if (activeFollowedGroups == null) {
			activeFollowedGroups = pagePositions.createPageable("groupsFollowed");
			activeFollowedGroups.setInitialPerPage(GROUPS_PER_PAGE);
			activeFollowedGroups.setSubsequentPerPage(GROUPS_PER_PAGE);
			
			stacker.pageUserGroupActivity(getViewpoint(), getViewedUser(), BLOCKS_PER_GROUP, GroupQueryType.FOLLOWED, activeFollowedGroups);
		}
		return activeFollowedGroups;
	}

	public int getActiveGroupsCount() {
		if (activeGroupsCount < 0)
			activeGroupsCount = groupSystem.findGroupsCount(getViewpoint(), getViewedUser(), MembershipStatus.ACTIVE);
			
		return activeGroupsCount;
	}
	
	public int getFollowedGroupsCount() {
		if (followedGroupsCount < 0)
			followedGroupsCount = groupSystem.findGroupsCount(getViewpoint(), getViewedUser(), MembershipStatus.FOLLOWER);
			
		return followedGroupsCount;
	}
	
	public int getActiveAndFollowedGroupsCount() {
		return getActiveGroupsCount() + getFollowedGroupsCount();
	}
	
	public List<GroupMugshotView> getInvitedGroupMugshots() {
		if (invitedGroupMugshots == null) {
			List<Group> groups = new ArrayList<Group>();
			for (GroupView group : getInvitedGroups().getList()) {
				groups.add(group.getGroup());
			}
			invitedGroupMugshots = stacker.getMugshotViews(getViewpoint(), groups, BLOCKS_PER_INVITED_GROUP, false);
		}
		return invitedGroupMugshots;
	}
}
