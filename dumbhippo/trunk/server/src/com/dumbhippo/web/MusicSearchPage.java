package com.dumbhippo.web;

import java.util.ArrayList;
import java.util.List;

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

	private void initRelated() {
		if (relatedPeople == null) {
			List<PersonMusicView> related = musicSystem.getRelatedPeople(signin.getViewpoint(),
					artist, album, song);
			List<TrackView> anonymous = new ArrayList<TrackView>();
			List<PersonMusicView> friends = new ArrayList<PersonMusicView>();
			for (PersonMusicView r : related) {
				if (r.getPerson() != null)
					friends.add(r);
				else
					anonymous.addAll(r.getTracks());
			}
			relatedPeople = new ListBean<PersonMusicView>(friends);
			recommendations = new ListBean<TrackView>(anonymous);
		}
	}
	
	public ListBean<TrackView> getRecommendations() {
		initRelated();
		return recommendations;
	}

	public ListBean<PersonMusicView> getRelatedPeople() {
		initRelated();
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
