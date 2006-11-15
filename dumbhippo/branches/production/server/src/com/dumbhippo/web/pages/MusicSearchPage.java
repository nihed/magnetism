package com.dumbhippo.web.pages;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.StringUtils;
import com.dumbhippo.server.MusicSystem;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.views.AlbumView;
import com.dumbhippo.server.views.ExpandedArtistView;
import com.dumbhippo.server.views.SystemViewpoint;
import com.dumbhippo.server.views.Viewpoint;
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
	
	// used in error messages so they are less mysterious.
	public String getSearchDescription() {
		if (artist != null && album != null && song != null) {
			return "the song " + song + " on the album " + album + " by " + artist;
		} else if (artist != null && album != null) {
			return "the album " + album + " by " + artist;
		} else if (artist != null && song != null) { 
			return "the song " + song + " by " + artist;
		} else if (artist != null) {
			return "the artist " + artist;
		} else if (album != null) {
			return "the album " + album;
		} else if (song != null) {
			return "the song " + song;
		} else {
			return null;
		}
	}
	
	public String getYahooSearchUrl() {
		StringBuilder sb = new StringBuilder("http://audio.search.yahoo.com/search/audio?p=");
		if (artist != null) {
			sb.append("%22");
			sb.append(StringUtils.urlEncode(artist));
			sb.append("%22+");
		}
		if (album != null) {
			sb.append("%22");
			sb.append(StringUtils.urlEncode(album));
			sb.append("%22+");
		}
		if (song != null) {
			sb.append("%22");
			sb.append(StringUtils.urlEncode(song));
			sb.append("%22+");
		}
		// chop extra +
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}
	
	public String getLastFmArtistUrl() {
		if (artist == null)
			return null;
		
		StringBuilder sb = new StringBuilder("http://www.last.fm/music/");
		sb.append(StringUtils.urlEncode(artist));
		return sb.toString();
	}
	
	public String getArtistOnlyUrl() {
		if (artist == null)
			return null;
		if (album == null && song == null)
			return null; // doesn't make sense to display this if already there
		return "/artist?artist=" + StringUtils.urlEncode(artist);
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
