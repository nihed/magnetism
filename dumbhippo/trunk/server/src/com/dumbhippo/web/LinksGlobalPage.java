package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksGlobalPage extends AbstractSigninOptionalPage {
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LinksGlobalPage.class);
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private PostingBoard postBoard;
	
	private Pageable<PostView> hotPosts;
	
	public LinksGlobalPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public Pageable<PostView> getHotPosts() {
		if (hotPosts == null) {
			hotPosts = pagePositions.createBoundedPageable("hotPosts");
			postBoard.pageHotPosts(getSignin().getViewpoint(), hotPosts);
		}
		return hotPosts;
	}
}
