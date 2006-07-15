package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.AlbumView;
import com.dumbhippo.server.ExpandedArtistView;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.SystemViewpoint;
import com.dumbhippo.server.Viewpoint;
import com.dumbhippo.web.PagePositions;
import com.dumbhippo.web.PagePositionsBean;
import com.dumbhippo.web.WebEJBUtil;

public class MusicSearchPage extends AbstractSigninOptionalPage {

	static private final Logger logger = GlobalSetup.getLogger(MusicSearchPage.class);
	
	@PagePositions
	PagePositionsBean pagePositions;
	
	private MusicSystem musicSystem;
	
	private String song;
	private String artist;
	private String album;
	
	private ExpandedArtistView expandedArtistView;
	private Pageable<AlbumView> albumsByArtist;
	
	boolean expandedArtistViewRetrieved;

	public MusicSearchPage() {
		musicSystem = WebEJBUtil.defaultLookup(MusicSystem.class);
		expandedArtistView = null;
		expandedArtistViewRetrieved = false;
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
			    albumsByArtist = pagePositions.createPageable("albumsByArtist"); 				
				// we will know which albums to load into the expandedArtistView based on the Pageable,
			    // the Pageable will come back from this call with the right albums set in its results field,
			    // they will be the same albums as expandedArtistView.getAlbums()
			    Viewpoint viewpoint = SystemViewpoint.getInstance();
			    if (getSignin().isValid()) {
			    	viewpoint = getSignin().getViewpoint();
			    }
				expandedArtistView = 
					musicSystem.expandedArtistSearch(viewpoint, artist, album, song, albumsByArtist);
			} catch (NotFoundException e) {
				logger.debug("Expanded artist not found {}", e.getMessage());
			}
		}
		return expandedArtistView;
	}
	
	public Pageable<AlbumView> getAlbumsByArtist() {
		getExpandedArtistView();
		return albumsByArtist;
	}
}
