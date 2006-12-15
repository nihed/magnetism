package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.GroupMugshotView;
import com.dumbhippo.web.WebEJBUtil;

public class MyGroupsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ActiveGroupsPage.class);
	
	static final int GROUPS_PER_PAGE = 5;
	static final int BLOCKS_PER_GROUP = 5;
	
	protected Stacker stacker;
	
	private Pageable<GroupMugshotView> activeGroups;
	
	public MyGroupsPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public Pageable<GroupMugshotView> getActiveGroups() {
		if (activeGroups == null) {
			activeGroups = pagePositions.createPageable("groups");
			activeGroups.setInitialPerPage(GROUPS_PER_PAGE);
			activeGroups.setSubsequentPerPage(GROUPS_PER_PAGE);
			
			stacker.pageUserGroupActivity(getViewpoint(), getViewedUser(), BLOCKS_PER_GROUP, activeGroups);
		}
		return activeGroups;
	}
}
