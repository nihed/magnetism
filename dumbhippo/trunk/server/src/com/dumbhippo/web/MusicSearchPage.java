package com.dumbhippo.web;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.ExpandedArtistView;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.SystemViewpoint;

public class MusicSearchPage {

	static private final Logger logger = GlobalSetup.getLogger(MusicSearchPage.class);
	
	private MusicSystem musicSystem;
	
	private String song;
	private String artist;
	private String album;
	
	private ExpandedArtistView expandedArtistView;
	private ListBean<AlbumView> albumsByArtist;
	private AlbumView bestAlbumView;
	
	boolean expandedArtistViewRetrieved;
	boolean bestAlbumViewRetrieved;

	public MusicSearchPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		expandedArtistView = null;
		bestAlbumView = null;
		expandedArtistViewRetrieved = false;
		bestAlbumViewRetrieved = false;
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

	public String getSong() {
		return song;
	}

	public void setSong(String song) {
		this.song = song;
	}
	
	public ExpandedArtistView getExpandedArtistView() {
		if (expandedArtistViewRetrieved) {
			return expandedArtistView;
		}		
		expandedArtistViewRetrieved = true;
		
		if (artist != null) {
			try {
				expandedArtistView = musicSystem.expandedArtistSearch(SystemViewpoint.getInstance(), artist, album, song);
			} catch (NotFoundException e) {
				logger.debug("Expanded artist not found {}", e.getMessage());
			}
		}
		return expandedArtistView;
	}
	
	public ListBean<AlbumView> getAlbumsByArtist() {
	    getExpandedArtistView();
		if (expandedArtistView != null)
		    albumsByArtist = new ListBean<AlbumView>(expandedArtistView.getAlbums());		
		return albumsByArtist;
	}
	
	public AlbumView getBestAlbum() {
		if (bestAlbumViewRetrieved) {
			return bestAlbumView;
		}
	    bestAlbumViewRetrieved = true;
		
	    getExpandedArtistView();
		if (expandedArtistView != null) {
			if (!expandedArtistView.getAlbums().isEmpty()) {
				bestAlbumView = expandedArtistView.getAlbums().get(0);
			}
		    for (AlbumView albumView : expandedArtistView.getAlbums()) {
		    	// this is a quick check so that we display an album with some album art
		    	if (!albumView.getSmallImageUrl().contains("no_image_available")) {
		    	    bestAlbumView = albumView;
		    	    break;
		    	}
		    }		    
		}
		return bestAlbumView;    
	}
}
