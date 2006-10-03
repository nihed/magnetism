package com.dumbhippo.server.views;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dumbhippo.StringUtils;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;

/**
 * The TrackView does not necessarily represent a Track, but rather can 
 * represent any album track, whether it has been played or not.
 * 
 * FIXME the AlbumView is kind of retrofitted in here, it should 
 * really be TrackView.getAlbumView() and drop most of the "wrapper"
 * accessors
 * 
 * @author hp
 *
 */
public class TrackView {
	 
	private AlbumView album;
	
	private String name;
	private Map<SongDownloadSource,String> downloads; 
	private int durationSeconds;
	private long lastListenTime;
	private int trackNumber; // both -1 and 0 mean that track number is unknown/inapplicable
	                         // valid track numbers are 1-based
	private int totalPlays;
	private int numberOfFriendsWhoPlayedTrack;
	private boolean showExpanded;
	private List<PersonMusicPlayView> personMusicPlayViews;
	
	
	public TrackView() {
		this.album = new AlbumView();
		this.personMusicPlayViews = new ArrayList<PersonMusicPlayView>();
		this.totalPlays = -1;
		this.numberOfFriendsWhoPlayedTrack = -1;
		this.showExpanded = false;
	}
	
	public TrackView(Track track) {
		this.album = new AlbumView(track.getAlbum(), track.getArtist());
		this.name = track.getName();
		this.durationSeconds = track.getDuration();
		this.trackNumber = track.getTrackNumber();
		this.personMusicPlayViews = new ArrayList<PersonMusicPlayView>();
		this.totalPlays = -1;
		this.numberOfFriendsWhoPlayedTrack = -1;
		this.showExpanded = false;
	}

	public TrackView(String name, String album, String artist, int duration, int trackNumber) {
		this.album = new AlbumView(album, artist);
		this.name = name;
		this.durationSeconds = duration;
		this.trackNumber = trackNumber; 
		this.personMusicPlayViews = new ArrayList<PersonMusicPlayView>();
		this.totalPlays = -1;
		this.numberOfFriendsWhoPlayedTrack = -1;
		this.showExpanded = false;
	}
	
	public AlbumView getAlbumView() {
		return album;
	}
	
	public String getDownloadUrl(SongDownloadSource source) {
		if (downloads == null)
			return null;
		else
			return downloads.get(source);
	}
	
	public void setDownloadUrl(SongDownloadSource source, String url) {
		if (downloads == null)
			downloads = new EnumMap<SongDownloadSource,String>(SongDownloadSource.class);
		downloads.put(source, url);
	}
	
	// These getDownloadUrl wrappers are convenient from JSTL expression language
	
	public String getYahooUrl() {
		return getDownloadUrl(SongDownloadSource.YAHOO);
	}

	public String getItunesUrl() {
		return getDownloadUrl(SongDownloadSource.ITUNES);
	}

	public String getRhapsodyUrl() {
		return getDownloadUrl(SongDownloadSource.RHAPSODY);
	}
	
	public String getAlbum() {
		return album.getTitle();
	}

	public void setAlbum(String album) {
		this.album.setTitle(album);
	}

	public String getArtist() {
		return album.getArtist();
	}

	public void setArtist(String artist) {
		album.setArtist(artist);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getTruncatedName() {
	    return StringUtils.truncateString(name, 34);	
	}

	public int getSmallImageHeight() {
		return album.getSmallImageHeight();
	}

	public void setSmallImageHeight(int smallImageHeight) {
		album.setSmallImageHeight(smallImageHeight);
	}

	public String getSmallImageUrl() {
		return album.getSmallImageUrl();
	}

	public void setSmallImageUrl(String smallImageUrl) {
		album.setSmallImageUrl(smallImageUrl);
	}

	public boolean isSmallImageUrlAvailable() {
	    return album.isSmallImageUrlAvailable();
	}
	public int getSmallImageWidth() {
		return album.getSmallImageWidth();
	}

	public void setSmallImageWidth(int smallImageWidth) {
		album.setSmallImageWidth(smallImageWidth);
	}

	public int getDurationSeconds() {
		return durationSeconds;
	}
	
	public void setDurationSeconds(int durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public long getLastListenTime() {
		return lastListenTime;
	}

	public void setLastListenTime(long lastListenTime) {
		this.lastListenTime = lastListenTime;
	}	
	
	public int getTrackNumber() {
		return trackNumber;
	}
	
	public void setTrackNumber(int trackNumber) {
		this.trackNumber = trackNumber;
	}
	
	public int getTotalPlays() {
		return totalPlays;
	}
	
	public void setTotalPlays(int totalPlays) {
		this.totalPlays = totalPlays;
	}	

	public int getNumberOfFriendsWhoPlayedTrack() {
		return numberOfFriendsWhoPlayedTrack;
	}
	
	public void setNumberOfFriendsWhoPlayedTrack(int numberOfFriendsWhoPlayedTrack) {
		this.numberOfFriendsWhoPlayedTrack = numberOfFriendsWhoPlayedTrack;
	}	
	
	public boolean isShowExpanded() {
		return showExpanded;
	}
	
	public void setShowExpanded(boolean showExpanded) {
		this.showExpanded = showExpanded;
	}	
	
	public List<PersonMusicPlayView> getPersonMusicPlayViews() {
		return personMusicPlayViews;
	}
	
	public PersonMusicPlayView getSinglePersonMusicPlayView() {
		if (!personMusicPlayViews.isEmpty()) {
			return personMusicPlayViews.get(0);
		}
		return null;
	}
	
	public void setPersonMusicPlayViews(List<PersonMusicPlayView> personMusicPlayViews) {
		this.personMusicPlayViews = personMusicPlayViews;
	}
	
	@Override
	public String toString() {
		return "{trackView artist=" + getArtist() + " album=" + getAlbum() + " name=" + getName() + "}";
	}
	
	public boolean isNowPlaying() {
		// TODO: make nowPlaing a field in the TrackHistory table that is updated
		// based on when the music actually stopped in addition to using this logic 
		long now = System.currentTimeMillis();
		long songEnd = getLastListenTime() + (getDurationSeconds() * 1000);
		boolean nowPlaying = songEnd + (30*1000) > now;
		return nowPlaying;
	}
}
