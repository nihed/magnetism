package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.TrackView;

public class MusicGlobalPage extends AbstractSigninOptionalPage {
	
	static private final int MAX_RESULTS = 3;
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(MusicGlobalPage.class);
	
	private MusicSystem musicSystem;
	
	private ListBean<TrackView> mostPlayedTracks;
	private ListBean<TrackView> onePlayTracks;
	
	public MusicGlobalPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}

	public ListBean<TrackView> getMostPlayedTracks() {
		if (mostPlayedTracks == null) {
			mostPlayedTracks = new ListBean<TrackView>(musicSystem.getFrequentTrackViews(getSignin().getViewpoint(), MAX_RESULTS));
		}
		return mostPlayedTracks;
	}

	public ListBean<TrackView> getOnePlayTracks() {
		if (onePlayTracks == null) {
			onePlayTracks = new ListBean<TrackView>(musicSystem.getOnePlayTrackViews(getSignin().getViewpoint(), MAX_RESULTS));
		}
		return onePlayTracks;
	}
}
