package com.dumbhippo.web.pages;

import java.util.List;

import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.PostingBoard;
import com.dumbhippo.server.views.PostView;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.web.WebEJBUtil;

public class ViewGroupPage extends AbstractGroupPage {
	static private final int MAX_POSTS_SHOWN = 10;
	
	private PostingBoard postBoard;
	private MusicSystem musicSystem;
	private Configuration configuration;
	private boolean haveLatestTrack;
	private TrackView latestTrack;
	
	public ViewGroupPage() {		
		postBoard = WebEJBUtil.defaultLookup(PostingBoard.class);
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}
	
	public List<PostView> getPosts() {
		assert getViewedGroup() != null;
		
		// we ask for 1 extra post to see if we need a "more posts" link
		return postBoard.getGroupPosts(signin.getViewpoint(), getViewedGroup(), 0, MAX_POSTS_SHOWN + 1);
	}

	public TrackView getLatestTrack() {
		if (!haveLatestTrack) {
			haveLatestTrack = true;
			List<TrackView> tracks = musicSystem.getLatestTrackViews(getSignin().getViewpoint(), getViewedGroup(), 1);
			if (tracks.size() > 0)
				latestTrack = tracks.get(0);
		}

		return latestTrack;
	}	
	
	
	public int getMaxPostsShown() {
		return MAX_POSTS_SHOWN;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
}
