package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.TrackView;

public class MusicSearchPage {

	static private final Logger logger = GlobalSetup.getLogger(MusicSearchPage.class);
	
	@Signin
	private SigninBean signin;
	
	private MusicSystem musicSystem;
	
	private String song;
	private String artist;
	private String album;
	
	private boolean triedSearch;
	private TrackView trackView;
	private ListBean<TrackView> recommendations;
	
	private ListBean<PersonMusicView> relatedPeople;

	public MusicSearchPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
	}
	
	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public ListBean<TrackView> getRecommendations() {
		if (recommendations == null) {
			recommendations = new ListBean<TrackView>(musicSystem.getRecommendations(signin.getViewpoint(),
					artist, album, song));
		}
		return recommendations;
	}

	public ListBean<PersonMusicView> getRelatedPeople() {
		if (relatedPeople == null) {
			relatedPeople = new ListBean<PersonMusicView>(musicSystem.getRelatedPeople(signin.getViewpoint(),
					artist, album, song));
		}
		return relatedPeople;
	}

	public String getSong() {
		return song;
	}

	public void setSong(String song) {
		this.song = song;
	}
	
	public TrackView getTrackView() {
		if (song == null)
			return null;
		
		if (!triedSearch) {
			triedSearch = true;
			try {
				trackView = musicSystem.songSearch(signin.getViewpoint(),
						artist, album, song);
			} catch (NotFoundException e) {
				logger.debug("Track not found");
			}
		}
		return trackView;
	}
	
	public SigninBean getSignin() {
		return signin;
	}
	
	public boolean getWeGotNothing() {
		return getTrackView() == null;
	}
}
