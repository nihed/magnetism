package com.dumbhippo.web;

import java.util.Collections;
import java.util.List;

import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TrackView;

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
			try {
				frequentTracks = new ListBean<TrackView>(musicSystem.getFrequentTrackViews(getSignin().getViewpoint(), getViewedGroup(), LIST_SIZE));
			} catch (NotFoundException e) {
				logger.debug("Failed to load frequent tracks, displaying empty list: {}", e.getMessage());
				List<TrackView> list = Collections.emptyList();
				frequentTracks = new ListBean<TrackView>(list);
			}
		}
		
		return frequentTracks;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			try {
				latestTracks = new ListBean<TrackView>(musicSystem.getLatestTrackViews(getSignin().getViewpoint(), getViewedGroup(), LIST_SIZE));
			} catch (NotFoundException e) {
				logger.debug("Failed to load latest tracks: {}", e.getMessage());
				List<TrackView> list = Collections.emptyList();
				latestTracks = new ListBean<TrackView>(list);
			}
		}

		return latestTracks;
	}	
}
