package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(ViewPersonPage.class);	

	static private final int MAX_POSTS_SHOWN = 4;

	private PostingBoard postBoard;
	
	private ListBean<PostView> posts;
		
	public ViewPersonPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
	}
	
	public ListBean<PostView> getPosts() {
		if (posts == null) {
			// always ask for max posts shown + 1 as a marker for whether to show the More link
			posts = new ListBean<PostView>(postBoard.getPostsFor(getSignin().getViewpoint(), getViewedUser(), 0, MAX_POSTS_SHOWN + 1));
		}
		return posts;
	}
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
	
}
