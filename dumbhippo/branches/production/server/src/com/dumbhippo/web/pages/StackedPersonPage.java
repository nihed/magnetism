package com.dumbhippo.web.pages;

import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.web.WebEJBUtil;

public class StackedPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(StackedPersonPage.class);
	
	// We override the default values for initial and subsequent results per page from Pageable
	static private final int INITIAL_BLOCKS_PER_PAGE = 5;
	static private final int BLOCKS_PER_PAGE = 20;
	
	protected Stacker stacker;
	
	private Pageable<BlockView> pageableMugshot;
	private Pageable<BlockView> pageableStack;
	
	public StackedPersonPage() {
		stacker = WebEJBUtil.defaultLookup(Stacker.class);
	}

	public Pageable<BlockView> getPageableMugshot() {
		if (pageableMugshot == null) {
			pageableMugshot = 
				pagePositions.createPageable("mugshot", INITIAL_BLOCKS_PER_PAGE); 
			pageableMugshot.setSubsequentPerPage(BLOCKS_PER_PAGE);
			pageableMugshot.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedUser(), pageableMugshot, true);
		}

		return pageableMugshot;
	}	
	
	public Pageable<BlockView> getPageableStack() {
		if (pageableStack == null) {
            if (getPageableMugshot().getPosition() == 0) {
			    pageableStack = pagePositions.createPageable("stacker", BLOCKS_PER_PAGE); 
            } else {
            	pageableStack = pagePositions.createBoundedPageable("stacker", INITIAL_BLOCKS_PER_PAGE, INITIAL_BLOCKS_PER_PAGE);
            }
			pageableStack.setSubsequentPerPage(BLOCKS_PER_PAGE);
			pageableStack.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedUser(), pageableStack, false);
		}

		return pageableStack;
	}
	
	public List<BlockView> getFaveStack() {
		return null;
	}
}
