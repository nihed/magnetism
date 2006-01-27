package com.dumbhippo.web;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.TrackView;

public class PersonMusicPage extends AbstractPersonPage {
	static private final Logger logger = GlobalSetup.getLogger(PersonMusicPage.class);
	
	static private final int LIST_SIZE = 5;
	
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	private boolean musicSharingEnabled;
	
	public PersonMusicPage() {
		
	}

	public ListBean<TrackView> getFrequentTracks() {
		if (frequentTracks == null) {
			try {
				frequentTracks = new ListBean<TrackView>(getMusicSystem().getFrequentTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
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
				latestTracks = new ListBean<TrackView>(getMusicSystem().getLatestTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
			} catch (NotFoundException e) {
				logger.debug("Failed to load latest tracks");
				List<TrackView> list = Collections.emptyList();
				latestTracks = new ListBean<TrackView>(list);
			}
		}

		return latestTracks;
	}
	
	public boolean isMusicSharingEnabled() {
		return musicSharingEnabled;
	}
	
	@Override
	public void setViewedPerson(User person) {
		super.setViewedPerson(person);
		musicSharingEnabled = getIdentitySpider().getMusicSharingEnabled(person);
	}
}
