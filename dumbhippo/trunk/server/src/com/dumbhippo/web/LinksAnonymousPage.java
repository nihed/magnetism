package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksAnonymousPage extends AbstractSigninOptionalPage {
	
	static private final int MAX_RESULTS = 3;
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(LinksAnonymousPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> hotPosts;
	
	public LinksAnonymousPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public ListBean<PostView> getHotPosts() {
		if (hotPosts == null) {
			hotPosts = new ListBean<PostView>(postBoard.getHotPosts(getSignin().getViewpoint(), 0, MAX_RESULTS));
		}
		return hotPosts;
	}
}
