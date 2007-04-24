package com.dumbhippo.server.views;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.dumbhippo.DateUtils;
import com.dumbhippo.StringUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.SongDownloadSource;
import com.dumbhippo.persistence.Track;
import com.dumbhippo.persistence.TrackHistory;

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
	private int totalPlays = -1;
	private int numberOfFriendsWhoPlayedTrack = -1;
	private boolean showExpanded = false;
	private List<PersonMusicPlayView> personMusicPlayViews;
	private TrackHistory trackHistory;
	
	private void fillDefaults() {
		if (name == null)
			name = "Unknown Title";
	}
	
	public TrackView(Track track, TrackHistory trackHistory) {
		this.album = new AlbumView(track.getAlbum(), track.getArtist());
		this.name = track.getName();
		this.durationSeconds = track.getDuration();
		this.trackNumber = track.getTrackNumber();
		this.trackHistory = trackHistory;
		
		if (trackHistory != null)
			this.lastListenTime = trackHistory.getLastUpdated().getTime();
		
		fillDefaults();
	}

	public TrackView(String name, String album, String artist, int duration, int trackNumber) {
		this.album = new AlbumView(album, artist);
		this.name = name;
		this.durationSeconds = duration;
		this.trackNumber = trackNumber; 
		
		fillDefaults();
	}
	
	public AlbumView getAlbumView() {
		return album;
	}
	
	public Map<SongDownloadSource, String> getDownloads() {
		if (downloads == null)
			return Collections.emptyMap();
		else
			return downloads;
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
	
	public String getAlbum() {
		return album.getTitle();
	}

	public String getArtist() {
		return album.getArtist();
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

	public String getSmallImageUrl() {
		return album.getSmallImageUrl();
	}

	public boolean isSmallImageUrlAvailable() {
	    return album.isSmallImageUrlAvailable();
	}
	
	public int getSmallImageWidth() {
		return album.getSmallImageWidth();
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
	
	public String getLastListenString() {
		return DateUtils.formatTimeAgo(new Date(lastListenTime));
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
	
	public String getDisplayTitle() {
		if (getArtist() != null && name != null)
			return getArtist() + " - " + name;
		else if (name != null)
			return getName();
		else if (getArtist() != null)
			return getArtist();
		else
			return "Music";
	}
	
	public void setShowExpanded(boolean showExpanded) {
		this.showExpanded = showExpanded;
	}	
	
	public List<PersonMusicPlayView> getPersonMusicPlayViews() {
		if (personMusicPlayViews != null)
			return personMusicPlayViews;
		else
			return Collections.emptyList();
	}
	
	public PersonMusicPlayView getSinglePersonMusicPlayView() {
		try {
			return getPersonMusicPlayViews().get(0);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public void setPersonMusicPlayViews(List<PersonMusicPlayView> personMusicPlayViews) {
		this.personMusicPlayViews = personMusicPlayViews;
	}
	
	public String getArtistPageLink() {
// /artist doesn't work without the (currently non-functional) Yahoo song search,
// so we just send people to last.fm instead.
//		return URLUtils.buildUrl("/artist", "track", getName(),
//				"artist", getArtist(),
//				"album", getAlbum());
		if (getArtist() != null && getName() != null) {
			try {
				return "http://last.fm/music/" + URLEncoder.encode(getArtist(), "UTF-8") + "/_/" + URLEncoder.encode(getName(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("UTF-8 not supported!");
			}
		} else {
			return null;
		}
	}
	
	public String getPlayId() {
		if (trackHistory != null)
			return trackHistory.getId();
		else
			return null;
	}
	
	public TrackHistory getTrackHistory() {
		return trackHistory;
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
	
	public void writeToXmlBuilder(XmlBuilder builder, String elementName) {
		builder.openElement(elementName,
							"playId", getPlayId(), 
							"lastListenTime", Long.toString(getLastListenTime()),
							"duration", Long.toString(getDurationSeconds() * 1000),
							"nowPlaying", Boolean.toString(isNowPlaying()),
							"url", getArtistPageLink());
		builder.appendTextNode("artist", getArtist());
		builder.appendTextNode("album", getAlbum());
		builder.appendTextNode("name", getName());
		if (album.isSmallImageUrlAvailable()) {
			builder.appendEmptyNode("thumbnail",
									"url", album.getSmallImageUrl(),
									"width", Integer.toString(album.getSmallImageWidth()),
									"height", Integer.toString(album.getSmallImageHeight()));
		}
		
		if (downloads != null) {
			for (SongDownloadSource source: downloads.keySet()) {
				String url = downloads.get(source);
				builder.appendEmptyNode("download", 
										"source", source.name(),
										"url", url);
			}
		}

		builder.closeElement();
	}
}
