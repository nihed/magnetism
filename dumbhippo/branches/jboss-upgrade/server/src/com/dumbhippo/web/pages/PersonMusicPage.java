package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.persistence.NowPlayingTheme;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.Character;
import com.dumbhippo.server.Configuration;
import com.dumbhippo.server.Enabled;
import com.dumbhippo.server.HippoProperty;
import com.dumbhippo.server.InvitationSystem;
import com.dumbhippo.server.NowPlayingThemeSystem;
import com.dumbhippo.server.PromotionCode;
import com.dumbhippo.server.TrackView;
import com.dumbhippo.web.ListBean;
import com.dumbhippo.web.WebEJBUtil;

public class PersonMusicPage extends AbstractPersonPage {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(PersonMusicPage.class);
	
	static private final int LIST_SIZE = 5;
	
	private Configuration configuration;
	private InvitationSystem invitationSystem;
	private NowPlayingThemeSystem nowPlayingSystem;
	private ListBean<TrackView> latestTracks;
	private ListBean<TrackView> frequentTracks;
	private ListBean<TrackView> popularTracks;
	private boolean musicSharingEnabled; 
	private int selfInvitations;
	private ListBean<NowPlayingTheme> exampleThemes;
	
	public PersonMusicPage() {
		selfInvitations = -1;
		configuration = WebEJBUtil.defaultLookup(Configuration.class);
		invitationSystem = WebEJBUtil.defaultLookup(InvitationSystem.class);
		nowPlayingSystem = WebEJBUtil.defaultLookup(NowPlayingThemeSystem.class);
	}

	public ListBean<TrackView> getFrequentTracks() {
		if (frequentTracks == null) {
			frequentTracks = new ListBean<TrackView>(getMusicSystem().getFrequentTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
		}
		
		return frequentTracks;
	}

	public ListBean<TrackView> getLatestTracks() {
		if (latestTracks == null) {
			latestTracks = new ListBean<TrackView>(getMusicSystem().getLatestTrackViews(getSignin().getViewpoint(), getViewedUser(), LIST_SIZE));
		}

		return latestTracks;
	}
	
	public ListBean<TrackView> getPopularTracks() {
		if (popularTracks == null) {
			popularTracks = new ListBean<TrackView>(getMusicSystem().getPopularTrackViews(getSignin().getViewpoint(), LIST_SIZE));
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
	public void setViewedUser(User user) {
		super.setViewedUser(user);
		musicSharingEnabled = getIdentitySpider().getMusicSharingEnabled(user, Enabled.RAW_PREFERENCE_ONLY);
	}
	
	public int getSelfInvitations() {
		if (selfInvitations < 0) {
			selfInvitations = invitationSystem.getInvitations(getAccountSystem().getCharacter(Character.MUSIC_GEEK));
		}
		return selfInvitations;
	}
	
	public String getPromotion() {
		return PromotionCode.MUSIC_INVITE_PAGE_200602.getCode();
	}
	
	public ListBean<NowPlayingTheme> getExampleThemes() {
		if (exampleThemes == null) {
			exampleThemes = new ListBean<NowPlayingTheme>(nowPlayingSystem.getExampleNowPlayingThemes(getSignin().getViewpoint(), 5));
		}
		return exampleThemes;
	}
}
