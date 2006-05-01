package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

public class LinksPersonPage extends AbstractPersonPage {
	
	@PagePositions
	private PagePositionsBean pagePositions;
	
	// These have to match expandablePager.tag
	static private final int INITIAL_RESULT_COUNT = 3;
	
	static private final Logger logger = GlobalSetup.getLogger(LinksPersonPage.class);
	
	private PostingBoard postBoard;
	
	private ListBean<PostView> favoritePosts;
	
	private Pageable<PostView> receivedPosts;
	private Pageable<PostView> sentPosts;
	
	private Boolean notifyPublicShares;
	
	public LinksPersonPage() {
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}

	public Pageable<PostView> getReceivedPosts() {
		if (receivedPosts == null) {
			logger.debug("Getting received posts for {}", getViewedUser());
			receivedPosts = pagePositions.createPageable("receivedPosts");
			postBoard.pageReceivedPosts(getUserSignin().getViewpoint(), getViewedUser(), receivedPosts);
		}
		return receivedPosts;
	}
	
	public ListBean<PostView> getFavoritePosts() {
		if (favoritePosts == null) {
			logger.debug("Getting favorite posts for {}", getViewedUser());
			favoritePosts = new ListBean<PostView>(postBoard.getFavoritePosts(getSignin().getViewpoint(), getViewedUser(), 0, INITIAL_RESULT_COUNT));
		}
		return favoritePosts;
	}
	
	public Pageable<PostView> getSentPosts() {
		if (sentPosts == null) {
			logger.debug("Getting sent posts for {}", getViewedUser());
			sentPosts = pagePositions.createPageable("sentPosts");
			postBoard.pagePostsFor(getSignin().getViewpoint(), getViewedUser(), sentPosts);
		}
		return sentPosts;
	}

	public boolean getNotifyPublicShares() {
		if (notifyPublicShares == null)
			notifyPublicShares = identitySpider.getNotifyPublicShares(getViewedUser());
		return notifyPublicShares;
	}
}
