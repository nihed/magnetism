package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.web.WebEJBUtil;

public class MyFriendsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MyFriendsPage.class);
	
	static final int USERS_PER_PAGE = 5;
	static final int BLOCKS_PER_USER = 5;
	static final int TOTAL_USERS = 1000;
	
	protected Stacker stacker;
	
	private Pageable<PersonMugshotView> activePeople;
	
	public MyFriendsPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public Pageable<PersonMugshotView> getActivePeople() {
		if (activePeople == null) {
			activePeople = pagePositions.createBoundedPageable("people");
			activePeople.setInitialPerPage(USERS_PER_PAGE);
			activePeople.setSubsequentPerPage(USERS_PER_PAGE);
			activePeople.setBound(TOTAL_USERS);
			
			// FIXME do the query limited by people we are friends with
			stacker.pageRecentUserActivity(getViewpoint(), activePeople, BLOCKS_PER_USER);
		}
		return activePeople;
	}
}
