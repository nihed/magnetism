package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.PersonMugshotView;

public class MyFriendsPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MyFriendsPage.class);
	
	static final int USERS_PER_PAGE = 5;
	static final int BLOCKS_PER_USER = 5;
	
	private Pageable<PersonMugshotView> activePeople;

	public Pageable<PersonMugshotView> getActivePeople() {
		if (activePeople == null) {
			activePeople = pagePositions.createPageable("people");
			activePeople.setInitialPerPage(USERS_PER_PAGE);
			activePeople.setSubsequentPerPage(USERS_PER_PAGE);

			stacker.pageContactActivity(getViewpoint(), getViewedUser(), BLOCKS_PER_USER, activePeople);
		}
		return activePeople;
	}
}
