package com.dumbhippo.web;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.PersonMusicView;
import com.dumbhippo.server.TrackView;

public class MusicSearchPage {

	static private final Logger logger = GlobalSetup.getLogger(MusicSearchPage.class);
	
	@Signin
	private SigninBean signin;
	
	private MusicSystem musicSystem;
	
	private enum Mode {
		ARTIST,
		ALBUM, 
		TRACK
	}
	
	private Mode mode;
	
	private String song;
	private String artist;
	private String album;
	
	private boolean triedSearch;
	private TrackView trackView;
	private AlbumView albumView;
	private ListBean<TrackView> recommendations;
	private ListBean<AlbumView> albumRecommendations;
	
	private ListBean<PersonMusicView> relatedPeople;

	public MusicSearchPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		mode = Mode.TRACK;
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

	public void setAlbumMode(boolean enabled) {
		if (enabled)
			mode = Mode.ALBUM;
		else
			mode = Mode.TRACK;
	}

	public void setArtistMode(boolean enabled) {
		if (enabled)
			mode = Mode.ARTIST;
		else
			mode = Mode.TRACK;
	}
	
	private void initRelated() {
		if (relatedPeople == null) {
			
			List<PersonMusicView> related;
			if (mode == Mode.ALBUM) {
				related = musicSystem.getRelatedPeopleWithAlbums(signin.getViewpoint(),
						artist, album, song);
			} else {
				related = musicSystem.getRelatedPeopleWithTracks(signin.getViewpoint(),
						artist, album, song);
			}
		
			List<TrackView> anonymousTracks = new ArrayList<TrackView>();
			List<AlbumView> anonymousAlbums = new ArrayList<AlbumView>();
			List<PersonMusicView> friends = new ArrayList<PersonMusicView>();
			for (PersonMusicView r : related) {
				if (r.getPerson() != null)
					friends.add(r);
				else {
					anonymousTracks.addAll(r.getTracks());
					anonymousAlbums.addAll(r.getAlbums());
				}
			}
			relatedPeople = new ListBean<PersonMusicView>(friends);
			recommendations = new ListBean<TrackView>(anonymousTracks);
			albumRecommendations = new ListBean<AlbumView>(anonymousAlbums);
		}
	}
	
	public ListBean<TrackView> getRecommendations() {
		initRelated();
		return recommendations;
	}

	public ListBean<AlbumView> getAlbumRecommendations() {
		initRelated();
		return albumRecommendations;
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
	
	private void doSearch() {
		if (!triedSearch) {
			triedSearch = true;
			
			if (song != null) {				
				try {
					trackView = musicSystem.songSearch(signin.getViewpoint(),
							artist, album, song);
				} catch (NotFoundException e) {
					logger.debug("Track not found");
				}
			}
			
			if (trackView == null && album != null) {
				// try an album-only search if the track name was unknown
				try {
					albumView = musicSystem.albumSearch(signin.getViewpoint(), artist, album);
				} catch (NotFoundException e) {
					logger.debug("Album not found");
				}
			} else if (trackView != null) {
				// but if we got a track view, be sure we use the same album from it
				albumView = trackView.getAlbumView();
			}
		}
	}
	
	public TrackView getTrackView() {
		doSearch();
		return trackView;
	}
	
	public AlbumView getAlbumView() {
		doSearch();
		return albumView;		
	}
	
	public SigninBean getSignin() {
		return signin;
	}
}
