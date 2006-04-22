package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPage extends AbstractSigninOptionalPage {
	
	static private final int MAX_RECEIVED_POSTS_SHOWN = 4;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> receivedPosts;
	
	public LinksPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getUserSignin().getUser());
			// + 1 as a marker for whether there are more
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(getUserSignin().getViewpoint(), getUserSignin().getUser(), 0, MAX_RECEIVED_POSTS_SHOWN + 1));
		}
		return receivedPosts;
	}
	
	public int getMaxReceivedPostsShown() {
		return MAX_RECEIVED_POSTS_SHOWN;
	}
}
