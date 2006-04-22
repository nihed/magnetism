package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPage extends AbstractSigninOptionalPage {
	
	static private final int MAX_RESULTS = 3;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> favoritePosts;
	
	private ListBean<PostView> receivedPosts;
	
	public LinksPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getUserSignin().getUser());
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(getUserSignin().getViewpoint(), getUserSignin().getUser(), 0, MAX_RESULTS));
		}
		return receivedPosts;
	}
		
	public ListBean<PostView> getFavoritePosts() {
		if (favoritePosts == null) {
			logger.debug("Getting favorite posts for {}", getUserSignin().getUser());
			favoritePosts = new ListBean<PostView>(postBoard.getFavoritePosts(getUserSignin().getViewpoint(), getUserSignin().getUser(), 0, MAX_RESULTS));
		}
		return favoritePosts;
	}

}
