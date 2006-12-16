package com.dumbhippo.web.pages;


import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.Group;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.server.views.GroupView;
import com.dumbhippo.web.WebEJBUtil;

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
	
	protected Stacker stacker;	
	
	public GroupsPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);		
	}
	
	private Pageable<GroupMugshotView> activeGroups;
	private List<GroupMugshotView> invitedGroupMugshots;
	
	public Pageable<GroupMugshotView> getActiveGroups() {
		if (activeGroups == null) {
			activeGroups = pagePositions.createPageable("groups");
			activeGroups.setInitialPerPage(GROUPS_PER_PAGE);
			activeGroups.setSubsequentPerPage(GROUPS_PER_PAGE);
			
			stacker.pageUserGroupActivity(getViewpoint(), getViewedUser(), BLOCKS_PER_GROUP, activeGroups);
		}
		return activeGroups;
	}
	
	public List<GroupMugshotView> getInvitedGroupMugshots() {
		if (invitedGroupMugshots == null) {
			List<Group> groups = new ArrayList<Group>();
			for (GroupView group : getInvitedGroups().getList()) {
				groups.add(group.getGroup());
			}
			invitedGroupMugshots = stacker.getMugshotViews(getViewpoint(), groups, BLOCKS_PER_INVITED_GROUP);
		}
		return invitedGroupMugshots;
	}
}
