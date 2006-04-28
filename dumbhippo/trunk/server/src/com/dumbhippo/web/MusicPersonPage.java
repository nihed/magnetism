package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.TrackView;

public class MusicPersonPage extends AbstractPersonPage {
	
	static private final int MAX_RESULTS = 3;
	
	static private final Logger logger = GlobalSetup.getLogger(MusicPersonPage.class);
	
	private ListBean<TrackView> recentTracks;
	private ListBean<TrackView> mostPlayedTracks;
	private ListBean<TrackView> friendsRecentTracks;
	
	public MusicPersonPage() {
	}

	public ListBean<TrackView> getRecentTracks() {
		if (recentTracks == null) {
			logger.debug("Getting recent tracks for {}", getViewedUser());
			recentTracks = new ListBean<TrackView>(getMusicSystem().getLatestTrackViews(getUserSignin().getViewpoint(), getViewedUser(), MAX_RESULTS));
		}
		return recentTracks;
	}
	
	public ListBean<TrackView> getMostPlayedTracks() {
		if (mostPlayedTracks == null) {
			logger.debug("Getting most played tracks for {}", getViewedUser());
			mostPlayedTracks = new ListBean<TrackView>(getMusicSystem().getFrequentTrackViews(getUserSignin().getViewpoint(), getViewedUser(), MAX_RESULTS));
		}
		return mostPlayedTracks;
	}
	
	public ListBean<TrackView> getFriendsRecentTracks() {
		if (friendsRecentTracks == null) {
			logger.debug("Getting recent tracks played by the friends of {}", getViewedUser());
			friendsRecentTracks = new ListBean<TrackView>(getMusicSystem().getFriendsLatestTrackViews(getUserSignin().getViewpoint(), MAX_RESULTS));
		}
		return friendsRecentTracks;
	}
}
