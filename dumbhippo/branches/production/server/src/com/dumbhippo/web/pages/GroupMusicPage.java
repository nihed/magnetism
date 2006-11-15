package com.dumbhippo.web.pages;

import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.views.TrackView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class GroupMusicPage extends AbstractGroupPage {
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	private MusicSystem musicSystem;

	static private final int LIST_SIZE = 5;
	
	public GroupMusicPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}
	
	public ListBean<TrackView> getFrequentTracks() {
		if (frequentTracks == null) {
			frequentTracks = new ListBean<TrackView>(musicSystem.getFrequentTrackViews(getSignin().getViewpoint(), getViewedGroup(), LIST_SIZE));
		}
		
		return frequentTracks;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			latestTracks = new ListBean<TrackView>(musicSystem.getLatestTrackViews(getSignin().getViewpoint(), getViewedGroup(), LIST_SIZE));
		}

		return latestTracks;
	}	
}
