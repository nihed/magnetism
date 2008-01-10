package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.web.WebEJBUtil;

public class StackedGroupPage extends GroupPage {
	@SuppressWarnings("unused")
	private static final Logger logger = GlobalSetup.getLogger(StackedGroupPage.class);
	
	static private final int INITIAL_BLOCKS_PER_PAGE = 5;
	static private final int SUBSEQUENT_BLOCKS_PER_PAGE = 20;

	private Stacker stacker;
	
	private Pageable<BlockView> pageableMugshot;
	private Pageable<BlockView> pageableStack;
		
	public StackedGroupPage() {			
		stacker =  WebEJBUtil.defaultLookup(Stacker.class);
	}
	
	public Pageable<BlockView> getPageableMugshot() {
		if (pageableMugshot == null) {
		    pageableMugshot = pagePositions.createPageable("mugshot", INITIAL_BLOCKS_PER_PAGE); 
			pageableMugshot.setSubsequentPerPage(SUBSEQUENT_BLOCKS_PER_PAGE);
			pageableMugshot.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedGroup().getGroup(), pageableMugshot, true);
		}

		return pageableMugshot;
	}

	public Pageable<BlockView> getPageableStack() {
		if (pageableStack == null) {
		    pageableStack = pagePositions.createPageable("stacker", INITIAL_BLOCKS_PER_PAGE); 
			pageableStack.setSubsequentPerPage(SUBSEQUENT_BLOCKS_PER_PAGE);
			pageableStack.setFlexibleResultCount(true);
			stacker.pageStack(getSignin().getViewpoint(), getViewedGroup().getGroup(), pageableStack, false);
		}

		return pageableStack;
	}
}
