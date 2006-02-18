package com.dumbhippo.web;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.TrackView;

public class PersonMusicPage extends AbstractPersonPage {
	static private final Logger logger = GlobalSetup.getLogger(PersonMusicPage.class);
	
	static private final int LIST_SIZE = 5;
	
	private Configuration configuration;
	private InvitationSystem invitationSystem;
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	private ListBean<TrackView> popularTracks;
	private boolean musicSharingEnabled; 
	private int selfInvitations;
	
	public PersonMusicPage() {
		selfInvitations = -1;
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
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
	
	public ListBean<TrackView> getPopularTracks() {
		if (popularTracks == null) {
			try {
				popularTracks = new ListBean<TrackView>(getMusicSystem().getPopularTrackViews(LIST_SIZE));
			} catch (NotFoundException e) {
				logger.debug("Failed to load popular tracks");
				List<TrackView> list = Collections.emptyList();
				popularTracks = new ListBean<TrackView>(list);
			}
		}
		
		return popularTracks;
	}
	
	public boolean isMusicSharingEnabled() {
		return musicSharingEnabled;
	}
	
	public String getDownloadUrlWindows() {
		return configuration.getProperty(HippoProperty.DOWNLOADURL_WINDOWS);
	}
	
	@Override
	public void setViewedPerson(User person) {
		super.setViewedPerson(person);
		musicSharingEnabled = getIdentitySpider().getMusicSharingEnabled(person);
	}
	
	public int getSelfInvitations() {
		if (selfInvitations < 0) {
			selfInvitations = invitationSystem.getInvitations(getIdentitySpider().getCharacter(Character.MUSIC_GEEK));
		}
		return selfInvitations;
	}
	
	public String getPromotion() {
		return PromotionCode.MUSIC_INVITE_PAGE_200602.getCode();
	}
}
