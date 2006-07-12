package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class LinksGlobalPage extends AbstractSigninOptionalPage {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LinksGlobalPage.class);
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private PostingBoard postBoard;
	
	private Configuration config;
	
	private Pageable<PostView> hotPosts;
	
	public LinksGlobalPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		config = WebEJBUtil.defaultLookup(Configuration.class);
	}

	public Pageable<PostView> getHotPosts() {
		if (hotPosts == null) {
			hotPosts = pagePositions.createBoundedPageable("recentlyShared");
			postBoard.pageHotPosts(getSignin().getViewpoint(), hotPosts);
		}
		return hotPosts;
	}
	
	// TODO: find an appropriate place for this method, something like 
	// GlobalPage ?
	public boolean isNewFeatures() {
	    return Boolean.valueOf(config.getProperty(HippoProperty.NEW_FEATURES));	
	}
}
