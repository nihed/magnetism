package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.TrackView;

public class MusicPersonPage extends AbstractPersonPage {
	static private final Logger logger = GlobalSetup.getLogger(MusicPersonPage.class);
	
	private Pageable<TrackView> recentTracks;
	private Pageable<TrackView> mostPlayedTracks;
	private Pageable<TrackView> friendsRecentTracks;
	
	public MusicPersonPage() {
	}

	public Pageable<TrackView> getRecentTracks() {
		if (recentTracks == null) {
			logger.debug("Getting recent tracks for {}", getViewedUser());
			recentTracks = pagePositions.createBoundedPageable("recentTracks"); 
			getMusicSystem().pageLatestTrackViews(getViewpoint(), getViewedUser(), recentTracks);
		}
		return recentTracks;
	}
	
	public Pageable<TrackView> getMostPlayedTracks() {
		if (mostPlayedTracks == null) {
			logger.debug("Getting most played tracks for {}", getViewedUser());
			mostPlayedTracks = pagePositions.createBoundedPageable("mostPlayedTracks"); 
			getMusicSystem().pageFrequentTrackViews(getViewpoint(), getViewedUser(), mostPlayedTracks);
		}
		return mostPlayedTracks;
	}
	
	public Pageable<TrackView> getFriendsRecentTracks() {
		if (friendsRecentTracks == null) {
			logger.debug("Getting recent tracks played by the friends of {}", getViewedUser());
			friendsRecentTracks = pagePositions.createBoundedPageable("friendsRecentTracks"); 
			getMusicSystem().pageFriendsLatestTrackViews(getUserSignin().getViewpoint(), friendsRecentTracks);
		}
		return friendsRecentTracks;
	}
}
