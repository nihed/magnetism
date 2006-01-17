package com.dumbhippo.web;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TrackView;

public class PersonMusicPage extends AbstractPersonPage {
	static private final Log logger = GlobalSetup.getLog(PersonMusicPage.class);
	
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	
	public PersonMusicPage() {
		
	}

	public ListBean<TrackView> getFrequentTracks() {
		if (frequentTracks == null) {
			try {
				frequentTracks = new ListBean<TrackView>(getMusicSystem().getFrequentTrackViews(getSignin().getViewpoint(), getViewedUser(), 25));
			} catch (NotFoundException e) {
				logger.debug("Failed to load frequent tracks");
				List<TrackView> list = Collections.emptyList();
				frequentTracks = new ListBean<TrackView>(list);
			}
		}
		
		return frequentTracks;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			try {
				latestTracks = new ListBean<TrackView>(getMusicSystem().getLatestTrackViews(getSignin().getViewpoint(), getViewedUser(), 25));
			} catch (NotFoundException e) {
				logger.debug("Failed to load latest tracks");
				List<TrackView> list = Collections.emptyList();
				latestTracks = new ListBean<TrackView>(list);
			}
		}

		return latestTracks;
	}
}
