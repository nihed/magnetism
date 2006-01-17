package com.dumbhippo.web;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PostView;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.TrackView;

/**
 * Displays a list of posts from a person
 * 
 */

public class ViewPersonPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Log logger = GlobalSetup.getLog(ViewPersonPage.class);	

	static private final int MAX_POSTS_SHOWN = 10;

	private PostingBoard postBoard;
	
	private ListBean<PostView> posts;
	
	private boolean lookedUpCurrentTrack;
	private TrackView currentTrack;
	
	public ViewPersonPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		lookedUpCurrentTrack = false;
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
	
	public TrackView getCurrentTrack() {
		if (!lookedUpCurrentTrack) {
			lookedUpCurrentTrack = true;
			try {
				currentTrack = getMusicSystem().getCurrentTrackView(getSignin().getViewpoint(), getViewedUser());
			} catch (NotFoundException e) {
			}
		}
		return currentTrack;
	}
}
