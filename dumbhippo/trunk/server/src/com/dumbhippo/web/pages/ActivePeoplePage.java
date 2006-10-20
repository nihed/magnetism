package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.views.PersonMugshotView;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class ActivePeoplePage extends AbstractSigninOptionalPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicGlobalPage.class);
	
	static final int USERS_PER_PAGE = 5;
	static final int BLOCKS_PER_GROUP = 5;
	
	protected Stacker stacker;
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private Pageable<PersonMugshotView> activePeople;
	
	public ActivePeoplePage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public Pageable<PersonMugshotView> getActivePeople() {
		if (activePeople == null) {
			activePeople = pagePositions.createPageable("groups");
			activePeople.setInitialPerPage(USERS_PER_PAGE);
			activePeople.setSubsequentPerPage(USERS_PER_PAGE);
			
			stacker.pageRecentUserActivity(getViewpoint(), activePeople, BLOCKS_PER_GROUP);
		}
		return activePeople;
	}
}