package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPersonPage extends AbstractPersonPage {
	
	static private final int MAX_RESULTS = 3;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPersonPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> favoritePosts;
	
	private ListBean<PostView> receivedPosts;
	private ListBean<PostView> sentPosts;
	
	public LinksPersonPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public ListBean<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getViewedUser());
			receivedPosts = new ListBean<PostView>(postBoard.getReceivedPosts(getUserSignin().getViewpoint(), getViewedUser(), 0, MAX_RESULTS));
		}
		return receivedPosts;
	}
	
	public ListBean<PostView> getFavoritePosts() {
		if (favoritePosts == null) {
			logger.debug("Getting favorite posts for {}", getViewedUser());
			favoritePosts = new ListBean<PostView>(postBoard.getFavoritePosts(getSignin().getViewpoint(), getViewedUser(), 0, MAX_RESULTS));
		}
		return favoritePosts;
	}
	
	public ListBean<PostView> getSentPosts() {
		if (sentPosts == null) {
			logger.debug("Getting sent posts for {}", getViewedUser());
			sentPosts = new ListBean<PostView>(postBoard.getPostsFor(getSignin().getViewpoint(), getViewedUser(), 0, MAX_RESULTS));
		}
		return sentPosts;
	}
}
